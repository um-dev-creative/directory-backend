package com.umdc.directory.api.v1.service;

import com.umdc.directory.constant.DirectoryAppConstants;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/// Field rules of the location requests (lengths and coordinates); the same limits as the columns of `directory_site.location`.
final class LocationValidator {

    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

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
        return Optional.empty();
    }

    private static boolean tooLong(String value, int max) {
        return value != null && value.trim().length() > max;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
