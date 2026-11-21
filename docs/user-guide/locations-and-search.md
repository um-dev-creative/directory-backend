# Locations (sedes) and search by city (user guide)

A business can have one or more **locations** (sedes), each in a city of the catalogue. Visitors can search businesses by city, category and text.

## For the owner of a business
| I want to | Call |
| --- | --- |
| See the cities I can choose | `GET /api/v1/cities` (optional `country=CA`, `q=tor`) |
| Add a location | `POST /api/v1/businesses/{businessId}/locations` with `cityId` and `addressLine` (optional: `name`, `postalCode`, `phone`, `latitude` + `longitude`, `main`) |
| Change a location | `PATCH /api/v1/businesses/{businessId}/locations/{locationId}` with only the fields to change |
| Remove a location | `DELETE /api/v1/businesses/{businessId}/locations/{locationId}` |

Rules:
- The first location is automatically the **main** one. Send `main: true` on another to make it the main one; the previous main stops being it.
- You cannot send `main: false` on the main location; make another one main instead.
- Deleting the main location makes the oldest remaining one the main one.
- Only the owner can add, change or remove; everyone with a session can read them (`GET .../locations`).
- Limits: name 120, address 200, postal code 10, phone 30 characters; latitude -90..90 and longitude -180..180, always sent together.

## For visitors: search
`GET /api/v1/businesses/search` with any of `city_id` (or `city` name), `category_id` (its sub-categories are included), `q` (text in name or description), `page` (from 1) and `per_page` (1 to 100, default 10). The filters are combined; results are ordered by name and each one lists its locations.

## What to expect
| Situation | Result |
| --- | --- |
| Search with no filters | All businesses, first page |
| `page=0` or `per_page=101` | 400, reason in the `message-error` header |
| Business without locations | Appears when no city filter is set; never when a city is set |
| Not the owner | 403; no session token: 401 |
| Unknown city in a location | 400 `City not found` |
