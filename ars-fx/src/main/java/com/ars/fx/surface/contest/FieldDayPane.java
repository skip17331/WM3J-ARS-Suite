package com.ars.fx.surface.contest;

import com.ars.fx.data.ContestState;
import com.ars.fx.data.FieldDaySheet;
import com.ars.fx.data.HubConfig;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The Field Day summary-sheet worksheet: transmitters on emergency power, the power multiplier and the points for each bonus, typed in,
 * with the entry's total worked out live from the logged QSO points. Each bonus is claimed with a count and is worth what the 2026
 * rules say, capped as they cap it ({@link FieldDaySheet}). What you type is kept for next time.
 */
public final class FieldDayPane {
    private FieldDayPane() {}

    public static boolean applies(String contestId) { return "ARRL_FIELD_DAY".equals(contestId); }

    public static Region build() {
        TextField tx = num("contest.fd.tx", "0");
        ComboBox<String> mult = new ComboBox<>();
        for (int m : FieldDaySheet.POWER_MULTIPLIERS)
            mult.getItems().add(m == 5 ? "5 - 5 W or less, battery / solar" : m == 2 ? "2 - up to 100 W (or 5 W on mains / generator)" : "1 - over 100 W");
        int saved = Math.max(1, FieldDaySheet.number(HubConfig.get("contest.fd.mult", "1")));
        mult.getSelectionModel().select(saved >= 5 ? 0 : saved >= 2 ? 1 : 2);
        List<TextField> counts = new ArrayList<>();
        List<Label> pts = new ArrayList<>();
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(3);
        int row = 0;
        grid.add(lbl("100% emergency power: transmitters (100 each, max 20)", "ct-note"), 0, row); grid.add(tx, 1, row++);
        for (FieldDaySheet.Category c : FieldDaySheet.CATEGORIES) {
            TextField f = num("contest.fd.claim." + c.key(), "0");
            f.setPromptText(c.maxUnits() == 1 ? "0/1" : "n");
            counts.add(f);
            Label p = lbl("", "ct-note"); pts.add(p);
            String each = c.maxUnits() == 1 ? "" : " each" + (c.maxUnits() > 0 ? ", max " + c.maxUnits() * c.unitPoints() : "");
            String unit = c.unit().isEmpty() ? "" : ", " + c.unit();
            grid.add(lbl(c.name() + "  (" + c.unitPoints() + each + "; " + c.classes() + unit + ")", "ct-note"), 0, row);
            grid.add(f, 1, row); grid.add(p, 2, row++);
        }
        Label result = lbl("", "jhub-card-title"), parts = lbl("", "ct-note"); parts.setWrapText(true); parts.setMaxWidth(320);
        Runnable calc = () -> {
            long b = FieldDaySheet.emergencyPower(FieldDaySheet.number(tx.getText()));
            for (int i = 0; i < counts.size(); i++) {
                int n = FieldDaySheet.CATEGORIES.get(i).points(FieldDaySheet.number(counts.get(i).getText()));
                pts.get(i).setText(n == 0 ? "" : "= " + n);
                b += n;
            }
            int q = ContestState.score().points(), m = FieldDaySheet.POWER_MULTIPLIERS[Math.max(0, mult.getSelectionModel().getSelectedIndex())];
            result.setText("Claimed score: " + String.format("%,d", FieldDaySheet.total(q, m, b)));
            parts.setText(q + " QSO points × " + m + " power multiplier + " + b + " bonus points");
        };
        for (TextField f : counts) f.textProperty().addListener((o, a, v) -> calc.run());
        tx.textProperty().addListener((o, a, v) -> calc.run());
        mult.valueProperty().addListener((o, a, v) -> {
            HubConfig.set("contest.fd.mult", String.valueOf(FieldDaySheet.POWER_MULTIPLIERS[Math.max(0, mult.getSelectionModel().getSelectedIndex())]));
            calc.run();
        });
        ContestState.addListener(() -> Platform.runLater(calc));
        calc.run();
        Label multRow = lbl("Power multiplier (the highest power of any contact)", "ct-note");
        Label note = lbl("ARRL does not score Field Day from the log: you declare the power multiplier and bonuses on the summary sheet. "
                + "Bonus values are from the 2026 rules (revised March 1, 2026); enter 1 to claim a bonus, or the count for the ones that score each. "
                + "The GOTA and free VHF stations do not count toward the emergency-power transmitters.", "ct-note");
        note.setWrapText(true); note.setMaxWidth(320);
        GridPane top = new GridPane(); top.setHgap(8); top.add(multRow, 0, 0); top.add(mult, 1, 0);
        VBox box = new VBox(8, top, grid, result, parts, note);
        box.setPadding(new Insets(2));
        return box;
    }

    private static TextField num(String key, String def) {
        TextField t = new TextField(HubConfig.get(key, def)); t.getStyleClass().add("jhub-mini-field"); t.setPrefWidth(60); t.setMaxWidth(60);
        t.textProperty().addListener((o, a, v) -> HubConfig.set(key, v == null ? "" : v.trim()));
        t.setAlignment(Pos.CENTER_RIGHT);
        return t;
    }
}
