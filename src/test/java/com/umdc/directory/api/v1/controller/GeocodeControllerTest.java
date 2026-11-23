package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.GeocodeService;
import com.umdc.directory.api.v1.to.GeocodeRequest;
import com.umdc.directory.api.v1.to.GeocodeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GeocodeControllerTest {

    @Mock
    private GeocodeService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new GeocodeController(service)).build();
    }

    @Test
    void postDelegatesAndSerialisesWithoutNullConfidence() throws Exception {
        when(service.geocode(any(), eq("tok"))).thenReturn(ResponseEntity.ok(
                new GeocodeResponse(List.of(new GeocodeResponse.Candidate("L", 43.6, -79.3, null)))));
        mvc.perform(post("/api/v1/geocode").header("session-token", "tok").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"1 Main\",\"city\":\"Toronto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].label").value("L"))
                .andExpect(jsonPath("$.candidates[0].latitude").value(43.6))
                .andExpect(jsonPath("$.candidates[0].confidence").doesNotExist());
        verify(service).geocode(new GeocodeRequest("1 Main", "Toronto", null), "tok");
    }

    @Test
    void missingHeaderReachesTheServiceAs401Path() throws Exception {
        when(service.geocode(any(), isNull())).thenReturn(ResponseEntity.status(401).build());
        mvc.perform(post("/api/v1/geocode").contentType(MediaType.APPLICATION_JSON).content("{\"address\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
