package com.prx.directory.jpa.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DurationToStringConverterTest {

    private final TimezoneEntity.DurationToStringConverter converter = new TimezoneEntity.DurationToStringConverter();

    @Test
    @DisplayName("Duration converter: null duration to database returns null")
    void convertToDatabaseColumn_nullDuration_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    @DisplayName("Duration converter: valid duration to database returns ISO string")
    void convertToDatabaseColumn_validDuration_returnsIsoString() {
        Duration duration = Duration.ofHours(5).plusMinutes(30);
        String result = converter.convertToDatabaseColumn(duration);
        assertEquals("PT5H30M", result);
    }

    @Test
    @DisplayName("Duration converter: negative duration to database returns ISO string")
    void convertToDatabaseColumn_negativeDuration_returnsIsoString() {
        Duration duration = Duration.ofHours(-3);
        String result = converter.convertToDatabaseColumn(duration);
        assertEquals("PT-3H", result);
    }

    @Test
    @DisplayName("Duration converter: null db string to entity returns null")
    void convertToEntityAttribute_nullString_returnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    @DisplayName("Duration converter: valid ISO string to entity returns duration")
    void convertToEntityAttribute_validIsoString_returnsDuration() {
        String dbData = "PT2H30M";
        Duration result = converter.convertToEntityAttribute(dbData);
        assertNotNull(result);
        assertEquals(Duration.ofHours(2).plusMinutes(30), result);
    }

    @Test
    @DisplayName("Duration converter: negative ISO string to entity returns negative duration")
    void convertToEntityAttribute_negativeIsoString_returnsDuration() {
        String dbData = "PT-5H";
        Duration result = converter.convertToEntityAttribute(dbData);
        assertNotNull(result);
        assertEquals(Duration.ofHours(-5), result);
    }

    @Test
    @DisplayName("Duration converter: HH:MM:SS format to entity returns duration")
    void convertToEntityAttribute_hhMmSsFormat_returnsDuration() {
        String dbData = "05:30:00";
        Duration result = converter.convertToEntityAttribute(dbData);
        assertNotNull(result);
        assertEquals(Duration.ofHours(5).plusMinutes(30), result);
    }

    @Test
    @DisplayName("Duration converter: negative HH:MM:SS format to entity returns negative duration")
    void convertToEntityAttribute_negativeHhMmSsFormat_returnsDuration() {
        String dbData = "-03:00:00";
        Duration result = converter.convertToEntityAttribute(dbData);
        assertNotNull(result);
        assertEquals(Duration.ofHours(-3), result);
    }

    @Test
    @DisplayName("Duration converter: invalid format throws IllegalArgumentException")
    void convertToEntityAttribute_invalidFormat_throwsException() {
        String dbData = "invalid-duration";
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute(dbData));
    }

    @Test
    @DisplayName("Duration converter: too few parts throws Exception")
    void convertToEntityAttribute_tooFewParts_throwsException() {
        String dbData = "05:30"; // only two parts
        assertThrows(Exception.class, () -> converter.convertToEntityAttribute(dbData));
    }

    @Test
    @DisplayName("Duration converter: non-numeric parts throw Exception")
    void convertToEntityAttribute_nonNumericParts_throwsException() {
        String dbData = "aa:bb:cc";
        assertThrows(Exception.class, () -> converter.convertToEntityAttribute(dbData));
    }
}
