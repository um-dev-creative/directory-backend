package com.prx.directory.jpa.repository;

import com.prx.directory.jpa.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repository interface for accessing CampaignEntity data.
 * This interface extends JpaRepository to provide CRUD operations for CampaignEntity.
 */
public interface CampaignRepository extends JpaRepository<CampaignEntity, UUID>, JpaSpecificationExecutor<CampaignEntity> {
}
