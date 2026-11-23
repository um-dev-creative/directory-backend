package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.OpeningHoursTO;
import com.umdc.directory.api.v1.to.OpeningSlotTO;
import com.umdc.directory.constant.OpeningHoursMessages;
import com.umdc.directory.jpa.entity.LocationOpeningHoursEntity;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/// Validation of a request to replace the opening hours of a location.
final class OpeningHoursValidator {

    private static final Pattern HH_MM = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private OpeningHoursValidator() {
    }

    /// @return the reason of the first problem, or null when valid; the entities to store are added to `out`
    static String validate(OpeningHoursTO body, UUID locationId, List<LocationOpeningHoursEntity> out) {
        if (Objects.isNull(body)) {
            return OpeningHoursMessages.BODY_REQUIRED;
        }
        var slots = Objects.isNull(body.slots()) ? List.<OpeningSlotTO>of() : body.slots();
        for (var slot : slots) {
            if (Objects.isNull(slot)) {
                return OpeningHoursMessages.BODY_REQUIRED;
            }
            if (slot.dayOfWeek() < 1 || slot.dayOfWeek() > 7) {
                return OpeningHoursMessages.DAY_RANGE;
            }
            if (slot.closed()) {
                if (slot.opensAt() != null || slot.closesAt() != null) {
                    return OpeningHoursMessages.CLOSED_WITH_TIMES;
                }
                out.add(new LocationOpeningHoursEntity(locationId, slot.dayOfWeek(), null, null, true));
                continue;
            }
            if (slot.opensAt() == null || slot.closesAt() == null) {
                return OpeningHoursMessages.TIMES_REQUIRED;
            }
            if (!HH_MM.matcher(slot.opensAt()).matches() || !HH_MM.matcher(slot.closesAt()).matches()) {
                return OpeningHoursMessages.TIME_FORMAT;
            }
            var opens = LocalTime.parse(slot.opensAt());
            var closes = LocalTime.parse(slot.closesAt());
            if (!opens.isBefore(closes)) {
                return OpeningHoursMessages.OPENS_BEFORE_CLOSES;
            }
            out.add(new LocationOpeningHoursEntity(locationId, slot.dayOfWeek(), opens, closes, false));
        }
        for (int day = 1; day <= 7; day++) {
            final int d = day;
            var ofDay = out.stream().filter(e -> e.getDayOfWeek() == d).sorted(Comparator.comparing(
                    (LocationOpeningHoursEntity e) -> e.getOpensAt() == null ? LocalTime.MIN : e.getOpensAt())).toList();
            if (ofDay.size() > OpeningHoursMessages.MAX_SLOTS_PER_DAY) {
                return OpeningHoursMessages.TOO_MANY_SLOTS;
            }
            if (ofDay.size() > 1 && ofDay.stream().anyMatch(LocationOpeningHoursEntity::isClosed)) {
                return OpeningHoursMessages.CLOSED_MIXED;
            }
            for (int i = 1; i < ofDay.size(); i++) {
                if (ofDay.get(i).getOpensAt().isBefore(ofDay.get(i - 1).getClosesAt())) {
                    return OpeningHoursMessages.OVERLAP;
                }
            }
        }
        return null;
    }
}
