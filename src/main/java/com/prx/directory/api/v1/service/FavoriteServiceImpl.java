package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.*;
import com.prx.directory.constant.DirectoryAppConstants;
import com.prx.directory.constant.FavoriteType;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Service
public class FavoriteServiceImpl implements FavoriteService {

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
    public ResponseEntity<FavoritesResponse> getFavorites(String sessionToken, String type, int page, int size, String sort) {
        // Return 501 Not Implemented if sort parameter is provided
        if (sort != null && !sort.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }

        UUID userId = JwtUtil.getUidFromToken(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<UserFavoriteEntity> favsList = userFavoriteRepository.findByUserId(userId);

        // filter by type if provided and map to DTO lists
        List<BusinessTO> stores = favsList.stream()
                .map(UserFavoriteEntity::getBusiness)
                .filter(Objects::nonNull)
                .map(businessMapper::toBusinessTO)
                .toList();

        List<ProductCreateResponse> products = favsList.stream()
                .map(UserFavoriteEntity::getProduct)
                .filter(Objects::nonNull)
                .map(productMapper::toProductCreateResponse)
                .toList();

        List<OfferTO> offers = favsList.stream()
                .map(UserFavoriteEntity::getCampaign)
                .filter(Objects::nonNull)
                .map(campaignMapper::toOfferTO)
                .toList();

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

        List<FavoriteItem> combined = new ArrayList<>();
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
    public ResponseEntity<FavoriteResponse> updateFavorite(String sessionToken, FavoriteUpdateRequest request) {
        if (Objects.isNull(request) || Objects.isNull(request.id())) {
            return ResponseEntity.badRequest().build();
        }

        UUID userId = JwtUtil.getUidFromToken(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<UserFavoriteEntity> opt = userFavoriteRepository.findById(request.id());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserFavoriteEntity entity = opt.get();
        // Authorization: only the owner may update (admins not modeled here)
        if (Objects.isNull(entity.getUser()) || !userId.equals(entity.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Apply mutable field: only 'active' can be updated (for soft-delete)
        if (request.active() != null) {
            entity.setActive(request.active());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        UserFavoriteEntity saved = userFavoriteRepository.save(entity);
        return ResponseEntity.ok(favoriteMapper.toResponse(saved));
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
