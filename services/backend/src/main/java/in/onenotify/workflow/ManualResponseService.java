package in.onenotify.workflow;

import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.documents.DocumentService;
import in.onenotify.integrations.Events;
import in.onenotify.providers.ProviderService;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualResponseService {
  public record Response(
      @NotNull WorkflowState outcome,
      @NotBlank @Size(max = 10000) String note,
      @NotBlank @Size(max = 120) String reference,
      String requestedCategory) {}

  private final Db db;
  private final Access access;
  private final WorkflowTransitions transitions;
  private final ProviderService providers;
  private final Events events;

  public ManualResponseService(
      Db db,
      Access access,
      WorkflowTransitions transitions,
      ProviderService providers,
      Events events) {
    this.db = db;
    this.access = access;
    this.transitions = transitions;
    this.providers = providers;
    this.events = events;
  }

  @Transactional
  public Object record(UUID id, Response r) {
    UUID caseId = access.providerCase(id, true);
    access.manage(caseId);
    var pc =
        db.one(
            "select p.adapter_type,p.estimated_days from provider_cases pc join providers p on p.id=pc.provider_id where pc.id=?",
            id);
    if (pc.get("adapter_type").equals("SIMULATED"))
      throw new ApiException(
          400,
          "USE_SIMULATION",
          "Use the clearly labelled simulation controls for this demo provider.");
    if (!Set.of(
            WorkflowState.SUBMITTED,
            WorkflowState.PROVIDER_ACKNOWLEDGED,
            WorkflowState.UNDER_REVIEW,
            WorkflowState.ADDITIONAL_DOCUMENTS_REQUIRED,
            WorkflowState.APPROVED,
            WorkflowState.COMPLETED,
            WorkflowState.REJECTED)
        .contains(r.outcome()))
      throw new ApiException(400, "INVALID_OUTCOME", "Choose a supported provider response.");
    var w = transitions.locked(id);
    if (r.outcome() == WorkflowState.SUBMITTED && w.state != WorkflowState.EXTERNAL_ACTION_REQUIRED)
      throw new ApiException(
          409, "NOT_READY", "Approve the manual package before recording submission.");
    if (r.outcome() == WorkflowState.ADDITIONAL_DOCUMENTS_REQUIRED) {
      if (!DocumentService.CATEGORIES.contains(Objects.toString(r.requestedCategory(), "")))
        throw new ApiException(
            400, "CATEGORY_REQUIRED", "Choose the document the provider requested.");
      db.update(
          "insert into workflow_requirements(id,provider_case_id,category,description) values(?,?,?,?) on conflict do nothing",
          UUID.randomUUID(),
          id,
          r.requestedCategory(),
          "Family-recorded provider request; verify against original correspondence.");
    }
    transitions.move(
        w,
        r.outcome(),
        access.actor(),
        "Family recorded provider outcome; reference: " + r.reference());
    db.update(
        "insert into correspondence(id,case_id,provider_case_id,author_id,kind,subject,body,reference) values(?,?,?,?,'RECORDED_RESPONSE','Family-recorded provider response',?,?)",
        UUID.randomUUID(),
        caseId,
        id,
        access.actor(),
        r.note(),
        r.reference());
    if (r.outcome() == WorkflowState.SUBMITTED)
      db.update(
          "insert into deadlines(id,provider_case_id,kind,source,due_at) values(?,?,'PROVIDER_RESPONSE','SYSTEM_ESTIMATE',now()+make_interval(days=>?))",
          UUID.randomUUID(),
          id,
          pc.get("estimated_days"));
    events.emit(
        "provider-response.received",
        id,
        caseId,
        Map.of("title", "A family member recorded a provider response"));
    return providers.detail(id);
  }
}
