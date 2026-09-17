# Local testing release

The current milestone is a complete, reproducible local testing build and a clean Git repository. Hosting and real provider integrations are deferred by the owner. No cloud account, domain, paid service or external organization access is needed.

## Start

```sh
git clone https://github.com/ayushsingh1524/OneNotify.git
cd OneNotify
docker compose up -d --build --wait
```

Open http://localhost:3000 and sign in with `family@example.test` / `Family-demo-2026!`. Use fabricated data and sample files. Mail is captured at http://localhost:8025.

## Acceptance checklist

| Journey | Expected result |
|---|---|
| Register / sign in / reset password / sign out | Local mail captures reset link; revoked session cannot access protected APIs |
| Create and resume a case | Five-step wizard stores draft and creates the intended case |
| Upload and preview a sample | Original bytes retained encrypted; authenticated preview works |
| Add SBI and prepare a request | Missing documents shown; exact files require explicit consent |
| Complete simulated lifecycle | Additional document request requires upload and fresh approval; completion updates case |
| Prepare a manual request | UI requires family to submit externally; no false “sent” result |
| Invite/revoke family | Role restrictions enforced; revoked access stops |
| Tasks, correspondence, timeline, search | Changes persist and activity is discoverable |
| Export | PDF summary and ZIP package download |
| Mobile and keyboard use | Navigation, form labels and preview controls usable |

The automated suites cover representative states; use the checklist for exploratory testing as well. File synthetic reproductions using the repository's bug template.

## Automated checks

Prerequisites beyond Docker: Java 21+, Maven 3.9+, Node 22+, Python 3.9+. Install frontend dependencies with `npm --prefix apps/web ci`, then install Chromium from `apps/web` using `npx playwright install chromium`.

- `sh scripts/check.sh`: backend tests/format, frontend format/lint/generated route types/typecheck/unit tests/build.
- `sh scripts/test-local.sh`: starts/rebuilds local Compose, runs API/security/manual suites, Playwright and a disposable DB restore drill. Leaves local services running.
- `python3 scripts/check-repo.py`: checks indexed files for unwanted artifacts/common credentials and locally known environment secrets without printing matches.

Test suites create synthetic records. They do not clean or reset your cases. `docker compose down` stops services and retains volumes. Do not use `down -v` unless intentionally deleting the local test database and vault.

## Test release boundaries

Provider actions are manual or simulated; discovery email is mocked, OCR is not configured, and AI suggestions are deterministic. Hindi covers navigation, not all content; PDF Unicode support remains future work. Deletion records review requests, not physical erasure. These are documented limitations of this test release, not blockers that require hosting now. See ROADMAP.md for future product and production work.

The optional staging files remain in the repository for future use. Do not run them as part of this local acceptance checklist.
