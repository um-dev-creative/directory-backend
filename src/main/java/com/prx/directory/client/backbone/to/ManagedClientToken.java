package com.prx.directory.client.backbone.to;

/// What backbone-rest answers to a managed-client token request: the Backbone M2M token and its life in seconds.
///
/// @param accessToken the Backbone M2M token (opaque to Directory)
/// @param expiresIn   seconds it is valid from issuance
public record ManagedClientToken(String accessToken, long expiresIn) {

    @Override
    public String toString() {
        return "ManagedClientToken[expiresIn=" + expiresIn + "]";
    }
}
