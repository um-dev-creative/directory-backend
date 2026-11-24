package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.FavoriteListItemRequest;
import com.umdc.directory.api.v1.to.FavoriteListNameRequest;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.constant.FavoriteListConstants;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CampaignEntity;
import com.umdc.directory.jpa.entity.FavoriteListEntity;
import com.umdc.directory.jpa.entity.FavoriteListItemEntity;
import com.umdc.directory.jpa.entity.ProductEntity;
import com.umdc.directory.jpa.entity.UserFavoriteEntity;
import com.umdc.directory.jpa.repository.FavoriteListItemRepository;
import com.umdc.directory.jpa.repository.FavoriteListRepository;
import com.umdc.directory.jpa.repository.UserFavoriteRepository;
import com.umdc.directory.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteListServiceImplTest {

    @Mock
    private FavoriteListRepository lists;
    @Mock
    private FavoriteListItemRepository items;
    @Mock
    private UserFavoriteRepository favorites;

    private FavoriteListServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private final String token = TestTokens.forUser(userId);
    private final UUID listId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FavoriteListServiceImpl(lists, items, favorites);
    }

    private static String reason(ResponseEntity<?> r) {
        return r.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER);
    }

    private FavoriteListEntity stored(String name) {
        var e = new FavoriteListEntity();
        e.setId(listId);
        e.setUserFk(userId);
        e.setName(name);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    private UserFavoriteEntity storeFavorite(UUID owner, boolean active) {
        var b = new BusinessEntity();
        b.setId(UUID.randomUUID());
        b.setName("Cafe");
        var f = new UserFavoriteEntity(owner);
        f.setId(UUID.randomUUID());
        f.setBusiness(b);
        f.setActive(active);
        return f;
    }

    private UserFavoriteEntity productFavorite() {
        var p = new ProductEntity();
        p.setId(UUID.randomUUID());
        p.setName("Pie");
        var f = new UserFavoriteEntity(userId);
        f.setId(UUID.randomUUID());
        f.setProduct(p);
        f.setActive(true);
        return f;
    }

    private void assertUnauthorized(ResponseEntity<?> r) {
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertEquals(DirectoryAppConstants.UNAUTHORIZED_MESSAGE, reason(r));
    }

    @Test
    void everyOperationAnswers401WithoutAValidToken() {
        assertUnauthorized(service.create(null, new FavoriteListNameRequest("a")));
        assertUnauthorized(service.list("bad"));
        assertUnauthorized(service.get(null, listId, 0, 1));
        assertUnauthorized(service.rename(null, listId, null));
        assertUnauthorized(service.delete(null, listId));
        assertUnauthorized(service.addItem(null, listId, null));
        assertUnauthorized(service.removeItem(null, listId, listId));
    }

    @Test
    void createRejectsMissingBodyAndBadNames() {
        assertEquals(FavoriteListConstants.BODY_REQUIRED, reason(service.create(token, null)));
        assertEquals(HttpStatus.BAD_REQUEST, service.create(token, new FavoriteListNameRequest(null)).getStatusCode());
        assertEquals(FavoriteListConstants.INVALID_NAME, reason(service.create(token, new FavoriteListNameRequest("  "))));
        assertEquals(FavoriteListConstants.INVALID_NAME,
                reason(service.create(token, new FavoriteListNameRequest("x".repeat(81)))));
        verify(lists, never()).saveAndFlush(any());
    }

    @Test
    void createEnforcesLimitAndUniqueName() {
        when(lists.countByUserFk(userId)).thenReturn((long) FavoriteListConstants.MAX_PER_USER);
        var full = service.create(token, new FavoriteListNameRequest("a"));
        assertEquals(HttpStatus.CONFLICT, full.getStatusCode());
        assertEquals(FavoriteListConstants.LIMIT_REACHED, reason(full));

        when(lists.countByUserFk(userId)).thenReturn(1L);
        when(lists.existsByUserFkAndNameIgnoreCase(userId, "a")).thenReturn(true);
        var dup = service.create(token, new FavoriteListNameRequest(" a "));
        assertEquals(HttpStatus.CONFLICT, dup.getStatusCode());
        assertEquals(FavoriteListConstants.DUPLICATE_NAME, reason(dup));
    }

    @Test
    void createStoresTheTrimmedNameForTheCaller() {
        when(lists.countByUserFk(userId)).thenReturn(0L);
        when(lists.saveAndFlush(any(FavoriteListEntity.class))).thenAnswer(i -> {
            var e = (FavoriteListEntity) i.getArgument(0);
            e.setId(listId);
            return e;
        });
        var r = service.create(token, new FavoriteListNameRequest("  Dinner "));
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals("Dinner", r.getBody().name());
        assertEquals(0, r.getBody().itemCount());
        assertEquals(listId, r.getBody().id());
        assertNotNull(r.getBody().createdAt());
    }

    @Test
    void createMapsAConstraintViolationTo409() {
        when(lists.saveAndFlush(any(FavoriteListEntity.class))).thenThrow(new DataIntegrityViolationException("dup"));
        var r = service.create(token, new FavoriteListNameRequest("a"));
        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertEquals(FavoriteListConstants.DUPLICATE_NAME, reason(r));
    }

    @Test
    void listReturnsCountsAndZeroForEmptyLists() {
        var other = stored("B");
        other.setId(UUID.randomUUID());
        when(lists.findByUserFkOrderByCreatedAtDesc(userId)).thenReturn(List.of(stored("A"), other));
        var rows = new ArrayList<Object[]>();
        rows.add(new Object[]{listId, 3L});
        when(items.countActiveByLists(any())).thenReturn(rows);
        var body = service.list(token).getBody();
        assertEquals(FavoriteListConstants.MAX_PER_USER, body.max());
        assertEquals(3, body.items().get(0).itemCount());
        assertEquals(0, body.items().get(1).itemCount());
    }

    @Test
    void listWithoutListsDoesNotQueryCounts() {
        when(lists.findByUserFkOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        assertEquals(0, service.list(token).getBody().items().size());
        verify(items, never()).countActiveByLists(any());
    }

    @Test
    void getReturnsAPageOfStoresAndProducts() {
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(stored("A")));
        var storeItem = new FavoriteListItemEntity();
        storeItem.setFavorite(storeFavorite(userId, true));
        storeItem.setCreatedAt(LocalDateTime.now());
        var productItem = new FavoriteListItemEntity();
        productItem.setFavorite(productFavorite());
        when(items.findPageByList(eq(listId), any(Pageable.class)))
                .thenAnswer(i -> new PageImpl<>(List.of(storeItem, productItem), i.<Pageable>getArgument(1), 2));
        var r = service.get(token, listId, 0, 500);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        var page = r.getBody().items();
        assertEquals(2, page.totalCount());
        assertEquals(0, page.page());
        assertEquals(FavoriteListConstants.MAX_PAGE_SIZE, page.perPage());
        assertEquals(1, page.totalPages());
        var first = page.items().iterator().next();
        assertEquals("STORE", first.type());
        assertEquals("Cafe", first.name());
        assertEquals("PRODUCT", page.items().stream().skip(1).findFirst().orElseThrow().type());
        assertEquals("A", r.getBody().name());
    }

    @Test
    void getRejectsUnknownListAndBadPaging() {
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.empty());
        var missing = service.get(token, listId, 0, 10);
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertEquals(FavoriteListConstants.LIST_NOT_FOUND, reason(missing));

        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(stored("A")));
        assertEquals(HttpStatus.BAD_REQUEST, service.get(token, listId, -1, 10).getStatusCode());
        var badSize = service.get(token, listId, 0, 0);
        assertEquals(FavoriteListConstants.INVALID_PAGING, reason(badSize));
    }

    @Test
    void renameChecksOwnerNameAndDuplicates() {
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.rename(token, listId, new FavoriteListNameRequest("n")).getStatusCode());

        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(stored("A")));
        assertEquals(FavoriteListConstants.INVALID_NAME, reason(service.rename(token, listId, null)));
        assertEquals(HttpStatus.BAD_REQUEST, service.rename(token, listId, new FavoriteListNameRequest(" ")).getStatusCode());

        when(lists.existsByUserFkAndNameIgnoreCaseAndIdNot(userId, "B", listId)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, service.rename(token, listId, new FavoriteListNameRequest("B")).getStatusCode());
    }

    @Test
    void renameSavesAndReturnsTheCount() {
        var entity = stored("A");
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(entity));
        when(lists.saveAndFlush(entity)).thenReturn(entity);
        when(items.countActiveByList(listId)).thenReturn(4L);
        var r = service.rename(token, listId, new FavoriteListNameRequest(" New "));
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("New", r.getBody().name());
        assertEquals(4, r.getBody().itemCount());
    }

    @Test
    void renameMapsAConstraintViolationTo409() {
        var entity = stored("A");
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(entity));
        when(lists.saveAndFlush(entity)).thenThrow(new DataIntegrityViolationException("dup"));
        assertEquals(HttpStatus.CONFLICT, service.rename(token, listId, new FavoriteListNameRequest("B")).getStatusCode());
    }

    @Test
    void deleteRemovesItemsAndList() {
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.delete(token, listId).getStatusCode());
        verify(items, never()).deleteByList(any());

        var entity = stored("A");
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(entity));
        assertEquals(HttpStatus.NO_CONTENT, service.delete(token, listId).getStatusCode());
        verify(items).deleteByList(listId);
        verify(lists).delete(entity);
    }

    @Test
    void addItemValidatesListBodyAndFavorite() {
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.empty());
        assertEquals(FavoriteListConstants.LIST_NOT_FOUND,
                reason(service.addItem(token, listId, new FavoriteListItemRequest(UUID.randomUUID()))));

        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(stored("A")));
        assertEquals(FavoriteListConstants.FAVORITE_ID_REQUIRED, reason(service.addItem(token, listId, null)));
        assertEquals(HttpStatus.BAD_REQUEST, service.addItem(token, listId, new FavoriteListItemRequest(null)).getStatusCode());

        var missingId = UUID.randomUUID();
        when(favorites.findById(missingId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.addItem(token, listId, new FavoriteListItemRequest(missingId)).getStatusCode());

        var foreign = storeFavorite(UUID.randomUUID(), true);
        when(favorites.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        var r = service.addItem(token, listId, new FavoriteListItemRequest(foreign.getId()));
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertEquals(FavoriteListConstants.FAVORITE_NOT_FOUND, reason(r));
        verify(items, never()).saveAndFlush(any());
    }

    @Test
    void addItemRejectsInactiveOfferAndDuplicate() {
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(stored("A")));

        var inactive = storeFavorite(userId, false);
        when(favorites.findById(inactive.getId())).thenReturn(Optional.of(inactive));
        var r1 = service.addItem(token, listId, new FavoriteListItemRequest(inactive.getId()));
        assertEquals(HttpStatus.CONFLICT, r1.getStatusCode());
        assertEquals(FavoriteListConstants.FAVORITE_INACTIVE, reason(r1));

        var offer = new UserFavoriteEntity(userId);
        offer.setId(UUID.randomUUID());
        offer.setCampaign(new CampaignEntity());
        offer.setActive(true);
        when(favorites.findById(offer.getId())).thenReturn(Optional.of(offer));
        var r2 = service.addItem(token, listId, new FavoriteListItemRequest(offer.getId()));
        assertEquals(HttpStatus.BAD_REQUEST, r2.getStatusCode());
        assertEquals(FavoriteListConstants.UNSUPPORTED_TYPE, reason(r2));

        var present = storeFavorite(userId, true);
        when(favorites.findById(present.getId())).thenReturn(Optional.of(present));
        when(items.findByListAndFavorite(listId, present.getId())).thenReturn(Optional.of(new FavoriteListItemEntity()));
        var r3 = service.addItem(token, listId, new FavoriteListItemRequest(present.getId()));
        assertEquals(HttpStatus.CONFLICT, r3.getStatusCode());
        assertEquals(FavoriteListConstants.ALREADY_IN_LIST, reason(r3));
    }

    @Test
    void addItemStoresStoreAndProductFavorites() {
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(stored("A")));
        when(items.findByListAndFavorite(any(), any())).thenReturn(Optional.empty());
        when(items.saveAndFlush(any(FavoriteListItemEntity.class))).thenAnswer(i -> i.getArgument(0));

        var store = storeFavorite(userId, true);
        when(favorites.findById(store.getId())).thenReturn(Optional.of(store));
        var r = service.addItem(token, listId, new FavoriteListItemRequest(store.getId()));
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals("STORE", r.getBody().type());
        assertEquals(store.getBusiness().getId(), r.getBody().itemId());

        var product = productFavorite();
        when(favorites.findById(product.getId())).thenReturn(Optional.of(product));
        var p = service.addItem(token, listId, new FavoriteListItemRequest(product.getId()));
        assertEquals("PRODUCT", p.getBody().type());
        assertEquals("Pie", p.getBody().name());
    }

    @Test
    void addItemMapsAConstraintViolationTo409() {
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(stored("A")));
        var store = storeFavorite(userId, true);
        when(favorites.findById(store.getId())).thenReturn(Optional.of(store));
        when(items.findByListAndFavorite(listId, store.getId())).thenReturn(Optional.empty());
        when(items.saveAndFlush(any(FavoriteListItemEntity.class))).thenThrow(new DataIntegrityViolationException("dup"));
        var r = service.addItem(token, listId, new FavoriteListItemRequest(store.getId()));
        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertEquals(FavoriteListConstants.ALREADY_IN_LIST, reason(r));
    }

    @Test
    void removeItemChecksListAndMembership() {
        var favoriteId = UUID.randomUUID();
        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.empty());
        assertEquals(FavoriteListConstants.LIST_NOT_FOUND, reason(service.removeItem(token, listId, favoriteId)));

        when(lists.findByIdAndUserFk(listId, userId)).thenReturn(Optional.of(stored("A")));
        when(items.findByListAndFavorite(listId, favoriteId)).thenReturn(Optional.empty());
        assertEquals(FavoriteListConstants.ITEM_NOT_IN_LIST, reason(service.removeItem(token, listId, favoriteId)));

        var item = new FavoriteListItemEntity();
        when(items.findByListAndFavorite(listId, favoriteId)).thenReturn(Optional.of(item));
        assertEquals(HttpStatus.NO_CONTENT, service.removeItem(token, listId, favoriteId).getStatusCode());
        verify(items).delete(item);
    }

    @Test
    void entitiesExposeTheirFields() {
        var item = new FavoriteListItemEntity();
        var id = UUID.randomUUID();
        var now = LocalDateTime.now();
        var fav = new UserFavoriteEntity(userId);
        item.setId(id);
        item.setListFk(listId);
        item.setFavorite(fav);
        item.setCreatedAt(now);
        assertEquals(id, item.getId());
        assertEquals(listId, item.getListFk());
        assertSame(fav, item.getFavorite());
        assertEquals(now, item.getCreatedAt());
    }
}
