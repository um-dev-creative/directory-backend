package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessSearchService;
import com.umdc.directory.api.v1.service.BusinessService;
import com.umdc.directory.api.v1.service.CityService;
import com.umdc.directory.api.v1.service.LocationService;
import com.umdc.directory.api.v1.to.BusinessSearchResponse;
import com.umdc.directory.api.v1.to.CityTO;
import com.umdc.directory.api.v1.to.LocationCreateRequest;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.api.v1.to.LocationUpdateRequest;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// The URLs, the parameters and the delegation of the city, location and search controllers. The business controller is registered
/// too: `GET /api/v1/businesses/search` must reach the search and not be taken for `GET /api/v1/businesses/{id}`.
@ExtendWith(MockitoExtension.class)
class LocationAndSearchRoutesTest {

    @Mock
    private BusinessService businessService;
    @Mock
    private BusinessSearchService searchService;
    @Mock
    private LocationService locationService;
    @Mock
    private CityService cityService;

    private MockMvc mvc;
    private final UUID businessId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new BusinessController(businessService), new BusinessSearchController(searchService),
                new LocationController(locationService), new CityController(cityService)).build();
    }

    @Test
    void searchIsRoutedBeforeTheBusinessById() throws Exception {
        var cityId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        when(searchService.search(cityId, "Toronto", categoryId, "pan", 2, 5, null, null, null, null))
                .thenReturn(ResponseEntity.ok(new BusinessSearchResponse(0, 2, 5, 0, List.of())));

        mvc.perform(get("/api/v1/businesses/search").param("city_id", cityId.toString()).param("city", "Toronto")
                        .param("category_id", categoryId.toString()).param("q", "pan").param("page", "2").param("per_page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_count").value(0))
                .andExpect(jsonPath("$.per_page").value(5));
        verifyNoInteractions(businessService);
    }

    @Test
    void searchPassesOpenNowAndThePoint() throws Exception {
        when(searchService.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(true), eq("43.6"), eq("-79.3"), eq("distance")))
                .thenReturn(ResponseEntity.ok(new BusinessSearchResponse(0, 1, 10, 0, List.of())));
        mvc.perform(get("/api/v1/businesses/search").param("open_now", "true").param("lat", "43.6").param("lng", "-79.3")
                        .param("sort", "distance"))
                .andExpect(status().isOk());
    }

    @Test
    void searchParametersAreOptional() throws Exception {
        when(searchService.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(ResponseEntity.ok(new BusinessSearchResponse(0, 1, 10, 0, List.of())));
        mvc.perform(get("/api/v1/businesses/search")).andExpect(status().isOk());
    }

    @Test
    void citiesAreListedWithOptionalFilters() throws Exception {
        when(cityService.list("CA", "tor")).thenReturn(ResponseEntity.ok(List.of(new CityTO(UUID.randomUUID(), "Toronto", "Ontario", "CA"))));
        mvc.perform(get("/api/v1/cities").param("country", "CA").param("q", "tor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Toronto"));
    }

    @Test
    void locationsAreListed() throws Exception {
        when(locationService.list(businessId)).thenReturn(ResponseEntity.ok(List.of()));
        mvc.perform(get("/api/v1/businesses/{id}/locations", businessId)).andExpect(status().isOk());
    }

    @Test
    void aLocationIsCreatedWithTheBodyAndTheSessionToken() throws Exception {
        when(locationService.create(eq(businessId), any(LocationCreateRequest.class), eq("tok"))).thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
        mvc.perform(post("/api/v1/businesses/{id}/locations", businessId).header("session-token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cityId\":\"" + UUID.randomUUID() + "\",\"addressLine\":\"1 Main St\",\"main\":true}"))
                .andExpect(status().isCreated());
        var captor = org.mockito.ArgumentCaptor.forClass(LocationCreateRequest.class);
        verify(locationService).create(eq(businessId), captor.capture(), eq("tok"));
        assertEquals("1 Main St", captor.getValue().addressLine());
        assertEquals(Boolean.TRUE, captor.getValue().main());
    }

    @Test
    void aLocationIsPatchedAndDeleted() throws Exception {
        when(locationService.update(eq(businessId), eq(locationId), any(LocationUpdateRequest.class), eq("tok"))).thenReturn(ResponseEntity.ok().build());
        when(locationService.delete(businessId, locationId, "tok")).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(patch("/api/v1/businesses/{id}/locations/{locationId}", businessId, locationId).header("session-token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/businesses/{id}/locations/{locationId}", businessId, locationId).header("session-token", "tok"))
                .andExpect(status().isNoContent());
    }

    @Test
    void theSessionTokenHeaderIsRequiredToChangeALocation() throws Exception {
        mvc.perform(delete("/api/v1/businesses/{id}/locations/{locationId}", businessId, locationId)).andExpect(status().isBadRequest());
        verifyNoInteractions(locationService);
    }

    @Test
    void theApiInterfacesFallBackToTheNotImplementedServices() {
        assertEquals(HttpStatus.NOT_IMPLEMENTED, new CityApi() {}.list(null, null).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, new BusinessSearchApi() {}.search(null, null, null, null, null, null, null, null, null, null).getStatusCode());
        var locationApi = new LocationApi() {};
        assertEquals(HttpStatus.NOT_IMPLEMENTED, locationApi.list(businessId).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, locationApi.create(businessId, null, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, locationApi.update(businessId, locationId, null, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, locationApi.delete(businessId, locationId, "t").getStatusCode());
        assertSame(cityService, new CityController(cityService).getService());
        assertSame(searchService, new BusinessSearchController(searchService).getService());
        assertSame(locationService, new LocationController(locationService).getService());
    }
}
