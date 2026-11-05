package com.ars.fx.surface;

import com.ars.fx.data.CustomAwards;
import com.ars.fx.data.CustomAwards.Def;
import com.ars.fx.data.JLogDb;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/**
 * "My awards": the operator's own award sheets. Pick one, see every item as worked / confirmed with when and who it
 * was first worked with, edit what counts, start from the 13 Colonies example, and share a sheet as a file.
 */
final class CustomAwardsView {
    private CustomAwardsView() {}

    static Node build() {
        ComboBox<Def> pick = new ComboBox<>();
        pick.setMinWidth(260);
        Label status = lbl("", "jl-emeta");
        VBox sheet = new VBox(0);
        CheckBox onlyNeeded = new CheckBox("Only what I still need");
        Label summary = lbl("", "jl-award-pct");
        Label desc = lbl("", "jl-emeta"); desc.setWrapText(true);
        StackPane bar = new StackPane(); bar.setMinHeight(8); bar.setMaxHeight(8);
        Region track = new Region(); track.getStyleClass().add("jl-award-bar-track"); track.setMinHeight(8); track.setMaxHeight(8); track.setMaxWidth(Double.MAX_VALUE);
        Region fill = new Region(); fill.getStyleClass().add("jl-award-bar-fill"); fill.setMinHeight(8); fill.setMaxHeight(8);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT); bar.getChildren().addAll(track, fill);

        List<JLogDb.QsoExport>[] log = new List[]{null};
        CustomAwards.Result[] shown = {null};

        Runnable[] render = new Runnable[1];
        Runnable[] reload = new Runnable[1];

        render[0] = () -> {
            Def d = pick.getValue();
            sheet.getChildren().clear();
            fill.maxWidthProperty().unbind(); fill.setMaxWidth(0);
            if (d == null) {
                summary.setText(""); desc.setText("");
                sheet.getChildren().add(lbl("No awards yet. Use New award or From template to make one - for example the 13 Colonies special event.", "jl-emeta"));
                return;
            }
            if (log[0] == null) { summary.setText("Reading the log…"); return; }
            CustomAwards.Result res = CustomAwards.compute(d, log[0]);
            shown[0] = res;
            desc.setText(d.description == null ? "" : d.description);
            summary.setText(res.worked + " / " + res.total() + " worked   ·   " + res.confirmed + " confirmed"
                    + (!d.trackBands.isEmpty() ? "   \u00b7   " + res.slotsWorked + " / " + res.slotsTotal + " item-band slots" : "")
                    + (res.total() > 0 && res.worked == res.total() ? "   \u2014   COMPLETE" : ""));
            fill.maxWidthProperty().bind(bar.widthProperty().multiply(res.total() == 0 ? 0 : (double) res.worked / res.total()));
            boolean tracked = !d.trackBands.isEmpty();
            sheet.getChildren().add(head(tracked));
            for (CustomAwards.Row r : res.rows) {
                if (onlyNeeded.isSelected() && r.worked) continue;
                sheet.getChildren().add(row(r, tracked, d));
            }
        };

        reload[0] = () -> {
            status.setText("");
            List<Def> defs = CustomAwards.list();
            Def keep = pick.getValue();
            pick.getItems().setAll(defs);
            Def sel = null;
            if (keep != null) for (Def x : defs) if (x.id.equals(keep.id)) sel = x;
            pick.setValue(sel != null ? sel : (defs.isEmpty() ? null : defs.get(0)));
            Thread t = new Thread(() -> {
                List<JLogDb.QsoExport> rows = JLogDb.allForExport();
                Platform.runLater(() -> { log[0] = rows; render[0].run(); });
            }, "custom-awards");
            t.setDaemon(true); t.start();
            render[0].run();
        };

        pick.setOnAction(e -> render[0].run());
        onlyNeeded.setOnAction(e -> render[0].run());

        // ---- actions ------------------------------------------------------------------------------
        Label newBtn = lbl("New award…", "jl-clearbtn"); newBtn.setStyle("-fx-cursor:hand;");
        newBtn.setOnMouseClicked(e -> edit(newBtn, CustomAwards.blank(), saved -> { reload[0].run(); pick.setValue(find(pick, saved)); }));
        Label tplBtn = lbl("From template ▾", "jl-clearbtn"); tplBtn.setStyle("-fx-cursor:hand;");
        tplBtn.setOnMouseClicked(e -> {
            MenuItem colonies = new MenuItem("13 Colonies special event");
            colonies.setOnAction(a -> edit(tplBtn, CustomAwards.template13Colonies(), saved -> { reload[0].run(); pick.setValue(find(pick, saved)); }));
            new ContextMenu(colonies).show(tplBtn, e.getScreenX(), e.getScreenY());
        });
        Label editBtn = lbl("Edit", "jl-clearbtn"); editBtn.setStyle("-fx-cursor:hand;");
        editBtn.setOnMouseClicked(e -> { if (pick.getValue() != null) edit(editBtn, pick.getValue(), saved -> reload[0].run()); });
        Label delBtn = lbl("Delete", "jl-clearbtn"); delBtn.setStyle("-fx-cursor:hand;-fx-text-fill:-ars-err;");
        delBtn.setOnMouseClicked(e -> {
            Def d = pick.getValue();
            if (d == null) return;
            Confirm.show(delBtn, "Delete this award?", "\"" + d + "\" is removed. Your log is not touched.",
                    new Confirm.Choice("Keep it", true, null),
                    new Confirm.Choice("Delete", false, () -> {
                        try { CustomAwards.delete(d); } catch (Exception ex) { status.setText("Could not delete: " + ex.getMessage()); }
                        reload[0].run();
                    }));
        });
        Label impBtn = lbl("Import…", "jl-clearbtn"); impBtn.setStyle("-fx-cursor:hand;");
        impBtn.setOnMouseClicked(e -> {
            FileChooser fc = new FileChooser(); fc.setTitle("Import an award sheet");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Award sheet (*.json)", "*.json"));
            java.io.File f = fc.showOpenDialog(impBtn.getScene() == null ? null : impBtn.getScene().getWindow());
            if (f == null) return;
            try { Def d = CustomAwards.importFile(f.toPath()); reload[0].run(); status.setText("Imported " + d); }
            catch (Exception ex) { status.setText("Could not import: " + ex.getMessage()); }
        });
        Label shareBtn = lbl("Share sheet", "jl-clearbtn"); shareBtn.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(shareBtn, "Write the award definition to a file you can send to someone else (they use Import)");
        shareBtn.setOnMouseClicked(e -> {
            Def d = pick.getValue(); if (d == null) return;
            try { status.setText("Saved " + CustomAwards.exportJson(d, exports())); }
            catch (Exception ex) { status.setText("Could not save: " + ex.getMessage()); }
        });
        Label csvBtn = lbl("Export CSV", "jl-clearbtn"); csvBtn.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(csvBtn, "The checklist with dates and calls, for a spreadsheet or to print");
        csvBtn.setOnMouseClicked(e -> {
            if (shown[0] == null) return;
            try { status.setText("Saved " + CustomAwards.exportCsv(shown[0], exports())); }
            catch (Exception ex) { status.setText("Could not save: " + ex.getMessage()); }
        });
        Label printBtn = lbl("Print sheet", "jl-clearbtn"); printBtn.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(printBtn, "Make a printable page with a tick box per item and open it in your browser");
        printBtn.setOnMouseClicked(e -> {
            if (shown[0] == null) return;
            try {
                Path html = CustomAwards.exportHtml(shown[0], exports(), com.ars.fx.data.HubConfig.call());
                status.setText("Saved " + html);
                try { java.awt.Desktop.getDesktop().browse(html.toUri()); } catch (Throwable t) { /* the file is saved; open it yourself */ }
            } catch (Exception ex) { status.setText("Could not make the sheet: " + ex.getMessage()); }
        });
        Label refresh = lbl("\u27f3 Refresh", "jl-clearbtn"); refresh.setStyle("-fx-cursor:hand;");
        refresh.setOnMouseClicked(e -> reload[0].run());

        HBox top = new HBox(10, pick, newBtn, tplBtn, editBtn, delBtn); top.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox top2 = new HBox(14, summary, sp, onlyNeeded, impBtn, shareBtn, csvBtn, printBtn, refresh); top2.setAlignment(Pos.CENTER_LEFT);
        ScrollPane scroll = new ScrollPane(sheet); scroll.setFitToWidth(true); scroll.setStyle("-fx-background-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox page = new VBox(10, top, desc, top2, bar, scroll, status);
        page.setPadding(new Insets(14));
        reload[0].run();
        return page;
    }

    private static Path exports() { return Paths.get(System.getProperty("user.home"), ".j-log", "exports"); }
    private static Def find(ComboBox<Def> pick, Def saved) {
        for (Def d : pick.getItems()) if (d.id.equals(saved.id)) return d;
        return saved;
    }

    // ---- sheet rows --------------------------------------------------------------------------------
    private static Label c(String s, double w, String cls) {
        Label l = lbl(s == null ? "" : s, cls);
        if (w > 0) { l.setMinWidth(w); l.setPrefWidth(w); } else { HBox.setHgrow(l, Priority.ALWAYS); l.setMaxWidth(Double.MAX_VALUE); }
        return l;
    }
    private static Node head(boolean tracked) {
        HBox h = new HBox(9, c("", 26, "jl-logc-h"), c("ITEM", -1, "jl-logc-h"), c("STATUS", 86, "jl-logc-h"), c("FIRST WORKED", 100, "jl-logc-h"),
                c("CALL", 100, "jl-logc-h"), c("BAND", 52, "jl-logc-h"), c("MODE", 56, "jl-logc-h"), c("QSOs", 44, "jl-logc-h"));
        if (tracked) h.getChildren().add(c("BANDS DONE", 170, "jl-logc-h"));
        h.getStyleClass().add("jl-loghd"); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
    private static Node row(CustomAwards.Row r, boolean tracked, Def d) {
        Label tick = c(r.confirmed ? "✔" : r.worked ? "●" : "○", 26, "jl-logc");
        tick.setStyle("-fx-font-size:15px;-fx-text-fill:" + (r.confirmed ? "-ars-ok" : r.worked ? "-ars-warn" : "-ars-t4") + ";");
        Label status = c(r.confirmed ? "Confirmed" : r.worked ? "Worked" : "Needed", 86, "jl-logc");
        status.setStyle("-fx-text-fill:" + (r.confirmed ? "-ars-ok" : r.worked ? "-ars-warn" : "-ars-t3") + ";");
        CustomAwards.Hit f = r.first;
        HBox row = new HBox(9, tick, c(r.item.label, -1, "jl-logc"), status,
                c(f == null ? "" : f.date(), 100, "jl-logc"), c(f == null ? "" : f.call(), 100, "jl-logc"),
                c(f == null ? "" : f.band(), 52, "jl-logc"), c(f == null ? "" : f.mode(), 56, "jl-logc"),
                c(r.qsos == 0 ? "" : String.valueOf(r.qsos), 44, "jl-logc"));
        if (tracked) {     // each tracked band: confirmed / worked / still needed
            StringBuilder sb = new StringBuilder();
            for (String b : d.trackBands) {
                String k = b.trim().toLowerCase(java.util.Locale.ROOT);
                sb.append(b.trim()).append(r.bandsConfirmed.contains(k) ? "\u2714 " : r.bandsWorked.contains(k) ? "\u25cf " : "\u25cb ");
            }
            row.getChildren().add(c(sb.toString().trim(), 170, "jl-logc"));
        }
        row.getStyleClass().add("jl-logrow"); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---- editor -------------------------------------------------------------------------------------
    private static void edit(Node owner, Def src, java.util.function.Consumer<Def> onSaved) {
        TextField name = new TextField(src.name);
        TextArea desc = new TextArea(src.description); desc.setPrefRowCount(2); desc.setWrapText(true);
        ComboBox<String> field = new ComboBox<>(); field.getItems().addAll(CustomAwards.FIELDS); field.setValue(src.field);
        ComboBox<String> how = new ComboBox<>(); how.getItems().addAll(CustomAwards.HOWS); how.setValue(src.how);
        Label fieldHelp = lbl("", "jl-emeta"); fieldHelp.setWrapText(true);
        Runnable help = () -> fieldHelp.setText("Matches " + CustomAwards.fieldHelp().getOrDefault(field.getValue(), ""));
        field.setOnAction(e -> help.run()); help.run();
        TextField bands = new TextField(String.join(", ", src.bands)); bands.setPromptText("any band  (e.g. 20m, 40m)");
        TextField modes = new TextField(String.join(", ", src.modes)); modes.setPromptText("any mode  (e.g. CW, SSB, FT8)");
        TextField from = new TextField(src.from); from.setPromptText("yyyy-mm-dd"); from.setPrefWidth(120);
        TextField to = new TextField(src.to); to.setPromptText("yyyy-mm-dd"); to.setPrefWidth(120);
        TextField track = new TextField(String.join(", ", src.trackBands)); track.setPromptText("optional: each item must be worked on all of these bands (e.g. 20m, 40m)");
        CheckBox confOnly = new CheckBox("Only confirmed QSOs count"); confOnly.setSelected(src.confirmedOnly);
        TextArea items = new TextArea(CustomAwards.itemsText(src.items)); items.setPrefRowCount(12);
        items.setPromptText("One item per line.\nK2A\nWM3GPO (bonus) = WM3GPO\nOhio = OH");
        Label itemHelp = lbl("One item per line. Just a name, or  Name = text1, text2  when the QSO shows something different from the name.", "jl-emeta");
        itemHelp.setWrapText(true);

        VBox box = new VBox(8); box.setPadding(new Insets(16)); box.setPrefWidth(560);
        box.getChildren().addAll(lbl(src.id == null || src.id.isBlank() ? "New award" : "Edit award", "jhub-card-title"),
                lbl("NAME", "jl-flabel"), name, lbl("DESCRIPTION", "jl-flabel"), desc,
                lbl("WHAT COUNTS", "jl-flabel"), new HBox(10, lbl("Match the", "jl-emeta"), field, how, lbl("of the QSO", "jl-emeta")), fieldHelp,
                lbl("ONLY QSOs ON (optional)", "jl-flabel"), new HBox(10, bands, modes),
                new HBox(10, lbl("between", "jl-emeta"), from, lbl("and", "jl-emeta"), to), confOnly,
                lbl("TRACK EACH ITEM ON EVERY BAND (optional)", "jl-flabel"), track,
                lbl("ITEMS", "jl-flabel"), items, itemHelp);
        for (Node n : box.getChildren()) if (n instanceof HBox h) { h.setAlignment(Pos.CENTER_LEFT); for (Node k : h.getChildren()) if (k instanceof TextField) HBox.setHgrow(k, Priority.ALWAYS); }

        Label msg = lbl("", "jl-emeta"); msg.setStyle("-fx-text-fill:-ars-err;");
        Label cancel = lbl("Cancel", "jl-clearbtn"); cancel.setStyle("-fx-cursor:hand;");
        Label save = lbl("Save", "jl-logbtn"); save.setStyle("-fx-cursor:hand;");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox btns = new HBox(14, msg, gap, cancel, save); btns.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(btns);

        ScrollPane sp = new ScrollPane(box); sp.setFitToWidth(true);
        javafx.scene.Scene scene = new javafx.scene.Scene(sp, 600, 700);
        com.ars.fx.Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        com.ars.fx.Themes.applyTo(sp, "log");
        Stage st = new Stage(); st.setTitle("Award sheet"); st.setScene(scene); st.initModality(Modality.APPLICATION_MODAL);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        cancel.setOnMouseClicked(e -> st.close());
        save.setOnMouseClicked(e -> {
            Def d = new Def();
            d.id = src.id;
            d.name = name.getText().trim();
            d.description = desc.getText().trim();
            d.field = field.getValue(); d.how = how.getValue();
            d.bands = split(bands.getText()); d.modes = split(modes.getText());
            d.from = from.getText().trim(); d.to = to.getText().trim();
            d.confirmedOnly = confOnly.isSelected();
            d.trackBands = split(track.getText());
            d.items = CustomAwards.parseItems(items.getText());
            if (d.name.isEmpty()) { msg.setText("Give the award a name."); return; }
            if (d.items.isEmpty()) { msg.setText("Add at least one item."); return; }
            if ((!d.from.isEmpty() && !d.from.matches("\\d{4}-\\d{2}-\\d{2}")) || (!d.to.isEmpty() && !d.to.matches("\\d{4}-\\d{2}-\\d{2}"))) {
                msg.setText("Dates are yyyy-mm-dd."); return;
            }
            try { onSaved.accept(CustomAwards.save(d)); st.close(); }
            catch (Exception ex) { msg.setText("Could not save: " + ex.getMessage()); }
        });
        st.show();
    }
    private static List<String> split(String s) {
        List<String> out = new java.util.ArrayList<>();
        if (s != null) for (String p : s.split("[,;]")) if (!p.isBlank()) out.add(p.trim());
        return out;
    }
}
