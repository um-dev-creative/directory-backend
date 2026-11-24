package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.service.GeoSearchValidator.MapQuery;
import com.umdc.directory.api.v1.service.GeoSearchValidator.NearbyQuery;
import com.umdc.directory.api.v1.to.BusinessMapResponse;
import com.umdc.directory.api.v1.to.BusinessNearbyItemTO;
import com.umdc.directory.api.v1.to.BusinessNearbyResponse;
import com.umdc.directory.api.v1.to.GeoPointTO;
import com.umdc.directory.api.v1.to.MapFeatureTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.repository.LocationGeoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.UseConcurrentHashMap"}) // text parameters on purpose; maps are local
public class BusinessGeoServiceImpl implements BusinessGeoService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessGeoServiceImpl.class);

    /// Grid of the nearby cache (degrees, about 110 m): the centre is snapped to it when the cache is on.
    static final double NEARBY_GRID = 0.001;
    /// A map cache cell is this fraction of the clustering cell at the zoom (never below {@link #MIN_MAP_STEP}).
    private static final double MAP_STEP_DIVISOR = 4d;
    static final double MIN_MAP_STEP = 0.0005;

    private final LocationGeoRepository repository;
    private final GeoResponseCache cache;

    /// Service without cache.
    public BusinessGeoServiceImpl(LocationGeoRepository repository) {
        this(repository, GeoResponseCache.disabled());
    }

    /// @param repository geo queries
    /// @param cache      Redis result cache (`umdc.geo.cache-enabled`); `null` is treated as disabled
    @Autowired
    public BusinessGeoServiceImpl(LocationGeoRepository repository, GeoResponseCache cache) {
        this.repository = repository;
        this.cache = cache != null ? cache : GeoResponseCache.disabled();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BusinessNearbyResponse> nearby(String lat, String lng, String radiusM, String categoryId, String cityId,
                                                         String text, String sort, String page, String perPage) {
        NearbyQuery q;
        try {
            q = GeoSearchValidator.nearby(lat, lng, radiusM, categoryId, cityId, text, sort, page, perPage);
        } catch (GeoSearchException e) {
            return badRequest(e);
        }
        String cacheKey = null;
        if (cache.enabled()) {
            q = snap(q);
            cacheKey = nearbyKey(q);
            var hit = cache.get(cacheKey, BusinessNearbyResponse.class);
            if (hit.isPresent()) {
                return ResponseEntity.ok(hit.get());
            }
        }
        // exact coordinates are not logged: rounded to ~1 km
        logger.info("Nearby search lat~{} lng~{} radiusM={} page={}", approx(q.lat()), approx(q.lng()), q.radiusM(), q.page());
        long total = repository.countNearby(q.lat(), q.lng(), q.radiusM(), q.categoryId(), q.cityId(), q.text());
        List<BusinessNearbyItemTO> items = repository.findNearby(q.lat(), q.lng(), q.radiusM(), q.categoryId(), q.cityId(), q.text(),
                        q.sortByName(), q.perPage(), (q.page() - 1) * q.perPage()).stream()
                .map(r -> new BusinessNearbyItemTO(r.locationId(), r.businessId(), r.name(), r.description(), r.categoryId(),
                        r.categoryName(), r.verified(), r.profileImageRef(), r.cityId(), r.cityName(), r.locationName(),
                        r.addressLine(), r.latitude(), r.longitude(), r.main(), Math.round(r.distanceM())))
                .toList();
        int totalPages = (int) ((total + q.perPage() - 1) / q.perPage());
        var body = new BusinessNearbyResponse(total, q.page(), q.perPage(), totalPages, items);
        if (cacheKey != null) {
            cache.put(cacheKey, body);
        }
        return ResponseEntity.ok(body);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BusinessMapResponse> map(String bbox, String zoom, String categoryId, String cityId, String text, String limit) {
        MapQuery q;
        try {
            q = GeoSearchValidator.map(bbox, zoom, categoryId, cityId, text, limit);
        } catch (GeoSearchException e) {
            return badRequest(e);
        }
        String cacheKey = null;
        if (cache.enabled()) {
            q = snap(q);
            cacheKey = mapKey(q);
            var hit = cache.get(cacheKey, BusinessMapResponse.class);
            if (hit.isPresent()) {
                return ResponseEntity.ok(hit.get());
            }
        }
        logger.info("Map search zoom={} limit={}", q.zoom(), q.limit());
        boolean clustered = q.zoom() < GeoSearchValidator.CLUSTER_BELOW_ZOOM;
        List<MapFeatureTO> features = new ArrayList<>();
        boolean truncated;
        if (clustered) {
            // 90 degrees at zoom 0, halved at each zoom level
            double cell = 90d / Math.pow(2, q.zoom());
            var rows = repository.findClusters(q.minLng(), q.minLat(), q.maxLng(), q.maxLat(), q.categoryId(), q.cityId(), q.text(),
                    cell, q.limit() + 1);
            truncated = rows.size() > q.limit();
            rows.stream().limit(q.limit()).forEach(r -> {
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("cluster", true);
                props.put("count", r.count());
                features.add(new MapFeatureTO("Feature", GeoPointTO.of(r.longitude(), r.latitude()), props));
            });
        } else {
            var rows = repository.findInBbox(q.minLng(), q.minLat(), q.maxLng(), q.maxLat(), q.categoryId(), q.cityId(), q.text(),
                    q.limit() + 1);
            truncated = rows.size() > q.limit();
            rows.stream().limit(q.limit()).forEach(r -> {
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("business_id", r.businessId());
                props.put("location_id", r.locationId());
                props.put("name", r.name());
                props.put("category_id", r.categoryId());
                props.put("verified", r.verified());
                props.put("main", r.main());
                features.add(new MapFeatureTO("Feature", GeoPointTO.of(r.longitude(), r.latitude()), props));
            });
        }
        var body = new BusinessMapResponse("FeatureCollection", features, truncated, clustered, q.zoom());
        if (cacheKey != null) {
            cache.put(cacheKey, body);
        }
        return ResponseEntity.ok(body);
    }

    /// Snaps the centre to the cache grid so that the cached answer is exactly the answer of the query that built the key.
    static NearbyQuery snap(NearbyQuery q) {
        return new NearbyQuery(snapCoordinate(q.lat()), snapCoordinate(q.lng()), q.radiusM(), q.categoryId(), q.cityId(), q.text(),
                q.sortByName(), q.page(), q.perPage());
    }

    /// Expands the bbox outwards to the cache grid of its zoom (clamped to the world), same reason as the nearby snap.
    static MapQuery snap(MapQuery q) {
        double step = mapStep(q.zoom());
        return new MapQuery(Math.max(-180d, Math.floor(q.minLng() / step) * step), Math.max(-90d, Math.floor(q.minLat() / step) * step),
                Math.min(180d, Math.ceil(q.maxLng() / step) * step), Math.min(90d, Math.ceil(q.maxLat() / step) * step), q.zoom(),
                q.categoryId(), q.cityId(), q.text(), q.limit());
    }

    private static double mapStep(int zoom) {
        return Math.max(MIN_MAP_STEP, 90d / Math.pow(2, zoom) / MAP_STEP_DIVISOR);
    }

    private static double snapCoordinate(double value) {
        return Math.round(Math.round(value / NEARBY_GRID) * NEARBY_GRID * 1_000_000d) / 1_000_000d;
    }

    /// Key of a nearby query: only the query, never the user. Free text and ids are hashed so the key has a fixed size.
    static String nearbyKey(NearbyQuery q) {
        return "umdc:directory:geo:nearby:v1:" + String.format(Locale.ROOT, "%.3f,%.3f", q.lat(), q.lng()) + ":" + q.radiusM() + ":"
                + (q.sortByName() ? "n" : "d") + ":" + q.page() + ":" + q.perPage() + ":" + filterHash(q.categoryId(), q.cityId(), q.text());
    }

    /// Key of a map query (see {@link #nearbyKey}); the bbox is expressed as grid indexes.
    static String mapKey(MapQuery q) {
        double step = mapStep(q.zoom());
        return "umdc:directory:geo:map:v1:" + q.zoom() + ":" + Math.round(q.minLng() / step) + "," + Math.round(q.minLat() / step) + ","
                + Math.round(q.maxLng() / step) + "," + Math.round(q.maxLat() / step) + ":" + q.limit() + ":"
                + filterHash(q.categoryId(), q.cityId(), q.text());
    }

    private static String filterHash(Object categoryId, Object cityId, String text) {
        String raw = categoryId + "|" + cityId + "|" + (text == null ? "" : text);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> ResponseEntity<T> badRequest(GeoSearchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, e.getMessage()).build();
    }

    private static String approx(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
