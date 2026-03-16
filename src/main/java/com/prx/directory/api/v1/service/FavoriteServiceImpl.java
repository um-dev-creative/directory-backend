package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.*;
import com.prx.directory.constant.DirectoryAppConstants;
import com.prx.directory.constant.FavoriteType;
import com.prx.directory.jpa.entity.ProductEntity;
import com.prx.directory.jpa.entity.UserEntity;
import com.prx.directory.jpa.entity.UserFavoriteEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.ProductRepository;
import com.prx.directory.jpa.repository.UserFavoriteRepository;
import com.prx.directory.mapper.BusinessMapper;
import com.prx.directory.mapper.CampaignMapper;
import com.prx.directory.mapper.FavoriteMapper;
import com.prx.directory.mapper.ProductMapper;
import com.prx.directory.util.JwtUtil;
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

    private final UserFavoriteRepository userFavoriteRepository;
    private final BusinessRepository businessRepository;
    private final ProductRepository productRepository;
    private final CampaignRepository campaignRepository;
    private final FavoriteMapper favoriteMapper;
    private final BusinessMapper businessMapper;
    private final ProductMapper productMapper;
    private final CampaignMapper campaignMapper;

    public FavoriteServiceImpl(UserFavoriteRepository userFavoriteRepository,
                               BusinessRepository businessRepository,
                               ProductRepository productRepository,
                               CampaignRepository campaignRepository,
                               FavoriteMapper favoriteMapper,
                               BusinessMapper businessMapper,
                               ProductMapper productMapper,
                               CampaignMapper campaignMapper) {
        this.userFavoriteRepository = userFavoriteRepository;
        this.businessRepository = businessRepository;
        this.productRepository = productRepository;
        this.campaignRepository = campaignRepository;
        this.favoriteMapper = favoriteMapper;
        this.businessMapper = businessMapper;
        this.productMapper = productMapper;
        this.campaignMapper = campaignMapper;
    }

    @Override
    @Transactional
    public ResponseEntity<FavoriteResponse> createFavorite(String sessionToken, FavoriteCreateRequest request) {
        // validate request
        if (Objects.isNull(request)) {
            return ResponseEntity.badRequest().build();
        }

        UUID itemId = request.itemId();
        FavoriteType type = request.type();

        UUID userId = JwtUtil.getUidFromToken(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserEntity user = new UserEntity();
        user.setId(userId);

        return switch (type) {
            case STORE -> processFavorite(user,
                    () -> businessRepository.findById(itemId),
                    UserFavoriteEntity::setBusiness,
                    () -> userFavoriteRepository.findByUserIdAndBusinessId(user.getId(), itemId));
            case PRODUCT -> processFavorite(user,
                    () -> productRepository.findById(itemId),
                    UserFavoriteEntity::setProduct,
                    () -> userFavoriteRepository.findByUserIdAndProductId(user.getId(), itemId));
            case OFFER -> processFavorite(user,
                    () -> campaignRepository.findById(itemId),
                    UserFavoriteEntity::setCampaign,
                    () -> userFavoriteRepository.findByUserIdAndCampaignId(user.getId(), itemId));
            // Defensive: default case added in case new FavoriteType values are introduced in the future.
        };
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<FavoritesResponse> getFavorites(String sessionToken, String type, int page, int size, String sort) {
        // Return 501 Not Implemented if sort parameter is provided
        if (sort != null && !sort.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }

        UUID userId = JwtUtil.getUidFromToken(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // findByUserId uses LEFT JOIN FETCH for business/product/campaign, so no N+1 lazy loads.
        List<UserFavoriteEntity> favsList = userFavoriteRepository.findByUserId(userId);

        // Single-pass partition: benchmark showed 3 separate stream traversals are 10–14×
        // slower and allocate 2.3–5.5× more bytes than one enhanced-for loop (see
        // FavoriteStreamBenchmark). Pre-sized lists avoid ArrayList resize copies.
        int bucket = favsList.size() / 3 + 1;
        List<BusinessTO> stores   = new ArrayList<>(bucket);
        List<ProductCreateResponse> products = new ArrayList<>(bucket);
        List<OfferTO> offers      = new ArrayList<>(bucket);

        for (UserFavoriteEntity fav : favsList) {
            if (fav.getBusiness() != null) {
                stores.add(businessMapper.toBusinessTO(fav.getBusiness()));
            } else if (fav.getProduct() != null) {
                products.add(productMapper.toProductCreateResponse(fav.getProduct()));
            } else if (fav.getCampaign() != null) {
                offers.add(campaignMapper.toOfferTO(fav.getCampaign()));
            }
        }

        // Apply type filter with pagination
        if (Objects.nonNull(type) && !type.isBlank()) {
            String t = type.trim().toLowerCase(Locale.ROOT);
            return switch (t) {
                case DirectoryAppConstants.FAVORITE_TYPE_STORES -> {
                    List<BusinessTO> paginatedStores = paginateList(stores, page, size);
                    yield ResponseEntity.ok(new FavoritesResponse(paginatedStores, List.of(), List.of()));
                }
                case DirectoryAppConstants.FAVORITE_TYPE_PRODUCTS -> {
                    List<ProductCreateResponse> paginatedProducts = paginateList(products, page, size);
                    yield ResponseEntity.ok(new FavoritesResponse(List.of(), paginatedProducts, List.of()));
                }
                case DirectoryAppConstants.FAVORITE_TYPE_OFFERS -> {
                    List<OfferTO> paginatedOffers = paginateList(offers, page, size);
                    yield ResponseEntity.ok(new FavoritesResponse(List.of(), List.of(), paginatedOffers));
                }
                default -> ResponseEntity.badRequest().build();
            };
        }

        // Pagination: When no type filter is specified, paginate across the combined list
        // to ensure the total number of items returned respects the size parameter.
        // Create a combined list with ordering: stores, then products, then offers
        record FavoriteItem(Object item, String itemType) {}

        List<FavoriteItem> combined = new ArrayList<>(stores.size() + products.size() + offers.size());
        stores.forEach(s -> combined.add(new FavoriteItem(s, DirectoryAppConstants.FAVORITE_TYPE_STORES)));
        products.forEach(p -> combined.add(new FavoriteItem(p, DirectoryAppConstants.FAVORITE_TYPE_PRODUCTS)));
        offers.forEach(o -> combined.add(new FavoriteItem(o, DirectoryAppConstants.FAVORITE_TYPE_OFFERS)));

        // Apply pagination to the combined list
        int from = Math.max(0, page * size);
        int to = Math.min(combined.size(), from + size);
        List<FavoriteItem> paginatedItems = from < to ? combined.subList(from, to) : List.of();

        // Separate back into type-specific lists
        List<BusinessTO> storesPage = new ArrayList<>();
        List<ProductCreateResponse> productsPage = new ArrayList<>();
        List<OfferTO> offersPage = new ArrayList<>();

        for (FavoriteItem item : paginatedItems) {
            switch (item.itemType()) {
                case DirectoryAppConstants.FAVORITE_TYPE_STORES  -> storesPage.add((BusinessTO) item.item());
                case DirectoryAppConstants.FAVORITE_TYPE_PRODUCTS -> productsPage.add((ProductCreateResponse) item.item());
                case DirectoryAppConstants.FAVORITE_TYPE_OFFERS -> offersPage.add((OfferTO) item.item());
                default -> ResponseEntity.notFound().build();
            }
        }

        FavoritesResponse response = new FavoritesResponse(storesPage, productsPage, offersPage);
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<FavoriteResponse> updateFavorite(String sessionToken, UUID favoriteId, FavoriteUpdateRequest request) {
        if (Objects.isNull(request)) {
            return ResponseEntity.badRequest().build();
        }

        UUID userId = JwtUtil.getUidFromToken(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<UserFavoriteEntity> opt = userFavoriteRepository.findById(favoriteId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Optional<ProductEntity> productOpt = productRepository.findById(request.id());

        if(productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserFavoriteEntity entity = opt.get();
        // Authorization: only the owner may update (admins not modeled here)
        if (Objects.isNull(entity.getUser()) || !userId.equals(entity.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        entity.setProduct(productOpt.get());

        // Apply mutable field: only 'active' can be updated (for soft-delete)
        if (request.active() != null) {
            entity.setActive(request.active());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        UserFavoriteEntity saved = userFavoriteRepository.save(entity);
        return ResponseEntity.ok(favoriteMapper.toResponse(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteFavorite(String sessionToken, UUID favoriteId) {
        UUID userId = JwtUtil.getUidFromToken(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<UserFavoriteEntity> opt = userFavoriteRepository.findById(favoriteId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserFavoriteEntity entity = opt.get();
        // Authorization: allow owner only for now. Admin role not modeled in JWT parsing.
        // When admin support is available extend this check accordingly.
        if (Objects.isNull(entity.getUser()) || !userId.equals(entity.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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

    private <T> List<T> paginateList(List<T> list, int page, int size) {
        int from = Math.max(0, page * size);
        int to = Math.min(list.size(), from + size);
        return from < to ? list.subList(from, to) : List.of();
    }

    private <T> ResponseEntity<FavoriteResponse> processFavorite(UserEntity user,
                                                                 Supplier<Optional<T>> entityFinder,
                                                                 BiConsumer<UserFavoriteEntity, T> setter,
                                                                 Supplier<Optional<UserFavoriteEntity>> existingFinder) {
        Optional<T> entityOpt = entityFinder.get();
        if (entityOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var existing = existingFinder.get();
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        UserFavoriteEntity fav = new UserFavoriteEntity(user);
        setter.accept(fav, entityOpt.get());
        fav.setCreatedAt(LocalDateTime.now());
        fav.setUpdatedAt(LocalDateTime.now());
        fav.setActive(true);
        UserFavoriteEntity saved = userFavoriteRepository.save(fav);
        return ResponseEntity.status(HttpStatus.CREATED).body(favoriteMapper.toResponse(saved));
    }
}
