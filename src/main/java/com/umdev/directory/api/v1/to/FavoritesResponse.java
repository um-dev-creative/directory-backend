package com.umdev.directory.api.v1.to;

import java.util.List;
import java.util.Objects;

/**
 * Response DTO grouping favorites by type.
 */
public record FavoritesResponse(
        List<BusinessTO> stores,
        List<ProductCreateResponse> products,
        List<OfferTO> offers
) {
    public FavoritesResponse {
        stores = Objects.requireNonNullElse(stores, List.of());
        products = Objects.requireNonNullElse(products, List.of());
        offers = Objects.requireNonNullElse(offers, List.of());
    }
}

