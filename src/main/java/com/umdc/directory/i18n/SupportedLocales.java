package com.umdc.directory.i18n;

import java.util.List;
import java.util.Locale;

/// Locales the API answers in and the resolution of the `Accept-Language` header to one of them.
///
/// Supported: English (`en`, the default), Spanish (`es`) and Canadian French (`fr-CA`). Any `fr-*` tag (and bare `fr`)
/// resolves to `fr-CA`. Unknown languages are skipped, so `de, es;q=0.5` gives `es`; nothing usable gives `en`.
public final class SupportedLocales {

    /// English, the default and the language of the source messages.
    public static final Locale EN = Locale.ENGLISH;
    /// Spanish.
    public static final Locale ES = Locale.of("es");
    /// Canadian French.
    public static final Locale FR_CA = Locale.of("fr", "CA");
    /// Locale used when the header is missing or has nothing supported.
    public static final Locale DEFAULT = EN;
    /// Request attribute holding the resolved {@link Locale}.
    public static final String REQUEST_ATTRIBUTE = SupportedLocales.class.getName() + ".LOCALE";
    /// Every supported locale.
    public static final List<Locale> ALL = List.of(EN, ES, FR_CA);

    private SupportedLocales() {
        throw new IllegalStateException("Utility class");
    }

    /// Picks the first supported language of an `Accept-Language` value, honouring quality values (highest first, `q=0`
    /// means not acceptable).
    ///
    /// @param acceptLanguage raw header value, may be null or malformed
    /// @return `en`, `es` or `fr-CA`; never null
    public static Locale resolve(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return DEFAULT;
        }
        List<Locale.LanguageRange> ranges;
        try {
            ranges = Locale.LanguageRange.parse(acceptLanguage);
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
        for (Locale.LanguageRange range : ranges) {
            if (range.getWeight() <= 0) {
                continue;
            }
            Locale match = fromLanguage(range.getRange());
            if (match != null) {
                return match;
            }
        }
        return DEFAULT;
    }

    /// Maps any locale (for example the one Spring keeps in its context) to a supported one.
    ///
    /// @param locale any locale, may be null
    /// @return the supported locale for its language, or the default
    public static Locale normalize(Locale locale) {
        if (locale == null) {
            return DEFAULT;
        }
        Locale match = fromLanguage(locale.getLanguage());
        return match == null ? DEFAULT : match;
    }

    /// Language tag of a supported locale, as sent in `Content-Language` (`en`, `es`, `fr-CA`).
    ///
    /// @param locale a locale, normalised first
    /// @return the BCP 47 tag
    public static String tag(Locale locale) {
        return normalize(locale).toLanguageTag();
    }

    private static Locale fromLanguage(String range) {
        String language = range.toLowerCase(Locale.ROOT);
        int dash = language.indexOf('-');
        if (dash > 0) {
            language = language.substring(0, dash);
        }
        return switch (language) {
            case "en" -> EN;
            case "es" -> ES;
            case "fr" -> FR_CA;
            default -> null;
        };
    }
}
