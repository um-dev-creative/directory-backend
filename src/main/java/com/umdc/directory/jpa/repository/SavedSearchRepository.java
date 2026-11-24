package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.SavedSearchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// Repository of the saved searches; every lookup is scoped to the owner.
public interface SavedSearchRepository extends JpaRepository<SavedSearchEntity, UUID> {

    List<SavedSearchEntity> findByUserFkOrderByCreatedAtDesc(UUID userFk);

    Optional<SavedSearchEntity> findByIdAndUserFk(UUID id, UUID userFk);

    long countByUserFk(UUID userFk);

    boolean existsByUserFkAndNameIgnoreCase(UUID userFk, String name);

    boolean existsByUserFkAndNameIgnoreCaseAndIdNot(UUID userFk, String name, UUID id);
}
