package com.umdc.directory.jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/// Geographic queries over `directory_site.location`: Haversine distance in portable SQL (PostgreSQL and H2 in PostgreSQL mode,
/// no PostGIS) with a bounding-box pre-filter that the index `idx_location_geo` can serve. Only locations with coordinates count.
@Repository
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.FieldDeclarationsShouldBeAtStartOfClass", "PMD.InsufficientStringBufferDeclaration",
        "PMD.ConsecutiveLiteralAppends", "PMD.AvoidDuplicateLiterals", "PMD.AvoidLiteralsInIfCondition", "PMD.AppendCharacterWithChar",
        "PMD.ConsecutiveAppendsShouldReuse"}) // SQL text builder
public class LocationGeoRepository {

    private static final double EARTH_RADIUS_M = 6_371_000d;
    private static final double METRES_PER_DEGREE = 111_320d;
    private static final String ESCAPE_CLAUSE = " escape '\\'";

    @PersistenceContext
    private EntityManager em;

    /// A location found by `nearby`.
    public record NearbyRow(UUID locationId, UUID businessId, String name, String description, UUID categoryId, String categoryName,
                            boolean verified, String profileImageRef, UUID cityId, String cityName, String locationName,
                            String addressLine, double latitude, double longitude, boolean main, double distanceM) {
    }

    /// A location found by `map`.
    public record MapPointRow(UUID locationId, UUID businessId, String name, UUID categoryId, boolean verified, boolean main,
                              double latitude, double longitude) {
    }

    /// A grid cell of the clustered map.
    public record ClusterRow(long count, double latitude, double longitude) {
    }

    /// @return the locations within `radiusM` metres, ordered by distance (or name) and then by id
    public List<NearbyRow> findNearby(double lat, double lng, int radiusM, UUID categoryId, UUID cityId, String text,
                                      boolean sortByName, int limit, int offset) {
        List<Object> params = new ArrayList<>();
        String inner = nearbyInner(lat, lng, radiusM, categoryId, cityId, text, params);
        String order = sortByName ? "t.name, t.distance_m, t.id" : "t.distance_m, t.id";
        Query query = em.createNativeQuery("select t.* from (" + inner + ") t where t.distance_m <= :radius order by " + order
                + " limit :lim offset :off");
        bind(query, params);
        query.setParameter("radius", (double) radiusM);
        query.setParameter("lim", limit);
        query.setParameter("off", offset);
        List<?> rows = query.getResultList();
        List<NearbyRow> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] c = (Object[]) row;
            result.add(new NearbyRow(uuid(c[0]), uuid(c[1]), (String) c[2], (String) c[3], uuid(c[4]), (String) c[5], bool(c[6]),
                    (String) c[7], uuid(c[8]), (String) c[9], (String) c[10], (String) c[11], num(c[12]), num(c[13]), bool(c[14]),
                    num(c[15])));
        }
        return result;
    }

    /// @return how many locations are within `radiusM` metres and match the filters
    public long countNearby(double lat, double lng, int radiusM, UUID categoryId, UUID cityId, String text) {
        List<Object> params = new ArrayList<>();
        String inner = nearbyInner(lat, lng, radiusM, categoryId, cityId, text, params);
        Query query = em.createNativeQuery("select count(*) from (" + inner + ") t where t.distance_m <= :radius");
        bind(query, params);
        query.setParameter("radius", (double) radiusM);
        return ((Number) query.getSingleResult()).longValue();
    }

    /// @return up to `limit` locations inside the bbox, ordered by id (the caller asks for one more than it shows to know if it was cut)
    public List<MapPointRow> findInBbox(double minLng, double minLat, double maxLng, double maxLat, UUID categoryId, UUID cityId,
                                        String text, int limit) {
        List<Object> params = new ArrayList<>();
        String where = bboxWhere(minLng, minLat, maxLng, maxLat, categoryId, cityId, text, params);
        Query query = em.createNativeQuery("select l.id, b.id, b.name, b.category_fk, b.verified, l.main, l.latitude, l.longitude "
                + FROM + where + " order by l.id limit :lim");
        bind(query, params);
        query.setParameter("lim", limit);
        List<?> rows = query.getResultList();
        List<MapPointRow> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] c = (Object[]) row;
            result.add(new MapPointRow(uuid(c[0]), uuid(c[1]), (String) c[2], uuid(c[3]), bool(c[4]), bool(c[5]), num(c[6]), num(c[7])));
        }
        return result;
    }

    /// @param cellDeg size of a grid cell in degrees
    /// @return up to `limit` cells with their location count and centroid, the most populated first
    public List<ClusterRow> findClusters(double minLng, double minLat, double maxLng, double maxLat, UUID categoryId, UUID cityId,
                                         String text, double cellDeg, int limit) {
        List<Object> params = new ArrayList<>();
        String where = bboxWhere(minLng, minLat, maxLng, maxLat, categoryId, cityId, text, params);
        // the cell size is a computed number, inlined so that GROUP BY and SELECT hold the very same expression
        String cell = String.format(Locale.ROOT, "%.9f", cellDeg);
        String cx = "floor(cast(l.longitude as double precision) / " + cell + ")";
        String cy = "floor(cast(l.latitude as double precision) / " + cell + ")";
        Query query = em.createNativeQuery("select count(*), avg(cast(l.latitude as double precision)), avg(cast(l.longitude as double precision)) "
                + FROM + where + " group by " + cx + ", " + cy + " order by count(*) desc, " + cx + ", " + cy + " limit :lim");
        bind(query, params);
        query.setParameter("lim", limit);
        List<?> rows = query.getResultList();
        List<ClusterRow> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] c = (Object[]) row;
            result.add(new ClusterRow(((Number) c[0]).longValue(), num(c[1]), num(c[2])));
        }
        return result;
    }

    private static final String FROM = "from directory_site.location l join directory_site.business b on b.id = l.business_fk "
            + "join directory_site.category c on c.id = b.category_fk join directory_site.city ci on ci.id = l.city_fk ";

    private String nearbyInner(double lat, double lng, int radiusM, UUID categoryId, UUID cityId, String text, List<Object> params) {
        double latDelta = radiusM / METRES_PER_DEGREE;
        double cos = Math.cos(Math.toRadians(lat));
        double lngDelta = cos < 1e-6 ? 360d : radiusM / (METRES_PER_DEGREE * cos);
        StringBuilder where = new StringBuilder(" where l.latitude is not null and l.longitude is not null");
        where.append(" and l.latitude between ").append(lit(Math.max(-90, lat - latDelta))).append(" and ").append(lit(Math.min(90, lat + latDelta)));
        double west = lng - lngDelta;
        double east = lng + lngDelta;
        if (lngDelta < 180d) {
            if (west < -180d) {
                where.append(" and (l.longitude >= ").append(lit(west + 360d)).append(" or l.longitude <= ").append(lit(east)).append(")");
            } else if (east > 180d) {
                where.append(" and (l.longitude >= ").append(lit(west)).append(" or l.longitude <= ").append(lit(east - 360d)).append(")");
            } else {
                where.append(" and l.longitude between ").append(lit(west)).append(" and ").append(lit(east));
            }
        }
        filters(where, categoryId, cityId, text, params);
        String d = "(2 * " + lit(EARTH_RADIUS_M) + " * asin(sqrt(least(1.0, power(sin(radians(cast(l.latitude as double precision) - " + lit(lat)
                + ") / 2), 2) + " + lit(Math.cos(Math.toRadians(lat))) + " * cos(radians(cast(l.latitude as double precision))) * power(sin(radians(cast(l.longitude as double precision) - "
                + lit(lng) + ") / 2), 2)))))";
        return "select l.id, b.id as business_id, b.name, b.description, b.category_fk, c.name as category_name, b.verified, b.profile_image_ref, "
                + "l.city_fk, ci.name as city_name, l.name as location_name, l.address_line, l.latitude, l.longitude, l.main, "
                + d + " as distance_m " + FROM + where;
    }

    private String bboxWhere(double minLng, double minLat, double maxLng, double maxLat, UUID categoryId, UUID cityId, String text,
                             List<Object> params) {
        StringBuilder where = new StringBuilder(" where l.latitude is not null and l.longitude is not null");
        where.append(" and l.latitude between ").append(lit(minLat)).append(" and ").append(lit(maxLat));
        where.append(" and l.longitude between ").append(lit(minLng)).append(" and ").append(lit(maxLng));
        filters(where, categoryId, cityId, text, params);
        return where.toString();
    }

    private void filters(StringBuilder where, UUID categoryId, UUID cityId, String text, List<Object> params) {
        if (categoryId != null) {
            where.append(" and (c.id = :p").append(params.size() + 1).append(" or c.category_parent_fk = :p").append(params.size() + 1).append(")");
            params.add(categoryId);
        }
        if (cityId != null) {
            where.append(" and l.city_fk = :p").append(params.size() + 1);
            params.add(cityId);
        }
        if (text != null) {
            int i = params.size() + 1;
            where.append(" and (lower(b.name) like :p").append(i).append(ESCAPE_CLAUSE)
                    .append(" or lower(coalesce(b.description, '')) like :p").append(i).append(ESCAPE_CLAUSE).append(")");
            params.add("%" + escape(text.toLowerCase(Locale.ROOT)) + "%");
        }
    }

    private static void bind(Query query, List<Object> params) {
        for (int i = 0; i < params.size(); i++) {
            query.setParameter("p" + (i + 1), params.get(i));
        }
    }

    /// A double as an SQL literal; the values come from validated numbers, never from text.
    private static String lit(double value) {
        return String.format(Locale.ROOT, "%.9f", value);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static UUID uuid(Object value) {
        if (value instanceof UUID u) {
            return u;
        }
        if (value instanceof byte[] bytes && bytes.length == 16) {
            var buffer = java.nio.ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(String.valueOf(value));
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean b && b;
    }

    private static double num(Object value) {
        return ((Number) value).doubleValue();
    }
}
