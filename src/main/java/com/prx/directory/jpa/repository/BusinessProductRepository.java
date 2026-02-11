package com.prx.directory.jpa.repository;

import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.BusinessProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BusinessProductRepository extends JpaRepository<BusinessProductEntity, UUID> {

    // Find business-product links by business reference with pagination
    Page<BusinessProductEntity> findByBusiness(BusinessEntity business, Pageable pageable);

    // Find business-product links by business reference and active flag with pagination
    Page<BusinessProductEntity> findByBusinessAndActive(BusinessEntity business, Boolean active, Pageable pageable);

}
