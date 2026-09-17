# Operations and recovery

Status: local database restore drill and encrypted backup/decryption drill verified; full staging object/API recovery and scheduled off-host backups pending environment selection.

## Backup

Choose an operator and an independent backup location. Keep the vault key and backup private key in a password manager or secret manager, separate from backups. Losing the vault key makes stored documents unreadable. A database dump contains sensitive profiles even though vault objects are encrypted.

`scripts/backup-encrypted.py` requires the standard `age` CLI and an `AGE_RECIPIENT` public key. It stops backend/MinIO briefly, streams the PostgreSQL dump and stopped MinIO data volume through encryption, writes a SHA-256 manifest and restarts services in `finally`. Use a maintenance window. It does not copy secrets, create remote storage, or schedule itself. Files use private permissions. Only a directory with a completed manifest is a completed backup. The local synthetic-data drill used an official age v1.3.2 binary whose archive checksum matched the upstream release. Encrypted database/object archives were produced successfully. The test identity is in `backups/local-drill-identity.txt` (ignored by Git); it is deliberately a LOCAL TEST key. Production backup identities must be stored separately from backups. See the [official age project](https://github.com/FiloSottile/age) for installation.

For staging, run from the repository with `COMPOSE_FILE=compose.yaml:compose.staging.yaml`, `COMPOSE_PROJECT_NAME=onenotify-staging` and `COMPOSE_ENV_FILES=.env.staging` in the process environment. Set `AGE_RECIPIENT` separately. Do not write the private age identity into source control. Upload completed encrypted archives to the chosen off-host location; alert on missed backups.

## Recovery drill

`python3 scripts/restore-drill.py` reads the local demo database, restores it into a new PostgreSQL container with no network or host ports and a temporary filesystem, checks migrations/tables/audit triggers, and destroys only that disposable container. It never drops or restores over the running database. This drill passed on 17 September 2026. With `--encrypted-backup DIRECTORY --identity KEY_FILE`, it also verifies the manifest, decrypts both archives, reads all object archive members and restores the decrypted database. This additional path is intended for small test backups and uses memory; production-sized recovery should stream data through the runbook below.

Full recovery must use a NEW isolated project on a separate host:

1. Verify the archive manifest. Decrypt with the separately held backup key into protected temporary storage, or pipe to restore tools.
2. Provision the same reviewed image versions and empty database/object volumes. Keep backend workers stopped and outbound network access disabled.
3. Restore the PostgreSQL dump with `pg_restore --no-owner --no-acl --exit-on-error`; provision runtime-role grants with the reviewed migration procedure.
4. Restore the MinIO archive into its stopped, empty data volume. Supply the matching vault key through the secret manager.
5. Invalidate all restored refresh tokens; clear Redis download tickets. Audit a recovery event after bringing the application up.
6. Verify case counts, audit history, original-document checksums, authenticated downloads, authorization and provider workflow states. Reconcile pending outbox events before enabling external delivery; never blindly replay real submissions.
7. Record measured recovery time and backup age. Proposed initial targets: 24-hour recovery point, 4-hour recovery time. These targets are not achieved until a full timed drill passes.

## Monitoring and incident response

Assign a named owner and escalation contact before shared deployment. Monitor HTTPS availability, failed login/rate-limit trends, disk/memory, database connectivity, ClamAV health/signature freshness, outbox failures, consumer lag and backup age. Existing health/metrics/outbox views supply inputs; external alert routing remains unconfigured.

If a credential is exposed: restrict ingress, revoke affected sessions, preserve audit evidence, rotate the exposed secret and examine access logs. Rotate signing secrets independently; do not change the vault key without a document re-encryption plan. Never include document contents or reset links in tickets. A reviewer must determine notification obligations for the actual incident and jurisdiction.

## Retention and deletion

Deletion requests currently stop documents appearing in new packages; they are not physical erasure. Before enabling automated deletion, approve retention durations, legal-hold handling, family authority checks, audit minimization and backup-expiry rules with an appropriate reviewer. Record request/review/outcome separately. Implement erasure in a retryable worker only after these decisions; do not delete historical consent/audit evidence ad hoc. No retention periods have been invented here.
