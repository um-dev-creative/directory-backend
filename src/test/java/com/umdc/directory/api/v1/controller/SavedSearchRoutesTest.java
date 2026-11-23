package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.SavedSearchService;
import com.umdc.directory.api.v1.to.LastSearchTO;
import com.umdc.directory.api.v1.to.SavedSearchCreateRequest;
import com.umdc.directory.api.v1.to.SavedSearchListResponse;
import com.umdc.directory.api.v1.to.SavedSearchRenameRequest;
import com.umdc.directory.api.v1.to.SavedSearchTO;
import com.umdc.directory.api.v1.to.SearchParamsTO;
import com.umdc.directory.api.v1.to.SearchReplayTO;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SavedSearchRoutesTest {

    private static final String BASE = "/api/v1/users/me";

    @Mock
    private SavedSearchService service;
    private MockMvc mvc;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SavedSearchController(service)).build();
    }

    @Test
    void postReadsTheBodyAndAnswersSnakeCase() throws Exception {
        var params = new SearchParamsTO("pizza", null, null, null, 43.6, -79.3, 1000, null, null);
        when(service.create(eq("tok"), any(SavedSearchCreateRequest.class))).thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                .body(new SavedSearchTO(id, "n", params, null, null, null)));
        mvc.perform(post(BASE + "/saved-searches").header("session-token", "tok").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"n\",\"params\":{\"q\":\"pizza\",\"lat\":43.6,\"lng\":-79.3,\"radius_m\":1000}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.params.radius_m").value(1000))
                .andExpect(jsonPath("$.params.category_id").doesNotExist());
    }

    @Test
    void listWithoutTokenReachesTheServiceWhichAnswers401() throws Exception {
        when(service.list(null)).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        mvc.perform(get(BASE + "/saved-searches")).andExpect(status().isUnauthorized());
        when(service.list("tok")).thenReturn(ResponseEntity.ok(new SavedSearchListResponse(List.of(), 20)));
        mvc.perform(get(BASE + "/saved-searches").header("session-token", "tok"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.max").value(20));
    }

    @Test
    void patchRenames() throws Exception {
        when(service.rename(eq("tok"), eq(id), any(SavedSearchRenameRequest.class)))
                .thenReturn(ResponseEntity.ok(new SavedSearchTO(id, "new", null, null, null, null)));
        mvc.perform(patch(BASE + "/saved-searches/{id}", id).header("session-token", "tok").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("new"));
    }

    @Test
    void deleteAnswers204() throws Exception {
        when(service.delete("tok", id)).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(delete(BASE + "/saved-searches/{id}", id).header("session-token", "tok")).andExpect(status().isNoContent());
    }

    @Test
    void replayReturnsEndpointAndParams() throws Exception {
        when(service.replay("tok", id)).thenReturn(ResponseEntity.ok(new SearchReplayTO("/api/v1/businesses/search", Map.of("q", "pizza"))));
        mvc.perform(get(BASE + "/saved-searches/{id}/replay", id).header("session-token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoint").value("/api/v1/businesses/search"))
                .andExpect(jsonPath("$.params.q").value("pizza"));
    }

    @Test
    void lastSearchGetAndPut() throws Exception {
        when(service.getLast("tok")).thenReturn(ResponseEntity.ok(new LastSearchTO(new SearchParamsTO("q", null, null, null, null, null, null, null, null), null)));
        mvc.perform(get(BASE + "/last-search").header("session-token", "tok"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.params.q").value("q"));
        when(service.putLast(eq("tok"), any(SearchParamsTO.class))).thenReturn(ResponseEntity.ok(new LastSearchTO(null, null)));
        mvc.perform(put(BASE + "/last-search").header("session-token", "tok").contentType(MediaType.APPLICATION_JSON).content("{\"q\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void theApiFallsBackToTheNotImplementedService() {
        var api = new SavedSearchApi() {};
        var all = List.of(
                api.createSavedSearch("t", null).getStatusCode(), api.listSavedSearches("t").getStatusCode(),
                api.renameSavedSearch("t", id, null).getStatusCode(), api.deleteSavedSearch("t", id).getStatusCode(),
                api.replaySavedSearch("t", id).getStatusCode(), api.getLastSearch("t").getStatusCode(),
                api.putLastSearch("t", null).getStatusCode());
        all.forEach(s -> assertEquals(HttpStatus.NOT_IMPLEMENTED, s));
        assertSame(service, new SavedSearchController(service).getService());
    }
}
