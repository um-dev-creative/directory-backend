package com.umdc.directory.jpa.spec;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.entity.LocationOpeningHoursEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/// Parts of the business search that the database evaluates so that no result is cut: the `open_now` filter and the order by distance.
/// Both compose with [BusinessSpecifications] and with page/count queries (one `findAll(spec, pageable)` gives rows and exact total).
public final class SearchRankingSpecifications {

    private static final double EARTH_RADIUS_M = 6_371_000d;
    /// A business with no coordinates goes after every business that has them.
    private static final double NO_DISTANCE = 1e18;
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);

    private SearchRankingSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    /// The local day and time "now" in a set of time zones.
    ///
    /// @param dayOfWeek   ISO day of the week (1 = Monday) in those zones
    /// @param time        local time (seconds) in those zones
    /// @param zoneIds     ids of `timezone` rows that resolve to those zones
    /// @param defaultZone true when the group also holds the businesses without a (known) time zone
    public record OpenGroup(int dayOfWeek, LocalTime time, Set<UUID> zoneIds, boolean defaultZone) {
    }

    /// Businesses with a location open in one of the groups. Same semantics as `OpeningHoursEvaluator.isOpenNow`: a slot of the local
    /// day that is not closed, with `opens <= time < closes` (`23:59` as closing time means until midnight). Slots never reach the next day
    /// (a night opening is a slot until 23:59 plus one from 00:00), so only the slots of the current local day matter.
    ///
    /// @param groups   one per distinct local (day, time)
    /// @param knownIds ids of every `timezone` row, to find the businesses whose zone is unknown (they use the default zone)
    public static Specification<BusinessEntity> openAt(List<OpenGroup> groups, Set<UUID> knownIds) {
        if (groups.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            for (OpenGroup group : groups) {
                Subquery<UUID> open = query.subquery(UUID.class);
                Root<LocationEntity> location = open.from(LocationEntity.class);
                Root<LocationOpeningHoursEntity> hours = open.from(LocationOpeningHoursEntity.class);
                open.select(location.get("id")).where(
                        cb.equal(location.get("business"), root),
                        cb.equal(hours.get("locationFk"), location.get("id")),
                        cb.isFalse(hours.get("closed")),
                        cb.equal(hours.get("dayOfWeek"), group.dayOfWeek()),
                        cb.lessThanOrEqualTo(hours.<LocalTime>get("opensAt"), group.time()),
                        cb.or(cb.greaterThan(hours.<LocalTime>get("closesAt"), group.time()),
                                cb.equal(hours.<LocalTime>get("closesAt"), END_OF_DAY)));
                any.add(cb.and(zone(root, cb, group, knownIds), cb.exists(open)));
            }
            return cb.or(any.toArray(new Predicate[0]));
        };
    }

    private static Predicate zone(Root<BusinessEntity> root, CriteriaBuilder cb, OpenGroup group, Set<UUID> knownIds) {
        Expression<UUID> zone = root.get("timezoneFk");
        Predicate listed = group.zoneIds().isEmpty() ? cb.disjunction() : zone.in(group.zoneIds());
        if (!group.defaultZone()) {
            return listed;
        }
        Predicate unknown = knownIds.isEmpty() ? cb.conjunction() : cb.or(cb.isNull(zone), cb.not(zone.in(knownIds)));
        return cb.or(listed, unknown);
    }

    /// Orders by the distance (whole metres) to the nearest location with coordinates, then name, then id; businesses without coordinates
    /// go last. The haversine uses the same radius as `LocationGeoRepository`. It sets the order of the query, so use it with an
    /// unsorted pageable; the count query is left alone.
    public static Specification<BusinessEntity> orderByDistance(double lat, double lng) {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType())) {
                Subquery<Double> nearest = query.subquery(Double.class);
                Root<LocationEntity> location = nearest.from(LocationEntity.class);
                nearest.select(cb.min(haversine(cb, location, lat, lng))).where(
                        cb.equal(location.get("business"), root),
                        cb.isNotNull(location.get("latitude")),
                        cb.isNotNull(location.get("longitude")));
                Expression<Double> metres = cb.function("floor", Double.class, cb.sum(nearest, 0.5d));
                query.orderBy(cb.asc(cb.coalesce(metres, NO_DISTANCE)), cb.asc(root.get("name")), cb.asc(root.get("id")));
            }
            return cb.conjunction();
        };
    }

    private static Expression<Double> haversine(CriteriaBuilder cb, Root<LocationEntity> location, double lat, double lng) {
        Expression<Double> lat2 = cb.toDouble(location.<java.math.BigDecimal>get("latitude"));
        Expression<Double> lng2 = cb.toDouble(location.<java.math.BigDecimal>get("longitude"));
        Expression<Double> halfLat = cb.prod(cb.function("radians", Double.class, cb.sum(lat2, -lat)), 0.5d);
        Expression<Double> halfLng = cb.prod(cb.function("radians", Double.class, cb.sum(lng2, -lng)), 0.5d);
        Expression<Double> sinLat = cb.function("sin", Double.class, halfLat);
        Expression<Double> sinLng = cb.function("sin", Double.class, halfLng);
        Expression<Double> a = cb.sum(cb.prod(sinLat, sinLat),
                cb.prod(cb.prod(Math.cos(Math.toRadians(lat)), cb.function("cos", Double.class, cb.function("radians", Double.class, lat2))),
                        cb.prod(sinLng, sinLng)));
        Expression<Double> clamped = cb.function("least", Double.class, a, cb.literal(1.0d));
        return cb.prod(2 * EARTH_RADIUS_M, cb.function("asin", Double.class, cb.sqrt(clamped)));
    }
}
