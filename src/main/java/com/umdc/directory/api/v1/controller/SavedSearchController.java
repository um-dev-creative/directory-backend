package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.SavedSearchService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class SavedSearchController implements SavedSearchApi {

    private final SavedSearchService savedSearchService;

    public SavedSearchController(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
    }

    @Override
    public SavedSearchService getService() {
        return savedSearchService;
    }
}
