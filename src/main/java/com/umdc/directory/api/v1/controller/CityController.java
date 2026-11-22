package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.CityService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cities")
public class CityController implements CityApi {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @Override
    public CityService getService() {
        return cityService;
    }
}
