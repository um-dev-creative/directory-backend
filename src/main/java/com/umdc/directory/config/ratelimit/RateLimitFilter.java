package com.umdc.directory.config.ratelimit;

import com.umdc.directory.constant.DirectoryAppConstants;
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
    private final int eventPermitsPerMinute;
    private final int geocodePermitsPerMinute;
    private final int mapPermitsPerMinute;
    private final int nearbyPermitsPerMinute;

    /// @param store                the bucket store
    /// @param apiPath              base path to protect
    /// @param ipPermitsPerMinute   quota per IP
    /// @param userPermitsPerMinute quota per user id
    /// @param trustForwardedHeader use the first `X-Forwarded-For` address as client IP (only behind a trusted proxy)
    public RateLimitFilter(RateLimitStore store, String apiPath, int ipPermitsPerMinute, int userPermitsPerMinute,
                           boolean trustForwardedHeader) {
        this(store, apiPath, ipPermitsPerMinute, userPermitsPerMinute, trustForwardedHeader, 0);
    }

    /// Same as the other constructor, plus a stricter quota for `POST {apiPath}/businesses/{id}/events` (click events), applied
    /// per client IP and business.
    ///
    /// @param eventPermitsPerMinute quota per IP and business for click events; 0 or less disables the extra bucket
    public RateLimitFilter(RateLimitStore store, String apiPath, int ipPermitsPerMinute, int userPermitsPerMinute,
                           boolean trustForwardedHeader, int eventPermitsPerMinute) {
        this(store, apiPath, ipPermitsPerMinute, userPermitsPerMinute, trustForwardedHeader, eventPermitsPerMinute, 0, 0, 0);
    }

    /// Same as the other constructors, plus dedicated quotas for the geo routes, each keyed by user id when a session token
    /// carries one, otherwise by client IP: `POST {apiPath}/geocode` (external provider cost), `GET {apiPath}/businesses/map`
    /// and `GET {apiPath}/businesses/nearby`.
    ///
    /// @param geocodePermitsPerMinute quota for geocoding; 0 or less disables that bucket
    /// @param mapPermitsPerMinute     quota for the map route; 0 or less disables that bucket
    /// @param nearbyPermitsPerMinute  quota for the nearby route; 0 or less disables that bucket
    public RateLimitFilter(RateLimitStore store, String apiPath, int ipPermitsPerMinute, int userPermitsPerMinute,
                           boolean trustForwardedHeader, int eventPermitsPerMinute, int geocodePermitsPerMinute,
                           int mapPermitsPerMinute, int nearbyPermitsPerMinute) {
        this.geocodePermitsPerMinute = geocodePermitsPerMinute;
        this.mapPermitsPerMinute = mapPermitsPerMinute;
        this.nearbyPermitsPerMinute = nearbyPermitsPerMinute;
        this.eventPermitsPerMinute = eventPermitsPerMinute;
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
        String ip = clientIp(request);
        RateLimitDecision decision = store.tryConsume("ip:" + ip, ipPermitsPerMinute);
        if (decision.allowed() && eventPermitsPerMinute > 0 && isEventPost(request)) {
            decision = store.tryConsume("ev:" + ip + ":" + request.getRequestURI(), eventPermitsPerMinute);
        }
        String uid = null;
        if (decision.allowed()) {
            uid = userId(request.getHeader(SESSION_HEADER));
            if (uid != null) {
                decision = store.tryConsume("uid:" + uid, userPermitsPerMinute);
            }
        }
        if (decision.allowed()) {
            decision = routeDecision(request, uid, ip, decision);
        }
        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setHeader("X-RateLimit-Limit", Integer.toString(decision.limit()));
        response.setHeader(DirectoryAppConstants.MESSAGE_HEADER, "Too many requests, retry in " + decision.retryAfterSeconds() + " seconds");
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"too_many_requests\",\"retryAfterSeconds\":" + decision.retryAfterSeconds() + "}");
    }

    /// Applies the dedicated geo bucket of the request route, if any. Keyed by user id, else by IP.
    private RateLimitDecision routeDecision(HttpServletRequest request, String uid, String ip, RateLimitDecision current) {
        String path = request.getRequestURI();
        String route;
        int permits;
        if ("POST".equals(request.getMethod()) && path.equals(apiPath + "/geocode")) {
            route = "geocode";
            permits = geocodePermitsPerMinute;
        } else if ("GET".equals(request.getMethod()) && path.equals(apiPath + "/businesses/map")) {
            route = "map";
            permits = mapPermitsPerMinute;
        } else if ("GET".equals(request.getMethod()) && path.equals(apiPath + "/businesses/nearby")) {
            route = "nearby";
            permits = nearbyPermitsPerMinute;
        } else {
            return current;
        }
        if (permits <= 0) {
            return current;
        }
        return store.tryConsume("geo:" + route + ":" + (uid != null ? "uid:" + uid : "ip:" + ip), permits);
    }

    private boolean isEventPost(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        String prefix = apiPath + "/businesses/";
        if (!path.startsWith(prefix) || !path.endsWith("/events") || path.length() <= prefix.length() + "/events".length()) {
            return false;
        }
        String id = path.substring(prefix.length(), path.length() - "/events".length());
        return id.indexOf('/') < 0;
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
