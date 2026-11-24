package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.CategoryTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/// Translated category names.
public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslationEntity, UUID> {

    /// Translations in one locale for a set of categories (one query for a whole page, no N+1).
    ///
    /// @param categoryIds category ids
    /// @param locale      BCP 47 tag
    /// @return the translations that exist
    List<CategoryTranslationEntity> findByCategoryFkInAndLocale(Collection<UUID> categoryIds, String locale);
}
