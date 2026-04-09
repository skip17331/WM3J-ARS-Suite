package com.hamclock.ui.panels;

import com.hamclock.app.ServiceRegistry;
import com.hamclock.service.geomag.GeomagneticAlert;
import com.hamclock.service.lightning.LightningData;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * Horizontal alert ticker scrolling right-to-left.
 * Shows geomagnetic storm alerts, lightning activity, and high-wind warnings.
 */
public class DxScrollerBar extends Pane {

    private static final double BAR_HEIGHT  = 66;
    private static final double SCROLL_SPEED = 80.0;   // px / second
    private static final double TICK_MS      = 30.0;

    private final ServiceRegistry services;
    private final Canvas canvas;

    private String  tickerText  = "";
    private String  tickerColor = "#00ccff";
    private double  textX       = -1;   // -1 = uninitialised
    private double  textWidth   = 0;
    private boolean needsRebuild = true;

    private final Timeline ticker;

    public DxScrollerBar(ServiceRegistry services) {
        this.services = services;
        canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.setHeight(BAR_HEIGHT);
        getChildren().add(canvas);
        setPrefHeight(BAR_HEIGHT);
        setMaxHeight(BAR_HEIGHT);
        setStyle("-fx-background-color: #05050d;");

        ticker = new Timeline(new KeyFrame(Duration.millis(TICK_MS), e -> tick()));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
    }

    /** Called every second by the animation loop. */
    public void update() {
        needsRebuild = true;
    }

    private void rebuild() {
        needsRebuild = false;
        StringBuilder sb = new StringBuilder();
        String color = "#00ccff";

        // ── Geomagnetic alerts ──────────────────────────────────────────────
        GeomagneticAlert geomag = services.geomagneticAlertProvider.getCached();
        if (geomag != null && geomag.getLevel() != GeomagneticAlert.Level.NONE) {
            double kp = geomag.getKpIndex();
            String scale = geomag.getGScale();
            String lvl = geomag.getLevel().name();
            sb.append(String.format("  ⚡ GEOMAGNETIC %s  Kp=%.1f  %s  Aurora visible to %.0f°N  ●  ",
                lvl, kp, scale, geomag.getAuroraVisibleLatitude()));
            color = switch (geomag.getLevel()) {
                case ALERT   -> "#ff4455";
                case WARNING -> "#ffaa00";
                case WATCH   -> "#ffdd00";
                default      -> "#00ccff";
            };
        }

        // ── Lightning activity ──────────────────────────────────────────────
        LightningData lightning = services.lightningProvider.getCached();
        if (lightning != null && !lightning.getStrikes().isEmpty()) {
            int count = lightning.getStrikes().size();
            sb.append(String.format("  ⚡ LIGHTNING ACTIVITY  %d strikes in last hour  ●  ", count));
            if (color.equals("#00ccff")) color = "#ffdd00";
        }

        // ── Quiet / no alerts ──────────────────────────────────────────────
        if (sb.length() == 0) {
            sb.append("  Space weather quiet  ●  No active alerts  ●  No lightning activity detected  ●  ");
            color = "#334455";
        }

        // Repeat text so it loops seamlessly
        String single = sb.toString();
        tickerText  = single + single;
        tickerColor = color;

        double fs = services.getSettings().getFontSize() * 2.31;
        textWidth = single.length() * fs * 0.6;
    }

    private void tick() {
        double w = canvas.getWidth();
        if (w <= 0) return;

        if (needsRebuild || textX < 0) {
            rebuild();
            if (textX < 0) textX = w;
        }

        // Advance scroll
        textX -= SCROLL_SPEED * (TICK_MS / 1000.0);
        if (textX < -textWidth) textX = w;

        // Render
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double fs = services.getSettings().getFontSize() * 2.31;
        gc.setFont(Font.font("Liberation Mono", fs));

        gc.setFill(Color.web("#05050d"));
        gc.fillRect(0, 0, w, BAR_HEIGHT);
        gc.setStroke(Color.web("#1a2a4a"));
        gc.setLineWidth(1);
        gc.strokeLine(0, 0, w, 0);

        gc.setFill(Color.web(tickerColor));
        gc.fillText(tickerText, textX, BAR_HEIGHT - 15);
    }

    public void stop() { ticker.stop(); }
}
