package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.ProductCreateRequest;
import com.umdc.directory.api.v1.to.ProductCreateResponse;
import com.umdc.directory.api.v1.to.ProductListResponse;
import com.umdc.directory.api.v1.to.ProductUpdateRequest;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.BusinessProductEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.ProductEntity;
import com.umdc.directory.jpa.repository.BusinessProductRepository;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.ProductRepository;
import com.umdc.directory.mapper.BusinessProductMapper;
import com.umdc.directory.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.umdc.directory.support.OwnerTokens.forUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("F2-A RF-02 products: create with business, update, deactivate, owner listing")
class ProductManagementTest {

    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final String ownerToken = forUser(owner);

    private ProductRepository productRepository;
    private BusinessProductRepository businessProductRepository;
    private BusinessRepository businessRepository;
    private ProductMapper productMapper;
    private BusinessProductMapper businessProductMapper;
    private ProductServiceImpl service;
    private ProductEntity product;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        businessProductRepository = Mockito.mock(BusinessProductRepository.class);
        businessRepository = Mockito.mock(BusinessRepository.class);
        productMapper = Mockito.mock(ProductMapper.class);
        businessProductMapper = Mockito.mock(BusinessProductMapper.class);
        service = new ProductServiceImpl(productRepository, businessProductRepository, businessRepository,
                productMapper, businessProductMapper);
        product = new ProductEntity();
        product.setId(productId);
        product.setName("old");
        product.setDescription("old desc");
        product.setActive(true);
        product.setCreatedBy(owner);
        product.setPrice(new BigDecimal("5.00"));
        product.setCurrency("USD");
        product.setImageRef("img");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(productMapper.toProductCreateResponse(any(ProductEntity.class))).thenAnswer(i -> {
            ProductEntity e = i.getArgument(0);
            return new ProductCreateResponse(e.getId(), e.getName(), e.getDescription(), null, null, null,
                    e.getPrice(), e.getCurrency(), e.getImageRef(), e.getActive());
        });
    }

    private ProductUpdateRequest patch(String name, String desc, UUID cat, BigDecimal price, String cur, String img, Boolean active) {
        return new ProductUpdateRequest(name, desc, cat, price, cur, img, active);
    }

    // ---------- create ----------

    private ProductCreateRequest create(BigDecimal price, String cur, String img, UUID biz) {
        return new ProductCreateRequest("n", "d", categoryId, price, cur, img, biz);
    }

    @Test
    @DisplayName("create with businessId of the owner saves the product with createdBy, price and the active link")
    void createWithBusiness() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(owner));
        when(productMapper.toProductEntity(any())).thenReturn(product);
        var link = new BusinessProductEntity();
        when(businessProductMapper.toSource(any())).thenReturn(link);

        var r = service.create(create(new BigDecimal("9.50"), "eur", "  ", businessId), ownerToken);

        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals(owner, product.getCreatedBy());
        assertEquals(new BigDecimal("9.50"), product.getPrice());
        assertEquals("EUR", product.getCurrency());
        assertNull(product.getImageRef());
        verify(businessProductRepository).save(link);
    }

    @Test
    @DisplayName("create with businessId: unknown business 404, other owner 403, nothing saved")
    void createWithBusinessRejected() {
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, service.create(create(null, null, null, businessId), ownerToken).getStatusCode());
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(other));
        assertEquals(HttpStatus.FORBIDDEN, service.create(create(null, null, null, businessId), ownerToken).getStatusCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("create without businessId does not link the product")
    void createWithoutBusinessDoesNotLink() {
        when(businessRepository.countByUserId(owner)).thenReturn(1);
        when(productMapper.toProductEntity(any())).thenReturn(product);
        assertEquals(HttpStatus.CREATED, service.create(create(null, null, "http://img", null), ownerToken).getStatusCode());
        verify(businessProductRepository, never()).save(any());
        assertEquals("http://img", product.getImageRef());
    }

    @Test
    @DisplayName("create: missing token 401, invalid price, currency, pair or image 400")
    void createValidation() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.create(create(null, null, null, null), null).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.create(create(new BigDecimal("-1"), "USD", null, null), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.create(create(new BigDecimal("1.234"), "USD", null, null), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.create(create(BigDecimal.ONE, "US", null, null), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.create(create(BigDecimal.ONE, null, null, null), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.create(create(null, "USD", null, null), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.create(create(null, null, "x".repeat(513), null), ownerToken).getStatusCode());
        verify(productRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    @DisplayName("PATCH changes only the sent fields and refreshes lastUpdate")
    void patchPartial() {
        var r = service.update(productId, patch("new", null, null, null, null, null, null), ownerToken, false);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("new", product.getName());
        assertEquals("old desc", product.getDescription());
        assertEquals(new BigDecimal("5.00"), product.getPrice());
        assertEquals("img", product.getImageRef());
        assertTrue(product.getActive());
        assertTrue(product.getLastUpdate() != null);
    }

    @Test
    @DisplayName("PATCH sets description, category, price, currency (upper-cased), image and reactivates")
    void patchAllFields() {
        product.setActive(false);
        var r = service.update(productId, patch("n", "d", categoryId, new BigDecimal("7.5"), "pen", "new-img", true), ownerToken, false);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("d", product.getDescription());
        assertEquals(categoryId, product.getCategoryFk().getId());
        assertEquals(new BigDecimal("7.5"), product.getPrice());
        assertEquals("PEN", product.getCurrency());
        assertEquals("new-img", product.getImageRef());
        assertTrue(product.getActive());
    }

    @Test
    @DisplayName("PATCH with only the currency keeps the stored price; with only the price keeps the stored currency")
    void patchMergesPrice() {
        service.update(productId, patch(null, null, null, null, "eur", null, null), ownerToken, false);
        assertEquals("EUR", product.getCurrency());
        assertEquals(new BigDecimal("5.00"), product.getPrice());
        service.update(productId, patch(null, null, null, new BigDecimal("1"), null, null, null), ownerToken, false);
        assertEquals(new BigDecimal("1"), product.getPrice());
        assertEquals("EUR", product.getCurrency());
    }

    @Test
    @DisplayName("PATCH with a blank imageRef clears the image; deactivates with active=false")
    void patchClearsImageAndDeactivates() {
        service.update(productId, patch(null, null, null, null, null, " ", false), ownerToken, false);
        assertNull(product.getImageRef());
        assertFalse(product.getActive());
    }

    @Test
    @DisplayName("PATCH rejects blank or too long name and description, invalid price and image without changing the product")
    void patchRejected() {
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch(" ", null, null, null, null, null, null), ownerToken, false).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch("x".repeat(256), null, null, null, null, null, null), ownerToken, false).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch(null, "", null, null, null, null, null), ownerToken, false).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch(null, "x".repeat(1501), null, null, null, null, null), ownerToken, false).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch(null, null, null, new BigDecimal("-0.01"), null, null, null), ownerToken, false).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch(null, null, null, null, "XYZ1", null, null), ownerToken, false).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch(null, null, null, null, null, "x".repeat(513), null), ownerToken, false).getStatusCode());
        assertEquals("old", product.getName());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT requires name, description and categoryId and clears the price and image that are absent")
    void putReplaces() {
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch(null, "d", categoryId, null, null, null, null), ownerToken, true).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch("n", " ", categoryId, null, null, null, null), ownerToken, true).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch("n", "d", null, null, null, null, null), ownerToken, true).getStatusCode());

        var r = service.update(productId, patch("n", "d", categoryId, null, null, null, null), ownerToken, true);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNull(product.getPrice());
        assertNull(product.getCurrency());
        assertNull(product.getImageRef());
        assertTrue(product.getActive());
    }

    @Test
    @DisplayName("update: null id or body 400, no token 401, unknown product 404, stranger 403")
    void updateAuthorization() {
        assertEquals(HttpStatus.BAD_REQUEST, service.update(null, patch(null, null, null, null, null, null, null), ownerToken, false).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, null, ownerToken, false).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.update(productId, patch(null, null, null, null, null, null, null), "bad", false).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.update(UUID.randomUUID(), patch(null, null, null, null, null, null, null), ownerToken, false).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.update(productId, patch("x", null, null, null, null, null, null), forUser(other), false).getStatusCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: the owner of a business that carries the product may edit it even when not the creator")
    void updateByLinkedOwner() {
        product.setCreatedBy(null);
        when(businessProductRepository.countOwnedLinks(productId, owner)).thenReturn(1L);
        assertEquals(HttpStatus.OK, service.update(productId, patch("x", null, null, null, null, null, null), ownerToken, false).getStatusCode());
        when(businessProductRepository.countOwnedLinks(productId, other)).thenReturn(0L);
        assertEquals(HttpStatus.FORBIDDEN, service.update(productId, patch("y", null, null, null, null, null, null), forUser(other), false).getStatusCode());
    }

    @Test
    @DisplayName("update: a database integrity error (unknown category) answers 400")
    void updateIntegrityError() {
        when(productRepository.save(any(ProductEntity.class))).thenThrow(new DataIntegrityViolationException("fk"));
        assertEquals(HttpStatus.BAD_REQUEST, service.update(productId, patch(null, null, UUID.randomUUID(), null, null, null, null), ownerToken, false).getStatusCode());
    }

    // ---------- deactivate ----------

    @Test
    @DisplayName("DELETE deactivates an active product and answers 204; a second call is idempotent without saving")
    void deactivate() {
        assertEquals(HttpStatus.NO_CONTENT, service.deactivate(productId, ownerToken).getStatusCode());
        assertFalse(product.getActive());
        verify(productRepository, Mockito.times(1)).save(product);
        assertEquals(HttpStatus.NO_CONTENT, service.deactivate(productId, ownerToken).getStatusCode());
        verify(productRepository, Mockito.times(1)).save(product);
    }

    @Test
    @DisplayName("DELETE: null id 400, no token 401, unknown 404, stranger 403")
    void deactivateRejected() {
        assertEquals(HttpStatus.BAD_REQUEST, service.deactivate(null, ownerToken).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.deactivate(productId, null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.deactivate(UUID.randomUUID(), ownerToken).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.deactivate(productId, forUser(other)).getStatusCode());
        assertTrue(product.getActive());
    }

    // ---------- owner listing ----------

    private PageImpl<BusinessProductEntity> onePage() {
        var bp = new BusinessProductEntity();
        bp.setProduct(product);
        return new PageImpl<>(List.of(bp));
    }

    @Test
    @DisplayName("owner listing without active filter returns every link, inactive included")
    void ownerListsAll() {
        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(owner));
        when(businessProductRepository.findByBusiness(any(BusinessEntity.class), any(Pageable.class))).thenReturn(onePage());

        ResponseEntity<ProductListResponse> r = service.findByBusinessId(businessId, 1, 10, null, true, ownerToken);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(1, r.getBody().totalCount());
        verify(businessProductRepository, never()).findByBusinessAndActiveAndProductActive(any(), any(), any(), any());
    }

    @Test
    @DisplayName("owner listing with active=false filters by the product status")
    void ownerListsFiltered() {
        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(owner));
        when(businessProductRepository.findByBusinessAndProductActive(any(BusinessEntity.class), eq(false), any(Pageable.class))).thenReturn(onePage());

        assertEquals(HttpStatus.OK, service.findByBusinessId(businessId, 1, 10, false, true, ownerToken).getStatusCode());
        verify(businessProductRepository).findByBusinessAndProductActive(any(BusinessEntity.class), eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("owner listing: no token 401, other user 403, business without owner 403")
    void ownerListingRejected() {
        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(other));
        assertEquals(HttpStatus.UNAUTHORIZED, service.findByBusinessId(businessId, 1, 10, null, true, null).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.findByBusinessId(businessId, 1, 10, null, true, ownerToken).getStatusCode());
        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.FORBIDDEN, service.findByBusinessId(businessId, 1, 10, null, true, ownerToken).getStatusCode());
    }

    @Test
    @DisplayName("includeInactive=false or null stays public and never reads the token")
    void publicModeIgnoresToken() {
        when(businessRepository.existsById(businessId)).thenReturn(true);
        when(businessProductRepository.findByBusinessAndActiveAndProductActive(any(BusinessEntity.class), eq(true), eq(true), any(Pageable.class))).thenReturn(onePage());

        assertEquals(HttpStatus.OK, service.findByBusinessId(businessId, 1, 10, false, false, null).getStatusCode());
        assertEquals(HttpStatus.OK, service.findByBusinessId(businessId, 1, 10, false, null, null).getStatusCode());
        verify(businessRepository, never()).findOwnerId(any());
    }

    @Test
    @DisplayName("the default ProductService answers 501 for the new operations")
    void defaultsAreNotImplemented() {
        ProductService defaults = new ProductService() { };
        assertEquals(HttpStatus.NOT_IMPLEMENTED, defaults.update(productId, null, "t", false).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, defaults.deactivate(productId, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, defaults.findByBusinessId(businessId, 1, 1, null, null, null).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, defaults.findByBusinessId(businessId, 1, 1, null).getStatusCode());
    }

    @Test
    @DisplayName("CategoryEntity id is set when the category changes")
    void categoryEntity() {
        service.update(productId, patch(null, null, categoryId, null, null, null, null), ownerToken, false);
        CategoryEntity c = product.getCategoryFk();
        assertEquals(categoryId, c.getId());
    }
}
