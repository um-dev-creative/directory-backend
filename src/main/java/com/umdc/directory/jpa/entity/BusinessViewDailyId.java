package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/// Key of the daily view counter: a business and a UTC day.
@Embeddable
public class BusinessViewDailyId implements Serializable {
    @Serial
    private static final long serialVersionUID = 6118820451923084417L;

    @NotNull
    @Column(name = "business_fk", nullable = false)
    private UUID businessId;

    @NotNull
    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;

    public BusinessViewDailyId() {
        // Default constructor
    }

    public BusinessViewDailyId(UUID businessId, LocalDate viewDate) {
        this.businessId = businessId;
        this.viewDate = viewDate;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public LocalDate getViewDate() {
        return viewDate;
    }

    public void setViewDate(LocalDate viewDate) {
        this.viewDate = viewDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BusinessViewDailyId that = (BusinessViewDailyId) o;
        return Objects.equals(businessId, that.businessId) && Objects.equals(viewDate, that.viewDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessId, viewDate);
    }
}
