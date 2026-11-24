package com.umdc.directory.api.v1.service;

import com.umdc.directory.security.SessionTokenVerifier;
import com.umdc.directory.api.v1.to.LastSearchTO;
import com.umdc.directory.api.v1.to.SavedSearchCreateRequest;
import com.umdc.directory.api.v1.to.SavedSearchListResponse;
import com.umdc.directory.api.v1.to.SavedSearchRenameRequest;
import com.umdc.directory.api.v1.to.SavedSearchTO;
import com.umdc.directory.api.v1.to.SearchParamsTO;
import com.umdc.directory.api.v1.to.SearchReplayTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.constant.SavedSearchConstants;
import com.umdc.directory.jpa.entity.LastSearchEntity;
import com.umdc.directory.jpa.entity.SavedSearchEntity;
import com.umdc.directory.jpa.entity.SearchParamsBase;
import com.umdc.directory.jpa.repository.LastSearchRepository;
import com.umdc.directory.jpa.repository.SavedSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class SavedSearchServiceImpl implements SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;
    private final LastSearchRepository lastSearchRepository;
    private final SessionTokenVerifier sessionTokenVerifier;

    public SavedSearchServiceImpl(SavedSearchRepository savedSearchRepository, LastSearchRepository lastSearchRepository) {
        this(savedSearchRepository, lastSearchRepository, SessionTokenVerifier.unverified());
    }

    @Autowired
    public SavedSearchServiceImpl(SavedSearchRepository savedSearchRepository, LastSearchRepository lastSearchRepository,
                                  SessionTokenVerifier sessionTokenVerifier) {
        this.sessionTokenVerifier = Objects.requireNonNullElseGet(sessionTokenVerifier, SessionTokenVerifier::unverified);
        this.savedSearchRepository = savedSearchRepository;
        this.lastSearchRepository = lastSearchRepository;
    }

    @Override
    @Transactional
    public ResponseEntity<SavedSearchTO> create(String sessionToken, SavedSearchCreateRequest request) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        if (Objects.isNull(request)) {
            return error(HttpStatus.BAD_REQUEST, SavedSearchConstants.BODY_REQUIRED);
        }
        var name = cleanName(request.name());
        if (Objects.isNull(name)) {
            return error(HttpStatus.BAD_REQUEST, SavedSearchConstants.INVALID_NAME);
        }
        if (Objects.isNull(request.params())) {
            return error(HttpStatus.BAD_REQUEST, SavedSearchConstants.PARAMS_REQUIRED);
        }
        var entity = new SavedSearchEntity();
        try {
            SearchParamsApplier.apply(request.params(), entity);
        } catch (GeoSearchException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (savedSearchRepository.countByUserFk(userId) >= SavedSearchConstants.MAX_PER_USER) {
            return error(HttpStatus.CONFLICT, SavedSearchConstants.LIMIT_REACHED);
        }
        if (savedSearchRepository.existsByUserFkAndNameIgnoreCase(userId, name)) {
            return error(HttpStatus.CONFLICT, SavedSearchConstants.DUPLICATE_NAME);
        }
        var now = LocalDateTime.now();
        entity.setUserFk(userId);
        entity.setName(name);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return ResponseEntity.status(HttpStatus.CREATED).body(toTO(savedSearchRepository.save(entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<SavedSearchListResponse> list(String sessionToken) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        var items = savedSearchRepository.findByUserFkOrderByCreatedAtDesc(userId).stream().map(SavedSearchServiceImpl::toTO).toList();
        return ResponseEntity.ok(new SavedSearchListResponse(items, SavedSearchConstants.MAX_PER_USER));
    }

    @Override
    @Transactional
    public ResponseEntity<SavedSearchTO> rename(String sessionToken, UUID id, SavedSearchRenameRequest request) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        var found = savedSearchRepository.findByIdAndUserFk(id, userId);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, SavedSearchConstants.SAVED_SEARCH_NOT_FOUND);
        }
        var name = cleanName(Objects.isNull(request) ? null : request.name());
        if (Objects.isNull(name)) {
            return error(HttpStatus.BAD_REQUEST, SavedSearchConstants.INVALID_NAME);
        }
        if (savedSearchRepository.existsByUserFkAndNameIgnoreCaseAndIdNot(userId, name, id)) {
            return error(HttpStatus.CONFLICT, SavedSearchConstants.DUPLICATE_NAME);
        }
        var entity = found.get();
        entity.setName(name);
        entity.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toTO(savedSearchRepository.save(entity)));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> delete(String sessionToken, UUID id) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        var found = savedSearchRepository.findByIdAndUserFk(id, userId);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, SavedSearchConstants.SAVED_SEARCH_NOT_FOUND);
        }
        savedSearchRepository.delete(found.get());
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<SearchReplayTO> replay(String sessionToken, UUID id) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        var found = savedSearchRepository.findByIdAndUserFk(id, userId);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, SavedSearchConstants.SAVED_SEARCH_NOT_FOUND);
        }
        var entity = found.get();
        entity.setLastUsedAt(LocalDateTime.now());
        savedSearchRepository.save(entity);
        return ResponseEntity.ok(toReplay(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<LastSearchTO> getLast(String sessionToken) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        return lastSearchRepository.findById(userId)
                .map(e -> ResponseEntity.ok(new LastSearchTO(SearchParamsApplier.toParams(e), e.getUpdatedAt())))
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, SavedSearchConstants.LAST_SEARCH_NOT_FOUND));
    }

    @Override
    @Transactional
    public ResponseEntity<LastSearchTO> putLast(String sessionToken, SearchParamsTO params) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        if (Objects.isNull(params)) {
            return error(HttpStatus.BAD_REQUEST, SavedSearchConstants.BODY_REQUIRED);
        }
        var entity = lastSearchRepository.findById(userId).orElseGet(() -> {
            var created = new LastSearchEntity();
            created.setUserFk(userId);
            return created;
        });
        try {
            SearchParamsApplier.apply(params, entity);
        } catch (GeoSearchException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        var saved = lastSearchRepository.save(entity);
        return ResponseEntity.ok(new LastSearchTO(SearchParamsApplier.toParams(saved), saved.getUpdatedAt()));
    }

    /// @return the trimmed name, or null when it is blank or longer than the limit
    private static String cleanName(String name) {
        if (Objects.isNull(name) || name.isBlank() || name.trim().length() > SavedSearchConstants.MAX_NAME_LENGTH) {
            return null;
        }
        return name.trim();
    }

    private static SavedSearchTO toTO(SavedSearchEntity e) {
        return new SavedSearchTO(e.getId(), e.getName(), SearchParamsApplier.toParams(e), e.getCreatedAt(), e.getUpdatedAt(), e.getLastUsedAt());
    }

    /// Picks the endpoint (bbox: map, point: nearby, else search) and the query parameter names that endpoint expects.
    private static SearchReplayTO toReplay(SearchParamsBase e) {
        var params = new LinkedHashMap<String, String>();
        put(params, "q", e.getQ());
        put(params, "category_id", e.getCategoryId());
        put(params, "city_id", e.getCityId());
        String endpoint;
        if (Objects.nonNull(e.getBbox())) {
            endpoint = SavedSearchConstants.ENDPOINT_MAP;
            put(params, "bbox", e.getBbox());
        } else if (Objects.nonNull(e.getLat())) {
            endpoint = SavedSearchConstants.ENDPOINT_NEARBY;
            put(params, "lat", e.getLat());
            put(params, "lng", e.getLng());
            put(params, "radius_m", e.getRadiusM());
            put(params, "sort", e.getSort());
        } else {
            endpoint = SavedSearchConstants.ENDPOINT_SEARCH;
            put(params, "city", e.getCity());
        }
        return new SearchReplayTO(endpoint, params);
    }

    private static void put(Map<String, String> params, String key, Object value) {
        if (Objects.nonNull(value)) {
            params.put(key, value.toString());
        }
    }

    private static <T> ResponseEntity<T> unauthorized() {
        return error(HttpStatus.UNAUTHORIZED, DirectoryAppConstants.UNAUTHORIZED_MESSAGE);
    }

    private static <T> ResponseEntity<T> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, message).build();
    }
}
