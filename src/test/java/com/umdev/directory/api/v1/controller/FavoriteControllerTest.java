package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.FavoriteService;
import com.umdev.directory.api.v1.to.FavoriteCreateRequest;
import com.umdev.directory.api.v1.to.FavoriteResponse;
import com.umdev.directory.api.v1.to.FavoriteUpdateRequest;
import com.umdev.directory.constant.FavoriteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private FavoriteController favoriteController;

    @Test
    @DisplayName("createFavorite should return CREATED when service creates new favorite")
    void createFavoriteShouldReturnCreated() {
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.STORE, UUID.randomUUID());
        FavoriteResponse response = new FavoriteResponse(UUID.randomUUID(), FavoriteType.STORE.name(), request.itemId(), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), true);
        when(favoriteService.createFavorite(any(), any(FavoriteCreateRequest.class))).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(response));

        ResponseEntity<FavoriteResponse> result = favoriteController.createFavorite("token", request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("createFavorite should return BAD_REQUEST when request invalid")
    void createFavoriteShouldReturnBadRequest() {
        when(favoriteService.createFavorite(any(), any())).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        ResponseEntity<FavoriteResponse> result = favoriteController.createFavorite("token", null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("updateFavorite should return BAD_REQUEST when path ID does not match body ID")
    void updateFavoriteShouldReturnBadRequestWhenIdMismatch() {
        UUID pathId = UUID.randomUUID();
        UUID bodyId = UUID.randomUUID();
        FavoriteUpdateRequest request = new FavoriteUpdateRequest(bodyId, null);

        ResponseEntity<FavoriteResponse> result = favoriteController.updateFavorite("token", pathId, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("updateFavorite should call service when path ID matches body ID")
    void updateFavoriteShouldCallServiceWhenIdMatches() {
        UUID id = UUID.randomUUID();
        FavoriteUpdateRequest request = new FavoriteUpdateRequest(id, false);
        FavoriteResponse response = new FavoriteResponse(id, "STORE", UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), false);
        when(favoriteService.updateFavorite(any(), any(FavoriteUpdateRequest.class))).thenReturn(ResponseEntity.ok(response));

        ResponseEntity<FavoriteResponse> result = favoriteController.updateFavorite("token", id, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}
