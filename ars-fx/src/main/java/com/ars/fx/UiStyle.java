package com.ars.fx;

import com.ars.fx.data.HubConfig;
import javafx.scene.Parent;

import java.util.List;

/**
 * Look-and-feel options that sit on top of the base stylesheet ({@code ars.css}), so a look can be tried and dropped without touching it.
 *
 * <p><b>Dashboard style.</b> A style is one extra stylesheet attached to the dashboard's root node only (a node's own stylesheets reach just its
 * subtree), with every rule scoped under the {@code .ins} class this class puts on that node. "Classic" attaches nothing, so it is exactly the
 * stylesheet-free look; picking it in J-Hub ▸ Setup ▸ Appearance is the whole of a revert. To try another look, drop a new css in
 * {@code css/styles/}, add one line to {@link #DASHBOARD_STYLES}, and it shows up in that menu.
 *
 * <p><b>UI size.</b> A whole-interface scale (text, widths, drawers and canvases together), handed to JavaFX before it starts, so it takes effect
 * on the next launch.
 */
public final class UiStyle {
    private UiStyle() {}

    public record Style(String key, String label, String css) {}

    /** The dashboard looks on offer. {@code css} null = the base stylesheet alone. */
    public static final List<Style> DASHBOARD_STYLES = List.of(
            new Style("classic",    "Classic",          null),
            new Style("instrument", "Instrument panel", "/com/ars/fx/css/styles/instrument.css"));

    public static final String DASHBOARD_KEY = "ui.style.dashboard";
    /** Whole-interface scale, as the labels shown in Setup; the first is the system default (nothing is set). JavaFX on Windows only honours 50% steps: 1.25 and 1.4 behave as 1.0, 1.75 as 1.5. */
    public static final String[] SCALES = {"100%", "150%", "200%"};
    public static final String SCALE_KEY = "ui.scale";
    private static final String SCOPE_CLASS = "ins";

    public static Style dashboard() {
        String k = HubConfig.get(DASHBOARD_KEY, "classic");
        for (Style s : DASHBOARD_STYLES) if (s.key().equals(k)) return s;
        return DASHBOARD_STYLES.get(0);
    }

    /** Put the chosen dashboard look on {@code root} (and take any other off it first). */
    public static void applyDashboard(Parent root) {
        if (root == null) return;
        for (Style s : DASHBOARD_STYLES) {
            if (s.css() == null) continue;
            var url = UiStyle.class.getResource(s.css());
            if (url != null) root.getStylesheets().remove(url.toExternalForm());
        }
        root.getStyleClass().remove(SCOPE_CLASS);
        Style cur = dashboard();
        if (cur.css() == null) return;
        var url = UiStyle.class.getResource(cur.css());
        if (url == null) return;
        root.getStyleClass().add(SCOPE_CLASS);
        root.getStylesheets().add(url.toExternalForm());
    }

    /** Call once, before JavaFX starts: turns the saved UI size into the toolkit's own scale property. A scale given on the command line wins. */
    public static void applyScale() {
        try {
            String v = HubConfig.get(SCALE_KEY, SCALES[0]);
            if (v == null || v.isBlank() || v.equals(SCALES[0])) return;
            // the toolkit wants a plain factor ("1.25"); the "125%" form is ignored
            String factor = String.valueOf(Integer.parseInt(v.replace("%", "").trim()) / 100.0);
            for (String p : new String[]{"glass.win.uiScale", "glass.gtk.uiScale"})
                if (System.getProperty(p) == null) System.setProperty(p, factor);
        } catch (Throwable ignored) { /* a bad config must never stop the app starting */ }
    }
}
