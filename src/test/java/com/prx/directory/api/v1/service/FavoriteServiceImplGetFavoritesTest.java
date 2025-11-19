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

import java.time.LocalDateTime;
import java.util.ArrayList;
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

        BusinessEntity be = new BusinessEntity();
        be.setId(bId);
        be.setName("B");
        ProductEntity pe = new ProductEntity();
        pe.setId(pId);
        pe.setName("P");
        CampaignEntity ce = new CampaignEntity();
        ce.setId(cId);
        ce.setTitle("C");
        ce.setStartDate(LocalDateTime.now());
        ce.setEndDate(LocalDateTime.now());

        UserFavoriteEntity uf1 = new UserFavoriteEntity();
        uf1.setBusiness(be);
        UserFavoriteEntity uf2 = new UserFavoriteEntity();
        uf2.setProduct(pe);
        UserFavoriteEntity uf3 = new UserFavoriteEntity();
        uf3.setCampaign(ce);

        try (MockedStatic<JwtUtil> mocked = Mockito.mockStatic(JwtUtil.class)) {
            mocked.when(() -> JwtUtil.getUidFromToken("token")).thenReturn(userId);

            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(uf1, uf2, uf3));

            BusinessTO bto = new BusinessTO(bId, "B", null, null, null, null, null, null, null, null, null, false, null);
            OfferTO oto = new OfferTO(cId, "C", null, null, ce.getStartDate(), ce.getEndDate(), null, false);

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
    @DisplayName("Pagination applies across combined list when no type filter is specified")
    void getFavoritesPaginationAcrossCombinedList() {
        UUID userId = UUID.randomUUID();

        // Create 5 stores, 5 products, 5 offers = 15 total
        List<UserFavoriteEntity> favorites = new ArrayList<>();
        List<BusinessEntity> businesses = new ArrayList<>();
        List<ProductEntity> products = new ArrayList<>();
        List<CampaignEntity> campaigns = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            BusinessEntity be = new BusinessEntity();
            be.setId(UUID.randomUUID());
            be.setName("Store" + i);
            businesses.add(be);
            UserFavoriteEntity uf = new UserFavoriteEntity();
            uf.setBusiness(be);
            favorites.add(uf);
        }

        for (int i = 0; i < 5; i++) {
            ProductEntity pe = new ProductEntity();
            pe.setId(UUID.randomUUID());
            pe.setName("Product" + i);
            products.add(pe);
            UserFavoriteEntity uf = new UserFavoriteEntity();
            uf.setProduct(pe);
            favorites.add(uf);
        }

        for (int i = 0; i < 5; i++) {
            CampaignEntity ce = new CampaignEntity();
            ce.setId(UUID.randomUUID());
            ce.setTitle("Offer" + i);
            ce.setStartDate(LocalDateTime.now());
            ce.setEndDate(LocalDateTime.now());
            campaigns.add(ce);
            UserFavoriteEntity uf = new UserFavoriteEntity();
            uf.setCampaign(ce);
            favorites.add(uf);
        }

        try (MockedStatic<JwtUtil> mocked = Mockito.mockStatic(JwtUtil.class)) {
            mocked.when(() -> JwtUtil.getUidFromToken("token")).thenReturn(userId);

            when(userFavoriteRepository.findByUserId(userId)).thenReturn(favorites);
            when(businessMapper.toBusinessTO(any(BusinessEntity.class)))
                    .thenAnswer(inv -> {
                        BusinessEntity b = inv.getArgument(0);
                        return new BusinessTO(b.getId(), b.getName(), null, null, null, null, null, null, null, null, null, false, null);
                    });
            when(campaignMapper.toOfferTO(any(CampaignEntity.class)))
                    .thenAnswer(inv -> {
                        CampaignEntity c = inv.getArgument(0);
                        return new OfferTO(c.getId(), c.getTitle(), null, null, c.getStartDate(), c.getEndDate(), null, false);
                    });

            // Request page 0 with size 10 - should return exactly 10 items total (5 stores + 5 products)
            ResponseEntity<FavoritesResponse> resp = favoriteService.getFavorites("token", null, 0, 10, null);
            assertTrue(resp.getStatusCode().is2xxSuccessful());
            FavoritesResponse body = resp.getBody();
            assertNotNull(body);

            int totalItems = body.stores().size() + body.products().size() + body.offers().size();
            assertEquals(10, totalItems, "Total items should be 10 when requesting page 0 with size 10");
            assertEquals(5, body.stores().size());
            assertEquals(5, body.products().size());
            assertEquals(0, body.offers().size());

            // Request page 1 with size 10 - should return exactly 5 items total (5 offers)
            ResponseEntity<FavoritesResponse> resp2 = favoriteService.getFavorites("token", null, 1, 10, null);
            assertTrue(resp2.getStatusCode().is2xxSuccessful());
            FavoritesResponse body2 = resp2.getBody();
            assertNotNull(body2);

            int totalItems2 = body2.stores().size() + body2.products().size() + body2.offers().size();
            assertEquals(5, totalItems2, "Total items should be 5 when requesting page 1 with size 10");
            assertEquals(0, body2.stores().size());
            assertEquals(0, body2.products().size());
            assertEquals(5, body2.offers().size());

            // Request page 2 with size 10 - should return 0 items
            ResponseEntity<FavoritesResponse> resp3 = favoriteService.getFavorites("token", null, 2, 10, null);
            assertTrue(resp3.getStatusCode().is2xxSuccessful());
            FavoritesResponse body3 = resp3.getBody();
            assertNotNull(body3);

            int totalItems3 = body3.stores().size() + body3.products().size() + body3.offers().size();
            assertEquals(0, totalItems3, "Total items should be 0 when requesting page 2 with size 10");
        }
    }
}
