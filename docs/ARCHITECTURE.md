# Architecture

OneNotify is a modular monolith, not a fleet of microservices. One Spring Boot process coordinates one PostgreSQL transaction per business command. Next.js proxies same-origin API calls and holds access tokens only in memory; refresh tokens live in HttpOnly cookies.

## Boundaries

| Module | Owns |
|---|---|
| auth | Password hashing, tokens, verification/reset email, identity and case authorization |
| users | Current-user profile projection |
| cases | Bereavement case/profile, server-side onboarding drafts |
| family | Membership and revocation |
| providers | Registry, requirement snapshots, organization assignments |
| workflow | State graph, aggregate locking, transitions, consent-gated submission |
| documents | Original bytes, encryption, object storage, metadata, temporary access |
| consent | Scope, purpose, selected documents, expiration and consumption |
| integrations | Provider adapter contracts, email-import seam, outbox and Kafka configuration |
| tasks | Family work and deadline scheduler |
| correspondence | Notes, recorded emails/letters, references, attachments and mentions |
| audit | Append-only record of sensitive actions |
| notifications | Idempotent domain event consumer, in-app and mock outbound channels |
| ai | Replaceable assistance interface and deterministic mock |
| search | Authorized case-scoped projections |
| export | PDF summaries and organization ZIP packages |
| admin | Demo registry, configuration drafts, failed-event operations and aggregates |

`common/Db` is a JDBC repository gateway, not a controller dependency. Workflow is a JPA entity with optimistic versioning and a pessimistic write lock for commands. API responses are DTOs/maps, never JPA entities. Immutable record DTOs validate inbound commands.

## Transaction and event boundary

A successful command writes its business state, audit entry and outbox event atomically. An outbox publisher uses `FOR UPDATE SKIP LOCKED`, sends a versioned event to Kafka, waits for broker acknowledgement, then marks it published. A process crash between send and marking can duplicate a delivery; the notification consumer inserts `(consumer,event_id)` in the same transaction as notifications. This makes its database effects idempotent.

Producer failures retry up to ten attempts before being available in admin for replay. Consumers retry three times with a fixed delay, then publish to `.DLT` topics. Current outbound notifications are log-only mocks; a real outbound delivery service would need its own delivery outbox rather than relying on transactional rollback of a network call.

## Storage and failure behavior

Documents are encrypted with random-nonce AES-GCM and case/document associated data before MinIO upload. Object keys are random identifiers, not filenames. A storage write and a relational transaction cannot be atomically committed together; a failed database commit can leave an orphan ciphertext object. Add an age-gated orphan sweep before production. Never delete an object merely because a request timed out.

Redis supports rate limiting and single-use 60-second vault tickets. Missing Redis fails closed for protected API rate checks. Durable submission idempotency and workflow locking use PostgreSQL, avoiding a distributed lock/database split-brain boundary.

## Deployment

Compose contains frontend, backend, PostgreSQL, Redis, Kafka, MinIO and Mailpit. All public local ports bind to loopback. App containers run as unprivileged users. No Kubernetes is required. Production needs TLS, managed secret injection, encrypted backups, authenticated Kafka/Redis, network isolation and a tested restore process.

## Post-demo deployment preparation

`compose.staging.yaml` overlays private ports, Caddy HTTPS/tester-IP restrictions, ClamAV, non-demo authentication, verified-sender SMTP configuration and separate runtime/migration database credentials. This is prepared configuration, not a live deployment. Access JWTs carry a database-backed revocable session ID. Runtime role restrictions are applied by migration V5. See STAGING.md and VERIFICATION.md for evidence and remaining boundaries.
