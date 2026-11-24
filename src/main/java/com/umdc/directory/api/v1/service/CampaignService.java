package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.CampaignListResponse;
import com.umdc.directory.api.v1.to.CampaignTO;
import com.umdc.directory.api.v1.to.CampaignUpdateRequest;
import com.umdc.directory.api.v1.to.CampaignUpdateResponse;
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
     *   <li>401 UNAUTHORIZED when the session token is missing or invalid</li>
     *   <li>403 FORBIDDEN when the token user does not own the business</li>
     *   <li>404 NOT FOUND when the business does not exist; optionally if related entities are looked up and not found (implementation-specific)</li>
     * </ul>
     * @param campaignTO inbound campaign transfer object; must include title, startDate, endDate, businessId, categoryId
     * @return ResponseEntity containing the persisted CampaignTO (including generated id, timestamps) or error status
     */
    default ResponseEntity<CampaignTO> create(CampaignTO campaignTO, String token) {
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
     * @param sort comma-separated sort fields, prefix with '-' for DESC. Allowed: title, start_date, end_date, created_date
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
     *   <li>401 UNAUTHORIZED when the session token is missing or invalid</li>
     *   <li>403 FORBIDDEN when the token user does not own the campaign's business (or the target business)</li>
     *   <li>409 CONFLICT when optimistic locking detects a concurrent modification</li>
     * </ul>
     * @param id unique identifier of the campaign to update
     * @param request update request with optional fields (only provided fields will be updated)
     * @return ResponseEntity containing a minimal CampaignUpdateResponse (id + lastUpdate) with HTTP 202 Accepted on success, or an error status
     */
    default ResponseEntity<CampaignUpdateResponse> update(UUID id, CampaignUpdateRequest request, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Lists campaigns as seen by the caller: the public sees scheduled and active campaigns only; the owner of a
     * business also sees its draft, paused and expired campaigns. Extra filters: {@code status}, {@code type}.
     * <p>Answers as {@link #list(Integer, Integer, String, Map)}; an invalid token is treated as no token.</p>
     * @param token optional session token
     */
    default ResponseEntity<CampaignListResponse> list(Integer page, Integer perPage, String sort,
                                                      Map<String, String> filters, String token) {
        return list(page, perPage, sort, filters);
    }

    /**
     * Finds a campaign as seen by the caller: 404 when it is hidden from consumers (draft, paused, expired) and the
     * caller does not own its business.
     * @param token optional session token
     */
    default ResponseEntity<CampaignTO> find(UUID id, String token) {
        return find(id);
    }

    /**
     * Deactivates (pauses) a campaign. 200 with the campaign; 401, 403, 404; 409 when the status does not allow it
     * (only ACTIVE and SCHEDULED campaigns can be paused).
     */
    default ResponseEntity<CampaignTO> deactivate(UUID id, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Reactivates a paused campaign. 200 with the campaign; 401, 403, 404; 409 when it is not PAUSED.
     */
    default ResponseEntity<CampaignTO> reactivate(UUID id, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Publishes a draft (makes it active). 200 with the campaign; 401, 403, 404; 409 when it is not a draft or its
     * end date has passed.
     */
    default ResponseEntity<CampaignTO> publish(UUID id, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
