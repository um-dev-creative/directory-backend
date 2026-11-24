package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.FavoriteListEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// Repository of the favorite lists; every lookup is scoped to the owner.
public interface FavoriteListRepository extends JpaRepository<FavoriteListEntity, UUID> {

    List<FavoriteListEntity> findByUserFkOrderByCreatedAtDesc(UUID userFk);

    Optional<FavoriteListEntity> findByIdAndUserFk(UUID id, UUID userFk);

    long countByUserFk(UUID userFk);

    boolean existsByUserFkAndNameIgnoreCase(UUID userFk, String name);

    boolean existsByUserFkAndNameIgnoreCaseAndIdNot(UUID userFk, String name, UUID id);
}
