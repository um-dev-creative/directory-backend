package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umdc.commons.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/// A branch (sede) of a business.
///
/// @param id          location id
/// @param businessId  the business it belongs to
/// @param city        its city
/// @param name        optional name of the branch
/// @param addressLine street address
/// @param postalCode  optional postal code
/// @param phone       optional phone
/// @param latitude    optional latitude (-90..90); present together with the longitude
/// @param longitude   optional longitude (-180..180); present together with the latitude
/// @param main        whether it is the main location of the business
/// @param createdAt   creation date
/// @param lastUpdate  last update date
public record LocationTO(
        UUID id,
        UUID businessId,
        CityTO city,
        String name,
        String addressLine,
        String postalCode,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean main,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime lastUpdate) {
}
