# Security model

## Identity and authorization

BCrypt cost 12 protects passwords (12–72 characters for new passwords). Access tokens are signed HMAC JWTs with a 15-minute lifetime and fixed issuer. Refresh tokens are 256-bit random values; only SHA-256 hashes are stored. Refresh rotates under a row lock; old tokens are revoked. Access tokens carry the refresh-session ID; every authenticated request checks that session against PostgreSQL. Logout, refresh rotation and password reset invalidate the associated access tokens for subsequent requests. Password reset revokes every refresh session. Already executing requests are not retroactively cancelled.

Non-demo registration requires a single-use, expiring email verification link before login and before invited membership becomes active. Demo registration bypasses email verification **only for local demonstration**. Existing verified users can be granted case membership by an owner or family administrator. Inviting a person is not proof of legal authority.

Case access is looked up from current membership on every read/write. This also applies after a vault ticket has been issued, so revocation prevents a subsequent download. Nonmembers receive 404. Viewers cannot mutate. Contributors/advisors can add documents, tasks and notes but cannot grant access or approve submission. Only case owners can request case deletion. System-admin status does not confer document access.

## Browser boundaries

Refresh cookies are HttpOnly, SameSite=Strict, scoped to `/api/v1/auth`; production requires Secure. Access tokens are memory-only. Normal state-changing endpoints require a bearer token, not an ambient authentication cookie. Cross-site unsafe requests are rejected through Fetch Metadata; Spring does not enable cross-origin API access. Cookie-authenticated refresh/logout are protected by strict same-site cookies. Use HTTPS and a trusted same-origin reverse proxy in production.

Security headers include no-sniff, frame denial, CSP, same-origin referrers and permissions policy. PDF previews use a sandboxed blob iframe. Inline script allowances are currently required by the Next.js rendering setup; nonce-based CSP is a hardening follow-up. Browser-generated files remain the user’s responsibility after download.

## Vault

- Maximum upload: 10 MiB; only PDF, PNG and JPEG magic signatures matching the declared MIME.
- Original bytes are immutable; each upload has its own object UUID and SHA-256 checksum.
- AES-256-GCM with a fresh 12-byte nonce and authenticated case/document key.
- A 32-byte base64 key is injected through `VAULT_KEY`. Production must use managed per-tenant/envelope keys and rotation, not the development key.
- Private bucket; application-authenticated plaintext reads use one-time Redis tickets with a 60-second lifetime.
- Storage abstraction supports signed ciphertext URLs; the UI never exposes an unencrypted public bucket URL.
- Local default scanning is an explicit **demo signature stub**, not antivirus. Set `MALWARE_SCANNER=clamav` for the real private clamd INSTREAM adapter. Only a complete clean response is accepted; connection failures, infections and malformed replies fail closed. Staging selects ClamAV. The daemon/signature lifecycle must be verified on the deployment host. Strict parser validation and process isolation remain open.
- Every read/export is audited. Deletion requests remove a file from new packages pending retention review.

## Consent and submission

Consent binds an actor, case, provider workflow, selected immutable document IDs, shared-field names, purpose, and a 30-minute expiry. Submission rechecks document membership, availability and requirements. A row lock serializes workflow commands. The unique `(provider_case_id,key)` stores the original actor, consent and result. Replaying the same request returns the same result; a key reused with a different actor/consent fails. No real external adapter transmits documents in this implementation.

## Logging and operations

Correlation IDs are server-generated, not blindly trusted from clients. Logs contain event IDs and failure types, not raw files, passwords, tokens or AI document text. Public errors are standardized and hide stack traces. Audit/profile updates store action metadata, not full sensitive before/after profiles. Actuator health is public; other actuator endpoints require the admin authority. Swagger describes public API shapes, not user records.

Configure `APP_ORIGIN`, `SECURE_COOKIE`, `JWT_SECRET`, `VAULT_KEY`, `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `REDIS_HOST`, `KAFKA_BOOTSTRAP`, `MINIO_ENDPOINT`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, and `MAIL_HOST` appropriately. Local Compose defaults to Mailpit. Staging configures authenticated STARTTLS using `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH`, `MAIL_STARTTLS`, and `MAIL_FROM`; verified external delivery remains an environment check.

## Required preproduction work

Independent threat modelling and penetration test; scanner/signature operational verification; strict file parsing; key management/rotation; provider verification; backup and restore drills; separate migration execution and removal of migration credentials from runtime; audit export to external tamper-evident storage; retention/erasure operations; email delivery hardening; dependency monitoring; accessibility testing; legal/privacy review. PostgreSQL triggers prevent application updates/deletes to history but a database superuser can alter them. This is not a compliance certification.

Staging provisions a non-superuser runtime database role and separate Flyway credentials. Migration V5 removes runtime writes to migration history and update/delete/truncate rights on audit/workflow history. Runtime cannot alter tables. The application process still receives migration credentials at startup; isolate migration execution before production.
