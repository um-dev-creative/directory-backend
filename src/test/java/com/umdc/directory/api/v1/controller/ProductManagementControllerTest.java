package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.ProductService;
import com.umdc.directory.api.v1.to.ProductUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RF-02 product controller and API delegation")
class ProductManagementControllerTest {

    private final UUID id = UUID.randomUUID();
    private final ProductUpdateRequest body = new ProductUpdateRequest("n", null, null, null, null, null, null);
    private final ProductService service = Mockito.mock(ProductService.class);
    private final ProductController controller = new ProductController(service);

    @Test
    @DisplayName("the controller sends PATCH as partial and PUT as full replacement")
    void controllerUpdate() {
        when(service.update(id, body, "t", false)).thenReturn(ResponseEntity.ok().build());
        when(service.update(id, body, "t", true)).thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).build());
        assertEquals(HttpStatus.OK, controller.patchProduct(id, body, "t").getStatusCode());
        assertEquals(HttpStatus.ACCEPTED, controller.putProduct(id, body, "t").getStatusCode());
    }

    @Test
    @DisplayName("the controller delegates DELETE and the listing with include_inactive and token")
    void controllerDeleteAndList() {
        when(service.deactivate(id, "t")).thenReturn(ResponseEntity.noContent().build());
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteProduct(id, "t").getStatusCode());
        controller.findByBusinessId(id, 1, 5, true, true, "t");
        verify(service).findByBusinessId(id, 1, 5, true, true, "t");
    }

    @Test
    @DisplayName("the API default methods call the service (501 by default)")
    void apiDefaults() {
        ProductApi api = new ProductApi() { };
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.patchProduct(id, body, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.putProduct(id, body, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.deleteProduct(id, "t").getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.findByBusinessId(id, 1, 1, null, null, null).getStatusCode());
    }
}
