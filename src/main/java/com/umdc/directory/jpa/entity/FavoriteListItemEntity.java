package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/// A favorite placed in a list (`directory_site.favorite_list_item`). A favorite may be in several lists.
@Entity
@Table(name = "favorite_list_item", schema = "directory_site")
public class FavoriteListItemEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "list_fk", nullable = false)
    private UUID listFk;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "favorite_fk", nullable = false)
    private UserFavoriteEntity favorite;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FavoriteListItemEntity() {
        super();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getListFk() {
        return listFk;
    }

    public void setListFk(UUID listFk) {
        this.listFk = listFk;
    }

    public UserFavoriteEntity getFavorite() {
        return favorite;
    }

    public void setFavorite(UserFavoriteEntity favorite) {
        this.favorite = favorite;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
