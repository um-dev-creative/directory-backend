package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.GeocodeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/geocode")
public class GeocodeController implements GeocodeApi {

    private final GeocodeService geocodeService;

    public GeocodeController(GeocodeService geocodeService) {
        this.geocodeService = geocodeService;
    }

    @Override
    public GeocodeService getService() {
        return geocodeService;
    }
}
