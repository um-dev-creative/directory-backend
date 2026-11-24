package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/// The last search of a user, one row per user (`directory_site.last_search`); the primary key is the user id.
@Entity
@Table(name = "last_search", schema = "directory_site")
public class LastSearchEntity extends SearchParamsBase implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "user_fk", nullable = false)
    private UUID userFk;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LastSearchEntity() {
        super();
    }

    public UUID getUserFk() {
        return userFk;
    }

    public void setUserFk(UUID userFk) {
        this.userFk = userFk;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
