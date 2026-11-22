package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessSearchService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/search")
public class BusinessSearchController implements BusinessSearchApi {

    private final BusinessSearchService businessSearchService;

    public BusinessSearchController(BusinessSearchService businessSearchService) {
        this.businessSearchService = businessSearchService;
    }

    @Override
    public BusinessSearchService getService() {
        return businessSearchService;
    }
}
