package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// Repository of the business locations. Every query that returns locations fetches the city, so the mapping needs no open session.
public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {

    /// Name of the `:businessId` query parameter.
    String BUSINESS_ID = "businessId";

    @Query("SELECT l FROM LocationEntity l JOIN FETCH l.city WHERE l.business.id = :businessId ORDER BY l.main DESC, l.createdDate ASC")
    List<LocationEntity> findByBusinessId(@Param(BUSINESS_ID) UUID businessId);

    @Query("SELECT l FROM LocationEntity l JOIN FETCH l.city WHERE l.business.id IN :businessIds ORDER BY l.main DESC, l.createdDate ASC")
    List<LocationEntity> findByBusinessIdIn(@Param("businessIds") Collection<UUID> businessIds);

    @Query("SELECT l FROM LocationEntity l JOIN FETCH l.city WHERE l.id = :id AND l.business.id = :businessId")
    Optional<LocationEntity> findByIdAndBusinessId(@Param("id") UUID id, @Param(BUSINESS_ID) UUID businessId);

    @Query("SELECT l FROM LocationEntity l JOIN FETCH l.city WHERE l.business.id = :businessId AND l.main = TRUE")
    Optional<LocationEntity> findMainByBusinessId(@Param(BUSINESS_ID) UUID businessId);

    @Query("SELECT count(l.id) FROM LocationEntity l WHERE l.business.id = :businessId")
    long countByBusinessId(@Param(BUSINESS_ID) UUID businessId);
}
