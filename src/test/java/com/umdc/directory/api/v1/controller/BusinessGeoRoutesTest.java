package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessGeoService;
import com.umdc.directory.api.v1.service.BusinessService;
import com.umdc.directory.api.v1.to.BusinessMapResponse;
import com.umdc.directory.api.v1.to.BusinessNearbyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// `/businesses/nearby` and `/businesses/map` must reach the geo controller and not be taken for `/businesses/{id}`.
@ExtendWith(MockitoExtension.class)
class BusinessGeoRoutesTest {

    @Mock
    private BusinessGeoService geoService;
    @Mock
    private BusinessService businessService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new BusinessController(businessService), new BusinessGeoController(geoService)).build();
    }

    @Test
    void nearbyIsRoutedBeforeTheBusinessById() throws Exception {
        when(geoService.nearby("43.6", "-79.3", "1000", "c", "k", "pan", "name", "2", "5"))
                .thenReturn(ResponseEntity.ok(new BusinessNearbyResponse(0, 2, 5, 0, List.of())));
        mvc.perform(get("/api/v1/businesses/nearby").param("lat", "43.6").param("lng", "-79.3").param("radius_m", "1000")
                        .param("category_id", "c").param("city_id", "k").param("q", "pan").param("sort", "name")
                        .param("page", "2").param("per_page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_count").value(0))
                .andExpect(jsonPath("$.per_page").value(5));
        verifyNoInteractions(businessService);
    }

    @Test
    void mapIsRoutedBeforeTheBusinessById() throws Exception {
        when(geoService.map("-80,43,-79,44", "12", null, null, null, "50"))
                .thenReturn(ResponseEntity.ok(new BusinessMapResponse("FeatureCollection", List.of(), false, true, 12)));
        mvc.perform(get("/api/v1/businesses/map").param("bbox", "-80,43,-79,44").param("zoom", "12").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.clustered").value(true))
                .andExpect(jsonPath("$.truncated").value(false));
        verifyNoInteractions(businessService);
    }

    @Test
    void parametersAreOptionalAtTheRouteSoTheServiceCanExplainWhy() throws Exception {
        when(geoService.nearby(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(ResponseEntity.badRequest().header("message-error", "lat is required").build());
        mvc.perform(get("/api/v1/businesses/nearby"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("message-error", "lat is required"));
    }

    @Test
    void theApiFallsBackToTheNotImplementedService() {
        var api = new BusinessGeoApi() {};
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.nearby(null, null, null, null, null, null, null, null, null).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.map(null, null, null, null, null, null).getStatusCode());
        assertSame(geoService, new BusinessGeoController(geoService).getService());
    }
}
