package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.CategoryCreateRequest;
import com.umdev.directory.api.v1.to.CategoryCreateResponse;
import com.umdev.directory.api.v1.to.CategoryGetResponse;
import com.umdev.directory.api.v1.to.PaginatedResponse;
import com.umdev.directory.jpa.entity.CategoryEntity;
import com.umdev.directory.jpa.repository.CategoryRepository;
import com.umdev.directory.mapper.CategoryMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

// Service implementation for category-related operations.
//
// @see CategoryService
@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // Constructs a new CategoryServiceImpl with the specified repository and mapper.
    //
    // @param categoryRepository the repository to use for category operations
    // @param categoryMapper     the mapper to use for category operations
    // @see CategoryRepository
    // @see CategoryMapper
    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public ResponseEntity<CategoryGetResponse> find(@NotNull UUID categoryId) {
        var categoryEntity = categoryRepository.findFirstById(categoryId);
        return categoryEntity.map(entity ->
                        ResponseEntity.ok(categoryMapper.toCategoryGetResponse(entity)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<PaginatedResponse<CategoryGetResponse>> findByParentId(@NotNull UUID parentId, int page, int size) {
        logger.info("Request to find categories by parent ID: {} with page: {}, size: {}", parentId, page, size);

        try {
            // Validate pagination parameters
            if (page < 0) {
                logger.warn("Invalid page number: {}. Page must be non-negative.", page);
                return ResponseEntity.badRequest().build();
            }

            if (size <= 0 || size > MAX_PAGE_SIZE) {
                logger.warn("Invalid page size: {}. Size must be between 1 and {}.", size, MAX_PAGE_SIZE);
                return ResponseEntity.badRequest().build();
            }

            // Check if parent category exists
            if (!categoryRepository.existsById(parentId)) {
                logger.warn("Parent category not found with ID: {}", parentId);
                return ResponseEntity.notFound().build();
            }

            // Create parent entity reference for query
            CategoryEntity categoryEntity = new CategoryEntity();
            categoryEntity.setId(parentId);

            // Query categories by parent with pagination at database level
            Pageable pageable = PageRequest.of(page, size);
            Page<CategoryEntity> categoryPage = categoryRepository.findByCategoryParentFk(categoryEntity, pageable);

            // Map entities to responses
            Collection<CategoryGetResponse> items = categoryMapper.toCategoryGetResponse(categoryPage.getContent());

            logger.info("Successfully retrieved {} categories for parent ID: {} (page: {}, size: {}, total: {})",
                       categoryPage.getNumberOfElements(), parentId, page, size, categoryPage.getTotalElements());

            PaginatedResponse<CategoryGetResponse> response = new PaginatedResponse<>(
                    categoryPage.getTotalElements(),
                    categoryPage.getNumber(),
                    categoryPage.getSize(),
                    categoryPage.getTotalPages(),
                    items
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error finding categories by parent ID: {}", parentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<Collection<CategoryGetResponse>> findAll() {
        return ResponseEntity.ok(categoryMapper.toCategoryGetResponse(categoryRepository.findAll()));
    }

    @Override
    @Transactional
    public ResponseEntity<CategoryCreateResponse> create(@Valid CategoryCreateRequest request) {
        // If parent ID provided, ensure parent exists
        if (request.categoryParentId() != null && !categoryRepository.existsById(request.categoryParentId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Map to entity, save, map to response
        CategoryEntity entityToSave = categoryMapper.toCategoryEntity(request);
        CategoryEntity saved = categoryRepository.save(entityToSave);
        CategoryCreateResponse response = categoryMapper.toCategoryCreateResponse(saved);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
