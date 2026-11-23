package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessEventService;
import com.umdc.directory.api.v1.to.BusinessEventRequestTO;
import com.umdc.directory.api.v1.to.BusinessEventSummaryTO;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
class BusinessEventRoutesTest {

    @Mock
    private BusinessEventService eventService;

    private MockMvc mvc;
    private final UUID businessId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new BusinessEventController(eventService)).build();
    }

    @Test
    void anEventIsPostedWithTypeCampaignIdAndTheSessionToken() throws Exception {
        when(eventService.recordEvent(businessId, new BusinessEventRequestTO("CALL", campaignId), "tok")).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(post("/api/v1/businesses/{id}/events", businessId).header("session-token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"CALL\",\"campaign_id\":\"" + campaignId + "\",\"ip\":\"x\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void theBodyIsPassedAsNullWhenMissing() throws Exception {
        when(eventService.recordEvent(businessId, null, "tok")).thenReturn(ResponseEntity.badRequest().build());
        mvc.perform(post("/api/v1/businesses/{id}/events", businessId).header("session-token", "tok").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theSummaryTakesTheRangeAsParameters() throws Exception {
        var day = LocalDate.of(2026, 11, 21);
        var zero = Map.of("CALL", 3L);
        when(eventService.summary(businessId, day.minusDays(1), day, "tok")).thenReturn(ResponseEntity.ok(new BusinessEventSummaryTO(
                businessId, day.minusDays(1), day, 3, zero,
                List.of(new BusinessEventSummaryTO.CampaignEvents(campaignId, 3, zero)),
                List.of(new BusinessEventSummaryTO.DayEvents(day, 3, zero)))));
        mvc.perform(get("/api/v1/businesses/{id}/events/summary", businessId).param("from", "2026-11-20").param("to", "2026-11-21")
                        .header("session-token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalsByType.CALL").value(3))
                .andExpect(jsonPath("$.campaigns[0].campaign_id").value(campaignId.toString()))
                .andExpect(jsonPath("$.daily[0].total").value(3));
    }

    @Test
    void theRangeIsOptional() throws Exception {
        when(eventService.summary(businessId, null, null, "tok")).thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        mvc.perform(get("/api/v1/businesses/{id}/events/summary", businessId).header("session-token", "tok")).andExpect(status().isForbidden());
    }

    @Test
    void theSessionTokenHeaderIsRequired() throws Exception {
        mvc.perform(post("/api/v1/businesses/{id}/events", businessId).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"CALL\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/businesses/{id}/events/summary", businessId)).andExpect(status().isBadRequest());
        verifyNoInteractions(eventService);
    }

    @Test
    void theApiFallsBackToTheNotImplementedService() {
        var api = new BusinessEventApi() {};
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.recordEvent(businessId, null, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.summary(businessId, null, null, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, new BusinessEventService() {}.recordEvent(businessId, null, "t").getStatusCode());
        assertSame(eventService, new BusinessEventController(eventService).getService());
    }
}
