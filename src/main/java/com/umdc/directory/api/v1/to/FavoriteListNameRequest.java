package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

/// Body of `POST /users/me/favorite-lists` and `PATCH /users/me/favorite-lists/{id}`.
public record FavoriteListNameRequest(@JsonProperty("name") String name) {
}
