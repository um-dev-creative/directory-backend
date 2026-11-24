package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

/// Body of `POST /users/me/saved-searches`.
public record SavedSearchCreateRequest(
        @JsonProperty("name") String name,
        @JsonProperty("params") SearchParamsTO params) {
}
