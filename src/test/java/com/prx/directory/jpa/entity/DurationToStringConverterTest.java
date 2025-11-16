package com.prx.directory.jpa.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DurationToStringConverterTest {

    private final TimezoneEntity.DurationToStringConverter converter = new TimezoneEntity.DurationToStringConverter();

    @Test
    void convertToDatabaseColumn_nullDuration_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_validDuration_returnsIsoString() {
        Duration duration = Duration.ofHours(5).plusMinutes(30);
        String result = converter.convertToDatabaseColumn(duration);
        assertEquals("PT5H30M", result);
    }

    @Test
    void convertToDatabaseColumn_negativeDuration_returnsIsoString() {
        Duration duration = Duration.ofHours(-3);
        String result = converter.convertToDatabaseColumn(duration);
        assertEquals("PT-3H", result);
    }

    @Test
    void convertToEntityAttribute_nullString_returnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttribute_validIsoString_returnsDuration() {
        String dbData = "PT2H30M";
        Duration result = converter.convertToEntityAttribute(dbData);
        assertNotNull(result);
        assertEquals(Duration.ofHours(2).plusMinutes(30), result);
    }

    @Test
    void convertToEntityAttribute_negativeIsoString_returnsDuration() {
        String dbData = "PT-5H";
        Duration result = converter.convertToEntityAttribute(dbData);
        assertNotNull(result);
        assertEquals(Duration.ofHours(-5), result);
    }

    @Test
    void convertToEntityAttribute_hhMmSsFormat_returnsDuration() {
        String dbData = "05:30:00";
        Duration result = converter.convertToEntityAttribute(dbData);
        assertNotNull(result);
        assertEquals(Duration.ofHours(5).plusMinutes(30), result);
    }

    @Test
    void convertToEntityAttribute_negativeHhMmSsFormat_returnsDuration() {
        String dbData = "-03:00:00";
        Duration result = converter.convertToEntityAttribute(dbData);
        assertNotNull(result);
        assertEquals(Duration.ofHours(-3), result);
    }

    @Test
    void convertToEntityAttribute_hhMmSsWithSeconds_returnsDuration() {
        String dbData = "01:15:45";
        Duration result = converter.convertToEntityAttribute(dbData);
        assertNotNull(result);
        assertEquals(Duration.ofHours(1).plusMinutes(15).plusSeconds(45), result);
    }

    @Test
    void convertToEntityAttribute_invalidFormat_throwsException() {
        String dbData = "invalid-duration";
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute(dbData));
    }

    @Test
    void convertToEntityAttribute_tooFewParts_throwsException() {
        String dbData = "05:30"; // only two parts
        assertThrows(Exception.class, () -> converter.convertToEntityAttribute(dbData));
    }

    @Test
    void convertToEntityAttribute_nonNumericParts_throwsException() {
        String dbData = "aa:bb:cc";
        assertThrows(Exception.class, () -> converter.convertToEntityAttribute(dbData));
    }
}

