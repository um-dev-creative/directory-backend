package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.SavedSearchCreateRequest;
import com.umdc.directory.api.v1.to.SavedSearchRenameRequest;
import com.umdc.directory.api.v1.to.SearchParamsTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.constant.SavedSearchConstants;
import com.umdc.directory.jpa.entity.LastSearchEntity;
import com.umdc.directory.jpa.entity.SavedSearchEntity;
import com.umdc.directory.jpa.repository.LastSearchRepository;
import com.umdc.directory.jpa.repository.SavedSearchRepository;
import com.umdc.directory.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedSearchServiceImplTest {

    @Mock
    private SavedSearchRepository saved;
    @Mock
    private LastSearchRepository last;

    private SavedSearchServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private final String token = TestTokens.forUser(userId);
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SavedSearchServiceImpl(saved, last);
    }

    private static SearchParamsTO text(String q) {
        return new SearchParamsTO(q, null, null, null, null, null, null, null, null);
    }

    private static String reason(ResponseEntity<?> r) {
        return r.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER);
    }

    private SavedSearchEntity stored(String name) {
        var e = new SavedSearchEntity();
        e.setId(UUID.randomUUID());
        e.setUserFk(userId);
        e.setName(name);
        e.setQ("pizza");
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    private void echoSave() {
        when(saved.save(any(SavedSearchEntity.class))).thenAnswer(i -> {
            var e = (SavedSearchEntity) i.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            return e;
        });
    }

    // ---- create

    @Test
    void createStoresTheNormalisedParams() {
        echoSave();
        var p = new SearchParamsTO(" pizza ", categoryId.toString(), null, " Toronto ", 43.65, -79.38, 2000, null, "NAME");
        var r = service.create(token, new SavedSearchCreateRequest("  Near me ", p));
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        var body = r.getBody();
        assertNotNull(body.id());
        assertEquals("Near me", body.name());
        assertEquals("pizza", body.params().q());
        assertEquals(categoryId.toString(), body.params().categoryId());
        assertEquals("Toronto", body.params().city());
        assertEquals(43.65, body.params().lat());
        assertEquals(2000, body.params().radiusM());
        assertEquals("name", body.params().sort());
        assertNull(body.params().bbox());
        assertNotNull(body.createdAt());
        assertNull(body.lastUsedAt());
    }

    @Test
    void createStoresABboxSearch() {
        echoSave();
        var p = new SearchParamsTO(null, null, null, null, null, null, null, " -80,43,-79,44 ", null);
        var r = service.create(token, new SavedSearchCreateRequest("map", p));
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals("-80,43,-79,44", r.getBody().params().bbox());
        assertNull(r.getBody().params().lat());
    }

    @Test
    void createWithPointAndNoRadiusOrSortKeepsThemNull() {
        echoSave();
        var p = new SearchParamsTO(null, null, null, null, 43.0, -79.0, null, null, null);
        var r = service.create(token, new SavedSearchCreateRequest("p", p));
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertNull(r.getBody().params().radiusM());
        assertNull(r.getBody().params().sort());
    }

    @Test
    void createWithOnlyACityNameIsAccepted() {
        echoSave();
        var p = new SearchParamsTO(null, null, null, "Toronto", null, null, null, null, null);
        assertEquals(HttpStatus.CREATED, service.create(token, new SavedSearchCreateRequest("c", p)).getStatusCode());
    }

    @Test
    void createRequiresASession() {
        var r = service.create("bad", new SavedSearchCreateRequest("n", text("q")));
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertEquals(DirectoryAppConstants.UNAUTHORIZED_MESSAGE, reason(r));
        assertEquals(HttpStatus.UNAUTHORIZED, service.create(null, null).getStatusCode());
    }

    @Test
    void createRejectsAMissingBodyNameOrParams() {
        assertEquals(SavedSearchConstants.BODY_REQUIRED, reason(service.create(token, null)));
        assertEquals(SavedSearchConstants.INVALID_NAME, reason(service.create(token, new SavedSearchCreateRequest(null, text("q")))));
        assertEquals(SavedSearchConstants.INVALID_NAME, reason(service.create(token, new SavedSearchCreateRequest("  ", text("q")))));
        assertEquals(SavedSearchConstants.INVALID_NAME, reason(service.create(token, new SavedSearchCreateRequest("n".repeat(81), text("q")))));
        assertEquals(SavedSearchConstants.PARAMS_REQUIRED, reason(service.create(token, new SavedSearchCreateRequest("n", null))));
        verify(saved, never()).save(any());
    }

    @Test
    void createRejectsInvalidParamsWithTheValidatorReason() {
        var bad = new SearchParamsTO(null, null, null, null, 91.0, -79.0, null, null, null);
        var r = service.create(token, new SavedSearchCreateRequest("n", bad));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertEquals("lat must be between -90 and 90", reason(r));
        var badBbox = new SearchParamsTO(null, null, null, null, null, null, null, "1,2,3", null);
        assertEquals("bbox must be minLng,minLat,maxLng,maxLat", reason(service.create(token, new SavedSearchCreateRequest("n", badBbox))));
        var badCat = new SearchParamsTO(null, "x", null, null, null, null, null, null, null);
        assertEquals("category_id must be a UUID", reason(service.create(token, new SavedSearchCreateRequest("n", badCat))));
    }

    @Test
    void createRejectsMeaninglessCombinations() {
        var half = new SearchParamsTO(null, null, null, null, 43.0, null, null, null, null);
        assertEquals(SavedSearchConstants.LAT_LNG_TOGETHER, reason(service.create(token, new SavedSearchCreateRequest("n", half))));
        var other = new SearchParamsTO(null, null, null, null, null, -79.0, null, null, null);
        assertEquals(SavedSearchConstants.LAT_LNG_TOGETHER, reason(service.create(token, new SavedSearchCreateRequest("n", other))));
        var both = new SearchParamsTO(null, null, null, null, 43.0, -79.0, null, "-80,43,-79,44", null);
        assertEquals(SavedSearchConstants.POINT_AND_BBOX, reason(service.create(token, new SavedSearchCreateRequest("n", both))));
        var radius = new SearchParamsTO("q", null, null, null, null, null, 100, null, null);
        assertEquals(SavedSearchConstants.GEO_NEEDS_POINT, reason(service.create(token, new SavedSearchCreateRequest("n", radius))));
        var sort = new SearchParamsTO("q", null, null, null, null, null, null, null, "name");
        assertEquals(SavedSearchConstants.GEO_NEEDS_POINT, reason(service.create(token, new SavedSearchCreateRequest("n", sort))));
        var city = new SearchParamsTO(null, null, null, "c".repeat(101), null, null, null, null, null);
        assertEquals(SavedSearchConstants.CITY_TOO_LONG, reason(service.create(token, new SavedSearchCreateRequest("n", city))));
        assertEquals(SavedSearchConstants.EMPTY_SEARCH, reason(service.create(token, new SavedSearchCreateRequest("n", text(" ")))));
    }

    @Test
    void createRejectsTheLimitAndDuplicateNames() {
        when(saved.countByUserFk(userId)).thenReturn((long) SavedSearchConstants.MAX_PER_USER);
        var r = service.create(token, new SavedSearchCreateRequest("n", text("q")));
        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertEquals(SavedSearchConstants.LIMIT_REACHED, reason(r));

        when(saved.countByUserFk(userId)).thenReturn(19L);
        when(saved.existsByUserFkAndNameIgnoreCase(userId, "n")).thenReturn(true);
        var d = service.create(token, new SavedSearchCreateRequest("n", text("q")));
        assertEquals(HttpStatus.CONFLICT, d.getStatusCode());
        assertEquals(SavedSearchConstants.DUPLICATE_NAME, reason(d));
        verify(saved, never()).save(any());
    }

    // ---- list

    @Test
    void listReturnsOnlyTheCallersRows() {
        var a = stored("a");
        a.setCity("Toronto");
        a.setCategoryId(categoryId);
        when(saved.findByUserFkOrderByCreatedAtDesc(userId)).thenReturn(List.of(a));
        var r = service.list(token);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(1, r.getBody().items().size());
        assertEquals(SavedSearchConstants.MAX_PER_USER, r.getBody().max());
        assertEquals(categoryId.toString(), r.getBody().items().get(0).params().categoryId());
        assertEquals(HttpStatus.UNAUTHORIZED, service.list(null).getStatusCode());
    }

    // ---- rename

    @Test
    void renameChangesTheName() {
        var e = stored("old");
        when(saved.findByIdAndUserFk(e.getId(), userId)).thenReturn(Optional.of(e));
        when(saved.existsByUserFkAndNameIgnoreCaseAndIdNot(userId, "new", e.getId())).thenReturn(false);
        echoSave();
        var r = service.rename(token, e.getId(), new SavedSearchRenameRequest(" new "));
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("new", r.getBody().name());
    }

    @Test
    void renameErrors() {
        var e = stored("old");
        assertEquals(HttpStatus.UNAUTHORIZED, service.rename(null, e.getId(), null).getStatusCode());

        when(saved.findByIdAndUserFk(e.getId(), userId)).thenReturn(Optional.of(e));
        assertEquals(SavedSearchConstants.INVALID_NAME, reason(service.rename(token, e.getId(), null)));
        assertEquals(SavedSearchConstants.INVALID_NAME, reason(service.rename(token, e.getId(), new SavedSearchRenameRequest(" "))));

        when(saved.existsByUserFkAndNameIgnoreCaseAndIdNot(userId, "dup", e.getId())).thenReturn(true);
        var dup = service.rename(token, e.getId(), new SavedSearchRenameRequest("dup"));
        assertEquals(HttpStatus.CONFLICT, dup.getStatusCode());
        assertEquals("old", e.getName());

        var missing = UUID.randomUUID();
        when(saved.findByIdAndUserFk(missing, userId)).thenReturn(Optional.empty());
        var nf = service.rename(token, missing, new SavedSearchRenameRequest("x"));
        assertEquals(HttpStatus.NOT_FOUND, nf.getStatusCode());
        assertEquals(SavedSearchConstants.SAVED_SEARCH_NOT_FOUND, reason(nf));
    }

    // ---- delete

    @Test
    void deleteRemovesTheRow() {
        var e = stored("a");
        when(saved.findByIdAndUserFk(e.getId(), userId)).thenReturn(Optional.of(e));
        assertEquals(HttpStatus.NO_CONTENT, service.delete(token, e.getId()).getStatusCode());
        verify(saved).delete(e);
    }

    @Test
    void deleteOfAnotherUsersRowIs404AndUnauthorizedIs401() {
        var id = UUID.randomUUID();
        when(saved.findByIdAndUserFk(id, userId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.delete(token, id).getStatusCode());
        verify(saved, never()).delete(any());
        assertEquals(HttpStatus.UNAUTHORIZED, service.delete("x", id).getStatusCode());
    }

    // ---- replay

    @Test
    void replayOfATextSearchTargetsSearch() {
        var e = stored("a");
        e.setCategoryId(categoryId);
        e.setCityId(UUID.randomUUID());
        e.setCity("Toronto");
        when(saved.findByIdAndUserFk(e.getId(), userId)).thenReturn(Optional.of(e));
        var r = service.replay(token, e.getId());
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(SavedSearchConstants.ENDPOINT_SEARCH, r.getBody().endpoint());
        assertEquals("pizza", r.getBody().params().get("q"));
        assertEquals("Toronto", r.getBody().params().get("city"));
        assertEquals(categoryId.toString(), r.getBody().params().get("category_id"));
        assertNotNull(e.getLastUsedAt());
        verify(saved).save(e);
    }

    @Test
    void replayOfAPointSearchTargetsNearby() {
        var e = stored("a");
        e.setLat(43.65);
        e.setLng(-79.38);
        e.setRadiusM(1500);
        e.setSort("name");
        when(saved.findByIdAndUserFk(e.getId(), userId)).thenReturn(Optional.of(e));
        var r = service.replay(token, e.getId());
        assertEquals(SavedSearchConstants.ENDPOINT_NEARBY, r.getBody().endpoint());
        assertEquals("43.65", r.getBody().params().get("lat"));
        assertEquals("-79.38", r.getBody().params().get("lng"));
        assertEquals("1500", r.getBody().params().get("radius_m"));
        assertEquals("name", r.getBody().params().get("sort"));
        assertTrue(!r.getBody().params().containsKey("city"));
    }

    @Test
    void replayOfABboxSearchTargetsMap() {
        var e = stored("a");
        e.setBbox("-80,43,-79,44");
        when(saved.findByIdAndUserFk(e.getId(), userId)).thenReturn(Optional.of(e));
        var r = service.replay(token, e.getId());
        assertEquals(SavedSearchConstants.ENDPOINT_MAP, r.getBody().endpoint());
        assertEquals("-80,43,-79,44", r.getBody().params().get("bbox"));
    }

    @Test
    void replayErrors() {
        var id = UUID.randomUUID();
        when(saved.findByIdAndUserFk(id, userId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.replay(token, id).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.replay(null, id).getStatusCode());
        verify(saved, never()).save(any());
    }

    // ---- last search

    @Test
    void getLastReturnsTheStoredSearch() {
        var e = new LastSearchEntity();
        e.setUserFk(userId);
        e.setQ("sushi");
        e.setUpdatedAt(LocalDateTime.now());
        when(last.findById(userId)).thenReturn(Optional.of(e));
        var r = service.getLast(token);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("sushi", r.getBody().params().q());
        assertSame(e.getUpdatedAt(), r.getBody().updatedAt());
    }

    @Test
    void getLastErrors() {
        when(last.findById(userId)).thenReturn(Optional.empty());
        var r = service.getLast(token);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertEquals(SavedSearchConstants.LAST_SEARCH_NOT_FOUND, reason(r));
        assertEquals(HttpStatus.UNAUTHORIZED, service.getLast(null).getStatusCode());
    }

    @Test
    void putLastCreatesTheRowTheFirstTime() {
        when(last.findById(userId)).thenReturn(Optional.empty());
        when(last.save(any(LastSearchEntity.class))).thenAnswer(i -> i.getArgument(0));
        var r = service.putLast(token, text("tacos"));
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("tacos", r.getBody().params().q());
        assertNotNull(r.getBody().updatedAt());
        verify(last).save(org.mockito.ArgumentMatchers.argThat(e -> userId.equals(e.getUserFk())));
    }

    @Test
    void putLastReplacesTheExistingRow() {
        var e = new LastSearchEntity();
        e.setUserFk(userId);
        e.setQ("old");
        e.setLat(1.0);
        e.setLng(1.0);
        when(last.findById(userId)).thenReturn(Optional.of(e));
        when(last.save(any(LastSearchEntity.class))).thenAnswer(i -> i.getArgument(0));
        var r = service.putLast(token, new SearchParamsTO("new", null, null, null, null, null, null, "-80,43,-79,44", null));
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("new", e.getQ());
        assertNull(e.getLat());
        assertEquals("-80,43,-79,44", e.getBbox());
    }

    @Test
    void putLastErrors() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.putLast(null, text("q")).getStatusCode());
        assertEquals(SavedSearchConstants.BODY_REQUIRED, reason(service.putLast(token, null)));
        when(last.findById(userId)).thenReturn(Optional.empty());
        var r = service.putLast(token, new SearchParamsTO(null, null, null, null, 200.0, 0.0, null, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        verify(last, never()).save(eq(null));
    }
}
