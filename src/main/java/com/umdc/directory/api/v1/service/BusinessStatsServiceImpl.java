package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessStatsTO;
import com.umdc.directory.api.v1.to.DailyViewsTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessViewDailyEntity;
import com.umdc.directory.jpa.entity.BusinessViewDailyId;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.BusinessViewDailyRepository;
import com.umdc.directory.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BusinessStatsServiceImpl implements BusinessStatsService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessStatsServiceImpl.class);

    private final BusinessViewDailyRepository viewRepository;
    private final BusinessRepository businessRepository;
    private final Clock clock;

    @Autowired
    public BusinessStatsServiceImpl(BusinessViewDailyRepository viewRepository, BusinessRepository businessRepository) {
        this(viewRepository, businessRepository, Clock.systemUTC());
    }

    /// For tests: the day is taken from the clock (always read in UTC).
    BusinessStatsServiceImpl(BusinessViewDailyRepository viewRepository, BusinessRepository businessRepository, Clock clock) {
        this.viewRepository = viewRepository;
        this.businessRepository = businessRepository;
        this.clock = clock;
    }

    /// Not transactional as a whole on purpose: the `UPDATE`, the `INSERT` and the retry each run in their own transaction, so a
    /// concurrent first view of the day (unique violation on the insert) cannot leave the others in an aborted transaction.
    @Override
    public ResponseEntity<Void> recordView(UUID businessId, String token) {
        var owner = Objects.isNull(businessId) ? Optional.<UUID>empty() : businessRepository.findOwnerId(businessId);
        if (owner.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER,
                    DirectoryAppConstants.BUSINESS_NOT_FOUND).build();
        }
        var viewer = JwtUtil.getUidFromToken(token);
        if (viewer != null && viewer.equals(owner.get())) {
            return ResponseEntity.noContent().build();
        }
        var today = today();
        if (viewRepository.increment(businessId, today) == 0) {
            try {
                viewRepository.saveAndFlush(new BusinessViewDailyEntity(new BusinessViewDailyId(businessId, today), 1));
            } catch (DataIntegrityViolationException e) {
                logger.debug("First view of {} on {} was counted concurrently; adding to the existing row", businessId, today);
                viewRepository.increment(businessId, today);
            }
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BusinessStatsTO> summary(UUID businessId, Integer days, String token) {
        int window = Objects.isNull(days) ? DirectoryAppConstants.STATS_DEFAULT_DAYS : days;
        if (window < 1 || window > DirectoryAppConstants.STATS_MAX_DAYS) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.STATS_INVALID_DAYS);
        }
        var caller = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(caller)) {
            return error(HttpStatus.UNAUTHORIZED, DirectoryAppConstants.UNAUTHORIZED_MESSAGE);
        }
        var owner = Objects.isNull(businessId) ? Optional.<UUID>empty() : businessRepository.findOwnerId(businessId);
        if (owner.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND);
        }
        if (!caller.equals(owner.get())) {
            return error(HttpStatus.FORBIDDEN, DirectoryAppConstants.STATS_ONLY_OWNER);
        }

        var to = today();
        var from = to.minusDays(window - 1L);
        // the last-7-days figure must not depend on a shorter requested window, so the series read covers both
        var readFrom = window >= DirectoryAppConstants.STATS_SHORT_WINDOW_DAYS ? from : to.minusDays(DirectoryAppConstants.STATS_SHORT_WINDOW_DAYS - 1L);
        Map<LocalDate, Long> viewsByDay = viewRepository.findSeries(businessId, readFrom, to).stream()
                .collect(Collectors.toMap(row -> row.getId().getViewDate(), BusinessViewDailyEntity::getViews));

        List<DailyViewsTO> daily = new ArrayList<>(window);
        long total = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            long views = viewsByDay.getOrDefault(day, 0L);
            daily.add(new DailyViewsTO(day, views));
            total += views;
        }
        long last7 = 0;
        for (long offset = 0; offset < DirectoryAppConstants.STATS_SHORT_WINDOW_DAYS; offset++) {
            last7 += viewsByDay.getOrDefault(to.minusDays(offset), 0L);
        }
        return ResponseEntity.ok(new BusinessStatsTO(businessId, from, to, total, viewsByDay.getOrDefault(to, 0L), last7, daily));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC));
    }

    private static <T> ResponseEntity<T> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, message).build();
    }
}
