package com.prx.directory.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class representing a business.
 * This class is mapped to the "business" table in the "directory_site" schema.
 * It includes a UUID as the primary key and various fields representing business details.
 */
@Entity
@Table(name = "business", schema = "directory_site")
public class BusinessEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1118746604883297160L;

    /**
     * The unique identifier for the business.
     * This field is automatically generated using the UUID generation strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ColumnDefault("directory_site.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The name of the business.
     * This field is required and has a maximum length of 255 characters.
     */
    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The description of the business.
     * This field is optional and has a maximum length of 1500 characters.
     */
    @Size(max = 1500)
    @Column(name = "description", length = 1500)
    private String description;

    /**
     * The date when the business was created.
     * This field is required.
     */
    @NotNull
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    /**
     * The date when the business was last updated.
     * This field is required.
     */
    @NotNull
    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    /**
     * The category to which the business belongs.
     * This field is required and is a many-to-one relationship with the CategoryEntity.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_fk", nullable = false)
    private CategoryEntity categoryFk;

    /**
     * The user who owns the business.
     * This field is required and is a many-to-one relationship with the UserEntity.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_fk", nullable = false)
    private UserEntity userEntityFk;

    @OneToMany(mappedBy = "business")
    private Set<DigitalContactEntity> digitalContacts;

    public BusinessEntity() {
        // Default constructor
    }

    /**
     * Gets the unique identifier for the business.
     *
     * @return the unique identifier for the business
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the business.
     *
     * @param id the unique identifier for the business
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the name of the business.
     *
     * @return the name of the business
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the business.
     *
     * @param name the name of the business
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the business.
     *
     * @return the description of the business
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the business.
     *
     * @param description the description of the business
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the date when the business was created.
     *
     * @return the date when the business was created
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the date when the business was created.
     *
     * @param createdDate the date when the business was created
     */
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the date when the business was last updated.
     *
     * @return the date when the business was last updated
     */
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Sets the date when the business was last updated.
     *
     * @param lastUpdate the date when the business was last updated
     */
    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * Gets the category to which the business belongs.
     *
     * @return the category to which the business belongs
     */
    public CategoryEntity getCategoryFk() {
        return categoryFk;
    }

    /**
     * Sets the category to which the business belongs.
     *
     * @param categoryFk the category to which the business belongs
     */
    public void setCategoryFk(CategoryEntity categoryFk) {
        this.categoryFk = categoryFk;
    }

    /**
     * Gets the user who owns the business.
     *
     * @return the user who owns the business
     */
    public UserEntity getUserFk() {
        return userEntityFk;
    }

    /**
     * Sets the user who owns the business.
     *
     * @param userEntityFk the user who owns the business
     */
    public void setUserFk(UserEntity userEntityFk) {
        this.userEntityFk = userEntityFk;
    }


    /**
     * Retrieves the set of digital contacts associated with the business.
     *
     * @return a set of {@link DigitalContactEntity} representing the digital contacts of the business
     */
    public Set<DigitalContactEntity> getDigitalContacts() {
        return digitalContacts;
    }

    /**
     * Sets the digital contacts associated with the business.
     *
     * @param digitalContacts a set of {@link DigitalContactEntity} representing the digital contacts to be associated with the business
     */
    public void setDigitalContacts(Set<DigitalContactEntity> digitalContacts) {
        this.digitalContacts = digitalContacts;
    }
}
