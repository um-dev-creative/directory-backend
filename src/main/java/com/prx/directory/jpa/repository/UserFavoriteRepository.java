package com.prx.directory.jpa.repository;

import com.prx.directory.jpa.entity.UserFavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserFavoriteEntity.
 * This interface extends JpaRepository to provide CRUD operations for UserFavoriteEntity.
 */
public interface UserFavoriteRepository extends JpaRepository<UserFavoriteEntity, UUID> {

    String USER_ID = "userId";
    String BUSINESS_ID = "businessId";
    String PRODUCT_ID = "productId";
    String CAMPAIGN_ID = "campaignId";

    // Common JPQL fragments to avoid duplicated literals
    String Q_USER_PREFIX = "SELECT uf FROM UserFavoriteEntity uf WHERE uf.user.id = :";

    // Full JPQL query constants (reuse the common prefix where applicable)
    String Q_FIND_BY_USER = Q_USER_PREFIX + USER_ID;
    String Q_FIND_BY_BUSINESS = "SELECT uf FROM UserFavoriteEntity uf WHERE uf.business.id = :" + BUSINESS_ID;
    String Q_FIND_BY_USER_AND_BUSINESS = Q_USER_PREFIX + USER_ID + " AND uf.business.id = :" + BUSINESS_ID;
    String Q_COUNT_ACTIVE_BY_USER = "SELECT COUNT(uf) FROM UserFavoriteEntity uf WHERE uf.user.id = :" + USER_ID + " AND uf.active = true";
    String Q_FIND_BY_USER_AND_PRODUCT = Q_USER_PREFIX + USER_ID + " AND uf.product.id = :" + PRODUCT_ID;
    String Q_FIND_BY_USER_AND_CAMPAIGN = Q_USER_PREFIX + USER_ID + " AND uf.campaign.id = :" + CAMPAIGN_ID;

    /**
     * Find favorites by user id.
     *
     * @param userId the user's UUID
     * @return list of UserFavoriteEntity
     */
    @Query(Q_FIND_BY_USER)
    List<UserFavoriteEntity> findByUserId(@Param(USER_ID) UUID userId);

    /**
     * Find favorites by business id.
     *
     * @param businessId the business UUID
     * @return list of UserFavoriteEntity
     */
    @Query(Q_FIND_BY_BUSINESS)
    List<UserFavoriteEntity> findByBusinessId(@Param(BUSINESS_ID) UUID businessId);

    /**
     * Find a favorite by user and business.
     *
     * @param userId     the user's UUID
     * @param businessId the business UUID
     * @return optional UserFavoriteEntity
     */
    @Query(Q_FIND_BY_USER_AND_BUSINESS)
    Optional<UserFavoriteEntity> findByUserIdAndBusinessId(@Param(USER_ID) UUID userId, @Param(BUSINESS_ID) UUID businessId);

    /**
     * Count active favorites for a user.
     *
     * @param userId the user's UUID
     * @return count
     */
    @Query(Q_COUNT_ACTIVE_BY_USER)
    long countActiveByUserId(@Param(USER_ID) UUID userId);

    /**
     * Find a favorite by user and product.
     *
     * @param userId    the user's UUID
     * @param productId the product UUID
     * @return optional UserFavoriteEntity
     */
    @Query(Q_FIND_BY_USER_AND_PRODUCT)
    Optional<UserFavoriteEntity> findByUserIdAndProductId(@Param(USER_ID) UUID userId, @Param(PRODUCT_ID) UUID productId);

    /**
     * Find a favorite by user and campaign.
     *
     * @param userId     the user's UUID
     * @param campaignId the campaign UUID
     * @return optional UserFavoriteEntity
     */
    @Query(Q_FIND_BY_USER_AND_CAMPAIGN)
    Optional<UserFavoriteEntity> findByUserIdAndCampaignId(@Param(USER_ID) UUID userId, @Param(CAMPAIGN_ID) UUID campaignId);
}
