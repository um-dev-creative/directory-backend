package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessCreateRequest;
import com.umdc.directory.api.v1.to.BusinessUpdateRequest;
import com.umdc.directory.client.backbone.BackboneClient;
import com.umdc.directory.client.backbone.to.BackboneUserGetResponse;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.UserEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CategoryRepository;
import com.umdc.directory.jpa.repository.DigitalContactRepository;
import com.umdc.directory.mapper.BusinessMapper;
import com.umdc.directory.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("BusinessServiceImpl - owner matrix for create and update (F0-A1)")
class BusinessOwnerChecksTest {

    private BusinessRepository businessRepository;
    private BackboneClient backboneClient;
    private BusinessMapper businessMapper;
    private BusinessServiceImpl service;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID otherId = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        businessRepository = mock(BusinessRepository.class);
        backboneClient = mock(BackboneClient.class);
        businessMapper = mock(BusinessMapper.class);
        service = new BusinessServiceImpl(mock(UserService.class), businessRepository, mock(CategoryRepository.class),
                mock(DigitalContactRepository.class), businessMapper, backboneClient);
    }

    private BusinessEntity ownedBusiness() {
        var entity = new BusinessEntity();
        entity.setId(businessId);
        entity.setName("Old");
        var user = new UserEntity();
        user.setId(ownerId);
        entity.setUserFk(user);
        return entity;
    }

    private BusinessUpdateRequest rename() {
        return new BusinessUpdateRequest(null, "New name", null, null, null, null, null, null);
    }

    // ---- update ----

    @Test
    @DisplayName("update - no token or invalid token -> 401 and the repository is not read")
    void update_withoutToken_unauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.update(businessId, rename(), null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.update(businessId, rename(), "garbage").getStatusCode());
        verifyNoInteractions(businessRepository);
    }

    @Test
    @DisplayName("update - business does not exist -> 404")
    void update_unknownBusiness_notFound() {
        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                service.update(businessId, rename(), TestTokens.forUser(ownerId)).getStatusCode());
    }

    @Test
    @DisplayName("update - another user -> 403 and nothing is changed")
    void update_otherUser_forbidden() {
        var entity = ownedBusiness();
        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(entity));

        var response = service.update(businessId, rename(), TestTokens.forUser(otherId));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Old", entity.getName());
        verify(businessRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - business without owner -> 403")
    void update_businessWithoutOwner_forbidden() {
        var entity = ownedBusiness();
        entity.setUserFk(null);
        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(entity));

        assertEquals(HttpStatus.FORBIDDEN,
                service.update(businessId, rename(), TestTokens.forUser(ownerId)).getStatusCode());
    }

    @Test
    @DisplayName("update - owner -> 200 and the change is persisted")
    void update_owner_ok() {
        var entity = ownedBusiness();
        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(entity));
        when(businessRepository.findByName("New name")).thenReturn(Optional.empty());
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.update(businessId, rename(), TestTokens.forUser(ownerId));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("New name", entity.getName());
    }

    // ---- create ----

    private BusinessCreateRequest createRequest(UUID userId) {
        return new BusinessCreateRequest("Biz", "Desc", UUID.randomUUID(), userId, "a@b.com", null, null, null);
    }

    private BackboneUserGetResponse backboneUser(UUID id) {
        return new BackboneUserGetResponse(id, "alias", "n", "u@e.com", "d", LocalDateTime.now(), LocalDateTime.now(),
                true, true, true, true, null, List.of(), List.of());
    }

    @Test
    @DisplayName("create - no or invalid token -> 401")
    void create_withoutToken_unauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.create(createRequest(ownerId), null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.create(createRequest(ownerId), "garbage").getStatusCode());
        verifyNoInteractions(businessRepository);
    }

    @Test
    @DisplayName("create - body userId different from the token user -> 403, nothing saved")
    void create_impersonation_forbidden() {
        var response = service.create(createRequest(otherId), TestTokens.forUser(ownerId));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(businessRepository);
        verifyNoInteractions(backboneClient);
    }

    @Test
    @DisplayName("create - all-zero userId is still a bad request")
    void create_zeroUserId_badRequest() {
        var zero = new UUID(0L, 0L);
        assertEquals(HttpStatus.BAD_REQUEST,
                service.create(createRequest(zero), TestTokens.forUser(ownerId)).getStatusCode());
    }

    @Test
    @DisplayName("create - body without userId uses the token user as owner")
    void create_noBodyUserId_usesTokenUser() {
        when(businessRepository.findByName("Biz")).thenReturn(Optional.empty());
        when(backboneClient.findUserById(ownerId)).thenReturn(backboneUser(ownerId));
        var entity = new BusinessEntity();
        when(businessMapper.toSource(any(BusinessCreateRequest.class))).thenReturn(entity);
        when(businessRepository.save(entity)).thenReturn(entity);

        var response = service.create(createRequest(null), TestTokens.forUser(ownerId));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var captor = ArgumentCaptor.forClass(BusinessCreateRequest.class);
        verify(businessMapper).toSource(captor.capture());
        assertNotNull(captor.getValue().userId());
        assertEquals(ownerId, captor.getValue().userId());
        verify(backboneClient).findUserById(ownerId);
    }

    @Test
    @DisplayName("create - body userId equal to the token user -> 201")
    void create_matchingUserId_created() {
        var request = createRequest(ownerId);
        when(businessRepository.findByName("Biz")).thenReturn(Optional.empty());
        when(backboneClient.findUserById(ownerId)).thenReturn(backboneUser(ownerId));
        var entity = new BusinessEntity();
        when(businessMapper.toSource(request)).thenReturn(entity);
        when(businessRepository.save(entity)).thenReturn(entity);

        assertEquals(HttpStatus.CREATED, service.create(request, TestTokens.forUser(ownerId)).getStatusCode());
    }
}
