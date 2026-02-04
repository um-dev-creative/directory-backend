package com.umdev.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(value = {SpringExtension.class})
class ProductCreateRequestTest {

    @Test
    @DisplayName("Create ProductCreateRequest successfully")
    void createProductCreateRequestSuccessfully() {
        UUID categoryId = UUID.randomUUID();
        ProductCreateRequest request = new ProductCreateRequest("Example Product", "This is an example product description.", categoryId);

        assertEquals("Example Product", request.name());
        assertEquals("This is an example product description.", request.description());
        assertEquals(categoryId, request.categoryId());
    }
}
