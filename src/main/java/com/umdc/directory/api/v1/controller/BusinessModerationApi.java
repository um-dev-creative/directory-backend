package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessModerationService;
import com.umdc.directory.api.v1.to.BusinessModerationTO;
import com.umdc.directory.api.v1.to.VerificationConfirmRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Moderation and ownership verification of a business.
@Tag(name = "Business moderation", description = "Ownership verification code (through Mercury) and moderation status")
public interface BusinessModerationApi {

    String UNAUTHORIZED = "401";
    String UNAUTHORIZED_TEXT = "Unauthorized";
    String FORBIDDEN = "403";
    String NOT_FOUND = "404";
    String BUSINESS_NOT_FOUND = "Business not found";

    default BusinessModerationService getService() {
        return new BusinessModerationService() {};
    }

    @Operation(summary = "Send the ownership verification code", description = "Sends a 9-digit code to the owner's e-mail through Mercury. Owner only. One e-mail per minute.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "The code was queued", content = @Content),
            @ApiResponse(responseCode = UNAUTHORIZED, description = UNAUTHORIZED_TEXT, content = @Content),
            @ApiResponse(responseCode = FORBIDDEN, description = "The caller is not the owner", content = @Content),
            @ApiResponse(responseCode = NOT_FOUND, description = BUSINESS_NOT_FOUND, content = @Content),
            @ApiResponse(responseCode = "409", description = "The owner has no e-mail", content = @Content),
            @ApiResponse(responseCode = "429", description = "Too soon, or Mercury is throttling (Retry-After)", content = @Content),
            @ApiResponse(responseCode = "503", description = "Mercury is not available", content = @Content)
    })
    @PostMapping(path = "/verification/request")
    default ResponseEntity<Void> requestVerification(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().requestVerification(id, sessionToken);
    }

    @Operation(summary = "Confirm the ownership verification code", description = "Marks the business as verified. Owner only. The code lasts 15 minutes and allows 5 wrong tries.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Verified", content = @Content),
            @ApiResponse(responseCode = "400", description = "Wrong or malformed code", content = @Content),
            @ApiResponse(responseCode = UNAUTHORIZED, description = UNAUTHORIZED_TEXT, content = @Content),
            @ApiResponse(responseCode = FORBIDDEN, description = "The caller is not the owner", content = @Content),
            @ApiResponse(responseCode = NOT_FOUND, description = BUSINESS_NOT_FOUND, content = @Content),
            @ApiResponse(responseCode = "409", description = "No code is pending", content = @Content),
            @ApiResponse(responseCode = "410", description = "The code expired", content = @Content),
            @ApiResponse(responseCode = "429", description = "Too many wrong codes", content = @Content)
    })
    @PostMapping(path = "/verification/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<Void> confirmVerification(@PathVariable("id") UUID id, @RequestBody VerificationConfirmRequest request,
            @Parameter(hidden = true) @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().confirmVerification(id, request, sessionToken);
    }

    @Operation(summary = "Moderation status of my business", description = "PENDING, APPROVED or REJECTED, and whether the ownership was verified. Owner only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The status"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = UNAUTHORIZED_TEXT, content = @Content),
            @ApiResponse(responseCode = FORBIDDEN, description = "The caller is not the owner", content = @Content),
            @ApiResponse(responseCode = NOT_FOUND, description = BUSINESS_NOT_FOUND, content = @Content)
    })
    @GetMapping(path = "/moderation", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<BusinessModerationTO> status(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().status(id, sessionToken);
    }

    @Operation(summary = "Approve a business", description = "Makes the business public. Only the role set in umdc.moderation.admin-role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Approved"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = UNAUTHORIZED_TEXT, content = @Content),
            @ApiResponse(responseCode = FORBIDDEN, description = "The caller is not a moderator", content = @Content),
            @ApiResponse(responseCode = NOT_FOUND, description = BUSINESS_NOT_FOUND, content = @Content)
    })
    @PostMapping(path = "/moderation/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<BusinessModerationTO> approve(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().approve(id, sessionToken);
    }

    @Operation(summary = "Reject a business", description = "Hides the business from the public. Only the role set in umdc.moderation.admin-role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rejected"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = UNAUTHORIZED_TEXT, content = @Content),
            @ApiResponse(responseCode = FORBIDDEN, description = "The caller is not a moderator", content = @Content),
            @ApiResponse(responseCode = NOT_FOUND, description = BUSINESS_NOT_FOUND, content = @Content)
    })
    @PostMapping(path = "/moderation/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<BusinessModerationTO> reject(@PathVariable("id") UUID id,
            @Parameter(hidden = true) @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().reject(id, sessionToken);
    }
}
