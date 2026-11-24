package com.umdc.directory.constant;

/// Limits and `message-error` reasons of the favorite lists of a consumer (RF-11).
public final class FavoriteListConstants {

    /// Lists a user may keep.
    public static final int MAX_PER_USER = 20;
    public static final int MAX_NAME_LENGTH = 80;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String TYPE_STORE = "STORE";
    public static final String TYPE_PRODUCT = "PRODUCT";

    public static final String LIST_NOT_FOUND = "Favorite list not found";
    public static final String FAVORITE_NOT_FOUND = "Favorite not found";
    public static final String ITEM_NOT_IN_LIST = "The favorite is not in the list";
    public static final String INVALID_NAME = "name is required and must have at most " + MAX_NAME_LENGTH + " characters";
    public static final String DUPLICATE_NAME = "A favorite list with that name already exists";
    public static final String LIMIT_REACHED = "At most " + MAX_PER_USER + " favorite lists per user";
    public static final String BODY_REQUIRED = "A request body is required";
    public static final String FAVORITE_ID_REQUIRED = "favorite_id is required";
    public static final String FAVORITE_INACTIVE = "Only active favorites can be added to a list";
    public static final String UNSUPPORTED_TYPE = "Only store and product favorites can be added to a list";
    public static final String ALREADY_IN_LIST = "The favorite is already in the list";
    public static final String INVALID_PAGING = "page must be >= 0 and size must be >= 1";

    private FavoriteListConstants() {
        throw new IllegalStateException("Utility class");
    }
}
