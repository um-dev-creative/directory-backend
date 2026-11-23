# Saved searches and last search (user guide)

As a signed-in consumer you can save a search under a name, list, rename and delete your saved searches, rerun one with a single call, and let the app remember the last search you made. You only ever see your own searches.

All the routes need your `session-token` header. Without a valid one you get `401`. Every error carries its reason in the `message-error` header.

## What a search contains
All fields are optional, but at least one must be present.

| Field | Meaning |
|---|---|
| `q` | text found in the name or the description (max 100 characters) |
| `category_id`, `city_id` | ids (UUID) |
| `city` | city name (used by the plain search) |
| `lat`, `lng`, `radius_m` | a point and a radius in metres (`lat` and `lng` go together; `radius_m` default 5000, max 50000) |
| `bbox` | `minLng,minLat,maxLng,maxLat` of a map area (cannot be combined with `lat`/`lng`) |
| `sort` | `distance` or `name` (only with `lat`/`lng`) |

The values are checked like in `/businesses/nearby` and `/businesses/map`: a bad value answers `400` with the reason.

## Save a search
`POST /api/v1/users/me/saved-searches`

```json
{ "name": "Pizza near home", "params": { "q": "pizza", "lat": 43.65, "lng": -79.38, "radius_m": 2000, "sort": "distance" } }
```

Answers `201` with the saved search (`id`, `name`, `params`, dates). The name is required (max 80 characters) and unique among your searches, ignoring upper/lower case. You can keep at most 20; the 21st answers `409`.

## List, rename, delete
- `GET /api/v1/users/me/saved-searches`: your searches, newest first, and `max` (20).
- `PATCH /api/v1/users/me/saved-searches/{id}` with `{ "name": "New name" }`: renames it (`409` if the name is taken).
- `DELETE /api/v1/users/me/saved-searches/{id}`: deletes it for good (`204`).

An id that does not exist, or that belongs to someone else, answers `404`.

## Replay a search
`GET /api/v1/users/me/saved-searches/{id}/replay` returns what to call and with which parameters:

```json
{ "endpoint": "/api/v1/businesses/nearby", "params": { "q": "pizza", "lat": "43.65", "lng": "-79.38", "radius_m": "2000", "sort": "distance" } }
```

Call that endpoint with a `GET` and those query parameters. A bbox search points to `/api/v1/businesses/map`, a point search to `/api/v1/businesses/nearby`, anything else to `/api/v1/businesses/search`. The saved search also records when you last used it (`last_used_at`).

## Last search
- `PUT /api/v1/users/me/last-search` with the search parameters (the same fields, no name) remembers it. There is one per user: each `PUT` replaces the previous one.
- `GET /api/v1/users/me/last-search` returns it (`params` and `updated_at`), or `404` when you have none yet.
