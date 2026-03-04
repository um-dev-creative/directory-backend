package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CategoryCreateRequest;
import com.prx.directory.api.v1.to.CategoryCreateResponse;
import com.prx.directory.api.v1.to.CategoryGetResponse;
import com.prx.directory.api.v1.to.PaginatedResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryServiceTest {

    private final CategoryService categoryService = new CategoryService() {};

    @Test
    @DisplayName("Find category by ID - Success")
    void findCategoryByIdSuccess() {
        UUID categoryId = UUID.randomUUID();
        ResponseEntity<CategoryGetResponse> response = categoryService.find(categoryId);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("Find categories by parent ID - Success")
    void findCategoriesByParentIdSuccess() {
        UUID parentId = UUID.randomUUID();
        ResponseEntity<PaginatedResponse<CategoryGetResponse>> response = categoryService.findByParentId(parentId, 0, 20);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("Find all categories - default returns NOT_IMPLEMENTED")
    void findAllDefault() {
        ResponseEntity<java.util.Collection<CategoryGetResponse>> response = categoryService.findAll();
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("Create category - default returns NOT_IMPLEMENTED")
    void createDefault() {
        CategoryCreateRequest request = new CategoryCreateRequest("Name", "Desc", null, true);
        ResponseEntity<CategoryCreateResponse> response = categoryService.create(request);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

}
