package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.UUID;

/// One time slot of the opening hours of a location (`directory_site.location_opening_hours`), in local time of the business.
@Entity
@Table(name = "location_opening_hours", schema = "directory_site")
public class LocationOpeningHoursEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 6481209335511730452L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "location_fk", nullable = false)
    private UUID locationFk;

    /// ISO day of the week: 1 = Monday ... 7 = Sunday.
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "closed", nullable = false)
    private boolean closed;

    public LocationOpeningHoursEntity() {
        // Default constructor
    }

    public LocationOpeningHoursEntity(UUID locationFk, int dayOfWeek, LocalTime opensAt, LocalTime closesAt, boolean closed) {
        this.locationFk = locationFk;
        this.dayOfWeek = dayOfWeek;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.closed = closed;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLocationFk() {
        return locationFk;
    }

    public void setLocationFk(UUID locationFk) {
        this.locationFk = locationFk;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(LocalTime opensAt) {
        this.opensAt = opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalTime closesAt) {
        this.closesAt = closesAt;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }
}
