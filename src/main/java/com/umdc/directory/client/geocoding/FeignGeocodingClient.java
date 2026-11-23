package com.umdc.directory.client.geocoding;

import com.umdc.directory.client.geocoding.to.GeocodingCandidate;
import com.umdc.directory.client.geocoding.to.MaptilerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InterruptedIOException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Objects;

/// [GeocodingClient] over [MaptilerGeocodingFeign]. Nothing is cached or stored. Failures are logged by class only: the exception
/// text of a Feign error carries the request URL, and the URL carries the API key.
@Component
public class FeignGeocodingClient implements GeocodingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeignGeocodingClient.class);

    private final MaptilerGeocodingFeign feign;
    private final GeocodingProperties properties;

    public FeignGeocodingClient(MaptilerGeocodingFeign feign, GeocodingProperties properties) {
        this.feign = feign;
        this.properties = properties;
    }

    @Override
    public List<GeocodingCandidate> search(String query, String country, int limit) {
        MaptilerResponse response;
        try {
            response = feign.geocode(sanitize(query), properties.getApiKey(), limit, country);
        } catch (RuntimeException e) {
            var kind = isTimeout(e) ? GeocodingException.Kind.TIMEOUT : GeocodingException.Kind.UNAVAILABLE;
            LOGGER.warn("Geocoding provider call failed: {} ({})", kind, e.getClass().getSimpleName());
            throw new GeocodingException(kind); // NOPMD PreserveStackTrace: the cause text carries the URL and the API key
        }
        if (Objects.isNull(response) || Objects.isNull(response.features())) {
            return List.of();
        }
        return response.features().stream()
                .filter(f -> Objects.nonNull(f) && Objects.nonNull(f.center()) && f.center().size() >= 2
                        && Objects.nonNull(f.center().get(0)) && Objects.nonNull(f.center().get(1)))
                .map(f -> new GeocodingCandidate(f.placeName(), f.center().get(1), f.center().get(0), f.relevance()))
                .toList();
    }

    /// Characters that would change the path of the provider URL.
    static String sanitize(String query) {
        return query.replaceAll("[/?#;\\\\]", " ").trim();
    }

    private static boolean isTimeout(Throwable error) {
        int depth = 0;
        for (Throwable t = error; t != null && depth < 10; t = t.getCause()) {
            if (t instanceof InterruptedIOException || t instanceof HttpTimeoutException) {
                return true;
            }
            depth++;
        }
        return false;
    }
}
