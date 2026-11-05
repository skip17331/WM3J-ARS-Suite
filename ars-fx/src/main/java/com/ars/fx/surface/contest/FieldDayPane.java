package com.ars.fx.surface.contest;

import com.ars.fx.data.ContestState;
import com.ars.fx.data.FieldDaySheet;
import com.ars.fx.data.HubConfig;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
 * with the entry's total worked out live from the logged QSO points. What a bonus is worth is entered from the current year's rules
 * ({@link FieldDaySheet}). What you type is kept for next time.
 */
public final class FieldDayPane {
    private FieldDayPane() {}

    public static boolean applies(String contestId) { return "ARRL_FIELD_DAY".equals(contestId); }

    public static Region build() {
        TextField tx = num("contest.fd.tx", "0"), mult = num("contest.fd.mult", "1");
        List<TextField> bonus = new ArrayList<>();
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(3);
        int row = 0;
        grid.add(lbl("100% emergency power: transmitters (100 each, max 20)", "ct-note"), 0, row); grid.add(tx, 1, row++);
        for (FieldDaySheet.Category c : FieldDaySheet.CATEGORIES) {
            TextField f = num("contest.fd.bonus." + c.key(), c.points() >= 0 ? String.valueOf(c.points()) : "0");
            f.setPromptText("pts");
            bonus.add(f);
            grid.add(lbl(c.name(), "ct-note"), 0, row); grid.add(f, 1, row++);
        }
        Label result = lbl("", "jhub-card-title"), parts = lbl("", "ct-note"); parts.setWrapText(true); parts.setMaxWidth(320);
        Runnable calc = () -> {
            long b = FieldDaySheet.emergencyPower(FieldDaySheet.number(tx.getText()));
            for (TextField f : bonus) b += FieldDaySheet.number(f.getText());
            int q = ContestState.score().points(), m = FieldDaySheet.number(mult.getText());
            result.setText("Claimed score: " + String.format("%,d", FieldDaySheet.total(q, m, b)));
            parts.setText(q + " QSO points × " + Math.max(1, m) + " power multiplier + " + b + " bonus points");
        };
        for (TextField f : bonus) f.textProperty().addListener((o, a, v) -> calc.run());
        tx.textProperty().addListener((o, a, v) -> calc.run());
        mult.textProperty().addListener((o, a, v) -> calc.run());
        ContestState.addListener(() -> Platform.runLater(calc));
        calc.run();
        Label multRow = lbl("Power multiplier (enter the value from the current rules)", "ct-note");
        Label note = lbl("ARRL does not score Field Day from the log: you declare the power multiplier and bonuses on the summary sheet. "
                + "Enter the points each bonus is worth in this year's rules - only emergency power and the web-submission bonus are filled in.", "ct-note");
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
