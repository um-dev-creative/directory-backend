package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.BusinessProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BusinessProductRepository extends JpaRepository<BusinessProductEntity, UUID> {

    // Find business-product links by business reference with pagination
    Page<BusinessProductEntity> findByBusiness(BusinessEntity business, Pageable pageable);

    // Find business-product links by business reference and active flag with pagination
    Page<BusinessProductEntity> findByBusinessAndActive(BusinessEntity business, Boolean active, Pageable pageable);

    // Public listing: the link and the product itself must both be active
    Page<BusinessProductEntity> findByBusinessAndActiveAndProductActive(BusinessEntity business, Boolean active, Boolean productActive, Pageable pageable);

    // Owner listing filtered by the product status
    Page<BusinessProductEntity> findByBusinessAndProductActive(BusinessEntity business, Boolean productActive, Pageable pageable);

    // Number of businesses owned by the user that carry the product
    @Query("SELECT count(bp) FROM BusinessProductEntity bp WHERE bp.product.id = :productId AND bp.business.userEntityFk.id = :userId")
    long countOwnedLinks(@Param("productId") UUID productId, @Param("userId") UUID userId);

}
