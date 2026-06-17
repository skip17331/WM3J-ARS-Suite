package com.ars.fx.surface.contest.map;

import com.ars.fx.data.ContestState;
import com.ars.fx.surface.JLogContest;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canvas renderer for a contest multiplier map. Fills each region from the
 * bundled geometry, lights worked regions from the live {@link ContestState}
 * score (resolving plugin ids through the map's aliasTargets), draws labels,
 * and on click fills the multiplier exchange field in {@link JLogContest}.
 */
public final class ContestMapPane extends Pane {

    private final MapGeometry.MapDef map;
    private final String fieldId;                       // exchange field filled on click (may be null)
    private final Canvas cv = new Canvas();
    private final Map<String, List<double[]>> polys = new HashMap<>();   // region id -> subpath polygons
    private final Map<String, String> aliasToPrimary = new HashMap<>();  // plugin id -> rendered region id

    public ContestMapPane(String mapName, String fieldId) {
        this.map = MapGeometry.load(mapName);
        this.fieldId = fieldId;
        getChildren().add(cv);
        setMinSize(220, 150); setPrefSize(320, 200);
        if (map != null) {
            for (var e : map.regions.entrySet()) polys.put(e.getKey(), SvgPathParser.parse(e.getValue().svgPath()));
            for (var e : map.aliasTargets.entrySet()) for (String alias : e.getValue()) aliasToPrimary.put(alias.toUpperCase(), e.getKey().toUpperCase());
        }
        cv.setOnMouseClicked(this::onClick);
        widthProperty().addListener((o, a, b) -> repaint());
        heightProperty().addListener((o, a, b) -> repaint());
        ContestState.addListener(() -> Platform.runLater(this::repaint));
    }

    @Override protected void layoutChildren() {
        cv.setWidth(getWidth()); cv.setHeight(getHeight());
        repaint();
    }

    private Set<String> workedPrimaries() {
        Set<String> out = new HashSet<>();
        if (map == null) return out;
        for (String t : ContestState.workedMults()) {
            String u = t.toUpperCase();
            out.add(aliasToPrimary.getOrDefault(u, u));
        }
        return out;
    }

    // viewBox → canvas transform (fit + centre)
    private double s, ox, oy;
    private void computeTransform(double w, double h) {
        double vbX = map.viewBox[0], vbY = map.viewBox[1], vbW = map.viewBox[2], vbH = map.viewBox[3];
        s = Math.min(w / vbW, h / vbH);
        ox = -vbX * s + (w - vbW * s) / 2;
        oy = -vbY * s + (h - vbH * s) / 2;
    }
    private double tx(double x) { return x * s + ox; }
    private double ty(double y) { return y * s + oy; }

    private void repaint() {
        double w = cv.getWidth(), h = cv.getHeight();
        GraphicsContext g = cv.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        if (map == null || w < 2 || h < 2) return;
        computeTransform(w, h);

        Set<String> worked = workedPrimaries();
        Color base = Color.web("#1a212b"), border = Color.web("#2b3644"), lit = Color.web("#67d283");
        boolean fewLabels = map.regions.size() <= 70;
        g.setLineWidth(0.7);
        for (var e : map.regions.entrySet()) {
            boolean on = worked.contains(e.getKey().toUpperCase());
            g.setFill(on ? lit : base);
            g.setStroke(on ? lit.darker() : border);
            for (double[] poly : polys.get(e.getKey())) {
                int n = poly.length / 2;
                double[] xs = new double[n], ys = new double[n];
                for (int i = 0; i < n; i++) { xs[i] = tx(poly[2 * i]); ys[i] = ty(poly[2 * i + 1]); }
                g.fillPolygon(xs, ys, n);
                g.strokePolygon(xs, ys, n);
            }
            if (fewLabels || on) {
                MapGeometry.Region r = map.regions.get(e.getKey());
                g.setFill(on ? Color.web("#07140c") : Color.web("#6a7684"));
                g.setFont(Font.font("JetBrains Mono", on ? FontWeight.BOLD : FontWeight.NORMAL, Math.max(7, Math.min(11, 9 * s / 1.0 > 11 ? 11 : 9))));
                g.setTextAlign(TextAlignment.CENTER);
                if (r.labelX() > 0 || r.labelY() > 0) g.fillText(e.getKey(), tx(r.labelX()), ty(r.labelY()) + 3);
            }
        }
    }

    private void onClick(MouseEvent ev) {
        if (map == null || fieldId == null) return;
        double vx = (ev.getX() - ox) / s, vy = (ev.getY() - oy) / s;
        for (var e : polys.entrySet()) {
            for (double[] poly : e.getValue()) {
                if (SvgPathParser.contains(poly, vx, vy)) { JLogContest.fillField(fieldId, e.getKey()); return; }
            }
        }
    }
}
