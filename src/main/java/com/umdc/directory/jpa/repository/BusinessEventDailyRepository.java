package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessEventDailyEntity;
import com.umdc.directory.jpa.entity.BusinessEventDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/// Repository of the daily click event counters.
public interface BusinessEventDailyRepository extends JpaRepository<BusinessEventDailyEntity, BusinessEventDailyId> {

    /// Name of the `:businessId` query parameter.
    String BUSINESS_ID = "businessId";

    /// Adds one to the counter of the key with a single `UPDATE` (atomic: concurrent events never overwrite each other).
    ///
    /// @return the number of rows updated: 0 when the key has no row yet
    @Modifying
    @Transactional
    @Query("UPDATE BusinessEventDailyEntity e SET e.eventCount = e.eventCount + 1 WHERE e.id.businessId = :businessId "
            + "AND e.id.campaignId = :campaignId AND e.id.eventType = :eventType AND e.id.eventDate = :eventDate")
    int increment(@Param(BUSINESS_ID) UUID businessId, @Param("campaignId") UUID campaignId,
                  @Param("eventType") String eventType, @Param("eventDate") LocalDate eventDate);

    /// The rows of the business between two days, both included, oldest first. Keys without events have no row.
    @Query("SELECT e FROM BusinessEventDailyEntity e WHERE e.id.businessId = :businessId AND e.id.eventDate BETWEEN :from AND :to "
            + "ORDER BY e.id.eventDate ASC, e.id.campaignId ASC, e.id.eventType ASC")
    List<BusinessEventDailyEntity> findSeries(@Param(BUSINESS_ID) UUID businessId, @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);
}
