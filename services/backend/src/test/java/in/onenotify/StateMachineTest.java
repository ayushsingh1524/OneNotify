package in.onenotify;

import static in.onenotify.workflow.WorkflowState.*;
import static org.junit.jupiter.api.Assertions.*;

import in.onenotify.common.ApiException;
import in.onenotify.workflow.*;
import org.junit.jupiter.api.Test;

class StateMachineTest {
  @Test
  void completeLifecycleRequiresReviewAndAdditionalDocuments() {
    WorkflowState[] path = {
      DRAFT,
      DOCUMENTS_PENDING,
      READY_TO_SUBMIT,
      USER_APPROVAL_REQUIRED,
      SUBMITTED,
      PROVIDER_ACKNOWLEDGED,
      UNDER_REVIEW,
      ADDITIONAL_DOCUMENTS_REQUIRED,
      READY_TO_SUBMIT,
      USER_APPROVAL_REQUIRED,
      RESUBMITTED,
      UNDER_REVIEW,
      APPROVED,
      COMPLETED
    };
    for (int i = 1; i < path.length; i++) StateMachine.validate(path[i - 1], path[i]);
  }

  @Test
  void cannotSkipApprovalOrReopenTerminalState() {
    assertThrows(ApiException.class, () -> StateMachine.validate(DRAFT, SUBMITTED));
    assertThrows(ApiException.class, () -> StateMachine.validate(UNDER_REVIEW, COMPLETED));
    for (var state : WorkflowState.values()) {
      assertThrows(ApiException.class, () -> StateMachine.validate(COMPLETED, state));
      assertThrows(ApiException.class, () -> StateMachine.validate(CANCELLED, state));
    }
  }

  @Test
  void manualSubmissionsAreExplicit() {
    StateMachine.validate(USER_APPROVAL_REQUIRED, EXTERNAL_ACTION_REQUIRED);
    StateMachine.validate(EXTERNAL_ACTION_REQUIRED, SUBMITTED);
  }

  @Test
  void pauseRequiresFreshDocumentReview() {
    StateMachine.validate(UNDER_REVIEW, PAUSED);
    StateMachine.validate(PAUSED, DOCUMENTS_PENDING);
    assertThrows(ApiException.class, () -> StateMachine.validate(PAUSED, SUBMITTED));
  }
}
