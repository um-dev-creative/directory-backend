package com.umdc.directory.api.v1.service;

import com.umdc.directory.constant.DirectoryAppConstants;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/// Field rules of the location requests (lengths and coordinates); the same limits as the columns of `directory_site.location`.
final class LocationValidator {

    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    /// Reasonable bounding box of Canada (south, north, west, east); a pin outside it is almost surely a typo or a swapped pair.
    static final BigDecimal CANADA_MIN_LATITUDE = new BigDecimal("41.6");
    static final BigDecimal CANADA_MAX_LATITUDE = new BigDecimal("83.2");
    static final BigDecimal CANADA_MIN_LONGITUDE = new BigDecimal("-141.1");
    static final BigDecimal CANADA_MAX_LONGITUDE = new BigDecimal("-52.5");

    static final String LOCATION_COORDINATES_NULL_ISLAND = "the point (0, 0) is not a valid location";
    static final String LOCATION_COORDINATES_OUTSIDE_CANADA = "latitude must be between 41.6 and 83.2 and longitude between -141.1 and -52.5 (Canada)";

    /// `umdc.location.bounds.enforced`, set at startup by [LocationBoundsProperties]; the (0, 0) check is always applied.
    private static final java.util.concurrent.atomic.AtomicBoolean BOUNDS_ENFORCED = new java.util.concurrent.atomic.AtomicBoolean(true);

    private LocationValidator() {
        throw new IllegalStateException("Utility class");
    }

    static Optional<String> validateFields(String name, String addressLine, String postalCode, String phone,
                                                   BigDecimal latitude, BigDecimal longitude) {
        if (tooLong(name, DirectoryAppConstants.LOCATION_NAME_MAX) || tooLong(addressLine, DirectoryAppConstants.LOCATION_ADDRESS_MAX)
                || tooLong(postalCode, DirectoryAppConstants.LOCATION_POSTAL_CODE_MAX) || tooLong(phone, DirectoryAppConstants.LOCATION_PHONE_MAX)) {
            return Optional.of(DirectoryAppConstants.LOCATION_FIELD_TOO_LONG);
        }
        if (Objects.isNull(latitude) != Objects.isNull(longitude)) {
            return Optional.of(DirectoryAppConstants.LOCATION_COORDINATES_PAIR);
        }
        if (latitude != null && (latitude.abs().compareTo(MAX_LATITUDE) > 0 || longitude.abs().compareTo(MAX_LONGITUDE) > 0)) {
            return Optional.of(DirectoryAppConstants.LOCATION_COORDINATES_RANGE);
        }
        if (latitude != null && latitude.signum() == 0 && longitude.signum() == 0) {
            return Optional.of(LOCATION_COORDINATES_NULL_ISLAND);
        }
        if (latitude != null && BOUNDS_ENFORCED.get() && outsideCanada(latitude, longitude)) {
            return Optional.of(LOCATION_COORDINATES_OUTSIDE_CANADA);
        }
        return Optional.empty();
    }

    static void setBoundsEnforced(boolean enforced) {
        BOUNDS_ENFORCED.set(enforced);
    }

    static boolean isBoundsEnforced() {
        return BOUNDS_ENFORCED.get();
    }

    private static boolean outsideCanada(BigDecimal latitude, BigDecimal longitude) {
        return latitude.compareTo(CANADA_MIN_LATITUDE) < 0 || latitude.compareTo(CANADA_MAX_LATITUDE) > 0
                || longitude.compareTo(CANADA_MIN_LONGITUDE) < 0 || longitude.compareTo(CANADA_MAX_LONGITUDE) > 0;
    }

    private static boolean tooLong(String value, int max) {
        return value != null && value.trim().length() > max;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
