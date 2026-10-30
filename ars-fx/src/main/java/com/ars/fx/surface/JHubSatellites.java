package com.ars.fx.surface;

import com.ars.fx.data.SatCatalog;
import com.ars.fx.data.SatCatalog.PassInfo;
import com.ars.fx.data.SatCatalog.Row;
import com.ars.fx.data.SatSelection;
import com.ars.fx.data.SatService;
import com.ars.fx.data.TleStore;
import com.ars.fx.shell.Shell;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.ars.fx.shell.Shell.lbl;

/**
 * J-Hub ▸ Satellites — where J-Sat's setup lives: refresh the orbital elements (Celestrak / AMSAT) and pick which
 * satellites to track from everything the elements and the radio registry cover.
 */
final class JHubSatellites {
    private JHubSatellites() {}

    private static final DateTimeFormatter UTC_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);
    private static final int[] MIN_EL = {0, 10, 30, 60};

    // picker state (kept across visits to the page)
    private static String query = "";
    private static int show = 0;          // 0 all · 1 amateur radio (registry) · 2 selected
    private static int minElIdx = 0;
    private static boolean dataOnly = false;
    private static volatile Map<Integer, PassInfo> passes;
    private static volatile long passesAtMs;

    static Region center() {
        Set<Integer> sel = new TreeSet<>(SatSelection.selected());

        Label srcV = lbl("", "cfg-sv"), ageV = lbl("", "cfg-sv"), selV = lbl("", "cfg-sv"), trkV = lbl("", "cfg-sv");
        HBox cells = new HBox(0, stat("ORBITAL DATA", srcV), stat("ELEMENT AGE", ageV), stat("SELECTED", selV), stat("TRACKABLE", trkV));
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);

        VBox list = new VBox(); list.getStyleClass().add("cfg-card");
        Label count = lbl("", "cfg-lh");

        Runnable[] renderList = new Runnable[1];
        Runnable updateStats = () -> {
            Instant last = TleStore.lastFetch();
            srcV.setText(last == null ? "Bundled snapshot" : "Downloaded " + UTC_FMT.format(last) + "Z");
            srcV.getStyleClass().remove("ok"); if (last != null) srcV.getStyleClass().add("ok");
            double[] a = TleStore.ageStats();
            ageV.setText(String.format(Locale.ROOT, "%.0f d oldest · %.0f d median", a[0], a[1]));
            selV.setText(String.valueOf(sel.size()));
            long trk = sel.stream().filter(id -> TleStore.current().containsKey(id)).count();
            trkV.setText(trk + (trk < sel.size() ? "  (" + (sel.size() - trk) + " without elements)" : ""));
        };

        Label status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);

        // selection changes persist at once; a moment after the last click J-Sat re-resolves what it tracks, and any
        // pick that has no orbital elements is looked up on Celestrak by NORAD id
        PauseTransition debounce = new PauseTransition(Duration.millis(700));
        debounce.setOnFinished(e -> {
            SatService.getInstance().reload();
            boolean missing = sel.stream().anyMatch(id -> id > 0 && !TleStore.current().containsKey(id) && !TleStore.isNotTracked(id));
            if (missing && !TleStore.refreshing()) {
                status.setText("Looking up orbital data for new picks…");
                TleStore.fetchMissingAsync(r -> Platform.runLater(() -> {
                    status.setText((r.ok() ? "✓ " : "• ") + r.message());
                    updateStats.run();
                    if (r.ok()) { passes = null; loadPasses(renderList[0]); renderList[0].run(); }   // only new elements change the list
                }));
            }
        });
        Runnable persist = () -> { SatSelection.setSelected(sel); updateStats.run(); debounce.playFromStart(); };

        // ── orbital data ────────────────────────────────────────────────────
        Label refresh = JHubDashboard.cfgBtn("Refresh now");
        refresh.setOnMouseClicked(e -> {
            if (TleStore.refreshing()) return;
            status.setText("Downloading from Celestrak and AMSAT…");
            TleStore.refreshAsync(r -> Platform.runLater(() -> {
                status.setText((r.ok() ? "✓ " : "✗ ") + r.message());
                updateStats.run();
                if (r.ok()) { passes = null; SatService.getInstance().reload(); loadPasses(renderList[0]); renderList[0].run(); }
            }));
        });
        StringBuilder srcs = new StringBuilder();
        for (String[] s : TleStore.SOURCES) { if (srcs.length() > 0) srcs.append(" · "); srcs.append(s[0]); }
        VBox data = new VBox(); data.getStyleClass().add("cfg-card");
        data.getChildren().addAll(
                JHubDashboard.cfgRow("Download orbital elements", srcs.toString(), refresh),
                JHubDashboard.cfgRow("Refresh automatically", "once a day while J-Sat runs", JHubDashboard.switchToggle(TleStore.autoRefresh(), TleStore::setAutoRefresh)));
        HBox statusRow = new HBox(status); statusRow.getStyleClass().add("cfg-row"); statusRow.setStyle("-fx-border-width:0;");
        data.getChildren().add(statusRow);

        // ── picker controls ─────────────────────────────────────────────────
        TextField search = new TextField(query); search.setPromptText("search name, NORAD id, mode…");
        search.getStyleClass().add("jhub-mini-field"); search.setMinWidth(240);
        search.textProperty().addListener((o, a, b) -> { query = b == null ? "" : b; renderList[0].run(); });
        Region showSeg = JHubDashboard.segSel(new String[]{"All", "Amateur radio", "Selected"}, show, i -> { show = i; renderList[0].run(); });
        Region elSeg = JHubDashboard.segSel(new String[]{"Any", "≥ 10°", "≥ 30°", "≥ 60°"}, minElIdx, i -> { minElIdx = i; renderList[0].run(); });
        Region dataSw = JHubDashboard.switchToggle(dataOnly, v -> { dataOnly = v; renderList[0].run(); });

        Label selShown = JHubDashboard.cfgBtn("Select shown"), clrShown = JHubDashboard.cfgBtn("Clear shown"), reset = JHubDashboard.cfgBtn("Reset to defaults");
        HBox actions = new HBox(10, selShown, clrShown, reset); actions.setAlignment(Pos.CENTER_LEFT);

        VBox ctl = new VBox(); ctl.getStyleClass().add("cfg-card");
        ctl.getChildren().addAll(
                JHubDashboard.cfgRow("Find", "narrow the list", search),
                JHubDashboard.cfgRow("Show", null, showSeg),
                JHubDashboard.cfgRow("Has a pass in the next 24 h", "best elevation over your station", elSeg),
                JHubDashboard.cfgRow("Only satellites with orbital data", "hides ones J-Sat cannot track yet", dataSw));
        HBox actRow = new HBox(20, count, new Region(), actions); actRow.getStyleClass().add("cfg-row"); actRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actRow.getChildren().get(1), Priority.ALWAYS);
        ctl.getChildren().add(actRow);

        // ── list ────────────────────────────────────────────────────────────
        renderList[0] = () -> Shell.keepScroll(list, () -> {
            // Rebuilding replaces the checkbox the operator just clicked. Without this the focus would fall back to the
            // first field on the page (Find) and the page would scroll up to show it — hence the jump.
            Node fo = list.getScene() == null ? null : list.getScene().getFocusOwner();
            Object focusedId = fo == null ? null : fo.getProperties().get("norad");
            List<Row> shown = filtered(sel);
            list.getChildren().clear();
            list.getChildren().add(headerRow());
            for (Row r : shown) list.getChildren().add(row(r, sel, persist));
            if (shown.isEmpty()) {
                Label none = lbl("No satellites match.", "cfg-lh"); none.setPadding(new Insets(14, 16, 14, 16));
                list.getChildren().add(none);
            }
            if (focusedId != null)
                for (Node n : list.lookupAll(".check-box")) if (focusedId.equals(n.getProperties().get("norad"))) { n.requestFocus(); break; }
            long on = shown.stream().filter(r -> sel.contains(r.norad())).count();
            count.setText(shown.size() + " shown · " + on + " selected of them"
                    + (passes == null && minElIdx > 0 ? " · predicting passes…" : ""));
        });
        selShown.setOnMouseClicked(e -> { for (Row r : filtered(sel)) sel.add(r.norad()); persist.run(); renderList[0].run(); });
        clrShown.setOnMouseClicked(e -> { for (Row r : filtered(sel)) sel.remove(r.norad()); persist.run(); renderList[0].run(); });
        reset.setOnMouseClicked(e -> { SatSelection.reset(); sel.clear(); sel.addAll(SatSelection.selected()); updateStats.run(); debounce.playFromStart(); renderList[0].run(); });

        updateStats.run();
        renderList[0].run();
        loadPasses(renderList[0]);

        Label desc = lbl("Choose which satellites J-Sat tracks. The list is built from the orbital elements held (Celestrak / AMSAT) "
                + "plus the amateur radio registry — a satellite with elements but no radio data is tracked for position and passes only.", "cfg-desc");
        desc.setWrapText(true);
        VBox pickSec = new VBox(6, lbl("SATELLITE SELECTION", "cfg-sec"), ctl, list);
        return JHubDashboard.cfgPage(JHubDashboard.cfgTop("J-Hub", "Satellites"), desc, cells,
                new VBox(6, lbl("ORBITAL DATA", "cfg-sec"), data), pickSec);
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private static VBox stat(String k, Label v) {
        VBox cell = new VBox(4, lbl(k, "cfg-sk"), v); cell.setMinWidth(150); cell.setPadding(new Insets(0, 24, 0, 0));
        return cell;
    }

    private static List<Row> filtered(Set<Integer> sel) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        double minEl = MIN_EL[minElIdx];
        Map<Integer, PassInfo> ps = passes;
        List<Row> out = new ArrayList<>();
        for (Row r : SatCatalog.rows()) {
            if (!q.isEmpty() && !(r.name().toLowerCase(Locale.ROOT).contains(q) || String.valueOf(r.norad()).contains(q)
                    || r.type().toLowerCase(Locale.ROOT).contains(q) || r.modes().toLowerCase(Locale.ROOT).contains(q))) continue;
            if (show == 1 && !r.inRegistry()) continue;
            if (show == 2 && !sel.contains(r.norad())) continue;
            if (dataOnly && !r.hasElements()) continue;
            if (minEl > 0 && ps != null) { PassInfo p = ps.get(r.norad()); if (p == null || p.maxElDeg() < minEl) continue; }
            out.add(r);
        }
        return out;
    }

    /** Predict 24 h of passes for everything we hold elements for, off the FX thread, then repaint the list. */
    private static void loadPasses(Runnable repaint) {
        if (passes != null && System.currentTimeMillis() - passesAtMs < 15 * 60_000) return;
        SatService svc = SatService.getInstance();
        Thread t = new Thread(() -> {
            Map<Integer, PassInfo> m = SatCatalog.passes24h(svc.obsLat(), svc.obsLon());
            passes = m; passesAtMs = System.currentTimeMillis();
            Platform.runLater(repaint);
        }, "sat-picker-passes");
        t.setDaemon(true); t.start();
    }

    private static HBox headerRow() {
        HBox h = new HBox(12, new Region(), lbl("SATELLITE", "cfg-sk"), spacer(), col("RADIO", 170), col("ELEMENTS", 96), col("NEXT 24 H", 130));
        ((Region) h.getChildren().get(0)).setMinWidth(22);
        h.getStyleClass().add("cfg-row"); h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static HBox row(Row r, Set<Integer> sel, Runnable persist) {
        CheckBox cb = new CheckBox(); cb.setSelected(sel.contains(r.norad())); cb.getProperties().put("norad", r.norad());
        cb.selectedProperty().addListener((o, a, b) -> { if (b) sel.add(r.norad()); else sel.remove(r.norad()); persist.run(); });

        Label nm = lbl(r.name(), "cfg-lt");
        String sub = r.norad() + (r.type().isBlank() ? "" : " · " + r.type()) + (r.status().isBlank() || r.status().equals("Active") ? "" : " · " + r.status());
        VBox name = new VBox(2, nm, lbl(sub, "cfg-lh")); name.setMinWidth(0);

        String radio = r.hasRadio()
                ? (r.downlinkHz() > 0 ? "↓ " + SatService.mhz(r.downlinkHz()) : "") + (r.uplinkHz() > 0 ? "  ↑ " + SatService.mhz(r.uplinkHz()) : "")
                : "no radio data";
        Label rl = col(radio.trim(), 170); if (!r.hasRadio()) rl.setStyle("-fx-text-fill:-ars-t4;");

        Label el;
        if (!r.hasElements()) { el = col("none", 96); el.setStyle("-fx-text-fill:-ars-err;"); }
        else {
            el = col(String.format(Locale.ROOT, "%.0f d old", r.ageDays()), 96);
            el.setStyle(r.ageDays() <= 7 ? "-fx-text-fill:-ars-ok;" : r.ageDays() > 30 ? "-fx-text-fill:-ars-err;" : "");
        }

        Label ps;
        Map<Integer, PassInfo> pm = passes;
        if (!r.hasElements()) ps = col("—", 130);
        else if (pm == null) ps = col("…", 130);
        else {
            PassInfo p = pm.get(r.norad());
            ps = (p == null || p.passes() == 0) ? col("no pass", 130)
                    : col(p.passes() + (p.passes() == 1 ? " pass" : " passes") + " · max " + Math.round(p.maxElDeg()) + "°", 130);
        }

        HBox row = new HBox(12, cb, name, spacer(), rl, el, ps);
        HBox.setHgrow(name, Priority.ALWAYS);
        row.getStyleClass().add("cfg-row"); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label col(String text, double w) {
        Label l = lbl(text, "cfg-lh"); l.setMinWidth(w); l.setPrefWidth(w); l.setMaxWidth(w); return l;
    }
    private static Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }
}
