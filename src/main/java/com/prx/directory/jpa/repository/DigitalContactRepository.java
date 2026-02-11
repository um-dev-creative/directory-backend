package com.prx.directory.jpa.repository;

import com.prx.directory.jpa.entity.DigitalContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for DigitalContactEntity.
 * This interface extends JpaRepository to provide CRUD operations for DigitalContactEntity.
 * The primary key type of DigitalContactEntity is UUID.
 */
public interface DigitalContactRepository extends JpaRepository<DigitalContactEntity, UUID> {


    @Query("SELECT b FROM DigitalContactEntity AS b WHERE b.business.id = :businessId ")
    List<DigitalContactEntity> findByBusinessId(@Param("businessId") UUID businessId);
}
