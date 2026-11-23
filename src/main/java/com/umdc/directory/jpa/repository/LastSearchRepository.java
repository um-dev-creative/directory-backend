package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.LastSearchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/// Repository of the last search; the id is the user id.
public interface LastSearchRepository extends JpaRepository<LastSearchEntity, UUID> {
}
