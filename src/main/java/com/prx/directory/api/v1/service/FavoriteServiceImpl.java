package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.BusinessTO;
import com.prx.directory.api.v1.to.FavoriteCreateRequest;
import com.prx.directory.api.v1.to.FavoriteResponse;
import com.prx.directory.api.v1.to.FavoritesResponse;
import com.prx.directory.api.v1.to.OfferTO;
import com.prx.directory.api.v1.to.ProductCreateResponse;
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
            default -> ResponseEntity.badRequest().build();
        };
    }

    @Override
    public ResponseEntity<FavoritesResponse> getFavorites(String sessionToken, String type, int page, int size, String sort) {
        // Sorting not yet implemented
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
                case "stores" -> {
                    int from = Math.max(0, page * size);
                    int to = Math.min(stores.size(), from + size);
                    List<BusinessTO> storesPage = from < to ? stores.subList(from, to) : List.of();
                    yield ResponseEntity.ok(new FavoritesResponse(storesPage, List.of(), List.of()));
                }
                case "products" -> {
                    int from = Math.max(0, page * size);
                    int to = Math.min(products.size(), from + size);
                    List<ProductCreateResponse> productsPage = from < to ? products.subList(from, to) : List.of();
                    yield ResponseEntity.ok(new FavoritesResponse(List.of(), productsPage, List.of()));
                }
                case "offers" -> {
                    int from = Math.max(0, page * size);
                    int to = Math.min(offers.size(), from + size);
                    List<OfferTO> offersPage = from < to ? offers.subList(from, to) : List.of();
                    yield ResponseEntity.ok(new FavoritesResponse(List.of(), List.of(), offersPage));
                }
                default -> ResponseEntity.badRequest().build();
            };
        }

        // When no type filter: return all grouped favorites without pagination
        // Pagination only applies when a specific type filter is provided
        FavoritesResponse response = new FavoritesResponse(stores, products, offers);
        return ResponseEntity.ok(response);
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
