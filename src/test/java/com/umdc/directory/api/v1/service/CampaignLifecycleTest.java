package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.CampaignListResponse;
import com.umdc.directory.api.v1.to.CampaignTO;
import com.umdc.directory.api.v1.to.CampaignUpdateRequest;
import com.umdc.directory.constant.CampaignStatus;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CampaignEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CampaignRepository;
import com.umdc.directory.jpa.repository.CategoryRepository;
import com.umdc.directory.mapper.CampaignMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.umdc.directory.support.OwnerTokens.forUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// RF-03: type, lifecycle status, deactivate/reactivate/publish, visibility.
class CampaignLifecycleTest {

    private CampaignRepository campaigns;
    private CategoryRepository categories;
    private BusinessRepository businesses;
    private CampaignStatusCounter counter;
    private CampaignServiceImpl service;

    private final UUID owner = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final String ownerToken = forUser(owner);

    @BeforeEach
    void setup() {
        campaigns = Mockito.mock(CampaignRepository.class);
        categories = Mockito.mock(CategoryRepository.class);
        businesses = Mockito.mock(BusinessRepository.class);
        counter = Mockito.mock(CampaignStatusCounter.class);
        when(businesses.findOwnerId(businessId)).thenReturn(Optional.of(owner));
        when(campaigns.save(any(CampaignEntity.class))).thenAnswer(i -> i.getArgument(0));
        service = new CampaignServiceImpl(campaigns, categories, businesses, new CampaignMapperImpl(), counter);
    }

    private CampaignEntity campaign(boolean active, String stored, int startDays, int endDays) {
        var b = new BusinessEntity();
        b.setId(businessId);
        var c = new CampaignEntity();
        c.setId(campaignId);
        c.setTitle("t");
        c.setBusinessFk(b);
        c.setActive(active);
        c.setStatus(stored);
        c.setStartDate(LocalDateTime.now().plusDays(startDays));
        c.setEndDate(LocalDateTime.now().plusDays(endDays));
        c.setLastUpdate(LocalDateTime.now().minusDays(1));
        when(campaigns.findById(campaignId)).thenReturn(Optional.of(c));
        return c;
    }

    // ---- deactivate ----

    @Test
    void deactivateActivePausesIt() {
        CampaignEntity c = campaign(true, null, -1, 5);
        ResponseEntity<CampaignTO> r = service.deactivate(campaignId, ownerToken);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("PAUSED", r.getBody().status());
        assertFalse(c.getActive());
        verify(campaigns).save(c);
    }

    @Test
    void deactivateScheduledPausesIt() {
        campaign(true, null, 2, 5);
        assertEquals("PAUSED", service.deactivate(campaignId, ownerToken).getBody().status());
    }

    @Test
    void deactivateRejectedFromPausedExpiredAndDraft() {
        for (CampaignEntity c : List.of(campaign(false, null, -1, 5), campaign(true, null, -5, -1), campaign(true, "DRAFT", 1, 5))) {
            when(campaigns.findById(campaignId)).thenReturn(Optional.of(c));
            var ex = assertThrows(ResponseStatusException.class, () -> service.deactivate(campaignId, ownerToken));
            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }
        verify(campaigns, never()).save(any());
    }

    @Test
    void lifecycleCallsNeedTokenOwnerAndExistingCampaign() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.deactivate(campaignId, null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.reactivate(campaignId, "bad").getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.publish(campaignId, "").getStatusCode());

        when(campaigns.findById(campaignId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.deactivate(campaignId, ownerToken)).getStatusCode());

        campaign(true, null, -1, 5);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.deactivate(campaignId, forUser(stranger))).getStatusCode());

        when(businesses.findOwnerId(businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.reactivate(campaignId, ownerToken)).getStatusCode());
    }

    @Test
    void campaignWithoutBusinessIsNotFoundForLifecycle() {
        CampaignEntity c = campaign(true, null, -1, 5);
        c.setBusinessFk(null);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.deactivate(campaignId, ownerToken)).getStatusCode());
    }

    // ---- reactivate ----

    @Test
    void reactivatePausedRunningGoesActive() {
        CampaignEntity c = campaign(false, null, -1, 5);
        var r = service.reactivate(campaignId, ownerToken);
        assertEquals("ACTIVE", r.getBody().status());
        assertTrue(c.getActive());
    }

    @Test
    void reactivatePausedNotStartedGoesScheduled() {
        campaign(false, null, 2, 5);
        assertEquals("SCHEDULED", service.reactivate(campaignId, ownerToken).getBody().status());
    }

    @Test
    void reactivateRejectedWhenNotPaused() {
        for (CampaignEntity c : List.of(campaign(true, null, -1, 5), campaign(false, null, -5, -1), campaign(false, "DRAFT", 1, 5))) {
            when(campaigns.findById(campaignId)).thenReturn(Optional.of(c));
            assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                    () -> service.reactivate(campaignId, ownerToken)).getStatusCode());
        }
    }

    // ---- publish ----

    @Test
    void publishDraftRunningGoesActiveAndClearsDraft() {
        CampaignEntity c = campaign(false, "DRAFT", -1, 5);
        var r = service.publish(campaignId, ownerToken);
        assertEquals("ACTIVE", r.getBody().status());
        assertNull(c.getStatus());
        assertTrue(c.getActive());
    }

    @Test
    void publishDraftFutureGoesScheduled() {
        campaign(true, "DRAFT", 3, 9);
        assertEquals("SCHEDULED", service.publish(campaignId, ownerToken).getBody().status());
    }

    @Test
    void publishRejectedWhenNotDraftOrAlreadyExpired() {
        CampaignEntity notDraft = campaign(true, null, -1, 5);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.publish(campaignId, ownerToken)).getStatusCode());
        assertNull(notDraft.getStatus());

        CampaignEntity pastDraft = campaign(true, "DRAFT", -9, -2);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.publish(campaignId, ownerToken)).getStatusCode());
        assertEquals("DRAFT", pastDraft.getStatus());
    }

    // ---- find with visibility ----

    @Test
    void findVisibleToEveryoneWhenActive() {
        campaign(true, null, -1, 5);
        assertEquals(HttpStatus.OK, service.find(campaignId, null).getStatusCode());
        assertEquals("ACTIVE", service.find(campaignId, "bad-token").getBody().status());
    }

    @Test
    void findHiddenCampaignOnlyForOwner() {
        campaign(false, null, -1, 5);
        assertEquals(HttpStatus.NOT_FOUND, service.find(campaignId, null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.find(campaignId, forUser(stranger)).getStatusCode());
        assertEquals("PAUSED", service.find(campaignId, ownerToken).getBody().status());
    }

    @Test
    void findHiddenCampaignWithoutBusinessIsNotFound() {
        CampaignEntity c = campaign(false, null, -1, 5);
        c.setBusinessFk(null);
        assertEquals(HttpStatus.NOT_FOUND, service.find(campaignId, ownerToken).getStatusCode());
    }

    @Test
    void findUnknownIsNotFound() {
        when(campaigns.findById(campaignId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.find(campaignId, ownerToken).getStatusCode());
    }

    // ---- list ----

    @Test
    void listWithStatusTypeAndTokenFilters() {
        CampaignEntity paused = campaign(false, null, -1, 5);
        when(campaigns.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(paused)));
        when(counter.countByStatus(any(), any())).thenReturn(new long[]{0, 1, 0});
        ResponseEntity<CampaignListResponse> r = service.list(1, 10, null,
                Map.of("business_id", businessId.toString(), "status", "paused", "type", "event"), ownerToken);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("PAUSED", r.getBody().items().get(0).status());
    }

    @Test
    void listRejectsBadStatusAndType() {
        assertEquals(HttpStatus.BAD_REQUEST, service.list(1, 10, null, Map.of("status", "nope"), null).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.list(1, 10, null, Map.of("type", "nope"), null).getStatusCode());
    }

    // ---- create: type and draft ----

    @Test
    void createDefaultsTypeToPromotion() {
        var r = service.create(new CampaignTO(null, "t", null, LocalDateTime.now(), LocalDateTime.now().plusDays(2),
                catOk(), businessId, null, null, null, null, null, null, null, null), ownerToken);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals("PROMOTION", r.getBody().type());
        assertEquals("ACTIVE", r.getBody().status());
    }

    private UUID catOk() {
        UUID cat = UUID.randomUUID();
        when(categories.existsById(cat)).thenReturn(true);
        return cat;
    }

    @Test
    void createNormalisesTypeAndKeepsDraft() {
        var r = service.create(new CampaignTO(null, "t", null, LocalDateTime.now(), LocalDateTime.now().plusDays(2),
                catOk(), businessId, null, null, null, true, null, "DRAFT", null, "event"), ownerToken);
        assertEquals("EVENT", r.getBody().type());
        assertEquals("DRAFT", r.getBody().status());
    }

    @Test
    void createIgnoresClientSuppliedLifecycleStatus() {
        var r = service.create(new CampaignTO(null, "t", null, LocalDateTime.now(), LocalDateTime.now().plusDays(2),
                catOk(), businessId, null, null, null, true, null, "EXPIRED", null, "ANNOUNCEMENT"), ownerToken);
        assertEquals("ACTIVE", r.getBody().status());
    }

    @Test
    void createRejectsUnknownType() {
        var to = new CampaignTO(null, "t", null, LocalDateTime.now(), LocalDateTime.now().plusDays(2),
                catOk(), businessId, null, null, null, true, null, null, null, "On-line");
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.create(to, ownerToken)).getStatusCode());
    }

    // ---- update: type and transitions, 202 kept ----

    private CampaignUpdateRequest patch(Boolean active, LocalDateTime end, String type) {
        return new CampaignUpdateRequest(null, null, null, end, null, null, active, null, null, null, type);
    }

    @Test
    void updateTypeKeepsAccepted() {
        CampaignEntity c = campaign(true, null, -1, 5);
        var r = service.update(campaignId, patch(null, null, "announcement"), ownerToken);
        assertEquals(HttpStatus.ACCEPTED, r.getStatusCode());
        assertEquals("ANNOUNCEMENT", c.getType());
    }

    @Test
    void updateRejectsUnknownType() {
        campaign(true, null, -1, 5);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.update(campaignId, patch(null, null, "x"), ownerToken)).getStatusCode());
    }

    @Test
    void updateActiveFlagPausesAndResumes() {
        CampaignEntity c = campaign(true, null, -1, 5);
        assertEquals(HttpStatus.ACCEPTED, service.update(campaignId, patch(false, null, null), ownerToken).getStatusCode());
        assertFalse(c.getActive());
        assertEquals(HttpStatus.ACCEPTED, service.update(campaignId, patch(true, null, null), ownerToken).getStatusCode());
    }

    @Test
    void updateCannotReviveAnExpiredCampaign() {
        campaign(true, null, -9, -2);
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.update(campaignId, patch(null, LocalDateTime.now().plusDays(3), null), ownerToken));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void updateCanLetACampaignExpireOrStayExpired() {
        campaign(true, null, -9, 5);
        assertEquals(HttpStatus.ACCEPTED, service.update(campaignId, patch(null, LocalDateTime.now().minusDays(1), null), ownerToken).getStatusCode());
        campaign(true, null, -9, -2);
        assertEquals(HttpStatus.ACCEPTED, service.update(campaignId, patch(false, null, null), ownerToken).getStatusCode());
    }

    @Test
    void statusEnumIsUsedByTheMapper() {
        assertEquals(CampaignStatus.PAUSED.name(), new CampaignMapperImpl().lifecycleStatus(campaign(false, null, -1, 5)));
    }
}
