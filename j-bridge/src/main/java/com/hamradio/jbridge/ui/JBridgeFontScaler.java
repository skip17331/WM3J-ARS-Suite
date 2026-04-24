package com.hamradio.jbridge.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Applies per-pane font-size overrides for j-bridge, sourced from
 * {@code jBridgeSettings.fonts} in {@code ~/ARS_Suite/j-hub/j-hub.json}.
 *
 * Generates a small override CSS against j-bridge's existing style classes
 * (see {@code resources/css/j-bridge.css}) and appends it to the scene's
 * stylesheet list after {@code j-bridge.css}, so later-stylesheet order
 * lets the override win at equal specificity.
 *
 * Also exposes the global {@code fontSize} from jBridgeSettings so
 * {@link MainWindow} can apply it to the scene root at startup.
 */
public final class JBridgeFontScaler {

    private static final Logger log = LoggerFactory.getLogger(JBridgeFontScaler.class);

    private JBridgeFontScaler() {}

    /** Read {@code jBridgeSettings.fontSize} and return it in px, or the
     *  provided fallback when absent/invalid. */
    public static int readGlobalFontSize(int fallbackPx) {
        JsonObject settings = loadJBridgeSettings();
        if (settings == null) return fallbackPx;
        JsonElement fs = settings.get("fontSize");
        try { return (fs != null && !fs.isJsonNull()) ? fs.getAsInt() : fallbackPx; }
        catch (Exception ex) { return fallbackPx; }
    }

    /** Generate and attach the per-pane override stylesheet to the given
     *  scene. No-op when no overrides are configured. */
    public static void apply(Scene scene) {
        if (scene == null) return;
        JsonObject settings = loadJBridgeSettings();
        if (settings == null) return;
        JsonElement fonts = settings.get("fonts");
        if (fonts == null || !fonts.isJsonObject()) return;
        try {
            String css = buildCss(fonts.getAsJsonObject());
            if (css.isBlank()) return;
            Path tmp = Files.createTempFile("jbridge-fonts-", ".css");
            try (FileWriter w = new FileWriter(tmp.toFile())) { w.write(css); }
            tmp.toFile().deleteOnExit();
            scene.getStylesheets().add(tmp.toUri().toString());
            log.info("Applied per-pane font overrides: {}", fonts);
        } catch (Exception e) {
            log.warn("JBridgeFontScaler failed: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Config load
    // -----------------------------------------------------------------

    private static JsonObject loadJBridgeSettings() {
        String home = System.getProperty("user.home", "");
        Path[] candidates = new Path[] {
            Paths.get(home, "ARS_Suite", "j-hub", "j-hub.json"),
            Paths.get(".", "j-hub.json"),
        };
        for (Path p : candidates) {
            if (!Files.isReadable(p)) continue;
            try (FileReader r = new FileReader(p.toFile())) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                JsonElement jb = root.get("jBridgeSettings");
                if (jb != null && jb.isJsonObject()) return jb.getAsJsonObject();
            } catch (Exception ignored) {}
        }
        return null;
    }

    // -----------------------------------------------------------------
    // CSS builder — pane keys match slider IDs in config.js
    // -----------------------------------------------------------------

    private static String buildCss(JsonObject f) {
        StringBuilder sb = new StringBuilder();
        int v;

        // toolbar — top bar + buttons + title/subtitle
        if ((v = intOf(f, "toolbar")) > 0) {
            appendRule(sb,
                ".jb-toolbar, .jb-toolbar .label, .tb-btn, .jb-title, .jb-sub",
                v);
        }
        // sidebar — status panel + key/value rows
        if ((v = intOf(f, "sidebar")) > 0) {
            appendRule(sb,
                ".jb-sidebar, .jb-sidebar .label, .jb-status-panel, " +
                ".jb-status-panel .label, .jb-panel-hdr, .jb-key-lbl, .jb-val-lbl",
                v);
        }
        // band — band activity panel rows
        if ((v = intOf(f, "band")) > 0) {
            appendRule(sb,
                ".jb-band-panel, .jb-band-panel .label, " +
                ".jb-band-hdr-row, .jb-band-hdr-lbl, .jb-band-row, .jb-band-data-lbl",
                v);
        }
        // table — WSJT-X decode table
        if ((v = intOf(f, "table")) > 0) {
            appendRule(sb,
                ".decode-table, .decode-table .table-cell, " +
                ".decode-table .column-header, .decode-table .column-header .label",
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
