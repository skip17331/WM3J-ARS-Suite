package com.ars.fx.shell;

import com.ars.fx.data.Propagation.Verdict;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/** A small coloured dot for a spot list row: green = likely open, amber = marginal, grey = probably closed. Null for no verdict. */
public final class PathChip {
    private PathChip() {}

    public static String color(Verdict v) {
        return v == Verdict.OPEN ? "#4ade80" : v == Verdict.MARGINAL ? "#fbbf24" : "#6b7683";
    }

    public static String tip(Verdict v) {
        return switch (v) {
            case OPEN -> "Path now: likely open (our estimate, not a measurement)";
            case MARGINAL -> "Path now: marginal (our estimate)";
            case CLOSED -> "Path now: probably closed (our estimate)";
        };
    }

    public static Node of(Verdict v) {
        if (v == null) return null;
        Region dot = new Region();
        dot.setMinSize(8, 8); dot.setMaxSize(8, 8);
        dot.setStyle("-fx-background-color:" + color(v) + ";-fx-background-radius:99;");
        Tips.tip(dot, tip(v));
        return dot;
    }
}
