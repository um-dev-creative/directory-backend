# Task: Optimize Timezone Query Performance Using Redis Cache

## Overview

Short summary

Implement a robust, production-ready Redis caching layer for the application's timezone list so that frequent requests for timezones are served with minimal latency and without repeatedly hitting PostgreSQL. The work includes design, implementation guidance for Java/Spring Boot (compatible with Redis Cloud), unit tests to maintain or improve coverage, documentation, and a small rollout plan.

## Background

The application currently reads the canonical timezone list from PostgreSQL on every request (or on-demand). Timezones are relatively static and frequently used to populate dropdowns and UI selectors. Repeated DB hits are unnecessary and add latency and load to PostgreSQL.

## Goals

- Serve timezone list from an in-memory cache (Redis) for all read requests.
- Keep cache correctness when timezone data changes in the database.
- Provide configuration that works locally (embedded or local Redis) and in cloud environments (Redis Cloud / managed Redis).
- Add unit tests that exercise the caching logic and keep or increase repository test coverage.

## Non-goals

- Replace PostgreSQL as the source of truth. Postgres remains the canonical storage for timezone data.
- Implement full multi-region Redis clustering or data migration — use managed Redis/Redis Cloud recommended configuration.

## Proposed Design

1. Cache key
   - Use a single well-known Redis key for the timezone list, e.g. `timezones:all:v1`.
   - Optionally include a version suffix (`v1`) to allow simple cache-busting on incompatible format changes.

2. Cache value format
   - Store JSON array of timezone DTOs (compact representation) as a single Redis string value.
   - Example value: `[ {"id":1,"zone":"America/New_York","display":"(UTC-05:00) Eastern Time"}, ... ]`

3. Cache lifecycle
   - On first request (cache miss): load from PostgreSQL, serialize to JSON, set in Redis with configurable TTL (recommended: 24h) or without TTL if invalidation is always performed on update.
   - On cache hit: deserialize and return.
   - On update to timezone data (rare operation): invalidate or refresh the cache immediately after the DB transaction commits.

4. Invalidation strategies
   - Active invalidation: When timezone rows are created/updated/deleted, the service that performs the write must invalidate (`DEL timezones:all:v1`) or push a refreshed value to Redis.
   - Passive TTL fallback: Keep a long TTL (e.g. 24 hours) so stale data is self-corrected even if invalidation fails.
   - Event-driven: If an event bus exists, publish a `timezone.updated` event and have a listener refresh the cache across instances.

5. Concurrency and race conditions
   - Use a cache stampede protection pattern: on cache miss, attempt to obtain a short-lived lock (e.g., `SETNX timezones:lock 1 PX 5000`) to ensure only one instance loads from DB and populates the cache. Others can wait briefly and then re-check the cache.

6. Observability
   - Emit metrics for cache hit/miss counts and latency.
   - Log cache populate and invalidation events at INFO level with correlation id.

## Implementation (Java / Spring Boot guidance)

- Use `spring-boot-starter-data-redis` with Lettuce (default) or Jedis client. Ensure compatibility with Redis Cloud connection strings.
- Create a `TimezoneCacheService` that encapsulates Redis interactions.
- Keep `TimezoneService` (existing service that reads from repository) unchanged except to delegate reads to `TimezoneCacheService`.

Suggested methods on `TimezoneCacheService`:
- `List<TimezoneDto> getAllTimezones()` - returns cached or DB-fetched list
- `void refreshTimezones()` - force reload from DB and write to cache
- `void invalidateTimezones()` - delete cached key

Example flow in `getAllTimezones()`:
- Try to read JSON string from Redis key `timezones:all:v1`
- If present: deserialize and return
- If absent: attempt to acquire a short lock
  - If lock acquired: load from DB, write to Redis, release lock, return
  - If lock not acquired: wait a short time and retry reading Redis (configurable backoff), eventually fallback to reading DB directly if necessary

## Configuration

Add properties (example `application.yml` keys):

redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}
  ssl: ${REDIS_SSL:false}
  cache:
    timezone-key: timezones:all:v1
    ttl-seconds: ${TIMEZONE_CACHE_TTL_SECONDS:86400}
    lock-key: timezones:lock
    lock-ttl-ms: ${TIMEZONE_LOCK_TTL_MS:5000}

When running against Redis Cloud, provide the managed instance connection URI via environment variables and enable SSL if required.

## Unit tests

- Tests should mock Redis interactions to exercise both cache-hit and cache-miss code paths.
- Tests should cover:
  - Cache hit returns cached value and does not call repository
  - Cache miss triggers DB load and populates Redis
  - refreshTimezones pushes the latest value to Redis
  - invalidateTimezones deletes the key
  - Lock behavior: when lock cannot be acquired, fallback and retry logic is exercised (use shorter TTLs in tests)
- Use Mockito and Spring Boot test slice patterns. Keep tests focused and deterministic.
- Ensure tests preserve or increase overall coverage. Add targeted unit tests to the service layer; avoid heavy integration tests unless already part of CI.

## Acceptance Criteria (detailed)

- AC-1: On first request (cache empty) the service fetches timezone list from PostgreSQL and writes it to Redis under key `timezones:all:v1`.
  - Evidence: Unit test asserting repository is called once and Redis `SET` is invoked with expected JSON.

- AC-2: On subsequent requests the service returns the list directly from Redis (no repository call).
  - Evidence: Unit test asserting repository is not called and Redis `GET` returns cached JSON.

- AC-3: When timezone data is updated via the write path, the cache is invalidated or refreshed immediately after the DB commit.
  - Evidence: Unit test or small integration test shows `invalidateTimezones()` is invoked after repository `save`/`update` and subsequent reads read updated value.

- AC-4: The caching works in local environment with local Redis and in cloud with Redis Cloud connection settings.
  - Evidence: Documentation and configuration examples added; environment variables documented.

- AC-5: Unit tests are added such that the repository-level or service-level coverage is maintained or increased. The new tests should not decrease global coverage below current CI gate.

- AC-6: No breaking changes to existing API contracts or database schema.
  - Evidence: Code changes limited to service layer and new cache service; API interfaces unchanged.

## Definition of Done

- Code implementing `TimezoneCacheService` and integration points is committed in a dedicated feature branch.
- Unit tests for cache behavior are included and pass locally.
- All existing unit tests pass.
- Coverage is maintained or improved (verify with `mvn test` and JaCoCo report).
- README or docs updated with the caching approach and required environment variables.
- A short rollout plan exists and is documented (see below).

## Tasks (checklist)

- [ ] Add configuration properties for Redis cache and lock
- [ ] Implement `TimezoneCacheService` with get/refresh/invalidate methods
- [ ] Update existing `TimezoneService` to delegate reads to cache service
- [ ] Add unit tests for cache-hit, cache-miss, refresh, invalidate and lock behavior
- [ ] Add documentation section describing how to configure Redis (local and Redis Cloud)
- [ ] Run `mvn test` and confirm coverage
- [ ] Create PR and include migration/rollback notes

## Files likely to change

- src/main/java/.../service/TimezoneService.java (or equivalent service)
- src/main/java/.../service/TimezoneCacheService.java (new)
- src/main/java/.../api/v1/to/TimezoneDto.java (if needed for DTO serialization)
- src/test/java/.../service/TimezoneCacheServiceTest.java
- src/test/java/.../service/TimezoneServiceTest.java (update/cover delegation)
- src/main/resources/application.yml (configuration examples)
- docs/ or README.md (documentation addition)

## Rollout and Validation

1. Deploy to staging with Redis (or local Redis) enabled.
2. Smoke test timezone endpoint and verify cache-populate log entry.
3. Issue multiple repeated requests and monitor Redis hit/miss metrics.
4. Update a timezone row, verify that `invalidateTimezones` is triggered and subsequent reads return updated values.

## Monitoring and Metrics

- Expose metrics for `timezone.cache.hit` and `timezone.cache.miss` (Prometheus friendly)
- Add logs for cache population and invalidation events

## Risks

- If invalidation is not executed after a write, UI may see stale timezone labels for up to TTL duration — mitigate by short TTL or mandatory invalidate on write path.
- Redis outages: implement fallback to DB read with graceful degradation and increase DB connection pool for short bursts.

## Estimated effort

- Implementation + unit tests + docs: 1-2 days (Small/Medium)

## Example Redis commands (for local verification)

```bash
# view cached key
redis-cli GET "timezones:all:v1"

# force delete
redis-cli DEL "timezones:all:v1"
```

## How to test locally (quick steps)

1. Start local Redis: `redis-server` (or use Docker)

```bash
# using Docker
docker run --rm -p 6379:6379 redis:7
```

2. Run unit tests

```bash
mvn -DskipITs test
```

3. Run the app locally and hit the timezone endpoint twice; first call populates Redis, second call should be served from Redis.

```bash
mvn -DskipTests package
java -jar target/directory-backend.jar
# then curl the endpoint
curl -v http://localhost:8080/api/v1/timezones
```


---

If you want, I can also open a follow-up task that implements the `TimezoneCacheService` skeleton and a first set of unit tests following the repository conventions. Indicate which package the service should live in (or I can infer from the project layout).
