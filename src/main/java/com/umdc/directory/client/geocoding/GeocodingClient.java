package com.umdc.directory.client.geocoding;

import com.umdc.directory.client.geocoding.to.GeocodingCandidate;

import java.util.List;

/// Port to a geocoding provider. Implementations must not cache or persist results (provider terms); they only return candidates.
public interface GeocodingClient {

    /// @param query   free-text address
    /// @param country ISO 3166-1 alpha-2 country filter, lower case
    /// @param limit   maximum candidates
    /// @return the candidates, possibly empty
    /// @throws GeocodingException on timeout or provider failure; its message never carries the key or the query
    List<GeocodingCandidate> search(String query, String country, int limit);
}
