# API contract, Phase 4 (user guide)

The OpenAPI file `src/main/resources/api/index.yaml` now describes everything added in Phase 4. This guide says what a client can do and what answers to expect.

## Verify and moderate a business

The owner proves the business e-mail with a code; an administrator approves or rejects it.

| Step | Call | Answer |
| --- | --- | --- |
| Ask for a code | `POST /api/v1/businesses/{businessId}/verification/request` | `202`; the 9-digit code arrives by e-mail (one per minute) |
| Confirm it | `POST /api/v1/businesses/{businessId}/verification/confirm` with `{"code": "<code>"}` | `204`; wrong code `400`, expired `410`, no pending code `409`, five wrong tries `429` |
| See the status | `GET /api/v1/businesses/{businessId}/moderation` | `{"businessId", "status": PENDING/APPROVED/REJECTED, "verified"}` |
| Approve / reject (admin) | `POST .../moderation/approve`, `POST .../moderation/reject` | `200` with the status; `403` when you are not an administrator |

All of them need the `session-token` header (`401` without it). When moderation is on, a business that is not approved is visible only to its owner: `GET /businesses/{businessId}` and `GET /businesses/user/{userId}` take an optional `session-token` for that.

`PATCH /businesses/{businessId}` answers `403` when the `id` in the body belongs to another user (a business cannot be given away).

## Language

Send `Accept-Language: en`, `es` or `fr-CA` (`fr` is treated as `fr-CA`; anything else is `en`). Every response has `Content-Language`, and the `message` and `message-error` headers come in that language. Category names are translated too; each category has a `locale` field.

## Login and registration

- `POST /auth/session-token`: `401` if the Backbone token (`session-token-bkd`) is not valid.
- `POST /auth/token` (legacy) may answer `403` when the service has disabled it.
- Registration and login can answer `503` (or `429` with `Retry-After`) when the e-mail service is not available or is throttling. Wait and retry; do not loop.

## Other notes

- Browsers: a cross-origin request needs an allowed origin, otherwise the answer is `403`.
- Search: `open_now=true` and `sort=distance` now look at all matching businesses; `total_count` is exact.
