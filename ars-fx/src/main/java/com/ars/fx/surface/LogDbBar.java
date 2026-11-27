package com.ars.fx.surface;

import com.ars.fx.data.LogDbInfo;
import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import static com.ars.fx.shell.Shell.lbl;

/** One-line "which database am I writing to" strip for J-Log, with a shortcut to the database tools. */
final class LogDbBar {
    private LogDbBar() {}

    static HBox build() {
        Label where = lbl("⛁  Log in use: " + LogDbInfo.normalLine(), "jl-emeta");
        where.setMinWidth(0);
        Tooltip.install(where, new Tooltip("Normal-log QSOs are stored here.\n" + LogDbInfo.normalPath()));
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        Label tools = lbl("Database tools ›", "jl-clearbtn"); tools.setStyle("-fx-cursor:hand;");
        tools.setOnMouseClicked(e -> Shell.navigate("setup:logdb"));   // J-Hub ▸ Data: database info, export, upload
        HBox bar = new HBox(10, where, gap);
        bar.getChildren().add(tools);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(0, 18, 6, 18));
        return bar;
    }

    /** The same information as a drawer: which database is in use, Database tools. */
    static javafx.scene.Node drawer() {
        Label where = lbl(LogDbInfo.normalLine(), "jl-emeta"); where.setWrapText(true);
        Tooltip.install(where, new Tooltip(String.valueOf(LogDbInfo.normalPath())));
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(8, where);
        Label tools = lbl("Database tools \u203a", "jl-clearbtn"); tools.setStyle("-fx-cursor:hand;");
        tools.setOnMouseClicked(e -> Shell.navigate("setup:logdb"));
        body.getChildren().add(tools);
        return Shell.drawer("Database", "log", "log", "", false, body);
    }
}
