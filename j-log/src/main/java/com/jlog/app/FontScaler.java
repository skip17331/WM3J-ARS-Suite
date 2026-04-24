package com.jlog.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates a CSS override stylesheet from the {@code jLogSettings.fonts}
 * block in {@code j-hub.json} and appends it to a Scene, so per-pane font
 * sizes configured via the j-hub web UI take effect on j-log startup.
 *
 * Selectors mirror the style classes in {@code com/jlog/css/base.css} so the
 * override wins via later-stylesheet order (same specificity). Missing keys
 * fall back to the existing base.css defaults — no override emitted.
 */
public final class FontScaler {

    private static final Logger log = LoggerFactory.getLogger(FontScaler.class);

    private FontScaler() {}

    public static void apply(Scene scene) {
        if (scene == null) return;
        Fonts f = loadFromJHubJson();
        if (f == null) return;
        try {
            String css = buildCss(f);
            if (css.isBlank()) return;
            Path tmp = Files.createTempFile("jlog-fonts-", ".css");
            try (FileWriter w = new FileWriter(tmp.toFile())) { w.write(css); }
            tmp.toFile().deleteOnExit();
            scene.getStylesheets().add(tmp.toUri().toString());
            log.info("Applied per-pane font overrides: {}", f);
        } catch (Exception e) {
            log.warn("FontScaler failed: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Config load
    // -----------------------------------------------------------------

    private static Fonts loadFromJHubJson() {
        String home = System.getProperty("user.home", "");
        Path[] candidates = new Path[] {
            Paths.get(home, "ARS_Suite", "j-hub", "j-hub.json"),
            Paths.get(".", "j-hub.json"),
        };
        for (Path p : candidates) {
            if (!Files.isReadable(p)) continue;
            try {
                JsonNode root = new ObjectMapper().readTree(p.toFile());
                JsonNode fonts = root.path("jLogSettings").path("fonts");
                if (fonts.isMissingNode() || !fonts.isObject()) return null;
                Fonts f = new Fonts();
                f.statusBar = fonts.path("statusBar").isInt() ? fonts.get("statusBar").asInt() : 0;
                f.entry     = fonts.path("entry")    .isInt() ? fonts.get("entry")    .asInt() : 0;
                f.table     = fonts.path("table")    .isInt() ? fonts.get("table")    .asInt() : 0;
                f.info      = fonts.path("info")     .isInt() ? fonts.get("info")     .asInt() : 0;
                f.spots     = fonts.path("spots")    .isInt() ? fonts.get("spots")    .asInt() : 0;
                return f;
            } catch (Exception ignored) {}
        }
        return null;
    }

    // -----------------------------------------------------------------
    // CSS builder
    // -----------------------------------------------------------------

    private static String buildCss(Fonts f) {
        StringBuilder sb = new StringBuilder();
        if (f.statusBar > 0) {
            appendRule(sb,
                ".clock-bar, .clock-bar .label, .clock-bar .text-field, " +
                ".clock-label, .clock-field, .station-call, .civ-status, " +
                ".status-label",
                f.statusBar);
        }
        if (f.entry > 0) {
            appendRule(sb,
                ".entry-pane, .entry-pane .label, .entry-pane .text-field, " +
                ".entry-pane .button, .entry-pane .check-box, .entry-label, " +
                ".entry-grid, .macro-bar, .macro-button",
                f.entry);
        }
        if (f.table > 0) {
            appendRule(sb,
                ".qso-table, .qso-table .table-cell, .qso-table .column-header, " +
                ".qso-table .column-header .label, .table-toolbar, " +
                ".table-toolbar .label, .table-toolbar .button",
                f.table);
        }
        if (f.info > 0) {
            appendRule(sb,
                ".info-pane-row, .info-pane-row .label, .info-pane-row .titled-pane > .title, " +
                ".info-content, .info-value",
                f.info);
        }
        if (f.spots > 0) {
            appendRule(sb,
                ".dx-pane, .dx-pane .label, .dx-pane .table-cell, " +
                ".dx-pane .column-header, .dx-pane .column-header .label, " +
                ".dx-pane .titled-pane > .title, .dx-pane .button, .dx-pane .text-field",
                f.spots);
        }
        return sb.toString();
    }

    private static void appendRule(StringBuilder sb, String selectors, int px) {
        sb.append(selectors).append(" { -fx-font-size: ").append(px).append("px; }\n");
    }

    private static final class Fonts {
        int statusBar, entry, table, info, spots;
        @Override public String toString() {
            return "status=" + statusBar + " entry=" + entry + " table=" + table
                 + " info=" + info + " spots=" + spots;
        }
    }
}
