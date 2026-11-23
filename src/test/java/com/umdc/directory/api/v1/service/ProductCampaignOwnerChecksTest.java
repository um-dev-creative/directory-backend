package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.CampaignTO;
import com.umdc.directory.api.v1.to.CampaignUpdateRequest;
import com.umdc.directory.api.v1.to.LinkBusinessProductRequest;
import com.umdc.directory.api.v1.to.ProductCreateRequest;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.BusinessProductEntity;
import com.umdc.directory.jpa.entity.CampaignEntity;
import com.umdc.directory.jpa.entity.ProductEntity;
import com.umdc.directory.jpa.repository.BusinessProductRepository;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CampaignRepository;
import com.umdc.directory.jpa.repository.CategoryRepository;
import com.umdc.directory.jpa.repository.ProductRepository;
import com.umdc.directory.mapper.BusinessProductMapper;
import com.umdc.directory.mapper.CampaignMapper;
import com.umdc.directory.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.umdc.directory.support.OwnerTokens.forUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("F0-A2 owner checks on product and campaign writes")
class ProductCampaignOwnerChecksTest {

    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    private ProductRepository productRepository;
    private BusinessProductRepository businessProductRepository;
    private BusinessRepository businessRepository;
    private ProductMapper productMapper;
    private BusinessProductMapper businessProductMapper;
    private ProductServiceImpl productService;

    private CampaignRepository campaignRepository;
    private CategoryRepository categoryRepository;
    private CampaignMapper campaignMapper;
    private CampaignServiceImpl campaignService;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        businessProductRepository = Mockito.mock(BusinessProductRepository.class);
        businessRepository = Mockito.mock(BusinessRepository.class);
        productMapper = Mockito.mock(ProductMapper.class);
        businessProductMapper = Mockito.mock(BusinessProductMapper.class);
        productService = new ProductServiceImpl(productRepository, businessProductRepository,
                businessRepository, productMapper, businessProductMapper);

        campaignRepository = Mockito.mock(CampaignRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        campaignMapper = Mockito.mock(CampaignMapper.class);
        campaignService = new CampaignServiceImpl(campaignRepository, categoryRepository, businessRepository,
                campaignMapper, Mockito.mock(CampaignStatusCounter.class));

        when(businessRepository.findOwnerId(businessId)).thenReturn(Optional.of(owner));
    }

    // ---------- product create ----------

    private ProductCreateRequest productRequest() {
        return new ProductCreateRequest("n", "d", categoryId);
    }

    @Test
    void productCreate_withoutToken_is401() {
        assertEquals(401, productService.create(productRequest(), null).getStatusCode().value());
        assertEquals(401, productService.create(productRequest(), "garbage").getStatusCode().value());
        verify(productRepository, never()).save(any());
    }

    @Test
    void productCreate_userWithoutBusiness_is403() {
        when(businessRepository.countByUserId(other)).thenReturn(0);
        assertEquals(403, productService.create(productRequest(), forUser(other)).getStatusCode().value());
        verify(productRepository, never()).save(any());
    }

    @Test
    void productCreate_businessOwner_is201() {
        when(businessRepository.countByUserId(owner)).thenReturn(2);
        when(productMapper.toProductEntity(any())).thenReturn(new ProductEntity());
        when(productRepository.save(any())).thenReturn(new ProductEntity());
        assertEquals(201, productService.create(productRequest(), forUser(owner)).getStatusCode().value());
    }

    // ---------- product link ----------

    @Test
    void productLink_withoutToken_is401() {
        var req = new LinkBusinessProductRequest(businessId, UUID.randomUUID());
        assertEquals(401, productService.linkProductToBusiness(req, null).getStatusCode().value());
        verify(businessProductRepository, never()).save(any());
    }

    @Test
    void productLink_nullBusinessId_is400() {
        var req = new LinkBusinessProductRequest(null, UUID.randomUUID());
        assertEquals(400, productService.linkProductToBusiness(req, forUser(owner)).getStatusCode().value());
    }

    @Test
    void productLink_businessNotFound_is404() {
        var req = new LinkBusinessProductRequest(UUID.randomUUID(), UUID.randomUUID());
        assertEquals(404, productService.linkProductToBusiness(req, forUser(owner)).getStatusCode().value());
        verify(businessProductRepository, never()).save(any());
    }

    @Test
    void productLink_otherUser_is403() {
        var req = new LinkBusinessProductRequest(businessId, UUID.randomUUID());
        assertEquals(403, productService.linkProductToBusiness(req, forUser(other)).getStatusCode().value());
        verify(businessProductRepository, never()).save(any());
    }

    @Test
    void productLink_owner_is201() {
        var req = new LinkBusinessProductRequest(businessId, UUID.randomUUID());
        var entity = new BusinessProductEntity();
        entity.setId(new com.umdc.directory.jpa.entity.BusinessProductEntityId(businessId, req.productId()));
        when(businessProductMapper.toSource(req)).thenReturn(entity);
        when(businessProductRepository.save(entity)).thenReturn(entity);
        assertEquals(201, productService.linkProductToBusiness(req, forUser(owner)).getStatusCode().value());
    }

    // ---------- campaign create ----------

    private CampaignTO campaignTo(UUID biz) {
        return new CampaignTO(null, "n", "d", null, null, categoryId, biz, null, null, null, true);
    }

    @Test
    void campaignCreate_withoutToken_is401() {
        assertEquals(401, campaignService.create(campaignTo(businessId), null).getStatusCode().value());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void campaignCreate_businessNotFound_is404() {
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        assertEquals(404, campaignService.create(campaignTo(UUID.randomUUID()), forUser(owner)).getStatusCode().value());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void campaignCreate_otherUser_is403() {
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        assertEquals(403, campaignService.create(campaignTo(businessId), forUser(other)).getStatusCode().value());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void campaignCreate_owner_is201() {
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        var to = campaignTo(businessId);
        var entity = new CampaignEntity();
        when(campaignMapper.toEntity(to)).thenReturn(entity);
        when(campaignRepository.save(entity)).thenReturn(entity);
        when(campaignMapper.toTO(entity)).thenReturn(to);
        assertEquals(201, campaignService.create(to, forUser(owner)).getStatusCode().value());
    }

    // ---------- campaign update ----------

    private CampaignEntity campaignOf(UUID biz) {
        var e = new CampaignEntity();
        e.setLastUpdate(LocalDateTime.parse("2025-11-01T00:00:00"));
        var b = new BusinessEntity();
        b.setId(biz);
        e.setBusinessFk(b);
        return e;
    }

    private CampaignUpdateRequest titleOnly() {
        return new CampaignUpdateRequest("t", null, null, null, null, null, null, null, null, null);
    }

    private CampaignUpdateRequest moveTo(UUID biz) {
        return new CampaignUpdateRequest(null, null, null, null, null, biz, null, null, null, null);
    }

    private int status(ResponseStatusException ex) {
        return ex.getStatusCode().value();
    }

    @Test
    void campaignUpdate_withoutToken_is401() {
        UUID id = UUID.randomUUID();
        assertEquals(401, campaignService.update(id, titleOnly(), null).getStatusCode().value());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void campaignUpdate_campaignNotFound_is404() {
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class, () -> campaignService.update(id, titleOnly(), forUser(owner)));
        assertEquals(404, status(ex));
    }

    @Test
    void campaignUpdate_campaignWithoutBusiness_is404() {
        UUID id = UUID.randomUUID();
        var e = new CampaignEntity();
        when(campaignRepository.findById(id)).thenReturn(Optional.of(e));
        var ex = assertThrows(ResponseStatusException.class, () -> campaignService.update(id, titleOnly(), forUser(owner)));
        assertEquals(404, status(ex));
    }

    @Test
    void campaignUpdate_otherUser_is403() {
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaignOf(businessId)));
        var ex = assertThrows(ResponseStatusException.class, () -> campaignService.update(id, titleOnly(), forUser(other)));
        assertEquals(403, status(ex));
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void campaignUpdate_owner_is202() {
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaignOf(businessId)));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertEquals(202, campaignService.update(id, titleOnly(), forUser(owner)).getStatusCode().value());
    }

    @Test
    void campaignUpdate_ownerStaleLastUpdate_stillConflict409() {
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaignOf(businessId)));
        var stale = new CampaignUpdateRequest("t", null, null, null, null, null, null, null, null,
                LocalDateTime.parse("2024-01-01T00:00:00"));
        var ex = assertThrows(ResponseStatusException.class, () -> campaignService.update(id, stale, forUser(owner)));
        assertEquals(409, status(ex));
    }

    @Test
    void campaignUpdate_moveToBusinessOfAnotherUser_is403() {
        UUID id = UUID.randomUUID();
        UUID foreign = UUID.randomUUID();
        when(businessRepository.findOwnerId(foreign)).thenReturn(Optional.of(other));
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaignOf(businessId)));
        var ex = assertThrows(ResponseStatusException.class, () -> campaignService.update(id, moveTo(foreign), forUser(owner)));
        assertEquals(403, status(ex));
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void campaignUpdate_moveToMissingBusiness_is404() {
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaignOf(businessId)));
        var ex = assertThrows(ResponseStatusException.class, () -> campaignService.update(id, moveTo(UUID.randomUUID()), forUser(owner)));
        assertEquals(404, status(ex));
    }

    @Test
    void campaignUpdate_moveToOwnOtherBusiness_is202() {
        UUID id = UUID.randomUUID();
        UUID mine = UUID.randomUUID();
        when(businessRepository.findOwnerId(mine)).thenReturn(Optional.of(owner));
        when(businessRepository.existsById(mine)).thenReturn(true);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaignOf(businessId)));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertEquals(202, campaignService.update(id, moveTo(mine), forUser(owner)).getStatusCode().value());
    }

    // ---------- public reads keep working without a token ----------

    @Test
    void campaignFind_staysPublic() {
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(404, campaignService.find(id).getStatusCode().value());
    }
}
