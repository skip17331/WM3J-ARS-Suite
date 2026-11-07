package com.ars.fx.surface;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** A little satellite for the J-Sat plots: a body, two solar panels and a dish, in the satellite's colour. */
final class SatIcon {
    private SatIcon() {}

    private static final Color OUTLINE = Color.web("#0b0f14");

    /**
     * Draw one satellite centred on (x, y), {@code size} pixels across the panels.
     * @param velocityRad screen direction of travel in radians (atan2 of dy, dx), or NaN when not known; the panels sit across it
     */
    static void draw(GraphicsContext g, double x, double y, Color color, double size, double alpha, double velocityRad) {
        double s = size / 2;                                               // half the span
        g.save();
        g.translate(x, y);
        if (!Double.isNaN(velocityRad)) g.rotate(Math.toDegrees(velocityRad) - 90);
        Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        Color panel = new Color(color.getRed() * 0.75, color.getGreen() * 0.75, color.getBlue() * 0.75, alpha);
        Color body = new Color(Math.min(1, color.getRed() * 0.4 + 0.6), Math.min(1, color.getGreen() * 0.4 + 0.6), Math.min(1, color.getBlue() * 0.4 + 0.6), alpha);
        double bw = s * 0.42, ph = s * 0.46, pw = s * 0.52, gap = bw + s * 0.06;

        // dish, on a short arm toward the front
        g.setStroke(new Color(OUTLINE.getRed(), OUTLINE.getGreen(), OUTLINE.getBlue(), 0.85 * alpha)); g.setLineWidth(2.4);
        g.strokeLine(0, bw * 0.6, 0, bw + s * 0.34);
        g.setStroke(fill); g.setLineWidth(1.1); g.strokeLine(0, bw * 0.6, 0, bw + s * 0.34);
        g.setFill(body); g.fillOval(-s * 0.16, bw + s * 0.26, s * 0.32, s * 0.2);

        // solar panels with their cell lines
        for (int side = -1; side <= 1; side += 2) {
            double x0 = side < 0 ? -gap - pw * 2 : gap;
            g.setFill(panel); g.fillRect(x0, -ph, pw * 2, ph * 2);
            g.setStroke(new Color(OUTLINE.getRed(), OUTLINE.getGreen(), OUTLINE.getBlue(), 0.9 * alpha)); g.setLineWidth(1);
            g.strokeRect(x0, -ph, pw * 2, ph * 2);
            g.setStroke(new Color(1, 1, 1, 0.35 * alpha)); g.setLineWidth(0.7);
            g.strokeLine(x0 + pw * 2 / 3, -ph, x0 + pw * 2 / 3, ph); g.strokeLine(x0 + pw * 4 / 3, -ph, x0 + pw * 4 / 3, ph);
            g.strokeLine(x0, 0, x0 + pw * 2, 0);
        }
        // arms between body and panels
        g.setStroke(new Color(OUTLINE.getRed(), OUTLINE.getGreen(), OUTLINE.getBlue(), 0.9 * alpha)); g.setLineWidth(1.2);
        g.strokeLine(-gap, 0, -bw, 0); g.strokeLine(bw, 0, gap, 0);

        // body
        g.setFill(body); g.fillRect(-bw, -bw, bw * 2, bw * 2);
        g.setStroke(new Color(OUTLINE.getRed(), OUTLINE.getGreen(), OUTLINE.getBlue(), 0.95 * alpha)); g.setLineWidth(1.3);
        g.strokeRect(-bw, -bw, bw * 2, bw * 2);
        g.setFill(fill); g.fillRect(-bw * 0.45, -bw * 0.45, bw * 0.9, bw * 0.9);
        g.restore();
    }
}
