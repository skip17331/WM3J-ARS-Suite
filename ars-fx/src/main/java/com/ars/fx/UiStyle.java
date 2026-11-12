package com.ars.fx;

import com.ars.fx.data.HubConfig;
import javafx.scene.Parent;

import java.util.List;

/**
 * Look-and-feel options that sit on top of the base stylesheet ({@code ars.css}), so a look can be tried and dropped without touching it.
 *
 * <p><b>Look.</b> A style is one extra stylesheet attached to a root node (a node's own stylesheets reach just its subtree), with every rule
 * scoped under the {@code .ins} class this class puts on that node. {@link Themes#applyTo} attaches it wherever it applies a colour scheme: the
 * main window's content and the pop-up windows, so the whole suite follows it. "Classic" attaches nothing, so it is exactly the
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
            new Style("instrument", "Instrument panel", "/com/ars/fx/css/styles/instrument.css"),
            new Style("modern",     "Modern",           "/com/ars/fx/css/styles/modern.css"),
            new Style("cockpit",    "Cockpit",          "/com/ars/fx/css/styles/cockpit.css"));

    public static final String DASHBOARD_KEY = "ui.style.dashboard";
    /** Whole-interface scale, as the labels shown in Setup. 100% is the system default (nothing is set). JavaFX on Windows honours the sizes below 100% as asked, but above 100% only
     *  in 50% steps (1.25 and 1.4 behave as 1.0, 1.75 as 1.5), so there is no 125%. */
    public static final String[] SCALES = {"50%", "75%", "85%", "95%", "100%", "150%", "200%"};
    public static final String SCALE_DEFAULT = "100%";
    public static final String SCALE_KEY = "ui.scale";
    /** The position of {@link #SCALE_DEFAULT} in {@link #SCALES}. */
    public static int scaleDefaultIndex() { for (int i = 0; i < SCALES.length; i++) if (SCALES[i].equals(SCALE_DEFAULT)) return i; return 0; }
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
            String v = HubConfig.get(SCALE_KEY, SCALE_DEFAULT);
            if (v == null || v.isBlank() || v.equals(SCALE_DEFAULT)) return;
            // the toolkit wants a plain factor ("1.25"); the "125%" form is ignored
            double pct = Integer.parseInt(v.replace("%", "").trim()) / 100.0;
            // On Linux the property replaces the scale JavaFX would pick itself (Xft.dpi / 96, or GDK_SCALE), so it has to be
            // applied on top of that: 75% on a 2x (4K) screen is 1.5, not 0.75.
            double linux = pct * systemScaleLinux();
            if (System.getProperty("glass.win.uiScale") == null) System.setProperty("glass.win.uiScale", String.valueOf(pct));
            if (System.getProperty("glass.gtk.uiScale") == null) System.setProperty("glass.gtk.uiScale", String.valueOf(linux));
        } catch (Throwable ignored) { /* a bad config must never stop the app starting */ }
    }

    /** The scale JavaFX's GTK toolkit uses when nothing is set: GDK_SCALE if given, else the X resource DPI / 96. 1.0 if unknown or not Linux. */
    static double systemScaleLinux() {
        try {
            if (!System.getProperty("os.name", "").toLowerCase().contains("linux")) return 1.0;
            String g = System.getenv("GDK_SCALE");
            if (g != null && !g.isBlank()) return Math.max(1.0, Double.parseDouble(g.trim()));
            Process pr = new ProcessBuilder("xrdb", "-query").redirectErrorStream(true).start();
            try (var r = new java.io.BufferedReader(new java.io.InputStreamReader(pr.getInputStream()))) {
                for (String line; (line = r.readLine()) != null; )
                    if (line.startsWith("Xft.dpi:")) return Math.max(1.0, Double.parseDouble(line.substring(8).trim()) / 96.0);
            }
        } catch (Throwable ignored) { }
        return 1.0;
    }
}
