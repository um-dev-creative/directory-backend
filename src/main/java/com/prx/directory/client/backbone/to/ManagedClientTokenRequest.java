package com.prx.directory.client.backbone.to;

import java.util.List;
import java.util.UUID;

/// Body of backbone-rest's `POST /api/v1/managed-clients/token` (client credentials of Directory's managed client).
///
/// @param clientId     the managed client
/// @param clientSecret its secret; never logged
/// @param scopes       the scopes asked for (a subset of the ones registered for the client)
public record ManagedClientTokenRequest(UUID clientId, String clientSecret, List<String> scopes) {

    @Override
    public String toString() {
        return "ManagedClientTokenRequest[clientId=" + clientId + ", scopes=" + scopes + "]";
    }
}
