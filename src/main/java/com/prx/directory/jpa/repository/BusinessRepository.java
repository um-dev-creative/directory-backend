package com.prx.directory.jpa.repository;

import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.UserEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for BusinessEntity.
 * This interface extends JpaRepository to provide CRUD operations for BusinessEntity.
 * The primary key type of BusinessEntity is UUID.
 */
public interface BusinessRepository extends JpaRepository<BusinessEntity, UUID> {

    Page<BusinessEntity> findByUserEntityFk(@NotNull UserEntity userEntityFk, Pageable pageable);

    Optional<BusinessEntity> findByName(@NotNull String name);

    @Query("SELECT count(bu.id) FROM BusinessEntity bu WHERE bu.userEntityFk.id = :userId")
    int findByUserId(@Param("userId") UUID userId);
}
