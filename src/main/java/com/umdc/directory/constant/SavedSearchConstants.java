package com.umdc.directory.constant;

/// Limits and messages of the saved searches and the last search of a consumer.
public final class SavedSearchConstants {

    /// Saved searches a user may keep.
    public static final int MAX_PER_USER = 20;
    public static final int MAX_NAME_LENGTH = 80;

    public static final String ENDPOINT_SEARCH = "/api/v1/businesses/search";
    public static final String ENDPOINT_NEARBY = "/api/v1/businesses/nearby";
    public static final String ENDPOINT_MAP = "/api/v1/businesses/map";

    public static final String SAVED_SEARCH_NOT_FOUND = "Saved search not found";
    public static final String LAST_SEARCH_NOT_FOUND = "No last search";
    public static final String INVALID_NAME = "name is required and must have at most " + MAX_NAME_LENGTH + " characters";
    public static final String DUPLICATE_NAME = "A saved search with that name already exists";
    public static final String LIMIT_REACHED = "At most " + MAX_PER_USER + " saved searches per user";
    public static final String BODY_REQUIRED = "A request body is required";
    public static final String PARAMS_REQUIRED = "params are required";
    public static final String EMPTY_SEARCH = "At least one search parameter is required";
    public static final String LAT_LNG_TOGETHER = "lat and lng must be sent together";
    public static final String GEO_NEEDS_POINT = "radius_m and sort require lat and lng";
    public static final String POINT_AND_BBOX = "lat/lng and bbox cannot be combined";
    public static final String CITY_TOO_LONG = "city must have at most 100 characters";

    private SavedSearchConstants() {
        throw new IllegalStateException("Utility class");
    }
}
