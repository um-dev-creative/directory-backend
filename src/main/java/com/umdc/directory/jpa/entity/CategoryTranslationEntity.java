package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/// Name of a category in one locale (`en`, `es`, `fr-CA`). Unique per (category, locale). `category_fk` is a plain UUID
/// column; the table lives in `directory_site` like its category.
@Entity
@Table(name = "category_translation", schema = "directory_site",
        uniqueConstraints = @UniqueConstraint(name = "uq_category_translation", columnNames = {"category_fk", "locale"}))
public class CategoryTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "category_fk", nullable = false)
    private UUID categoryFk;

    @NotNull
    @Size(max = 10)
    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @NotNull
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    /// Default constructor for JPA.
    public CategoryTranslationEntity() {
        // JPA
    }

    /// @param categoryFk category id
    /// @param locale     BCP 47 tag
    /// @param name       translated name
    public CategoryTranslationEntity(UUID categoryFk, String locale, String name) {
        this.categoryFk = categoryFk;
        this.locale = locale;
        this.name = name;
    }

    /// @return row id
    public UUID getId() {
        return id;
    }

    /// @param id row id
    public void setId(UUID id) {
        this.id = id;
    }

    /// @return category id
    public UUID getCategoryFk() {
        return categoryFk;
    }

    /// @param categoryFk category id
    public void setCategoryFk(UUID categoryFk) {
        this.categoryFk = categoryFk;
    }

    /// @return BCP 47 tag of the translation
    public String getLocale() {
        return locale;
    }

    /// @param locale BCP 47 tag
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /// @return translated name
    public String getName() {
        return name;
    }

    /// @param name translated name
    public void setName(String name) {
        this.name = name;
    }
}
