package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessCreateRequest;
import com.umdc.directory.api.v1.to.BusinessCreateResponse;
import com.umdc.directory.api.v1.to.BusinessTO;
import com.umdc.directory.client.backbone.BackboneClient;
import com.umdc.directory.client.backbone.to.BackboneUserGetResponse;
import com.umdc.directory.config.ModerationSettings;
import com.umdc.directory.constant.ModerationStatus;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CategoryRepository;
import com.umdc.directory.jpa.repository.DigitalContactRepository;
import com.umdc.directory.mapper.BusinessMapper;
import com.umdc.directory.support.TestTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/// S8: who sees a business that is not approved, and with which status a new business starts.
class BusinessModerationVisibilityTest {

    private final BusinessRepository repository = mock(BusinessRepository.class);
    private final BusinessMapper mapper = mock(BusinessMapper.class);
    private final BackboneClient backbone = mock(BackboneClient.class);
    private BusinessServiceImpl service;
    private final UUID owner = UUID.randomUUID();
    private final UUID business = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BusinessServiceImpl(mock(UserService.class), repository, mock(CategoryRepository.class),
                mock(DigitalContactRepository.class), mapper, backbone, new ContactTypeProvider(backbone, 600));
        ReflectionTestUtils.setField(service, "initialRoleId", UUID.randomUUID().toString());
        ReflectionTestUtils.setField(service, "applicationId", UUID.randomUUID());
    }

    @AfterEach
    void reset() {
        ModerationSettings.configure(false, "LH_ADMIN", "roles");
    }

    private BusinessEntity entity(ModerationStatus status) {
        var e = new BusinessEntity();
        e.setId(business);
        e.setUserFk(owner);
        e.setModerationStatus(status);
        return e;
    }

    private void whenDetail(BusinessEntity entity) {
        when(repository.findBusinessWithDigitalContactsById(business)).thenReturn(Optional.of(entity));
        when(mapper.toBusinessTO(any(BusinessEntity.class), any())).thenReturn(mock(BusinessTO.class));
    }

    @Test
    void moderationOffShowsEveryStatusToEveryone() {
        whenDetail(entity(ModerationStatus.REJECTED));

        assertEquals(HttpStatus.OK, service.findById(business, null).getStatusCode());
        assertEquals(HttpStatus.OK, service.findById(business).getStatusCode());
    }

    @Test
    void moderationOnHidesAPendingBusinessFromEveryoneButItsOwner() {
        ModerationSettings.configure(true, "LH_ADMIN", "roles");
        whenDetail(entity(ModerationStatus.PENDING));

        assertEquals(HttpStatus.NOT_FOUND, service.findById(business, null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.findById(business, "garbage").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.findById(business, TestTokens.forUser(UUID.randomUUID())).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.findById(business).getStatusCode());
        assertEquals(HttpStatus.OK, service.findById(business, TestTokens.forUser(owner)).getStatusCode());
    }

    @Test
    void moderationOnHidesARejectedBusinessAndShowsAnApprovedOne() {
        ModerationSettings.configure(true, "LH_ADMIN", "roles");
        whenDetail(entity(ModerationStatus.REJECTED));
        assertEquals(HttpStatus.NOT_FOUND, service.findById(business, null).getStatusCode());

        whenDetail(entity(ModerationStatus.APPROVED));
        assertEquals(HttpStatus.OK, service.findById(business, null).getStatusCode());
    }

    @Test
    void theListOfAUserShowsOnlyApprovedToOthersAndEverythingToTheOwner() {
        ModerationSettings.configure(true, "LH_ADMIN", "roles");
        var approved = entity(ModerationStatus.APPROVED);
        var pending = entity(ModerationStatus.PENDING);
        pending.setId(UUID.randomUUID());
        when(repository.findByUserEntityFk(owner)).thenReturn(Set.of(approved, pending));
        var approvedTO = mock(BusinessTO.class);
        var pendingTO = mock(BusinessTO.class);
        when(mapper.toBusinessTO(eq(approved), any())).thenReturn(approvedTO);
        when(mapper.toBusinessTO(eq(pending), any())).thenReturn(pendingTO);

        assertEquals(Set.of(approvedTO), service.findByUserId(owner, null).getBody());
        assertEquals(Set.of(approvedTO), service.findByUserId(owner, TestTokens.forUser(UUID.randomUUID())).getBody());
        assertEquals(Set.of(approvedTO, pendingTO), service.findByUserId(owner, TestTokens.forUser(owner)).getBody());
        assertEquals(Set.of(approvedTO), service.findByUserId(owner).getBody());
    }

    @Test
    void aNewBusinessIsPendingWhenModerationIsOnAndApprovedWhenOff() {
        var category = UUID.randomUUID();
        var request = new BusinessCreateRequest("Name", "d", category, owner, null, null, null, null);
        var backboneUser = new BackboneUserGetResponse(owner, "a", null, "a@x.com", "A", null, null, true, false, false, false, null, List.of(), List.of());
        when(backbone.findUserById(owner)).thenReturn(backboneUser);
        when(repository.findByName("Name")).thenReturn(Optional.empty());
        when(mapper.toSource(request)).thenAnswer(inv -> entity(ModerationStatus.APPROVED));
        when(repository.save(any(BusinessEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toBusinessCreateResponse(any())).thenReturn(mock(BusinessCreateResponse.class));
        var saved = org.mockito.ArgumentCaptor.forClass(BusinessEntity.class);

        ModerationSettings.configure(true, "LH_ADMIN", "roles");
        assertEquals(HttpStatus.CREATED, service.create(request, TestTokens.forUser(owner)).getStatusCode());
        ModerationSettings.configure(false, "LH_ADMIN", "roles");
        assertEquals(HttpStatus.CREATED, service.create(request, TestTokens.forUser(owner)).getStatusCode());

        verify(repository, times(2)).save(saved.capture());
        assertEquals(ModerationStatus.PENDING, saved.getAllValues().get(0).getModerationStatus());
        assertEquals(ModerationStatus.APPROVED, saved.getAllValues().get(1).getModerationStatus());
    }
}
