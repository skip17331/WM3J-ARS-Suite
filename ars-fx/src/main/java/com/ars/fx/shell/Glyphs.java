package com.ars.fx.shell;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Transform;

/**
 * Module + UI glyphs, ported 1:1 from ars-shared.jsx ARSGlyph. Each is drawn in
 * a 24x24 viewBox as a Group of JavaFX shapes (stroke = the module hue), then
 * scaled to the requested size. Deliberately simple geometric marks.
 */
public final class Glyphs {
    private Glyphs() {}

    public static Node glyph(String id, double size, Color color) {
        Group g = new Group();
        switch (id) {
            case "log" -> add(g, color, line(5,7,19,7), line(5,12,19,12), line(5,17,14,17));
            case "map" -> add(g, color, ring(12,12,7.5), line(12,3,12,21), line(3.5,12,20.5,12),
                    path("M12 4.5c3 2.4 3 12.6 0 15"), path("M12 4.5c-3 2.4-3 12.6 0 15"));
            case "bridge" -> add(g, color, ring(6,12,2.4), ring(18,12,2.4), line(8.4,12,15.6,12),
                    path("M4 8.5a5 5 0 000 7"), path("M20 8.5a5 5 0 010 7"));
            case "digi" -> add(g, color, line(5,12,5,15), line(9,9,9,18), line(12,5,12,19), line(15,9,15,18), line(19,12,19,15));
            case "sat" -> {
                Ellipse e = new Ellipse(12,12,9,4.5); e.getTransforms().add(Transform.rotate(-30,12,12));
                add(g, color, e); g.getChildren().add(dot(12,12,2.4,color));
            }
            case "vault" -> {
                Rectangle r = new Rectangle(4.5,6,15,13); r.setArcWidth(4); r.setArcHeight(4);
                add(g, color, r, path("M9 6V4.5h6V6"), ring(12,12.5,2));
            }
            case "learn" -> add(g, color, path("M5 5.5h7a2.5 2.5 0 012.5 2.5v11a2 2 0 00-2-2H5z"),
                    path("M19 5.5h-4.5"), path("M14.5 8v11"));
            case "gear" -> add(g, color, ring(12,12,3),
                    path("M12 2.6v3M12 18.4v3M2.6 12h3M18.4 12h3M5.4 5.4l2.1 2.1M16.5 16.5l2.1 2.1M18.6 5.4l-2.1 2.1M7.5 16.5l-2.1 2.1"));
            default -> { // hub: target
                add(g, color, ring(12,12,7.5), line(12,4.5,12,2.5), line(12,21.5,12,19.5));
                g.getChildren().add(dot(12,12,2.6,color));
            }
        }
        double sc = size / 24.0;
        g.setScaleX(sc); g.setScaleY(sc);
        return new Group(g);
    }

    private static void add(Group g, Color color, Shape... shapes) {
        for (Shape s : shapes) {
            s.setStroke(color); s.setStrokeWidth(1.9);
            s.setStrokeLineCap(StrokeLineCap.ROUND); s.setStrokeLineJoin(StrokeLineJoin.ROUND);
            if (s.getFill() == Color.BLACK || s.getFill() == null) s.setFill(Color.TRANSPARENT);
            g.getChildren().add(s);
        }
    }
    private static Line line(double x1,double y1,double x2,double y2) { return new Line(x1,y1,x2,y2); }
    private static Circle ring(double cx,double cy,double r) { Circle c = new Circle(cx,cy,r); c.setFill(Color.TRANSPARENT); return c; }
    private static Circle dot(double cx,double cy,double r,Color color) { Circle c = new Circle(cx,cy,r); c.setFill(color); return c; }
    private static SVGPath path(String d) { SVGPath p = new SVGPath(); p.setContent(d); p.setFill(Color.TRANSPARENT); return p; }
}
