package in.onenotify.workflow;

import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.integrations.Events;
import in.onenotify.providers.ProviderService;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoProviderService {
  private final Db db;
  private final Access access;
  private final WorkflowTransitions transitions;
  private final ProviderService providers;
  private final Events events;
  private final boolean enabled;

  public DemoProviderService(
      Db db,
      Access access,
      WorkflowTransitions transitions,
      ProviderService providers,
      Events events,
      @Value("${app.demo-enabled}") boolean enabled) {
    this.db = db;
    this.access = access;
    this.transitions = transitions;
    this.providers = providers;
    this.events = events;
    this.enabled = enabled;
  }

  @Transactional
  public Object advance(UUID id) {
    UUID caseId = access.providerCase(id, true);
    access.manage(caseId);
    if (!enabled
        || !db.exists(
            "select p.id from providers p join provider_cases pc on pc.provider_id=p.id where pc.id=? and p.demo and p.adapter_type='SIMULATED'",
            id))
      throw new ApiException(403, "DEMO_ONLY", "Simulation is only available for demo providers.");
    var w = transitions.locked(id);
    WorkflowState next =
        switch (w.state) {
          case SUBMITTED -> WorkflowState.PROVIDER_ACKNOWLEDGED;
          case PROVIDER_ACKNOWLEDGED, RESUBMITTED -> WorkflowState.UNDER_REVIEW;
          case UNDER_REVIEW ->
              w.demoStep == 0
                  ? WorkflowState.ADDITIONAL_DOCUMENTS_REQUIRED
                  : WorkflowState.APPROVED;
          case APPROVED -> WorkflowState.COMPLETED;
          default ->
              throw new ApiException(
                  409,
                  "NOT_READY",
                  "Complete the current family action before simulating a response.");
        };
    String message = "DEMO provider response: " + next.name().replace('_', ' ');
    if (next == WorkflowState.ADDITIONAL_DOCUMENTS_REQUIRED) {
      w.demoStep = 1;
      db.update(
          "insert into workflow_requirements(id,provider_case_id,category,description) values(?,?,'NOMINEE_DOCUMENT','DEMO: please provide a nominee document. Verify actual requirements with your provider.') on conflict do nothing",
          UUID.randomUUID(),
          id);
      db.update("update deadlines set resolved=true where provider_case_id=?", id);
      db.update(
          "insert into deadlines(id,provider_case_id,kind,source,due_at) values(?,?,'USER_ACTION','SYSTEM_ESTIMATE',now()+interval '7 days')",
          UUID.randomUUID(),
          id);
      message =
          "DEMO: Please supply a nominee document. This is a simulated requirement, not an official procedure.";
    }
    transitions.move(w, next, null, message);
    db.update(
        "insert into correspondence(id,case_id,provider_case_id,kind,subject,body) values(?,?,?,'PROVIDER_RESPONSE','Simulated provider response',?)",
        UUID.randomUUID(),
        caseId,
        id,
        message);
    events.emit(
        "provider-response.received",
        id,
        caseId,
        Map.of("title", "A simulated provider response arrived", "state", next.name()));
    return providers.detail(id);
  }
}
