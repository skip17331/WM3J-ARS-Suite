package com.hamradio.modem.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates a per-pane font-size override stylesheet for j-digi and attaches
 * it to the scene after the main theme stylesheet, so CSS order lets the
 * overrides win over the base em-relative values defined in
 * {@code MainWindow.buildCss()}.
 *
 * Source of truth is the {@code jDigiSettings.fonts} block in
 * {@code ~/ARS_Suite/j-hub/j-hub.json}; a null block means no overrides.
 *
 * Thread-safe reapply: {@link #apply(Scene)} removes any prior override before
 * writing the new one, so live updates from CONFIG_UPDATE don't leak
 * stylesheets into the scene.
 */
public final class JDigiFontScaler {

    private static final Logger log = LoggerFactory.getLogger(JDigiFontScaler.class);

    /** Marker file-URI suffix so we can identify our own stylesheet entry. */
    private static final String MARKER = "/jdigi-fonts-";

    private JDigiFontScaler() {}

    /** Read j-hub.json and apply overrides to the given scene. */
    public static void apply(Scene scene) {
        if (scene == null) return;
        JsonObject fonts = loadFontsFromJHubJson();
        apply(scene, fonts);
    }

    /** Apply an already-parsed {@code fonts} JSON object to the scene. Use
     *  this from the CONFIG_UPDATE live-update path to avoid re-reading the
     *  file every time. */
    public static void apply(Scene scene, JsonObject fonts) {
        if (scene == null) return;
        Platform.runLater(() -> {
            // Remove previous override if any
            scene.getStylesheets().removeIf(u -> u.contains(MARKER));
            if (fonts == null || fonts.size() == 0) return;
            try {
                String css = buildCss(fonts);
                if (css.isBlank()) return;
                Path tmp = Files.createTempFile("jdigi-fonts-", ".css");
                try (FileWriter w = new FileWriter(tmp.toFile())) { w.write(css); }
                tmp.toFile().deleteOnExit();
                scene.getStylesheets().add(tmp.toUri().toString());
                log.info("Applied per-pane font overrides: {}", fonts);
            } catch (Exception e) {
                log.warn("JDigiFontScaler failed: {}", e.getMessage());
            }
        });
    }

    // -----------------------------------------------------------------
    // Config load
    // -----------------------------------------------------------------

    private static JsonObject loadFontsFromJHubJson() {
        String home = System.getProperty("user.home", "");
        Path[] candidates = new Path[] {
            Paths.get(home, "ARS_Suite", "j-hub", "j-hub.json"),
            Paths.get(".", "j-hub.json"),
        };
        for (Path p : candidates) {
            if (!Files.isReadable(p)) continue;
            try (FileReader r = new FileReader(p.toFile())) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                JsonElement jdigi = root.get("jDigiSettings");
                if (jdigi == null || !jdigi.isJsonObject()) continue;
                JsonElement fonts = jdigi.getAsJsonObject().get("fonts");
                if (fonts != null && fonts.isJsonObject()) return fonts.getAsJsonObject();
            } catch (Exception ignored) {}
        }
        return null;
    }

    // -----------------------------------------------------------------
    // CSS builder — keys match slider IDs in j-hub web UI config.js
    // -----------------------------------------------------------------

    private static String buildCss(JsonObject f) {
        StringBuilder sb = new StringBuilder();
        int v;

        // rxTx — RX/TX text areas (.rx-area / .tx-area use 1em, so scale the
        // root of the area; also target panel titles so header doesn't
        // dwarf the body)
        if ((v = intOf(f, "rxTx")) > 0) {
            appendRule(sb,
                ".rx-area, .tx-area, .jd-rx-header, .jd-tx-header, .jd-rx-sub",
                v);
        }
        // freq — frequency display + callsign
        if ((v = intOf(f, "freq")) > 0) {
            appendRule(sb,
                ".jd-freq-display, .jd-callsign, .jd-rotor, .jd-grid-label, " +
                ".jd-audio-sub, .jd-mode-tag",
                v);
        }
        // statusBar — bottom status strip
        if ((v = intOf(f, "statusBar")) > 0) {
            appendRule(sb,
                ".jd-statusbar, .jd-statusbar .label, .jd-status-label, " +
                ".jd-sb-item, .jd-sb-item .label",
                v);
        }
        // toolbar — top toolbar + buttons
        if ((v = intOf(f, "toolbar")) > 0) {
            appendRule(sb,
                ".jd-toolbar, .jd-toolbar .label, .tb-btn, " +
                ".xmit-btn, .cancel-btn, .afc-btn, .sql-btn",
                v);
        }
        // entry — entry pane, macros, section labels
        if ((v = intOf(f, "entry")) > 0) {
            appendRule(sb,
                ".jd-entry-pane, .jd-entry-pane .label, .jd-entry-pane .text-field, " +
                ".entry-label, .jd-section-label, .jd-panel-title, " +
                ".macro-button, .macro-button-programmable, .macro-button-disabled, " +
                ".dupe-status, .exchange-hint",
                v);
        }
        return sb.toString();
    }

    private static int intOf(JsonObject f, String k) {
        JsonElement e = f.get(k);
        try { return (e != null && !e.isJsonNull()) ? e.getAsInt() : 0; }
        catch (Exception ex) { return 0; }
    }

    private static void appendRule(StringBuilder sb, String selectors, int px) {
        sb.append(selectors).append(" { -fx-font-size: ").append(px).append("px; }\n");
    }
}
