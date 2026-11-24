package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.OpeningHoursTO;
import com.umdc.directory.api.v1.to.OpeningSlotTO;
import com.umdc.directory.api.v1.to.OpeningStatusTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.constant.OpeningHoursMessages;
import com.umdc.directory.jpa.entity.LocationOpeningHoursEntity;
import com.umdc.directory.jpa.entity.TimezoneEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.LocationOpeningHoursRepository;
import com.umdc.directory.jpa.repository.LocationRepository;
import com.umdc.directory.util.JwtUtil;
import com.umdc.directory.util.OpeningHoursEvaluator;
import com.umdc.directory.util.OpeningHoursEvaluator.Slot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class OpeningHoursServiceImpl implements OpeningHoursService {

    private static final DateTimeFormatter HH_MM_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final LocationOpeningHoursRepository hoursRepository;
    private final LocationRepository locationRepository;
    private final BusinessRepository businessRepository;
    private final Clock clock;

    @Autowired
    public OpeningHoursServiceImpl(LocationOpeningHoursRepository hoursRepository, LocationRepository locationRepository,
                                   BusinessRepository businessRepository) {
        this(hoursRepository, locationRepository, businessRepository, Clock.systemUTC());
    }

    /// For tests: "now" comes from the clock.
    OpeningHoursServiceImpl(LocationOpeningHoursRepository hoursRepository, LocationRepository locationRepository,
                            BusinessRepository businessRepository, Clock clock) {
        this.hoursRepository = hoursRepository;
        this.locationRepository = locationRepository;
        this.businessRepository = businessRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<OpeningHoursTO> get(UUID businessId, UUID locationId) {
        if (!locationExists(businessId, locationId)) {
            return error(HttpStatus.NOT_FOUND, DirectoryAppConstants.LOCATION_NOT_FOUND);
        }
        return ResponseEntity.ok(toTO(locationId, zoneOf(businessId)));
    }

    @Override
    @Transactional
    public ResponseEntity<OpeningHoursTO> replace(UUID businessId, UUID locationId, OpeningHoursTO body, String token) {
        var denied = ownerCheck(businessId, token);
        if (denied != null) {
            return error(denied.status(), denied.reason());
        }
        if (!locationExists(businessId, locationId)) {
            return error(HttpStatus.NOT_FOUND, DirectoryAppConstants.LOCATION_NOT_FOUND);
        }
        var parsed = new ArrayList<LocationOpeningHoursEntity>();
        var problem = OpeningHoursValidator.validate(body, locationId, parsed);
        if (problem != null) {
            return error(HttpStatus.BAD_REQUEST, problem);
        }
        hoursRepository.deleteByLocationId(locationId);
        hoursRepository.saveAll(parsed);
        return ResponseEntity.ok(toTO(locationId, zoneOf(businessId)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<OpeningStatusTO> status(UUID businessId, UUID locationId, String at) {
        Instant instant;
        try {
            instant = Objects.isNull(at) || at.isBlank() ? clock.instant() : Instant.parse(at.trim());
        } catch (DateTimeParseException e) {
            return error(HttpStatus.BAD_REQUEST, OpeningHoursMessages.INVALID_AT);
        }
        if (!locationExists(businessId, locationId)) {
            return error(HttpStatus.NOT_FOUND, DirectoryAppConstants.LOCATION_NOT_FOUND);
        }
        var zone = zoneOf(businessId);
        var slots = hoursRepository.findByLocationId(locationId).stream()
                .map(h -> new Slot(DayOfWeek.of(h.getDayOfWeek()), h.getOpensAt(), h.getClosesAt(), h.isClosed())).toList();
        boolean open = OpeningHoursEvaluator.isOpenNow(slots, zone, instant);
        var change = OpeningHoursEvaluator.nextChange(slots, zone, instant);
        String when = change.map(c -> c.at().atZone(zone).toOffsetDateTime().toString()).orElse(null);
        return ResponseEntity.ok(new OpeningStatusTO(open, open ? when : null, open ? null : when, zone.getId()));
    }

    private record Denied(HttpStatus status, String reason) {
    }

    /// @return why the caller may not write the hours of the business (401, 404 or 403), or null when it is the owner
    private Denied ownerCheck(UUID businessId, String token) {
        var caller = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(caller)) {
            return new Denied(HttpStatus.UNAUTHORIZED, DirectoryAppConstants.UNAUTHORIZED_MESSAGE);
        }
        var owner = Objects.isNull(businessId) ? Optional.<UUID>empty() : businessRepository.findOwnerId(businessId);
        if (owner.isEmpty()) {
            return new Denied(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND);
        }
        return caller.equals(owner.get()) ? null : new Denied(HttpStatus.FORBIDDEN, DirectoryAppConstants.UNAUTHORIZED_MESSAGE);
    }

    private boolean locationExists(UUID businessId, UUID locationId) {
        return Objects.nonNull(businessId) && Objects.nonNull(locationId)
                && locationRepository.findByIdAndBusinessId(locationId, businessId).isPresent();
    }

    /// Zone of the business: its timezone name when it is an IANA id, else its fixed UTC offset, else the default zone.
    ZoneId zoneOf(UUID businessId) {
        return zoneOf(hoursRepository.findTimezoneOfBusiness(businessId).orElse(null));
    }

    /// Zone of a timezone row: its name when it is an IANA id, else its fixed UTC offset, else the default zone (also for null).
    static ZoneId zoneOf(TimezoneEntity timezone) {
        if (timezone != null) {
            try {
                return ZoneId.of(timezone.getName());
            } catch (DateTimeException | NullPointerException e) {
                var offset = timezone.getUtcOffset();
                if (offset != null) {
                    try {
                        return ZoneOffset.ofTotalSeconds((int) offset.toSeconds());
                    } catch (DateTimeException ignored) {
                        // fall through to the default
                    }
                }
            }
        }
        return ZoneId.of(OpeningHoursMessages.DEFAULT_ZONE);
    }

    private OpeningHoursTO toTO(UUID locationId, ZoneId zone) {
        var slots = hoursRepository.findByLocationId(locationId).stream()
                .map(h -> new OpeningSlotTO(h.getDayOfWeek(),
                        h.getOpensAt() == null ? null : h.getOpensAt().format(HH_MM_FORMAT),
                        h.getClosesAt() == null ? null : h.getClosesAt().format(HH_MM_FORMAT),
                        h.isClosed())).toList();
        return new OpeningHoursTO(slots, zone.getId());
    }

    private static <T> ResponseEntity<T> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, message).build();
    }
}
