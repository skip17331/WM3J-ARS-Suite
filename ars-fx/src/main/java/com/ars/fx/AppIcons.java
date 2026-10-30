package com.ars.fx;

import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The suite's icons: one mark for the app, the tray and the pop-up windows, and a title-card icon per module for loose
 * module windows. They live in {@code com/ars/fx/icons/} (also read by the installer for its shortcuts). Without a
 * window icon the desktop shows a generic gear in the panel and task switcher.
 */
public final class AppIcons {
    private AppIcons() {}

    private static final String BASE = "/com/ars/fx/icons/";
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    /** The app mark ({@code suite.png}), or a module's icon ({@code log}, {@code map}, {@code sat}, {@code digi},
     *  {@code bridge}, {@code vault}, {@code learn}, {@code hub}); the app mark for anything else. */
    public static Image of(String name) {
        String n = name == null || name.isBlank() ? "suite" : name.toLowerCase();
        Image img = CACHE.computeIfAbsent(n, k -> load(k));
        return img != null ? img : (n.equals("suite") ? null : of("suite"));
    }

    /** Give a window the app mark, or the module's own icon when {@code module} names one. */
    public static void apply(Stage stage, String module) {
        if (stage == null) return;
        Image img = of(module);
        if (img != null) { stage.getIcons().setAll(img); }
    }

    /** The app mark as an AWT image sized for the system tray (smoothly scaled), or null if it can't be read. */
    public static java.awt.Image tray(int size) {
        try (InputStream in = AppIcons.class.getResourceAsStream(BASE + "suite.png")) {
            if (in == null) return null;
            BufferedImage src = javax.imageio.ImageIO.read(in);
            int s = Math.max(16, size);
            BufferedImage out = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = out.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, s, s, null);
            g.dispose();
            return out;
        } catch (Exception e) { return null; }
    }

    /** Close the native start-up splash (shown by the JVM from the jar manifest) once the first window is up. Safe when
     *  there is no splash, and when AWT is unavailable. */
    public static void closeSplash() {
        try {
            java.awt.SplashScreen s = java.awt.SplashScreen.getSplashScreen();
            if (s != null && s.isVisible()) s.close();
        } catch (Throwable ignored) { /* no splash, headless, or not supported */ }
    }

    private static Image load(String name) {
        try (InputStream in = AppIcons.class.getResourceAsStream(BASE + name + ".png")) {
            return in == null ? null : new Image(in);
        } catch (Exception e) { return null; }
    }
}
