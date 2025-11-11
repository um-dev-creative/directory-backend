package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.FavoriteCreateRequest;
import com.prx.directory.api.v1.to.FavoriteResponse;
import com.prx.directory.constant.FavoriteType;
import com.prx.directory.jpa.entity.UserEntity;
import com.prx.directory.jpa.entity.UserFavoriteEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.ProductRepository;
import com.prx.directory.jpa.repository.UserFavoriteRepository;
import com.prx.directory.mapper.FavoriteMapper;
import com.prx.directory.util.JwtUtil;
import com.prx.security.service.SessionJwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class FavoriteServiceImplTest {

    @Mock
    UserFavoriteRepository userFavoriteRepository;
    @Mock
    BusinessRepository businessRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    CampaignRepository campaignRepository;
    @Mock
    SessionJwtService sessionJwtService;
    @Mock
    FavoriteMapper favoriteMapper;

    @InjectMocks
    FavoriteServiceImpl favoriteService;

    UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(sessionJwtService.getUsernameFromToken(anyString())).thenReturn(userId.toString());
    }

    @Test
    @DisplayName("createFavorite should return NOT_FOUND when referenced business missing")
    void createFavoriteShouldReturnNotFoundWhenBusinessMissing() {
        UUID businessId = UUID.randomUUID();
        try (MockedStatic<JwtUtil> mockedStatic = Mockito.mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(businessRepository.existsById(businessId)).thenReturn(false);

            ResponseEntity<FavoriteResponse> resp = favoriteService.createFavorite("token", new FavoriteCreateRequest(FavoriteType.STORE, businessId));

            assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        }
    }

    @Test
    @DisplayName("createFavorite should create and return CREATED when new favorite")
    void createFavoriteShouldReturnCreatedWhenNew() {
        UUID businessId = UUID.randomUUID();
        var userFavoriteId = UUID.randomUUID();
        UserFavoriteEntity saved = new UserFavoriteEntity();
        FavoriteResponse favoriteResponse = new FavoriteResponse(userFavoriteId, FavoriteType.STORE.name(), UUID.randomUUID(), userId, LocalDateTime.now());

        try (MockedStatic<JwtUtil> mockedStatic = Mockito.mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(businessRepository.existsById(businessId)).thenReturn(true);
            when(userFavoriteRepository.findByUserIdAndBusinessId(any(), any())).thenReturn(Optional.empty());

            saved.setId(userFavoriteId);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            // set user so toResponse() can read user id
            var userEntity = new UserEntity();
            userEntity.setId(userId);
            saved.setUser(userEntity);

            when(userFavoriteRepository.save(any())).thenReturn(saved);
            when(favoriteMapper.toResponse(any(UserFavoriteEntity.class))).thenReturn(favoriteResponse);

            ResponseEntity<FavoriteResponse> resp = favoriteService.createFavorite("token", new FavoriteCreateRequest(FavoriteType.STORE, businessId));

            assertEquals(HttpStatus.CREATED, resp.getStatusCode());
            // body not null
            Assertions.assertNotNull(resp.getBody());
            assertEquals(saved.getId(), resp.getBody().id());
        }
    }

    @Test
    @DisplayName("createFavorite should return CONFLICT when favorite already exists")
    void createFavoriteShouldReturnOkWhenExists() {
        UUID businessId = UUID.randomUUID();
        var userFavoriteId = UUID.randomUUID();
        when(businessRepository.existsById(businessId)).thenReturn(true);
        UserFavoriteEntity existing = new UserFavoriteEntity();
        FavoriteResponse favoriteResponse = new FavoriteResponse(userFavoriteId, FavoriteType.STORE.name(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now());
        existing.setId(userFavoriteId);
        existing.setCreatedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        var userEntity = new UserEntity();
        userEntity.setId(userId);
        existing.setUser(userEntity);

        try (MockedStatic<JwtUtil> mockedStatic = Mockito.mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserIdAndBusinessId(any(), any())).thenReturn(Optional.of(existing));
            when(favoriteMapper.toResponse(any(UserFavoriteEntity.class))).thenReturn(favoriteResponse);

            ResponseEntity<FavoriteResponse> resp = favoriteService.createFavorite("token", new FavoriteCreateRequest(FavoriteType.STORE, businessId));

            assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        }
    }
}
