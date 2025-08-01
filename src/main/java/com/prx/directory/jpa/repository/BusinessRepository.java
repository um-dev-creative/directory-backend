package com.prx.directory.jpa.repository;

import com.prx.directory.jpa.entity.BusinessEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
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
public interface BusinessRepository extends JpaRepository<BusinessEntity, UUID> {

    @Query("SELECT bu FROM BusinessEntity bu WHERE bu.userEntityFk.id = :userId")
    Set<BusinessEntity> findByUserEntityFk(@NotNull @Param("userId") UUID userId);

    Optional<BusinessEntity> findByName(@NotNull String name);

    @Query("SELECT count(bu.id) FROM BusinessEntity bu WHERE bu.userEntityFk.id = :userId")
    int countByUserId(@Param("userId") UUID userId);

    @Query("SELECT b.id FROM BusinessEntity b LEFT JOIN b.digitalContacts dc ON dc.business.id = b.id  WHERE b.userEntityFk.id = :userId")
    Set<UUID> findIdCollectionByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM BusinessEntity b LEFT JOIN FETCH b.digitalContacts dc LEFT JOIN FETCH dc.contactTypeEntity WHERE b.id = :businessId")
    Optional<BusinessEntity> findBusinessWithDigitalContactsById(@Param("businessId") UUID businessId);

}
