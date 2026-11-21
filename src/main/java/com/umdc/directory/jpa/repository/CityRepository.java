package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/// Repository of the cities catalogue; the filtered listing is built with `CitySpecifications`.
public interface CityRepository extends JpaRepository<CityEntity, UUID>, JpaSpecificationExecutor<CityEntity> {
}
