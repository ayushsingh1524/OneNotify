# Workflow engine

`StateMachine` is the single transition authority. Controllers expose business commands (`prepare`, `submit`, `pause`, `cancel`, `resume`, and demo response), never arbitrary strings that directly overwrite a state.

`WorkflowTransitions` locks the JPA aggregate and validates each transition, updates its timestamp/version, appends a workflow event and audit event, and adds a version-1 outbox message. Terminal and paused workflows resolve their active estimates. Provider submission creates a system-estimated response deadline; a simulated additional-document response replaces it with a user-action estimate.

## Submission

1. Reauthorize actor and case membership.
2. Obtain the workflow row lock.
3. Return the prior result if the same idempotency key, actor and consent are already recorded.
4. Require `USER_APPROVAL_REQUIRED`.
5. Validate unexpired, unrevoked, unused consent and current selected-document requirements.
6. Execute the configured adapter. Current adapters are side-effect-free manual or simulated implementations.
7. Record transition, consume consent, set deadline, save result and commit the audit/outbox.

Real adapters must move external transmission into a durable delivery job/outbox, use the same stable provider idempotency reference, and reconcile ambiguous timeout outcomes. Do not bolt a non-idempotent network call into the current database transaction.

## Additional documents

The first simulated review requests `NOMINEE_DOCUMENT`. This is a demo rule, not an official provider procedure. The requirement is attached to that specific workflow, not silently applied to every family. A new upload and fresh consent are necessary before resubmission. The subsequent simulated review approves and completes.

## Reminders

Every minute, the scheduler emits a reminder when an unresolved estimate is within three days. Two days after a provider-response estimate, it suggests escalation through a validated transition where allowed. `OFFICIAL`, `PROVIDER_DEFINED`, and `SYSTEM_ESTIMATE` are distinct database values; demo data uses only `SYSTEM_ESTIMATE`. Reminded/escalated timestamps prevent repeated events. Scheduler rows use skip-locked transactions for safe multiple instances.

Pause does not submit or cancel anything with a real provider. Resume returns to document review and requires a new preparation/consent cycle. Completed/cancelled states cannot be reopened. Cancellation preserves history.

## Manual provider updates

Non-simulated adapters stop at external action required. After completing the provider’s official process, an owner/admin may record submission and subsequent outcomes through `record-response`, with a reference and note. These are explicitly family-reported records, not authenticated provider decisions. The same state graph and audit requirements apply. Additional-document outcomes require a supported category. When every organization workflow is completed, the case status and completion notification update; adding another provider reactivates it.
