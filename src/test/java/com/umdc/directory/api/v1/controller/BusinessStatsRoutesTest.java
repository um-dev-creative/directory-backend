package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessService;
import com.umdc.directory.api.v1.service.BusinessStatsService;
import com.umdc.directory.api.v1.to.BusinessStatsTO;
import com.umdc.directory.api.v1.to.DailyViewsTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BusinessStatsRoutesTest {

    @Mock
    private BusinessStatsService statsService;
    @Mock
    private BusinessService businessService;

    private MockMvc mvc;
    private final UUID businessId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new BusinessController(businessService), new BusinessStatsController(statsService)).build();
    }

    @Test
    void aViewIsCountedWithThePostAndTheSessionToken() throws Exception {
        when(statsService.recordView(businessId, "tok")).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(post("/api/v1/businesses/{id}/views", businessId).header("session-token", "tok")).andExpect(status().isNoContent());
    }

    @Test
    void theSummaryTakesTheWindowAsAParameter() throws Exception {
        var day = LocalDate.of(2026, 11, 21);
        when(statsService.summary(businessId, 7, "tok")).thenReturn(ResponseEntity.ok(
                new BusinessStatsTO(businessId, day.minusDays(6), day, 5, 2, 5, List.of(new DailyViewsTO(day, 2)))));
        mvc.perform(get("/api/v1/businesses/{id}/stats", businessId).param("days", "7").header("session-token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViews").value(5))
                .andExpect(jsonPath("$.viewsToday").value(2))
                .andExpect(jsonPath("$.daily[0].views").value(2));
    }

    @Test
    void theWindowIsOptional() throws Exception {
        when(statsService.summary(businessId, null, "tok")).thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        mvc.perform(get("/api/v1/businesses/{id}/stats", businessId).header("session-token", "tok")).andExpect(status().isForbidden());
    }

    @Test
    void theSessionTokenHeaderIsRequired() throws Exception {
        mvc.perform(post("/api/v1/businesses/{id}/views", businessId)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/businesses/{id}/stats", businessId)).andExpect(status().isBadRequest());
        verifyNoInteractions(statsService);
    }

    @Test
    void theApiFallsBackToTheNotImplementedService() {
        var api = new BusinessStatsApi() {};
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.recordView(businessId, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.summary(businessId, 7, "t").getStatusCode());
        assertSame(statsService, new BusinessStatsController(statsService).getService());
        verifyNoInteractions(businessService);
    }
}
