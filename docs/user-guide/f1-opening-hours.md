# Opening hours per location (user guide)

Each location (sede) of your business can publish its opening hours, so people see "Open until 21:00" or "Closes in 30 min".

## Set the hours (owner only)
`PUT /api/v1/businesses/{businessId}/locations/{locationId}/hours` replaces the whole schedule. Only the owner of the business can do it.

```json
{
  "slots": [
    { "day_of_week": 1, "opens_at": "09:00", "closes_at": "13:00", "closed": false },
    { "day_of_week": 1, "opens_at": "15:00", "closes_at": "21:00", "closed": false },
    { "day_of_week": 7, "closed": true }
  ]
}
```

- `day_of_week`: 1 = Monday ... 7 = Sunday.
- Times are `HH:mm`, local time of your business. The business time zone is used; if it has none, `America/Toronto`.
- A day can have up to 4 slots (for example a lunch break). Slots of the same day cannot overlap.
- A closed day is one slot with `"closed": true` and no times. Days without slots are also shown as closed.
- Past midnight: use a slot until `23:59` and another from `00:00` on the next day. They are read as one continuous opening.
- Sending an empty list removes the hours.

If something is wrong you get `400` and the reason in the `message-error` header. `401` means no session, `403` that you are not the owner, `404` that the business or location does not exist.

## Read the hours and the status (public)
- `GET .../hours` returns the slots and the time zone.
- `GET .../hours/status` returns whether the location is open now and the next change:

```json
{ "open_now": true, "closes_at": "2026-11-23T21:00-05:00", "timezone": "America/Toronto" }
```

When closed, `opens_at` is returned instead of `closes_at`. Add `?at=2026-11-23T15:00:00Z` to ask about another instant.
