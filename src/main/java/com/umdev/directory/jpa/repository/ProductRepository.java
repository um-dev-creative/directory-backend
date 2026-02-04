package com.umdev.directory.jpa.repository;

import com.umdev.directory.jpa.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository interface for accessing ProductEntity data.
 * This interface extends JpaRepository to provide CRUD operations for ProductEntity.
 */
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
}
