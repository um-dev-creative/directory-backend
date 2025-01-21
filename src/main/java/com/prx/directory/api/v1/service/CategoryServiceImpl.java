package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CategoryGetResponse;
import com.prx.directory.jpa.entity.CategoryEntity;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.mapper.CategoryMapper;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

/// Service implementation for category-related operations.
///
/// @see CategoryService
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    ///  Constructs a new CategoryServiceImpl with the specified repository and mapper.
    ///
    /// @param categoryRepository the repository to use for category operations
    /// @param categoryMapper     the mapper to use for category operations
    /// @see CategoryRepository
    /// @see CategoryMapper
    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public ResponseEntity<CategoryGetResponse> find(@NotNull @NotEmpty UUID categoryId) {
        var categoryEntity = categoryRepository.findFirstById(categoryId);
        return categoryEntity.map(entity ->
                        ResponseEntity.ok(categoryMapper.toCategoryGetResponse(entity)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Collection<CategoryGetResponse>> findByParentId(@NotNull @NotEmpty UUID parentId) {
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(parentId);
        var categoryEntities = categoryRepository.findByCategoryParentFk(categoryEntity);
        return categoryEntities.map(entities ->
                        ResponseEntity.ok(categoryMapper.toCategoryGetResponse(entities)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
