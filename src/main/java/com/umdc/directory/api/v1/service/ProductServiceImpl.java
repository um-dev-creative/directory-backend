package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.*;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.BusinessProductEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.ProductEntity;
import com.umdc.directory.jpa.repository.BusinessProductRepository;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.ProductRepository;
import com.umdc.directory.mapper.BusinessProductMapper;
import com.umdc.directory.mapper.ProductMapper;
import com.umdc.directory.util.JwtUtil;
import com.umdc.directory.util.ProductValidator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.umdc.directory.constant.DirectoryAppConstants.DEFAULT_PAGE;
import static com.umdc.directory.constant.DirectoryAppConstants.DEFAULT_PER_PAGE;

/**
 * Service implementation for handling product-related operations.
 * This class provides methods to create products, link them to businesses,
 * and retrieve products by business ID.
 * It interacts with the data repositories and utilizes mappers for data conversion.
 * The behaviors include:
 * - Creating a product entity and persisting it to the database.
 * - Linking a product to a business entity and persisting the relationship.
 * - Fetching a paginated list of products associated with a specific business.
 */
@Service
@SuppressWarnings("PMD.GodClass") // create, link, list, update and deactivate share the ownership and validation helpers
public class ProductServiceImpl implements ProductService {

    private static final int MAX_NAME = 255;
    private static final int MAX_DESCRIPTION = 1500;

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final BusinessProductRepository businessProductRepository;
    private final BusinessRepository businessRepository;
    private final ProductMapper productMapper;
    private final BusinessProductMapper businessProductMapper;

    public ProductServiceImpl(ProductRepository productRepository,
                              BusinessProductRepository businessProductRepository,
                              BusinessRepository businessRepository,
                              ProductMapper productMapper,
                              BusinessProductMapper businessProductMapper) {
        this.productRepository = productRepository;
        this.businessProductRepository = businessProductRepository;
        this.businessRepository = businessRepository;
        this.productMapper = productMapper;
        this.businessProductMapper = businessProductMapper;
    }

    @Override
    public ResponseEntity<ProductCreateResponse> create(@Valid ProductCreateRequest productCreateRequest, String token) {
        if(Objects.isNull(productCreateRequest)) {
            return ResponseEntity.badRequest().build();
        }
        // Without businessId the product is a catalogue item: only owners of at least one business may create one.
        // With businessId the caller must own that business and the product is linked to it as active.
        var userId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (Objects.nonNull(ProductValidator.validatePrice(productCreateRequest.price(), productCreateRequest.currency()))
                || Objects.nonNull(ProductValidator.validateImageRef(productCreateRequest.imageRef()))) {
            return ResponseEntity.badRequest().build();
        }
        var businessId = productCreateRequest.businessId();
        if (Objects.nonNull(businessId)) {
            var ownerId = businessRepository.findOwnerId(businessId);
            if (ownerId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            if (!userId.equals(ownerId.get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else if (businessRepository.countByUserId(userId) <= 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var entity = productMapper.toProductEntity(productCreateRequest);
        entity.setCreatedBy(userId);
        entity.setPrice(productCreateRequest.price());
        entity.setCurrency(ProductValidator.normalizeCurrency(productCreateRequest.currency()));
        entity.setImageRef(blankToNull(productCreateRequest.imageRef()));
        var saved = productRepository.save(entity);
        if (Objects.nonNull(businessId)) {
            businessProductRepository.save(businessProductMapper.toSource(new LinkBusinessProductRequest(businessId, saved.getId())));
        }
        return new ResponseEntity<>(productMapper.toProductCreateResponse(saved), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<LinkBusinessProductResponse> linkProductToBusiness(LinkBusinessProductRequest businessProductLinkCreateRequest, String token) {
        if(Objects.isNull(businessProductLinkCreateRequest) || Objects.isNull(businessProductLinkCreateRequest.businessId())) {
            return ResponseEntity.badRequest().build();
        }
        var userId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var ownerId = businessRepository.findOwnerId(businessProductLinkCreateRequest.businessId());
        if (ownerId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!userId.equals(ownerId.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var businessProductEntityResult = businessProductRepository.save(businessProductMapper.toSource(businessProductLinkCreateRequest));
        if(Objects.isNull(businessProductEntityResult.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(
                businessProductMapper.toLinkBusinessProductResponse(businessProductEntityResult),
                HttpStatus.CREATED);
    }

    /// Public listing: only active products (the `active` filter is ignored here).
    @Override
    public ResponseEntity<ProductListResponse> findByBusinessId(UUID businessId, Integer page, Integer perPage, Boolean active) {
        return findByBusinessId(businessId, page, perPage, active, false, null);
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ProductListResponse> findByBusinessId(UUID businessId, Integer page, Integer perPage, Boolean active,
                                                                Boolean includeInactive, String token) {
        logger.info("Listing products for business {} page={} perPage={} active={} includeInactive={}", businessId, page, perPage, active, includeInactive);
        try {
            if (businessId == null) {
                return ResponseEntity.badRequest().build();
            }

            // Validate page/perPage (client uses 1-based page). Convert to 0-based for Spring Data.
            int resolvedPage = (page == null) ? DEFAULT_PAGE : page;
            int resolvedPerPage = (perPage == null) ? DEFAULT_PER_PAGE : perPage;

            if (resolvedPage < DEFAULT_PAGE) {
                return ResponseEntity.badRequest().build();
            }
            if (resolvedPerPage < DEFAULT_PAGE || resolvedPerPage > DirectoryAppConstants.MAX_PER_PAGE) {
                return ResponseEntity.badRequest().build();
            }

            // Check business exists
            if (!businessRepository.existsById(businessId)) {
                return ResponseEntity.notFound().build();
            }

            boolean ownerMode = Boolean.TRUE.equals(includeInactive);
            if (ownerMode) {
                var userId = JwtUtil.getUidFromToken(token);
                if (Objects.isNull(userId)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                var ownerId = businessRepository.findOwnerId(businessId);
                if (ownerId.isEmpty() || !userId.equals(ownerId.get())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            BusinessEntity businessRef = new BusinessEntity();
            businessRef.setId(businessId);

            Pageable pageable = PageRequest.of(resolvedPage - 1, resolvedPerPage);
            Page<BusinessProductEntity> pageResult;
            if (ownerMode && active == null) {
                pageResult = businessProductRepository.findByBusiness(businessRef, pageable);
            } else if (ownerMode) {
                pageResult = businessProductRepository.findByBusinessAndProductActive(businessRef, active, pageable);
            } else {
                pageResult = businessProductRepository.findByBusinessAndActiveAndProductActive(businessRef, true, true, pageable);
            }

            List<ProductListItemTO> items = pageResult.getContent().stream()
                    .map(BusinessProductEntity::getProduct)
                    .filter(Objects::nonNull)
                    .map(productMapper::toProductListItemTO)
                    .toList();

            ProductListResponse response = new ProductListResponse(
                    pageResult.getTotalElements(),
                    pageResult.getNumber()+1,
                    pageResult.getSize(),
                    pageResult.getTotalPages(),
                    items
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid input", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error listing products for business {}", businessId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<ProductCreateResponse> update(UUID productId, ProductUpdateRequest request, String token, boolean replace) {
        if (Objects.isNull(productId) || Objects.isNull(request)) {
            return ResponseEntity.badRequest().build();
        }
        var access = authorize(productId, token);
        if (Objects.nonNull(access.denied())) {
            return ResponseEntity.status(access.denied()).build();
        }
        var product = access.product();
        if (!applyUpdate(product, request, replace)) {
            return ResponseEntity.badRequest().build();
        }
        product.setLastUpdate(LocalDateTime.now());
        try {
            return ResponseEntity.ok(productMapper.toProductCreateResponse(productRepository.save(product)));
        } catch (DataIntegrityViolationException e) {
            logger.warn("Product {} update rejected by the database", productId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<Void> deactivate(UUID productId, String token) {
        if (Objects.isNull(productId)) {
            return ResponseEntity.badRequest().build();
        }
        var access = authorize(productId, token);
        if (Objects.nonNull(access.denied())) {
            return ResponseEntity.status(access.denied()).build();
        }
        var product = access.product();
        if (Boolean.TRUE.equals(product.getActive())) {
            product.setActive(false);
            product.setLastUpdate(LocalDateTime.now());
            productRepository.save(product);
        }
        return ResponseEntity.noContent().build();
    }

    // Result of the write authorisation: the status to answer when denied (401, 404, 403), or the product when allowed.
    private record Access(HttpStatus denied, ProductEntity product) {
    }

    private Access authorize(UUID productId, String token) {
        var userId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(userId)) {
            return new Access(HttpStatus.UNAUTHORIZED, null);
        }
        var found = productRepository.findById(productId);
        if (found.isEmpty()) {
            return new Access(HttpStatus.NOT_FOUND, null);
        }
        if (!isOwner(found.get(), userId)) {
            return new Access(HttpStatus.FORBIDDEN, null);
        }
        return new Access(null, found.get());
    }

    // The creator, or the owner of any business that carries the product.
    private boolean isOwner(ProductEntity product, UUID userId) {
        return userId.equals(product.getCreatedBy()) || businessProductRepository.countOwnedLinks(product.getId(), userId) > 0;
    }

    // Validates the request and copies it onto the entity; false when it is invalid (nothing is changed in that case).
    private boolean applyUpdate(ProductEntity product, ProductUpdateRequest r, boolean replace) {
        if (replace && (isBlank(r.name()) || isBlank(r.description()) || Objects.isNull(r.categoryId()))) {
            return false;
        }
        if ((Objects.nonNull(r.name()) && (r.name().isBlank() || r.name().length() > MAX_NAME))
                || (Objects.nonNull(r.description()) && (r.description().isBlank() || r.description().length() > MAX_DESCRIPTION))) {
            return false;
        }
        // PATCH merges with the stored price; PUT replaces it (absent means cleared)
        BigDecimal price = replace || Objects.nonNull(r.price()) ? r.price() : product.getPrice();
        String currency = replace || Objects.nonNull(r.currency()) ? r.currency() : product.getCurrency();
        if (Objects.nonNull(ProductValidator.validatePrice(price, currency))
                || Objects.nonNull(ProductValidator.validateImageRef(r.imageRef()))) {
            return false;
        }
        if (Objects.nonNull(r.name())) {
            product.setName(r.name());
        }
        if (Objects.nonNull(r.description())) {
            product.setDescription(r.description());
        }
        if (Objects.nonNull(r.categoryId())) {
            var category = new CategoryEntity();
            category.setId(r.categoryId());
            product.setCategoryFk(category);
        }
        product.setPrice(price);
        product.setCurrency(ProductValidator.normalizeCurrency(currency));
        if (replace || Objects.nonNull(r.imageRef())) {
            product.setImageRef(blankToNull(r.imageRef()));
        }
        if (Objects.nonNull(r.active())) {
            product.setActive(r.active());
        }
        return true;
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }
}
