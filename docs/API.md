# REST API v1

Base path: `/api/v1`. Interactive schema: `/swagger-ui/index.html`; machine schema: `/v3/api-docs`. Sign in and use **Authorize** with the returned access token. Never paste production tokens into third-party tools.

## Authentication

`POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/verify-email`; `GET /auth/me`. Refresh uses the scoped HttpOnly cookie. Access tokens go in `Authorization: Bearer <token>`.

## Case resources

| Method | Path | Purpose |
|---|---|---|
| GET / POST | `/cases` | List authorized cases / create |
| GET / PUT | `/cases/{id}` | Case summary/profile / update |
| GET | `/cases/{id}/timeline` | Public-safe append-only activity projection |
| POST | `/cases/{id}/deletion-request` | Owner requests retention review |
| GET / PUT / DELETE | `/onboarding` | Owner-only saved draft |
| GET / POST | `/cases/{id}/members` | Family members / grant membership or record invitation |
| DELETE | `/cases/{id}/members/{member}` | Revoke nonowner membership |
| GET / POST | `/cases/{id}/providers` | Requests / add organization |
| GET | `/providers` | Demo registry |
| GET | `/provider-cases/{id}` | State, requirements, deadlines, timeline and messages |
| PUT | `/provider-cases/{id}/assignee` | Assign active family editor |
| POST | `/provider-cases/{id}/prepare` | Deterministic requirement check |
| POST | `/consents` | Exact selected-document consent |
| DELETE | `/consents/{id}` | Revoke unused consent |
| POST | `/provider-cases/{id}/submit` | Consent-gated idempotent submission |
| POST | `/provider-cases/{id}/action` | Pause, resume, or cancel |
| POST | `/provider-cases/{id}/record-response` | Family-reported external outcome with reference and note |
| POST | `/provider-cases/{id}/simulate-response` | Demo-only provider advancement |
| GET / POST | `/cases/{id}/documents` | Metadata / multipart upload |
| POST | `/documents/{id}/access` | 60-second single-use ticket |
| GET | `/documents/{id}/content?ticket=…` | Authorized decrypted bytes |
| DELETE | `/documents/{id}` | Request retention review |
| POST | `/documents/{id}/analyze` | Local mock/PDF-text assistance |
| GET / POST | `/cases/{id}/tasks` | Tasks / create |
| PUT | `/tasks/{id}` | Set complete/open |
| GET / POST | `/cases/{id}/correspondence` | Recorded messages/notes |
| GET | `/cases/{id}/discovery` | Possible/confirmed/ignored relationships |
| POST | `/cases/{id}/discovery/sample-email` | Sample message ingestion |
| PUT | `/cases/{id}/discovery/{candidate}` | Confirm or ignore |
| GET | `/search?caseId=…&q=…` | Case-authorized global search |
| GET | `/notifications` | Current member’s notifications |
| PUT | `/notifications/{id}/read` | Mark read |
| GET | `/cases/{id}/export` | PDF summary |
| GET | `/provider-cases/{id}/package` | ZIP cover letter/manifest/original files |
| POST | `/provider-cases/{id}/assist` | Reviewed draft/requirements explanation |

Submission needs an `Idempotency-Key` header (8–120 alphanumeric/underscore/hyphen characters) and `{ "consentId": "uuid" }`. Consent requires `{ "providerCaseId": "uuid", "documentIds": ["uuid"], "approved": true }`. Only owners/admins can approve/submit. Missing requirements return 400; incompatible states and key reuse return 409. Nonmember resources return 404.

Uploads use `multipart/form-data`, a `category` field and `file` part. Accepted MIME: application/pdf, image/png, image/jpeg. Access tickets do not replace bearer authentication.

## Administration

`GET /admin`, `/admin/metrics`; `POST /admin/providers`; `PUT /admin/providers/{id}`; `POST /admin/events/{id}/retry`; `PUT /admin/configuration/{key}`. All require the system admin role; they never grant case document access.

## Errors

```json
{"timestamp":"2026-09-16T00:00:00Z","status":409,"code":"INVALID_TRANSITION","message":"This action is not available at the current step.","correlationId":"server-generated-uuid"}
```

Validation messages are intentionally conservative and do not echo sensitive payloads. Authentication filter errors include status/code/message/correlation ID; controllers include timestamp. Rate limit responses use HTTP 429 and Retry-After. Current lists are bounded for timeline/search/notifications; cursor pagination for very large cases is a production follow-up.
