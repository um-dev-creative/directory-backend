package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.SearchParamsTO;
import com.umdc.directory.constant.SavedSearchConstants;
import com.umdc.directory.jpa.entity.SearchParamsBase;

import java.util.Objects;
import java.util.UUID;

/// Validates search parameters with {@link GeoSearchValidator} and copies them to and from the entities.
final class SearchParamsApplier {

    private static final int MAX_CITY_LENGTH = 100;

    private SearchParamsApplier() {
        throw new IllegalStateException("Utility class");
    }

    /// Validates the parameters with {@link GeoSearchValidator} and copies the normalised values into the entity.
    ///
    /// @throws GeoSearchException when a parameter is invalid or the combination makes no sense
    @SuppressWarnings("PMD.CognitiveComplexity")
    static void apply(SearchParamsTO p, SearchParamsBase target) {
        boolean hasPoint = Objects.nonNull(p.lat()) || Objects.nonNull(p.lng());
        boolean hasBbox = Objects.nonNull(p.bbox()) && !p.bbox().isBlank();
        if (hasPoint && (Objects.isNull(p.lat()) || Objects.isNull(p.lng()))) {
            throw new GeoSearchException(SavedSearchConstants.LAT_LNG_TOGETHER);
        }
        if (hasPoint && hasBbox) {
            throw new GeoSearchException(SavedSearchConstants.POINT_AND_BBOX);
        }
        boolean hasSort = Objects.nonNull(p.sort()) && !p.sort().isBlank();
        if (!hasPoint && (Objects.nonNull(p.radiusM()) || hasSort)) {
            throw new GeoSearchException(SavedSearchConstants.GEO_NEEDS_POINT);
        }
        String city = Objects.isNull(p.city()) || p.city().isBlank() ? null : p.city().trim();
        if (Objects.nonNull(city) && city.length() > MAX_CITY_LENGTH) {
            throw new GeoSearchException(SavedSearchConstants.CITY_TOO_LONG);
        }
        UUID categoryId;
        UUID cityId;
        String text;
        if (hasPoint) {
            var nearby = GeoSearchValidator.nearby(String.valueOf(p.lat()), String.valueOf(p.lng()),
                    Objects.isNull(p.radiusM()) ? null : String.valueOf(p.radiusM()), p.categoryId(), p.cityId(), p.q(), p.sort(), null, null);
            categoryId = nearby.categoryId();
            cityId = nearby.cityId();
            text = nearby.text();
            target.setLat(nearby.lat());
            target.setLng(nearby.lng());
            target.setRadiusM(Objects.isNull(p.radiusM()) ? null : nearby.radiusM());
            target.setSort(hasSort ? (nearby.sortByName() ? "name" : "distance") : null);
            target.setBbox(null);
        } else if (hasBbox) {
            var map = GeoSearchValidator.map(p.bbox(), null, p.categoryId(), p.cityId(), p.q(), null);
            categoryId = map.categoryId();
            cityId = map.cityId();
            text = map.text();
            target.setBbox(p.bbox().trim());
            target.setLat(null);
            target.setLng(null);
            target.setRadiusM(null);
            target.setSort(null);
        } else {
            var filters = GeoSearchValidator.filters(p.categoryId(), p.cityId(), p.q());
            categoryId = filters.categoryId();
            cityId = filters.cityId();
            text = filters.text();
            target.setBbox(null);
            target.setLat(null);
            target.setLng(null);
            target.setRadiusM(null);
            target.setSort(null);
        }
        if (!hasPoint && !hasBbox && Objects.isNull(categoryId) && Objects.isNull(cityId) && Objects.isNull(text) && Objects.isNull(city)) {
            throw new GeoSearchException(SavedSearchConstants.EMPTY_SEARCH);
        }
        target.setQ(text);
        target.setCategoryId(categoryId);
        target.setCityId(cityId);
        target.setCity(city);
    }

    static SearchParamsTO toParams(SearchParamsBase e) {
        return new SearchParamsTO(e.getQ(), Objects.toString(e.getCategoryId(), null), Objects.toString(e.getCityId(), null), e.getCity(),
                e.getLat(), e.getLng(), e.getRadiusM(), e.getBbox(), e.getSort());
    }
}
