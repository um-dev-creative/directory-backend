package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/// Opening hours of a location. As a request only `slots` is read.
///
/// @param slots    every slot, in week order (empty when no hours were set)
/// @param timezone IANA zone the hours are expressed in (response only)
public record OpeningHoursTO(
        @JsonProperty("slots") List<OpeningSlotTO> slots,
        @JsonProperty("timezone") String timezone) {
}
