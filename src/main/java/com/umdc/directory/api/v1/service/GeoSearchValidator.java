package com.umdc.directory.api.v1.service;

import com.umdc.directory.constant.DirectoryAppConstants;

import java.util.UUID;

/// Parses and validates the parameters of `nearby` and `map`. Every value arrives as text so that a malformed one
/// produces a `400` with the reason in `message-error` instead of a bare framework error.
@SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.AvoidLiteralsInIfCondition"}) // text parameters on purpose: see class comment
public final class GeoSearchValidator {

    public static final int DEFAULT_RADIUS_M = 5000;
    public static final int MAX_RADIUS_M = 50_000;
    public static final int DEFAULT_MAP_LIMIT = 200;
    public static final int MAX_MAP_LIMIT = 500;
    public static final int MIN_ZOOM = 0;
    public static final int MAX_ZOOM = 22;
    public static final int DEFAULT_ZOOM = 14;
    /// Below this zoom the map answers with clusters.
    public static final int CLUSTER_BELOW_ZOOM = 14;
    /// Largest bbox accepted, in degrees on each axis.
    public static final double MAX_BBOX_SPAN_DEG = 90d;
    public static final int MAX_TEXT_LENGTH = 100;

    private GeoSearchValidator() {
        throw new IllegalStateException("Utility class");
    }

    /// Validated parameters of `nearby`.
    public record NearbyQuery(double lat, double lng, int radiusM, UUID categoryId, UUID cityId, String text,
                              boolean sortByName, int page, int perPage) {
    }

    /// Validated parameters of `map`.
    public record MapQuery(double minLng, double minLat, double maxLng, double maxLat, int zoom, UUID categoryId,
                           UUID cityId, String text, int limit) {
    }

    /// @throws GeoSearchException when a parameter is missing, malformed or out of range
    public static NearbyQuery nearby(String lat, String lng, String radiusM, String categoryId, String cityId, String q,
                                     String sort, String page, String perPage) {
        double latitude = coordinate(lat, "lat", 90);
        double longitude = coordinate(lng, "lng", 180);
        int radius = blank(radiusM) ? DEFAULT_RADIUS_M : integer(radiusM, "radius_m");
        if (radius <= 0 || radius > MAX_RADIUS_M) {
            throw new GeoSearchException("radius_m must be between 1 and " + MAX_RADIUS_M);
        }
        boolean byName;
        if (blank(sort) || "distance".equalsIgnoreCase(sort.trim())) {
            byName = false;
        } else if ("name".equalsIgnoreCase(sort.trim())) {
            byName = true;
        } else {
            throw new GeoSearchException("sort must be distance or name");
        }
        int resolvedPage = blank(page) ? DirectoryAppConstants.DEFAULT_PAGE : integer(page, "page");
        int resolvedPerPage = blank(perPage) ? DirectoryAppConstants.DEFAULT_PER_PAGE : integer(perPage, "per_page");
        if (resolvedPage < 1 || resolvedPerPage < 1 || resolvedPerPage > DirectoryAppConstants.MAX_PER_PAGE) {
            throw new GeoSearchException(DirectoryAppConstants.SEARCH_INVALID_PAGINATION);
        }
        return new NearbyQuery(latitude, longitude, radius, uuid(categoryId, "category_id"), uuid(cityId, "city_id"), text(q),
                byName, resolvedPage, resolvedPerPage);
    }

    /// @throws GeoSearchException when a parameter is missing, malformed, inverted or out of range
    public static MapQuery map(String bbox, String zoom, String categoryId, String cityId, String q, String limit) {
        if (blank(bbox)) {
            throw new GeoSearchException("bbox is required: minLng,minLat,maxLng,maxLat");
        }
        String[] parts = bbox.split(",", -1);
        if (parts.length != 4) {
            throw new GeoSearchException("bbox must be minLng,minLat,maxLng,maxLat");
        }
        double minLng = coordinate(parts[0], "bbox minLng", 180);
        double minLat = coordinate(parts[1], "bbox minLat", 90);
        double maxLng = coordinate(parts[2], "bbox maxLng", 180);
        double maxLat = coordinate(parts[3], "bbox maxLat", 90);
        if (minLng >= maxLng || minLat >= maxLat) {
            throw new GeoSearchException("bbox is inverted: min values must be lower than max values");
        }
        if (maxLng - minLng > MAX_BBOX_SPAN_DEG || maxLat - minLat > MAX_BBOX_SPAN_DEG) {
            throw new GeoSearchException("bbox is too large: at most " + (int) MAX_BBOX_SPAN_DEG + " degrees on each axis");
        }
        int resolvedZoom = blank(zoom) ? DEFAULT_ZOOM : integer(zoom, "zoom");
        if (resolvedZoom < MIN_ZOOM || resolvedZoom > MAX_ZOOM) {
            throw new GeoSearchException("zoom must be between " + MIN_ZOOM + " and " + MAX_ZOOM);
        }
        int resolvedLimit = blank(limit) ? DEFAULT_MAP_LIMIT : integer(limit, "limit");
        if (resolvedLimit < 1 || resolvedLimit > MAX_MAP_LIMIT) {
            throw new GeoSearchException("limit must be between 1 and " + MAX_MAP_LIMIT);
        }
        return new MapQuery(minLng, minLat, maxLng, maxLat, resolvedZoom, uuid(categoryId, "category_id"), uuid(cityId, "city_id"),
                text(q), resolvedLimit);
    }

    /// Validated optional point and order of `/businesses/search`; `lat` and `lng` are null when no point was sent.
    public record SearchGeo(Double lat, Double lng, boolean sortByDistance) {
    }

    /// @param lat  optional latitude; sent together with `lng`
    /// @param lng  optional longitude; sent together with `lat`
    /// @param sort optional: `name` (default) or `distance` (needs the point)
    /// @throws GeoSearchException when only one coordinate is sent, a value is invalid or `distance` is asked without a point
    public static SearchGeo searchGeo(String lat, String lng, String sort) {
        boolean hasLat = !blank(lat);
        boolean hasLng = !blank(lng);
        if (hasLat != hasLng) {
            throw new GeoSearchException("lat and lng must be sent together");
        }
        boolean byDistance;
        if (blank(sort) || "name".equalsIgnoreCase(sort.trim())) {
            byDistance = false;
        } else if ("distance".equalsIgnoreCase(sort.trim())) {
            byDistance = true;
        } else {
            throw new GeoSearchException("sort must be distance or name");
        }
        if (!hasLat) {
            if (byDistance) {
                throw new GeoSearchException("sort=distance needs lat and lng");
            }
            return new SearchGeo(null, null, false);
        }
        return new SearchGeo(coordinate(lat, "lat", 90), coordinate(lng, "lng", 180), byDistance);
    }

    /// Validated filters without geometry (saved searches that have no point or bbox).
    public record Filters(UUID categoryId, UUID cityId, String text) {
    }

    /// @throws GeoSearchException when an id is not a UUID or the text is too long
    public static Filters filters(String categoryId, String cityId, String q) {
        return new Filters(uuid(categoryId, "category_id"), uuid(cityId, "city_id"), text(q));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static double coordinate(String value, String name, double limit) {
        if (blank(value)) {
            throw new GeoSearchException(name + " is required");
        }
        double parsed;
        try {
            parsed = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new GeoSearchException(name + " must be a number", e);
        }
        if (!Double.isFinite(parsed) || parsed < -limit || parsed > limit) {
            throw new GeoSearchException(name + " must be between -" + (int) limit + " and " + (int) limit);
        }
        return parsed;
    }

    private static int integer(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new GeoSearchException(name + " must be an integer", e);
        }
    }

    private static UUID uuid(String value, String name) {
        if (blank(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new GeoSearchException(name + " must be a UUID", e);
        }
    }

    private static String text(String q) {
        if (blank(q)) {
            return null;
        }
        if (q.trim().length() > MAX_TEXT_LENGTH) {
            throw new GeoSearchException("q must have at most " + MAX_TEXT_LENGTH + " characters");
        }
        return q.trim();
    }
}
