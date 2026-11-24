package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.FavoriteListItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// Repository of the items of the favorite lists. Counts and pages only include active favorites.
public interface FavoriteListItemRepository extends JpaRepository<FavoriteListItemEntity, UUID> {

    String LIST_ID = "listId";
    String LIST_IDS = "listIds";
    String FAVORITE_ID = "favoriteId";

    String Q_PAGE = "SELECT i FROM FavoriteListItemEntity i JOIN FETCH i.favorite f "
            + "LEFT JOIN FETCH f.business LEFT JOIN FETCH f.product "
            + "WHERE i.listFk = :" + LIST_ID + " AND f.active = true";
    String Q_PAGE_COUNT = "SELECT COUNT(i) FROM FavoriteListItemEntity i WHERE i.listFk = :" + LIST_ID
            + " AND i.favorite.active = true";
    String Q_COUNTS = "SELECT i.listFk, COUNT(i) FROM FavoriteListItemEntity i WHERE i.listFk IN :" + LIST_IDS
            + " AND i.favorite.active = true GROUP BY i.listFk";

    /// One page of the active favorites of a list, with their targets fetched.
    @Query(value = Q_PAGE, countQuery = Q_PAGE_COUNT)
    Page<FavoriteListItemEntity> findPageByList(@Param(LIST_ID) UUID listId, Pageable pageable);

    /// Rows of `[list id, number of active favorites]`; lists without active items are absent.
    @Query(Q_COUNTS)
    List<Object[]> countActiveByLists(@Param(LIST_IDS) Collection<UUID> listIds);

    @Query(Q_PAGE_COUNT)
    long countActiveByList(@Param(LIST_ID) UUID listId);

    @Query("SELECT i FROM FavoriteListItemEntity i WHERE i.listFk = :" + LIST_ID + " AND i.favorite.id = :" + FAVORITE_ID)
    Optional<FavoriteListItemEntity> findByListAndFavorite(@Param(LIST_ID) UUID listId, @Param(FAVORITE_ID) UUID favoriteId);

    @Modifying
    @Query("DELETE FROM FavoriteListItemEntity i WHERE i.listFk = :" + LIST_ID)
    void deleteByList(@Param(LIST_ID) UUID listId);
}
