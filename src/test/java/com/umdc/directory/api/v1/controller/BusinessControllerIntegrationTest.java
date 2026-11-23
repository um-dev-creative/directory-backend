package com.umdc.directory.api.v1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umdc.directory.api.v1.service.BusinessService;
import com.umdc.directory.api.v1.to.BusinessUpdateRequest;
import com.umdc.directory.api.v1.to.BusinessUpdateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BusinessControllerIntegrationTest {

    @Mock
    private BusinessService businessService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BusinessController(businessService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("PATCH business creates digital contact using existing contact type")
    void patchBusiness_usesManagedContactTypeReference() throws Exception {
        UUID businessId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                null,
                null,
                null,
                null,
                "support@example.com",
                null,
                null,
                null
        );

        when(businessService.update(eq(businessId), any(BusinessUpdateRequest.class), any()))
                .thenReturn(ResponseEntity.ok(new BusinessUpdateResponse(LocalDateTime.now())));

        mockMvc.perform(
                        patch("/api/v1/businesses/{id}", businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("session-token", "t")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH business returns BAD_REQUEST when contact type does not exist")
    void patchBusiness_missingContactType_returnsBadRequest() throws Exception {
        UUID businessId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                null,
                null,
                null,
                null,
                "support@example.com",
                null,
                null,
                null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("message", "Contact type not found for id 00000000-0000-0000-0000-000000000011");

        when(businessService.update(eq(businessId), any(BusinessUpdateRequest.class), any()))
                .thenReturn(new ResponseEntity<>(null, headers, HttpStatus.BAD_REQUEST));

        mockMvc.perform(
                        patch("/api/v1/businesses/{id}", businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("session-token", "t")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(header().string("message", "Contact type not found for id 00000000-0000-0000-0000-000000000011"));
    }
}
