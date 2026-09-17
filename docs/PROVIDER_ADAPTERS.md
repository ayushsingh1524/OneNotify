# Provider adapters

`ProviderAdapter` exposes mode, request validation, preparation, submission, status check, response parsing, and cancellation hooks. Requests carry a provider-case ID, consent ID, exact immutable document IDs and action. Results contain reference, state, instructions, and a `simulated` flag.

| Mode | Included behavior |
|---|---|
| SIMULATED | Stable `DEMO-<workflow>` reference, simulated submission; explicit controls advance mock responses |
| MANUAL | Produces external-action instructions; no transmission |
| EMAIL | Manual email preparation; no email to an institution is sent |
| FORM | Manual form completion; obtain current official forms directly |
| EXTERNAL_PORTAL | Open provider’s official site and follow its procedure |
| API | Safe manual fallback until a credentialed, authorized adapter is installed |

All seeded rules are demo configurations. Real institution names do not imply partnership, integration, verification or official procedure. Production onboarding requires documented authority, current verified requirements, support/grievance contacts, per-action documents and deadlines, and a verified timestamp.

Provider requirements are snapshotted into a request at creation. Registry edits affect new workflows, not silently rewrite historical requirements. Provider-specific packages include a cover letter, manifest and original required files, not invented official forms. External adapter status reconciliation and inbound authenticated webhooks remain production extensions.
