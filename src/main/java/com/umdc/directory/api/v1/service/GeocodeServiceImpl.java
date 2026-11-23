package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.GeocodeRequest;
import com.umdc.directory.api.v1.to.GeocodeResponse;
import com.umdc.directory.client.geocoding.GeocodingClient;
import com.umdc.directory.client.geocoding.GeocodingException;
import com.umdc.directory.client.geocoding.GeocodingProperties;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

/// Validates the request, calls the provider and returns up to 5 candidates. The address is never logged, cached or stored.
@Service
public class GeocodeServiceImpl implements GeocodeService {

    static final int MAX_FIELD = 200;
    static final int MAX_CANDIDATES = 5;
    static final String DEFAULT_COUNTRY = "ca";

    static final String DISABLED = "Geocoding is not enabled";
    static final String ADDRESS_REQUIRED = "address is required";
    static final String FIELD_TOO_LONG = "address, city and country must have at most 200 characters";
    static final String COUNTRY_INVALID = "country must be an ISO 3166-1 alpha-2 code";
    static final String PROVIDER_TIMEOUT = "Geocoding provider timed out";
    static final String PROVIDER_FAILED = "Geocoding provider failed";

    private final GeocodingClient client;
    private final GeocodingProperties properties;

    public GeocodeServiceImpl(GeocodingClient client, GeocodingProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public ResponseEntity<GeocodeResponse> geocode(GeocodeRequest request, String token) {
        if (Objects.isNull(JwtUtil.getUidFromToken(token))) {
            return error(HttpStatus.UNAUTHORIZED, DirectoryAppConstants.UNAUTHORIZED_MESSAGE);
        }
        if (Objects.isNull(request) || Objects.isNull(request.address()) || request.address().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, ADDRESS_REQUIRED);
        }
        if (tooLong(request.address()) || tooLong(request.city()) || tooLong(request.country())) {
            return error(HttpStatus.BAD_REQUEST, FIELD_TOO_LONG);
        }
        var country = Objects.isNull(request.country()) || request.country().isBlank()
                ? DEFAULT_COUNTRY : request.country().trim().toLowerCase(java.util.Locale.ROOT);
        if (!country.matches("[a-z]{2}")) {
            return error(HttpStatus.BAD_REQUEST, COUNTRY_INVALID);
        }
        if (!properties.isEnabled()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, DISABLED);
        }
        var query = request.address().trim()
                + (Objects.isNull(request.city()) || request.city().isBlank() ? "" : ", " + request.city().trim());
        try {
            var candidates = client.search(query, country, Math.min(MAX_CANDIDATES, properties.effectiveLimit())).stream()
                    .filter(c -> validRange(c.latitude(), 90) && validRange(c.longitude(), 180))
                    .limit(MAX_CANDIDATES)
                    .map(c -> new GeocodeResponse.Candidate(c.label(), c.latitude(), c.longitude(), c.confidence()))
                    .toList();
            return ResponseEntity.ok(new GeocodeResponse(candidates));
        } catch (GeocodingException e) {
            return e.getKind() == GeocodingException.Kind.TIMEOUT
                    ? error(HttpStatus.GATEWAY_TIMEOUT, PROVIDER_TIMEOUT)
                    : error(HttpStatus.BAD_GATEWAY, PROVIDER_FAILED);
        }
    }

    private static boolean tooLong(String value) {
        return Objects.nonNull(value) && value.trim().length() > MAX_FIELD;
    }

    private static boolean validRange(double value, double max) {
        return !Double.isNaN(value) && Math.abs(value) <= max;
    }

    private static ResponseEntity<GeocodeResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, message).build();
    }
}
