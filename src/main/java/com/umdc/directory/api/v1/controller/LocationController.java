package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.LocationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/locations")
public class LocationController implements LocationApi {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    public LocationService getService() {
        return locationService;
    }
}
