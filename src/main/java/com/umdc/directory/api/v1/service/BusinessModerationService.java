package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessModerationTO;
import com.umdc.directory.api.v1.to.VerificationConfirmRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/// Moderation (S8) and ownership verification of businesses.
public interface BusinessModerationService {

    /// Sends a verification code to the owner's e-mail through Mercury. Owner only.
    ///
    /// @param businessId the business
    /// @param token      the session token of the caller
    /// @return 202 when queued; 401, 403, 404, 429 (cooldown or Mercury throttling, with `Retry-After`), 503
    default ResponseEntity<Void> requestVerification(UUID businessId, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Confirms the code the owner received; the business becomes `verified`. Owner only.
    ///
    /// @param businessId the business
    /// @param request    the code
    /// @param token      the session token of the caller
    /// @return 204 when confirmed; 400 wrong or malformed code, 401, 403, 404, 409 no pending code, 410 expired, 429 too many attempts
    default ResponseEntity<Void> confirmVerification(UUID businessId, VerificationConfirmRequest request, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// The moderation status of a business. Owner only.
    ///
    /// @param businessId the business
    /// @param token      the session token of the caller
    /// @return the status; 401, 403, 404
    default ResponseEntity<BusinessModerationTO> status(UUID businessId, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Approves a business. Only the configured admin role (`umdc.moderation.admin-role`).
    ///
    /// @param businessId the business
    /// @param token      the session token of the caller
    /// @return the new state; 401, 403 (not an admin), 404
    default ResponseEntity<BusinessModerationTO> approve(UUID businessId, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Rejects a business. Only the configured admin role (`umdc.moderation.admin-role`).
    ///
    /// @param businessId the business
    /// @param token      the session token of the caller
    /// @return the new state; 401, 403 (not an admin), 404
    default ResponseEntity<BusinessModerationTO> reject(UUID businessId, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
