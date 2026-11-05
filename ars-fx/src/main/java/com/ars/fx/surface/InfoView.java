package com.ars.fx.surface;

import com.ars.fx.data.HubConfig;
import com.ars.fx.data.NewsFeeds;
import com.ars.fx.data.UpcomingEvents;
import com.ars.fx.data.UpcomingEvents.Event;
import com.ars.fx.data.UpcomingEvents.Kind;
import com.ars.fx.data.UpcomingEvents.Loaded;
import com.ars.fx.shell.Shell;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
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
 * hamfests, soonest first, with ongoing events marked NOW - and a News tile of ham radio headlines along the bottom.
 * A shared range (calendars) and search word (all tiles) narrow what is shown. Feeds are in {@link UpcomingEvents}
 * and {@link NewsFeeds}.
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

        // ---- the News tile ----
        List<NewsFeeds.Headline> headlines = new ArrayList<>();
        Map<String, String> newsProblems = new java.util.LinkedHashMap<>();
        Instant[] newsAt = {null};
        boolean[] newsLoading = {true};
        VBox newsRows = new VBox(); Label newsCount = lbl("", "jhub-card-sub"); Label newsNote = lbl("", "jhub-card-sub");
        ComboBox<String> newsSource = new ComboBox<>(); newsSource.setValue("All sources");

        ComboBox<String> range = new ComboBox<>();
        range.getItems().addAll("Next 7 days", "Next 30 days", "Next 90 days", "Everything");
        range.setValue(HubConfig.get("info.range", "Next 30 days"));
        if (!range.getItems().contains(range.getValue())) range.setValue("Next 30 days");
        TextField search = new TextField(); search.setPromptText("Search all tiles…"); search.setPrefWidth(220);

        Runnable[] load = new Runnable[1];

        Runnable renderNews = () -> {
            String src = "All sources".equals(newsSource.getValue()) ? null : newsSource.getValue();
            List<NewsFeeds.Headline> shown = NewsFeeds.latest(headlines, src, search.getText(), NewsFeeds.TOTAL);
            newsCount.setText(String.valueOf(shown.size()));
            newsRows.getChildren().clear();
            Instant now = Instant.now();
            if (shown.isEmpty()) {
                String m = newsLoading[0] ? "Loading headlines\u2026" : NewsFeeds.active().isEmpty()
                        ? "No news feeds are switched on. Choose some under Feeds\u2026" : "No headlines match.";
                Label l = lbl(m, "jl-emeta"); l.setPadding(new Insets(14, 10, 14, 10));
                newsRows.getChildren().add(l);
            } else {
                for (NewsFeeds.Headline h : shown) newsRows.getChildren().add(newsRow(h, now));
            }
            String when = newsAt[0] == null ? "" : "updated " + DateTimeFormatter.ofPattern("d MMM HH:mm'Z'", Locale.ENGLISH).format(newsAt[0].atZone(ZoneOffset.UTC)) + "  \u00b7  ";
            String bad = newsProblems.isEmpty() ? "" : "  \u00b7  " + newsProblems.size() + (newsProblems.size() == 1 ? " feed" : " feeds") + " had a problem: "
                    + String.join("; ", newsProblems.entrySet().stream().map(e -> e.getKey() + " (" + e.getValue() + ")").toList());
            newsNote.setText(when + "headlines link to each source site" + bad);
            newsNote.setStyle(newsProblems.isEmpty() ? "" : "-fx-text-fill:-ars-warn;");
        };

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
        boolean[] newsBusy = {false};
        java.util.function.Consumer<Boolean> runNews = force -> {
            if (newsBusy[0]) return;
            newsBusy[0] = true; newsLoading[0] = true;
            Thread t = new Thread(() -> {
                List<NewsFeeds.Feed> feeds = NewsFeeds.active();
                NewsFeeds.Result r = NewsFeeds.load(feeds, force, Instant.now());
                Platform.runLater(() -> {
                    headlines.clear(); headlines.addAll(r.headlines());
                    newsProblems.clear(); newsProblems.putAll(r.problems());
                    newsAt[0] = r.fetched();
                    String keep = newsSource.getValue();
                    newsSource.getItems().setAll("All sources");
                    for (NewsFeeds.Feed f : feeds) newsSource.getItems().add(f.name());
                    newsSource.setValue(newsSource.getItems().contains(keep) ? keep : "All sources");
                    newsLoading[0] = false; newsBusy[0] = false;
                    renderNews.run();
                });
            }, "info-news");
            t.setDaemon(true); t.start();
        };
        load[0] = () -> {
            runNews.accept(false);
            if (busy[0]) return;
            busy[0] = true;
            runLoad(false, all, loads, tiles, status, render, () -> busy[0] = false);
        };
        Label refresh = lbl("⟳ Refresh", "jl-clearbtn"); refresh.setStyle("-fx-cursor:hand;");
        refresh.setOnMouseClicked(e -> {
            runNews.accept(true);
            if (!busy[0]) { busy[0] = true; runLoad(true, all, loads, tiles, status, render, () -> busy[0] = false); }
        });
        Label sources = lbl("Sources…", "jl-clearbtn"); sources.setStyle("-fx-cursor:hand;");
        sources.setOnMouseClicked(e -> editSources(sources, () -> load[0].run()));

        range.setOnAction(e -> { HubConfig.set("info.range", range.getValue()); render.run(); });
        search.textProperty().addListener((o, a, b) -> { render.run(); renderNews.run(); });
        newsSource.setOnAction(e -> renderNews.run());

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

        // ---- the News tile spans the bottom ----
        Region ndot = new Region(); ndot.setMinSize(10, 10); ndot.setMaxSize(10, 10); ndot.setStyle("-fx-background-color:-ars-digi;-fx-background-radius:5;");
        newsCount.setStyle("-fx-font-weight:bold;-fx-text-fill:-ars-digi;-fx-font-size:14px;");
        Label feedsLink = lbl("Feeds\u2026", "jl-clearbtn"); feedsLink.setStyle("-fx-cursor:hand;");
        feedsLink.setOnMouseClicked(e -> editSources(feedsLink, () -> load[0].run()));
        Region ngap = new Region(); HBox.setHgrow(ngap, Priority.ALWAYS);
        Label request = mailLink("Request a change", "Info feeds: add or remove a source", 12);
        HBox nhead = new HBox(12, ndot, lbl("News", "jhub-card-title"), newsSource, ngap, newsCount, request, feedsLink);
        nhead.getStyleClass().add("jhub-card-head");
        ScrollPane nscroll = new ScrollPane(newsRows); nscroll.setFitToWidth(true); nscroll.setStyle("-fx-background-color:transparent;");
        nscroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        newsRows.setPadding(new Insets(4, 6, 8, 6));
        newsNote.setPadding(new Insets(0, 16, 8, 16)); newsNote.setWrapText(true);
        VBox newsCard = new VBox(nhead, nscroll, newsNote);
        newsCard.getStyleClass().add("jhub-card"); newsCard.setMinWidth(0);
        VBox.setVgrow(nscroll, Priority.ALWAYS);
        grid.add(newsCard, 0, 1, 3, 1);
        javafx.scene.layout.GridPane.setFillHeight(newsCard, true);
        grid.setVgap(16);
        grid.getRowConstraints().clear();
        javafx.scene.layout.RowConstraints r0 = new javafx.scene.layout.RowConstraints(); r0.setPercentHeight(42); r0.setFillHeight(true);
        javafx.scene.layout.RowConstraints r1 = new javafx.scene.layout.RowConstraints(); r1.setPercentHeight(58); r1.setFillHeight(true);
        grid.getRowConstraints().addAll(r0, r1);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(12, lbl("Info", "jhub-card-title"), range, search, sp, status, refresh, sources);
        bar.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(grid, Priority.ALWAYS);
        // bottom: who made it (large), then the legal notes and a way to ask for a feed to be added or removed
        Label who = lbl("WM3J", "jhub-card-title"); who.setStyle("-fx-font-size:18px;-fx-font-weight:bold;");
        HBox mine = new HBox(20, who, link("wm3j.net", "https://www.wm3j.net", 16),
                link("GitHub repositories", "https://github.com/skip17331?tab=repositories", 16),
                link("WM3J-ARS-Suite on GitHub", "https://github.com/skip17331/WM3J-ARS-Suite", 16));
        mine.setAlignment(Pos.CENTER_LEFT);
        Label ask = mailLink("Contact: " + CONTACT + " \u2014 to have a feed added or removed", "Info feeds: add or remove a source", 13);
        HBox legal = new HBox(18, doc("GPL v3 license", "license"), doc("Code of Conduct", "conduct"), doc("Legal & privacy notes", "legal"), ask);
        legal.setAlignment(Pos.CENTER_LEFT);
        VBox links = new VBox(6, mine, legal);
        VBox page = new VBox(14, bar, grid, links);
        page.setPadding(new Insets(16, 22, 18, 22)); page.setStyle("-fx-background-color:-ars-bg;");
        page.setMinWidth(0);
        HBox.setHgrow(page, Priority.ALWAYS);

        Region frame = Shell.frame(dock, top, page, null);
        render.run();
        renderNews.run();
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

    /** One headline on one line: age, source, title. Click opens the source page. */
    private static Node newsRow(NewsFeeds.Headline h, Instant now) {
        Label age = lbl(NewsFeeds.age(h.published(), now), "jl-eyebrow"); age.setMinWidth(46); age.setPrefWidth(46);
        age.setStyle("-fx-text-fill:-ars-t2;-fx-font-weight:bold;");
        Label src = lbl(h.source(), "jl-emeta"); src.setMinWidth(150); src.setPrefWidth(150); src.setMaxWidth(150); src.setTextOverrun(OverrunStyle.ELLIPSIS);
        Label title = lbl(h.title(), "jhub-card-title"); title.setTextOverrun(OverrunStyle.ELLIPSIS); title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS); title.setMinWidth(0);
        HBox r = new HBox(10, age, src, title); r.setAlignment(Pos.CENTER_LEFT); r.setPadding(new Insets(7, 10, 7, 10));
        r.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;-fx-cursor:hand;");
        javafx.scene.control.Tooltip.install(r, new javafx.scene.control.Tooltip(h.title() + "\n" + h.source()
                + (h.published() == null ? "" : "  \u00b7  " + DateTimeFormatter.ofPattern("d MMM yyyy HH:mm'Z'", Locale.ENGLISH).format(h.published().atZone(ZoneOffset.UTC)))
                + "\n(click to read it on the source site)"));
        String link = h.link();
        r.setOnMouseClicked(e -> open(link));
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

    static final String CONTACT = "wm3jmike@gmail.com";

    /** An underlined link label that opens in the browser. */
    private static Label link(String text, String url, double size) {
        Label l = lbl(text, "jl-emeta"); l.setStyle("-fx-cursor:hand;-fx-underline:true;-fx-font-size:" + size + "px;-fx-text-fill:-ars-accent;");
        com.ars.fx.shell.Tips.tip(l, url);
        l.setOnMouseClicked(e -> open(url));
        return l;
    }
    /** A label that opens one of the local legal documents in the app. */
    private static Label doc(String text, String id) {
        Label l = lbl(text, "jl-emeta"); l.setStyle("-fx-cursor:hand;-fx-underline:true;-fx-font-size:13px;-fx-text-fill:-ars-accent;");
        com.ars.fx.shell.Tips.tip(l, "Read it here - this copy is kept on your computer");
        l.setOnMouseClicked(e -> LegalView.show(l, id));
        return l;
    }
    /** A link that starts an email to the project contact; if no mail program is set up, the address is copied instead. */
    private static Label mailLink(String text, String subject, double size) {
        Label l = lbl(text, "jl-emeta"); l.setStyle("-fx-cursor:hand;-fx-underline:true;-fx-font-size:" + size + "px;-fx-text-fill:-ars-accent;");
        com.ars.fx.shell.Tips.tip(l, "Opens your mail program (or copies the address if you have none set up)");
        l.setOnMouseClicked(e -> mail(l, text, subject));
        return l;
    }
    private static void mail(Label shown, String original, String subject) {
        try {
            java.awt.Desktop.getDesktop().mail(java.net.URI.create("mailto:" + CONTACT + "?subject=" + java.net.URLEncoder.encode(subject, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20")));
        } catch (Throwable t) {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent(); cc.putString(CONTACT);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            shown.setText("Address copied: " + CONTACT);
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
            p.setOnFinished(ev -> shown.setText(original)); p.play();
        }
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
        Map<NewsFeeds.Feed, CheckBox> newsBoxes = new java.util.LinkedHashMap<>();
        java.util.Set<String> onNow = NewsFeeds.enabledIds();
        VBox newsBox = new VBox(4);
        NewsFeeds.Group lastGroup = null;
        for (NewsFeeds.Feed f : NewsFeeds.CATALOG) {
            if (f.group() != lastGroup) { newsBox.getChildren().add(lbl(f.group().label.toUpperCase(Locale.ROOT), "jl-flabel")); lastGroup = f.group(); }
            CheckBox cb = new CheckBox(f.name()); cb.setSelected(onNow.contains(f.id()));
            newsBoxes.put(f, cb); newsBox.getChildren().add(cb);
        }
        TextArea custom = new TextArea(HubConfig.get("info.news.custom", "")); custom.setPrefRowCount(3);
        custom.setPromptText("https://example.org/feed   (one RSS or Atom address per line)");
        Label note = lbl("Contests and hamfests come from an iCalendar (.ics) link you provide. The WA7BNM Contest Calendar permits its iCal feed for "
                + "personal calendar use; paste your own link from contestcalendar.com. Feeds are fetched at most every six hours and kept on disk.", "jl-emeta");
        note.setWrapText(true); note.setMaxWidth(520);
        Label msg = lbl("", "jl-emeta"); msg.setStyle("-fx-text-fill:-ars-err;");
        Label cancel = lbl("Cancel", "jl-clearbtn"); cancel.setStyle("-fx-cursor:hand;");
        Label save = lbl("Save", "jl-logbtn"); save.setStyle("-fx-cursor:hand;");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox btns = new HBox(14, msg, gap, cancel, save); btns.setAlignment(Pos.CENTER_LEFT);
        Label newsNote2 = lbl("News shows only the headline, the source and a link to the source site. Feeds are fetched at most every six hours.", "jl-emeta");
        newsNote2.setWrapText(true); newsNote2.setMaxWidth(520);
        VBox box = new VBox(8, lbl("Sources", "jhub-card-title"), dxOn, lbl("DXPEDITION CALENDAR (leave blank for the default)", "jl-flabel"), dx,
                lbl("CONTEST CALENDAR", "jl-flabel"), contests, lbl("HAMFEST CALENDAR", "jl-flabel"), fests, note,
                lbl("NEWS FEEDS", "jhub-card-title"), newsBox, lbl("YOUR OWN NEWS FEEDS", "jl-flabel"), custom, newsNote2, btns);
        box.setPadding(new Insets(16)); box.setPrefWidth(560);
        ScrollPane boxScroll = new ScrollPane(box); boxScroll.setFitToWidth(true); boxScroll.setPrefViewportHeight(640);
        javafx.scene.Scene scene = new javafx.scene.Scene(boxScroll);
        com.ars.fx.Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        com.ars.fx.Themes.applyTo(boxScroll, "info");
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
            java.util.Set<String> on = new java.util.LinkedHashSet<>();
            for (Map.Entry<NewsFeeds.Feed, CheckBox> en : newsBoxes.entrySet()) if (en.getValue().isSelected()) on.add(en.getKey().id());
            NewsFeeds.setEnabled(on);
            NewsFeeds.setCustom(custom.getText());
            st.close(); onSaved.run();
        });
        st.show();
    }
}
