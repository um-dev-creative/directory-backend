package com.umdc.directory.i18n;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Translates the English text of the `message` and `message-error` headers with the bundles under `i18n/messages*.properties`.
///
/// The key of a text is `msg.` plus its normalised form (see {@link #keyOf}): lower case, every number replaced by `n`, every
/// other run of non-alphanumeric characters replaced by `_`. The numbers found go to the translation as `{0}`, `{1}`, ...
/// A text of the form `<parameter> <rest>` with no entry of its own is looked up as `param <rest>` and `{name}` is replaced by
/// the parameter. A text with no entry passes through unchanged, so a new message never breaks a response.
@Component
public class MessageTranslator {

    private static final String BUNDLE = "i18n/messages";
    private static final String PREFIX = "msg.";
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9#]+");
    private static final ResourceBundle.Control CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    /// Default constructor.
    public MessageTranslator() {
        // no state
    }

    /// Key of an English message in the bundles.
    ///
    /// @param text English text
    /// @return `msg.` plus the normalised text
    public static String keyOf(String text) {
        String numbered = NUMBER.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("#");
        String flat = NON_ALNUM.matcher(numbered).replaceAll("_");
        flat = flat.replaceAll("^_+|_+$", "").replace('#', 'n');
        return PREFIX + flat;
    }

    /// Translates a message.
    ///
    /// @param text   English message, may be null
    /// @param locale target locale (normalised to a supported one)
    /// @return the translation, or `text` itself when English, blank, or unknown
    public String translate(String text, Locale locale) {
        if (text == null || text.isBlank()) {
            return text;
        }
        Locale target = SupportedLocales.normalize(locale);
        if (SupportedLocales.EN.equals(target)) {
            return text;
        }
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle(BUNDLE, target, CONTROL);
        } catch (MissingResourceException e) {
            return text;
        }
        List<String> numbers = new ArrayList<>();
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        String exact = lookup(bundle, keyOf(text));
        if (exact != null) {
            return fill(exact, numbers, null);
        }
        int space = text.indexOf(' ');
        if (space > 0) {
            String generic = lookup(bundle, keyOf("param" + text.substring(space)));
            if (generic != null) {
                return fill(generic, numbers, text.substring(0, space));
            }
        }
        return text;
    }

    private static String lookup(ResourceBundle bundle, String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : null;
    }

    private static String fill(String template, List<String> numbers, String name) {
        String result = template;
        for (int i = 0; i < numbers.size(); i++) {
            result = result.replace("{" + i + "}", numbers.get(i));
        }
        return name == null ? result : result.replace("{name}", name);
    }
}
