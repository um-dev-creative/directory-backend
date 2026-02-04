package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.BusinessCreateRequest;
import com.umdev.directory.api.v1.to.BusinessCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessServiceTest {

    private final BusinessService businessService = new BusinessService() {};

    @Test
    @DisplayName("Create business successfully")
    void createBusinessSuccessfully() {
        BusinessCreateRequest request = new BusinessCreateRequest(
                "Example Business",
                "This is an example business description.",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@email.ext",
                "user@email.ext",
                "user@email.ext",
                "www.example.com"
                );

        ResponseEntity<BusinessCreateResponse> response = businessService.create(request);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("Create business with null request")
    void createBusinessWithNullRequest() {
        ResponseEntity<BusinessCreateResponse> response = businessService.create(null);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }
}
