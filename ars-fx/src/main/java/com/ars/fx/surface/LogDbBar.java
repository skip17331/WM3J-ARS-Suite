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

    static HBox build(boolean contest) {
        Label where = lbl("⛁  Log in use: " + (contest ? LogDbInfo.contestLine() : LogDbInfo.normalLine()), "jl-emeta");
        where.setMinWidth(0);
        Tooltip.install(where, new Tooltip(contest
                ? "Contest QSOs are stored in a separate database from your normal log.\n" + LogDbInfo.contestPath()
                : "Normal-log QSOs are stored here.\n" + LogDbInfo.normalPath()));
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        Label tools = lbl("Database tools ›", "jl-clearbtn"); tools.setStyle("-fx-cursor:hand;");
        tools.setOnMouseClicked(e -> Shell.navigate("hubdata"));   // J-Hub ▸ Data: database info, export, upload
        HBox bar = new HBox(10, where, gap);
        if (contest) {
            Label sw = lbl("Switch database", "jl-clearbtn"); sw.setStyle("-fx-cursor:hand;");
            sw.setOnMouseClicked(e -> JLogContest.switchDatabase(sw));
            bar.getChildren().add(sw);
        }
        bar.getChildren().add(tools);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(0, 18, 6, 18));
        return bar;
    }
}
