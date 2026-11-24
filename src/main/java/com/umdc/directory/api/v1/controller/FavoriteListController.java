package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.FavoriteListService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class FavoriteListController implements FavoriteListApi {

    private final FavoriteListService favoriteListService;

    public FavoriteListController(FavoriteListService favoriteListService) {
        this.favoriteListService = favoriteListService;
    }

    @Override
    public FavoriteListService getService() {
        return favoriteListService;
    }
}
