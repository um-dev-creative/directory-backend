package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.FavoriteListDetailTO;
import com.umdc.directory.api.v1.to.FavoriteListItemRequest;
import com.umdc.directory.api.v1.to.FavoriteListItemTO;
import com.umdc.directory.api.v1.to.FavoriteListNameRequest;
import com.umdc.directory.api.v1.to.FavoriteListTO;
import com.umdc.directory.api.v1.to.FavoriteListsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/// Favorite lists of the consumer identified by the session token. Every error carries its reason in `message-error`.
/// A list of another user is `404`, never `403`.
public interface FavoriteListService {

    /// @return `201`; `400` invalid name; `401`; `409` duplicate name or limit reached
    default ResponseEntity<FavoriteListTO> create(String sessionToken, FavoriteListNameRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return `200` with the caller's lists and their item counts; `401`
    default ResponseEntity<FavoriteListsResponse> list(String sessionToken) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return `200`; `400` invalid paging; `401`; `404`
    default ResponseEntity<FavoriteListDetailTO> get(String sessionToken, UUID id, int page, int size) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return `200`; `400` invalid name; `401`; `404`; `409` duplicate name
    default ResponseEntity<FavoriteListTO> rename(String sessionToken, UUID id, FavoriteListNameRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Deletes the list and its items; the favorites themselves are kept.
    ///
    /// @return `204`; `401`; `404`
    default ResponseEntity<Void> delete(String sessionToken, UUID id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return `201`; `400` no favorite id or offer favorite; `401`; `404` list or favorite not the caller's;
    /// `409` favorite inactive or already in the list
    default ResponseEntity<FavoriteListItemTO> addItem(String sessionToken, UUID id, FavoriteListItemRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return `204`; `401`; `404` list not the caller's or favorite not in it
    default ResponseEntity<Void> removeItem(String sessionToken, UUID id, UUID favoriteId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
