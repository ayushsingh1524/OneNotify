# Preproduction review pack

This is an engineering handoff for an independent reviewer and family usability sessions, not a completed external audit. Date: 17 September 2026.

## Scope and data flow

Browser → same-origin HTTPS gateway → Next.js proxy → Spring Boot → PostgreSQL/Redis/Kafka and private encrypted object storage. ClamAV receives plaintext upload bytes on the private container network before persistence. SMTP receives destination addresses and single-use verification/reset links. Browser downloads leave application custody. No provider or Gmail receives user data in the supplied application.

Assets: family identities, deceased-person details, document originals, vault key, consent records, session/reset credentials, provider correspondence and audit history. Trust boundaries: public ingress, family membership, privileged operator, application/database, storage/scanner, mail provider, backup operator and future external provider.

## Review matrix

| Threat / failure | Existing control and evidence | Remaining review |
|---|---|---|
| Another family reads a case/file | Per-request membership, 404 isolation, ticket actor binding; API tests | Enumerate every new endpoint and export path |
| Revoked session remains usable | Signed session ID checked against active database token; logout/reset/rotation tests | Concurrent requests, multi-tab UX, reset/session race cases |
| Malicious or malformed document | Size/MIME magic checks; real scanner adapter rejects non-clean/error/timeout | Actual signatures, strict parser validation, isolated processing, orphan cleanup |
| Administrator bypasses family access | System role does not grant document membership; API tests | Operational access, secret-manager policy and privileged actions |
| History tampering | Append-only triggers; staging runtime cannot update/delete/truncate history | External audit anchoring and migration credentials isolation |
| Key loss / compromise | AEAD per object, unique nonce, context binding; crypto tests | Managed envelope keys, rotation, escrow and whole-vault recovery |
| Duplicate or unauthorized submission | Exact-document expiring consent, locks, idempotency tests | Real delivery outbox, reconciliation and signed provider receipts |
| Backup loss / leak | Streaming age encryption and database restore drill | Off-host copy, retention, key segregation, actual object/API recovery |
| Mail or account takeover | Single-use reset/verification, secure staging cookies, STARTTLS configuration | Sender-domain verification, delivery/rate abuse and identity assurance |
| Deletion contradicts legal hold / consent evidence | Requests recorded; no automatic erasure | Approved retention/hold policy and retryable erasure implementation |
| Public staging exposes sample data | IP allowlist, no demo seed, only HTTPS gateway public | Host firewall, DNS/TLS issuance and actual network scan |

## Independent security/privacy reviewer deliverables

Review authentication, authorization, reset/enrollment, document handling, key management, export/consent, audit, logging, dependencies and deployment configuration. Supply reproducible findings with severity, affected versions, remediation and retest evidence. Record the actual entity operating the service, purpose/data minimization, legal basis, processor terms, retention and rights-handling process. No compliance standard or legal conclusion is asserted by this pack.

## Family usability session — synthetic data only

Allow 30–45 minutes per participant. Include keyboard-only use, screen-reader use where available, a small mobile screen and at least one person unfamiliar with administrative terminology.

Ask the participant to: create a case; resume an unfinished draft; find/add an organization; upload and preview a sample; distinguish missing documents from waiting for a response; invite a viewer; explain the sharing approval; prepare a manual request; record an acknowledgment; respond to a new document request; find the timeline; export a summary; sign out.

Record completion, assistance needed, confusing wording, accidental sharing concerns and recovery from an error. Do not coach during the first attempt. Confirm they understand that a prepared manual package has not been submitted and a family-reported update is not provider-authenticated. Capture no real bereavement documents or account details.

Automated Axe scans cover selected page states only. They do not establish screen-reader usability, translated-language accuracy, low-literacy comprehension or legal accessibility compliance.

## Release gates

A named owner must accept review findings, demonstrate whole-system restore and key recovery, confirm retention/erasure behavior, verify deployed scanning/mail/TLS, and approve the exact provider scenario. Until then, stage only synthetic data. Local test results cannot substitute for those acceptance decisions.
