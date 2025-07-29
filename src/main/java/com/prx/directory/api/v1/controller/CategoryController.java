package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.CategoryService;
import com.prx.directory.api.v1.to.CategoryGetResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController implements CategoryApi {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public ResponseEntity<CategoryGetResponse> find(UUID categoryId) {
        return categoryService.find(categoryId);
    }

    @Override
    public ResponseEntity<Collection<CategoryGetResponse>> findByParentId(UUID parentId) {
        return categoryService.findByParentId(parentId);
    }

    @Override
    public ResponseEntity<Collection<CategoryGetResponse>> findAll() {
        return categoryService.findAll();
    }
}
