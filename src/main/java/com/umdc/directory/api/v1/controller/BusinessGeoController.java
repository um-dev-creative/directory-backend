package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessGeoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// `/businesses/nearby` and `/businesses/map` are literal paths: they win over `/businesses/{id}`.
@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessGeoController implements BusinessGeoApi {

    private final BusinessGeoService service;

    public BusinessGeoController(BusinessGeoService service) {
        this.service = service;
    }

    @Override
    public BusinessGeoService getService() {
        return service;
    }
}
