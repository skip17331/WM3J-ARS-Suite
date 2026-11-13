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
 * <p><b>UI size.</b> A whole-interface scale (text, widths, drawers and canvases together), applied live by {@link #scaled} to the main window.
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
    /** Whole-interface scale, as the labels shown in Setup. 100% is the default. Applied live by {@link #scaled}, so any step works. */
    public static final String[] SCALES = {"50%", "75%", "85%", "95%", "100%", "125%", "150%", "200%"};
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

    /** The Modern style is the space age look: a starfield behind the page and a mission clock in the status bar. */
    public static boolean isModern() { return "modern".equals(dashboard().key()); }

    /** The Instrument panel style shows round needle gauges in the status strip when it is open. */
    public static boolean isInstrument() { return "instrument".equals(dashboard().key()); }

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

    /** The saved UI size as a factor (1.0 = 100%). A bad or missing value is 1.0, so a broken config never stops the app starting. */
    public static double scale() {
        try {
            double f = Integer.parseInt(HubConfig.get(SCALE_KEY, SCALE_DEFAULT).replace("%", "").trim()) / 100.0;
            return f >= 0.25 && f <= 4 ? f : 1.0;
        } catch (RuntimeException e) { return 1.0; }
    }

    /**
     * A pane that shows {@code content} at the saved UI size and follows changes to it live: the content is laid out at (window size / scale) and
     * scaled to fill the window, so text, widths, drawers and canvases all change together. Only this window is scaled; pop-up windows are not.
     */
    public static javafx.scene.layout.StackPane scaled(javafx.scene.layout.Region content) {
        javafx.scene.transform.Scale tf = new javafx.scene.transform.Scale(1, 1, 0, 0);
        content.getTransforms().add(tf);
        content.setManaged(false);                       // sized by hand below; dialogs the app adds to the scene root (a StackPane) still stack over it, unscaled
        javafx.scene.layout.StackPane view = new javafx.scene.layout.StackPane(content) {
            @Override protected void layoutChildren() {
                super.layoutChildren();
                double s = scale();
                tf.setX(s); tf.setY(s);
                content.resizeRelocate(0, 0, getWidth() / s, getHeight() / s);
            }
        };
        view.setMinSize(0, 0);
        HubConfig.addChangeListener(k -> {
            if (SCALE_KEY.equals(k) || "*".equals(k)) javafx.application.Platform.runLater(view::requestLayout);
        });
        return view;
    }

}
