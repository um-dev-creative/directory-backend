package com.prx.directory.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user's favorite item in the directory.
 * <p>
 * A favorite can be associated with a business, product, or campaign, and is always linked to a user.
 * This entity tracks which items a user has marked as favorite, along with status and timestamps.
 */
@Entity
@Table(name = "user_favorite", schema = "directory_site")
public class UserFavoriteEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -8555594513161495866L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_fk", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_fk")
    private BusinessEntity business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_fk")
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_fk")
    private CampaignEntity campaign;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Default no-arg constructor required by JPA and by code style rules.
     */
    public UserFavoriteEntity() {
        //Default constructor
    }

    /**
     * Convenience constructor to create a favorite for a given user.
     *
     * @param user the user owning the favorite
     */
    public UserFavoriteEntity(UserEntity user) {
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public BusinessEntity getBusiness() {
        return business;
    }

    public void setBusiness(BusinessEntity business) {
        this.business = business;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public CampaignEntity getCampaign() {
        return campaign;
    }

    public void setCampaign(CampaignEntity campaign) {
        this.campaign = campaign;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
