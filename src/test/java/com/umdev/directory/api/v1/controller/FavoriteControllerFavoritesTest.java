package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.FavoriteService;
import com.umdev.directory.api.v1.to.FavoritesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class FavoriteControllerFavoritesTest {

    private FavoriteController controller;

    @Mock
    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new FavoriteController(favoriteService);
    }

    @Test
    void delegateGetFavorites() {
        when(favoriteService.getFavorites("token", null, 0, 10, null))
                .thenReturn(ResponseEntity.ok(new FavoritesResponse(null, null, null)));

        var resp = controller.getFavorites("token", null, 0, 10, null);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
    }
}

