package in.onenotify;

import static org.mockito.Mockito.*;

import in.onenotify.common.Db;
import in.onenotify.integrations.Events;
import in.onenotify.tasks.DeadlineScheduler;
import in.onenotify.workflow.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DeadlineSchedulerTest {
  @Test
  void reminderAndOverdueEscalationAreDistinct() {
    Db db = mock(Db.class);
    Events events = mock(Events.class);
    WorkflowTransitions transitions = mock(WorkflowTransitions.class);
    UUID id = UUID.randomUUID(), caseId = UUID.randomUUID(), deadline = UUID.randomUUID();
    Map<String, Object> row = Map.of("id", deadline, "provider_case_id", id, "case_id", caseId);
    when(db.list(anyString())).thenReturn(List.of(row), List.of(row));
    Workflow workflow = new Workflow(id);
    workflow.state = WorkflowState.SUBMITTED;
    when(transitions.locked(id)).thenReturn(workflow);
    new DeadlineScheduler(db, events, transitions).tick();
    verify(events).emit(eq("deadline.approaching"), eq(id), eq(caseId), anyMap());
    verify(transitions)
        .move(
            eq(workflow),
            eq(WorkflowState.ESCALATION_REQUIRED),
            isNull(),
            contains("not a statutory deadline"));
  }

  @Test
  void completedWorkflowsCannotBeEscalated() {
    Db db = mock(Db.class);
    Events events = mock(Events.class);
    WorkflowTransitions transitions = mock(WorkflowTransitions.class);
    UUID id = UUID.randomUUID();
    Map<String, Object> row =
        Map.of("id", UUID.randomUUID(), "provider_case_id", id, "case_id", UUID.randomUUID());
    when(db.list(anyString())).thenReturn(List.of(), List.of(row));
    Workflow workflow = new Workflow(id);
    workflow.state = WorkflowState.COMPLETED;
    when(transitions.locked(id)).thenReturn(workflow);
    new DeadlineScheduler(db, events, transitions).tick();
    verify(transitions, never()).move(any(), any(), any(), anyString());
  }
}
