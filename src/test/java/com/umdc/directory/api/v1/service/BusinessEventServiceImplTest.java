package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessEventRequestTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEventDailyEntity;
import com.umdc.directory.jpa.entity.BusinessEventDailyId;
import com.umdc.directory.jpa.repository.BusinessEventDailyRepository;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessEventServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T23:30:00Z"), ZoneId.of("Europe/Madrid"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    private static final UUID NIL = BusinessEventDailyId.NO_CAMPAIGN;

    @Mock
    private BusinessEventDailyRepository eventRepository;
    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private CampaignRepository campaignRepository;

    private BusinessEventServiceImpl service;
    private final UUID businessId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BusinessEventServiceImpl(eventRepository, businessRepository, campaignRepository, CLOCK);
    }

    private static String token(UUID uid) {
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"uid\":\"" + uid + "\"}").getBytes(StandardCharsets.UTF_8));
        return "header." + payload + ".signature";
    }

    private static BusinessEventDailyEntity row(UUID business, UUID campaign, String type, LocalDate day, long count) {
        return new BusinessEventDailyEntity(new BusinessEventDailyId(business, campaign, type, day), count);
    }

    // ---- recordEvent

    @Test
    void recordEventRejectsAMissingBodyAndAnInvalidType() {
        assertEquals(HttpStatus.BAD_REQUEST, service.recordEvent(businessId, null, "t").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.recordEvent(businessId, new BusinessEventRequestTO(null, null), "t").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.recordEvent(businessId, new BusinessEventRequestTO(" ", null), "t").getStatusCode());
        var response = service.recordEvent(businessId, new BusinessEventRequestTO("LIKE", null), "t");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(DirectoryAppConstants.EVENT_INVALID_TYPE, response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        verifyNoInteractions(eventRepository, businessRepository);
    }

    @Test
    void recordEventAnswers404WhenTheBusinessDoesNotExist() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.recordEvent(businessId, new BusinessEventRequestTO("CALL", null), "t").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.recordEvent(null, new BusinessEventRequestTO("CALL", null), "t").getStatusCode());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void recordEventAnswers404WhenTheCampaignIsNotTheBusinessOnes() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(campaignRepository.existsByIdAndBusinessFk_Id(campaignId, businessId)).thenReturn(false);
        var response = service.recordEvent(businessId, new BusinessEventRequestTO("WEB", campaignId), token(UUID.randomUUID()));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(DirectoryAppConstants.CAMPAIGN_NOT_FOUND, response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        verifyNoInteractions(eventRepository);
    }

    @Test
    void recordEventIncrementsTheRowOfTodayInUtcWithoutACampaign() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(eventRepository.increment(businessId, NIL, "ROUTE", TODAY)).thenReturn(1);
        var response = service.recordEvent(businessId, new BusinessEventRequestTO("route", null), token(UUID.randomUUID()));
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(eventRepository).increment(businessId, NIL, "ROUTE", TODAY);
        verify(eventRepository, never()).saveAndFlush(any());
        verifyNoInteractions(campaignRepository);
    }

    @Test
    void recordEventWithACampaignAndAnonymousClient() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(campaignRepository.existsByIdAndBusinessFk_Id(campaignId, businessId)).thenReturn(true);
        when(eventRepository.increment(businessId, campaignId, "SAVE", TODAY)).thenReturn(1);
        assertEquals(HttpStatus.NO_CONTENT, service.recordEvent(businessId, new BusinessEventRequestTO("SAVE", campaignId), null).getStatusCode());
        verify(eventRepository).increment(businessId, campaignId, "SAVE", TODAY);
    }

    @Test
    void recordEventDoesNotCountTheOwner() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        assertEquals(HttpStatus.NO_CONTENT, service.recordEvent(businessId, new BusinessEventRequestTO("CALL", null), token(ownerId)).getStatusCode());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void theFirstEventOfTheKeyInsertsTheRowWithOne() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(eventRepository.increment(businessId, NIL, "CALL", TODAY)).thenReturn(0);
        assertEquals(HttpStatus.NO_CONTENT, service.recordEvent(businessId, new BusinessEventRequestTO("CALL", null), null).getStatusCode());
        var captor = ArgumentCaptor.forClass(BusinessEventDailyEntity.class);
        verify(eventRepository).saveAndFlush(captor.capture());
        assertEquals(1, captor.getValue().getEventCount());
        assertEquals(new BusinessEventDailyId(businessId, NIL, "CALL", TODAY), captor.getValue().getId());
        verify(eventRepository, times(1)).increment(businessId, NIL, "CALL", TODAY);
    }

    @Test
    void aConcurrentFirstEventFallsBackToTheAtomicUpdate() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(eventRepository.increment(businessId, NIL, "CALL", TODAY)).thenReturn(0, 1);
        when(eventRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertEquals(HttpStatus.NO_CONTENT, service.recordEvent(businessId, new BusinessEventRequestTO("CALL", null), null).getStatusCode());
        verify(eventRepository, times(2)).increment(businessId, NIL, "CALL", TODAY);
    }

    // ---- summary

    @Test
    void summaryRequiresAValidTokenTheBusinessAndTheOwner() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.summary(businessId, null, null, "garbage").getStatusCode());
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.summary(businessId, null, null, token(ownerId)).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.summary(null, null, null, token(ownerId)).getStatusCode());
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        var forbidden = service.summary(businessId, null, null, token(UUID.randomUUID()));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertEquals(DirectoryAppConstants.EVENTS_ONLY_OWNER, forbidden.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        verifyNoInteractions(eventRepository);
    }

    @Test
    void summaryRejectsAnInvertedOrTooLongRange() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        assertEquals(HttpStatus.BAD_REQUEST, service.summary(businessId, TODAY, TODAY.minusDays(1), token(ownerId)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.summary(businessId, TODAY.minusDays(365), TODAY, token(ownerId)).getStatusCode());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void summaryDefaultsToTheLast30DaysAndFillsZeros() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        var first = TODAY.minusDays(29);
        when(eventRepository.findSeries(businessId, first, TODAY)).thenReturn(List.of());
        var body = service.summary(businessId, null, null, token(ownerId)).getBody();
        assertEquals(first, body.from());
        assertEquals(TODAY, body.to());
        assertEquals(0, body.total());
        assertEquals(30, body.daily().size());
        assertEquals(List.of("CALL", "ROUTE", "SAVE", "WEB"), List.copyOf(body.totalsByType().keySet()));
        assertTrue(body.campaigns().isEmpty());
    }

    @Test
    void summaryAggregatesPerTypeCampaignAndDay() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        var day1 = TODAY.minusDays(1);
        when(eventRepository.findSeries(businessId, day1, TODAY)).thenReturn(List.of(
                row(businessId, NIL, "CALL", day1, 2),
                row(businessId, campaignId, "CALL", day1, 3),
                row(businessId, campaignId, "WEB", TODAY, 4)));
        var body = service.summary(businessId, day1, TODAY, token(ownerId)).getBody();
        assertEquals(9, body.total());
        assertEquals(5, body.totalsByType().get("CALL"));
        assertEquals(4, body.totalsByType().get("WEB"));
        assertEquals(0, body.totalsByType().get("ROUTE"));
        assertEquals(2, body.campaigns().size());
        var none = body.campaigns().stream().filter(c -> c.campaignId() == null).findFirst().orElseThrow();
        assertEquals(2, none.total());
        var camp = body.campaigns().stream().filter(c -> campaignId.equals(c.campaignId())).findFirst().orElseThrow();
        assertEquals(7, camp.total());
        assertEquals(3, camp.byType().get("CALL"));
        assertEquals(2, body.daily().size());
        assertEquals(5, body.daily().get(0).total());
        assertEquals(4, body.daily().get(1).byType().get("WEB"));
        assertNull(null);
    }
}
