package com.hamclock.ui.panels;

import com.hamclock.app.ServiceRegistry;
import com.hamclock.service.propagation.PropagationData;
import com.hamclock.service.propagation.PropagationData.BandCondition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Band conditions panel showing 80m–6m conditions as colored pills.
 * All font sizes use em — cascades from root font size setting.
 */
public class BandConditionsPanel extends VBox {

    private final ServiceRegistry services;
    private final Map<String, Label> bandLabels = new LinkedHashMap<>();
    private final Map<String, HBox>  bandRows   = new LinkedHashMap<>();

    private static final String[] BANDS = {
        "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m"
    };

    public BandConditionsPanel(ServiceRegistry services) {
        this.services = services;

        setSpacing(3);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #0d1020; -fx-background-radius: 4; " +
                 "-fx-border-color: #1a2a5a; -fx-border-radius: 4; -fx-border-width: 1;");

        Label title = new Label("📻  BAND CONDITIONS");
        title.setStyle("-fx-font-size: 0.77em; -fx-font-weight: bold; -fx-text-fill: #aabbdd; -fx-padding: 0 0 4 0;");
        getChildren().add(title);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(2);

        for (int i = 0; i < BANDS.length; i++) {
            String band = BANDS[i];
            HBox row = buildBandRow(band, BandCondition.FAIR);
            bandRows.put(band, row);
            grid.add(row, i % 2, i / 2);
        }

        getChildren().add(grid);
        update();
    }

    private HBox buildBandRow(String band, BandCondition condition) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);

        Rectangle dot = new Rectangle(8, 8);
        dot.setArcWidth(4);
        dot.setArcHeight(4);
        dot.setFill(Color.web(condition.colorHex));
        dot.setId("dot_" + band);

        Label bandLabel = new Label(band);
        bandLabel.setStyle("-fx-font-size: 0.77em; -fx-text-fill: #778899; -fx-min-width: 2.5em;");

        Label condLabel = new Label(condition.label);
        condLabel.setStyle("-fx-font-size: 0.69em; -fx-font-weight: bold; -fx-text-fill: " + condition.colorHex + ";");
        bandLabels.put(band, condLabel);

        row.getChildren().addAll(dot, bandLabel, condLabel);
        return row;
    }

    public void update() {
        PropagationData data = services.propagationDataProvider.getCached();
        if (data == null) return;

        Map<String, BandCondition> conditions = data.getBandConditions();
        for (String band : BANDS) {
            BandCondition bc = conditions.getOrDefault(band, BandCondition.FAIR);

            HBox row = bandRows.get(band);
            if (row == null) continue;

            Rectangle dot = (Rectangle) row.getChildren().stream()
                .filter(n -> n instanceof Rectangle)
                .findFirst().orElse(null);
            if (dot != null) dot.setFill(Color.web(bc.colorHex));

            Label condLabel = bandLabels.get(band);
            if (condLabel != null) {
                condLabel.setText(bc.label);
                condLabel.setStyle("-fx-font-size: 0.69em; -fx-font-weight: bold; -fx-text-fill: " + bc.colorHex + ";");
            }
        }
    }
}
