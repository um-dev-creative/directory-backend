# Search without a result limit (F4-E)

`GET /api/v1/businesses/search` keeps the same parameters and the same answer shape. What changes is that `open_now=true` and `sort=distance` no longer look only at the first 1000 businesses.

## What you get

- `open_now=true` returns every business that is open now, wherever it is in the alphabet. `total_count` is the exact number of businesses open now that match your other filters, and `total_pages` follows from it.
- `sort=distance` orders every matching business by distance (nearest first, then name; businesses without coordinates go last) and pages through all of them.
- Both together work too: the open businesses, nearest first.
- Requests without these two parameters behave as before.

## Notes

- "Open now" uses the time zone of each business, as before. A business without a time zone uses `America/Toronto`.
- The search no longer writes your text, your city or your position to the server logs; only counts and yes/no flags are logged.
- Locations with a latitude outside -90..90 or a longitude outside -180..180 are now also rejected by the database.
