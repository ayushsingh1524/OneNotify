package in.onenotify.workflow;

import static in.onenotify.workflow.WorkflowState.*;

import in.onenotify.common.ApiException;
import java.util.*;

public final class StateMachine {
  private static final Map<WorkflowState, Set<WorkflowState>> ALLOWED =
      Map.ofEntries(
          Map.entry(DRAFT, Set.of(DOCUMENTS_PENDING, READY_TO_SUBMIT, CANCELLED)),
          Map.entry(DOCUMENTS_PENDING, Set.of(READY_TO_SUBMIT, PAUSED, CANCELLED)),
          Map.entry(
              READY_TO_SUBMIT,
              Set.of(USER_APPROVAL_REQUIRED, DOCUMENTS_PENDING, PAUSED, CANCELLED)),
          Map.entry(
              USER_APPROVAL_REQUIRED,
              Set.of(
                  SUBMITTED,
                  RESUBMITTED,
                  EXTERNAL_ACTION_REQUIRED,
                  DOCUMENTS_PENDING,
                  PAUSED,
                  CANCELLED)),
          Map.entry(
              SUBMITTED,
              Set.of(PROVIDER_ACKNOWLEDGED, FAILED, ESCALATION_REQUIRED, PAUSED, CANCELLED)),
          Map.entry(
              PROVIDER_ACKNOWLEDGED,
              Set.of(UNDER_REVIEW, ADDITIONAL_DOCUMENTS_REQUIRED, PAUSED, CANCELLED)),
          Map.entry(
              UNDER_REVIEW,
              Set.of(
                  ADDITIONAL_DOCUMENTS_REQUIRED,
                  APPROVED,
                  REJECTED,
                  ESCALATION_REQUIRED,
                  PAUSED,
                  CANCELLED)),
          Map.entry(
              ADDITIONAL_DOCUMENTS_REQUIRED,
              Set.of(READY_TO_SUBMIT, DOCUMENTS_PENDING, PAUSED, CANCELLED)),
          Map.entry(
              RESUBMITTED,
              Set.of(
                  UNDER_REVIEW,
                  PROVIDER_ACKNOWLEDGED,
                  FAILED,
                  ESCALATION_REQUIRED,
                  PAUSED,
                  CANCELLED)),
          Map.entry(APPROVED, Set.of(COMPLETED)),
          Map.entry(FAILED, Set.of(READY_TO_SUBMIT, DOCUMENTS_PENDING, CANCELLED)),
          Map.entry(
              ESCALATION_REQUIRED,
              Set.of(UNDER_REVIEW, READY_TO_SUBMIT, DOCUMENTS_PENDING, PAUSED, CANCELLED)),
          Map.entry(EXTERNAL_ACTION_REQUIRED, Set.of(SUBMITTED, PAUSED, CANCELLED)),
          Map.entry(PAUSED, Set.of(DOCUMENTS_PENDING, CANCELLED)),
          Map.entry(REJECTED, Set.of(DOCUMENTS_PENDING, CANCELLED)));

  public static void validate(WorkflowState from, WorkflowState to) {
    if (!ALLOWED.getOrDefault(from, Set.of()).contains(to))
      throw new ApiException(
          409, "INVALID_TRANSITION", "This action is not available at the current step.");
  }

  public static Set<WorkflowState> next(WorkflowState from) {
    return ALLOWED.getOrDefault(from, Set.of());
  }

  private StateMachine() {}
}
