# Data model

UUID primary keys, foreign keys and scoped uniqueness enforce relationships. Flyway owns the schema. Hibernate validates (never creates) its workflow mapping.

```mermaid
erDiagram
    users ||--o{ case_members : participates
    bereavement_cases ||--o{ case_members : authorizes
    bereavement_cases ||--|| deceased_profiles : describes
    bereavement_cases ||--o{ provider_cases : tracks
    providers ||--o{ provider_cases : serves
    providers ||--o{ provider_requirements : defines
    provider_cases ||--|| workflow_instances : owns
    provider_cases ||--o{ workflow_requirements : snapshots
    provider_cases ||--o{ workflow_events : records
    bereavement_cases ||--o{ documents : stores
    documents ||--o{ document_versions : versions
    documents ||--o{ document_access_logs : audits
    provider_cases ||--o{ consents : approves
    provider_cases ||--o{ deadlines : estimates
    provider_cases ||--o{ correspondence : records
    bereavement_cases ||--o{ tasks : coordinates
    bereavement_cases ||--o{ audit_events : records
```

Supporting tables: refresh tokens, reset tokens, verification tokens, onboarding drafts, provider adapter configuration, correspondence attachments, notifications, discovery candidates, AI analyses, outbox events, consumer deduplication, submission keys, admin configuration drafts, and product timing metrics.

JSONB is limited to event envelopes, consent document IDs/shared field lists, analysis outputs, onboarding drafts and configuration. Core relationships remain normalized. A document upload is a distinct immutable file; `version` tracks the sequential category upload number within a case and `document_versions` stores its original storage/checksum record. A production revision UI should group explicit document lineages rather than infer that two unrelated files are replacements.

Indexes cover case membership, case documents, organization workflows, time-ordered history, active deadlines, tasks, notifications and pending outbox delivery. The workflow has an optimistic version column and command-level pessimistic locks. History triggers reject updates/deletes. Sensitive internal storage paths and access/security metadata are omitted from user-facing document lists and reports.
