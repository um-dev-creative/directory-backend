package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.FavoriteListService;
import com.umdc.directory.api.v1.to.FavoriteListDetailTO;
import com.umdc.directory.api.v1.to.FavoriteListItemRequest;
import com.umdc.directory.api.v1.to.FavoriteListItemTO;
import com.umdc.directory.api.v1.to.FavoriteListNameRequest;
import com.umdc.directory.api.v1.to.FavoriteListTO;
import com.umdc.directory.api.v1.to.FavoriteListsResponse;
import com.umdc.directory.api.v1.to.PaginatedResponse;
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

import java.time.LocalDateTime;
import java.util.List;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FavoriteListRoutesTest {

    private static final String BASE = "/api/v1/users/me/favorite-lists";

    @Mock
    private FavoriteListService service;
    private MockMvc mvc;
    private final UUID id = UUID.randomUUID();
    private final UUID favoriteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FavoriteListController(service)).build();
    }

    private FavoriteListTO to(String name, long count) {
        return new FavoriteListTO(id, name, count, null, null);
    }

    @Test
    void postReadsTheBodyAndAnswersSnakeCase() throws Exception {
        when(service.create(eq("tok"), any(FavoriteListNameRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(to("Dinner", 0)));
        mvc.perform(post(BASE).header("session-token", "tok").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Dinner\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.item_count").value(0))
                .andExpect(jsonPath("$.name").value("Dinner"));
    }

    @Test
    void listWithoutTokenReachesTheServiceWhichAnswers401() throws Exception {
        when(service.list(null)).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        when(service.list("tok")).thenReturn(ResponseEntity.ok(new FavoriteListsResponse(List.of(to("A", 2)), 20)));
        mvc.perform(get(BASE).header("session-token", "tok"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.max").value(20))
                .andExpect(jsonPath("$.items[0].item_count").value(2));
    }

    @Test
    void getPassesPagingWithDefaults() throws Exception {
        var item = new FavoriteListItemTO(favoriteId, "STORE", UUID.randomUUID(), "Cafe", LocalDateTime.now());
        var detail = new FavoriteListDetailTO(id, "A", null, null, new PaginatedResponse<>(1, 0, 20, 1, List.of(item)));
        when(service.get("tok", id, 0, 20)).thenReturn(ResponseEntity.ok(detail));
        when(service.get("tok", id, 2, 5)).thenReturn(ResponseEntity.ok(detail));
        mvc.perform(get(BASE + "/{id}", id).header("session-token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.total_count").value(1))
                .andExpect(jsonPath("$.items.items[0].favorite_id").value(favoriteId.toString()))
                .andExpect(jsonPath("$.items.items[0].type").value("STORE"));
        mvc.perform(get(BASE + "/{id}", id).param("page", "2").param("size", "5").header("session-token", "tok"))
                .andExpect(status().isOk());
    }

    @Test
    void patchRenames() throws Exception {
        when(service.rename(eq("tok"), eq(id), any(FavoriteListNameRequest.class))).thenReturn(ResponseEntity.ok(to("New", 1)));
        mvc.perform(patch(BASE + "/{id}", id).header("session-token", "tok").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    void deleteAnswers204() throws Exception {
        when(service.delete("tok", id)).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(delete(BASE + "/{id}", id).header("session-token", "tok")).andExpect(status().isNoContent());
    }

    @Test
    void itemsAreAddedAndRemoved() throws Exception {
        var item = new FavoriteListItemTO(favoriteId, "PRODUCT", UUID.randomUUID(), "Pie", null);
        when(service.addItem(eq("tok"), eq(id), any(FavoriteListItemRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(item));
        mvc.perform(post(BASE + "/{id}/items", id).header("session-token", "tok").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favorite_id\":\"" + favoriteId + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("PRODUCT"));
        when(service.removeItem("tok", id, favoriteId)).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(delete(BASE + "/{id}/items/{f}", id, favoriteId).header("session-token", "tok"))
                .andExpect(status().isNoContent());
    }

    @Test
    void theApiFallsBackToTheNotImplementedService() {
        var api = new FavoriteListApi() {};
        var all = List.of(
                api.createFavoriteList("t", null).getStatusCode(), api.listFavoriteLists("t").getStatusCode(),
                api.getFavoriteList("t", id, 0, 20).getStatusCode(), api.renameFavoriteList("t", id, null).getStatusCode(),
                api.deleteFavoriteList("t", id).getStatusCode(), api.addFavoriteListItem("t", id, null).getStatusCode(),
                api.removeFavoriteListItem("t", id, favoriteId).getStatusCode());
        all.forEach(s -> assertEquals(HttpStatus.NOT_IMPLEMENTED, s));
        var svc = new FavoriteListService() {};
        assertEquals(HttpStatus.NOT_IMPLEMENTED, svc.create("t", null).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, svc.list("t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, svc.get("t", id, 0, 1).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, svc.rename("t", id, null).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, svc.delete("t", id).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, svc.addItem("t", id, null).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, svc.removeItem("t", id, favoriteId).getStatusCode());
        assertSame(service, new FavoriteListController(service).getService());
    }
}
