package com.umdc.directory.api.v1.service;

import com.umdc.directory.security.SessionTokenVerifier;
import com.umdc.directory.api.v1.to.BusinessEventRequestTO;
import com.umdc.directory.api.v1.to.BusinessEventSummaryTO;
import com.umdc.directory.constant.BusinessEventType;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEventDailyEntity;
import com.umdc.directory.jpa.entity.BusinessEventDailyId;
import com.umdc.directory.jpa.repository.BusinessEventDailyRepository;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CampaignRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Service
public class BusinessEventServiceImpl implements BusinessEventService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessEventServiceImpl.class);

    private final BusinessEventDailyRepository eventRepository;
    private final BusinessRepository businessRepository;
    private final CampaignRepository campaignRepository;
    private final Clock clock;
    private final SessionTokenVerifier sessionTokenVerifier;

    public BusinessEventServiceImpl(BusinessEventDailyRepository eventRepository, BusinessRepository businessRepository,
                                    CampaignRepository campaignRepository) {
        this(eventRepository, businessRepository, campaignRepository, Clock.systemUTC(), SessionTokenVerifier.unverified());
    }

    @Autowired
    public BusinessEventServiceImpl(BusinessEventDailyRepository eventRepository, BusinessRepository businessRepository,
                                    CampaignRepository campaignRepository, SessionTokenVerifier sessionTokenVerifier) {
        this(eventRepository, businessRepository, campaignRepository, Clock.systemUTC(), sessionTokenVerifier);
    }

    /// For tests: the day is taken from the clock (always read in UTC).
    BusinessEventServiceImpl(BusinessEventDailyRepository eventRepository, BusinessRepository businessRepository,
                             CampaignRepository campaignRepository, Clock clock) {
        this(eventRepository, businessRepository, campaignRepository, clock, SessionTokenVerifier.unverified());
    }

    BusinessEventServiceImpl(BusinessEventDailyRepository eventRepository, BusinessRepository businessRepository,
                             CampaignRepository campaignRepository, Clock clock, SessionTokenVerifier sessionTokenVerifier) {
        this.sessionTokenVerifier = Objects.requireNonNullElseGet(sessionTokenVerifier, SessionTokenVerifier::unverified);
        this.eventRepository = eventRepository;
        this.businessRepository = businessRepository;
        this.campaignRepository = campaignRepository;
        this.clock = clock;
    }

    /// Not transactional as a whole on purpose: the `UPDATE`, the `INSERT` and the retry each run in their own transaction, so a
    /// concurrent first event of the day (unique violation on the insert) cannot leave the others in an aborted transaction.
    @Override
    public ResponseEntity<Void> recordEvent(UUID businessId, BusinessEventRequestTO request, String token) {
        if (Objects.isNull(request)) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.EVENT_BODY_REQUIRED);
        }
        var type = BusinessEventType.parse(request.type());
        if (type.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.EVENT_INVALID_TYPE);
        }
        var owner = Objects.isNull(businessId) ? Optional.<UUID>empty() : businessRepository.findOwnerId(businessId);
        if (owner.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND);
        }
        var campaignId = request.campaignId();
        if (campaignId != null && !campaignRepository.existsByIdAndBusinessFk_Id(campaignId, businessId)) {
            return error(HttpStatus.NOT_FOUND, DirectoryAppConstants.CAMPAIGN_NOT_FOUND);
        }
        var actor = sessionTokenVerifier.uid(token);
        if (actor != null && actor.equals(owner.get())) {
            return ResponseEntity.noContent().build();
        }
        var key = campaignId == null ? BusinessEventDailyId.NO_CAMPAIGN : campaignId;
        var name = type.get().name();
        var today = today();
        if (eventRepository.increment(businessId, key, name, today) == 0) {
            try {
                eventRepository.saveAndFlush(new BusinessEventDailyEntity(new BusinessEventDailyId(businessId, key, name, today), 1));
            } catch (DataIntegrityViolationException e) {
                logger.debug("First {} event of {} on {} was counted concurrently; adding to the existing row", name, businessId, today);
                eventRepository.increment(businessId, key, name, today);
            }
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BusinessEventSummaryTO> summary(UUID businessId, LocalDate from, LocalDate to, String token) {
        var denied = ownerCheck(businessId, token);
        if (denied.isPresent()) {
            return error(denied.get(), denied.get() == HttpStatus.UNAUTHORIZED ? DirectoryAppConstants.UNAUTHORIZED_MESSAGE
                    : denied.get() == HttpStatus.NOT_FOUND ? DirectoryAppConstants.BUSINESS_NOT_FOUND : DirectoryAppConstants.EVENTS_ONLY_OWNER);
        }
        var last = Objects.isNull(to) ? today() : to;
        var first = Objects.isNull(from) ? last.minusDays(DirectoryAppConstants.EVENTS_DEFAULT_DAYS - 1L) : from;
        if (first.isAfter(last) || ChronoUnit.DAYS.between(first, last) + 1 > DirectoryAppConstants.EVENTS_MAX_DAYS) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.EVENTS_INVALID_RANGE);
        }

        var totals = zeros();
        ConcurrentMap<UUID, Map<String, Long>> byCampaign = new ConcurrentSkipListMap<>();
        ConcurrentMap<LocalDate, Map<String, Long>> byDay = new ConcurrentSkipListMap<>();
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            byDay.put(day, zeros());
        }
        for (var row : eventRepository.findSeries(businessId, first, last)) {
            var id = row.getId();
            var count = row.getEventCount();
            totals.merge(id.getEventType(), count, Long::sum);
            byCampaign.computeIfAbsent(id.getCampaignId(), k -> zeros()).merge(id.getEventType(), count, Long::sum);
            byDay.get(id.getEventDate()).merge(id.getEventType(), count, Long::sum);
        }
        List<BusinessEventSummaryTO.CampaignEvents> campaigns = new ArrayList<>();
        byCampaign.forEach((campaign, byType) -> campaigns.add(new BusinessEventSummaryTO.CampaignEvents(
                BusinessEventDailyId.NO_CAMPAIGN.equals(campaign) ? null : campaign, sum(byType), byType)));
        List<BusinessEventSummaryTO.DayEvents> daily = new ArrayList<>(byDay.size());
        byDay.forEach((day, byType) -> daily.add(new BusinessEventSummaryTO.DayEvents(day, sum(byType), byType)));
        return ResponseEntity.ok(new BusinessEventSummaryTO(businessId, first, last, sum(totals), totals, campaigns, daily));
    }

    /// @return empty when the token's user owns the business, else the status to answer: 401, 404 or 403
    private Optional<HttpStatus> ownerCheck(UUID businessId, String token) {
        var caller = sessionTokenVerifier.uid(token);
        if (Objects.isNull(caller)) {
            return Optional.of(HttpStatus.UNAUTHORIZED);
        }
        var owner = Objects.isNull(businessId) ? Optional.<UUID>empty() : businessRepository.findOwnerId(businessId);
        if (owner.isEmpty()) {
            return Optional.of(HttpStatus.NOT_FOUND);
        }
        return caller.equals(owner.get()) ? Optional.empty() : Optional.of(HttpStatus.FORBIDDEN);
    }

    private static Map<String, Long> zeros() {
        ConcurrentMap<String, Long> map = new ConcurrentSkipListMap<>();
        for (var type : BusinessEventType.values()) {
            map.put(type.name(), 0L);
        }
        return map;
    }

    private static long sum(Map<String, Long> byType) {
        return byType.values().stream().mapToLong(Long::longValue).sum();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC));
    }

    private static <T> ResponseEntity<T> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, message).build();
    }
}
