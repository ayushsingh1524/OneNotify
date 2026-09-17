# Local verification — 17 September 2026

The complete Docker Compose application is running locally. All seven services are healthy: frontend, backend, PostgreSQL, Redis, Kafka, MinIO and Mailpit. Application images build successfully.

| Check | Result |
|---|---|
| `mvn -q -f services/backend/pom.xml test spotless:check` | 23 tests pass; no failures, errors or skips; includes real PostgreSQL Testcontainers migrations and append-only audit checks |
| `npm --prefix apps/web run format:check` | Pass |
| `npm --prefix apps/web run lint` | Pass |
| `npm --prefix apps/web run typecheck` | Pass |
| `npm --prefix apps/web test` | 6 tests pass |
| `npm --prefix apps/web run test:e2e` | 5 Chromium tests pass against Docker frontend |
| `node scripts/integration.mjs` | 56 API checks pass |
| `node scripts/security-extra.mjs` | Session rotation/replay, cross-site rejection, Mailpit password reset, single-use reset, password change and refresh revocation pass |
| `node scripts/manual-lifecycle.mjs` | Consent-gated preparation, family-recorded provider responses and automatic case completion pass |
| `docker compose up -d --wait` | All seven services healthy |

Browser coverage includes automated WCAG A/AA scans on landing/dashboard, seven public account/help/privacy routes, and eight family-workspace routes, desktop navigation, 390-pixel mobile vault navigation without horizontal overflow, case creation, document upload and preview, provider selection, explicit consent, simulated submission, additional-document request and resubmission, completion, and ZIP package download. Screenshots are in `docs/screenshots/`.

API coverage includes case isolation, family permissions and revocation, concurrent idempotent submission, exact-document consent, encrypted document round-trip, single-use download tickets, invalid file rejection, tasks, mentions, search, discovery confirmation, PDF/ZIP export, audit events and asynchronous Kafka notifications.

The Docker build skips the Testcontainers test because the build container has no Docker socket. The final host Maven run executes that test successfully against Docker; all 23 tests have zero skips.

These results establish the tested local demonstration paths, not production certification or exhaustive accessibility/security coverage. Providers are simulated or manually handled; no external organization is contacted. Live adapters, real malware scanning, OCR, full translations, Unicode PDF fonts, retention/erasure operations and production operational readiness remain documented in README.md and ROADMAP.md.

## Post-demo hardening verification

- Revoked access sessions rejected after logout, reset and refresh rotation through real API requests. Client regression tests ensure expired bearer tokens do not interfere with cookie-based logout.
- ClamAV protocol tests cover clean response, malware response, daemon error, truncated response, unavailable daemon and blocked writes bounded by a whole-operation deadline. These use a controlled socket peer; actual virus-signature operation still needs deployment verification.
- Five Flyway migrations pass on real PostgreSQL. Runtime-role tests confirm ordinary reads/audit inserts work while schema changes, history erasure and migration-table writes fail.
- Staging Compose template validates with all non-gateway ports removed. Caddy validates the HTTPS/IP-allowlist configuration in its official container without publishing a port or requesting a certificate.
- The deployment readiness checker correctly rejects missing domain, tester IP and SMTP settings. Generated `.env.staging` has mode 0600 and is ignored by Git; secrets were not printed.
- Local logical database backup restored into a disposable PostgreSQL container with no network/host ports. Both encrypted database and object archives were created, their hashes and decryption verified, object archive members read successfully, and the decrypted database restored. Actual restored MinIO API reads and vault-key recovery remain untested.
- Age v1.3.2 was used from temporary storage after verifying the official release archive checksum. Test-only backup identity and archives live under ignored `backups/`; no remote backup or paid resource was created.
- Browser scan found and fixed account reassurance/family-badge contrast; account marketing heading changed to H2 so the form remains the main H1.

Prepared operational scripts have Python syntax checks. The GitHub workflow now includes staging structure validation and the database restore drill. Full production readiness remains open in ROADMAP.md and PROGRESS.md.
