package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessEventService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}")
public class BusinessEventController implements BusinessEventApi {

    private final BusinessEventService businessEventService;

    public BusinessEventController(BusinessEventService businessEventService) {
        this.businessEventService = businessEventService;
    }

    @Override
    public BusinessEventService getService() {
        return businessEventService;
    }
}
