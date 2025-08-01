package com.prx.directory.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing a product.
 * This class is mapped to the "product" table in the "directory_site" schema.
 * It includes a UUID as the primary key and various fields representing product details.
 */
@Entity
@Table(name = "product", schema = "directory_site")
public class ProductEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -1774039642617350471L;

    /**
     * The unique identifier for the product.
     * This field is automatically generated using the UUID generation strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ColumnDefault("directory_site.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The name of the product.
     * This field is required and has a maximum length of 255 characters.
     */
    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The description of the product.
     * This field is required and has a maximum length of 1500 characters.
     */
    @Size(max = 1500)
    @NotNull
    @Column(name = "description", nullable = false, length = 1500)
    private String description;

    /**
     * The date when the product was createdAt.
     * This field is required.
     */
    @NotNull
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    /**
     * The date when the product was last updated.
     * This field is required.
     */
    @NotNull
    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    /**
     * Indicates whether the product is active.
     * This field is required and defaults to true.
     */
    @NotNull
    @ColumnDefault("true")
    @Column(name = "active", nullable = false)
    private Boolean active = false;

    /**
     * The category to which the product belongs.
     * This field is required and is a many-to-one relationship with the CategoryEntity.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_fk", nullable = false)
    private CategoryEntity categoryFk;

    public ProductEntity() {
        // Default constructor
    }

    /**
     * Gets the unique identifier for the product.
     *
     * @return the unique identifier for the product
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the product.
     *
     * @param id the unique identifier for the product
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the name of the product.
     *
     * @return the name of the product
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the product.
     *
     * @param name the name of the product
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the product.
     *
     * @return the description of the product
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the product.
     *
     * @param description the description of the product
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the date when the product was createdAt.
     *
     * @return the date when the product was createdAt
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the date when the product was createdAt.
     *
     * @param createDate the date when the product was createdAt
     */
    public void setCreatedDate(LocalDateTime createDate) {
        this.createdDate = createDate;
    }

    /**
     * Gets the date when the product was last lastUpdate.
     *
     * @return the date when the product was last lastUpdate
     */
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Sets the date when the product was last lastUpdate.
     *
     * @param lastDate the date when the product was last lastUpdate
     */
    public void setLastUpdate(LocalDateTime lastDate) {
        this.lastUpdate = lastDate;
    }

    /**
     * Gets the active status of the product.
     *
     * @return the active status of the product
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Sets the active status of the product.
     *
     * @param active the active status of the product
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * Gets the category to which the product belongs.
     *
     * @return the category to which the product belongs
     */
    public CategoryEntity getCategoryFk() {
        return categoryFk;
    }

    /**
     * Sets the category to which the product belongs.
     *
     * @param categoryFk the category to which the product belongs
     */
    public void setCategoryFk(CategoryEntity categoryFk) {
        this.categoryFk = categoryFk;
    }

}
