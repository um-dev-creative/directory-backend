package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.OpeningHoursTO;
import com.umdc.directory.api.v1.to.OpeningSlotTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.constant.OpeningHoursMessages;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.entity.LocationOpeningHoursEntity;
import com.umdc.directory.jpa.entity.TimezoneEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.LocationOpeningHoursRepository;
import com.umdc.directory.jpa.repository.LocationRepository;
import com.umdc.directory.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpeningHoursServiceImplTest {

    /// Monday 2026-11-23 15:00 UTC = 10:00 in Toronto.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private LocationOpeningHoursRepository hoursRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private BusinessRepository businessRepository;

    private OpeningHoursServiceImpl service;
    private final UUID businessId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final String ownerToken = TestTokens.forUser(ownerId);

    @BeforeEach
    void setUp() {
        service = new OpeningHoursServiceImpl(hoursRepository, locationRepository, businessRepository, CLOCK);
        lenient().when(hoursRepository.findTimezoneOfBusiness(businessId)).thenReturn(Optional.empty());
        lenient().when(hoursRepository.findByLocationId(locationId)).thenReturn(List.of());
    }

    private void locationExists() {
        when(locationRepository.findByIdAndBusinessId(locationId, businessId)).thenReturn(Optional.of(new LocationEntity()));
    }

    private static OpeningSlotTO slot(int day, String from, String to) {
        return new OpeningSlotTO(day, from, to, false);
    }

    private static OpeningHoursTO body(OpeningSlotTO... slots) {
        return new OpeningHoursTO(List.of(slots), null);
    }

    private static LocationOpeningHoursEntity row(int day, String from, String to) {
        return new LocationOpeningHoursEntity(UUID.randomUUID(), day, LocalTime.parse(from), LocalTime.parse(to), false);
    }

    private static TimezoneEntity zone(String name, Duration offset) {
        var t = new TimezoneEntity();
        t.setName(name);
        t.setUtcOffset(offset);
        return t;
    }

    // ---- replace: access matrix

    @Test
    void replaceAnswers401WithoutAValidToken() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.replace(businessId, locationId, body(), null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.replace(businessId, locationId, body(), "garbage").getStatusCode());
        verify(hoursRepository, never()).saveAll(anyList());
    }

    @Test
    void replaceAnswers404WhenTheBusinessDoesNotExist() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.replace(businessId, locationId, body(), ownerToken).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.replace(null, locationId, body(), ownerToken).getStatusCode());
    }

    @Test
    void replaceAnswers403ForAnotherUser() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(UUID.randomUUID()));
        assertEquals(HttpStatus.FORBIDDEN, service.replace(businessId, locationId, body(), ownerToken).getStatusCode());
        verify(hoursRepository, never()).deleteByLocationId(any());
    }

    @Test
    void replaceAnswers404WhenTheLocationIsNotOfTheBusiness() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(locationRepository.findByIdAndBusinessId(locationId, businessId)).thenReturn(Optional.empty());
        var response = service.replace(businessId, locationId, body(), ownerToken);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(DirectoryAppConstants.LOCATION_NOT_FOUND, response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
    }

    @Test
    void theOwnerReplacesTheWholeSchedule() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        locationExists();
        when(hoursRepository.findByLocationId(locationId)).thenReturn(List.of(row(1, "09:00", "13:00")));
        var response = service.replace(businessId, locationId,
                body(slot(1, "09:00", "13:00"), slot(1, "15:00", "19:00"), new OpeningSlotTO(7, null, null, true)), ownerToken);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("America/Toronto", response.getBody().timezone());
        assertEquals("09:00", response.getBody().slots().getFirst().opensAt());
        verify(hoursRepository).deleteByLocationId(locationId);
        verify(hoursRepository).saveAll(anyList());
    }

    @Test
    void anEmptyListClearsTheSchedule() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        locationExists();
        assertEquals(HttpStatus.OK, service.replace(businessId, locationId, new OpeningHoursTO(null, null), ownerToken).getStatusCode());
        verify(hoursRepository).deleteByLocationId(locationId);
    }

    // ---- replace: validation

    private String badRequestReason(OpeningHoursTO body) {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        locationExists();
        var response = service.replace(businessId, locationId, body, ownerToken);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(hoursRepository, never()).deleteByLocationId(any());
        return response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER);
    }

    @Test
    void aMissingBodyIsRejected() {
        assertEquals(OpeningHoursMessages.BODY_REQUIRED, badRequestReason(null));
    }

    @Test
    void aNullSlotIsRejected() {
        var slots = new ArrayList<OpeningSlotTO>();
        slots.add(null);
        assertEquals(OpeningHoursMessages.BODY_REQUIRED, badRequestReason(new OpeningHoursTO(slots, null)));
    }

    @Test
    void theDayMustBeBetween1And7() {
        assertEquals(OpeningHoursMessages.DAY_RANGE, badRequestReason(body(slot(0, "09:00", "10:00"))));
    }

    @Test
    void theDayUpperBoundIsChecked() {
        assertEquals(OpeningHoursMessages.DAY_RANGE, badRequestReason(body(slot(8, "09:00", "10:00"))));
    }

    @Test
    void timesAreRequiredOnAnOpenSlot() {
        assertEquals(OpeningHoursMessages.TIMES_REQUIRED, badRequestReason(body(slot(1, null, "10:00"))));
        assertEquals(OpeningHoursMessages.TIMES_REQUIRED, badRequestReason(body(slot(1, "09:00", null))));
    }

    @Test
    void timesMustBeHhMm() {
        assertEquals(OpeningHoursMessages.TIME_FORMAT, badRequestReason(body(slot(1, "9:00", "10:00"))));
        assertEquals(OpeningHoursMessages.TIME_FORMAT, badRequestReason(body(slot(1, "09:00", "24:00"))));
        assertEquals(OpeningHoursMessages.TIME_FORMAT, badRequestReason(body(slot(1, "09:00", "10:60"))));
        assertEquals(OpeningHoursMessages.TIME_FORMAT, badRequestReason(body(slot(1, "09:00:00", "10:00"))));
    }

    @Test
    void openingMustBeBeforeClosing() {
        assertEquals(OpeningHoursMessages.OPENS_BEFORE_CLOSES, badRequestReason(body(slot(1, "22:00", "02:00"))));
        assertEquals(OpeningHoursMessages.OPENS_BEFORE_CLOSES, badRequestReason(body(slot(1, "10:00", "10:00"))));
    }

    @Test
    void aClosedDayCannotCarryTimesOrOtherSlots() {
        assertEquals(OpeningHoursMessages.CLOSED_WITH_TIMES, badRequestReason(body(new OpeningSlotTO(1, "09:00", null, true))));
        assertEquals(OpeningHoursMessages.CLOSED_MIXED,
                badRequestReason(body(new OpeningSlotTO(1, null, null, true), slot(1, "09:00", "10:00"))));
        assertEquals(OpeningHoursMessages.CLOSED_MIXED,
                badRequestReason(body(new OpeningSlotTO(1, null, null, true), new OpeningSlotTO(1, null, null, true))));
    }

    @Test
    void overlappingSlotsOfTheSameDayAreRejected() {
        assertEquals(OpeningHoursMessages.OVERLAP, badRequestReason(body(slot(2, "09:00", "13:00"), slot(2, "12:00", "15:00"))));
        // unsorted input is sorted before comparing
        assertEquals(OpeningHoursMessages.OVERLAP, badRequestReason(body(slot(2, "15:00", "18:00"), slot(2, "09:00", "16:00"))));
    }

    @Test
    void touchingSlotsAndSameTimesOnDifferentDaysAreAccepted() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        locationExists();
        var response = service.replace(businessId, locationId,
                body(slot(2, "09:00", "13:00"), slot(2, "13:00", "15:00"), slot(3, "09:00", "13:00")), ownerToken);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void atMostFourSlotsPerDay() {
        assertEquals(OpeningHoursMessages.TOO_MANY_SLOTS, badRequestReason(body(slot(1, "06:00", "07:00"), slot(1, "08:00", "09:00"),
                slot(1, "10:00", "11:00"), slot(1, "12:00", "13:00"), slot(1, "14:00", "15:00"))));
    }

    @Test
    void fourSlotsPerDayOnAllSevenDaysIsAccepted() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        locationExists();
        var slots = new ArrayList<OpeningSlotTO>();
        for (int d = 1; d <= 7; d++) {
            slots.add(slot(d, "06:00", "07:00"));
            slots.add(slot(d, "08:00", "09:00"));
            slots.add(slot(d, "10:00", "11:00"));
            slots.add(slot(d, "12:00", "13:00"));
        }
        assertEquals(HttpStatus.OK, service.replace(businessId, locationId, new OpeningHoursTO(slots, null), ownerToken).getStatusCode());
    }

    // ---- get

    @Test
    void getIsPublicAndAnswers404ForAnUnknownLocation() {
        when(locationRepository.findByIdAndBusinessId(locationId, businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.get(businessId, locationId).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.get(null, locationId).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.get(businessId, null).getStatusCode());
    }

    @Test
    void getReturnsTheSlotsWithTheBusinessZone() {
        locationExists();
        var closedDay = new LocationOpeningHoursEntity(locationId, 7, null, null, true);
        when(hoursRepository.findByLocationId(locationId)).thenReturn(List.of(row(1, "09:00", "13:00"), closedDay));
        when(hoursRepository.findTimezoneOfBusiness(businessId)).thenReturn(Optional.of(zone("Europe/Madrid", Duration.ofHours(1))));
        var body = service.get(businessId, locationId).getBody();
        assertEquals("Europe/Madrid", body.timezone());
        assertEquals(2, body.slots().size());
        assertTrue(body.slots().get(1).closed());
        assertNull(body.slots().get(1).opensAt());
    }

    // ---- zone resolution

    @Test
    void theZoneFallsBackToTheFixedOffsetWhenTheNameIsNotIana() {
        when(hoursRepository.findTimezoneOfBusiness(businessId)).thenReturn(Optional.of(zone("Eastern Standard Time", Duration.ofHours(-5))));
        assertEquals("-05:00", service.zoneOf(businessId).getId());
    }

    @Test
    void theZoneFallsBackToTorontoWhenNothingIsUsable() {
        when(hoursRepository.findTimezoneOfBusiness(businessId)).thenReturn(Optional.of(zone("???", null)));
        assertEquals("America/Toronto", service.zoneOf(businessId).getId());
        when(hoursRepository.findTimezoneOfBusiness(businessId)).thenReturn(Optional.of(zone(null, Duration.ofHours(30))));
        assertEquals("America/Toronto", service.zoneOf(businessId).getId());
        when(hoursRepository.findTimezoneOfBusiness(businessId)).thenReturn(Optional.empty());
        assertEquals("America/Toronto", service.zoneOf(businessId).getId());
    }

    // ---- status

    @Test
    void statusAnswers400ForAnInvalidInstantAnd404ForAnUnknownLocation() {
        var bad = service.status(businessId, locationId, "tomorrow");
        assertEquals(HttpStatus.BAD_REQUEST, bad.getStatusCode());
        assertEquals(OpeningHoursMessages.INVALID_AT, bad.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        when(locationRepository.findByIdAndBusinessId(locationId, businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.status(businessId, locationId, null).getStatusCode());
    }

    @Test
    void statusUsesTheClockAndReportsTheNextClosing() {
        locationExists();
        when(hoursRepository.findByLocationId(locationId)).thenReturn(List.of(row(1, "09:00", "21:00")));
        var status = service.status(businessId, locationId, " ").getBody(); // blank = now = Monday 10:00 Toronto
        assertTrue(status.openNow());
        assertEquals("2026-11-23T21:00-05:00", status.closesAt());
        assertNull(status.opensAt());
        assertEquals("America/Toronto", status.timezone());
    }

    @Test
    void statusAtAnInstantReportsTheNextOpeningWhenClosed() {
        locationExists();
        when(hoursRepository.findByLocationId(locationId)).thenReturn(List.of(row(1, "09:00", "21:00")));
        var status = service.status(businessId, locationId, "2026-11-24T01:00:00Z").getBody(); // Monday 20:00 -> open; use Tuesday morning
        assertTrue(status.openNow());
        var closed = service.status(businessId, locationId, "2026-11-24T15:00:00Z").getBody(); // Tuesday: no slots
        assertFalse(closed.openNow());
        assertEquals("2026-11-30T09:00-05:00", closed.opensAt());
        assertNull(closed.closesAt());
    }

    @Test
    void statusWithoutHoursIsClosedWithNoChange() {
        locationExists();
        var status = service.status(businessId, locationId, null).getBody();
        assertFalse(status.openNow());
        assertNull(status.opensAt());
        assertNull(status.closesAt());
    }
}
