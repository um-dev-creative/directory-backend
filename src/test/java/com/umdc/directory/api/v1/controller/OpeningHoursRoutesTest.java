package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.OpeningHoursService;
import com.umdc.directory.api.v1.to.OpeningHoursTO;
import com.umdc.directory.api.v1.to.OpeningSlotTO;
import com.umdc.directory.api.v1.to.OpeningStatusTO;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OpeningHoursRoutesTest {

    @Mock
    private OpeningHoursService hoursService;

    private MockMvc mvc;
    private final UUID businessId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();
    private static final String BASE = "/api/v1/businesses/{b}/locations/{l}/hours";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new OpeningHoursController(hoursService)).build();
    }

    @Test
    void getHoursReturnsSnakeCaseJson() throws Exception {
        when(hoursService.get(businessId, locationId)).thenReturn(ResponseEntity.ok(
                new OpeningHoursTO(List.of(new OpeningSlotTO(1, "09:00", "13:00", false)), "America/Toronto")));
        mvc.perform(get(BASE, businessId, locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].day_of_week").value(1))
                .andExpect(jsonPath("$.slots[0].opens_at").value("09:00"))
                .andExpect(jsonPath("$.timezone").value("America/Toronto"));
    }

    @Test
    void putHoursReadsTheBodyAndTheSessionToken() throws Exception {
        when(hoursService.replace(eq(businessId), eq(locationId), any(OpeningHoursTO.class), eq("tok")))
                .thenReturn(ResponseEntity.ok(new OpeningHoursTO(List.of(), "America/Toronto")));
        mvc.perform(put(BASE, businessId, locationId).header("session-token", "tok").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slots\":[{\"day_of_week\":2,\"opens_at\":\"10:00\",\"closes_at\":\"12:00\",\"closed\":false}]}"))
                .andExpect(status().isOk());
    }

    @Test
    void putHoursRequiresTheSessionToken() throws Exception {
        mvc.perform(put(BASE, businessId, locationId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(hoursService);
    }

    @Test
    void statusTakesTheOptionalInstant() throws Exception {
        when(hoursService.status(businessId, locationId, "2026-11-23T15:00:00Z")).thenReturn(
                ResponseEntity.ok(new OpeningStatusTO(true, "2026-11-23T21:00-05:00", null, "America/Toronto")));
        when(hoursService.status(businessId, locationId, null)).thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        mvc.perform(get(BASE + "/status", businessId, locationId).param("at", "2026-11-23T15:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open_now").value(true))
                .andExpect(jsonPath("$.closes_at").value("2026-11-23T21:00-05:00"))
                .andExpect(jsonPath("$.opens_at").doesNotExist());
        mvc.perform(get(BASE + "/status", businessId, locationId)).andExpect(status().isNotFound());
    }

    @Test
    void theApiFallsBackToTheNotImplementedService() {
        var api = new OpeningHoursApi() {};
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.getHours(businessId, locationId).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.putHours(businessId, locationId, null, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.getStatus(businessId, locationId, null).getStatusCode());
        assertSame(hoursService, new OpeningHoursController(hoursService).getService());
    }
}
