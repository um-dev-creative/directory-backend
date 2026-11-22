package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/// A city of the catalogue (`directory_site.city`). The search by city and the locations of a business point to it.
@Entity
@Table(name = "city", schema = "directory_site")
public class CityEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 4471320984420167713L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 100)
    @NotNull
    @Column(name = "province", nullable = false, length = 100)
    private String province;

    /// ISO 3166-1 alpha-2 code, e.g. `CA`.
    @Size(min = 2, max = 2)
    @NotNull
    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @NotNull
    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    public CityEntity() {
        // Default constructor
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
