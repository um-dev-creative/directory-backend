package com.umdc.directory.constant;

public final class DirectoryAppConstants {
    public static final String APPLICATION_NAME = "Directory-Site";

    public static final String ENTITY_PACKAGE = "com.umdc.directory.jpa.entity";
    public static final String REPOSITORY_PACKAGE = "com.umdc.directory.jpa.repository";
    public static final String MESSAGE_ERROR_HEADER = "message-error";
    public static final String MESSAGE_HEADER = "message";
    public static final String USER_NOT_FOUND_MESSAGE = "User not found.";

    // Campaign pagination defaults
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PER_PAGE = 10;
    public static final int MAX_PER_PAGE = 100;

    // Campaign error messages
    public static final String CAMPAIGN_CATEGORY_ID_REQUIRED = "categoryId is required";
    public static final String CAMPAIGN_BUSINESS_ID_REQUIRED = "businessId is required";
    public static final String CAMPAIGN_ID_REQUIRED = "Campaign id is required";
    public static final String CAMPAIGN_REQUEST_BODY_REQUIRED = "Request body is required";
    public static final String CAMPAIGN_NOT_FOUND = "Campaign not found";
    public static final String CAMPAIGN_MODIFIED_BY_ANOTHER_REQUEST = "Campaign has been modified by another request. Please refresh and try again.";
    public static final String CAMPAIGN_START_DATE_AFTER_END_DATE = "startDate must be before or equal to endDate";
    public static final String CATEGORY_NOT_FOUND = "Category not found";
    public static final String BUSINESS_NOT_FOUND = "Business not found";

    // Standard API error messages and codes
    public static final String INTERNAL_SERVER_ERROR_CODE = "500";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";

    // Campaign sort fields
    public static final String CAMPAIGN_SORT_DEFAULT_FIELD = "createdDate";
    public static final String CAMPAIGN_SORT_FIELD_TITLE = "title";
    public static final String CAMPAIGN_SORT_FIELD_START_DATE = "start_date";
    public static final String CAMPAIGN_SORT_FIELD_END_DATE = "end_date";
    public static final String CAMPAIGN_SORT_FIELD_CREATED_DATE = "created_date";
    public static final String CAMPAIGN_SORT_PROPERTY_START_DATE = "startDate";
    public static final String CAMPAIGN_SORT_PROPERTY_END_DATE = "endDate";
    public static final String CAMPAIGN_SORT_DELIMITER = ",";
    public static final String CAMPAIGN_SORT_DESC_PREFIX = "-";

    // Redis cache keys and settings for timezone
    public static final String TIMEZONE_CACHE_KEY = "timezones:all:v1";
    public static final String TIMEZONE_CACHE_LOCK_KEY = "timezones:lock";
    public static final long TIMEZONE_CACHE_DEFAULT_TTL_SECONDS = 86_400L; // 24 hours
    public static final long TIMEZONE_CACHE_LOCK_TTL_MS = 5_000L;

    // Business statistics (views)
    public static final int STATS_DEFAULT_DAYS = 30;
    public static final int STATS_MAX_DAYS = 365;
    public static final int STATS_SHORT_WINDOW_DAYS = 7;
    public static final String STATS_INVALID_DAYS = "days must be between 1 and " + STATS_MAX_DAYS;
    public static final String STATS_ONLY_OWNER = "Only the owner of the business can see its statistics";
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";

    // Aggregated click events (RF-10)
    public static final int EVENTS_DEFAULT_DAYS = 30;
    public static final int EVENTS_MAX_DAYS = 365;
    public static final String EVENT_INVALID_TYPE = "type must be one of ROUTE, CALL, WEB, SAVE";
    public static final String EVENT_BODY_REQUIRED = "Request body is required";
    public static final String EVENTS_INVALID_RANGE = "from must not be after to and the range must be at most " + EVENTS_MAX_DAYS + " days";
    public static final String EVENTS_ONLY_OWNER = "Only the owner of the business can see its events";

    // Locations (sedes), cities and business search
    public static final String CITY_NOT_FOUND = "City not found";
    public static final String LOCATION_NOT_FOUND = "Location not found";
    public static final String LOCATION_REQUEST_BODY_REQUIRED = "Request body is required";
    public static final String LOCATION_CITY_REQUIRED = "cityId is required";
    public static final String LOCATION_ADDRESS_REQUIRED = "addressLine is required";
    public static final String LOCATION_FIELD_TOO_LONG = "A field exceeds its maximum length (name 120, addressLine 200, postalCode 10, phone 30)";
    public static final String LOCATION_COORDINATES_PAIR = "latitude and longitude must be sent together";
    public static final String LOCATION_COORDINATES_RANGE = "latitude must be between -90 and 90 and longitude between -180 and 180";
    public static final String LOCATION_MAIN_CANNOT_BE_UNSET = "A business needs a main location: make another location the main one instead";
    public static final int LOCATION_NAME_MAX = 120;
    public static final int LOCATION_ADDRESS_MAX = 200;
    public static final int LOCATION_POSTAL_CODE_MAX = 10;
    public static final int LOCATION_PHONE_MAX = 30;
    public static final String SEARCH_INVALID_PAGINATION = "page must be >= 1 and per_page between 1 and " + MAX_PER_PAGE;

    // Favorite type filters
    public static final String FAVORITE_TYPE_STORES = "stores";
    public static final String FAVORITE_TYPE_PRODUCTS = "products";
    public static final String FAVORITE_TYPE_OFFERS = "offers";

    private DirectoryAppConstants() {
        throw new IllegalStateException("Utility class");
    }
}
