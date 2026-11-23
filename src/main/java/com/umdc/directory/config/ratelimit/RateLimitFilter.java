package com.umdc.directory.config.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Rate limits the API (`umdc.api.endpoint` + `/**`) per client IP and, when a `session-token` header is present, per user id.
/// Anything outside the API path (`/actuator/health`, probes, docs) is never limited. A rejected request gets `429` with
/// `Retry-After` (seconds) and `X-RateLimit-Limit`.
///
/// The user id is read from the token payload (`uid`, else `sub`) only to choose the bucket; the token is not verified here
/// (the session interceptor does that). Forging it can only create more user buckets: the IP bucket always applies.
public class RateLimitFilter extends OncePerRequestFilter {

    /// Header carrying the session token.
    static final String SESSION_HEADER = "session-token";
    private static final Pattern UID = Pattern.compile("\"(?:uid|sub)\"\\s*:\\s*\"?([^\",}\\s]+)\"?");

    private final RateLimitStore store;
    private final String apiPath;
    private final int ipPermitsPerMinute;
    private final int userPermitsPerMinute;
    private final boolean trustForwardedHeader;

    /// @param store                the bucket store
    /// @param apiPath              base path to protect
    /// @param ipPermitsPerMinute   quota per IP
    /// @param userPermitsPerMinute quota per user id
    /// @param trustForwardedHeader use the first `X-Forwarded-For` address as client IP (only behind a trusted proxy)
    public RateLimitFilter(RateLimitStore store, String apiPath, int ipPermitsPerMinute, int userPermitsPerMinute,
                           boolean trustForwardedHeader) {
        this.store = store;
        this.apiPath = apiPath;
        this.ipPermitsPerMinute = ipPermitsPerMinute;
        this.userPermitsPerMinute = userPermitsPerMinute;
        this.trustForwardedHeader = trustForwardedHeader;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals(apiPath) || path.startsWith(apiPath + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RateLimitDecision decision = store.tryConsume("ip:" + clientIp(request), ipPermitsPerMinute);
        if (decision.allowed()) {
            String uid = userId(request.getHeader(SESSION_HEADER));
            if (uid != null) {
                decision = store.tryConsume("uid:" + uid, userPermitsPerMinute);
            }
        }
        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setHeader("X-RateLimit-Limit", Integer.toString(decision.limit()));
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"too_many_requests\",\"retryAfterSeconds\":" + decision.retryAfterSeconds() + "}");
    }

    String clientIp(HttpServletRequest request) {
        if (trustForwardedHeader) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    static String userId(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher m = UID.matcher(payload);
            return m.find() ? m.group(1) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
