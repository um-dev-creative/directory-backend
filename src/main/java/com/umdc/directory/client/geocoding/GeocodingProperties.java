package com.umdc.directory.client.geocoding;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/// `umdc.geocoding.*`. Disabled by default; the API key is never in the repository: set `UMDC_GEOCODING_API_KEY` (or Vault).
@Component
@ConfigurationProperties(prefix = "umdc.geocoding")
public class GeocodingProperties {

    /// Provider switch. While `false`, `POST /api/v1/geocode` answers 503.
    private boolean enabled = false;
    /// Base URL of a Maptiler-style geocoding API.
    private String baseUrl = "https://api.maptiler.com";
    /// Provider API key. Empty by default.
    private String apiKey = "";
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;
    /// Candidates returned at most (hard cap 5).
    private int maxCandidates = 5;

    public GeocodingProperties() {
        // bound by Spring from umdc.geocoding.*
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    /// Candidates to ask for: between 1 and 5.
    public int effectiveLimit() {
        return Math.max(1, Math.min(5, maxCandidates));
    }
}
