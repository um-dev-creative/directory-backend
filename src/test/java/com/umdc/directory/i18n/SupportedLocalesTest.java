package com.umdc.directory.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupportedLocalesTest {

    @Test
    void missingOrBlankHeaderIsEnglish() {
        assertEquals(SupportedLocales.EN, SupportedLocales.resolve(null));
        assertEquals(SupportedLocales.EN, SupportedLocales.resolve("  "));
    }

    @Test
    void supportedLanguages() {
        assertEquals(SupportedLocales.ES, SupportedLocales.resolve("es"));
        assertEquals(SupportedLocales.ES, SupportedLocales.resolve("es-MX"));
        assertEquals(SupportedLocales.EN, SupportedLocales.resolve("en-US"));
        assertEquals(SupportedLocales.FR_CA, SupportedLocales.resolve("fr-CA"));
        assertEquals(SupportedLocales.FR_CA, SupportedLocales.resolve("fr"));
        assertEquals(SupportedLocales.FR_CA, SupportedLocales.resolve("FR-fr"));
    }

    @Test
    void qualityOrderAndUnknownFallback() {
        assertEquals(SupportedLocales.ES, SupportedLocales.resolve("de, es;q=0.5"));
        assertEquals(SupportedLocales.FR_CA, SupportedLocales.resolve("en;q=0.3, fr-CA;q=0.9"));
        assertEquals(SupportedLocales.EN, SupportedLocales.resolve("de, ja;q=0.5"));
        assertEquals(SupportedLocales.EN, SupportedLocales.resolve("es;q=0"));
        assertEquals(SupportedLocales.EN, SupportedLocales.resolve("*"));
    }

    @Test
    void malformedHeaderIsEnglish() {
        assertEquals(SupportedLocales.EN, SupportedLocales.resolve("es;q=abc"));
    }

    @Test
    void normalizeAndTag() {
        assertEquals(SupportedLocales.EN, SupportedLocales.normalize(null));
        assertEquals(SupportedLocales.EN, SupportedLocales.normalize(Locale.GERMAN));
        assertEquals(SupportedLocales.FR_CA, SupportedLocales.normalize(Locale.FRANCE));
        assertEquals("fr-CA", SupportedLocales.tag(Locale.CANADA_FRENCH));
        assertEquals("es", SupportedLocales.tag(Locale.of("es", "ES")));
        assertEquals("en", SupportedLocales.tag(null));
        assertEquals(3, SupportedLocales.ALL.size());
    }

    @Test
    void utilityClassCannotBeInstantiated() throws Exception {
        var ctor = SupportedLocales.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var e = assertThrows(java.lang.reflect.InvocationTargetException.class, ctor::newInstance);
        assertEquals(IllegalStateException.class, e.getCause().getClass());
    }
}
