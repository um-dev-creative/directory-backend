package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.LocationOpeningHoursEntity;
import com.umdc.directory.jpa.entity.TimezoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// Repository of the opening hours of the locations.
public interface LocationOpeningHoursRepository extends JpaRepository<LocationOpeningHoursEntity, UUID> {

    /// The slots of a location in week order.
    @Query("SELECT h FROM LocationOpeningHoursEntity h WHERE h.locationFk = :locationId ORDER BY h.dayOfWeek ASC, h.opensAt ASC")
    List<LocationOpeningHoursEntity> findByLocationId(@Param("locationId") UUID locationId);

    /// Removes every slot of a location (the first half of replacing the whole schedule).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM LocationOpeningHoursEntity h WHERE h.locationFk = :locationId")
    int deleteByLocationId(@Param("locationId") UUID locationId);

    /// Time zone of a business; empty when the business does not exist or has none.
    @Query("SELECT t FROM TimezoneEntity t WHERE t.id = (SELECT b.timezoneFk FROM BusinessEntity b WHERE b.id = :businessId)")
    Optional<TimezoneEntity> findTimezoneOfBusiness(@Param("businessId") UUID businessId);
}
