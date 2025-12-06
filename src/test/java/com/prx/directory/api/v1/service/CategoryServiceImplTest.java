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
        when(categoryRepository.existsById(parentId)).thenReturn(true);
        when(categoryRepository.findByCategoryParentFk(any(CategoryEntity.class))).thenReturn(Optional.of(List.of(categoryEntity)));
        when(categoryMapper.toCategoryGetResponse(ArgumentMatchers.anyCollection())).thenReturn(categoryGetResponses);

        ResponseEntity<Collection<CategoryGetResponse>> response = categoryServiceImpl.findByParentId(parentId, 0, 20);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categoryGetResponses, response.getBody());
    }

    @Test
    @DisplayName("Find categories by parent ID - Parent not found")
    void findCategoriesByParentIdNotFound() {
        UUID parentId = UUID.randomUUID();
        when(categoryRepository.existsById(parentId)).thenReturn(false);
        
        ResponseEntity<Collection<CategoryGetResponse>> response = categoryServiceImpl.findByParentId(parentId, 0, 20);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Find categories by parent ID - Invalid page number")
    void findCategoriesByParentIdInvalidPage() {
        UUID parentId = UUID.randomUUID();
        
        ResponseEntity<Collection<CategoryGetResponse>> response = categoryServiceImpl.findByParentId(parentId, -1, 20);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Find categories by parent ID - Invalid page size")
    void findCategoriesByParentIdInvalidSize() {
        UUID parentId = UUID.randomUUID();
        
        ResponseEntity<Collection<CategoryGetResponse>> response = categoryServiceImpl.findByParentId(parentId, 0, 0);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        response = categoryServiceImpl.findByParentId(parentId, 0, 101);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Find categories by parent ID - Empty result")
    void findCategoriesByParentIdEmpty() {
        UUID parentId = UUID.randomUUID();
        when(categoryRepository.existsById(parentId)).thenReturn(true);
        when(categoryRepository.findByCategoryParentFk(any(CategoryEntity.class))).thenReturn(Optional.empty());
        when(categoryMapper.toCategoryGetResponse(ArgumentMatchers.anyCollection())).thenReturn(List.of());

        ResponseEntity<Collection<CategoryGetResponse>> response = categoryServiceImpl.findByParentId(parentId, 0, 20);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    @DisplayName("Find categories by parent ID - Pagination")
    void findCategoriesByParentIdPagination() {
        UUID parentId = UUID.randomUUID();
        List<CategoryEntity> entities = new java.util.ArrayList<>();
        List<CategoryGetResponse> responses = new java.util.ArrayList<>();
        
        for (int i = 0; i < 25; i++) {
            UUID categoryId = UUID.randomUUID();
            CategoryEntity entity = new CategoryEntity();
            entity.setId(categoryId);
            entity.setName("Category " + i);
            entity.setDescription("Description " + i);
            entity.setCreatedDate(LocalDateTime.now());
            entity.setLastUpdate(LocalDateTime.now());
            entity.setActive(true);
            entities.add(entity);
            
            responses.add(new CategoryGetResponse(categoryId, "Category " + i, "Description " + i, 
                                                   null, LocalDateTime.now(), LocalDateTime.now(), true));
        }

        when(categoryRepository.existsById(parentId)).thenReturn(true);
        when(categoryRepository.findByCategoryParentFk(any(CategoryEntity.class))).thenReturn(Optional.of(entities));
        when(categoryMapper.toCategoryGetResponse(ArgumentMatchers.anyCollection())).thenAnswer(invocation -> {
            Collection<CategoryEntity> input = invocation.getArgument(0);
            return responses.subList(0, input.size());
        });

        // Test first page
        ResponseEntity<Collection<CategoryGetResponse>> response = categoryServiceImpl.findByParentId(parentId, 0, 20);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(20, response.getBody().size());
        
        // Test second page
        response = categoryServiceImpl.findByParentId(parentId, 1, 20);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().size());
    }

    @Test
    @DisplayName("Find categories by parent ID - Null ID")
    void findCategoriesByParentIdNullId() {
        ResponseEntity<Collection<CategoryGetResponse>> response = categoryServiceImpl.findByParentId(null, 0, 20);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
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
    @DisplayName("Create category - Persistence failure")
    void createCategoryPersistenceFailure() {
        CategoryEntity entityToSave = new CategoryEntity();
        entityToSave.setName("Test Category");

        when(categoryMapper.toCategoryEntity(any(CategoryCreateRequest.class))).thenReturn(entityToSave);
        when(categoryRepository.save(any(CategoryEntity.class))).thenThrow(new DataIntegrityViolationException("Database error"));

        CategoryCreateRequest request = new CategoryCreateRequest("Test Category", "Desc", null, true);
        
        try {
            categoryServiceImpl.create(request);
        } catch (DataIntegrityViolationException e) {
            assertEquals("Database error", e.getMessage());
        }
    }
}
