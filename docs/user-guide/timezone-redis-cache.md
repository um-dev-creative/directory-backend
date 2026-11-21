# Timezone list cache (user guide)

The list of time zones (`GET /api/v1/timezones`) rarely changes, so Directory keeps a copy in **Redis** and answers from it instead of querying PostgreSQL on every request.
Clients of the API see no change: same URL, same response.

## What to expect

| Situation | Result |
| --- | --- |
| First request after start or after the cache expired | Read from PostgreSQL, then stored in Redis |
| Later requests (within 24 hours) | Answered from Redis, no database query |
| Redis is down or unreachable | Answered from PostgreSQL; a `WARN` line `timezone.cache.unavailable` is logged. The endpoint keeps working, only slower |
| Several instances start at once | One of them loads the data; the others read the database directly for that moment |

## Set-up for operators

1. Make a Redis instance reachable from Directory. Use the standard Spring settings (`spring.data.redis.host`, `port`, `password`, `ssl.enabled`), supplied through the environment or Vault. Never commit them.
2. Nothing else is needed. The cache key is `timezones:all:v1` and it expires after 24 hours.
3. To force a refresh, delete the key `timezones:all:v1` in Redis; the next request reloads it.
