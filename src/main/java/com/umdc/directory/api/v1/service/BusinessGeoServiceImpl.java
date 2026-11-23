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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.UseConcurrentHashMap"}) // text parameters on purpose; maps are local
public class BusinessGeoServiceImpl implements BusinessGeoService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessGeoServiceImpl.class);

    private final LocationGeoRepository repository;

    public BusinessGeoServiceImpl(LocationGeoRepository repository) {
        this.repository = repository;
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
        return ResponseEntity.ok(new BusinessNearbyResponse(total, q.page(), q.perPage(), totalPages, items));
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
        return ResponseEntity.ok(new BusinessMapResponse("FeatureCollection", features, truncated, clustered, q.zoom()));
    }

    private static <T> ResponseEntity<T> badRequest(GeoSearchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, e.getMessage()).build();
    }

    private static String approx(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
