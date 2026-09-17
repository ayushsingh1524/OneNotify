# OneNotify implementation tracker

Updated: 17 September 2026. Current phase: local testing release and GitHub handoff. Hosting is explicitly deferred. Estimated remaining work for repository packaging and verification: 15–25 minutes; GitHub authentication may affect publishing.

`Written` means implementation exists. `Verified` requires a successful relevant check.

| Phase | Deliverable | Status |
|---|---|---|
| 1 | Repository and infrastructure | Complete; all seven Compose services healthy |
| 2 | Spring Boot foundation | Compiled on host and Java 21 container |
| 3 | Authentication and security | Verified by lifecycle and permission API checks |
| 4 | Database and Flyway | Five migrations verified with real PostgreSQL/Testcontainers |
| 5 | Case management | API and browser lifecycle verified |
| 6 | Family collaboration | Invitations, permissions, revocation and mentions verified by API |
| 7 | Encrypted document vault | Crypto/ticket/permission/upload tests pass |
| 8 | Provider registry | Seeded with twelve demo providers |
| 9 | Workflow state machine | Unit, API and browser lifecycle tests pass |
| 10 | Tasks and deadlines | Implemented; task API and scheduler regression checks pass |
| 11 | Provider adapters | Manual and simulated lifecycle checks pass; no live external submission |
| 12 | Kafka events | Real broker delivery and notification checks pass |
| 13 | Notifications | Kafka-driven and local reset-email checks pass |
| 14 | AI abstraction | Implemented deterministic demo assistance; live AI is not configured |
| 15 | Document/email discovery | Written; mock assistance and embedded PDF text |
| 16–24 | Frontend, onboarding, dashboards, vault, family, timeline, admin, search | Implemented; production build and browser regression pass |
| 25 | PDF/package exports | API and browser package download verified |
| 26–28 | Tests, security hardening, observability | Unit/security/API/browser checks pass on full container stack |
| 29 | Demo seed and complete lifecycle | Complete for local demonstration; final checks pass |
| 30 | Documentation | Complete; verification evidence and production gaps documented |
| 31 | Dockerized integration test | Complete for local demonstration; final checks pass |
| 32 | Final polish | Complete for local demonstration; final checks pass |

## Original demo verification log

- Docker daemon reachable (29.7.2).
- Local Java, Maven, Node and npm available.
- First compiler check identified an annotation name collision; fixed.
- 56 end-to-end API checks passed, including concurrent duplicate submissions, permission restrictions, crypto round-trip, exports and Kafka notifications.
- 16 backend tests passed, including real PostgreSQL migration/append-only checks.
- 4 frontend unit tests passed.
- Production Next.js build passed with Webpack.
- Frontend dependency audit: zero reported vulnerabilities.
- Desktop dashboard and mobile vault navigation passed.
- Browser regression caught premature wizard submission and repeated file selection; both fixed. Complete browser lifecycle now passes.
- Java 21 backend Docker image built successfully.
- Manual provider lifecycle and automatic case completion passed.
- Password reset email, refresh rotation, replay rejection and cross-site request checks passed.
- Accessibility contrast fixes verified: no violations in the automated WCAG A/AA scans of the landing page and case dashboard. This is not a whole-product accessibility certification.
- Final frontend/backend images built; all seven Compose services healthy.
- Three Playwright tests pass against the container frontend, including document preview, mobile navigation and complete simulated submission/resubmission.
- Formatting, lint and TypeScript checks pass. See VERIFICATION.md for commands and coverage.

## Task flow

Backend and persistence → secure lifecycle APIs → frontend journeys → seed and infrastructure → build → integration/security tests → browser end-to-end test → fix failures → documentation and final status.

### Final container check

Resolved the cross-platform npm lockfile issue using the Node 22 container toolchain. Both application images build successfully. The final Docker frontend and backend passed the browser and API suites on 17 September 2026. A preview test selector was narrowed to the named document image to distinguish it from the close-button icon.

Open http://localhost:3000. Demo account: `family@example.test` / `Family-demo-2026!`.

## Post-demo task flow

| Step | Work | Status | Approximate time |
|---|---|---|---|
| A | Automated walkthrough and regression review | Complete; 5 browser tests pass. Human family sessions pending | Complete locally |
| B | Revocable access tokens, ClamAV adapter, authenticated SMTP | Implemented and tested; actual scanner signatures and external mail need staging verification | Complete locally |
| C | HTTPS staging configuration, database roles and recovery tooling | Config validated; encrypted local backup and DB restore pass | Complete locally |
| D | Automated regression, recovery drill and evidence | 23 backend + 6 frontend + 5 browser tests; API/security suites pass | Complete locally |
| E | Hosting comparison and first-provider manual pilot pack | Prepared; no outreach or external submission | Complete |
| F | Live deployment, real email/domain, authorized provider integration | Waiting for accounts/access; no purchases authorized | Depends on setup |
| G | Independent security/privacy and human accessibility review | External review needed | Schedule with reviewers |

User has no hosting/domain or provider API access yet. Prepare files and recommendations; do not create paid resources. A manually assisted SBI pilot is a proposal, not a verified integration or partnership.

## What remains before real data

- Select hosting/domain/mail, deploy private synthetic staging and verify actual TLS, mail and ClamAV signatures. Allow approximately 1–2 engineering days once accounts/DNS are ready; third-party verification time varies.
- Isolate migration execution, restrict storage credentials, add strict isolated document parsing and orphan cleanup; implement managed keys and rotation for the chosen platform.
- Decide retention/legal-hold rules, implement reviewed erasure operations, and complete whole-system object/key recovery plus off-host backup/alerts.
- Complete regional-language content/Unicode PDF output and human accessibility testing.
- Obtain independent security/privacy review and retest findings. These are not services an automated local test can certify.
- Confirm a specific provider scenario and authorized submission channel. The SBI pack is a proposed manual pilot, not an API integration.

Remaining engineering and review should be planned in weeks, not as a few-minute deployment. Exact estimates depend on hosting, identity-assurance scope, retention decisions and reviewer findings.

Prepared handoffs: [staging](STAGING.md), [hosting options](HOSTING_OPTIONS.md), [operations](OPERATIONS.md), [provider pilot](PROVIDER_PILOT.md), [review pack](REVIEW_PACK.md).

## Local testing release / GitHub handoff

| Step | Status |
|---|---|
| Confirm target repo and preserve history | Target has no refs; local main initialized |
| Reproducible setup and local test guide | Prepared |
| Exclude secrets, backups, keys and generated caches | In progress; index audit required before commit |
| Final unit/build/API/browser checks | In progress |
| Commit and upload to ayushsingh1524/OneNotify | Pending verification and available authentication |
| Hosting / paid infrastructure | Deferred by owner; not part of this milestone |
