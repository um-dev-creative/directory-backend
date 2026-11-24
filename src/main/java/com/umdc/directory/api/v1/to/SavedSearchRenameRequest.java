package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

/// Body of `PATCH /users/me/saved-searches/{id}`.
public record SavedSearchRenameRequest(@JsonProperty("name") String name) {
}
