package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessStatsTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessViewDailyEntity;
import com.umdc.directory.jpa.entity.BusinessViewDailyId;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.BusinessViewDailyRepository;
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
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessStatsServiceImplTest {

    /// 2026-11-21 23:30 in UTC, which is already the 22nd in a UTC+2 zone: the day must come from UTC.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T23:30:00Z"), ZoneId.of("Europe/Madrid"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

    @Mock
    private BusinessViewDailyRepository viewRepository;
    @Mock
    private BusinessRepository businessRepository;

    private BusinessStatsServiceImpl service;
    private final UUID businessId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final String ownerToken = token(ownerId);

    @BeforeEach
    void setUp() {
        service = new BusinessStatsServiceImpl(viewRepository, businessRepository, CLOCK);
    }

    private static String token(UUID uid) {
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"uid\":\"" + uid + "\"}").getBytes(StandardCharsets.UTF_8));
        return "header." + payload + ".signature";
    }

    private static BusinessViewDailyEntity row(UUID businessId, LocalDate day, long views) {
        return new BusinessViewDailyEntity(new BusinessViewDailyId(businessId, day), views);
    }

    // ---- recordView

    @Test
    void recordViewAnswers404WhenTheBusinessDoesNotExist() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.empty());
        var response = service.recordView(businessId, "anything");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.recordView(null, "anything").getStatusCode());
        verifyNoInteractions(viewRepository);
    }

    @Test
    void recordViewAddsOneToTodayInUtc() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(viewRepository.increment(businessId, TODAY)).thenReturn(1);
        var response = service.recordView(businessId, token(UUID.randomUUID()));
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(viewRepository).increment(businessId, TODAY);
        verify(viewRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordViewCountsAnonymousViewersToo() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(viewRepository.increment(businessId, TODAY)).thenReturn(1);
        assertEquals(HttpStatus.NO_CONTENT, service.recordView(businessId, null).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, service.recordView(businessId, "garbage").getStatusCode());
        verify(viewRepository, times(2)).increment(businessId, TODAY);
    }

    @Test
    void recordViewDoesNotCountTheOwner() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        assertEquals(HttpStatus.NO_CONTENT, service.recordView(businessId, ownerToken).getStatusCode());
        verifyNoInteractions(viewRepository);
    }

    @Test
    void theFirstViewOfTheDayInsertsTheRowWithOneView() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(viewRepository.increment(businessId, TODAY)).thenReturn(0);
        assertEquals(HttpStatus.NO_CONTENT, service.recordView(businessId, null).getStatusCode());
        var captor = ArgumentCaptor.forClass(BusinessViewDailyEntity.class);
        verify(viewRepository).saveAndFlush(captor.capture());
        assertEquals(1, captor.getValue().getViews());
        assertEquals(new BusinessViewDailyId(businessId, TODAY), captor.getValue().getId());
        verify(viewRepository, times(1)).increment(businessId, TODAY);
    }

    @Test
    void aConcurrentFirstViewFallsBackToIncrementingTheExistingRow() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(viewRepository.increment(businessId, TODAY)).thenReturn(0).thenReturn(1);
        when(viewRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));
        assertEquals(HttpStatus.NO_CONTENT, service.recordView(businessId, null).getStatusCode());
        verify(viewRepository, times(2)).increment(businessId, TODAY);
    }

    // ---- summary

    @Test
    void summaryRejectsAWindowOutOfRange() {
        for (int days : new int[]{0, -1, 366}) {
            var response = service.summary(businessId, days, ownerToken);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(DirectoryAppConstants.STATS_INVALID_DAYS, response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        }
        verifyNoInteractions(viewRepository, businessRepository);
    }

    @Test
    void summaryNeedsASessionWithAUid() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.summary(businessId, 30, "garbage").getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.summary(businessId, 30, null).getStatusCode());
    }

    @Test
    void summaryAnswers404ForAnUnknownBusinessAnd403ForANonOwner() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.summary(businessId, 30, ownerToken).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.summary(null, 30, ownerToken).getStatusCode());

        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(UUID.randomUUID()));
        var forbidden = service.summary(businessId, 30, ownerToken);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertEquals(DirectoryAppConstants.STATS_ONLY_OWNER, forbidden.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        verifyNoInteractions(viewRepository);
    }

    @Test
    void summaryFillsTheDaysWithoutViewsWithZeros() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(viewRepository.findSeries(businessId, TODAY.minusDays(9), TODAY)).thenReturn(List.of(
                row(businessId, TODAY.minusDays(9), 4), row(businessId, TODAY.minusDays(2), 3), row(businessId, TODAY, 5)));

        BusinessStatsTO stats = service.summary(businessId, 10, ownerToken).getBody();

        assertEquals(businessId, stats.businessId());
        assertEquals(TODAY.minusDays(9), stats.from());
        assertEquals(TODAY, stats.to());
        assertEquals(10, stats.daily().size());
        assertEquals(TODAY.minusDays(9), stats.daily().get(0).date());
        assertEquals(TODAY, stats.daily().get(9).date());
        assertEquals(4, stats.daily().get(0).views());
        assertEquals(0, stats.daily().get(1).views());
        assertEquals(12, stats.totalViews());
        assertEquals(5, stats.viewsToday());
        assertEquals(8, stats.viewsLast7Days());
    }

    @Test
    void theLastSevenDaysDoNotDependOnAShorterWindow() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(viewRepository.findSeries(businessId, TODAY.minusDays(6), TODAY)).thenReturn(List.of(
                row(businessId, TODAY.minusDays(5), 2), row(businessId, TODAY, 1)));

        BusinessStatsTO stats = service.summary(businessId, 2, ownerToken).getBody();

        assertEquals(2, stats.daily().size());
        assertEquals(1, stats.totalViews());
        assertEquals(1, stats.viewsToday());
        assertEquals(3, stats.viewsLast7Days());
    }

    @Test
    void summaryDefaultsToThirtyDaysAndAcceptsTheLimits() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(ownerId));
        when(viewRepository.findSeries(any(), any(), any())).thenReturn(List.of());

        assertEquals(30, service.summary(businessId, null, ownerToken).getBody().daily().size());
        assertEquals(1, service.summary(businessId, 1, ownerToken).getBody().daily().size());
        var year = service.summary(businessId, 365, ownerToken).getBody();
        assertEquals(365, year.daily().size());
        assertEquals(0, year.totalViews());
        assertTrue(year.daily().stream().allMatch(day -> day.views() == 0));
        assertEquals(ZoneOffset.UTC.getId(), "Z");
    }
}
