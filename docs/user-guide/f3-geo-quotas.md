# Request limits for geocoding and map searches

To keep the service fast for everyone and to control the cost of the external address lookup, three operations have their own, stricter limits (per minute):

| Operation | Default limit per minute |
|---|---|
| Address lookup (`POST /api/v1/geocode`) | 10 |
| Businesses on a map area (`GET /api/v1/businesses/map`) | 60 |
| Businesses near a point (`GET /api/v1/businesses/nearby`) | 60 |

## How it works for you

- If you are signed in, the limit is counted for your account, no matter from which device or network you connect.
- If you are not identified, the limit is counted for your network address.
- When you exceed the limit the service answers `429 Too Many Requests`. The `Retry-After` header tells you how many seconds to wait, and the `message` header carries a readable explanation. The request is not processed, so nothing is looked up or charged.
- The general limits per network address and per user keep applying as before.

## Tips

- Wait the number of seconds in `Retry-After` before trying again.
- Do not call the address lookup on every keystroke; call it when the user submits the address.
- Move the map and then request the area once, instead of on every small movement.
