package com.wm3j.jmap.ui.panels;

import com.wm3j.jmap.service.config.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Top-bar time display showing UTC, local time, and optional second timezone.
 * Also hosts the ID countdown timer on the left when enabled.
 * All font sizes use em units — scales with the root font size setting.
 */
public class TimePanel extends HBox {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE dd-MMM-yyyy");

    private final Label utcTimeLabel;
    private final Label utcDateLabel;
    private final Label localTimeLabel;
    private final Label localDateLabel;
    private final HBox  utcBlock;
    private final HBox  localBlock;

    // ID timer display (left side)
    private final VBox  timerBlock;
    private final Label timerTimeLabel;
    private final Label timerStatusLabel;

    private Settings settings;

    public TimePanel(Settings settings) {
        this.settings = settings;

        setAlignment(Pos.CENTER);
        setPadding(new Insets(2, 16, 2, 16));
        setSpacing(24);
        setStyle("-fx-background-color: #05050d; -fx-border-color: #1a2a4a; -fx-border-width: 0 0 1 0;");
        setPrefHeight(-1);

        // ── ID timer block (left) ──────────────────────────────────────────
        Label timerIdLabel = new Label("ID TIMER");
        timerIdLabel.setStyle("-fx-font-size: 0.85em; -fx-font-weight: bold; -fx-text-fill: #888899;");

        timerTimeLabel = new Label("10:00");
        timerTimeLabel.setStyle("-fx-font-size: 2.15em; -fx-font-weight: bold; -fx-text-fill: #00cc66;");

        timerStatusLabel = new Label("RUNNING");
        timerStatusLabel.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #666677;");

        timerBlock = new VBox(1);
        timerBlock.setAlignment(Pos.CENTER_LEFT);
        timerBlock.getChildren().addAll(timerIdLabel, timerTimeLabel, timerStatusLabel);
        timerBlock.setVisible(settings.isShowCountdownTimer());
        timerBlock.setManaged(settings.isShowCountdownTimer());

        // ── UTC block ──────────────────────────────────────────────────────
        utcTimeLabel = new Label("--:--:--");
        utcTimeLabel.setStyle("-fx-font-size: 2.15em; -fx-font-weight: bold; -fx-text-fill: #00ccff;");

        utcDateLabel = new Label("---");
        utcDateLabel.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #666677;");

        utcBlock = makeTimeBlock("UTC", "#888899", utcTimeLabel, utcDateLabel);

        // ── Local block ────────────────────────────────────────────────────
        String tz = (settings.getTimezone() == null || settings.getTimezone().isBlank()
                || settings.getTimezone().equals("UTC"))
            ? ZoneId.systemDefault().getId()
            : settings.getTimezone();

        localTimeLabel = new Label("--:--:--");
        localTimeLabel.setStyle("-fx-font-size: 2.15em; -fx-font-weight: bold; -fx-text-fill: #88ff88;");

        localDateLabel = new Label("---");
        localDateLabel.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #666677;");

        localBlock = makeTimeBlock(tz, "#888899", localTimeLabel, localDateLabel);

        // ── Spacers to push clocks center/right ────────────────────────────
        Region spacerL = new Region();
        Region spacerR = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        utcBlock.setVisible(settings.isShowUtcTime());
        utcBlock.setManaged(settings.isShowUtcTime());
        localBlock.setVisible(settings.isShowLocalTime());
        localBlock.setManaged(settings.isShowLocalTime());

        getChildren().addAll(timerBlock, spacerL, utcBlock, spacerR, localBlock);

        update();
    }

    private HBox makeTimeBlock(String tzName, String tzColor, Label timeLabel, Label dateLabel) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(1, 0, 1, 0));

        Label tzLabel = new Label(tzName);
        tzLabel.setStyle("-fx-font-size: 0.85em; -fx-font-weight: bold; -fx-text-fill: " + tzColor + ";");

        VBox vb = new VBox(1);
        vb.setAlignment(Pos.CENTER_LEFT);
        vb.getChildren().addAll(tzLabel, timeLabel, dateLabel);

        box.getChildren().add(vb);
        return box;
    }

    // ── Clock update ──────────────────────────────────────────────────────

    public void update() {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);

        if (settings.isShowUtcTime()) {
            utcTimeLabel.setText(nowUtc.format(TIME_FMT));
            utcDateLabel.setText(nowUtc.format(DATE_FMT));
        }

        if (settings.isShowLocalTime()) {
            ZoneId localZone = resolveZone(settings.getTimezone());
            ZonedDateTime nowLocal = nowUtc.withZoneSameInstant(localZone);
            localTimeLabel.setText(nowLocal.format(TIME_FMT));
            localDateLabel.setText(nowLocal.format(DATE_FMT));
        }
    }

    // ── Timer update (called by DashboardLayout) ──────────────────────────

    /** Update the timer readout. colorHex e.g. "#00cc66". */
    public void updateTimer(String timeStr, String colorHex, String status) {
        timerTimeLabel.setText(timeStr);
        timerTimeLabel.setStyle(
            "-fx-font-size: 2.15em; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        timerStatusLabel.setText(status);
    }

    /** Show/hide the time digit during flash. */
    public void flashTimerText(boolean visible) {
        timerTimeLabel.setVisible(visible);
    }

    // ── Settings change ───────────────────────────────────────────────────

    public void settingsChanged(Settings newSettings) {
        this.settings = newSettings;
        boolean showUtc   = newSettings.isShowUtcTime();
        boolean showLocal = newSettings.isShowLocalTime();
        boolean showTimer = newSettings.isShowCountdownTimer();

        utcBlock.setVisible(showUtc);
        utcBlock.setManaged(showUtc);
        localBlock.setVisible(showLocal);
        localBlock.setManaged(showLocal);
        timerBlock.setVisible(showTimer);
        timerBlock.setManaged(showTimer);

        boolean show = showUtc || showLocal || showTimer;
        setVisible(show);
        setManaged(show);
    }

    private static ZoneId resolveZone(String tz) {
        if (tz == null || tz.isBlank() || tz.equals("UTC")) return ZoneId.systemDefault();
        try { return ZoneId.of(tz); }
        catch (Exception e) {
            try { return ZoneId.of(tz, ZoneId.SHORT_IDS); }
            catch (Exception e2) { return ZoneId.systemDefault(); }
        }
    }
}
