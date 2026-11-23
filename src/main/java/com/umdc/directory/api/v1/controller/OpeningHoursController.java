package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.OpeningHoursService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/locations/{locationId}")
public class OpeningHoursController implements OpeningHoursApi {

    private final OpeningHoursService openingHoursService;

    public OpeningHoursController(OpeningHoursService openingHoursService) {
        this.openingHoursService = openingHoursService;
    }

    @Override
    public OpeningHoursService getService() {
        return openingHoursService;
    }
}
