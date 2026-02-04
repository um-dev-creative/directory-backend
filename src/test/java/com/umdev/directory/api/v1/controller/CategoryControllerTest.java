package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.CategoryService;
import com.umdev.directory.api.v1.to.CategoryGetResponse;
import com.umdev.directory.api.v1.to.PaginatedResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController controller;

    @Test
    @DisplayName("find returns OK with body")
    void find_ok() {
        var resp = new CategoryGetResponse(UUID.randomUUID(), "name", null, null, null, null, true);
        when(categoryService.find(any(UUID.class))).thenReturn(ResponseEntity.ok(resp));
        var out = controller.find(UUID.randomUUID());
        assertEquals(HttpStatus.OK, out.getStatusCode());
        assertEquals(resp, out.getBody());
    }

    @Test
    @DisplayName("findByParentId returns OK")
    void findByParent_ok() {
        var list = List.of(new CategoryGetResponse(UUID.randomUUID(), "name", null, null, null, null, true));
        var paged = new PaginatedResponse<>(1L, 0, 20, 1, list);
        when(categoryService.findByParentId(any(UUID.class), anyInt(), anyInt())).thenReturn(ResponseEntity.ok(paged));
        ResponseEntity<PaginatedResponse<CategoryGetResponse>> out = controller.findByParentId(UUID.randomUUID(), 0, 20);
        assertEquals(HttpStatus.OK, out.getStatusCode());
        assertEquals(1, out.getBody().items().size());
    }

    @Test
    @DisplayName("findAll returns OK")
    void findAll_ok() {
        var list = List.of(new CategoryGetResponse(UUID.randomUUID(), "name", null, null, null, null, true));
        when(categoryService.findAll()).thenReturn(ResponseEntity.ok(list));
        var out = controller.findAll();
        assertEquals(HttpStatus.OK, out.getStatusCode());
        assertEquals(1, out.getBody().size());
    }
}
