package com.umdc.directory.jpa.spec;

import com.umdc.directory.config.ModerationSettings;
import com.umdc.directory.constant.ModerationStatus;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.CityEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

/// Builds the search of businesses by city, category and text. A criterion that is not set adds no predicate.
public final class BusinessSpecifications {

    private static final char ESCAPE = '\\';

    private BusinessSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    /// @param criteria the filters
    /// @return the specification that combines the filters that are set
    public static Specification<BusinessEntity> from(BusinessSearchCriteria criteria) {
        return inCity(criteria.cityId(), criteria.city())
                .and(inCategory(criteria.categoryId()))
                .and(matchesText(criteria.text()))
                .and(approvedOnly());
    }

    /// With moderation on (`umdc.moderation.enabled`), only `APPROVED` businesses; otherwise no predicate.
    static Specification<BusinessEntity> approvedOnly() {
        if (!ModerationSettings.enabled()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("moderationStatus"), ModerationStatus.APPROVED);
    }

    /// Businesses that have at least one location in the city (by id, or by name when no id is given). `EXISTS` keeps one row per business.
    static Specification<BusinessEntity> inCity(UUID cityId, String cityName) {
        if (cityId == null && !StringUtils.hasText(cityName)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<LocationEntity> location = subquery.from(LocationEntity.class);
            Join<LocationEntity, CityEntity> city = location.join("city");
            Predicate sameBusiness = cb.equal(location.get("business"), root);
            Predicate sameCity = cityId != null
                    ? cb.equal(city.get("id"), cityId)
                    : cb.equal(cb.lower(city.get("name")), cityName.trim().toLowerCase(Locale.ROOT));
            subquery.select(location.get("id")).where(sameBusiness, sameCity);
            return cb.exists(subquery);
        };
    }

    /// Businesses of the category, or of a category whose parent is it.
    static Specification<BusinessEntity> inCategory(UUID categoryId) {
        if (categoryId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Join<BusinessEntity, CategoryEntity> category = root.join("categoryFk", JoinType.INNER);
            Join<CategoryEntity, CategoryEntity> parent = category.join("categoryParentFk", JoinType.LEFT);
            return cb.or(cb.equal(category.get("id"), categoryId), cb.equal(parent.get("id"), categoryId));
        };
    }

    /// Case-insensitive match of the text in the name or the description; `%`, `_` and `\` in the text are literals.
    static Specification<BusinessEntity> matchesText(String text) {
        if (!StringUtils.hasText(text)) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escape(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, ESCAPE),
                cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), pattern, ESCAPE));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
