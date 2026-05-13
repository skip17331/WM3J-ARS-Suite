package com.morsetrainer.i18n;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internationalisation helper for morse-trainer.
 *
 * English + Spanish ship embedded at
 *   resources/com/morsetrainer/i18n/messages_<lang>.properties
 * Additional packs (de / fr / it / pt) load from
 *   ~/.j-hub/lang/morse-trainer/messages_<lang>.properties
 *
 * morse-trainer is standalone (no j-hub messages). Language is taken from
 * ~/.morse-trainer/config.json (key "language") or "en" if absent.
 */
public final class I18n {

    private static final Logger LOG = Logger.getLogger(I18n.class.getName());
    private static final String BUNDLE_BASE = "com.morsetrainer.i18n.messages";
    private static final File PACK_DIR =
            new File(System.getProperty("user.home"), ".j-hub/lang/morse-trainer");

    private static ResourceBundle bundle;
    private static Locale currentLocale = Locale.ENGLISH;

    private I18n() {}

    public static void load(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) languageCode = "en";
        currentLocale = Locale.forLanguageTag(languageCode);
        try {
            ClassLoader loader = externalPackLoader();
            bundle = (loader != null)
                ? ResourceBundle.getBundle(BUNDLE_BASE, currentLocale, loader)
                : ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
            LOG.info("morse-trainer language bundle: " + languageCode);
        } catch (MissingResourceException ex) {
            LOG.log(Level.WARNING, "morse-trainer language '" + languageCode + "' not found, falling back to English");
            bundle = ResourceBundle.getBundle(BUNDLE_BASE, Locale.ENGLISH);
        }
    }

    private static ClassLoader externalPackLoader() {
        if (!PACK_DIR.isDirectory()) return null;
        try {
            URL url = PACK_DIR.toURI().toURL();
            return new URLClassLoader(new URL[]{url}, I18n.class.getClassLoader());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Could not open external pack dir " + PACK_DIR + ": " + ex.getMessage());
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
