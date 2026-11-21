package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessStatsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}")
public class BusinessStatsController implements BusinessStatsApi {

    private final BusinessStatsService businessStatsService;

    public BusinessStatsController(BusinessStatsService businessStatsService) {
        this.businessStatsService = businessStatsService;
    }

    @Override
    public BusinessStatsService getService() {
        return businessStatsService;
    }
}
