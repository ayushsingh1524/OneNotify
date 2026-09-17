# Recruiter demo guide

Start `docker compose up --build`, open http://localhost:3000, and sign in with the README credentials. Allow a few seconds for asynchronous notification delivery.

## Five-minute story

1. Show the calm landing page and the case dashboard. Explain that it tracks work; it is not a chatbot and does not claim to represent a family legally.
2. Create a case through the five-step wizard. Use fictional information. Demonstrate **Save for later** and return if desired.
3. Upload `scripts/fixtures/sample.png` twice with separate categories: Death certificate and User identity. These are visibly sample bytes, not real certificates.
4. Add SBI through Find accounts. Open it and prepare the request. Removing/missing a required category produces Documents pending instead of a fake success.
5. Review the sharing statement, choose the exact files and approve simulated submission. Explain the idempotency key and transactional outbox.
6. Simulate three responses: acknowledged, under review, then an additional nominee document request. The timeline and correspondence reflect each step.
7. Upload the same sample file as Nominee document. Reprepare and approve again. Simulate under review, approved, then completed.
8. Show the dashboard completion count, search, family task assignment and immutable timeline.
9. Export the case PDF and provider ZIP package. Explain that the package contains a draft and originals, not invented official forms.
10. Open demo administration for registry configuration, Kafka event counts, failed-job replay and aggregate metrics.

## Family permissions

The seeded contributor account can add documents and notes but cannot approve submissions or invite others. A VIEWER can read only. A separate newly registered family has no access to the seeded case. The automated API suite checks these restrictions, including a download ticket issued before membership revocation.

## Local email

Forgot-password messages appear only in Mailpit at http://localhost:8025. They contain a single-use 30-minute reset link. No institution receives email. Outside demo mode registration requires email verification; local demo accounts bypass verification for convenience.

## What to say accurately

- “These are simulated provider rules and responses.”
- “Kafka is actually running; notifications are consumed idempotently.”
- “Files are encrypted before reaching MinIO.”
- “Image OCR and Gmail are extension points; the demo reads embedded PDF text and sample messages.”
- “This has a tested local lifecycle; production adoption needs the documented security and operations work.”

Screenshots in `docs/screenshots` are generated from browser tests. `docs/PROGRESS.md` records actual verification rather than treating file creation as proof.
