# TASK-009: Implement API Rate Limiting

## Priority
**High**

## Status
Open

## Component
- API Gateway / Security
- Package: `com.umdc.directory.security`
- New Component: Rate Limiting Filter/Interceptor

## Description
The API currently has no rate limiting, making it vulnerable to abuse, DoS attacks, and excessive resource consumption. Implement a rate limiting mechanism to protect the API and ensure fair usage.

## Current Behavior
- No rate limiting on any endpoints
- Users can make unlimited requests
- Potential for abuse and service degradation

## Expected Behavior
- Limit requests per user/IP address
- Return HTTP 429 Too Many Requests when limit exceeded
- Include rate limit information in response headers:
  - `X-RateLimit-Limit` - Maximum requests allowed
  - `X-RateLimit-Remaining` - Requests remaining
  - `X-RateLimit-Reset` - Time when limit resets (Unix timestamp)
- Different limits for different endpoint tiers:
  - **Public endpoints** (no auth): 10 requests/minute per IP
  - **Authenticated endpoints**: 100 requests/minute per user
  - **Admin endpoints**: 1000 requests/minute per user

## Proposed Rate Limit Tiers

| Endpoint Type | Rate Limit | Window | Key |
|--------------|------------|--------|-----|
| Public (no auth) | 10 req/min | 1 minute | IP Address |
| User authenticated | 100 req/min | 1 minute | User ID |
| Admin authenticated | 1000 req/min | 1 minute | User ID |
| Create operations | 10 req/min | 1 minute | User ID |
| List/read operations | 100 req/min | 1 minute | User ID |

## Acceptance Criteria
1. **Rate limit enforcement**
   - Given: User exceeds rate limit
   - When: Request is sent
   - Then: HTTP 429 returned with retry-after header

2. **Rate limit headers**
   - Given: Any API request
   - When: Response is sent
   - Then: Headers include limit, remaining, reset time

3. **Different limits by tier**
   - Given: Public vs authenticated requests
   - When: Requests are counted
   - Then: Different limits apply

4. **Limit reset after window**
   - Given: User hits rate limit
   - When: Time window expires
   - Then: Counter resets, requests allowed again

5. **Configurable limits**
   - Given: Rate limits in configuration
   - When: Application starts
   - Then: Limits are loaded from config (not hardcoded)

## Technical Approach

### Option 1: Spring Interceptor + In-Memory Cache
```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final ConcurrentHashMap<String, RateLimitBucket> buckets;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        String key = getUserKey(request);
        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> new RateLimitBucket());
        
        if (bucket.tryConsume()) {
            addRateLimitHeaders(response, bucket);
            return true;
        } else {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(bucket.getResetSeconds()));
            return false;
        }
    }
}
```

### Option 2: Redis-based Rate Limiting (Recommended for production)
- Use Redis for distributed rate limiting
- Supports multiple application instances
- Persistent across restarts

### Option 3: Spring Cloud Gateway Rate Limiter (if using gateway)

## Technical Tasks
- [ ] Choose rate limiting strategy (in-memory vs Redis)
- [ ] Create `RateLimitInterceptor` or filter
- [ ] Implement token bucket or sliding window algorithm
- [ ] Add configuration properties (`application.yml`)
- [ ] Add rate limit headers to responses
- [ ] Create custom exception for rate limit exceeded
- [ ] Add global exception handler for 429 responses
- [ ] Create unit tests for rate limiter
- [ ] Create integration tests verifying limits
- [ ] Add monitoring/metrics for rate limit hits
- [ ] Document rate limits in OpenAPI spec
- [ ] Update README with rate limit information

## Configuration Properties
```yaml
# application.yml
api:
  rate-limit:
    enabled: true
    tiers:
      public:
        requests: 10
        window-seconds: 60
      authenticated:
        requests: 100
        window-seconds: 60
      admin:
        requests: 1000
        window-seconds: 60
    storage: memory  # or redis
    redis:
      host: localhost
      port: 6379
```

## Testing Requirements
- Unit test: Token bucket logic
- Unit test: Sliding window logic
- Integration test: Rate limit enforcement
- Integration test: Rate limit reset after window
- Integration test: Different limits for different tiers
- Load test: Verify performance under high load

## Dependencies
- Guava (for `RateLimiter`) or Bucket4j library
- Redis (optional, for distributed rate limiting)
- Spring AOP or Interceptor support

## Estimated Effort
**12-16 hours**
- Research and design: 2 hours
- Implementation (in-memory): 4 hours
- Implementation (Redis): 6 hours (if chosen)
- Testing: 4 hours
- Documentation: 2 hours

## References
- Bucket4j library: https://github.com/bucket4j/bucket4j
- Spring rate limiting patterns
- RFC 6585 (429 Too Many Requests)

## Quality Gates
- [ ] All tests pass
- [ ] Rate limits enforced correctly
- [ ] Headers returned in all responses
- [ ] Performance impact < 5ms per request
- [ ] Documentation updated

## Security Considerations
- Rate limiting by IP can be bypassed with proxies
- Consider combining IP + User ID for authenticated requests
- Admin endpoints need higher limits but still should be limited
- Consider whitelisting trusted IPs
- Monitor for distributed attacks (many IPs)

## Monitoring & Alerting
Add metrics for:
- Rate limit hits per endpoint
- Top users hitting limits
- Rate limit bypass attempts
- Overall request volume

## Business Value
- **Protect infrastructure** from abuse and DoS
- **Ensure fair usage** for all users
- **Reduce costs** by preventing resource exhaustion
- **Improve reliability** by preventing overload
- **Security** against brute force attacks

## Notes
Start with in-memory implementation for MVP, migrate to Redis for production when scaling to multiple instances.

