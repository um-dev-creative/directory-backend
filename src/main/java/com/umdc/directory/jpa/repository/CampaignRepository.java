package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository interface for accessing CampaignEntity data.
 * This interface extends JpaRepository to provide CRUD operations for CampaignEntity.
 */
public interface CampaignRepository extends JpaRepository<CampaignEntity, UUID>, JpaSpecificationExecutor<CampaignEntity> {

    /// Whether the campaign exists and belongs to the business.
    boolean existsByIdAndBusinessFk_Id(UUID id, UUID businessId);

    long countByActiveTrue();

    long countByActiveFalse();

    long countByEndDateBefore(LocalDateTime ts);
}
