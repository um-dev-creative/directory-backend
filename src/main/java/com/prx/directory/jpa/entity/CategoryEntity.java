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
 * Entity class representing a category.
 * This class is mapped to the "category" table in the "directory_site" schema.
 * It includes a UUID as the primary key and various fields representing category details.
 */
@Entity
@Table(name = "category", schema = "directory_site")
public class CategoryEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 7390418209692828638L;

    /**
     * The unique identifier for the category.
     * This field is automatically generated using the UUID generation strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ColumnDefault("directory_site.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The name of the category.
     * This field is required and has a maximum length of 255 characters.
     */
    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The description of the category.
     * This field is required and has a maximum length of 1200 characters.
     */
    @Size(max = 1200)
    @NotNull
    @Column(name = "description", nullable = false, length = 1200)
    private String description;

    /**
     * The parent category of this category.
     * This field is optional and represents a many-to-one relationship with the CategoryEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_parent_fk")
    private CategoryEntity categoryParentFk;

    /**
     * The date when the category was createdAt.
     * This field is required.
     */
    @NotNull
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    /**
     * The date when the category was last updated.
     * This field is required.
     */
    @NotNull
    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    /**
     * Indicates whether the category is active.
     * This field is required and defaults to true.
     */
    @NotNull
    @ColumnDefault("true")
    @Column(name = "active", nullable = false)
    private Boolean active = false;

    public CategoryEntity() {
        // Default constructor
    }

    /**
     * Gets the unique identifier for the category.
     *
     * @return the unique identifier for the category
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the category.
     *
     * @param id the unique identifier for the category
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the name of the category.
     *
     * @return the name of the category
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the category.
     *
     * @param name the name of the category
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the category.
     *
     * @return the description of the category
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the category.
     *
     * @param description the description of the category
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the parent category of this category.
     *
     * @return the parent category of this category
     */
    public CategoryEntity getCategoryParentFk() {
        return categoryParentFk;
    }

    /**
     * Sets the parent category of this category.
     *
     * @param categoryParentFk the parent category of this category
     */
    public void setCategoryParentFk(CategoryEntity categoryParentFk) {
        this.categoryParentFk = categoryParentFk;
    }

    /**
     * Gets the date when the category was createdAt.
     *
     * @return the date when the category was createdAt
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the date when the category was createdAt.
     *
     * @param createDate the date when the category was createdAt
     */
    public void setCreatedDate(LocalDateTime createDate) {
        this.createdDate = createDate;
    }

    /**
     * Gets the date when the category was last lastUpdate.
     *
     * @return the date when the category was last lastUpdate
     */
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Sets the date when the category was last lastUpdate.
     *
     * @param lastUpdate the date when the category was last lastUpdate
     */
    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * Gets the active status of the category.
     *
     * @return the active status of the category
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Sets the active status of the category.
     *
     * @param active the active status of the category
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

}
