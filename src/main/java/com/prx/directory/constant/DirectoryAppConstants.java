package com.prx.directory.constant;

public final class DirectoryAppConstants {
    public static final String ENTITY_PACKAGE = "com.prx.directory.jpa.entity";
    public static final String REPOSITORY_PACKAGE = "com.prx.directory.jpa.repository";
    public static final String MESSAGE_ERROR_HEADER = "message-error";
    public static final String MESSAGE_HEADER = "message";
    public static final String USER_NOT_FOUND_MESSAGE = "User not found.";

    // Campaign pagination defaults
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PER_PAGE = 20;
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
    public static final String CAMPAIGN_SORT_FIELD_NAME = "name";
    public static final String CAMPAIGN_SORT_FIELD_START_DATE = "start_date";
    public static final String CAMPAIGN_SORT_FIELD_END_DATE = "end_date";
    public static final String CAMPAIGN_SORT_FIELD_CREATED_DATE = "created_date";
    public static final String CAMPAIGN_SORT_PROPERTY_START_DATE = "startDate";
    public static final String CAMPAIGN_SORT_PROPERTY_END_DATE = "endDate";
    public static final String CAMPAIGN_SORT_DELIMITER = ",";
    public static final String CAMPAIGN_SORT_DESC_PREFIX = "-";

    // Favorite type filters
    public static final String FAVORITE_TYPE_STORES = "stores";
    public static final String FAVORITE_TYPE_PRODUCTS = "products";
    public static final String FAVORITE_TYPE_OFFERS = "offers";

    private DirectoryAppConstants() {
        throw new IllegalStateException("Utility class");
    }
}
