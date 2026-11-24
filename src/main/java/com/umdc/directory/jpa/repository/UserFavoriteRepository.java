package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.UserFavoriteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    String Q_USER_PREFIX = "SELECT uf FROM UserFavoriteEntity uf WHERE uf.userFk = :";

    // Full JPQL query constants (reuse the common prefix where applicable)
    String Q_FIND_BY_USER = "SELECT uf FROM UserFavoriteEntity uf "
            + "LEFT JOIN FETCH uf.business "
            + "LEFT JOIN FETCH uf.product "
            + "LEFT JOIN FETCH uf.campaign "
            + "WHERE uf.userFk = :" + USER_ID + " AND uf.active = true";
    String BUCKET = "bucket";
    /** Bucket of a favorite: 0 = store, 1 = product, 2 = offer (keeps the stores, products, offers order). */
    String BUCKET_EXPR = "CASE WHEN uf.business IS NOT NULL THEN 0 WHEN uf.product IS NOT NULL THEN 1 ELSE 2 END";
    String Q_PAGE_WHERE = " WHERE uf.userFk = :" + USER_ID + " AND uf.active = true"
            + " AND (:" + BUCKET + " < 0 OR " + BUCKET_EXPR + " = :" + BUCKET + ")";
    String Q_PAGE_BY_USER = "SELECT uf FROM UserFavoriteEntity uf "
            + "LEFT JOIN FETCH uf.business b "
            + "LEFT JOIN FETCH uf.product p "
            + "LEFT JOIN FETCH uf.campaign c"
            + Q_PAGE_WHERE + " ORDER BY " + BUCKET_EXPR + " ASC";
    String Q_PAGE_COUNT = "SELECT COUNT(uf) FROM UserFavoriteEntity uf" + Q_PAGE_WHERE;
    String Q_FIND_BY_BUSINESS = "SELECT uf FROM UserFavoriteEntity uf WHERE uf.business.id = :" + BUSINESS_ID;
    String Q_FIND_BY_USER_AND_BUSINESS = Q_USER_PREFIX + USER_ID + " AND uf.business.id = :" + BUSINESS_ID;
    String Q_COUNT_ACTIVE_BY_USER = "SELECT COUNT(uf) FROM UserFavoriteEntity uf WHERE uf.userFk = :" + USER_ID + " AND uf.active = true";
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

    /**
     * Database-paginated active favorites of a user, ordered by type bucket (stores, products,
     * offers) and then by the sort carried in the {@link Pageable}. The sort may reference the
     * aliases {@code uf}, {@code b}, {@code p} and {@code c}.
     *
     * @param userId   the user's UUID
     * @param bucket   0 stores, 1 products, 2 offers, negative for all types
     * @param pageable page request (0-based) with optional sort
     * @return one page of favorites with their targets fetched
     */
    @Query(value = Q_PAGE_BY_USER, countQuery = Q_PAGE_COUNT)
    Page<UserFavoriteEntity> findPageByUser(@Param(USER_ID) UUID userId, @Param(BUCKET) int bucket, Pageable pageable);
}
