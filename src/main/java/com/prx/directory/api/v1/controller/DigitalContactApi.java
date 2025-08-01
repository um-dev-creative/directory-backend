package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.DigitalContactService;
import com.prx.directory.api.v1.to.DigitalContactTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

public interface DigitalContactApi {

    default DigitalContactService getService() {
        return new DigitalContactService() {};
    }

    /**
     * Retrieve a paginated list of digital contacts.
     *
     * @param page the page number (default 0)
     * @param size the page size (default 10)
     * @return paginated list of digital contacts
     */
    @Operation(summary = "Get all digital contacts", description = "Returns a paginated list of digital contacts.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful retrieval of digital contacts"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    @GetMapping
    default ResponseEntity<Page<DigitalContactTO>> getAllDigitalContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(getService().getAllDigitalContacts(Pageable.ofSize(size)));
    }

    /**
     * Retrieve a digital contact by its unique identifier.
     *
     * @param id the unique identifier of the digital contact
     * @return the digital contact details
     */
    @Operation(summary = "Get digital contact by ID", description = "Returns the details of a specific digital contact by its unique identifier.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful retrieval of digital contact"),
        @ApiResponse(responseCode = "400", description = "Invalid ID format"),
        @ApiResponse(responseCode = "404", description = "Digital contact not found")
    })
    @GetMapping("/{id}")
    default ResponseEntity<DigitalContactTO> getDigitalContactById(@PathVariable UUID id) {
        return ResponseEntity.ok(getService().getDigitalContactById(id).orElse(null));
    }
    
    /**
     * Retrieve a list of digital contacts by business ID.
     *
     * @param businessId the unique identifier of the business
     * @return list of digital contacts for the specified business
     */
    @Operation(summary = "Get digital contacts by business ID", description = "Returns a list of digital contacts associated with the specified business ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful retrieval of digital contacts for business"),
        @ApiResponse(responseCode = "400", description = "Invalid business ID format"),
        @ApiResponse(responseCode = "404", description = "No digital contacts found for business")
    })
    @GetMapping("/business/{businessId}")
    default ResponseEntity<List<DigitalContactTO>> getDigitalContactsByBusinessId(@PathVariable UUID businessId) {
        List<DigitalContactTO> contacts = getService().getDigitalContactsByBusinessId(businessId);
        if (contacts == null || contacts.isEmpty()) {
            return ResponseEntity.status(404).body(List.of());
        }
        return ResponseEntity.ok(contacts);
    }
}
