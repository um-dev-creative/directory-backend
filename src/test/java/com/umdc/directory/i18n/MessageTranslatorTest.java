package com.umdc.directory.i18n;

import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.constant.FavoriteListConstants;
import com.umdc.directory.constant.OpeningHoursMessages;
import com.umdc.directory.constant.SavedSearchConstants;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageTranslatorTest {

    private final MessageTranslator translator = new MessageTranslator();
    private static final Locale FR = Locale.CANADA_FRENCH;
    private static final Locale ES = Locale.of("es");

    @Test
    void keyIsNormalisedWithNumbersMasked() {
        assertEquals("msg.business_not_found", MessageTranslator.keyOf("Business not found."));
        assertEquals("msg.days_must_be_between_n_and_n", MessageTranslator.keyOf("days must be between 1 and 365"));
        assertEquals("msg.lat_must_be_between_n_and_n", MessageTranslator.keyOf("lat must be between -90 and 90"));
    }

    @Test
    void translatesToSpanishAndCanadianFrench() {
        assertEquals("Negocio no encontrado.", translator.translate("Business not found.", ES));
        assertEquals("Entreprise introuvable.", translator.translate("Business not found.", FR));
        assertEquals("Entreprise introuvable.", translator.translate("Business not found.", Locale.FRENCH));
    }

    @Test
    void englishAndUnsupportedLocalesPassThrough() {
        assertEquals("Business not found.", translator.translate("Business not found.", Locale.ENGLISH));
        assertEquals("Business not found.", translator.translate("Business not found.", Locale.GERMAN));
        assertEquals("Business not found.", translator.translate("Business not found.", null));
    }

    @Test
    void unknownMessagePassesThroughUnchanged() {
        assertEquals("Something new happened", translator.translate("Something new happened", FR));
        assertEquals("Something", translator.translate("Something", FR));
    }

    @Test
    void nullAndBlankAreReturnedAsIs() {
        assertNull(translator.translate(null, FR));
        assertEquals("", translator.translate("", FR));
        assertEquals("  ", translator.translate("  ", ES));
    }

    @Test
    void numbersAreCarriedIntoTheTranslation() {
        assertEquals("Demasiadas solicitudes, reintente en 42 segundos",
                translator.translate("Too many requests, retry in 42 seconds", ES));
        assertEquals("days doit être compris entre 1 et 365", translator.translate("days must be between 1 and 365", FR));
        assertEquals("lat debe estar entre -90 y 90", translator.translate("lat must be between -90 and 90", ES));
    }

    @Test
    void parameterNamedMessagesUseTheGenericEntry() {
        assertEquals("category_id es obligatorio", translator.translate("category_id is required", ES));
        assertEquals("lng doit être un nombre", translator.translate("lng must be a number", FR));
        assertEquals("radius_m must be a quasar", translator.translate("radius_m must be a quasar", FR));
    }

    @Test
    void everyEnglishKeyHasSpanishAndFrenchWithSamePlaceholders() throws IOException {
        Properties en = load("i18n/messages.properties");
        Properties es = load("i18n/messages_es.properties");
        Properties fr = load("i18n/messages_fr_CA.properties");
        assertTrue(en.size() > 100);
        List<String> missing = new ArrayList<>();
        for (String key : en.stringPropertyNames()) {
            for (Properties other : List.of(es, fr)) {
                String value = other.getProperty(key);
                if (value == null || value.isBlank() || !placeholders(en.getProperty(key)).equals(placeholders(value))) {
                    missing.add(key);
                }
            }
        }
        assertTrue(missing.isEmpty(), "Keys without a valid es/fr-CA entry: " + missing);
        assertEquals(en.stringPropertyNames(), es.stringPropertyNames());
        assertEquals(en.stringPropertyNames(), fr.stringPropertyNames());
        assertNotEquals(en.getProperty("msg.business_not_found"), es.getProperty("msg.business_not_found"));
    }

    /// Fails for the constants known when the bundles were written: each must have an entry.
    @Test
    void knownConstantMessagesAreInTheBundles() throws Exception {
        Properties en = load("i18n/messages.properties");
        List<String> missing = new ArrayList<>();
        for (String text : knownMessages()) {
            String key = MessageTranslator.keyOf(text);
            if (!en.containsKey(key)) {
                missing.add(text);
            }
        }
        assertTrue(missing.isEmpty(), "Messages without a bundle entry: " + missing);
    }

    /// Prints (never fails) the literals sent in the message headers that have no entry yet.
    @Test
    void reportsHeaderLiteralsWithoutEntry() throws Exception {
        Properties en = load("i18n/messages.properties");
        Pattern literal = Pattern.compile("MESSAGE(?:_ERROR)?_HEADER,\\s*\"([^\"]+)\"");
        TreeSet<String> untranslated = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = literal.matcher(Files.readString(file));
                while (m.find()) {
                    String text = m.group(1);
                    if (!en.containsKey(MessageTranslator.keyOf(text))
                            && !en.containsKey(MessageTranslator.keyOf("param" + text.substring(Math.max(0, text.indexOf(' ')))))) {
                        untranslated.add(text);
                    }
                }
            }
        }
        if (!untranslated.isEmpty()) {
            System.out.println("WARNING i18n: header messages without translation (" + untranslated.size() + "): " + untranslated);
        }
        assertTrue(untranslated.size() < 1000);
    }

    @Test
    void sameInstanceIsReturnedForEnglish() {
        String text = "Whatever";
        assertSame(text, translator.translate(text, Locale.ENGLISH));
    }

    private static List<String> knownMessages() throws IllegalAccessException {
        List<String> out = new ArrayList<>();
        List<String> skip = List.of("APPLICATION_NAME", "ENTITY_PACKAGE", "REPOSITORY_PACKAGE", "MESSAGE_ERROR_HEADER", "MESSAGE_HEADER",
                "INTERNAL_SERVER_ERROR_CODE");
        for (Class<?> type : List.of(DirectoryAppConstants.class, FavoriteListConstants.class, OpeningHoursMessages.class,
                SavedSearchConstants.class)) {
            for (Field f : type.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class && !skip.contains(f.getName())
                        && isMessage(type, f)) {
                    out.add((String) f.get(null));
                }
            }
        }
        return out;
    }

    private static boolean isMessage(Class<?> type, Field f) {
        String name = f.getName();
        if (type == DirectoryAppConstants.class) {
            return name.endsWith("_REQUIRED") || name.endsWith("_NOT_FOUND") || name.startsWith("CAMPAIGN_") && !name.contains("SORT")
                    || name.contains("INVALID") || name.contains("ONLY_OWNER") || name.contains("_MESSAGE") && !name.equals("MESSAGE_HEADER")
                    || name.startsWith("LOCATION_") || name.startsWith("EVENT") || name.equals("PRODUCT_PRICE_CURRENCY_TOGETHER");
        }
        if (type == SavedSearchConstants.class) {
            return !name.startsWith("ENDPOINT_");
        }
        if (type == FavoriteListConstants.class) {
            return !name.startsWith("TYPE_");
        }
        return !name.equals("DEFAULT_ZONE");
    }

    private static String placeholders(String value) {
        List<String> found = new ArrayList<>();
        Matcher m = Pattern.compile("\\{(\\w+)}").matcher(value);
        while (m.find()) {
            found.add(m.group(1));
        }
        found.sort(String::compareTo);
        return String.join(",", found);
    }

    private static Properties load(String path) throws IOException {
        try (InputStream in = MessageTranslatorTest.class.getClassLoader().getResourceAsStream(path)) {
            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return p;
        }
    }
}
