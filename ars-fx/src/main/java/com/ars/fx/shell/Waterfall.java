package com.ars.fx.shell;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Spectrogram / waterfall, ported from suite-shell.jsx SuiteWaterfall.
 * Noise floor + per-signal vertical traces, dark-blue→cyan→amber intensity
 * ramp. mode tweaks the synthetic signal (cw = dashed, ft8 = sequenced).
 */
public final class Waterfall {
    private Waterfall() {}

    /** signals: each {xFraction(0..1), widthPx, intensity}. */
    public static Canvas canvas(double w, double h, String mode, double[][] signals) {
        int W = (int) w, H = (int) h;
        WritableImage img = new WritableImage(W, H);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double v = rnd(x * 0.7 + y * 3.1) * 0.18;   // noise floor
                for (double[] s : signals) {
                    double d = Math.abs(x - s[0] * W);
                    if (d < s[1]) {
                        double fall = 1 - d / s[1];
                        double tmod = switch (mode) {
                            case "ft8" -> 0.6 + 0.4 * Math.sin((y / (double) H) * Math.PI * 2 + s[0] * 9);
                            case "cw"  -> ((Math.floor(y / 7.0) + Math.floor(s[0] * 30)) % 2 != 0) ? 1 : 0.15;
                            default    -> 0.9;
                        };
                        v = Math.max(v, fall * s[2] * tmod);
                    }
                }
                double t = Math.min(1, v);
                int r = clamp((int) (20 + t * 235));
                int g = clamp((int) (30 + t * 180));
                int b = clamp((int) (60 + (1 - t) * 90 + t * 40));
                pw.setArgb(x, y, 0xff000000 | (r << 16) | (g << 8) | b);
            }
        }
        Canvas cv = new Canvas(w, h);
        cv.getGraphicsContext2D().drawImage(img, 0, 0);
        return cv;
    }

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(v, 255); }
    private static double rnd(double s) { double x = Math.sin(s * 12.9898) * 43758.5453; return x - Math.floor(x); }
}
