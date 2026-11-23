package com.umdc.directory.api.v1.to;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoToTest {

    @Test
    void aPointIsRoundedToFiveDecimals() {
        var point = GeoPointTO.of(-79.3831876, 43.6532194);
        assertEquals("Point", point.type());
        assertEquals(List.of(-79.38319, 43.65322), point.coordinates());
    }
}
