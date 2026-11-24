package com.umdc.directory.api.v1.service;

import com.umdc.directory.security.SessionTokenVerifier;
import com.umdc.directory.api.v1.to.*;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.constant.FavoriteType;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CampaignEntity;
import com.umdc.directory.jpa.entity.ProductEntity;
import com.umdc.directory.jpa.entity.UserFavoriteEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CampaignRepository;
import com.umdc.directory.jpa.repository.ProductRepository;
import com.umdc.directory.jpa.repository.UserFavoriteRepository;
import com.umdc.directory.mapper.BusinessMapper;
import com.umdc.directory.mapper.CampaignMapper;
import com.umdc.directory.mapper.FavoriteMapper;
import com.umdc.directory.mapper.ProductMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteServiceImpl.class);
    private static final int BUCKET_STORE = 0;
    private static final int BUCKET_PRODUCT = 1;
    private static final int BUCKET_OFFER = 2;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int SORT_PARTS_MAX = 2;

    private final UserFavoriteRepository userFavoriteRepository;
    private final BusinessRepository businessRepository;
    private final ProductRepository productRepository;
    private final CampaignRepository campaignRepository;
    private final FavoriteMapper favoriteMapper;
    private final BusinessMapper businessMapper;
    private final ContactTypeProvider contactTypeProvider;
    private final ProductMapper productMapper;
    private final CampaignMapper campaignMapper;
    private final SessionTokenVerifier sessionTokenVerifier;

    public FavoriteServiceImpl(UserFavoriteRepository userFavoriteRepository,
                               BusinessRepository businessRepository,
                               ProductRepository productRepository,
                               CampaignRepository campaignRepository,
                               FavoriteMapper favoriteMapper,
                               BusinessMapper businessMapper,
                               ContactTypeProvider contactTypeProvider,
                               ProductMapper productMapper,
                               CampaignMapper campaignMapper) {
        this(userFavoriteRepository, businessRepository, productRepository, campaignRepository, favoriteMapper,
                businessMapper, contactTypeProvider, productMapper, campaignMapper, SessionTokenVerifier.unverified());
    }

    @Autowired
    public FavoriteServiceImpl(UserFavoriteRepository userFavoriteRepository,
                               BusinessRepository businessRepository,
                               ProductRepository productRepository,
                               CampaignRepository campaignRepository,
                               FavoriteMapper favoriteMapper,
                               BusinessMapper businessMapper,
                               ContactTypeProvider contactTypeProvider,
                               ProductMapper productMapper,
                               CampaignMapper campaignMapper,
                               SessionTokenVerifier sessionTokenVerifier) {
        this.sessionTokenVerifier = Objects.requireNonNullElseGet(sessionTokenVerifier, SessionTokenVerifier::unverified);
        this.userFavoriteRepository = userFavoriteRepository;
        this.businessRepository = businessRepository;
        this.productRepository = productRepository;
        this.campaignRepository = campaignRepository;
        this.favoriteMapper = favoriteMapper;
        this.businessMapper = businessMapper;
        this.contactTypeProvider = contactTypeProvider;
        this.productMapper = productMapper;
        this.campaignMapper = campaignMapper;
    }

    @Override
    public ResponseEntity<FavoriteResponse> createFavorite(String sessionToken, FavoriteCreateRequest request) {
        // validate request
        if (Objects.isNull(request)) {
            return ResponseEntity.badRequest().build();
        }

        UUID itemId = request.itemId();
        FavoriteType type = request.type();

        UUID userId = sessionTokenVerifier.uid(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return switch (type) {
            case STORE -> processFavorite(userId,
                    () -> businessRepository.findById(itemId),
                    UserFavoriteEntity::setBusiness,
                    () -> userFavoriteRepository.findByUserIdAndBusinessId(userId, itemId));
            case PRODUCT -> processFavorite(userId,
                    () -> productRepository.findById(itemId),
                    UserFavoriteEntity::setProduct,
                    () -> userFavoriteRepository.findByUserIdAndProductId(userId, itemId));
            case OFFER -> processFavorite(userId,
                    () -> campaignRepository.findById(itemId),
                    UserFavoriteEntity::setCampaign,
                    () -> userFavoriteRepository.findByUserIdAndCampaignId(userId, itemId));
            // Defensive: default case added in case new FavoriteType values are introduced in the future.
        };
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<FavoritesResponse> getFavorites(String sessionToken, String type, int page, int size, String sort) {
        UUID userId = sessionTokenVerifier.uid(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int bucket = -1;
        if (Objects.nonNull(type) && !type.isBlank()) {
            bucket = switch (type.trim().toLowerCase(Locale.ROOT)) {
                case DirectoryAppConstants.FAVORITE_TYPE_STORES -> BUCKET_STORE;
                case DirectoryAppConstants.FAVORITE_TYPE_PRODUCTS -> BUCKET_PRODUCT;
                case DirectoryAppConstants.FAVORITE_TYPE_OFFERS -> BUCKET_OFFER;
                default -> -2;
            };
            if (bucket == -2) {
                return ResponseEntity.badRequest().build();
            }
        }
        if (size < MIN_PAGE_SIZE) {
            return ResponseEntity.badRequest().build();
        }
        Sort order = parseSort(sort);
        if (order == null) {
            return ResponseEntity.badRequest().build();
        }

        // Pagination is done by the database (0-based page). Targets are fetched with the page.
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(size, MAX_PAGE_SIZE), order);
        Page<UserFavoriteEntity> result = userFavoriteRepository.findPageByUser(userId, bucket, pageable);

        List<BusinessTO> stores = new ArrayList<>();
        List<ProductCreateResponse> products = new ArrayList<>();
        List<OfferTO> offers = new ArrayList<>();
        var contactTypeNames = contactTypeProvider.names();
        for (UserFavoriteEntity fav : result.getContent()) {
            if (fav.getBusiness() != null) {
                stores.add(businessMapper.toBusinessTO(fav.getBusiness(), contactTypeNames));
            } else if (fav.getProduct() != null) {
                products.add(productMapper.toProductCreateResponse(fav.getProduct()));
            } else if (fav.getCampaign() != null) {
                offers.add(campaignMapper.toOfferTO(fav.getCampaign()));
            }
        }
        return ResponseEntity.ok(new FavoritesResponse(stores, products, offers));
    }

    /**
     * Parses {@code field[,direction]}. Fields: {@code created_at} (or {@code createdAt}) and {@code name};
     * directions {@code asc|desc}. Returns null when the value is invalid. The default is created_at desc.
     */
    private Sort parseSort(String sort) {
        Sort.Direction direction = Sort.Direction.DESC;
        String field = "created_at";
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", -1);
            if (parts.length > SORT_PARTS_MAX) {
                return null;
            }
            field = parts[0].trim().toLowerCase(Locale.ROOT);
            if (parts.length == SORT_PARTS_MAX) {
                direction = switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
                    case "asc" -> Sort.Direction.ASC;
                    case "desc" -> Sort.Direction.DESC;
                    default -> null;
                };
                if (direction == null) {
                    return null;
                }
            } else {
                direction = Sort.Direction.ASC;
            }
        }
        return switch (field) {
            case "created_at", "createdat" -> Sort.by(direction, "createdAt").and(Sort.by("id"));
            case "name" -> JpaSort.unsafe(direction, "COALESCE(b.name, p.name, c.title)").and(Sort.by("id"));
            default -> null;
        };
    }

    @Override
    public ResponseEntity<FavoriteResponse> updateFavorite(String sessionToken, UUID favoriteId, FavoriteUpdateRequest request) {
        if (Objects.isNull(request)) {
            return ResponseEntity.badRequest().build();
        }

        UUID userId = sessionTokenVerifier.uid(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<UserFavoriteEntity> opt = userFavoriteRepository.findById(favoriteId);
        HttpStatus denied = accessDenied(opt, userId);
        if (denied != null) {
            return ResponseEntity.status(denied).build();
        }
        UserFavoriteEntity entity = opt.orElseThrow();

        // The target is resolved according to the type of the favorite (store, offer or product).
        UUID targetId = request.id();
        Optional<UserFavoriteEntity> sameTarget;
        if (entity.getBusiness() != null) {
            Optional<BusinessEntity> target = businessRepository.findById(targetId);
            if (target.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            entity.setBusiness(target.get());
            sameTarget = userFavoriteRepository.findByUserIdAndBusinessId(userId, targetId);
        } else if (entity.getCampaign() != null) {
            Optional<CampaignEntity> target = campaignRepository.findById(targetId);
            if (target.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            entity.setCampaign(target.get());
            sameTarget = userFavoriteRepository.findByUserIdAndCampaignId(userId, targetId);
        } else {
            Optional<ProductEntity> target = productRepository.findById(targetId);
            if (target.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            entity.setProduct(target.get());
            sameTarget = userFavoriteRepository.findByUserIdAndProductId(userId, targetId);
        }

        // Apply mutable field: 'active' (soft-delete / reactivation)
        if (request.active() != null) {
            entity.setActive(request.active());
            if (request.active()) {
                entity.setDeletedAt(null);
                entity.setDeletedBy(null);
            }
        }

        // Another active favorite of the same user and target would be a duplicate.
        boolean duplicate = sameTarget.isPresent()
                && !favoriteId.equals(sameTarget.get().getId())
                && Boolean.TRUE.equals(sameTarget.get().getActive())
                && Boolean.TRUE.equals(entity.getActive());
        if (duplicate) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        entity.setUpdatedAt(LocalDateTime.now());
        return persist(entity, HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteFavorite(String sessionToken, UUID favoriteId) {
        UUID userId = sessionTokenVerifier.uid(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<UserFavoriteEntity> opt = userFavoriteRepository.findById(favoriteId);
        HttpStatus denied = accessDenied(opt, userId);
        if (denied != null) {
            return ResponseEntity.status(denied).build();
        }
        UserFavoriteEntity entity = opt.orElseThrow();

        // Soft-delete: mark inactive and set audit fields
        entity.setActive(false);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setDeletedBy(userId);
        entity.setUpdatedAt(LocalDateTime.now());
        userFavoriteRepository.save(entity);

        // Audit log
        LOGGER.info("Favorite {} soft-deleted by user {}", favoriteId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Owner-only authorization (admins are not modeled): 404 when the favorite is missing,
     * 403 when it belongs to another user, null when access is granted.
     */
    private HttpStatus accessDenied(Optional<UserFavoriteEntity> favorite, UUID userId) {
        if (favorite.isEmpty()) {
            return HttpStatus.NOT_FOUND;
        }
        UUID owner = favorite.get().getUserFk();
        if (Objects.isNull(owner) || !userId.equals(owner)) {
            return HttpStatus.FORBIDDEN;
        }
        return null;
    }

    private <T> ResponseEntity<FavoriteResponse> processFavorite(UUID userId,
                                                                 Supplier<Optional<T>> entityFinder,
                                                                 BiConsumer<UserFavoriteEntity, T> setter,
                                                                 Supplier<Optional<UserFavoriteEntity>> existingFinder) {
        Optional<T> entityOpt = entityFinder.get();
        if (entityOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var existing = existingFinder.get();
        if (existing.isPresent()) {
            UserFavoriteEntity previous = existing.get();
            if (Boolean.TRUE.equals(previous.getActive())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            // Soft-deleted favorite: reactivate it instead of failing with 409.
            previous.setActive(true);
            previous.setDeletedAt(null);
            previous.setDeletedBy(null);
            previous.setUpdatedAt(LocalDateTime.now());
            return persist(previous, HttpStatus.CREATED);
        }
        UserFavoriteEntity fav = new UserFavoriteEntity(userId);
        setter.accept(fav, entityOpt.get());
        fav.setCreatedAt(LocalDateTime.now());
        fav.setUpdatedAt(LocalDateTime.now());
        fav.setActive(true);
        return persist(fav, HttpStatus.CREATED);
    }

    /**
     * Saves and flushes so a unique-index violation (concurrent duplicate) surfaces here and is
     * answered with 409. Callers are intentionally not transactional: a constraint violation
     * would otherwise mark the surrounding transaction rollback-only.
     */
    private ResponseEntity<FavoriteResponse> persist(UserFavoriteEntity entity, HttpStatus okStatus) {
        try {
            UserFavoriteEntity saved = userFavoriteRepository.saveAndFlush(entity);
            return ResponseEntity.status(okStatus).body(favoriteMapper.toResponse(saved));
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("Favorite rejected by a database constraint (duplicate): {}", ex.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
