package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessModerationService;
import com.umdc.directory.api.v1.service.BusinessService;
import com.umdc.directory.api.v1.to.BusinessModerationTO;
import com.umdc.directory.api.v1.to.VerificationConfirmRequest;
import com.umdc.directory.constant.ModerationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BusinessModerationRoutesTest {

    @Mock
    private BusinessModerationService moderation;
    @Mock
    private BusinessService businessService;

    private MockMvc mvc;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new BusinessController(businessService), new BusinessModerationController(moderation)).build();
    }

    @Test
    void requestIsAPostWithTheSessionToken() throws Exception {
        when(moderation.requestVerification(id, "tok")).thenReturn(ResponseEntity.accepted().build());
        mvc.perform(post("/api/v1/businesses/{id}/verification/request", id).header("session-token", "tok")).andExpect(status().isAccepted());
    }

    @Test
    void confirmTakesTheCodeInTheBody() throws Exception {
        when(moderation.confirmVerification(id, new VerificationConfirmRequest("123456789"), "tok")).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(post("/api/v1/businesses/{id}/verification/confirm", id).header("session-token", "tok")
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"123456789\"}")).andExpect(status().isNoContent());
    }

    @Test
    void statusIsAGetOfTheModerationResource() throws Exception {
        when(moderation.status(id, "tok")).thenReturn(ResponseEntity.ok(new BusinessModerationTO(id, ModerationStatus.PENDING, false)));
        mvc.perform(get("/api/v1/businesses/{id}/moderation", id).header("session-token", "tok"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    void approveAndRejectArePosts() throws Exception {
        when(moderation.approve(id, "tok")).thenReturn(ResponseEntity.ok(new BusinessModerationTO(id, ModerationStatus.APPROVED, false)));
        when(moderation.reject(id, "tok")).thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        mvc.perform(post("/api/v1/businesses/{id}/moderation/approve", id).header("session-token", "tok"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/businesses/{id}/moderation/reject", id).header("session-token", "tok")).andExpect(status().isForbidden());
    }

    @Test
    void theDetailAndTheUserListPassTheOptionalSessionToken() throws Exception {
        when(businessService.findById(id, "tok")).thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        when(businessService.findByUserId(id, null)).thenReturn(ResponseEntity.ok(Set.of()));
        mvc.perform(get("/api/v1/businesses/{id}", id).header("session-token", "tok")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/businesses/user/{id}", id)).andExpect(status().isOk());
        verify(businessService).findByUserId(id, null);
    }

    @Test
    void theDefaultServiceOfTheApiAnswersNotImplemented() {
        var api = new BusinessModerationApi() {
        };
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.requestVerification(id, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.confirmVerification(id, new VerificationConfirmRequest("1"), "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.status(id, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.approve(id, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.reject(id, "t").getStatusCode());
        var service = new BusinessModerationService() {
        };
        assertEquals(HttpStatus.NOT_IMPLEMENTED, service.requestVerification(id, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, service.approve(id, "t").getStatusCode());
    }
}
