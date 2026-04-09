package com.hamclock.ui.panels;

import com.hamclock.app.ServiceRegistry;
import com.hamclock.service.propagation.PropagationData;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Propagation panel showing FOT, MUF, LUF in the right sidebar.
 * All font sizes use em — cascades from root font size setting.
 */
public class PropagationPanel extends VBox {

    private final ServiceRegistry services;

    private final Label fotLabel;
    private final Label mufLabel;
    private final Label lufLabel;
    private final Label sfiLabel;

    public PropagationPanel(ServiceRegistry services) {
        this.services = services;

        setSpacing(4);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #0d1020; -fx-background-radius: 4; " +
                 "-fx-border-color: #1a2a5a; -fx-border-radius: 4; -fx-border-width: 1;");

        Label title = new Label("📡  PROPAGATION");
        title.setStyle("-fx-font-size: 0.77em; -fx-font-weight: bold; -fx-text-fill: #aabbdd; -fx-padding: 0 0 4 0;");
        getChildren().add(title);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(3);

        fotLabel = valueLabel("-- MHz");
        mufLabel = valueLabel("-- MHz");
        lufLabel = valueLabel("-- MHz");
        sfiLabel = valueLabel("---");

        grid.add(keyLabel("FOT"), 0, 0); grid.add(fotLabel, 1, 0);
        grid.add(keyLabel("MUF"), 0, 1); grid.add(mufLabel, 1, 1);
        grid.add(keyLabel("LUF"), 0, 2); grid.add(lufLabel, 1, 2);
        grid.add(keyLabel("SFI"), 0, 3); grid.add(sfiLabel, 1, 3);

        getChildren().add(grid);
        update();
    }

    public void update() {
        PropagationData data = services.propagationDataProvider.getCached();

        if (data == null) {
            Thread t = new Thread(() -> {
                try { services.propagationDataProvider.fetch(); }
                catch (Exception ignored) {}
                javafx.application.Platform.runLater(this::update);
            });
            t.setDaemon(true);
            t.start();
            return;
        }

        fotLabel.setText(String.format("%.1f MHz", data.getFot()));
        lufLabel.setText(String.format("%.1f MHz", data.getLuf()));
        sfiLabel.setText(String.format("%.0f", data.getSfi()));

        // MUF with color
        String mufColor = data.getMuf() >= 21 ? "#00ff88" :
                          data.getMuf() >= 14 ? "#88cc00" :
                          data.getMuf() >= 7  ? "#cccc00" : "#cc6600";
        mufLabel.setText(String.format("%.1f MHz", data.getMuf()));
        mufLabel.setStyle("-fx-font-size: 1.08em; -fx-font-weight: bold; -fx-text-fill: " + mufColor + ";");
    }

    private Label keyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 0.77em; -fx-text-fill: #556688;");
        return l;
    }

    private Label valueLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 1em; -fx-font-weight: bold; -fx-text-fill: #ccddff;");
        return l;
    }
}
