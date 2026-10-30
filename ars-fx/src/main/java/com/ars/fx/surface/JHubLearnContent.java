package com.ars.fx.surface;

import com.ars.fx.data.LearnContent;
import com.ars.fx.data.LogDbInfo;
import com.ars.fx.shell.Shell;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import static com.ars.fx.shell.Shell.lbl;

/**
 * J-Hub ▸ Data ▸ J-LEARN CONTENT — where the reference library's text comes from and how to refresh it. The chapters sit in
 * {@code ~/.j-learn/content/}, apart from the program, so they can be updated (and edited) on their own: pick a source and
 * press Update files, or restore the copy that ships with the app.
 */
final class JHubLearnContent {
    private JHubLearnContent() {}

    static VBox build() {
        Label state = lbl("", "cfg-lt"), status = lbl("", "cfg-lh");
        status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        Runnable paint = () -> state.setText(LearnContent.fileCount() + " files · " + LearnContent.origin());

        TextField src = new TextField(LearnContent.source()); src.getStyleClass().add("jhub-mini-field");
        src.setMinWidth(380); src.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(src, Priority.ALWAYS);
        src.setPromptText(LearnContent.DEFAULT_SOURCE);
        src.focusedProperty().addListener((o, was, now) -> { if (!now) LearnContent.setSource(src.getText()); });

        Label update = JHubDashboard.cfgBtn("Update files"), restore = JHubDashboard.cfgBtn("Restore bundled copy"), open = JHubDashboard.cfgBtn("Open folder");
        boolean[] busy = {false};
        update.setOnMouseClicked(e -> {
            if (busy[0]) return; busy[0] = true; LearnContent.setSource(src.getText());
            status.setText("Working …");
            Thread t = new Thread(() -> {
                LearnContent.Result r = LearnContent.update(src.getText(), m -> Platform.runLater(() -> status.setText(m)));
                Platform.runLater(() -> { busy[0] = false; paint.run(); status.setText((r.ok() ? "✓ " : "✗ ") + r.message()
                        + (r.ok() ? ". The old files are kept in " + LogDbInfo.pretty(LearnContent.root().resolve("content.backup")) + "." : "")); });
            }, "learn-update");
            t.setDaemon(true); t.start();
        });
        restore.setOnMouseClicked(e -> Confirm.show(restore, "Restore the bundled J-Learn content?",
                "Puts back the chapters that came with this version of the app. Your current folder is kept as content.backup, so edits you made can be recovered.",
                new Confirm.Choice("Cancel", false, null),
                new Confirm.Choice("Restore", true, () -> {
                    LearnContent.Result r = LearnContent.restoreBundled(); paint.run(); status.setText((r.ok() ? "✓ " : "✗ ") + r.message());
                })));
        open.setOnMouseClicked(e -> status.setText(LogDbInfo.openFolder(LearnContent.dir()) ? "Opened " + LogDbInfo.pretty(LearnContent.dir())
                : "Could not open the folder — it is " + LearnContent.dir()));
        com.ars.fx.shell.Tips.tip(update, "Download the chapters from the source above and replace the folder (the old one is kept as a backup)");
        com.ars.fx.shell.Tips.tip(restore, "Put back the chapters that shipped with this version of the app");
        paint.run();

        HBox btns = new HBox(10, update, restore, open); btns.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox actions = new HBox(20, new VBox(2, lbl("Refresh the reference library", "cfg-lt"),
                lbl("Use after the chapters are updated on GitHub, or to undo local edits", "cfg-lh")), sp, btns);
        actions.getStyleClass().add("cfg-row"); actions.setAlignment(Pos.CENTER_LEFT);
        HBox statusRow = new HBox(status); statusRow.getStyleClass().add("cfg-row"); statusRow.setStyle("-fx-border-width:0;");
        return JHubDashboard.section("J-LEARN CONTENT",
            JHubDashboard.cfgRow("Chapters on disk", LogDbInfo.pretty(LearnContent.dir()), state),
            JHubDashboard.cfgRow("Update source", "a .zip URL (GitHub archive or a content-only zip), a local .zip, or a folder", src),
            actions, statusRow);
    }
}
