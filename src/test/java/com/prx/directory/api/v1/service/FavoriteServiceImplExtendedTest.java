package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.*;
import com.prx.directory.constant.FavoriteType;
import com.prx.directory.jpa.entity.*;
import com.prx.directory.jpa.repository.*;
import com.prx.directory.mapper.*;
import com.prx.directory.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplExtendedTest {

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

    private FavoriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FavoriteServiceImpl(userFavoriteRepository, businessRepository,
                productRepository, campaignRepository, favoriteMapper, businessMapper,
                productMapper, campaignMapper);
    }

    @Test
    @DisplayName("createFavorite - null request returns BAD_REQUEST")
    void createFavorite_nullRequest() {
        ResponseEntity<FavoriteResponse> result = service.createFavorite("token", null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNoInteractions(userFavoriteRepository);
    }

    @Test
    @DisplayName("createFavorite - null userId from token returns UNAUTHORIZED")
    void createFavorite_nullUserId() {
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.STORE, UUID.randomUUID());

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(null);

            ResponseEntity<FavoriteResponse> result = service.createFavorite("token", request);

            assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        }
    }

    @Test
    @DisplayName("createFavorite - STORE type success")
    void createFavorite_storeSuccess() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.STORE, itemId);
        BusinessEntity business = new BusinessEntity();
        UserFavoriteEntity savedFavorite = new UserFavoriteEntity(new UserEntity());
        FavoriteResponse expected = mock(FavoriteResponse.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(businessRepository.findById(itemId)).thenReturn(Optional.of(business));
            when(userFavoriteRepository.findByUserIdAndBusinessId(userId, itemId)).thenReturn(Optional.empty());
            when(userFavoriteRepository.save(any(UserFavoriteEntity.class))).thenReturn(savedFavorite);
            when(favoriteMapper.toResponse(savedFavorite)).thenReturn(expected);

            ResponseEntity<FavoriteResponse> result = service.createFavorite("token", request);

            assertEquals(HttpStatus.CREATED, result.getStatusCode());
            assertEquals(expected, result.getBody());
            verify(userFavoriteRepository).save(any(UserFavoriteEntity.class));
        }
    }

    @Test
    @DisplayName("createFavorite - STORE not found returns NOT_FOUND")
    void createFavorite_storeNotFound() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.STORE, itemId);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(businessRepository.findById(itemId)).thenReturn(Optional.empty());

            ResponseEntity<FavoriteResponse> result = service.createFavorite("token", request);

            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
            verify(userFavoriteRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("createFavorite - STORE already favorited returns CONFLICT")
    void createFavorite_storeConflict() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.STORE, itemId);
        BusinessEntity business = new BusinessEntity();
        UserFavoriteEntity existing = new UserFavoriteEntity(new UserEntity());

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(businessRepository.findById(itemId)).thenReturn(Optional.of(business));
            when(userFavoriteRepository.findByUserIdAndBusinessId(userId, itemId)).thenReturn(Optional.of(existing));

            ResponseEntity<FavoriteResponse> result = service.createFavorite("token", request);

            assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
            verify(userFavoriteRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("createFavorite - PRODUCT type success")
    void createFavorite_productSuccess() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.PRODUCT, itemId);
        ProductEntity product = new ProductEntity();
        UserFavoriteEntity savedFavorite = new UserFavoriteEntity(new UserEntity());
        FavoriteResponse expected = mock(FavoriteResponse.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(productRepository.findById(itemId)).thenReturn(Optional.of(product));
            when(userFavoriteRepository.findByUserIdAndProductId(userId, itemId)).thenReturn(Optional.empty());
            when(userFavoriteRepository.save(any(UserFavoriteEntity.class))).thenReturn(savedFavorite);
            when(favoriteMapper.toResponse(savedFavorite)).thenReturn(expected);

            ResponseEntity<FavoriteResponse> result = service.createFavorite("token", request);

            assertEquals(HttpStatus.CREATED, result.getStatusCode());
            assertEquals(expected, result.getBody());
        }
    }

    @Test
    @DisplayName("createFavorite - PRODUCT not found returns NOT_FOUND")
    void createFavorite_productNotFound() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.PRODUCT, itemId);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(productRepository.findById(itemId)).thenReturn(Optional.empty());

            ResponseEntity<FavoriteResponse> result = service.createFavorite("token", request);

            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        }
    }

    @Test
    @DisplayName("createFavorite - OFFER type success")
    void createFavorite_offerSuccess() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.OFFER, itemId);
        CampaignEntity campaign = new CampaignEntity();
        UserFavoriteEntity savedFavorite = new UserFavoriteEntity(new UserEntity());
        FavoriteResponse expected = mock(FavoriteResponse.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(campaignRepository.findById(itemId)).thenReturn(Optional.of(campaign));
            when(userFavoriteRepository.findByUserIdAndCampaignId(userId, itemId)).thenReturn(Optional.empty());
            when(userFavoriteRepository.save(any(UserFavoriteEntity.class))).thenReturn(savedFavorite);
            when(favoriteMapper.toResponse(savedFavorite)).thenReturn(expected);

            ResponseEntity<FavoriteResponse> result = service.createFavorite("token", request);

            assertEquals(HttpStatus.CREATED, result.getStatusCode());
            assertEquals(expected, result.getBody());
        }
    }

    @Test
    @DisplayName("createFavorite - OFFER not found returns NOT_FOUND")
    void createFavorite_offerNotFound() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.OFFER, itemId);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(campaignRepository.findById(itemId)).thenReturn(Optional.empty());

            ResponseEntity<FavoriteResponse> result = service.createFavorite("token", request);

            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        }
    }

    @Test
    @DisplayName("getFavorites - sort parameter returns NOT_IMPLEMENTED")
    void getFavorites_sortNotImplemented() {
        UUID userId = UUID.randomUUID();

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", null, 0, 10, "name");

            assertEquals(HttpStatus.NOT_IMPLEMENTED, result.getStatusCode());
            verifyNoInteractions(userFavoriteRepository);
        }
    }

    @Test
    @DisplayName("getFavorites - null userId from token returns UNAUTHORIZED")
    void getFavorites_nullUserId() {
        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(null);

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", null, 0, 10, null);

            assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        }
    }

    @Test
    @DisplayName("getFavorites - all types without filter")
    void getFavorites_allTypes() {
        UUID userId = UUID.randomUUID();
        BusinessEntity business = new BusinessEntity();
        ProductEntity product = new ProductEntity();
        CampaignEntity campaign = new CampaignEntity();

        UserFavoriteEntity fav1 = new UserFavoriteEntity(new UserEntity());
        fav1.setBusiness(business);
        UserFavoriteEntity fav2 = new UserFavoriteEntity(new UserEntity());
        fav2.setProduct(product);
        UserFavoriteEntity fav3 = new UserFavoriteEntity(new UserEntity());
        fav3.setCampaign(campaign);

        BusinessTO businessTO = mock(BusinessTO.class);
        ProductCreateResponse productTO = mock(ProductCreateResponse.class);
        OfferTO offerTO = mock(OfferTO.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(fav1, fav2, fav3));
            when(businessMapper.toBusinessTO(business)).thenReturn(businessTO);
            when(productMapper.toProductCreateResponse(product)).thenReturn(productTO);
            when(campaignMapper.toOfferTO(campaign)).thenReturn(offerTO);

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", null, 0, 10, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(1, result.getBody().stores().size());
            assertEquals(1, result.getBody().products().size());
            assertEquals(1, result.getBody().offers().size());
        }
    }

    @Test
    @DisplayName("getFavorites - filter by stores type")
    void getFavorites_filterStores() {
        UUID userId = UUID.randomUUID();
        BusinessEntity business1 = new BusinessEntity();
        BusinessEntity business2 = new BusinessEntity();

        UserFavoriteEntity fav1 = new UserFavoriteEntity(new UserEntity());
        fav1.setBusiness(business1);
        UserFavoriteEntity fav2 = new UserFavoriteEntity(new UserEntity());
        fav2.setBusiness(business2);

        BusinessTO businessTO1 = mock(BusinessTO.class);
        BusinessTO businessTO2 = mock(BusinessTO.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(fav1, fav2));
            when(businessMapper.toBusinessTO(business1)).thenReturn(businessTO1);
            when(businessMapper.toBusinessTO(business2)).thenReturn(businessTO2);

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", "stores", 0, 10, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(2, result.getBody().stores().size());
            assertTrue(result.getBody().products().isEmpty());
            assertTrue(result.getBody().offers().isEmpty());
        }
    }

    @Test
    @DisplayName("getFavorites - filter by products type")
    void getFavorites_filterProducts() {
        UUID userId = UUID.randomUUID();
        ProductEntity product = new ProductEntity();

        UserFavoriteEntity fav = new UserFavoriteEntity(new UserEntity());
        fav.setProduct(product);

        ProductCreateResponse productTO = mock(ProductCreateResponse.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(fav));
            when(productMapper.toProductCreateResponse(product)).thenReturn(productTO);

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", "products", 0, 10, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertTrue(result.getBody().stores().isEmpty());
            assertEquals(1, result.getBody().products().size());
            assertTrue(result.getBody().offers().isEmpty());
        }
    }

    @Test
    @DisplayName("getFavorites - filter by offers type")
    void getFavorites_filterOffers() {
        UUID userId = UUID.randomUUID();
        CampaignEntity campaign = new CampaignEntity();

        UserFavoriteEntity fav = new UserFavoriteEntity(new UserEntity());
        fav.setCampaign(campaign);

        OfferTO offerTO = mock(OfferTO.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(fav));
            when(campaignMapper.toOfferTO(campaign)).thenReturn(offerTO);

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", "offers", 0, 10, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertTrue(result.getBody().stores().isEmpty());
            assertTrue(result.getBody().products().isEmpty());
            assertEquals(1, result.getBody().offers().size());
        }
    }

    @Test
    @DisplayName("getFavorites - invalid type filter returns BAD_REQUEST")
    void getFavorites_invalidTypeFilter() {
        UUID userId = UUID.randomUUID();

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of());

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", "invalid", 0, 10, null);

            assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        }
    }

    @Test
    @DisplayName("getFavorites - pagination with page 0 size 2")
    void getFavorites_pagination() {
        UUID userId = UUID.randomUUID();
        BusinessEntity b1 = new BusinessEntity();
        BusinessEntity b2 = new BusinessEntity();
        BusinessEntity b3 = new BusinessEntity();

        UserFavoriteEntity fav1 = new UserFavoriteEntity(new UserEntity());
        fav1.setBusiness(b1);
        UserFavoriteEntity fav2 = new UserFavoriteEntity(new UserEntity());
        fav2.setBusiness(b2);
        UserFavoriteEntity fav3 = new UserFavoriteEntity(new UserEntity());
        fav3.setBusiness(b3);

        BusinessTO bt1 = mock(BusinessTO.class);
        BusinessTO bt2 = mock(BusinessTO.class);
        BusinessTO bt3 = mock(BusinessTO.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(fav1, fav2, fav3));
            when(businessMapper.toBusinessTO(b1)).thenReturn(bt1);
            when(businessMapper.toBusinessTO(b2)).thenReturn(bt2);
            when(businessMapper.toBusinessTO(b3)).thenReturn(bt3);

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", "stores", 0, 2, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(2, result.getBody().stores().size());
        }
    }

    @Test
    @DisplayName("getFavorites - pagination page 1 size 2")
    void getFavorites_paginationPage1() {
        UUID userId = UUID.randomUUID();
        BusinessEntity b1 = new BusinessEntity();
        BusinessEntity b2 = new BusinessEntity();
        BusinessEntity b3 = new BusinessEntity();

        UserFavoriteEntity fav1 = new UserFavoriteEntity(new UserEntity());
        fav1.setBusiness(b1);
        UserFavoriteEntity fav2 = new UserFavoriteEntity(new UserEntity());
        fav2.setBusiness(b2);
        UserFavoriteEntity fav3 = new UserFavoriteEntity(new UserEntity());
        fav3.setBusiness(b3);

        BusinessTO bt1 = mock(BusinessTO.class);
        BusinessTO bt2 = mock(BusinessTO.class);
        BusinessTO bt3 = mock(BusinessTO.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(fav1, fav2, fav3));
            when(businessMapper.toBusinessTO(b1)).thenReturn(bt1);
            when(businessMapper.toBusinessTO(b2)).thenReturn(bt2);
            when(businessMapper.toBusinessTO(b3)).thenReturn(bt3);

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", "stores", 1, 2, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(1, result.getBody().stores().size());
        }
    }

    @Test
    @DisplayName("getFavorites - empty favorites returns empty response")
    void getFavorites_empty() {
        UUID userId = UUID.randomUUID();

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of());

            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", null, 0, 10, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertTrue(result.getBody().stores().isEmpty());
            assertTrue(result.getBody().products().isEmpty());
            assertTrue(result.getBody().offers().isEmpty());
        }
    }

    @Test
    @DisplayName("getFavorites - mixed types with combined pagination")
    void getFavorites_mixedTypesCombinedPagination() {
        UUID userId = UUID.randomUUID();
        BusinessEntity b1 = new BusinessEntity();
        ProductEntity p1 = new ProductEntity();
        CampaignEntity c1 = new CampaignEntity();

        UserFavoriteEntity fav1 = new UserFavoriteEntity(new UserEntity());
        fav1.setBusiness(b1);
        UserFavoriteEntity fav2 = new UserFavoriteEntity(new UserEntity());
        fav2.setProduct(p1);
        UserFavoriteEntity fav3 = new UserFavoriteEntity(new UserEntity());
        fav3.setCampaign(c1);

        BusinessTO bt1 = mock(BusinessTO.class);
        ProductCreateResponse pt1 = mock(ProductCreateResponse.class);
        OfferTO ot1 = mock(OfferTO.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(userFavoriteRepository.findByUserId(userId)).thenReturn(List.of(fav1, fav2, fav3));
            when(businessMapper.toBusinessTO(b1)).thenReturn(bt1);
            when(productMapper.toProductCreateResponse(p1)).thenReturn(pt1);
            when(campaignMapper.toOfferTO(c1)).thenReturn(ot1);

            // Get only first 2 items
            ResponseEntity<FavoritesResponse> result = service.getFavorites("token", null, 0, 2, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            // Should return first 2 items (store and product)
            int totalItems = result.getBody().stores().size() +
                            result.getBody().products().size() +
                            result.getBody().offers().size();
            assertEquals(2, totalItems);
        }
    }

    @Test
    @DisplayName("createFavorite - sets timestamps and active flag")
    void createFavorite_setsTimestampsAndActive() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FavoriteCreateRequest request = new FavoriteCreateRequest(FavoriteType.STORE, itemId);
        BusinessEntity business = new BusinessEntity();

        ArgumentCaptor<UserFavoriteEntity> captor = ArgumentCaptor.forClass(UserFavoriteEntity.class);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(businessRepository.findById(itemId)).thenReturn(Optional.of(business));
            when(userFavoriteRepository.findByUserIdAndBusinessId(userId, itemId)).thenReturn(Optional.empty());
            when(userFavoriteRepository.save(any(UserFavoriteEntity.class))).thenReturn(new UserFavoriteEntity(new UserEntity()));
            when(favoriteMapper.toResponse(any())).thenReturn(mock(FavoriteResponse.class));

            service.createFavorite("token", request);

            verify(userFavoriteRepository).save(captor.capture());
            UserFavoriteEntity saved = captor.getValue();
            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getUpdatedAt());
            assertTrue(saved.getActive());
        }
    }
}

