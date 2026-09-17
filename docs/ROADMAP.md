# Roadmap

Updated 17 September 2026. **Current scope: local testing and GitHub handoff. Hosting is deferred by the owner.** None of the hosting, production, or optional extension work below is required to run the local test release. See [testing](TESTING.md) and [progress](PROGRESS.md).

## Prepared after the local demo

- Revocable access sessions, including logout/reset/refresh-rotation checks.
- Real ClamAV stream adapter with fail-closed protocol tests; staging service configuration.
- Authenticated STARTTLS SMTP and configurable sender.
- HTTPS and tester-IP restricted staging configuration with private infrastructure ports.
- Separate database runtime/migration roles and permission tests.
- Local disposable database restore drill; encrypted local database/object backup and decryption verified; full object-service/key recovery and off-host storage pending.
- Hosting comparison and proposed SBI manual pilot pack.

Live staging, external email delivery, real scanner signatures and provider access are not yet verified.

## Before real sensitive data

- Independent security and privacy review; deployment threat model and incident response owner.
- Activate and verify ClamAV on the selected host; add strict parser validation, quarantine policy and orphan-object cleanup.
- Managed envelope encryption and key rotation; isolate migrations from the running application and restrict the storage credential.
- Full object/key recovery, off-host encrypted backup and retention/erasure drills; verified outbound mail.
- Verified provider-specific requirements/actions and official-document provenance.
- Full accessibility assessment with keyboard/screen reader and low-literacy family users.

## Product extensions

- Authorized, credentialed provider adapters with durable delivery outbox, reconciliation and verified webhooks.
- Provider-authenticated receipt verification beyond the included family-reported manual updates.
- Consented Gmail ingestion, real OCR, reviewed structured metadata, and optional replaceable AI models.
- Complete Hindi and regional-language catalogs, Unicode PDF fonts, translations reviewed by native speakers.
- Expiry/revision UI, explicit document lineages, legal deadline source citations, and large-case cursor pagination.
- Apply reviewed admin configuration versions through an approval/release lifecycle rather than ad hoc live graph edits.
- External audit anchoring, richer tracing and SLO alerts; dedicated services only when scaling evidence warrants extraction.

Kubernetes is not required for this project. Modular boundaries, tests and operational readiness take priority over additional deployables.
