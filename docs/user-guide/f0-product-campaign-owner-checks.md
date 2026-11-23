# Owner checks on products and campaigns (user guide)

Only the owner of a business can create products for it, link products to it, or create and edit its campaigns. Reading stays public.

## What you need

Send your session token in the `session-token` header on every write request.

| Action | Endpoint | Result for the owner | Other user | No token | Not found |
| --- | --- | --- | --- | --- | --- |
| Create a product | `POST /api/v1/products` | `201` | `403` (you own no business) | `401` | n/a |
| Link a product to a business | `POST /api/v1/products/link` | `201` | `403` | `401` | `404` (business) |
| Create a campaign | `POST /api/v1/campaigns` | `201` | `403` | `401` | `404` (business) |
| Edit a campaign | `PATCH /api/v1/campaigns/{id}` | `202` | `403` | `401` | `404` (campaign or business) |

Public, no token: list campaigns, get a campaign, list the products of a business.

## Good to know

- To move a campaign to another business you must own both businesses.
- Editing still needs the current `lastUpdate` to avoid overwriting someone else's change (`409` on conflict).
- Campaign creation with an unknown business now answers `404` instead of `400`.
