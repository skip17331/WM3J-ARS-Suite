package com.ars.fx.shell;

import com.ars.fx.AppVersion;
import com.ars.fx.data.UpdateApplier;
import com.ars.fx.data.UpdateChecker;
import com.ars.fx.data.UpdateChecker.Release;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import static com.ars.fx.shell.Shell.lbl;

/**
 * A small "update available" card in the bottom-right corner of the docked window. It is an overlay on the
 * host {@link StackPane}, which {@code Launcher.show()} rebuilds on every navigation — so call
 * {@link #attach} after each rebuild to put the card back.
 */
public final class UpdateBanner {
    private UpdateBanner() {}

    private static HostServices hostServices;
    private static StackPane host;
    private static volatile Release shown;
    private static VBox card;

    /** Start listening for update results. Call once, after the first window is up. */
    public static void install(StackPane hostPane, HostServices hs) {
        host = hostPane; hostServices = hs;
        UpdateChecker.addListener(r -> Platform.runLater(() -> { shown = r; card = build(r); attach(host); }));
        // an earlier update whose helper failed or rolled back leaves a note: say so, once
        String last = UpdateApplier.takeLastResult();
        if (last != null) { card = buildMessage("The last update didn't finish", last); attach(host); }
    }

    private static VBox buildMessage(String title, String text) {
        Label t = lbl(title, "ub-title");
        Label m = lbl(text, "ub-sub"); m.setWrapText(true); m.setMaxWidth(340);
        Label ok = lbl("Dismiss", "ub-link"); ok.setStyle("-fx-cursor:hand;");
        ok.setOnMouseClicked(e -> dismiss());
        VBox c = new VBox(4, t, m, ok);
        c.getStyleClass().add("ub-card");
        c.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(c, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(c, new Insets(0, 16, 16, 0));
        return c;
    }

    /** Re-add the card (if an update is pending) after the host's children were replaced. */
    public static void attach(StackPane hostPane) {
        if (card == null || hostPane == null || hostPane.getChildren().contains(card)) return;
        hostPane.getChildren().add(card);
    }

    private static void dismiss() {
        if (host != null && card != null) host.getChildren().remove(card);
        card = null; shown = null;
    }

    /** Download + verify off the UI thread, then hand over to the helper and exit; on any failure the app keeps running unchanged. */
    private static void apply(Release r, Label now, Label status, HBox actions) {
        actions.setDisable(true);
        status.setText("Downloading " + r.tag() + "…");
        Thread t = new Thread(() -> {
            try {
                java.nio.file.Path jar = UpdateApplier.download(r, p -> Platform.runLater(() -> status.setText("Downloading " + r.tag() + "… " + Math.round(p * 100) + "%")));
                Platform.runLater(() -> status.setText("Restarting to finish the update…"));
                UpdateApplier.launchHelper(jar);
                Platform.runLater(() -> { Platform.exit(); System.exit(0); });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    status.setText("Update failed: " + ex.getMessage());
                    actions.setDisable(false);
                });
            }
        }, "update-apply");
        t.setDaemon(true); t.start();
    }

    private static VBox build(Release r) {
        Label title = lbl("Update available — " + r.tag(), "ub-title");
        String dated = r.publishedDate().isEmpty() ? "" : " This release is dated " + r.publishedDate() + ".";
        Label sub = lbl("You have v" + AppVersion.current() + "." + dated, "ub-sub");
        Label notes = lbl("What's new", "ub-link"); notes.setStyle("-fx-cursor:hand;");
        notes.setOnMouseClicked(e -> { if (hostServices != null && !r.url().isEmpty()) hostServices.showDocument(r.url()); });
        Label skip = lbl("Skip this version", "ub-link"); skip.setStyle("-fx-cursor:hand;");
        skip.setOnMouseClicked(e -> { UpdateChecker.skip(r); dismiss(); });
        Label later = lbl("Later", "ub-link"); later.setStyle("-fx-cursor:hand;");
        later.setOnMouseClicked(e -> dismiss());
        HBox actions = new HBox(14, notes, skip, later); actions.setAlignment(Pos.CENTER_LEFT);
        if (UpdateApplier.supported()) {
            Label now = lbl("Update now", "ub-btn"); now.setStyle("-fx-cursor:hand;");
            now.setOnMouseClicked(e -> apply(r, now, sub, actions));
            actions.getChildren().add(0, now);
        } else if (UpdateApplier.manualReason() != null) {
            sub.setText(UpdateApplier.manualReason()); sub.setWrapText(true); sub.setMaxWidth(340);
        }
        VBox c = new VBox(4, title, sub, actions);
        if (UpdateApplier.isGitCheckout()) {        // a checkout can pull what no release has yet: say how much that is
            Label more = lbl("", "ub-sub"); more.setWrapText(true); more.setMaxWidth(340); more.setManaged(false); more.setVisible(false);
            c.getChildren().add(2, more);
            Thread t = new Thread(() -> {
                String note = UpdateChecker.unreleasedNote(UpdateChecker.unreleasedChanges(r.tag()));
                if (!note.isEmpty()) Platform.runLater(() -> { more.setText(note); more.setManaged(true); more.setVisible(true); });
            }, "update-unreleased");
            t.setDaemon(true); t.start();
        }
        c.getStyleClass().add("ub-card");
        c.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(c, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(c, new Insets(0, 16, 16, 0));
        return c;
    }
}
