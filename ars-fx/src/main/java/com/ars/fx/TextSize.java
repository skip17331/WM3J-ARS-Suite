package com.ars.fx;

import com.ars.fx.data.HubConfig;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text size, independent of the window scale ({@link UiStyle#scale()}). The window scale zooms everything (text, widths, drawers); this
 * one changes only the size of the type, so a small font can be made readable without making the layout bigger.
 *
 * <p>Every font size in the suite is a {@code font-size: Npx} in a stylesheet or an inline style. The stylesheets are rewritten with
 * each size multiplied by the text factor (cached under {@code ~/.j-hub/css-cache/}), and a light timer rewrites inline styles on the
 * open windows. At 100% nothing is rewritten, so the default look is untouched.
 */
public final class TextSize {
    private TextSize() {}

    public static final String KEY = "ui.textSize";
    public static final String[] SIZES = {"85%", "100%", "115%", "130%", "150%", "175%", "200%"};
    public static final String DEFAULT = "100%";
    public static int defaultIndex() { for (int i = 0; i < SIZES.length; i++) if (SIZES[i].equals(DEFAULT)) return i; return 0; }

    private static final Pattern PX = Pattern.compile("(font-size\\s*:\\s*)([0-9]*\\.?[0-9]+)(px)");
    private static final String ORIG = "ts.orig", DONE = "ts.done";
    private static final List<WeakReference<Scene>> scenes = new ArrayList<>();
    private static Timeline timer;
    private static boolean wired;

    /** The text factor (1.0 = 100%); a bad value is 1.0. */
    public static double factor() {
        try {
            double f = Integer.parseInt(HubConfig.get(KEY, DEFAULT).replace("%", "").trim()) / 100.0;
            return f >= 0.5 && f <= 4 ? f : 1.0;
        } catch (RuntimeException e) { return 1.0; }
    }

    /** {@code css} with every {@code font-size: Npx} multiplied by {@code f} (to the nearest half pixel, never below 1). */
    public static String scaleCss(String css, double f) {
        if (f == 1.0 || css == null) return css;
        Matcher m = PX.matcher(css);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            double px = Math.max(1, Math.round(Double.parseDouble(m.group(2)) * f * 2) / 2.0);
            String v = px == Math.rint(px) ? String.valueOf((long) px) : String.valueOf(px);
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + v + m.group(3)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** The URL to attach for the bundled stylesheet {@code resource}: the original at 100%, else a rewritten copy. */
    public static String url(String resource) {
        var orig = TextSize.class.getResource(resource);
        if (orig == null) return null;
        double f = factor();
        if (f == 1.0) return orig.toExternalForm();
        try {
            String name = resource.substring(resource.lastIndexOf('/') + 1).replace(".css", "");
            Path dir = Paths.get(System.getProperty("user.home"), ".j-hub", "css-cache");
            Files.createDirectories(dir);
            Path out = dir.resolve(name + "@" + Math.round(f * 100) + ".css");
            if (!Files.isRegularFile(out)) {
                String css;
                try (var in = orig.openStream()) { css = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
                Files.writeString(out, scaleCss(css, f), StandardCharsets.UTF_8);
            }
            return out.toUri().toString();
        } catch (IOException | RuntimeException e) {
            return orig.toExternalForm();                    // a failed rewrite must never leave the window unstyled
        }
    }

    /** Whether {@code sheet} is the bundled stylesheet {@code resource} or one of its rewritten copies. */
    public static boolean isOurs(String sheet, String resource) {
        String name = resource.substring(resource.lastIndexOf('/') + 1).replace(".css", "");
        return sheet.endsWith("/" + name + ".css") || (sheet.contains("/css-cache/") && sheet.contains("/" + name + "@"));
    }

    /** Remember {@code scene} so a change of text size reaches it live, and size its inline styles. */
    public static void track(Scene scene) {
        synchronized (scenes) { scenes.add(new WeakReference<>(scene)); }
        if (!wired) {
            wired = true;
            HubConfig.addChangeListener(k -> {
                if (KEY.equals(k) || "*".equals(k)) javafx.application.Platform.runLater(TextSize::refreshAll);
            });
            timer = new Timeline(new KeyFrame(Duration.millis(500), e -> sweepAll()));
            timer.setCycleCount(Timeline.INDEFINITE);
            timer.play();
        }
    }

    private static List<Scene> live() {
        List<Scene> out = new ArrayList<>();
        synchronized (scenes) {
            scenes.removeIf(r -> r.get() == null);
            for (var r : scenes) { Scene s = r.get(); if (s != null) out.add(s); }
        }
        return out;
    }

    /** The factor changed: swap each window's stylesheets, then redo the inline styles. */
    private static void refreshAll() {
        String base = "/com/ars/fx/css/ars.css";
        for (Scene sc : live()) {
            var sheets = sc.getStylesheets();
            int at = -1;
            for (int i = 0; i < sheets.size(); i++) if (isOurs(sheets.get(i), base)) { at = i; sheets.remove(i); break; }
            String u = url(base);
            if (u != null) { if (at < 0) sheets.add(u); else sheets.add(at, u); }
            if (sc.getRoot() != null) UiStyle.applyDashboard(sc.getRoot());
        }
        sweepAll();
    }

    private static void sweepAll() {
        double f = factor();
        for (Scene sc : live()) if (sc.getRoot() != null) sweep(sc.getRoot(), f);
    }

    /** Rewrite inline {@code -fx-font-size} on {@code n} and below; the original text is kept so any factor can be applied from it. */
    private static void sweep(Node n, double f) {
        String cur = n.getStyle();
        var props = n.getProperties();
        Object done = props.get(DONE);
        if (done != null && !done.equals(cur)) { props.remove(DONE); props.remove(ORIG); done = null; }   // the code set a new style: that is the new original
        String orig = done == null ? cur : (String) props.get(ORIG);
        if (orig != null && orig.contains("font-size")) {
            String next = scaleCss(orig, f);
            if (!next.equals(cur)) n.setStyle(next);
            props.put(ORIG, orig); props.put(DONE, next);
        } else if (done != null) { props.remove(DONE); props.remove(ORIG); }
        if (n instanceof Parent p) for (Node c : p.getChildrenUnmodifiable()) sweep(c, f);
    }
}
