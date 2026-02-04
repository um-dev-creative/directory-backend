package com.umdev.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class BusinessProductEntityId implements Serializable {
    @Serial
    private static final long serialVersionUID = -201742942413256456L;
    @NotNull
    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    public BusinessProductEntityId() {
        // Default constructor
    }

    public BusinessProductEntityId(UUID businessId, UUID productId) {
        this.businessId = businessId;
        this.productId = productId;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BusinessProductEntityId entity = (BusinessProductEntityId) o;
        return Objects.equals(this.productId, entity.productId) &&
                Objects.equals(this.businessId, entity.businessId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, businessId);
    }

}
