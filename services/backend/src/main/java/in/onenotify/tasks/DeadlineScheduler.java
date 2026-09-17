package in.onenotify.tasks;

import in.onenotify.common.Db;
import in.onenotify.integrations.Events;
import in.onenotify.workflow.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeadlineScheduler {
  private final Db db;
  private final Events events;
  private final WorkflowTransitions transitions;

  public DeadlineScheduler(Db db, Events events, WorkflowTransitions transitions) {
    this.db = db;
    this.events = events;
    this.transitions = transitions;
  }

  @Scheduled(fixedDelay = 60000)
  @Transactional
  public void tick() {
    for (var d :
        db.list(
            "select d.*,pc.case_id from deadlines d join provider_cases pc on pc.id=d.provider_case_id join workflow_instances locked_workflow on locked_workflow.id=pc.id where not d.resolved and d.due_at<now()+interval '3 days' and d.reminded_at is null for update of locked_workflow skip locked")) {
      db.update("update deadlines set reminded_at=now() where id=?", d.get("id"));
      events.emit(
          "deadline.approaching",
          (UUID) d.get("provider_case_id"),
          (UUID) d.get("case_id"),
          Map.of("title", "An estimated deadline needs attention"));
    }
    for (var d :
        db.list(
            "select d.*,pc.case_id from deadlines d join provider_cases pc on pc.id=d.provider_case_id join workflow_instances locked_workflow on locked_workflow.id=pc.id where not d.resolved and d.kind='PROVIDER_RESPONSE' and d.due_at<now()-interval '2 days' and d.escalated_at is null for update of locked_workflow skip locked")) {
      var w = transitions.locked((UUID) d.get("provider_case_id"));
      if (StateMachine.next(w.state).contains(WorkflowState.ESCALATION_REQUIRED))
        transitions.move(
            w,
            WorkflowState.ESCALATION_REQUIRED,
            null,
            "System estimate exceeded; consider a follow-up. This is not a statutory deadline.");
      db.update("update deadlines set escalated_at=now() where id=?", d.get("id"));
    }
  }
}
