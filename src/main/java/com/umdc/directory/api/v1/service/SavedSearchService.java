package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.LastSearchTO;
import com.umdc.directory.api.v1.to.SavedSearchCreateRequest;
import com.umdc.directory.api.v1.to.SavedSearchListResponse;
import com.umdc.directory.api.v1.to.SavedSearchRenameRequest;
import com.umdc.directory.api.v1.to.SavedSearchTO;
import com.umdc.directory.api.v1.to.SearchParamsTO;
import com.umdc.directory.api.v1.to.SearchReplayTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/// Saved searches and last search of the consumer identified by the session token. Every error carries its reason in `message-error`.
public interface SavedSearchService {

    /// @return `201`; `400` invalid name or params; `401`; `409` duplicate name or limit reached
    default ResponseEntity<SavedSearchTO> create(String sessionToken, SavedSearchCreateRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return `200` with the caller's saved searches; `401`
    default ResponseEntity<SavedSearchListResponse> list(String sessionToken) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return `200`; `400` invalid name; `401`; `404` not found or not the caller's; `409` duplicate name
    default ResponseEntity<SavedSearchTO> rename(String sessionToken, UUID id, SavedSearchRenameRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return `204`; `401`; `404` not found or not the caller's
    default ResponseEntity<Void> delete(String sessionToken, UUID id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Returns what the client must call to rerun the search and stamps `last_used_at`.
    ///
    /// @return `200`; `401`; `404` not found or not the caller's
    default ResponseEntity<SearchReplayTO> replay(String sessionToken, UUID id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return `200`; `401`; `404` when the caller has no last search
    default ResponseEntity<LastSearchTO> getLast(String sessionToken) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Creates or replaces the last search of the caller.
    ///
    /// @return `200`; `400` invalid params; `401`
    default ResponseEntity<LastSearchTO> putLast(String sessionToken, SearchParamsTO params) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
