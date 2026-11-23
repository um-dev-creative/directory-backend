package com.umdc.directory.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/// Request to create a product. `businessId` is optional: when present the product is also linked, as active, to that
/// business (the caller must own it); when absent the product is only created and linked later with `POST /products/link`.
/// `price` and `currency` are sent together or not at all.
public record ProductCreateRequest(
        @NotBlank(message = "Empty value invalid,description required")
        String name,
        @NotBlank(message = "Empty value invalid,description required")
        String description,
        @NotNull
        UUID categoryId,
        BigDecimal price,
        String currency,
        String imageRef,
        UUID businessId
) {

    /// Legacy constructor without price, image and business.
    public ProductCreateRequest(String name, String description, UUID categoryId) {
        this(name, description, categoryId, null, null, null, null);
    }

    @Override
    public String toString() {
        return "ProductCreateRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", categoryId=" + categoryId +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", imageRef='" + imageRef + '\'' +
                ", businessId=" + businessId +
                '}';
    }
}
