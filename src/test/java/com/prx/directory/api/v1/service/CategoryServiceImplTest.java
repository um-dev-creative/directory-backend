package com.prx.directory.api.v1.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
