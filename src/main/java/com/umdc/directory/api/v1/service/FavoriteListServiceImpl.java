package com.umdc.directory.api.v1.service;

import com.umdc.directory.security.SessionTokenVerifier;
import com.umdc.directory.api.v1.to.FavoriteListDetailTO;
import com.umdc.directory.api.v1.to.FavoriteListItemRequest;
import com.umdc.directory.api.v1.to.FavoriteListItemTO;
import com.umdc.directory.api.v1.to.FavoriteListNameRequest;
import com.umdc.directory.api.v1.to.FavoriteListTO;
import com.umdc.directory.api.v1.to.FavoriteListsResponse;
import com.umdc.directory.api.v1.to.PaginatedResponse;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.constant.FavoriteListConstants;
import com.umdc.directory.jpa.entity.FavoriteListEntity;
import com.umdc.directory.jpa.entity.FavoriteListItemEntity;
import com.umdc.directory.jpa.repository.FavoriteListItemRepository;
import com.umdc.directory.jpa.repository.FavoriteListRepository;
import com.umdc.directory.jpa.repository.UserFavoriteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class FavoriteListServiceImpl implements FavoriteListService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteListServiceImpl.class);

    private final FavoriteListRepository listRepository;
    private final FavoriteListItemRepository itemRepository;
    private final UserFavoriteRepository favoriteRepository;
    private final SessionTokenVerifier sessionTokenVerifier;

    public FavoriteListServiceImpl(FavoriteListRepository listRepository,
                                   FavoriteListItemRepository itemRepository,
                                   UserFavoriteRepository favoriteRepository) {
        this(listRepository, itemRepository, favoriteRepository, SessionTokenVerifier.unverified());
    }

    @Autowired
    public FavoriteListServiceImpl(FavoriteListRepository listRepository,
                                   FavoriteListItemRepository itemRepository,
                                   UserFavoriteRepository favoriteRepository,
                                   SessionTokenVerifier sessionTokenVerifier) {
        this.sessionTokenVerifier = Objects.requireNonNullElseGet(sessionTokenVerifier, SessionTokenVerifier::unverified);
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.favoriteRepository = favoriteRepository;
    }

    /// Not transactional on purpose: a unique-index violation must be answered with 409 (see FavoriteServiceImpl).
    @Override
    public ResponseEntity<FavoriteListTO> create(String sessionToken, FavoriteListNameRequest request) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        if (Objects.isNull(request)) {
            return error(HttpStatus.BAD_REQUEST, FavoriteListConstants.BODY_REQUIRED);
        }
        var name = cleanName(request.name());
        if (Objects.isNull(name)) {
            return error(HttpStatus.BAD_REQUEST, FavoriteListConstants.INVALID_NAME);
        }
        if (listRepository.countByUserFk(userId) >= FavoriteListConstants.MAX_PER_USER) {
            return error(HttpStatus.CONFLICT, FavoriteListConstants.LIMIT_REACHED);
        }
        if (listRepository.existsByUserFkAndNameIgnoreCase(userId, name)) {
            return error(HttpStatus.CONFLICT, FavoriteListConstants.DUPLICATE_NAME);
        }
        var entity = new FavoriteListEntity();
        var now = LocalDateTime.now();
        entity.setUserFk(userId);
        entity.setName(name);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(FavoriteListMapper.toTO(listRepository.saveAndFlush(entity), 0));
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("Favorite list rejected by a database constraint: {}", ex.getMostSpecificCause().getMessage());
            return error(HttpStatus.CONFLICT, FavoriteListConstants.DUPLICATE_NAME);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<FavoriteListsResponse> list(String sessionToken) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        var lists = listRepository.findByUserFkOrderByCreatedAtDesc(userId);
        Map<UUID, Long> counts = new ConcurrentHashMap<>();
        if (!lists.isEmpty()) {
            var ids = lists.stream().map(FavoriteListEntity::getId).toList();
            for (Object[] row : itemRepository.countActiveByLists(ids)) {
                counts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }
        var items = lists.stream().map(l -> FavoriteListMapper.toTO(l, counts.getOrDefault(l.getId(), 0L))).toList();
        return ResponseEntity.ok(new FavoriteListsResponse(items, FavoriteListConstants.MAX_PER_USER));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<FavoriteListDetailTO> get(String sessionToken, UUID id, int page, int size) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        var found = listRepository.findByIdAndUserFk(id, userId);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, FavoriteListConstants.LIST_NOT_FOUND);
        }
        if (page < 0 || size < 1) {
            return error(HttpStatus.BAD_REQUEST, FavoriteListConstants.INVALID_PAGING);
        }
        int perPage = Math.min(size, FavoriteListConstants.MAX_PAGE_SIZE);
        var result = itemRepository.findPageByList(id,
                PageRequest.of(page, perPage, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"))));
        var items = result.getContent().stream().map(FavoriteListMapper::toItemTO).toList();
        var paged = new PaginatedResponse<>(result.getTotalElements(), page, perPage, result.getTotalPages(), items);
        var list = found.get();
        return ResponseEntity.ok(new FavoriteListDetailTO(list.getId(), list.getName(), list.getCreatedAt(), list.getUpdatedAt(), paged));
    }

    @Override
    public ResponseEntity<FavoriteListTO> rename(String sessionToken, UUID id, FavoriteListNameRequest request) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        var found = listRepository.findByIdAndUserFk(id, userId);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, FavoriteListConstants.LIST_NOT_FOUND);
        }
        var name = cleanName(Objects.isNull(request) ? null : request.name());
        if (Objects.isNull(name)) {
            return error(HttpStatus.BAD_REQUEST, FavoriteListConstants.INVALID_NAME);
        }
        if (listRepository.existsByUserFkAndNameIgnoreCaseAndIdNot(userId, name, id)) {
            return error(HttpStatus.CONFLICT, FavoriteListConstants.DUPLICATE_NAME);
        }
        var entity = found.get();
        entity.setName(name);
        entity.setUpdatedAt(LocalDateTime.now());
        try {
            var saved = listRepository.saveAndFlush(entity);
            return ResponseEntity.ok(FavoriteListMapper.toTO(saved, itemRepository.countActiveByList(id)));
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("Favorite list rename rejected by a database constraint: {}", ex.getMostSpecificCause().getMessage());
            return error(HttpStatus.CONFLICT, FavoriteListConstants.DUPLICATE_NAME);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Void> delete(String sessionToken, UUID id) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        var found = listRepository.findByIdAndUserFk(id, userId);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, FavoriteListConstants.LIST_NOT_FOUND);
        }
        itemRepository.deleteByList(id);
        listRepository.delete(found.get());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FavoriteListItemTO> addItem(String sessionToken, UUID id, FavoriteListItemRequest request) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        if (listRepository.findByIdAndUserFk(id, userId).isEmpty()) {
            return error(HttpStatus.NOT_FOUND, FavoriteListConstants.LIST_NOT_FOUND);
        }
        if (Objects.isNull(request) || Objects.isNull(request.favoriteId())) {
            return error(HttpStatus.BAD_REQUEST, FavoriteListConstants.FAVORITE_ID_REQUIRED);
        }
        var favorite = favoriteRepository.findById(request.favoriteId())
                .filter(f -> userId.equals(f.getUserFk()));
        if (favorite.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, FavoriteListConstants.FAVORITE_NOT_FOUND);
        }
        var fav = favorite.get();
        if (!Boolean.TRUE.equals(fav.getActive())) {
            return error(HttpStatus.CONFLICT, FavoriteListConstants.FAVORITE_INACTIVE);
        }
        if (Objects.isNull(fav.getBusiness()) && Objects.isNull(fav.getProduct())) {
            return error(HttpStatus.BAD_REQUEST, FavoriteListConstants.UNSUPPORTED_TYPE);
        }
        if (itemRepository.findByListAndFavorite(id, fav.getId()).isPresent()) {
            return error(HttpStatus.CONFLICT, FavoriteListConstants.ALREADY_IN_LIST);
        }
        var item = new FavoriteListItemEntity();
        item.setListFk(id);
        item.setFavorite(fav);
        item.setCreatedAt(LocalDateTime.now());
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(FavoriteListMapper.toItemTO(itemRepository.saveAndFlush(item)));
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("Favorite list item rejected by a database constraint: {}", ex.getMostSpecificCause().getMessage());
            return error(HttpStatus.CONFLICT, FavoriteListConstants.ALREADY_IN_LIST);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Void> removeItem(String sessionToken, UUID id, UUID favoriteId) {
        var userId = sessionTokenVerifier.uid(sessionToken);
        if (Objects.isNull(userId)) {
            return unauthorized();
        }
        if (listRepository.findByIdAndUserFk(id, userId).isEmpty()) {
            return error(HttpStatus.NOT_FOUND, FavoriteListConstants.LIST_NOT_FOUND);
        }
        var item = itemRepository.findByListAndFavorite(id, favoriteId);
        if (item.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, FavoriteListConstants.ITEM_NOT_IN_LIST);
        }
        itemRepository.delete(item.get());
        return ResponseEntity.noContent().build();
    }

    /// @return the trimmed name, or null when it is blank or longer than the limit
    private static String cleanName(String name) {
        if (Objects.isNull(name) || name.isBlank() || name.trim().length() > FavoriteListConstants.MAX_NAME_LENGTH) {
            return null;
        }
        return name.trim();
    }

    private static <T> ResponseEntity<T> unauthorized() {
        return error(HttpStatus.UNAUTHORIZED, DirectoryAppConstants.UNAUTHORIZED_MESSAGE);
    }

    private static <T> ResponseEntity<T> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, message).build();
    }
}
