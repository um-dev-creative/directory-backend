package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.FavoriteCreateRequest;
import com.umdc.directory.api.v1.to.FavoriteResponse;
import com.umdc.directory.api.v1.to.FavoriteUpdateRequest;
import com.umdc.directory.api.v1.to.FavoritesResponse;
import com.umdc.directory.constant.FavoriteType;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CampaignEntity;
import com.umdc.directory.jpa.entity.ProductEntity;
import com.umdc.directory.jpa.entity.UserFavoriteEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CampaignRepository;
import com.umdc.directory.jpa.repository.ProductRepository;
import com.umdc.directory.jpa.repository.UserFavoriteRepository;
import com.umdc.directory.mapper.BusinessMapper;
import com.umdc.directory.mapper.CampaignMapper;
import com.umdc.directory.mapper.FavoriteMapper;
import com.umdc.directory.mapper.ProductMapper;
import com.umdc.directory.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// F0-B favorites: sort, database paging, update by type, reactivation and duplicate handling.
class FavoriteServiceImplF0Test {

    private final UUID userId = UUID.randomUUID();
    private FavoriteServiceImpl service;
    private MockedStatic<JwtUtil> jwt;

    @Mock
    private UserFavoriteRepository favorites;
    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private BusinessMapper businessMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CampaignMapper campaignMapper;

    private final ContactTypeProvider contactTypeProvider = org.mockito.Mockito.mock(ContactTypeProvider.class);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new FavoriteServiceImpl(favorites, businessRepository, productRepository, campaignRepository,
                favoriteMapper, businessMapper, contactTypeProvider, productMapper, campaignMapper);
        jwt = Mockito.mockStatic(JwtUtil.class);
        jwt.when(() -> JwtUtil.getUidFromToken("token")).thenReturn(userId);
    }

    @AfterEach
    void tearDown() {
        jwt.close();
    }

    private Pageable pageableOf(String type, int page, int size, String sort) {
        when(favorites.findPageByUser(eq(userId), anyInt(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        ResponseEntity<FavoritesResponse> resp = service.getFavorites("token", type, page, size, sort);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(favorites).findPageByUser(eq(userId), anyInt(), captor.capture());
        return captor.getValue();
    }

    // ---- sort ----

    @Test
    void sortByNameAscendingIsAcceptedAndUsesTheTargetName() {
        Pageable pageable = pageableOf(null, 0, 10, "name,asc");

        Sort.Order order = pageable.getSort().iterator().next();
        assertEquals(Sort.Direction.ASC, order.getDirection());
        assertTrue(order.getProperty().contains("COALESCE(b.name, p.name, c.title)"));
    }

    @Test
    void sortByCreatedAtDescendingIsAccepted() {
        Pageable pageable = pageableOf(null, 0, 10, "created_at,desc");

        Sort.Order order = pageable.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void sortWithoutDirectionIsAscendingAndCamelCaseIsAccepted() {
        Pageable pageable = pageableOf(null, 0, 10, " createdAt ");

        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void defaultSortIsCreatedAtDescending() {
        Pageable pageable = pageableOf(null, 0, 10, null);

        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"bogus", "name,sideways", "name,asc,extra", "business.name,asc"})
    void invalidSortIsABadRequestAndNeverHitsTheDatabase(String sort) {
        ResponseEntity<FavoritesResponse> resp = service.getFavorites("token", null, 0, 10, sort);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        verifyNoInteractions(favorites);
    }

    // ---- paging ----

    @Test
    void typeFilterMapsToTheBucketAndPagingGoesToTheDatabase() {
        when(favorites.findPageByUser(eq(userId), eq(1), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<FavoritesResponse> resp = service.getFavorites("token", " Products ", 2, 5, null);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(favorites).findPageByUser(eq(userId), eq(1), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void storesAndOffersFiltersUseTheirBuckets() {
        when(favorites.findPageByUser(eq(userId), anyInt(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.getFavorites("token", "stores", 0, 10, null);
        service.getFavorites("token", "offers", 0, 10, null);

        verify(favorites).findPageByUser(eq(userId), eq(0), any(Pageable.class));
        verify(favorites).findPageByUser(eq(userId), eq(2), any(Pageable.class));
    }

    @Test
    void negativePageIsTreatedAsFirstPageAndHugeSizeIsCapped() {
        Pageable pageable = pageableOf(null, -3, 100000, null);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    void sizeBelowOneIsABadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, service.getFavorites("token", null, 0, 0, null).getStatusCode());
        verifyNoInteractions(favorites);
    }

    @Test
    void invalidTypeIsABadRequestAndUnauthorizedIsChecked() {
        assertEquals(HttpStatus.BAD_REQUEST, service.getFavorites("token", "bogus", 0, 10, null).getStatusCode());
        jwt.when(() -> JwtUtil.getUidFromToken("anon")).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, service.getFavorites("anon", null, 0, 10, "name").getStatusCode());
        verifyNoInteractions(favorites);
    }

    @Test
    void pageItemsAreSplitByType() {
        UserFavoriteEntity store = new UserFavoriteEntity();
        store.setBusiness(new BusinessEntity());
        UserFavoriteEntity product = new UserFavoriteEntity();
        product.setProduct(new ProductEntity());
        UserFavoriteEntity offer = new UserFavoriteEntity();
        offer.setCampaign(new CampaignEntity());
        UserFavoriteEntity empty = new UserFavoriteEntity();
        when(favorites.findPageByUser(eq(userId), eq(-1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(store, product, offer, empty)));

        FavoritesResponse body = service.getFavorites("token", null, 0, 10, null).getBody();

        assertNotNull(body);
        assertEquals(1, body.stores().size());
        assertEquals(1, body.products().size());
        assertEquals(1, body.offers().size());
    }

    // ---- create: reactivation, duplicate, race ----

    private UserFavoriteEntity softDeleted() {
        UserFavoriteEntity fav = new UserFavoriteEntity(userEntity());
        fav.setActive(false);
        fav.setDeletedAt(LocalDateTime.now());
        fav.setDeletedBy(userId);
        return fav;
    }

    private UUID userEntity() {
        return userId;
    }

    @Test
    void createReactivatesASoftDeletedFavoriteInsteadOfConflict() {
        UUID itemId = UUID.randomUUID();
        UserFavoriteEntity old = softDeleted();
        when(businessRepository.findById(itemId)).thenReturn(Optional.of(new BusinessEntity()));
        when(favorites.findByUserIdAndBusinessId(userId, itemId)).thenReturn(Optional.of(old));
        when(favorites.saveAndFlush(any(UserFavoriteEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(favoriteMapper.toResponse(any(UserFavoriteEntity.class))).thenReturn(Mockito.mock(FavoriteResponse.class));

        ResponseEntity<FavoriteResponse> resp = service.createFavorite("token", new FavoriteCreateRequest(FavoriteType.STORE, itemId));

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertTrue(old.getActive());
        assertNull(old.getDeletedAt());
        assertNull(old.getDeletedBy());
    }

    @Test
    void createKeepsConflictForAnActiveDuplicate() {
        UUID itemId = UUID.randomUUID();
        UserFavoriteEntity active = new UserFavoriteEntity(userEntity());
        when(productRepository.findById(itemId)).thenReturn(Optional.of(new ProductEntity()));
        when(favorites.findByUserIdAndProductId(userId, itemId)).thenReturn(Optional.of(active));

        ResponseEntity<FavoriteResponse> resp = service.createFavorite("token", new FavoriteCreateRequest(FavoriteType.PRODUCT, itemId));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        verify(favorites, never()).saveAndFlush(any());
    }

    @Test
    void createAnswersConflictWhenTheUniqueIndexRejectsARace() {
        UUID itemId = UUID.randomUUID();
        when(campaignRepository.findById(itemId)).thenReturn(Optional.of(new CampaignEntity()));
        when(favorites.findByUserIdAndCampaignId(userId, itemId)).thenReturn(Optional.empty());
        when(favorites.saveAndFlush(any(UserFavoriteEntity.class))).thenThrow(new DataIntegrityViolationException("uq_fav_user_campaign"));

        ResponseEntity<FavoriteResponse> resp = service.createFavorite("token", new FavoriteCreateRequest(FavoriteType.OFFER, itemId));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    // ---- update by type ----

    private UserFavoriteEntity owned(UUID id) {
        UserFavoriteEntity fav = new UserFavoriteEntity(userEntity());
        fav.setId(id);
        return fav;
    }

    private void stubSave() {
        when(favorites.saveAndFlush(any(UserFavoriteEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(favoriteMapper.toResponse(any(UserFavoriteEntity.class))).thenReturn(Mockito.mock(FavoriteResponse.class));
    }

    @Test
    void updateOfAStoreFavoriteLooksUpABusinessNotAProduct() {
        UUID favId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserFavoriteEntity fav = owned(favId);
        fav.setBusiness(new BusinessEntity());
        BusinessEntity target = new BusinessEntity();
        when(favorites.findById(favId)).thenReturn(Optional.of(fav));
        when(businessRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(favorites.findByUserIdAndBusinessId(userId, targetId)).thenReturn(Optional.empty());
        stubSave();

        ResponseEntity<FavoriteResponse> resp = service.updateFavorite("token", favId, new FavoriteUpdateRequest(targetId, null));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(target, fav.getBusiness());
        assertNull(fav.getProduct());
        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOfAnOfferFavoriteLooksUpACampaign() {
        UUID favId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserFavoriteEntity fav = owned(favId);
        fav.setCampaign(new CampaignEntity());
        CampaignEntity target = new CampaignEntity();
        when(favorites.findById(favId)).thenReturn(Optional.of(fav));
        when(campaignRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(favorites.findByUserIdAndCampaignId(userId, targetId)).thenReturn(Optional.empty());
        stubSave();

        assertEquals(HttpStatus.OK, service.updateFavorite("token", favId, new FavoriteUpdateRequest(targetId, true)).getStatusCode());
        assertEquals(target, fav.getCampaign());
        verifyNoInteractions(productRepository, businessRepository);
    }

    @Test
    void updateReturnsNotFoundWhenTheTargetOfItsTypeIsMissing() {
        UUID favId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserFavoriteEntity store = owned(favId);
        store.setBusiness(new BusinessEntity());
        when(favorites.findById(favId)).thenReturn(Optional.of(store));
        when(businessRepository.findById(targetId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.updateFavorite("token", favId, new FavoriteUpdateRequest(targetId, null)).getStatusCode());

        UUID offerId = UUID.randomUUID();
        UserFavoriteEntity offer = owned(offerId);
        offer.setCampaign(new CampaignEntity());
        when(favorites.findById(offerId)).thenReturn(Optional.of(offer));
        when(campaignRepository.findById(targetId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.updateFavorite("token", offerId, new FavoriteUpdateRequest(targetId, null)).getStatusCode());

        UUID productFavId = UUID.randomUUID();
        UserFavoriteEntity product = owned(productFavId);
        product.setProduct(new ProductEntity());
        when(favorites.findById(productFavId)).thenReturn(Optional.of(product));
        when(productRepository.findById(targetId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.updateFavorite("token", productFavId, new FavoriteUpdateRequest(targetId, null)).getStatusCode());
    }

    @Test
    void updateIsOwnerCheckedBeforeAnyTargetLookup() {
        UUID favId = UUID.randomUUID();
        UserFavoriteEntity foreign = new UserFavoriteEntity();
        foreign.setUserFk(UUID.randomUUID());
        when(favorites.findById(favId)).thenReturn(Optional.of(foreign));

        assertEquals(HttpStatus.FORBIDDEN, service.updateFavorite("token", favId, new FavoriteUpdateRequest(UUID.randomUUID(), null)).getStatusCode());
        verifyNoInteractions(productRepository, businessRepository, campaignRepository);
    }

    @Test
    void updateGuardsNullRequestAndUnauthorized() {
        assertEquals(HttpStatus.BAD_REQUEST, service.updateFavorite("token", UUID.randomUUID(), null).getStatusCode());
        jwt.when(() -> JwtUtil.getUidFromToken("anon")).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, service.updateFavorite("anon", UUID.randomUUID(),
                new FavoriteUpdateRequest(UUID.randomUUID(), null)).getStatusCode());
    }

    @Test
    void updateRejectsMovingOntoATargetThatIsAlreadyAnActiveFavorite() {
        UUID favId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserFavoriteEntity fav = owned(favId);
        fav.setProduct(new ProductEntity());
        UserFavoriteEntity other = owned(UUID.randomUUID());
        when(favorites.findById(favId)).thenReturn(Optional.of(fav));
        when(productRepository.findById(targetId)).thenReturn(Optional.of(new ProductEntity()));
        when(favorites.findByUserIdAndProductId(userId, targetId)).thenReturn(Optional.of(other));

        assertEquals(HttpStatus.CONFLICT, service.updateFavorite("token", favId, new FavoriteUpdateRequest(targetId, null)).getStatusCode());
        verify(favorites, never()).saveAndFlush(any());
    }

    @Test
    void updateReactivatingClearsTheSoftDeleteAuditFields() {
        UUID favId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserFavoriteEntity fav = softDeleted();
        fav.setId(favId);
        fav.setProduct(new ProductEntity());
        when(favorites.findById(favId)).thenReturn(Optional.of(fav));
        when(productRepository.findById(targetId)).thenReturn(Optional.of(new ProductEntity()));
        when(favorites.findByUserIdAndProductId(userId, targetId)).thenReturn(Optional.of(fav));
        stubSave();

        assertEquals(HttpStatus.OK, service.updateFavorite("token", favId, new FavoriteUpdateRequest(targetId, true)).getStatusCode());
        assertTrue(fav.getActive());
        assertNull(fav.getDeletedAt());
        assertNull(fav.getDeletedBy());
    }

    @Test
    void updateSoftDeactivationKeepsTheFavoriteAndAllowsAnInactiveDuplicate() {
        UUID favId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserFavoriteEntity fav = owned(favId);
        fav.setProduct(new ProductEntity());
        UserFavoriteEntity other = owned(UUID.randomUUID());
        when(favorites.findById(favId)).thenReturn(Optional.of(fav));
        when(productRepository.findById(targetId)).thenReturn(Optional.of(new ProductEntity()));
        when(favorites.findByUserIdAndProductId(userId, targetId)).thenReturn(Optional.of(other));
        stubSave();

        assertEquals(HttpStatus.OK, service.updateFavorite("token", favId, new FavoriteUpdateRequest(targetId, false)).getStatusCode());
        assertFalse(fav.getActive());
    }

    @Test
    void updateAnswersConflictWhenTheUniqueIndexRejectsIt() {
        UUID favId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserFavoriteEntity fav = owned(favId);
        fav.setProduct(new ProductEntity());
        when(favorites.findById(favId)).thenReturn(Optional.of(fav));
        when(productRepository.findById(targetId)).thenReturn(Optional.of(new ProductEntity()));
        when(favorites.findByUserIdAndProductId(userId, targetId)).thenReturn(Optional.empty());
        when(favorites.saveAndFlush(any(UserFavoriteEntity.class))).thenThrow(new DataIntegrityViolationException("uq"));

        assertEquals(HttpStatus.CONFLICT, service.updateFavorite("token", favId, new FavoriteUpdateRequest(targetId, null)).getStatusCode());
    }
}
