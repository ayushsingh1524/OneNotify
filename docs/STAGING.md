# Private staging deployment

Prepared on 17 September 2026. No cloud resources have been purchased or deployed. This configuration is for synthetic-data evaluation; it is not a production approval.

## What is prepared

- A separate Compose project and fresh volumes, demo seed disabled.
- Caddy HTTPS gateway; only ports 80/443 published. Tester public-IP allowlist.
- PostgreSQL, Redis, Kafka, MinIO and ClamAV available only inside Docker networking.
- Distinct PostgreSQL runtime and migration credentials. Runtime cannot alter schema, modify migration records or erase audit history.
- Secure cookies, unique generated application secrets, authenticated STARTTLS SMTP and configurable sender.
- Real clamd INSTREAM adapter; unavailable/error/infected/incomplete responses reject upload before persistence.

ClamAV's official Docker guidance recommends 4 GB for the scanner alone. Start evaluating the entire stack on an 8 GB host; 16 GB provides more room for builds and signatures. This is an engineering estimate, not a measured capacity guarantee. [ClamAV Docker guidance](https://docs.clamav.net/manual/Installing/Docker.html)

## Before deploying

1. Choose a host/region and register a domain you control. Configure its staging DNS record to the server IP.
2. Install Docker with Compose 2.24.4 or later. Restrict SSH to your IP; allow 80/443. Do not publish infrastructure ports.
3. Copy source to the host. Run `python3 scripts/staging-env.py` there once, or securely transfer the generated private file. It refuses to overwrite existing secrets.
4. Fill `.env.staging` with domain, tester IP/CIDR, ACME contact and SMTP credentials. Configure the provider's sender/domain verification and SPF/DKIM/DMARC before sending mail. Do not paste secrets into chat or commit them.
5. Run `python3 scripts/check-staging.py`. It does not print secrets. The example template intentionally fails readiness checks.
6. Use a new project; never mount demo database volumes into staging. Role initialization runs only on an empty database volume.

```sh
docker compose --env-file .env.staging -p onenotify-staging -f compose.yaml -f compose.staging.yaml up -d --build --wait
```

After deployment verify: HTTPS and cookie flags, unapproved IP rejection, email registration/verification/reset delivery, clean upload, EICAR rejection, scanner outage rejection, revoked-session rejection, manual request consent and downloaded package. Verify the first fresh ClamAV signature download finishes; no clean result means no accepted upload.

No admin account is seeded outside demo mode. Register and verify the operator account normally. A trusted operator can assign its `system_role='ADMIN'` through a controlled database maintenance session after confirming the exact account; document the change. Do not expose an unauthenticated bootstrap endpoint.

No provider catalog is seeded outside demo mode either. Add only reviewed manual provider entries in administration; the pilot pack describes the review criteria. Do not copy demo rules into a live catalog and mark them verified.

## Limits before real data

Migration credentials remain available to the backend startup process; a separate deployment migration job and secret removal are still needed. MinIO currently uses its root storage credential; create a bucket-restricted runtime policy before production. Vault encryption still uses one injected key, not managed envelope encryption/rotation. Real malware detection complements but does not replace strict parsing and resource-isolated document processing. SMTP delivery, certificate issuance, ClamAV signatures, off-host backup, alerts and actual host recovery must be verified on the chosen environment.

See [operations](OPERATIONS.md), [hosting choices](HOSTING_OPTIONS.md), [provider pilot](PROVIDER_PILOT.md) and [roadmap](ROADMAP.md).

Compose removal of development ports follows the official [merge/reset semantics](https://docs.docker.com/reference/compose-file/merge/). The scanner implements the official [ClamD protocol](https://docs.clamav.net/manual/Usage/ClamdProtocol.html).
