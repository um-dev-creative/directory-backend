package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository interface for BusinessEntity.
 * This interface extends JpaRepository to provide CRUD operations for BusinessEntity.
 * The primary key type of BusinessEntity is UUID.
 */
public interface BusinessRepository extends JpaRepository<BusinessEntity, UUID>, JpaSpecificationExecutor<BusinessEntity> {

    @Query("SELECT bu FROM BusinessEntity bu WHERE bu.userFk = :userId")
    Set<BusinessEntity> findByUserEntityFk(@NotNull @Param("userId") UUID userId);

    Optional<BusinessEntity> findByName(@NotNull String name);

    @Query("SELECT count(bu.id) FROM BusinessEntity bu WHERE bu.userFk = :userId")
    int countByUserId(@Param("userId") UUID userId);

    @Query("SELECT b.id FROM BusinessEntity b LEFT JOIN b.digitalContacts dc ON dc.business.id = b.id  WHERE b.userFk = :userId")
    Set<UUID> findIdCollectionByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM BusinessEntity b LEFT JOIN FETCH b.digitalContacts dc WHERE b.id = :businessId")
    Optional<BusinessEntity> findBusinessWithDigitalContactsById(@Param("businessId") UUID businessId);

    /// Id of the user who owns the business, without loading the business; empty when it does not exist.
    @Query("SELECT b.userFk FROM BusinessEntity b WHERE b.id = :businessId")
    Optional<UUID> findOwnerId(@Param("businessId") UUID businessId);

    /// The search (`BusinessSpecifications`) reads the category name of every row, so it is fetched with the page.
    @Override
    @EntityGraph(attributePaths = {"categoryFk"})
    Page<BusinessEntity> findAll(Specification<BusinessEntity> spec, Pageable pageable);

}
