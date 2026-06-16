package com.ars.fx.surface;

import com.ars.fx.data.JVaultDb;
import com.ars.fx.data.JVaultDb.Cat;
import com.ars.fx.data.JVaultDb.Item;
import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.IntConsumer;

import static com.ars.fx.shell.Shell.lbl;

/** J-Vault — station inventory & estate planning, backed by the real ~/.j-vault/inventory.db. */
public final class JVaultView {
    private JVaultView() {}

    public static Region build() {
        Region dock = Shell.dock("vault");
        JVaultDb.Totals t0 = JVaultDb.totals();
        String[][] stats = {{"ITEMS", String.valueOf(t0.count())}, {"TOTAL VALUE", money(t0.value()), "vault"},
                {"INSTALLED", t0.installed() + "/" + t0.count()}};
        HBox top = Shell.topBar("vault", "vault", "J-Vault", "Station inventory & estate planning", stats, null);

        // state
        String[] query = {""};
        String[] cat = {"All"};
        int[] sel = {-1};

        VBox summaryBox = new VBox(); summaryBox.setMinHeight(Region.USE_PREF_SIZE);
        Runnable refreshSummary = () -> { JVaultDb.Totals t = JVaultDb.totals();
            summaryBox.getChildren().setAll(summaryCards(t, JVaultDb.contacts().size())); };
        refreshSummary.run();

        VBox detailBody = new VBox(9);
        StackPane mainHost = new StackPane(); mainHost.setAlignment(Pos.TOP_LEFT);

        Runnable[] refreshTable = {null};
        IntConsumer[] onSelectH = {null};
        IntConsumer[] onDeleteH = {null};
        IntConsumer[] openFormH = {null};

        Runnable backToTable = () -> { refreshTable[0].run(); refreshSummary.run(); };
        openFormH[0] = id -> mainHost.getChildren().setAll(formView(id, backToTable));
        onDeleteH[0] = id -> {
            if (JVaultDb.deleteItem(id)) { if (sel[0] == id) { sel[0] = -1; placeholderDetail(detailBody); } backToTable.run(); }
        };
        onSelectH[0] = id -> { sel[0] = id; showDetail(detailBody, JVaultDb.item(id), openFormH[0], onDeleteH[0]); };
        refreshTable[0] = () -> mainHost.getChildren().setAll(tableView(query[0], cat[0], sel[0], onSelectH[0]));

        placeholderDetail(detailBody);
        refreshTable[0].run();

        Label exportStatus = lbl("", "jv-item-sub");
        Region controls = searchRow(query, cat, refreshTable[0], () -> openFormH[0].accept(-1), exportStatus);

        VBox center = new VBox(16, summaryBox, controls, mainHost, exportStatus);
        center.setPadding(new Insets(18, 24, 24, 24)); center.setStyle("-fx-background-color:-ars-bg;");
        ScrollPane sp = new ScrollPane(center); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setStyle("-fx-background-color:-ars-bg;");
        HBox.setHgrow(sp, Priority.ALWAYS);

        Region rail = Shell.rail(new Node[]{
                Shell.drawer("Item detail", "vault", "vault", "", true, detailBody),
                Shell.drawer("Estate · first-call contacts", "learn", "learn", "", true, estateBody())});
        return Shell.frame(dock, top, sp, rail);
    }

    // ---- summary cards -----------------------------------------------------
    private static List<Node> summaryCards(JVaultDb.Totals t, int contacts) {
        HBox h = new HBox(16);
        String[][] cards = {{"INVENTORY ITEMS", String.valueOf(t.count())},
                {"TOTAL EST. VALUE", money(t.value())},
                {"INSTALLED", t.installed() + " / " + t.count()},
                {"ESTATE CONTACTS", String.valueOf(contacts)}};
        for (String[] s : cards) {
            VBox card = new VBox(6, lbl(s[0], "jv-sum-k"), lbl(s[1], "jv-sum-v")); card.getStyleClass().add("jv-sum");
            HBox.setHgrow(card, Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE);
            h.getChildren().add(card);
        }
        return List.of(h);
    }

    // ---- search / filter / actions ----------------------------------------
    private static Region searchRow(String[] query, String[] cat, Runnable refresh, Runnable onAdd, Label status) {
        TextField search = new TextField(); search.setPromptText("🔍  Search inventory…");
        search.getStyleClass().add("jhub-mini-field"); search.setMinWidth(240); search.setMaxWidth(280);
        search.textProperty().addListener((o, a, b) -> { query[0] = b; refresh.run(); });

        ComboBox<String> catBox = new ComboBox<>(); catBox.getItems().add("All");
        for (Cat c : JVaultDb.categories()) catBox.getItems().add(c.name());
        catBox.setValue("All"); catBox.setMinWidth(200);
        catBox.valueProperty().addListener((o, a, b) -> { cat[0] = b == null ? "All" : b; refresh.run(); });

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label add = btn("+ Add item", true);
        add.setOnMouseClicked(e -> onAdd.run());
        Label export = btn("Export CSV", false);
        export.setOnMouseClicked(e -> {
            try {
                Path dir = Paths.get(System.getProperty("user.home"), ".j-vault", "backups"); Files.createDirectories(dir);
                Path f = dir.resolve("inventory-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv");
                Files.writeString(f, JVaultDb.exportCsv());
                status.setText("✓ Exported " + JVaultDb.totals().count() + " items → " + f);
            } catch (Exception ex) { status.setText("✗ Export failed: " + ex.getMessage()); }
        });
        HBox row = new HBox(12, search, catBox, sp, export, add); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---- inventory table ---------------------------------------------------
    private static final double[] VC = {150, -1, 92, 110, 120, 58};
    private static Region tableView(String query, String cat, int sel, IntConsumer onSelect) {
        VBox tbl = new VBox();
        HBox hd = new HBox(12); hd.getStyleClass().add("jv-th"); hd.setAlignment(Pos.CENTER_LEFT);
        String[] cols = {"CATEGORY", "ITEM", "VALUE", "DISPOSITION", "INSTALL", ""};
        for (int i = 0; i < cols.length; i++) hd.getChildren().add(cell(lbl(cols[i]), i));
        tbl.getChildren().add(hd);

        List<Item> items = JVaultDb.items(query, cat);
        if (items.isEmpty()) {
            Label none = lbl("No items" + (query.isBlank() && "All".equals(cat) ? " yet — add one above." : " match the filter."));
            none.setStyle("-fx-font-size:12px;-fx-text-fill:-ars-t4;-fx-padding:18 4 18 4;");
            tbl.getChildren().add(none);
        } else {
            for (Item it : items) tbl.getChildren().add(itemRow(it, sel, onSelect));
        }
        return tbl;
    }
    private static Region itemRow(Item it, int sel, IntConsumer onSelect) {
        HBox r = new HBox(12); r.getStyleClass().add("jv-row"); r.setAlignment(Pos.CENTER_LEFT);
        r.setStyle("-fx-cursor:hand;" + (it.id() == sel ? "-fx-background-color:rgba(127,127,127,0.08);" : ""));
        VBox item = new VBox(1, lbl(it.title().isBlank() ? "(unnamed)" : it.title(), "jv-item-nm"),
                lbl(it.serial() == null || it.serial().isBlank() ? "—" : "S/N " + it.serial(), "jv-item-sub"));
        Region dot = new Region(); dot.getStyleClass().add("jv-disp-dot"); dot.setStyle("-fx-background-color:" + dispColor(it.disposition()) + ";");
        HBox disp = new HBox(8, dot, lbl(nv(it.disposition()), "jv-disp")); disp.setAlignment(Pos.CENTER_LEFT);
        r.getChildren().addAll(cell(catTag(it.category()), 0), cell(item, 1), cell(lbl(money(it.estimatedValue()), "jv-val"), 2),
                cell(disp, 3), cell(lbl(nv(it.installStatus()), "jv-disp"), 4));
        Label edit = lbl("›", "jv-disp"); edit.setStyle("-fx-text-fill:-ars-t4;-fx-font-size:15px;");
        r.getChildren().add(cell(edit, 5));
        r.setOnMouseClicked(e -> onSelect.accept(it.id()));
        return r;
    }

    // ---- add / edit form ---------------------------------------------------
    private static Region formView(int id, Runnable back) {
        Item ex = id >= 0 ? JVaultDb.item(id) : null;
        List<Cat> cats = JVaultDb.categories();
        ComboBox<String> category = new ComboBox<>(); for (Cat c : cats) category.getItems().add(c.name());
        category.setValue(ex != null ? ex.category() : (cats.isEmpty() ? null : cats.get(0).name())); category.setMinWidth(220);
        TextField mfr = f(ex == null ? "" : ex.manufacturer(), "Icom");
        TextField model = f(ex == null ? "" : ex.model(), "IC-7610");
        TextField serial = f(ex == null ? "" : ex.serial(), "serial #");
        TextField acq = f(ex == null ? "" : ex.dateAcquired(), "YYYY-MM-DD");
        TextField purchase = f(ex == null || ex.purchasePrice() == 0 ? "" : trim0(ex.purchasePrice()), "0");
        TextField value = f(ex == null || ex.estimatedValue() == 0 ? "" : trim0(ex.estimatedValue()), "0");
        ComboBox<String> disp = combo(JVaultDb.DISPOSITIONS, ex == null ? "working" : ex.disposition());
        ComboBox<String> install = combo(JVaultDb.INSTALL, ex == null ? "installed" : ex.installStatus());
        TextField location = f(ex == null ? "" : ex.location(), "shack / closet / tower");
        TextField notes = f(ex == null ? "" : ex.notes(), "notes"); notes.setMaxWidth(Double.MAX_VALUE);

        HBox r1 = new HBox(12, fc("CATEGORY", category), fc("MANUFACTURER", mfr), fc("MODEL", model));
        HBox r2 = new HBox(12, fc("SERIAL #", serial), fc("ACQUIRED", acq), fc("PURCHASE $", purchase), fc("EST. VALUE $", value));
        HBox r3 = new HBox(12, fc("DISPOSITION", disp), fc("INSTALL", install), fc("LOCATION", location));
        VBox notesCell = fc("NOTES", notes); HBox.setHgrow(notesCell, Priority.ALWAYS);
        HBox r4 = new HBox(12, notesCell);

        Label save = btn(id >= 0 ? "Save changes" : "Add to inventory", true);
        Label cancel = btn("Cancel", false);
        Label del = id >= 0 ? btn("Delete", false) : null;
        save.setOnMouseClicked(e -> {
            String cn = category.getValue();
            int typeId = cats.stream().filter(c -> c.name().equals(cn)).map(Cat::id).findFirst().orElse(-1);
            if (typeId < 0) return;
            double pp = num(purchase.getText()), ev = num(value.getText());
            if (id >= 0) JVaultDb.updateItem(id, typeId, mfr.getText(), model.getText(), serial.getText(), acq.getText(), pp, ev, disp.getValue(), install.getValue(), location.getText(), notes.getText());
            else JVaultDb.addItem(typeId, mfr.getText(), model.getText(), serial.getText(), acq.getText(), pp, ev, disp.getValue(), install.getValue(), location.getText(), notes.getText());
            back.run();
        });
        cancel.setOnMouseClicked(e -> back.run());
        boolean[] armed = {false};
        if (del != null) del.setOnMouseClicked(e -> {
            if (!armed[0]) { armed[0] = true; del.setText("Delete? click again"); }
            else { JVaultDb.deleteItem(id); back.run(); }
        });
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox actions = new HBox(12); actions.setAlignment(Pos.CENTER_LEFT);
        if (del != null) actions.getChildren().add(del);
        actions.getChildren().addAll(sp, cancel, save);

        VBox card = new VBox(14, lbl(id >= 0 ? "Edit item" : "New inventory item", "jhub-card-title"),
                r1, r2, r3, r4, actions);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color:-ars-surface-1;-fx-border-color:-ars-border;-fx-border-width:1;-fx-background-radius:10;-fx-border-radius:10;");
        card.setMaxWidth(1000);
        return card;
    }

    // ---- rail: item detail -------------------------------------------------
    private static void placeholderDetail(VBox body) {
        body.getChildren().setAll(lbl("Select an item to see its details, or add a new one.", "jv-note"));
        ((Label) body.getChildren().get(0)).setWrapText(true);
    }
    private static void showDetail(VBox body, Item it, IntConsumer onEdit, IntConsumer onDelete) {
        if (it == null) { placeholderDetail(body); return; }
        body.getChildren().clear();
        body.getChildren().add(new VBox(1, lbl(it.title().isBlank() ? "(unnamed)" : it.title(), "jv-dname"),
                lbl(it.category() + (it.dateAcquired() == null || it.dateAcquired().isBlank() ? "" : " · " + it.dateAcquired()), "jv-dsub")));
        GridPane g = new GridPane(); g.setHgap(20); g.setVgap(11);
        g.add(dkv("EST. VALUE", money(it.estimatedValue())), 0, 0); g.add(dkv("DISPOSITION", nv(it.disposition())), 1, 0);
        g.add(dkv("PURCHASE", money(it.purchasePrice())), 0, 1); g.add(dkv("INSTALL", nv(it.installStatus())), 1, 1);
        g.add(dkv("SERIAL", nv(it.serial())), 0, 2); g.add(dkv("LOCATION", nv(it.location())), 1, 2);
        for (int c = 0; c < 2; c++) { ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(50); g.getColumnConstraints().add(cc); }
        body.getChildren().add(g);
        if (it.notes() != null && !it.notes().isBlank()) {
            Label note = lbl(it.notes(), "jv-note"); note.setWrapText(true); note.setMaxWidth(Double.MAX_VALUE); body.getChildren().add(note);
        }
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label edit = btn("Edit", true), del = btn("Delete", false);
        edit.setOnMouseClicked(e -> onEdit.accept(it.id()));
        boolean[] armed = {false};
        del.setOnMouseClicked(e -> { if (!armed[0]) { armed[0] = true; del.setText("Delete?"); } else onDelete.accept(it.id()); });
        HBox actions = new HBox(10, edit, sp, del); actions.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().add(actions);
    }

    // ---- rail: estate contacts --------------------------------------------
    private static VBox estateBody() {
        VBox box = new VBox(9);
        Runnable[] refresh = {null};
        VBox list = new VBox(0);
        refresh[0] = () -> {
            list.getChildren().clear();
            List<JVaultDb.Contact> cs = JVaultDb.contacts();
            if (cs.isEmpty()) { Label e = lbl("No first-call contacts yet.", "jv-note"); e.setWrapText(true); list.getChildren().add(e); }
            else for (JVaultDb.Contact c : cs) {
                VBox txt = new VBox(1, lbl(c.name() + (c.callsign() == null || c.callsign().isBlank() ? "" : " · " + c.callsign()), "jv-doc-t"),
                        lbl(nv(c.relationship()) + (c.itemsWanted() == null || c.itemsWanted().isBlank() ? "" : " — " + c.itemsWanted()), "jv-doc-s"));
                HBox.setHgrow(txt, Priority.ALWAYS); txt.setMaxWidth(Double.MAX_VALUE);
                Label x = lbl("✕", "jv-doc-d"); x.setStyle("-fx-cursor:hand;-fx-text-fill:-ars-t4;");
                boolean[] armed = {false};
                x.setOnMouseClicked(e -> { if (!armed[0]) { armed[0] = true; x.setText("del?"); x.setStyle("-fx-cursor:hand;-fx-text-fill:-ars-err;"); } else { JVaultDb.deleteContact(c.id()); refresh[0].run(); } });
                HBox row = new HBox(10, txt, x); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jv-doc");
                list.getChildren().add(row);
            }
        };
        refresh[0].run();
        TextField nm = mini("name"), cs = mini("callsign"), rel = mini("relationship"), want = mini("items wanted");
        Label add = btn("Add contact", true);
        add.setOnMouseClicked(e -> { if (!nm.getText().isBlank()) { JVaultDb.addContact(nm.getText(), cs.getText(), rel.getText(), want.getText());
            for (TextField t : new TextField[]{nm, cs, rel, want}) t.clear(); refresh[0].run(); } });
        VBox form = new VBox(6, new HBox(6, nm, cs), new HBox(6, rel, want), add);
        box.getChildren().addAll(list, form);
        return box;
    }

    // ---- helpers -----------------------------------------------------------
    private static Label btn(String text, boolean primary) {
        Label b = lbl(text, "cfg-select"); b.setStyle("-fx-cursor:hand;-fx-padding:6 14 6 14;-fx-background-radius:7;"
                + (primary ? "-fx-background-color:-ars-vault;-fx-text-fill:#06121a;-fx-font-weight:bold;" : "-fx-background-color:-ars-surface-3;-fx-text-fill:-ars-t2;"));
        return b;
    }
    private static TextField f(String val, String prompt) {
        TextField t = new TextField(val == null ? "" : val); t.setPromptText(prompt); t.getStyleClass().add("jhub-mini-field"); t.setMinWidth(120);
        return t;
    }
    private static TextField mini(String prompt) { TextField t = new TextField(); t.setPromptText(prompt); t.getStyleClass().add("jhub-mini-field"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE); return t; }
    private static ComboBox<String> combo(String[] opts, String val) {
        ComboBox<String> c = new ComboBox<>(); for (String o : opts) c.getItems().add(o); c.setValue(val == null || val.isBlank() ? opts[0] : val); c.setMinWidth(150); return c;
    }
    private static VBox fc(String label, javafx.scene.control.Control control) {
        control.setMaxWidth(Double.MAX_VALUE);
        VBox v = new VBox(5, lbl(label, "jl-flabel"), control); HBox.setHgrow(v, Priority.ALWAYS); v.setMaxWidth(Double.MAX_VALUE); return v;
    }
    private static Label catTag(String c) { return lbl(c == null ? "—" : c, "jv-cat-tag"); }
    private static VBox dkv(String k, String v) { return new VBox(2, lbl(k, "jv-dk"), lbl(v, "jv-dv")); }
    private static Region cell(Node n, int i) {
        HBox c = new HBox(n); c.setAlignment(Pos.CENTER_LEFT);
        if (VC[i] < 0) { HBox.setHgrow(c, Priority.ALWAYS); c.setMaxWidth(Double.MAX_VALUE); }
        else { c.setMinWidth(VC[i]); c.setPrefWidth(VC[i]); c.setMaxWidth(VC[i]); }
        return c;
    }
    private static String dispColor(String d) {
        if (d == null) return "-ars-t4";
        return switch (d) {
            case "working" -> "-ars-ok"; case "needs repair" -> "-ars-warn";
            case "for sale", "sold" -> "-ars-vault"; case "retired" -> "-ars-err"; default -> "-ars-t3";
        };
    }
    private static String nv(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private static String money(double v) { return v == 0 ? "—" : "$" + String.format("%,.0f", v); }
    private static String trim0(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v); }
    private static double num(String s) { try { return Double.parseDouble(s.replaceAll("[$,]", "").trim()); } catch (Exception e) { return 0; } }
}
