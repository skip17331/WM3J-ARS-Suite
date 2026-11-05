package com.ars.fx.surface;

import com.ars.fx.data.HubConfig;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ars.fx.shell.Shell.lbl;

/**
 * What the operator SENDS in a contest (the entryRow-1 fields of a contest definition: precedence, check,
 * section, zone, state, ...). Each value comes from J-Hub Setup where Setup already holds it, otherwise from an
 * answer saved the first time it was asked ({@code contest.sent.<fieldId>}); anything still unknown is asked for
 * when the contest page opens, so the sent exchange is never a row of empty boxes.
 */
final class SentExchange {
    private SentExchange() {}

    /** Contest fields that are the operator's own report/serial, filled elsewhere. */
    private static boolean automatic(String id) { return id.equals("rst_sent") || id.equals("serial_sent"); }

    /** The sent-side fields of a contest, in the definition's order. */
    static List<FieldDef> sentFields(ContestPlugin plugin) {
        List<FieldDef> out = new ArrayList<>();
        for (FieldDef fd : plugin.getEntryFields())
            if (fd.getEntryRow() == 1 && !"band".equals(fd.getId()) && !"mode".equals(fd.getId())) out.add(fd);
        return out;
    }

    /** Config key that holds this field's value. Setup's own keys are used where Setup already asks for it. */
    private static String key(String fieldId) {
        return switch (fieldId) {
            case "prec_sent"  -> "contest.ss.precedence";
            case "check_sent" -> "contest.ss.check";
            case "sect_sent"  -> "contest.ss.section";
            case "zone_sent"  -> "station.cqZone";
            case "grid_sent"  -> "station.grid";
            case "name_sent"  -> "station.name";
            case "power_sent" -> "station.txPower";
            default           -> "contest.sent." + fieldId;
        };
    }

    /** The value to send for a field, or "" if it is not known yet. */
    static String value(FieldDef fd, String mode) {
        if (fd.getId().equals("rst_sent")) return isPhone(mode) ? "59" : "599";
        String v = HubConfig.get(key(fd.getId()), "").trim();
        return v.isEmpty() ? "" : v.toUpperCase();
    }
    static void save(String fieldId, String v) { HubConfig.set(key(fieldId), v == null ? "" : v.trim()); }

    static boolean isPhone(String mode) {
        if (mode == null) return false;
        String m = mode.trim().toUpperCase();
        return m.equals("SSB") || m.equals("USB") || m.equals("LSB") || m.equals("AM") || m.equals("FM") || m.contains("PHONE");
    }

    /** Sent fields with no value yet (excluding the automatic ones). */
    static List<FieldDef> missing(ContestPlugin plugin, String mode) {
        List<FieldDef> out = new ArrayList<>();
        for (FieldDef fd : sentFields(plugin)) if (!automatic(fd.getId()) && value(fd, mode).isEmpty()) out.add(fd);
        return out;
    }

    /** Ask for the given sent fields (all of them when {@code which} is every sent field, i.e. "Edit"). */
    static void ask(Node owner, ContestPlugin plugin, List<FieldDef> which, String mode, Runnable onSaved) {
        if (which.isEmpty()) return;
        VBox box = new VBox(10); box.setPadding(new Insets(16));
        box.getChildren().add(lbl("Your exchange — " + plugin.getContestName(), "jhub-card-title"));
        Label why = lbl("This is what you send. It is remembered, and the station details also live in J-Hub ▸ Setup.", "jl-emeta");
        why.setWrapText(true); why.setMaxWidth(420);
        box.getChildren().add(why);
        Map<String, Control> inputs = new LinkedHashMap<>();
        for (FieldDef fd : which) {
            if (automatic(fd.getId())) continue;
            Control c;
            if (fd.getOptions() != null && !fd.getOptions().isEmpty() && fd.getOptions().size() <= 12) {
                ComboBox<String> cb = new ComboBox<>(); cb.getItems().addAll(fd.getOptions()); cb.setEditable(true);
                cb.setValue(value(fd, mode)); cb.setPrefWidth(160); c = cb;
            } else {
                TextField t = new TextField(value(fd, mode)); t.setPrefWidth(160); c = t;
            }
            Label l = lbl(fd.getLabel() == null ? fd.getId() : fd.getLabel(), "jl-flabel"); l.setMinWidth(90);
            HBox row = new HBox(10, l, c); row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
            inputs.put(fd.getId(), c);
        }
        Label skip = lbl("Not now", "jl-clearbtn"); skip.setStyle("-fx-cursor:hand;");
        Label save = lbl("Save", "jl-logbtn");      save.setStyle("-fx-cursor:hand;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox btns = new HBox(14, skip, sp, save); btns.setAlignment(Pos.CENTER_LEFT); btns.setPadding(new Insets(8, 0, 0, 0));
        box.getChildren().add(btns);

        javafx.scene.Scene scene = new javafx.scene.Scene(box);
        com.ars.fx.Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        com.ars.fx.Themes.applyTo(box, "log");
        javafx.stage.Stage st = new javafx.stage.Stage(); st.setTitle("Contest exchange"); st.setScene(scene);
        st.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        skip.setOnMouseClicked(e -> st.close());
        save.setOnMouseClicked(e -> {
            for (Map.Entry<String, Control> en : inputs.entrySet()) {
                Control c = en.getValue();
                String v = c instanceof ComboBox<?> cb ? (cb.getEditor().getText() == null ? "" : cb.getEditor().getText())
                        : ((TextField) c).getText();
                if (v != null && !v.isBlank()) save(en.getKey(), v);
            }
            st.close();
            if (onSaved != null) onSaved.run();
        });
        st.show();
    }
}
