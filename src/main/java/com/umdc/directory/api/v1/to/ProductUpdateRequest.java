package com.umdc.directory.api.v1.to;

import java.math.BigDecimal;
import java.util.UUID;

/// Body of `PATCH /products/{productId}` (only the sent fields change) and `PUT /products/{productId}` (full replacement: name,
/// description and categoryId are required, an absent price/currency/imageRef is cleared, an absent active keeps its value).
/// In PATCH a blank `imageRef` clears the image.
public record ProductUpdateRequest(
        String name,
        String description,
        UUID categoryId,
        BigDecimal price,
        String currency,
        String imageRef,
        Boolean active
) {
}
