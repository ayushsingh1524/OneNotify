package in.onenotify.workflow;

import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.consent.ConsentService;
import in.onenotify.integrations.*;
import in.onenotify.providers.ProviderService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {
  private final Db db;
  private final Access access;
  private final WorkflowTransitions transitions;
  private final ProviderService providers;
  private final ConsentService consents;
  private final AdapterRegistry adapters;
  private final Json json;

  public WorkflowService(
      Db db,
      Access access,
      WorkflowTransitions transitions,
      ProviderService providers,
      ConsentService consents,
      AdapterRegistry adapters,
      Json json) {
    this.db = db;
    this.access = access;
    this.transitions = transitions;
    this.providers = providers;
    this.consents = consents;
    this.adapters = adapters;
    this.json = json;
  }

  @Transactional
  public Object prepare(UUID id) {
    UUID caseId = access.providerCase(id, true);
    var w = transitions.locked(id);
    boolean missing =
        providers.requirements(id, caseId).stream()
            .anyMatch(r -> !Boolean.TRUE.equals(r.get("available")));
    if (w.state == WorkflowState.USER_APPROVAL_REQUIRED && !missing) return providers.detail(id);
    if (missing) {
      if (w.state != WorkflowState.DOCUMENTS_PENDING)
        transitions.move(
            w, WorkflowState.DOCUMENTS_PENDING, access.actor(), "Required documents are missing.");
    } else {
      if (w.state != WorkflowState.READY_TO_SUBMIT)
        transitions.move(
            w, WorkflowState.READY_TO_SUBMIT, access.actor(), "Required documents are available.");
      transitions.move(
          w,
          WorkflowState.USER_APPROVAL_REQUIRED,
          access.actor(),
          "Please review exactly what will be shared.");
    }
    return providers.detail(id);
  }

  @Transactional
  public Object submit(UUID id, UUID consent, String key) {
    UUID caseId = access.providerCase(id, true);
    access.manage(caseId);
    if (key == null || !key.matches("[A-Za-z0-9_-]{8,120}"))
      throw new ApiException(400, "IDEMPOTENCY_REQUIRED", "A valid idempotency key is required.");
    var w = transitions.locked(id);
    var prior =
        db.list(
            "select result,actor_id,consent_id from submission_keys where provider_case_id=? and key=?",
            id,
            key);
    if (!prior.isEmpty()) {
      var p = prior.getFirst();
      if (!p.get("actor_id").equals(access.actor()) || !p.get("consent_id").equals(consent))
        throw new ApiException(409, "KEY_REUSED", "Use a new key for a different request.");
      return json.read(p.get("result").toString());
    }
    if (w.state != WorkflowState.USER_APPROVAL_REQUIRED)
      throw new ApiException(409, "NOT_READY", "Prepare and approve the request first.");
    var docs = consents.validate(consent, id, caseId);
    var provider =
        db.one(
            "select p.adapter_type,p.estimated_days,pc.action from providers p join provider_cases pc on pc.provider_id=p.id where pc.id=?",
            id);
    var adapter = adapters.get(provider.get("adapter_type").toString());
    var result =
        adapter.submit(
            new ProviderAdapter.Request(id, consent, docs, provider.get("action").toString()));
    var target = WorkflowState.valueOf(result.status());
    if (target == WorkflowState.SUBMITTED && w.submittedAt != null)
      target = WorkflowState.RESUBMITTED;
    transitions.move(w, target, access.actor(), result.instruction());
    db.update("update consents set consumed_at=now() where id=?", consent);
    db.update("update deadlines set resolved=true where provider_case_id=?", id);
    if (target == WorkflowState.SUBMITTED || target == WorkflowState.RESUBMITTED)
      db.update(
          "insert into deadlines(id,provider_case_id,kind,source,due_at) values(?,?,'PROVIDER_RESPONSE','SYSTEM_ESTIMATE',now()+make_interval(days=>?))",
          UUID.randomUUID(),
          id,
          provider.get("estimated_days"));
    db.update(
        "insert into submission_keys(provider_case_id,key,actor_id,consent_id,result) values(?,?,?,?,?::jsonb)",
        id,
        key,
        access.actor(),
        consent,
        json.write(result));
    return result;
  }

  @Transactional
  public Object action(UUID id, String action) {
    access.providerCase(id, true);
    var w = transitions.locked(id);
    var next =
        switch (action) {
          case "pause" -> WorkflowState.PAUSED;
          case "cancel" -> WorkflowState.CANCELLED;
          case "resume" -> WorkflowState.DOCUMENTS_PENDING;
          default -> throw new ApiException(400, "INVALID_ACTION", "Unknown action.");
        };
    transitions.move(w, next, access.actor(), "Family requested: " + action);
    return providers.detail(id);
  }
}
