package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.BusinessService;
import com.prx.directory.api.v1.to.BusinessCreateRequest;
import com.prx.directory.api.v1.to.BusinessCreateResponse;
import com.prx.directory.api.v1.to.BusinessTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessApiTest {

    @Mock
    private BusinessService businessService;

    @InjectMocks
    private BusinessApi businessApi = new BusinessApi() {};

    @Test
    @DisplayName("Create new business - Success")
    void createNewBusinessSuccess() {
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BusinessCreateRequest request = new BusinessCreateRequest(
                "Business Name",
                "Business Description",
                categoryId,
                userId,
                "email@example.com",
                "customer@example.com",
                "order@example.com",
                "http://example.com"
        );

        ResponseEntity<BusinessCreateResponse> result = businessApi.post(request);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, result.getStatusCode());
    }

    @Test
    @DisplayName("Create new business - Invalid request")
    void createNewBusinessInvalidRequest() {
        BusinessCreateRequest request = null;

        ResponseEntity<BusinessCreateResponse> result = businessApi.post(request);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, result.getStatusCode());
    }

    @Test
    @DisplayName("Find business by ID - Not found")
    void findBusinessByIdNotFound() {
        UUID id = UUID.randomUUID();

        ResponseEntity<BusinessTO> result = businessApi.findBusiness(id);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, result.getStatusCode());
    }

    @Test
    @DisplayName("Find businesses by user ID - Success")
    void findBusinessesByUserIdSuccess() {
        UUID userId = UUID.randomUUID();
        ResponseEntity<Set<BusinessTO>> result = businessApi.findByUserId(userId);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, result.getStatusCode());
    }

}
