package com.jlog.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Internationalisation helper.
 *
 * Strings are stored in:
 *   resources/com/jlog/i18n/messages_<lang>.properties
 *
 * Usage:
 *   I18n.get("label.callsign")
 *   I18n.get("msg.saved", count)
 */
public class I18n {

    private static final Logger log = LoggerFactory.getLogger(I18n.class);
    private static ResourceBundle bundle;
    private static Locale currentLocale = Locale.ENGLISH;

    public static void load(String languageCode) {
        try {
            currentLocale = Locale.forLanguageTag(languageCode);
            bundle = ResourceBundle.getBundle(
                "com.jlog.i18n.messages", currentLocale);
            log.info("Loaded language bundle: {}", languageCode);
        } catch (MissingResourceException ex) {
            log.warn("Language bundle not found for '{}', falling back to English", languageCode);
            bundle = ResourceBundle.getBundle(
                "com.jlog.i18n.messages", Locale.ENGLISH);
        }
    }

    public static String get(String key) {
        if (bundle == null) return key;
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            log.warn("Missing i18n key: {}", key);
            return key;
        }
    }

    public static String get(String key, Object... args) {
        return String.format(get(key), args);
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }
}
