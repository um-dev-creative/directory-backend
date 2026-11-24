package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessService;
import com.umdc.directory.api.v1.to.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController implements BusinessApi {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @Override
    public ResponseEntity<BusinessCreateResponse> post(BusinessCreateRequest businessCreateRequest, String sessionToken) {
        return businessService.create(businessCreateRequest, sessionToken);
    }

    @Override
    public ResponseEntity<BusinessTO> findBusiness(UUID id, String sessionToken) {
        return businessService.findById(id, sessionToken);
    }

    @Override
    public ResponseEntity<Set<BusinessTO>> findByUserId(UUID userId, String sessionToken) {
        return businessService.findByUserId(userId, sessionToken);
    }

    @Override
    public ResponseEntity<BusinessUpdateResponse> update(UUID id, BusinessUpdateRequest request, String sessionToken) {
        return businessService.update(id, request, sessionToken);
    }

    @Override
    public ResponseEntity<Void> deleteBusiness(UUID businessId, String sessionToken) {
        return businessService.deleteBusiness(businessId, sessionToken);
    }
}
