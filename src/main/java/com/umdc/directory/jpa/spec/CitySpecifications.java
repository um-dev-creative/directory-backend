package com.umdc.directory.jpa.spec;

import com.umdc.directory.jpa.entity.CityEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

/// Filters of the cities listing. A criterion that is not set adds no predicate.
public final class CitySpecifications {

    private static final char ESCAPE = '\\';

    private CitySpecifications() {
        throw new IllegalStateException("Utility class");
    }

    /// @param country ISO 3166-1 alpha-2 code (case-insensitive), or blank for every country
    /// @param text    text found in the city name (case-insensitive), or blank for every city
    /// @return the specification that combines the filters that are set
    public static Specification<CityEntity> from(String country, String text) {
        return inCountry(country).and(nameContains(text));
    }

    static Specification<CityEntity> inCountry(String country) {
        if (!StringUtils.hasText(country)) {
            return Specification.unrestricted();
        }
        String code = country.trim().toUpperCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(root.get("country"), code);
    }

    static Specification<CityEntity> nameContains(String text) {
        if (!StringUtils.hasText(text)) {
            return Specification.unrestricted();
        }
        String escaped = text.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String pattern = "%" + escaped + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, ESCAPE);
    }
}
