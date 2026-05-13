package com.hamradio.jsat.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Internationalisation helper for j-sat.
 *
 * English + Spanish ship embedded at
 *   resources/com/hamradio/jsat/i18n/messages_<lang>.properties
 * Additional packs (de / fr / it / pt) load from
 *   ~/.j-hub/lang/j-sat/messages_<lang>.properties
 */
public class I18n {

    private static final Logger log = LoggerFactory.getLogger(I18n.class);
    private static final String BUNDLE_BASE = "com.hamradio.jsat.i18n.messages";
    private static final File PACK_DIR =
            new File(System.getProperty("user.home"), ".j-hub/lang/j-sat");

    private static ResourceBundle bundle;
    private static Locale currentLocale = Locale.ENGLISH;

    public static void load(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) languageCode = "en";
        currentLocale = Locale.forLanguageTag(languageCode);
        try {
            ClassLoader loader = externalPackLoader();
            bundle = (loader != null)
                ? ResourceBundle.getBundle(BUNDLE_BASE, currentLocale, loader)
                : ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
            log.info("j-sat language bundle: {}", languageCode);
        } catch (MissingResourceException ex) {
            log.warn("j-sat language '{}' not found, falling back to English", languageCode);
            bundle = ResourceBundle.getBundle(BUNDLE_BASE, Locale.ENGLISH);
        }
    }

    private static ClassLoader externalPackLoader() {
        if (!PACK_DIR.isDirectory()) return null;
        try {
            URL url = PACK_DIR.toURI().toURL();
            return new URLClassLoader(new URL[]{url}, I18n.class.getClassLoader());
        } catch (Exception ex) {
            log.warn("Could not open external pack dir {}: {}", PACK_DIR, ex.getMessage());
            return null;
        }
    }

    public static String get(String key) {
        if (bundle == null) return key;
        try { return bundle.getString(key); }
        catch (MissingResourceException ex) { return key; }
    }

    public static String get(String key, Object... args) {
        return String.format(get(key), args);
    }

    public static Locale getCurrentLocale() { return currentLocale; }
}
