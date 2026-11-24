package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessModerationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{id}")
public class BusinessModerationController implements BusinessModerationApi {

    private final BusinessModerationService businessModerationService;

    public BusinessModerationController(BusinessModerationService businessModerationService) {
        this.businessModerationService = businessModerationService;
    }

    @Override
    public BusinessModerationService getService() {
        return businessModerationService;
    }
}
