package com.umdc.directory.client.geocoding;

import com.umdc.directory.client.geocoding.to.MaptilerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/// Raw Feign call to a Maptiler-style geocoder. The URL comes from `umdc.geocoding.base-url`; use [GeocodingClient] instead.
@FeignClient(name = "geocodingClient", url = "${umdc.geocoding.base-url:https://api.maptiler.com}",
        configuration = GeocodingFeignConfiguration.class)
public interface MaptilerGeocodingFeign {

    @GetMapping("/geocoding/{query}.json")
    MaptilerResponse geocode(@PathVariable("query") String query, @RequestParam("key") String key,
                             @RequestParam("limit") int limit, @RequestParam("country") String country);
}
