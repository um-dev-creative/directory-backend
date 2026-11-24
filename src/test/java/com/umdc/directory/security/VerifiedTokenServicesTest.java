package com.umdc.directory.security;

import com.umdc.directory.api.v1.service.BusinessEventServiceImpl;
import com.umdc.directory.api.v1.service.FavoriteListServiceImpl;
import com.umdc.directory.api.v1.service.FavoriteServiceImpl;
import com.umdc.directory.api.v1.service.SavedSearchServiceImpl;
import com.umdc.directory.api.v1.to.BusinessEventRequestTO;
import com.umdc.directory.api.v1.to.FavoriteListNameRequest;
import com.umdc.directory.jpa.repository.BusinessEventDailyRepository;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CampaignRepository;
import com.umdc.directory.support.TestTokens;
import com.umdc.security.jwt.JwtConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// The authorisation call sites listed in the guide refuse a forged (unsigned/foreign) token when verification is on.
@DisplayName("Services with a verifying SessionTokenVerifier")
class VerifiedTokenServicesTest {

    private SessionTokenVerifier verifier;
    private SessionJwtServiceImpl jwt;
    private final UUID uid = UUID.randomUUID();
    private final String forged = TestTokens.forUser(uid);
    private String good;

    @BeforeEach
    void setUp() {
        var p = new JwtConfigProperties();
        p.setSecret("bG9jYWxEZXZTZWNyZXRLZXlGb3JEaXJlY3RvcnlOb3RGb3JQcm9kdWN0aW9uVXNlMTIzNDU2");
        p.setExpirationMs(60_000L);
        jwt = new SessionJwtServiceImpl(p);
        verifier = new SessionTokenVerifier(jwt, true);
        good = jwt.generateSessionToken("u", Map.of("uid", uid.toString()));
    }

    @Test
    void savedSearchesRefuseAForgedToken() {
        var service = new SavedSearchServiceImpl(null, null, verifier);
        assertEquals(HttpStatus.UNAUTHORIZED, service.list(forged).getStatusCode());
    }

    @Test
    void favoriteListsRefuseAForgedToken() {
        var service = new FavoriteListServiceImpl(null, null, null, verifier);
        assertEquals(HttpStatus.UNAUTHORIZED, service.list(forged).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.create(forged, new FavoriteListNameRequest("x")).getStatusCode());
    }

    @Test
    void favoritesRefuseAForgedToken() {
        var service = new FavoriteServiceImpl(null, null, null, null, null, null, null, null, null, verifier);
        assertEquals(HttpStatus.UNAUTHORIZED, service.getFavorites(forged, null, 0, 10, null).getStatusCode());
    }

    @Test
    void anOwnerClickWithAForgedTokenIsStillCountedAsAnAnonymousClick() {
        var events = mock(BusinessEventDailyRepository.class);
        var businesses = mock(BusinessRepository.class);
        var campaigns = mock(CampaignRepository.class);
        var businessId = UUID.randomUUID();
        when(businesses.findOwnerId(businessId)).thenReturn(Optional.of(uid));
        when(events.increment(any(), any(), any(), any())).thenReturn(1);
        var service = new BusinessEventServiceImpl(events, businesses, campaigns, verifier);
        var request = new BusinessEventRequestTO("CALL", null);

        // genuine owner token: the click is ignored (204, nothing recorded)
        assertEquals(HttpStatus.NO_CONTENT, service.recordEvent(businessId, request, good).getStatusCode());
        // forged owner token: not recognised as the owner, so the click is counted
        org.mockito.Mockito.verify(events, org.mockito.Mockito.never()).increment(any(), any(), any(), any());
        service.recordEvent(businessId, request, forged);
        org.mockito.Mockito.verify(events).increment(any(), any(), any(), any());
    }
}
