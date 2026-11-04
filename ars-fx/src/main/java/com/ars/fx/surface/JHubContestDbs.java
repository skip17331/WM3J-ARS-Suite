package com.ars.fx.surface;

import com.ars.fx.data.ContestLogs;
import com.ars.fx.data.ContestLogs.Info;
import com.ars.fx.data.LogDbInfo;
import com.ars.fx.shell.Shell;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import static com.ars.fx.shell.Shell.lbl;

/**
 * J-Hub ▸ Data — contest database tools: the databases offered when entering contest mode (each can be archived),
 * and the archive (each can be restored). Archived files live in {@code ~/.j-log/archive/<Contest>_<Year>/}.
 */
final class JHubContestDbs {
    private JHubContestDbs() {}

    static Region build() {
        VBox usableCard = new VBox(); usableCard.getStyleClass().add("cfg-card");
        VBox archCard = new VBox(); archCard.getStyleClass().add("cfg-card");
        Label status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        Runnable[] refresh = new Runnable[1];

        refresh[0] = () -> Shell.keepScroll(usableCard, () -> {
            usableCard.getChildren().clear();
            java.util.List<Info> usable = ContestLogs.usable();
            for (Info i : usable) usableCard.getChildren().add(usableRow(i, status, refresh[0]));
            if (usable.isEmpty()) usableCard.getChildren().add(emptyRow("No contest databases yet — one is created when you start a contest log in J-Log."));

            archCard.getChildren().clear();
            java.util.List<Info> arch = ContestLogs.archived();
            for (Info i : arch) archCard.getChildren().add(archivedRow(i, status, refresh[0]));
            if (arch.isEmpty()) archCard.getChildren().add(emptyRow("Nothing archived."));
            Label open = JHubDashboard.cfgBtn("Open archive folder");
            open.setOnMouseClicked(e -> status.setText(LogDbInfo.openFolder(ContestLogs.archiveRoot())
                    ? "Opened " + LogDbInfo.pretty(ContestLogs.archiveRoot())
                    : "Could not open the folder — it is " + ContestLogs.archiveRoot()));
            HBox foot = new HBox(10, status, new Region(), open); foot.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(foot.getChildren().get(1), Priority.ALWAYS); HBox.setHgrow(status, Priority.SOMETIMES);
            foot.getStyleClass().add("cfg-row"); foot.setStyle("-fx-border-width:0;");
            archCard.getChildren().add(foot);
        });
        refresh[0].run();

        // "Open archive folder" (and "Open folder" above) hand the operator a file manager, where a database can be moved or
        // deleted behind our back. Rebuild both lists whenever this window regains focus, so they match what is on disk.
        javafx.beans.value.ChangeListener<Boolean> onFocus = (o, was, now) -> { if (now) refresh[0].run(); };
        usableCard.sceneProperty().addListener((o, oldScene, newScene) -> {
            if (oldScene != null && oldScene.getWindow() != null) oldScene.getWindow().focusedProperty().removeListener(onFocus);
            if (newScene == null) return;
            if (newScene.getWindow() != null) newScene.getWindow().focusedProperty().addListener(onFocus);
            else newScene.windowProperty().addListener((o2, a, w) -> { if (w != null) w.focusedProperty().addListener(onFocus); });
        });

        return new VBox(20,
                new VBox(6, lbl("CONTEST DATABASES", "cfg-sec"),
                        lbl("Offered when you enter contest mode. Archive one you are finished with to move it into storage.", "cfg-desc"), usableCard),
                new VBox(6, lbl("ARCHIVED CONTEST DATABASES", "cfg-sec"), archCard));
    }

    private static HBox usableRow(Info i, Label status, Runnable refresh) {
        Label archive = JHubDashboard.cfgBtn("Archive");
        archive.setOnMouseClicked(e -> Confirm.show(archive, "Archive " + i.fileName() + "?",
                "It moves to " + LogDbInfo.pretty(ContestLogs.archiveRoot()) + "/<contest>_<year>/ and is no longer offered when you enter contest mode. "
                        + "Its QSOs are kept in the file, and you can restore it from this page."
                        + (i.current() ? " This is the database in use now, so contest mode will ask which one to use next time." : ""),
                new Confirm.Choice("Cancel", false, null),
                new Confirm.Choice("Archive", true, () -> {
                    try { status.setText("✓ Archived to " + LogDbInfo.pretty(ContestLogs.archive(i.path()))); }
                    catch (Exception ex) { status.setText("✗ Could not archive: " + ex.getMessage()); }
                    if (i.current()) Shell.navigate("setup:logdb");   // the "in use" summary above must change too, so rebuild the page
                    else refresh.run();
                })));
        return row(i.fileName() + (i.current() ? "  · in use" : ""), meta(i), archive);
    }

    private static HBox archivedRow(Info i, Label status, Runnable refresh) {
        Label restore = JHubDashboard.cfgBtn("Restore");
        restore.setOnMouseClicked(e -> {
            try { status.setText("✓ Restored to " + LogDbInfo.pretty(ContestLogs.restore(i.path()))); }
            catch (Exception ex) { status.setText("✗ Could not restore: " + ex.getMessage()); }
            refresh.run();
        });
        String dir = i.path().getParent() == null ? "" : i.path().getParent().getFileName() + "/";
        return row(dir + i.fileName(), meta(i), restore);
    }

    private static HBox row(String title, String meta, Label action) {
        VBox txt = new VBox(2, lbl(title, "cfg-lt"), lbl(meta, "cfg-lh"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox r = new HBox(20, txt, sp, action); r.getStyleClass().add("cfg-row"); r.setAlignment(Pos.CENTER_LEFT);
        return r;
    }

    private static HBox emptyRow(String text) {
        HBox r = new HBox(lbl(text, "cfg-lh")); r.getStyleClass().add("cfg-row"); r.setAlignment(Pos.CENTER_LEFT);
        return r;
    }

    private static String meta(Info i) {
        return (i.contestName().isBlank() ? "no contest yet" : i.contestName())
                + " · started " + (i.started().isBlank() ? "unknown" : i.started())
                + " · " + i.qsos() + (i.qsos() == 1 ? " QSO" : " QSOs");
    }
}
