package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessViewDailyEntity;
import com.umdc.directory.jpa.entity.BusinessViewDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/// Repository of the daily view counters.
public interface BusinessViewDailyRepository extends JpaRepository<BusinessViewDailyEntity, BusinessViewDailyId> {

    /// Name of the `:businessId` query parameter.
    String BUSINESS_ID = "businessId";

    /// Adds one view to the row of the day with a single `UPDATE` (atomic: concurrent views never overwrite each other).
    ///
    /// @return the number of rows updated: 0 when the day has no row yet
    @Modifying
    @Transactional
    @Query("UPDATE BusinessViewDailyEntity v SET v.views = v.views + 1 WHERE v.id.businessId = :businessId AND v.id.viewDate = :viewDate")
    int increment(@Param(BUSINESS_ID) UUID businessId, @Param("viewDate") LocalDate viewDate);

    /// The rows of the business between two days, both included, oldest first. Days without views have no row.
    @Query("SELECT v FROM BusinessViewDailyEntity v WHERE v.id.businessId = :businessId AND v.id.viewDate BETWEEN :from AND :to "
            + "ORDER BY v.id.viewDate ASC")
    List<BusinessViewDailyEntity> findSeries(@Param(BUSINESS_ID) UUID businessId, @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);
}
