package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CategoryCreateRequest;
import com.prx.directory.api.v1.to.CategoryCreateResponse;
import com.prx.directory.api.v1.to.CategoryGetResponse;
import com.prx.directory.jpa.entity.CategoryEntity;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.mapper.CategoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CategoryServiceImplTest {

    @InjectMocks
    CategoryServiceImpl categoryServiceImpl;

    @Mock
    CategoryRepository categoryRepository;
    
    @Mock
    CategoryMapper categoryMapper;

    @Test
    @DisplayName("Find category by ID - Success")
    void findCategoryByIdSuccess() {
        UUID categoryId = UUID.randomUUID();
        CategoryGetResponse categoryGetResponse = new CategoryGetResponse(
                categoryId,
                "Test Category",
                "Test Description",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
        );
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(categoryId);
        categoryEntity.setName("Test Category");
        categoryEntity.setDescription("Test Description");
        categoryEntity.setCreatedDate(LocalDateTime.now());
        categoryEntity.setLastUpdate(LocalDateTime.now());
        categoryEntity.setActive(true);

        when(categoryRepository.findFirstById(categoryId)).thenReturn(java.util.Optional.of(categoryEntity));
        when(categoryMapper.toCategoryGetResponse(any(CategoryEntity.class))).thenReturn(categoryGetResponse);

        ResponseEntity<CategoryGetResponse> response = categoryServiceImpl.find(categoryId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categoryGetResponse, response.getBody());
    }

    @Test
    @DisplayName("Find category by ID - Null ID")
    void findCategoryByIdNullId() {
        ResponseEntity<CategoryGetResponse> response = categoryServiceImpl.find(null);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Find categories by parent ID - Success")
    void findCategoriesByParentIdSuccess() {
        UUID parentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryGetResponse categoryGetResponse = new CategoryGetResponse(
                categoryId,
                "Test Category",
                "Test Description",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
        );
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(categoryId);
        categoryEntity.setName("Test Category");
        categoryEntity.setDescription("Test Description");
        categoryEntity.setCreatedDate(LocalDateTime.now());
        categoryEntity.setLastUpdate(LocalDateTime.now());
        categoryEntity.setActive(true);

        Collection<CategoryGetResponse> categoryGetResponses = List.of(categoryGetResponse);
        when(categoryRepository.findByCategoryParentFk(any(CategoryEntity.class))).thenReturn(Optional.of(List.of(categoryEntity)));
        when(categoryMapper.toCategoryGetResponse(ArgumentMatchers.anyCollection())).thenReturn(categoryGetResponses);

        ResponseEntity<Collection<CategoryGetResponse>> response = categoryServiceImpl.findByParentId(parentId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categoryGetResponses, response.getBody());
    }

    @Test
    @DisplayName("Find categories by parent ID - Null ID")
    void findCategoriesByParentIdNullId() {
        ResponseEntity<Collection<CategoryGetResponse>> response = categoryServiceImpl.findByParentId(null);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Create category - Success")
    void createCategorySuccess() {
        UUID categoryId = UUID.randomUUID();
        CategoryEntity savedEntity = new CategoryEntity();
        savedEntity.setId(categoryId);
        savedEntity.setName("New Category");
        savedEntity.setDescription("Desc");
        savedEntity.setCreatedDate(java.time.LocalDateTime.now());
        savedEntity.setLastUpdate(java.time.LocalDateTime.now());
        savedEntity.setActive(true);

        when(categoryRepository.existsById(any(UUID.class))).thenReturn(true); // safe default
        when(categoryMapper.toCategoryEntity(any(CategoryCreateRequest.class))).thenReturn(savedEntity);
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(savedEntity);
        when(categoryMapper.toCategoryCreateResponse(any(CategoryEntity.class))).thenReturn(new CategoryCreateResponse(categoryId, savedEntity.getCreatedDate()));

        CategoryCreateRequest request = new CategoryCreateRequest("New Category", "Desc", null, true);
        var response = categoryServiceImpl.create(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(categoryId, response.getBody().id());
    }

    @Test
    @DisplayName("Create category - Parent not found")
    void createCategoryParentNotFound() {
        UUID parentId = UUID.randomUUID();
        CategoryCreateRequest request = new CategoryCreateRequest("Child", "Desc", parentId, true);
        when(categoryRepository.existsById(parentId)).thenReturn(false);

        var response = categoryServiceImpl.create(request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Create category - With parent success")
    void createCategoryWithParentSuccess() {
        UUID parentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity savedEntity = new CategoryEntity();
        savedEntity.setId(categoryId);
        savedEntity.setName("Child");
        savedEntity.setDescription("Desc");
        savedEntity.setCreatedDate(java.time.LocalDateTime.now());
        savedEntity.setLastUpdate(java.time.LocalDateTime.now());
        savedEntity.setActive(true);

        when(categoryRepository.existsById(parentId)).thenReturn(true);
        when(categoryMapper.toCategoryEntity(any(CategoryCreateRequest.class))).thenReturn(savedEntity);
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(savedEntity);
        when(categoryMapper.toCategoryCreateResponse(any(CategoryEntity.class))).thenReturn(new CategoryCreateResponse(categoryId, savedEntity.getCreatedDate()));

        CategoryCreateRequest request = new CategoryCreateRequest("Child", "Desc", parentId, true);
        var response = categoryServiceImpl.create(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(categoryId, response.getBody().id());
    }

    @Test
    @DisplayName("Create category - Database constraint violation")
    void createCategoryConstraintViolation() {
        CategoryEntity entityToSave = new CategoryEntity();
        entityToSave.setName("Duplicate");
        entityToSave.setDescription("Desc");
        entityToSave.setActive(true);

        when(categoryRepository.existsById(any(UUID.class))).thenReturn(true);
        when(categoryMapper.toCategoryEntity(any(CategoryCreateRequest.class))).thenReturn(entityToSave);
        when(categoryRepository.save(any(CategoryEntity.class))).thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        CategoryCreateRequest request = new CategoryCreateRequest("Duplicate", "Desc", null, true);
        assertThrows(DataIntegrityViolationException.class, () -> categoryServiceImpl.create(request));
    }

    @Test
    @DisplayName("Create category - Database access error")
    void createCategoryDatabaseError() {
        CategoryEntity entityToSave = new CategoryEntity();
        entityToSave.setName("Test");
        entityToSave.setDescription("Desc");
        entityToSave.setActive(true);

        when(categoryRepository.existsById(any(UUID.class))).thenReturn(true);
        when(categoryMapper.toCategoryEntity(any(CategoryCreateRequest.class))).thenReturn(entityToSave);
        when(categoryRepository.save(any(CategoryEntity.class))).thenThrow(new DataAccessException("Database connection error") {});

        CategoryCreateRequest request = new CategoryCreateRequest("Test", "Desc", null, true);
        assertThrows(DataAccessException.class, () -> categoryServiceImpl.create(request));
    }
}
