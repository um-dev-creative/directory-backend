package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.BusinessService;
import com.prx.directory.api.v1.to.BusinessCreateRequest;
import com.prx.directory.api.v1.to.BusinessCreateResponse;
import com.prx.directory.api.v1.to.BusinessTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController implements BusinessApi {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @Override
    public ResponseEntity<BusinessCreateResponse> post(BusinessCreateRequest businessCreateRequest) {
        return businessService.create(businessCreateRequest);
    }

    @Override
    public ResponseEntity<BusinessTO> findBusiness(UUID id) {
        return businessService.findById(id);
    }

    @Override
    public ResponseEntity<Page<BusinessTO>> findByUserId(UUID userId, Pageable pageable) {
        return businessService.findByUserId(userId, pageable);
    }

    @Override
    public ResponseEntity<Void> deleteBusiness(UUID businessId, String sessionToken) {

        return businessService.deleteBusiness(businessId, sessionToken);
    }
}
