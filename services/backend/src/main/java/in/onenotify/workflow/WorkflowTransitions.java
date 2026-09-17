package in.onenotify.workflow;

import in.onenotify.audit.Audit;
import in.onenotify.common.*;
import in.onenotify.integrations.Events;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowTransitions {
  private final Db db;
  private final WorkflowRepository repo;
  private final Audit audit;
  private final Events events;

  public WorkflowTransitions(Db db, WorkflowRepository repo, Audit audit, Events events) {
    this.db = db;
    this.repo = repo;
    this.audit = audit;
    this.events = events;
  }

  public Workflow locked(UUID id) {
    return repo.lock(id)
        .orElseThrow(() -> new ApiException(404, "NOT_FOUND", "Workflow not found."));
  }

  @Transactional
  public void move(Workflow w, WorkflowState next, UUID actor, String reason) {
    StateMachine.validate(w.state, next);
    var pc = db.one("select case_id from provider_cases where id=?", w.id);
    UUID caseId = (UUID) pc.get("case_id");
    var previous = w.state;
    w.state = next;
    w.updatedAt = Instant.now();
    if ((next == WorkflowState.SUBMITTED || next == WorkflowState.RESUBMITTED)
        && w.submittedAt == null) w.submittedAt = Instant.now();
    repo.saveAndFlush(w);
    db.update(
        "insert into workflow_events(id,provider_case_id,actor_id,from_state,to_state,reason) values(?,?,?,?,?,?)",
        UUID.randomUUID(),
        w.id,
        actor,
        previous.name(),
        next.name(),
        reason);
    audit.record(
        actor,
        caseId,
        w.id,
        "WORKFLOW_" + next.name(),
        "PROVIDER_CASE",
        w.id,
        Map.of("state", previous),
        Map.of("state", next));
    events.emit(
        "workflow.status-changed",
        w.id,
        caseId,
        Map.of(
            "state",
            next.name(),
            "previous",
            previous.name(),
            "title",
            "Organization checklist updated"));
    if (Set.of(WorkflowState.COMPLETED, WorkflowState.CANCELLED, WorkflowState.PAUSED)
        .contains(next))
      db.update("update deadlines set resolved=true where provider_case_id=?", w.id);
    if (next == WorkflowState.COMPLETED
        && !db.exists(
            "select pc.id from provider_cases pc join workflow_instances w on w.id=pc.id where pc.case_id=? and w.state<>'COMPLETED'",
            caseId)) {
      db.update("update bereavement_cases set status='COMPLETED' where id=?", caseId);
      audit.record(
          actor,
          caseId,
          null,
          "CASE_COMPLETED",
          "CASE",
          caseId,
          Map.of(),
          Map.of("status", "COMPLETED"));
      events.emit(
          "notification.requested",
          caseId,
          caseId,
          Map.of("title", "All organization requests in your case are complete"));
    }
  }
}
