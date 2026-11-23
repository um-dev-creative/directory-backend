package com.umdc.directory.api.v1.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoSearchValidatorTest {

    private static String nearbyError(String lat, String lng, String radius, String cat, String city, String q, String sort, String page, String perPage) {
        return assertThrows(GeoSearchException.class, () -> GeoSearchValidator.nearby(lat, lng, radius, cat, city, q, sort, page, perPage)).getMessage();
    }

    private static String mapError(String bbox, String zoom, String cat, String city, String q, String limit) {
        return assertThrows(GeoSearchException.class, () -> GeoSearchValidator.map(bbox, zoom, cat, city, q, limit)).getMessage();
    }

    @Test
    void nearbyDefaults() {
        var q = GeoSearchValidator.nearby("43.65", "-79.38", null, null, null, null, null, null, null);
        assertEquals(5000, q.radiusM());
        assertFalse(q.sortByName());
        assertEquals(1, q.page());
        assertEquals(10, q.perPage());
        assertNull(q.text());
        assertNull(q.categoryId());
    }

    @Test
    void nearbyFullParameters() {
        var cat = UUID.randomUUID();
        var city = UUID.randomUUID();
        var q = GeoSearchValidator.nearby(" 90 ", "-180", "50000", cat.toString(), city.toString(), " pan ", "NAME", "2", "100");
        assertEquals(90d, q.lat());
        assertEquals(-180d, q.lng());
        assertEquals(50000, q.radiusM());
        assertEquals(cat, q.categoryId());
        assertEquals(city, q.cityId());
        assertEquals("pan", q.text());
        assertTrue(q.sortByName());
        assertEquals(2, q.page());
        assertEquals(100, q.perPage());
        assertFalse(GeoSearchValidator.nearby("0", "0", "", "", "", " ", "distance", "", "").sortByName());
    }

    @Test
    void nearbyRejectsInvalidValues() {
        assertEquals("lat is required", nearbyError(null, "0", null, null, null, null, null, null, null));
        assertEquals("lng is required", nearbyError("0", " ", null, null, null, null, null, null, null));
        assertEquals("lat must be a number", nearbyError("abc", "0", null, null, null, null, null, null, null));
        assertEquals("lat must be a number".replace("lat", "lng"), nearbyError("0", "x", null, null, null, null, null, null, null));
        assertTrue(nearbyError("90.1", "0", null, null, null, null, null, null, null).startsWith("lat must be between"));
        assertTrue(nearbyError("-90.1", "0", null, null, null, null, null, null, null).startsWith("lat must be between"));
        assertTrue(nearbyError("0", "180.1", null, null, null, null, null, null, null).startsWith("lng must be between"));
        assertTrue(nearbyError("NaN", "0", null, null, null, null, null, null, null).startsWith("lat must be between"));
        assertTrue(nearbyError("0", "Infinity", null, null, null, null, null, null, null).startsWith("lng must be between"));
        assertTrue(nearbyError("0", "0", "0", null, null, null, null, null, null).startsWith("radius_m must be between"));
        assertTrue(nearbyError("0", "0", "-5", null, null, null, null, null, null).startsWith("radius_m must be between"));
        assertTrue(nearbyError("0", "0", "50001", null, null, null, null, null, null).startsWith("radius_m must be between"));
        assertEquals("radius_m must be an integer", nearbyError("0", "0", "1.5", null, null, null, null, null, null));
        assertEquals("sort must be distance or name", nearbyError("0", "0", null, null, null, null, "rating", null, null));
        assertEquals("category_id must be a UUID", nearbyError("0", "0", null, "x", null, null, null, null, null));
        assertEquals("city_id must be a UUID", nearbyError("0", "0", null, null, "x", null, null, null, null));
        assertTrue(nearbyError("0", "0", null, null, null, "a".repeat(101), null, null, null).startsWith("q must have"));
        assertTrue(nearbyError("0", "0", null, null, null, null, null, "0", null).contains("page"));
        assertTrue(nearbyError("0", "0", null, null, null, null, null, "1", "101").contains("per_page"));
        assertTrue(nearbyError("0", "0", null, null, null, null, null, "1", "0").contains("per_page"));
    }

    @Test
    void mapDefaultsAndFullParameters() {
        var q = GeoSearchValidator.map("-80,43,-79,44", null, null, null, null, null);
        assertEquals(14, q.zoom());
        assertEquals(200, q.limit());
        assertEquals(-80d, q.minLng());
        assertEquals(44d, q.maxLat());
        var cat = UUID.randomUUID();
        var full = GeoSearchValidator.map("-80,43,-79,44", "22", cat.toString(), UUID.randomUUID().toString(), "x", "500");
        assertEquals(22, full.zoom());
        assertEquals(500, full.limit());
        assertEquals(cat, full.categoryId());
        assertEquals(0, GeoSearchValidator.map("-80,43,-79,44", "0", null, null, null, "1").zoom());
    }

    @Test
    void mapRejectsInvalidBbox() {
        assertTrue(mapError(null, null, null, null, null, null).startsWith("bbox is required"));
        assertTrue(mapError(" ", null, null, null, null, null).startsWith("bbox is required"));
        assertTrue(mapError("1,2,3", null, null, null, null, null).startsWith("bbox must be"));
        assertTrue(mapError("1,2,3,4,5", null, null, null, null, null).startsWith("bbox must be"));
        assertEquals("bbox minLng must be a number", mapError("a,2,3,4", null, null, null, null, null));
        assertTrue(mapError("-181,0,3,4", null, null, null, null, null).startsWith("bbox minLng must be between"));
        assertTrue(mapError("0,-91,3,4", null, null, null, null, null).startsWith("bbox minLat must be between"));
        assertTrue(mapError("-79,43,-80,44", null, null, null, null, null).startsWith("bbox is inverted"));
        assertTrue(mapError("-80,44,-79,43", null, null, null, null, null).startsWith("bbox is inverted"));
        assertTrue(mapError("-80,43,-80,44", null, null, null, null, null).startsWith("bbox is inverted"));
        assertTrue(mapError("-100,0,0,10", null, null, null, null, null).startsWith("bbox is too large"));
        assertTrue(mapError("0,-50,10,50", null, null, null, null, null).startsWith("bbox is too large"));
    }

    @Test
    void mapRejectsInvalidOtherParameters() {
        assertTrue(mapError("-80,43,-79,44", "23", null, null, null, null).startsWith("zoom must be"));
        assertTrue(mapError("-80,43,-79,44", "-1", null, null, null, null).startsWith("zoom must be"));
        assertEquals("zoom must be an integer", mapError("-80,43,-79,44", "z", null, null, null, null));
        assertTrue(mapError("-80,43,-79,44", null, null, null, null, "0").startsWith("limit must be"));
        assertTrue(mapError("-80,43,-79,44", null, null, null, null, "501").startsWith("limit must be"));
        assertEquals("category_id must be a UUID", mapError("-80,43,-79,44", null, "x", null, null, null));
    }

    @Test
    void theUtilityClassCannotBeInstantiated() throws Exception {
        var ctor = GeoSearchValidator.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var e = assertThrows(java.lang.reflect.InvocationTargetException.class, ctor::newInstance);
        assertTrue(e.getCause() instanceof IllegalStateException);
    }
}
