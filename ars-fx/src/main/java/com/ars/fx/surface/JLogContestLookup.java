package com.ars.fx.surface;

import com.ars.fx.data.CallHistory;
import com.ars.fx.data.CallsignLookup;
import com.ars.fx.data.ContestState;
import com.ars.fx.data.DxccInfo;
import com.ars.fx.data.Geo;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.LookupConfig;
import com.ars.fx.data.MapTracks;
import com.ars.fx.data.Propagation;
import com.ars.fx.shell.Shell;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ars.fx.shell.Shell.lbl;

/**
 * A callsign lookup for the contest page that has nothing to do with the entry form: its own box, its own answer. It never reads the form,
 * changes it, moves the focus into it or starts a dupe check; the only link is the optional "from the form" button, which copies the call
 * in one direction, out of the form. The answer is the callbook's (name, place, grid), the country, how far and which way, what the call
 * history says the station usually sends, and whether and how it is in this contest's log.
 */
final class JLogContestLookup {
    private JLogContestLookup() {}

    static Node drawer() {
        TextField in = new TextField(); in.setPromptText("Call"); in.getStyleClass().add("jhub-mini-field"); in.setPrefColumnCount(10);
        Label go = lbl("Look up", "jl-logbtn"); go.setStyle("-fx-cursor:hand;-fx-padding:6 14;");
        Label fromForm = lbl("from the form", "jl-clearbtn"); fromForm.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(fromForm, "Copy the call typed in the entry form into this box (the form itself is not touched)");
        VBox out = new VBox(4);
        Runnable run = () -> look(in.getText(), out);
        go.setOnMouseClicked(e -> run.run());
        in.setOnAction(e -> run.run());
        in.textProperty().addListener((o, a, b) -> { if (b != null && !b.equals(b.toUpperCase(Locale.ROOT))) in.setText(b.toUpperCase(Locale.ROOT)); });
        fromForm.setOnMouseClicked(e -> {
            Object c = JLogContest.fieldControls == null ? null : JLogContest.fieldControls.get("callsign");
            if (c instanceof TextField t && t.getText() != null && !t.getText().isBlank()) { in.setText(t.getText().trim().toUpperCase(Locale.ROOT)); run.run(); }
        });
        HBox row = new HBox(8, in, go); row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(in, Priority.ALWAYS);
        VBox body = new VBox(8, row, fromForm, out);
        return Shell.drawer("Call lookup", "accent", "hub", "", false, body);
    }

    private static void look(String typed, VBox out) {
        String c = typed == null ? "" : typed.trim().toUpperCase(Locale.ROOT);
        if (c.length() < 3) { show(out, List.of("Type a call to look up.")); return; }
        show(out, List.of("⋯ looking up " + c + "…"));
        ContestPlugin plugin = ContestState.plugin();
        List<QsoRecord> log = ContestState.qsos();
        Thread t = new Thread(() -> {
            List<String> lines = new ArrayList<>();
            try { lines.addAll(answer(c, plugin, log)); } catch (Exception e) { lines.add("Lookup failed: " + e.getMessage()); }
            Platform.runLater(() -> show(out, lines));
        }, "contest-lookup");
        t.setDaemon(true); t.start();
    }

    /** Everything known about a call, as lines of text. Off the FX thread: the callbook is a network call. */
    static List<String> answer(String c, ContestPlugin plugin, List<QsoRecord> log) {
        List<String> lines = new ArrayList<>();
        CallsignLookup.Result r = LookupConfig.anyEnabled() ? CallsignLookup.lookup(c) : null;
        String base = c.replaceAll("[-/].*", "");
        DxccInfo.Info d = DxccInfo.of(base);
        lines.add(r != null && r.name() != null && !r.name().isBlank() ? c + " — " + r.name() : c);
        List<String> where = new ArrayList<>();
        if (r != null) for (String s : new String[]{r.city(), r.state(), r.county()}) if (s != null && !s.isBlank()) where.add(s);
        String country = r != null && r.country() != null && !r.country().isBlank() ? r.country() : d == null ? "" : d.country();
        if (!country.isBlank()) where.add(country);
        if (!where.isEmpty()) lines.add(String.join(", ", where));
        double[] ll = null;
        String grid = r == null ? null : r.grid();
        if (grid != null && grid.trim().length() >= 4) { try { ll = com.jlog.util.Maidenhead.center(grid.trim()); } catch (RuntimeException ignored) { /* bad grid */ } }
        if (ll == null) ll = Geo.near(c);
        StringBuilder g = new StringBuilder();
        if (grid != null && !grid.isBlank()) g.append("Grid ").append(grid.trim());
        if (ll != null && HubConfig.hasLocation()) {
            double km = Propagation.distanceKm(new Propagation.Point(HubConfig.lat(), HubConfig.lon()), new Propagation.Point(ll[0], ll[1]));
            double brg = MapTracks.bearing(HubConfig.lat(), HubConfig.lon(), ll[0], ll[1]);
            if (g.length() > 0) g.append("  ·  ");
            g.append(String.format(Locale.ROOT, "%,.0f km  ·  %03.0f°", km, brg));
        }
        if (g.length() > 0) lines.add(g.toString());
        if (r == null) lines.add(LookupConfig.anyEnabled() ? "The callbook has no answer for this call." : "No callbook is switched on (Setup ▸ Lookup).");
        Map<String, String> h = CallHistory.get(c);
        if (!h.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, String> e : h.entrySet()) if (!e.getValue().isBlank()) parts.add(e.getKey() + " " + e.getValue());
            if (!parts.isEmpty()) lines.add("Usually sends: " + String.join(", ", parts));
        }
        List<QsoRecord> mine = new ArrayList<>();
        for (QsoRecord q : log) if (q.getCallsign() != null && q.getCallsign().equalsIgnoreCase(c)) mine.add(q);
        if (plugin != null) {
            if (mine.isEmpty()) lines.add("Not worked in this contest.");
            else {
                lines.add("Worked in this contest (" + mine.size() + "×):");
                for (QsoRecord q : mine) lines.add("  " + JLogContest.describe(plugin, q));
            }
        }
        if (r != null && r.source() != null) lines.add("Source: " + r.source());
        return lines;
    }

    private static void show(VBox out, List<String> lines) {
        out.getChildren().clear();
        boolean first = true;
        for (String s : lines) {
            Label l = lbl(s, first ? "jhub-card-title" : "jl-emeta"); l.setWrapText(true); l.setMaxWidth(Double.MAX_VALUE);
            if (!first) l.setStyle("-fx-font-size:13px;");
            out.getChildren().add(l);
            first = false;
        }
    }
}
