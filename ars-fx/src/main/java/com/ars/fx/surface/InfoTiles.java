package com.ars.fx.surface;

import com.ars.fx.data.ActiveNow;
import com.ars.fx.data.DayReference;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.StationStats;
import com.ars.fx.data.UpcomingEvents;
import com.ars.fx.shell.Shell;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The Info page's third row (punch list 5-6): Active now, Your stats and Reference. Each fills itself in the background and on
 * {@link #refresh(boolean)}; none of them changes the tiles above it.
 */
final class InfoTiles {
    private InfoTiles() {}

    private static VBox activeRows, statsRows, refRows;
    private static Label activeCount;
    private static Supplier<List<UpcomingEvents.Event>> events = List::of;

    static VBox activeTile(Supplier<List<UpcomingEvents.Event>> calendar) {
        events = calendar;
        activeRows = new VBox(); activeCount = lbl("", "jhub-card-sub");
        return card("Active now", "-ars-ok", activeCount, activeRows,
                "POTA activators spotted in the last hour (from pota.app), and DXpeditions on the air today. NEW ONE / NEW PARK: your log still needs it. Click one to tune the rig and fill J-Log-Quick on the dashboard.");
    }

    static VBox statsTile() {
        statsRows = new VBox();
        return card("Your stats", "-ars-log", null, statsRows, "From your normal log. Confirmations count each QSO once per service.");
    }

    static VBox referenceTile() {
        refRows = new VBox();
        return card("Reference", "-ars-sat", null, refRows, "Sunrise and sunset at your station (UTC), your licence renewal and a J-Learn section for today.");
    }

    /** Fill all three, in the background. {@code force} refetches the activator lists now. */
    static void refresh(boolean force) {
        List<UpcomingEvents.Event> cal = events.get();                  // read the calendar here, on the FX thread that fills it
        Thread t = new Thread(() -> {
            fillActive(force, cal);
            fillStats();
            fillReference();
        }, "info-tiles");
        t.setDaemon(true);
        t.start();
    }

    // ---- Active now ---------------------------------------------------------------------------------------------------------
    private static void fillActive(boolean force, List<UpcomingEvents.Event> cal) {
        List<ActiveNow.Activation> acts;
        try { acts = ActiveNow.fetch(force); } catch (RuntimeException e) { acts = List.of(); }
        List<UpcomingEvents.Event> dx = ActiveNow.dxpeditionsToday(cal, LocalDate.now(ZoneOffset.UTC));
        List<ActiveNow.Activation> shown = acts;
        Platform.runLater(() -> {
            if (activeRows == null) return;
            activeRows.getChildren().clear();
            for (UpcomingEvents.Event e : dx) activeRows.getChildren().add(row("DX", e.title(), e.detail() == null ? "" : e.detail(), "", null));
            Instant now = Instant.now();
            for (ActiveNow.Activation a : shown) {
                String age = a.spotted() == null ? "" : Duration.between(a.spotted(), now).toMinutes() + "m";
                String where = (a.ref() + "  " + a.refName()).trim();
                String freq = a.freqKhz() > 0 ? String.format(java.util.Locale.ROOT, "%.1f", a.freqKhz()) + " " + a.mode() : a.mode();
                activeRows.getChildren().add(row(a.program(), a.call() + "  " + freq + "  " + age, where, a.need(), a));
            }
            if (activeRows.getChildren().isEmpty()) activeRows.getChildren().add(note("Nobody on the air just now, or the lists could not be read."));
            activeCount.setText(shown.size() + " activators" + (dx.isEmpty() ? "" : " · " + dx.size() + " DXpeditions"));
        });
    }

    private static Node row(String tag, String main, String sub, String need, ActiveNow.Activation a) {
        Label t = lbl(tag, "jl-emeta"); t.setMinWidth(38);
        Label m = lbl(main, "jhub-card-title"); m.setTextOverrun(OverrunStyle.ELLIPSIS); m.setMaxWidth(Double.MAX_VALUE);
        Label s = lbl(sub, "jl-emeta"); s.setTextOverrun(OverrunStyle.ELLIPSIS); s.setMaxWidth(Double.MAX_VALUE);
        VBox text = new VBox(1, m, s); text.setMinWidth(0); HBox.setHgrow(text, Priority.ALWAYS);
        HBox r = new HBox(8, t, text);
        if (need != null && !need.isEmpty()) {
            Label n = lbl(need, "jl-emeta"); n.setStyle("-fx-text-fill:-ars-ok;-fx-font-weight:bold;");
            r.getChildren().add(n);
        }
        r.setAlignment(Pos.CENTER_LEFT); r.setPadding(new Insets(7, 10, 7, 10));
        r.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;" + (a != null ? "-fx-cursor:hand;" : ""));
        if (a != null) {
            long hz = Math.round(a.freqKhz() * 1000);
            r.setOnMouseClicked(e -> {
                SpotAction.onSpot(r, a.call(), hz, a.mode(), com.ars.fx.data.Bands.of(hz));   // tunes, and queues the call for the log
                Shell.navigate("hub");                                                          // the dashboard's J-Log-Quick takes it
            });
        }
        return r;
    }

    // ---- Your stats ---------------------------------------------------------------------------------------------------------
    private static void fillStats() {
        StationStats.Stats s;
        try { s = StationStats.now(); } catch (RuntimeException e) { s = null; }
        StationStats.Stats st = s;
        Platform.runLater(() -> {
            if (statsRows == null) return;
            statsRows.getChildren().clear();
            if (st == null) { statsRows.getChildren().add(note("The log could not be read.")); return; }
            statsRows.getChildren().addAll(
                    line("QSOs", st.today() + " today · " + st.thisMonth() + " this month · " + st.thisYear() + " this year · " + st.total() + " in all"),
                    line("DXCC", st.dxccWorked() + " worked · " + st.dxccConfirmed() + " confirmed · " + st.newEntitiesThisMonth() + " new this month"),
                    line("Confirmed", st.lotwConfirmed() + " LoTW · " + st.eqslConfirmed() + " eQSL · " + st.qslConfirmed() + " QSL"),
                    line("To upload", st.lotwPending() + " to LoTW · " + st.eqslPending() + " to eQSL"));
        });
    }

    // ---- Reference ----------------------------------------------------------------------------------------------------------
    private static void fillReference() {
        double[] loc = HubConfig.location();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        DayReference.SunTimes sun = loc == null ? null : DayReference.sun(loc[0], loc[1], today);
        String licence = "";
        try { licence = DayReference.licenceReminder(com.ars.fx.data.JVaultDb.licenseInfo().expires(), today); } catch (RuntimeException ignored) { }
        String tip = DayReference.tipOfTheDay(DayReference.learnTitles(com.ars.fx.data.LearnContent.dir()), today.getDayOfYear());
        String lic = licence;
        Platform.runLater(() -> {
            if (refRows == null) return;
            refRows.getChildren().clear();
            if (loc == null) refRows.getChildren().add(line("Sun", HubConfig.LOCATION_HINT));
            else if (sun == null) refRows.getChildren().add(line("Sun", "no sunrise or sunset at your station today"));
            else {
                refRows.getChildren().add(line("Sun", "rise " + DayReference.hhmm(sun.rise()) + "Z · set " + DayReference.hhmm(sun.set()) + "Z"));
                refRows.getChildren().add(line("Greyline", DayReference.greyline(sun.rise()) + " and " + DayReference.greyline(sun.set())));
            }
            if (!lic.isEmpty()) refRows.getChildren().add(line("Licence", lic));
            if (tip != null) {
                Node r = line("J-Learn", tip + "  ›");
                r.setStyle(r.getStyle() + "-fx-cursor:hand;");
                r.setOnMouseClicked(e -> Shell.navigate("learn"));
                com.ars.fx.shell.Tips.tip(r, "Today's J-Learn section: open J-Learn and find it in the chapter list");
                refRows.getChildren().add(r);
            }
        });
    }

    // ---- shared -------------------------------------------------------------------------------------------------------------
    private static VBox card(String title, String hue, Label count, VBox rows, String tip) {
        Region dot = new Region(); dot.setMinSize(10, 10); dot.setMaxSize(10, 10); dot.setStyle("-fx-background-color:" + hue + ";-fx-background-radius:5;");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = new HBox(9, dot, lbl(title, "jhub-card-title"), gap);
        if (count != null) head.getChildren().add(count);
        head.getStyleClass().add("jhub-card-head"); head.setAlignment(Pos.CENTER_LEFT);
        com.ars.fx.shell.Tips.tip(head, tip);
        rows.setPadding(new Insets(4, 6, 8, 6));
        rows.getChildren().add(note("Loading…"));
        ScrollPane sp = new ScrollPane(rows); sp.setFitToWidth(true); sp.setStyle("-fx-background-color:transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox card = new VBox(head, sp); card.getStyleClass().add("jhub-card"); card.setMinWidth(0);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return card;
    }

    private static Node line(String label, String value) {
        Label k = lbl(label, "jl-emeta"); k.setMinWidth(80);
        Label v = lbl(value, "jhub-card-title"); v.setWrapText(true); v.setMaxWidth(Double.MAX_VALUE);
        HBox r = new HBox(8, k, v); HBox.setHgrow(v, Priority.ALWAYS);
        r.setAlignment(Pos.TOP_LEFT); r.setPadding(new Insets(7, 10, 7, 10));
        r.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;");
        return r;
    }

    private static Label note(String s) { Label l = lbl(s, "jl-emeta"); l.setWrapText(true); l.setPadding(new Insets(8, 10, 8, 10)); return l; }
}
