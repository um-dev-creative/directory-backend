package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/// Body of `POST /users/me/favorite-lists/{id}/items`: the id of one of the caller's active favorites.
public record FavoriteListItemRequest(@JsonProperty("favorite_id") UUID favoriteId) {
}
