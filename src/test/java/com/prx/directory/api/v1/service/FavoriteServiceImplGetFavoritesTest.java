package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.BusinessTO;
import com.prx.directory.api.v1.to.FavoritesResponse;
import com.prx.directory.api.v1.to.OfferTO;
import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.entity.ProductEntity;
import com.prx.directory.jpa.entity.UserFavoriteEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.ProductRepository;
import com.prx.directory.jpa.repository.UserFavoriteRepository;
import com.prx.directory.mapper.BusinessMapper;
import com.prx.directory.mapper.CampaignMapper;
import com.prx.directory.mapper.FavoriteMapper;
import com.prx.directory.mapper.ProductMapper;
import com.prx.directory.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FavoriteServiceImplGetFavoritesTest {

    private FavoriteServiceImpl favoriteService;

    @Mock
    private UserFavoriteRepository userFavoriteRepository;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        favoriteService = new FavoriteServiceImpl(userFavoriteRepository,
                businessRepository, productRepository, campaignRepository,
                favoriteMapper, businessMapper, productMapper, campaignMapper);
    }
    @Test
    @DisplayName("401 when token missing or invalid")
    void getFavoritesUnauthorized() {
        ResponseEntity<FavoritesResponse> resp = favoriteService.getFavorites(null, null, 0, 10, null);
        assertTrue(resp.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Returns empty lists when user has no favorites")
    void getFavoritesEmpty() {
        UUID userId = UUID.randomUUID();
        try (MockedStatic<JwtUtil> mocked = Mockito.mockStatic(JwtUtil.class)) {
            mocked.when(() -> JwtUtil.getUidFromToken("token")).thenReturn(userId);

            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of());

            ResponseEntity<FavoritesResponse> resp = favoriteService.getFavorites("token", null, 0, 10, null);
            assertTrue(resp.getStatusCode().is2xxSuccessful());
            FavoritesResponse body = resp.getBody();
            assertNotNull(body);
            assertTrue(body.stores().isEmpty());
            assertTrue(body.products().isEmpty());
            assertTrue(body.offers().isEmpty());
        }
    }

    @Test
    @DisplayName("Returns grouped favorites with type filter and pagination")
    void getFavoritesGroupedAndFiltered() {
        UUID userId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        UUID pId = UUID.randomUUID();
        UUID cId = UUID.randomUUID();

        BusinessEntity be = new BusinessEntity(); be.setId(bId); be.setName("B");
        ProductEntity pe = new ProductEntity(); pe.setId(pId); pe.setName("P");
        CampaignEntity ce = new CampaignEntity(); ce.setId(cId); ce.setName("C"); ce.setStartDate(Instant.now()); ce.setEndDate(Instant.now());

        UserFavoriteEntity uf1 = new UserFavoriteEntity(); uf1.setBusiness(be);
        UserFavoriteEntity uf2 = new UserFavoriteEntity(); uf2.setProduct(pe);
        UserFavoriteEntity uf3 = new UserFavoriteEntity(); uf3.setCampaign(ce);

        try (MockedStatic<JwtUtil> mocked = Mockito.mockStatic(JwtUtil.class)) {
            mocked.when(() -> JwtUtil.getUidFromToken("token")).thenReturn(userId);

            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(uf1, uf2, uf3));

            BusinessTO bto = new BusinessTO(bId, "B", null, null, null, null, null, null, null, null, null, false, null);
            OfferTO oto = new OfferTO(cId, "C", null, null, ce.getStartDate(), ce.getEndDate(), false);

            when(businessMapper.toBusinessTO(any(BusinessEntity.class))).thenReturn(bto);
            when(campaignMapper.toOfferTO(any(CampaignEntity.class))).thenReturn(oto);

            // full response
            ResponseEntity<FavoritesResponse> resp = favoriteService.getFavorites("token", null, 0, 10, null);
            assertTrue(resp.getStatusCode().is2xxSuccessful());
            FavoritesResponse f = resp.getBody();
            assertNotNull(f);
            assertEquals(1, f.stores().size());
            assertEquals(1, f.products().size());
            assertEquals(1, f.offers().size());

            // filter stores only
            ResponseEntity<FavoritesResponse> storesResp = favoriteService.getFavorites("token", "stores", 0, 10, null);
            assertTrue(storesResp.getStatusCode().is2xxSuccessful());
            assertNotNull(storesResp.getBody());
            assertEquals(1, storesResp.getBody().stores().size());
            assertTrue(storesResp.getBody().products().isEmpty());

            // filter products only
            ResponseEntity<FavoritesResponse> productsResp = favoriteService.getFavorites("token", "products", 0, 10, null);
            assertTrue(productsResp.getStatusCode().is2xxSuccessful());
            assertNotNull(productsResp.getBody());
            assertEquals(1, productsResp.getBody().products().size());
            assertTrue(productsResp.getBody().stores().isEmpty());
        }
    }

    @Test
    @DisplayName("Returns 501 when sort parameter is provided")
    void getFavoritesWithSortNotImplemented() {
        UUID userId = UUID.randomUUID();
        try (MockedStatic<JwtUtil> mocked = Mockito.mockStatic(JwtUtil.class)) {
            mocked.when(() -> JwtUtil.getUidFromToken("token")).thenReturn(userId);

            ResponseEntity<FavoritesResponse> resp = favoriteService.getFavorites("token", null, 0, 10, "name");
            assertEquals(501, resp.getStatusCode().value());
        }
    }

    @Test
    @DisplayName("Paginate across combined list when no type filter")
    void getFavoritesCombinedPagination() {
        UUID userId = UUID.randomUUID();
        UUID bId1 = UUID.randomUUID();
        UUID bId2 = UUID.randomUUID();
        UUID pId1 = UUID.randomUUID();

        BusinessEntity be1 = new BusinessEntity(); be1.setId(bId1); be1.setName("B1");
        BusinessEntity be2 = new BusinessEntity(); be2.setId(bId2); be2.setName("B2");
        ProductEntity pe1 = new ProductEntity(); pe1.setId(pId1); pe1.setName("P1");

        UserFavoriteEntity uf1 = new UserFavoriteEntity(); uf1.setBusiness(be1);
        UserFavoriteEntity uf2 = new UserFavoriteEntity(); uf2.setBusiness(be2);
        UserFavoriteEntity uf3 = new UserFavoriteEntity(); uf3.setProduct(pe1);

        try (MockedStatic<JwtUtil> mocked = Mockito.mockStatic(JwtUtil.class)) {
            mocked.when(() -> JwtUtil.getUidFromToken("token")).thenReturn(userId);

            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(uf1, uf2, uf3));

            BusinessTO bto1 = new BusinessTO(bId1, "B1", null, null, null, null, null, null, null, null, null, false, null);
            BusinessTO bto2 = new BusinessTO(bId2, "B2", null, null, null, null, null, null, null, null, null, false, null);

            when(businessMapper.toBusinessTO(be1)).thenReturn(bto1);
            when(businessMapper.toBusinessTO(be2)).thenReturn(bto2);

            // Request page 0, size 2 - should return first 2 items (both stores)
            ResponseEntity<FavoritesResponse> resp = favoriteService.getFavorites("token", null, 0, 2, null);
            assertTrue(resp.getStatusCode().is2xxSuccessful());
            FavoritesResponse f = resp.getBody();
            assertNotNull(f);
            assertEquals(2, f.stores().size());
            assertEquals(0, f.products().size());
            assertEquals(0, f.offers().size());

            // Request page 1, size 2 - should return next item (product)
            ResponseEntity<FavoritesResponse> resp2 = favoriteService.getFavorites("token", null, 1, 2, null);
            assertTrue(resp2.getStatusCode().is2xxSuccessful());
            FavoritesResponse f2 = resp2.getBody();
            assertNotNull(f2);
            assertEquals(0, f2.stores().size());
            assertEquals(1, f2.products().size());
            assertEquals(0, f2.offers().size());
        }
    }
}
