package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.FavoriteCreateRequest;
import com.prx.directory.api.v1.to.FavoriteResponse;
import com.prx.directory.constant.FavoriteType;
import com.prx.directory.jpa.entity.UserEntity;
import com.prx.directory.jpa.entity.UserFavoriteEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.ProductRepository;
import com.prx.directory.jpa.repository.UserFavoriteRepository;
import com.prx.directory.mapper.FavoriteMapper;
import com.prx.directory.util.JwtUtil;
import com.prx.security.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteRepository userFavoriteRepository;
    private final BusinessRepository businessRepository;
    private final ProductRepository productRepository;
    private final CampaignRepository campaignRepository;
    private final FavoriteMapper favoriteMapper;

    public FavoriteServiceImpl(UserFavoriteRepository userFavoriteRepository,
                               BusinessRepository businessRepository,
                               ProductRepository productRepository,
                               CampaignRepository campaignRepository,
                               FavoriteMapper favoriteMapper) {
        this.userFavoriteRepository = userFavoriteRepository;
        this.businessRepository = businessRepository;
        this.productRepository = productRepository;
        this.campaignRepository = campaignRepository;
        this.favoriteMapper = favoriteMapper;
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
                    () -> businessRepository.existsById(itemId),
                    () -> businessRepository.findById(itemId),
                    UserFavoriteEntity::setBusiness,
                    () -> userFavoriteRepository.findByUserIdAndBusinessId(user.getId(), itemId));
            case PRODUCT -> processFavorite(user,
                    () -> productRepository.existsById(itemId),
                    () -> productRepository.findById(itemId),
                    UserFavoriteEntity::setProduct,
                    () -> userFavoriteRepository.findByUserIdAndProductId(user.getId(), itemId));
            case OFFER -> processFavorite(user,
                    () -> campaignRepository.existsById(itemId),
                    () -> campaignRepository.findById(itemId),
                    UserFavoriteEntity::setCampaign,
                                        () -> userFavoriteRepository.findByUserIdAndCampaignId(user.getId(), itemId));
            // Defensive: default case added in case new FavoriteType values are introduced in the future.
            default -> ResponseEntity.badRequest().build();
        };
    }

    private <T> ResponseEntity<FavoriteResponse> processFavorite(UserEntity user,
                                                                 BooleanSupplier existsChecker,
                                                                 Supplier<Optional<T>> entityFinder,
                                                                 BiConsumer<UserFavoriteEntity, T> setter,
                                                                 Supplier<Optional<UserFavoriteEntity>> existingFinder) {
        if (!existsChecker.getAsBoolean()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var existing = existingFinder.get();
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        UserFavoriteEntity fav = new UserFavoriteEntity(user);
        T ent = entityFinder.get().orElse(null);
        setter.accept(fav, ent);
        fav.setCreatedAt(LocalDateTime.now());
        fav.setUpdatedAt(LocalDateTime.now());
        fav.setActive(true);
        UserFavoriteEntity saved = userFavoriteRepository.save(fav);
        return ResponseEntity.status(HttpStatus.CREATED).body(favoriteMapper.toResponse(saved));
    }
}
