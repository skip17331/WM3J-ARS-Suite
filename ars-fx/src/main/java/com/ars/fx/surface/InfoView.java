package com.ars.fx.surface;

import com.ars.fx.data.HubConfig;
import com.ars.fx.data.UpcomingEvents;
import com.ars.fx.data.UpcomingEvents.Event;
import com.ars.fx.data.UpcomingEvents.Kind;
import com.ars.fx.data.UpcomingEvents.Loaded;
import com.ars.fx.shell.Shell;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Info: what is happening in the hobby. This first page is "Upcoming" - one tile each for contests, DXpeditions and
 * hamfests, soonest first, with ongoing events marked NOW. A shared range and search word narrow all three tiles.
 * Feeds are in {@link UpcomingEvents}.
 */
public final class InfoView {
    private InfoView() {}

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);

    /** One tile: its list and the labels that change. */
    private static final class Tile {
        final Kind kind;
        final VBox card = new VBox();
        final VBox rows = new VBox();
        final Label count = lbl("", "jhub-card-sub");
        final Label credit = lbl("", "jhub-card-sub");
        Tile(Kind kind) { this.kind = kind; }
    }

    public static Region build() {
        Region dock = Shell.dock("info");
        HBox top = Shell.topBar("info", "info", "Info", "Upcoming", new String[][]{}, null);

        List<Event> all = new ArrayList<>();
        Map<Kind, Loaded> loads = new EnumMap<>(Kind.class);
        Label status = lbl("", "jl-emeta");
        Map<Kind, Tile> tiles = new EnumMap<>(Kind.class);
        for (Kind k : Kind.values()) tiles.put(k, new Tile(k));

        ComboBox<String> range = new ComboBox<>();
        range.getItems().addAll("Next 7 days", "Next 30 days", "Next 90 days", "Everything");
        range.setValue(HubConfig.get("info.range", "Next 30 days"));
        if (!range.getItems().contains(range.getValue())) range.setValue("Next 30 days");
        TextField search = new TextField(); search.setPromptText("Search all tiles…"); search.setPrefWidth(220);

        Runnable[] load = new Runnable[1];

        Runnable render = () -> {
            int days = switch (range.getValue()) { case "Next 7 days" -> 7; case "Next 30 days" -> 30; case "Next 90 days" -> 90; default -> 0; };
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            for (Tile t : tiles.values()) {
                List<Event> shown = UpcomingEvents.upcoming(all, today, days, EnumSet.of(t.kind), search.getText());
                t.count.setText(String.valueOf(shown.size()));
                t.rows.getChildren().clear();
                Loaded l = loads.get(t.kind);
                if (shown.isEmpty()) {
                    t.rows.getChildren().add(empty(t.kind, l, all.isEmpty() && loads.isEmpty(), () -> load[0].run()));
                } else {
                    for (Event e : shown) t.rows.getChildren().add(row(e, today));
                }
            }
        };

        boolean[] busy = {false};
        load[0] = () -> {
            if (busy[0]) return;
            busy[0] = true;
            runLoad(false, all, loads, tiles, status, render, () -> busy[0] = false);
        };
        Label refresh = lbl("⟳ Refresh", "jl-clearbtn"); refresh.setStyle("-fx-cursor:hand;");
        refresh.setOnMouseClicked(e -> { if (!busy[0]) { busy[0] = true; runLoad(true, all, loads, tiles, status, render, () -> busy[0] = false); } });
        Label sources = lbl("Sources…", "jl-clearbtn"); sources.setStyle("-fx-cursor:hand;");
        sources.setOnMouseClicked(e -> editSources(sources, () -> load[0].run()));

        range.setOnAction(e -> { HubConfig.set("info.range", range.getValue()); render.run(); });
        search.textProperty().addListener((o, a, b) -> render.run());

        // ---- tiles in a three-column grid ----
        GridPane grid = new GridPane(); grid.setHgap(16);
        int col = 0;
        for (Tile t : tiles.values()) {
            build(t);
            ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(100.0 / 3); cc.setHgrow(Priority.ALWAYS); cc.setMinWidth(260);
            grid.getColumnConstraints().add(cc);
            grid.add(t.card, col++, 0);
            javafx.scene.layout.GridPane.setVgrow(t.card, Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillHeight(t.card, true);
        }
        grid.setMinWidth(0);
        javafx.scene.layout.RowConstraints rc = new javafx.scene.layout.RowConstraints(); rc.setVgrow(Priority.ALWAYS); rc.setFillHeight(true);
        grid.getRowConstraints().add(rc);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(12, lbl("Upcoming", "jhub-card-title"), range, search, sp, status, refresh, sources);
        bar.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(grid, Priority.ALWAYS);
        VBox page = new VBox(14, bar, grid);
        page.setPadding(new Insets(16, 22, 18, 22)); page.setStyle("-fx-background-color:-ars-bg;");
        page.setMinWidth(0);
        HBox.setHgrow(page, Priority.ALWAYS);

        Region frame = Shell.frame(dock, top, page, null);
        render.run();
        load[0].run();
        return frame;
    }

    /** Header (colour dot, name, count, source credit) over a scrolling list. */
    private static void build(Tile t) {
        String hue = switch (t.kind) { case CONTEST -> "-ars-log"; case DXPEDITION -> "-ars-map"; case HAMFEST -> "-ars-sat"; };
        Region dot = new Region(); dot.setMinSize(10, 10); dot.setMaxSize(10, 10); dot.setStyle("-fx-background-color:" + hue + ";-fx-background-radius:5;");
        Label name = lbl(t.kind.label, "jhub-card-title");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        t.count.setStyle("-fx-font-weight:bold;-fx-text-fill:" + hue + ";-fx-font-size:14px;");
        HBox head = new HBox(9, dot, name, gap, t.count); head.getStyleClass().add("jhub-card-head");
        t.credit.setPadding(new Insets(0, 16, 8, 16)); t.credit.setWrapText(true);
        ScrollPane sp = new ScrollPane(t.rows); sp.setFitToWidth(true); sp.setStyle("-fx-background-color:transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        t.rows.setPadding(new Insets(4, 6, 8, 6));
        VBox.setVgrow(sp, Priority.ALWAYS);
        t.card.getStyleClass().add("jhub-card");
        t.card.getChildren().addAll(head, sp, t.credit);
        t.card.setMinWidth(0);
    }

    /** A compact row: date on the left, name over detail on the right, ongoing marked NOW. */
    private static Node row(Event e, LocalDate today) {
        boolean now = !e.start().isAfter(today);
        Label date = lbl(now ? "NOW" : DAY.format(e.start()), "jl-eyebrow");
        date.setMinWidth(54); date.setPrefWidth(54);
        date.setStyle(now ? "-fx-text-fill:-ars-ok;-fx-font-weight:bold;" : "-fx-text-fill:-ars-t2;-fx-font-weight:bold;");
        Label name = lbl(e.title(), "jhub-card-title"); name.setTextOverrun(OverrunStyle.ELLIPSIS); name.setMaxWidth(Double.MAX_VALUE);
        String until = e.start().equals(e.end()) ? "" : (now ? "until " : SHORT.format(e.start()) + " – ") + SHORT.format(e.end());
        String sub = (until + (e.time().isEmpty() ? "" : (until.isEmpty() ? "" : "  ") + e.time())).trim();
        Label dates = lbl(sub, "jl-emeta");
        VBox text = new VBox(1, name);
        if (!sub.isEmpty()) text.getChildren().add(dates);
        if (e.detail() != null && !e.detail().isBlank()) {
            Label d = lbl(e.detail(), "jl-emeta"); d.setTextOverrun(OverrunStyle.ELLIPSIS); d.setMaxWidth(Double.MAX_VALUE);
            text.getChildren().add(d);
        }
        HBox.setHgrow(text, Priority.ALWAYS); text.setMinWidth(0);
        HBox r = new HBox(8, date, text); r.setAlignment(Pos.TOP_LEFT); r.setPadding(new Insets(7, 10, 7, 10));
        r.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;");
        StringBuilder tip = new StringBuilder(e.title());
        if (!sub.isEmpty()) tip.append("\n").append(sub);
        if (e.detail() != null && !e.detail().isBlank()) tip.append("\n").append(e.detail());
        if (e.location() != null && !e.location().isBlank()) tip.append("\n").append(e.location());
        String url = e.url();
        if (url != null && UpcomingEvents.validUrl(url)) {
            tip.append("\n(click to open the event page)");
            r.setStyle(r.getStyle() + "-fx-cursor:hand;");
            r.setOnMouseClicked(ev -> open(url));
        }
        javafx.scene.control.Tooltip.install(r, new javafx.scene.control.Tooltip(tip.toString()));
        return r;
    }

    private static Node empty(Kind kind, Loaded l, boolean loading, Runnable reload) {
        String msg;
        if (loading) msg = "Loading…";
        else if (l == null) msg = "Turned off under Sources…";
        else if (!l.source().configured())
            msg = kind == Kind.CONTEST ? "No contest calendar yet. Add your own .ics link under Sources…" : "No hamfest calendar yet. Add a .ics link under Sources…";
        else if (l != null && l.error() != null && l.events().isEmpty()) msg = l.error();
        else msg = "Nothing in this range.";
        Label m = lbl(msg, "jl-emeta"); m.setWrapText(true); m.setPadding(new Insets(14, 10, 14, 10));
        return m;
    }

    private static void runLoad(boolean force, List<Event> all, Map<Kind, Loaded> loads, Map<Kind, Tile> tiles, Label status, Runnable render, Runnable done) {
        status.setText("Updating…");
        Thread t = new Thread(() -> {
            List<Loaded> got = new ArrayList<>();
            for (UpcomingEvents.Source s : UpcomingEvents.sources()) got.add(UpcomingEvents.load(s, force, Instant.now()));
            Platform.runLater(() -> {
                all.clear(); loads.clear();
                String problem = "";
                for (Loaded l : got) {
                    loads.put(l.source().kind(), l);
                    all.addAll(l.events());
                    if (l.error() != null && l.source().configured()) problem = l.source().kind().label + ": " + l.error();
                }
                for (Tile tile : tiles.values()) credit(tile, loads.get(tile.kind));
                status.setText(problem);
                status.setStyle(problem.isEmpty() ? "" : "-fx-text-fill:-ars-warn;");
                render.run();
                done.run();
            });
        }, "info-feeds");
        t.setDaemon(true); t.start();
    }

    /** "Source name - updated ..." under a tile, the source name a link back to the site. */
    private static void credit(Tile t, Loaded l) {
        if (l == null || !l.source().configured()) { t.credit.setText(""); t.credit.setOnMouseClicked(null); t.credit.setStyle(""); return; }
        String when = l.fetched() == null ? "" : (l.fromCache() ? "  ·  saved copy from " : "  ·  updated ")
                + DateTimeFormatter.ofPattern("d MMM HH:mm'Z'", Locale.ENGLISH).format(l.fetched().atZone(ZoneOffset.UTC));
        t.credit.setText(l.source().label() + when);
        String u = l.source().creditUrl();
        if (u != null && !u.isBlank()) {
            t.credit.setStyle("-fx-cursor:hand;-fx-underline:true;");
            t.credit.setOnMouseClicked(e -> open(u));
        } else { t.credit.setStyle(""); t.credit.setOnMouseClicked(null); }
    }

    private static void open(String url) {
        try { java.awt.Desktop.getDesktop().browse(java.net.URI.create(url)); } catch (Throwable ignored) {}
    }

    // ---- Sources dialog ------------------------------------------------------------------------------
    private static void editSources(Node owner, Runnable onSaved) {
        javafx.scene.control.CheckBox dxOn = new javafx.scene.control.CheckBox("Show DXpeditions (NG3K Announced DX Operations)");
        dxOn.setSelected(HubConfig.getBool("info.dx.enabled", true));
        TextField dx = new TextField(HubConfig.get("info.dx.url", "")); dx.setPromptText("default: " + UpcomingEvents.DX_DEFAULT);
        TextField contests = new TextField(HubConfig.get("info.contests.url", "")); contests.setPromptText("https://… your contest calendar (.ics link)");
        TextField fests = new TextField(HubConfig.get("info.hamfests.url", "")); fests.setPromptText("https://… a hamfest calendar (.ics link)");
        Label note = lbl("Contests and hamfests come from an iCalendar (.ics) link you provide. The WA7BNM Contest Calendar permits its iCal feed for "
                + "personal calendar use; paste your own link from contestcalendar.com. Feeds are fetched at most every six hours and kept on disk.", "jl-emeta");
        note.setWrapText(true); note.setMaxWidth(520);
        Label msg = lbl("", "jl-emeta"); msg.setStyle("-fx-text-fill:-ars-err;");
        Label cancel = lbl("Cancel", "jl-clearbtn"); cancel.setStyle("-fx-cursor:hand;");
        Label save = lbl("Save", "jl-logbtn"); save.setStyle("-fx-cursor:hand;");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox btns = new HBox(14, msg, gap, cancel, save); btns.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(8, lbl("Sources", "jhub-card-title"), dxOn, lbl("DXPEDITION CALENDAR (leave blank for the default)", "jl-flabel"), dx,
                lbl("CONTEST CALENDAR", "jl-flabel"), contests, lbl("HAMFEST CALENDAR", "jl-flabel"), fests, note, btns);
        box.setPadding(new Insets(16)); box.setPrefWidth(560);
        javafx.scene.Scene scene = new javafx.scene.Scene(box);
        com.ars.fx.Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        com.ars.fx.Themes.applyTo(box, "info");
        Stage st = new Stage(); st.setTitle("Info sources"); st.setScene(scene); st.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        cancel.setOnMouseClicked(e -> st.close());
        save.setOnMouseClicked(e -> {
            for (TextField f : new TextField[]{dx, contests, fests})
                if (!f.getText().isBlank() && !UpcomingEvents.validUrl(f.getText())) { msg.setText("Addresses must start with https://"); return; }
            HubConfig.setBool("info.dx.enabled", dxOn.isSelected());
            HubConfig.set("info.dx.url", dx.getText().trim());
            HubConfig.set("info.contests.url", contests.getText().trim());
            HubConfig.set("info.hamfests.url", fests.getText().trim());
            st.close(); onSaved.run();
        });
        st.show();
    }
}
