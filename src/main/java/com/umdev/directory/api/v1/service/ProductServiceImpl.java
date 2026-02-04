package com.umdev.directory.api.v1.service;

import com.umdev.directory.constant.DirectoryAppConstants;
import com.umdev.directory.jpa.entity.BusinessEntity;
import com.umdev.directory.jpa.entity.BusinessProductEntity;
import com.umdev.directory.jpa.repository.BusinessProductRepository;
import com.umdev.directory.jpa.repository.BusinessRepository;
import com.umdev.directory.jpa.repository.ProductRepository;
import com.umdev.directory.mapper.BusinessProductMapper;
import com.umdev.directory.mapper.ProductMapper;
import com.umdev.directory.api.v1.to.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.umdev.directory.constant.DirectoryAppConstants.DEFAULT_PAGE;
import static com.umdev.directory.constant.DirectoryAppConstants.DEFAULT_PER_PAGE;

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
public class ProductServiceImpl implements ProductService {

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
    public ResponseEntity<ProductCreateResponse> create(@Valid ProductCreateRequest productCreateRequest) {
        if(Objects.isNull(productCreateRequest)) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(
                productMapper.toProductCreateResponse(productRepository
                        .save(productMapper.toProductEntity(productCreateRequest))),
                HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<LinkBusinessProductResponse> linkProductToBusiness(LinkBusinessProductRequest businessProductLinkCreateRequest) {
        if(Objects.isNull(businessProductLinkCreateRequest)) {
            return ResponseEntity.badRequest().build();
        }
        var businessProductEntityResult = businessProductRepository.save(businessProductMapper.toSource(businessProductLinkCreateRequest));
        if(Objects.isNull(businessProductEntityResult.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(
                businessProductMapper.toLinkBusinessProductResponse(businessProductEntityResult),
                HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ProductListResponse> findByBusinessId(UUID businessId, Integer page, Integer perPage, Boolean active) {
        logger.info("Listing products for business {} page={} perPage={} active={}", businessId, page, perPage, active);
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

            BusinessEntity businessRef = new BusinessEntity();
            businessRef.setId(businessId);

            Pageable pageable = PageRequest.of(resolvedPage - 1, resolvedPerPage);
            Page<BusinessProductEntity> pageResult;
            if (active == null) {
                pageResult = businessProductRepository.findByBusiness(businessRef, pageable);
            } else {
                pageResult = businessProductRepository.findByBusinessAndActive(businessRef, active, pageable);
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
}
