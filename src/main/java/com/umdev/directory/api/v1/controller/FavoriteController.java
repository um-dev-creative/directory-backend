package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.FavoriteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController implements FavoriteApi {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Override
    public FavoriteService getService() {
        return this.favoriteService;
    }
}

