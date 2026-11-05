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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Info: what is happening in the hobby. This first page is "Upcoming" - contests, DXpeditions and hamfests in one
 * date-ordered list, filtered by kind, how far ahead, and a word. Feeds are in {@link UpcomingEvents}.
 */
public final class InfoView {
    private InfoView() {}

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    public static Region build() {
        Region dock = Shell.dock("info");
        HBox top = Shell.topBar("info", "info", "Info", "Upcoming", new String[][]{}, null);

        List<Event> all = new ArrayList<>();
        List<Loaded> loads = new ArrayList<>();
        VBox list = new VBox(0);
        VBox footer = new VBox(3);
        Label count = lbl("", "jl-emeta");

        CheckBox cContest = new CheckBox("Contests"), cDx = new CheckBox("DXpeditions"), cFest = new CheckBox("Hamfests");
        for (CheckBox c : new CheckBox[]{cContest, cDx, cFest}) c.setSelected(true);
        ComboBox<String> range = new ComboBox<>();
        range.getItems().addAll("Next 7 days", "Next 30 days", "Next 90 days", "Everything");
        range.setValue(HubConfig.get("info.range", "Next 30 days"));
        if (!range.getItems().contains(range.getValue())) range.setValue("Next 30 days");
        TextField search = new TextField(); search.setPromptText("Search… (call, entity, contest)"); search.setPrefWidth(240);

        Runnable render = () -> {
            list.getChildren().clear();
            Set<Kind> kinds = EnumSet.noneOf(Kind.class);
            if (cContest.isSelected()) kinds.add(Kind.CONTEST);
            if (cDx.isSelected()) kinds.add(Kind.DXPEDITION);
            if (cFest.isSelected()) kinds.add(Kind.HAMFEST);
            int days = switch (range.getValue()) { case "Next 7 days" -> 7; case "Next 30 days" -> 30; case "Next 90 days" -> 90; default -> 0; };
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            List<Event> shown = UpcomingEvents.upcoming(all, today, days, kinds, search.getText());
            count.setText(shown.size() + (shown.size() == 1 ? " event" : " events"));
            if (shown.isEmpty()) {
                list.getChildren().add(lbl(all.isEmpty() ? (loads.isEmpty() ? "Loading…" : "Nothing loaded yet - check Sources.") : "Nothing matches.", "jl-emeta"));
                return;
            }
            String lastHead = "";
            for (Event e : shown) {
                String head = e.start().isBefore(today) ? "Under way now" : heading(e.start(), today);
                if (!head.equals(lastHead)) {
                    Label h = lbl(head, "jl-flabel"); h.setPadding(new Insets(14, 0, 4, 0));
                    list.getChildren().add(h); lastHead = head;
                }
                list.getChildren().add(row(e, today));
            }
        };

        Runnable[] load = new Runnable[1];
        boolean[] busy = {false};
        load[0] = () -> {
            if (busy[0]) return;
            busy[0] = true;
            runLoad(false, all, loads, footer, render, () -> busy[0] = false);
        };
        Label refresh = lbl("⟳ Refresh", "jl-clearbtn"); refresh.setStyle("-fx-cursor:hand;");
        refresh.setOnMouseClicked(e -> { if (busy[0]) return; busy[0] = true; runLoad(true, all, loads, footer, render, () -> busy[0] = false); });
        Label sources = lbl("Sources…", "jl-clearbtn"); sources.setStyle("-fx-cursor:hand;");
        sources.setOnMouseClicked(e -> editSources(sources, () -> load[0].run()));

        for (CheckBox c : new CheckBox[]{cContest, cDx, cFest}) c.setOnAction(e -> render.run());
        range.setOnAction(e -> { HubConfig.set("info.range", range.getValue()); render.run(); });
        search.textProperty().addListener((o, a, b) -> render.run());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox title = new HBox(12, lbl("Upcoming", "jhub-card-title"), count, sp, refresh, sources); title.setAlignment(Pos.CENTER_LEFT);
        HBox filters = new HBox(14, cContest, cDx, cFest, range, search); filters.setAlignment(Pos.CENTER_LEFT);
        ScrollPane scroll = new ScrollPane(list); scroll.setFitToWidth(true); scroll.setStyle("-fx-background-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox page = new VBox(10, title, filters, scroll, footer);
        page.setPadding(new Insets(16, 22, 14, 22)); page.setStyle("-fx-background-color:-ars-bg;");
        page.setMinWidth(0);
        HBox.setHgrow(page, Priority.ALWAYS);

        Region frame = Shell.frame(dock, top, page, null);
        render.run();
        load[0].run();
        return frame;
    }

    private static void runLoad(boolean force, List<Event> all, List<Loaded> loads, VBox footer, Runnable render, Runnable done) {
        Thread t = new Thread(() -> {
            List<Loaded> got = new ArrayList<>();
            for (UpcomingEvents.Source s : UpcomingEvents.sources()) got.add(UpcomingEvents.load(s, force, Instant.now()));
            Platform.runLater(() -> {
                all.clear(); loads.clear(); loads.addAll(got);
                for (Loaded l : got) all.addAll(l.events());
                footer.getChildren().setAll(sourceLines(got));
                render.run();
                done.run();
            });
        }, "info-feeds");
        t.setDaemon(true); t.start();
    }

    private static String heading(LocalDate d, LocalDate today) {
        if (d.equals(today)) return "Today";
        if (d.equals(today.plusDays(1))) return "Tomorrow";
        return DAY.format(d);
    }

    private static Node row(Event e, LocalDate today) {
        String hue = switch (e.kind()) { case CONTEST -> "-ars-log"; case DXPEDITION -> "-ars-map"; case HAMFEST -> "-ars-sat"; };
        Label chip = lbl(switch (e.kind()) { case CONTEST -> "CONTEST"; case DXPEDITION -> "DX"; case HAMFEST -> "HAMFEST"; }, "jl-eyebrow");
        chip.setMinWidth(78); chip.setStyle("-fx-text-fill:" + hue + ";-fx-font-weight:bold;");
        Label name = lbl(e.title(), "jhub-card-title"); name.setWrapText(true);
        String when = e.start().equals(e.end()) ? SHORT.format(e.start()) : SHORT.format(e.start()) + " – " + SHORT.format(e.end());
        if (!e.time().isEmpty()) when += "   " + e.time();
        if (e.location() != null && !e.location().isBlank()) when += "   ·   " + e.location();
        Label dates = lbl(when, "jl-emeta");
        VBox text = new VBox(2, name, dates);
        if (e.detail() != null && !e.detail().isBlank()) {
            Label d = lbl(e.detail(), "jl-emeta"); d.setWrapText(true); text.getChildren().add(d);
        }
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox r = new HBox(12, chip, text); r.setAlignment(Pos.TOP_LEFT);
        r.setPadding(new Insets(7, 4, 7, 4));
        r.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;");
        String url = e.url();
        if (url != null && UpcomingEvents.validUrl(url)) {
            r.setStyle(r.getStyle() + "-fx-cursor:hand;");
            com.ars.fx.shell.Tips.tip(r, "Open the event page in your browser");
            r.setOnMouseClicked(ev -> open(url));
        }
        return r;
    }

    private static void open(String url) {
        try { java.awt.Desktop.getDesktop().browse(java.net.URI.create(url)); } catch (Throwable ignored) {}
    }

    private static List<Node> sourceLines(List<Loaded> loads) {
        List<Node> out = new ArrayList<>();
        for (Loaded l : loads) {
            if (!l.source().configured()) {
                if (l.source().kind() == Kind.CONTEST)
                    out.add(lbl("Contests: not set up. Add your own contest-calendar link under Sources (the WA7BNM calendar allows its iCal feed for personal use).", "jl-emeta"));
                continue;
            }
            Label name = lbl(l.source().label(), "jl-emeta");
            if (l.source().creditUrl() != null && !l.source().creditUrl().isBlank()) {
                name.setStyle("-fx-cursor:hand;-fx-underline:true;");
                String u = l.source().creditUrl();
                name.setOnMouseClicked(e -> open(u));
            }
            String state = l.error() != null ? l.error()
                    : (l.fetched() == null ? "" : (l.fromCache() ? "saved copy from " : "updated ") + DateTimeFormatter.ofPattern("d MMM HH:mm'Z'", Locale.ENGLISH).format(l.fetched().atZone(ZoneOffset.UTC)));
            Label st = lbl("— " + l.events().size() + " events · " + state, "jl-emeta");
            if (l.error() != null) st.setStyle("-fx-text-fill:-ars-warn;");
            HBox line = new HBox(6, lbl(l.source().kind().label + ":", "jl-emeta"), name, st);
            line.setAlignment(Pos.CENTER_LEFT);
            out.add(line);
        }
        return out;
    }

    // ---- Sources dialog ------------------------------------------------------------------------------
    private static void editSources(Node owner, Runnable onSaved) {
        CheckBox dxOn = new CheckBox("Show DXpeditions (NG3K Announced DX Operations)"); dxOn.setSelected(HubConfig.getBool("info.dx.enabled", true));
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
