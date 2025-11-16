package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CampaignListResponse;
import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.api.v1.to.CampaignUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

/**
 * Service boundary for Campaign operations used by REST controllers.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Validate foreign keys (businessId, categoryId) and domain rules (startDate <= endDate)</li>
 *   <li>Assign server-managed fields (id, createdDate, lastUpdate) during creation</li>
 *   <li>Delegate persistence to {@code CampaignRepository}</li>
 * </ul>
 * Return type is {@link ResponseEntity} to allow the implementation to control HTTP status codes.
 */
public interface CampaignService {

    /**
     * Creates a new Campaign.
     * <p>Expected HTTP status codes:</p>
     * <ul>
     *   <li>201 CREATED when the campaign is persisted successfully</li>
     *   <li>400 BAD REQUEST when validation fails (missing required fields, invalid dates, missing FKs)</li>
     *   <li>404 NOT FOUND optionally if related entities are looked up and not found (implementation-specific)</li>
     * </ul>
     * @param campaignTO inbound campaign transfer object; must include name, startDate, endDate, businessId, categoryId
     * @return ResponseEntity containing the persisted CampaignTO (including generated id, timestamps) or error status
     */
    default ResponseEntity<CampaignTO> create(CampaignTO campaignTO) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Finds an existing Campaign by its UUID.
     * <p>Expected HTTP status codes:</p>
     * <ul>
     *   <li>200 OK when found</li>
     *   <li>404 NOT FOUND when no campaign exists for the given id</li>
     * </ul>
     * @param id unique identifier of the campaign
     * @return ResponseEntity with CampaignTO if found or 404 status if not
     */
    default ResponseEntity<CampaignTO> find(UUID id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Lists campaigns with pagination, sorting and optional filters.
     * <p>Expected HTTP status codes:</p>
     * <ul>
     *   <li>200 OK on success</li>
     *   <li>400 BAD REQUEST for invalid pagination or sort parameters</li>
     * </ul>
     * @param page 1-based page number (default 1)
     * @param perPage page size (default 20, max 100)
     * @param sort comma-separated sort fields, prefix with '-' for DESC. Allowed: name, start_date, end_date, created_date
     * @param filters optional filters map (e.g., q, active, business_id, category_id, *_from, *_to)
     * @return ResponseEntity with CampaignListResponse
     */
    default ResponseEntity<CampaignListResponse> list(Integer page, Integer perPage, String sort, Map<String, String> filters) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Updates an existing Campaign.
     * <p>Expected HTTP status codes:</p>
     * <ul>
     *   <li>200 OK when the campaign is updated successfully</li>
     *   <li>400 BAD REQUEST when validation fails (invalid field lengths, invalid dates, invalid FKs)</li>
     *   <li>404 NOT FOUND when the campaign or related entities (category/business) don't exist</li>
     *   <li>409 CONFLICT when optimistic locking detects a concurrent modification</li>
     * </ul>
     * @param id unique identifier of the campaign to update
     * @param request update request with optional fields (only provided fields will be updated)
     * @return ResponseEntity containing the updated CampaignTO (including updated lastUpdate) or error status
     */
    default ResponseEntity<CampaignTO> update(UUID id, CampaignUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
