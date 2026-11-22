package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;

/// Number of views of a business on one UTC day (`directory_site.business_view_daily`). It holds no personal data.
@Entity
@Table(name = "business_view_daily", schema = "directory_site")
public class BusinessViewDailyEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 3320914488120556931L;

    @EmbeddedId
    private BusinessViewDailyId id;

    @Column(name = "views", nullable = false)
    private long views;

    public BusinessViewDailyEntity() {
        // Default constructor
    }

    public BusinessViewDailyEntity(BusinessViewDailyId id, long views) {
        this.id = id;
        this.views = views;
    }

    public BusinessViewDailyId getId() {
        return id;
    }

    public void setId(BusinessViewDailyId id) {
        this.id = id;
    }

    public long getViews() {
        return views;
    }

    public void setViews(long views) {
        this.views = views;
    }
}
