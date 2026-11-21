package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.TimezoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TimezoneRepository extends JpaRepository<TimezoneEntity, UUID> {
}
