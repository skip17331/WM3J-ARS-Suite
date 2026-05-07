package com.morsetrainer.ui;

import com.morsetrainer.core.AppConfig;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.nio.file.Paths;

/**
 * Settings UI. Edits the global AppConfig in place and saves to JSON when
 * the user clicks Save. Most numeric fields use spinners; text fields back
 * the hardware port settings.
 */
public class SettingsView {

    private final Stage owner;
    public SettingsView(Stage owner) { this.owner = owner; }

    public void show() {
        AppConfig cfg = AppConfig.get();

        Spinner<Integer> wpm = new Spinner<>(5, 50, cfg.wpm);
        Spinner<Integer> farns = new Spinner<>(5, 50, cfg.farnsworthWpm);
        Spinner<Integer> tone = new Spinner<>(300, 1200, cfg.toneFrequencyHz);
        Slider volume = new Slider(0, 1, cfg.toneVolume); volume.setShowTickLabels(true);
        TextField arduinoPort = new TextField(cfg.arduinoSerialPort);
        Spinner<Integer> baud = new Spinner<>(9600, 230_400, cfg.arduinoBaudRate);
        Spinner<Integer> udpPort = new Spinner<>(1024, 65_535, cfg.pizeroUdpPort);
        TextField bleChar = new TextField(cfg.pizeroBleCharacteristic);
        TextField kochOrder = new TextField(cfg.kochOrder);
        kochOrder.setPrefColumnCount(40);
        Spinner<Integer> sessionLen = new Spinner<>(10, 500, cfg.letterSessionCount);
        CheckBox autoExport = new CheckBox("Auto-export session JSON");
        autoExport.setSelected(cfg.autoExportSessions);
        CheckBox adaptive = new CheckBox("Adaptive decoder (auto-track WPM)");
        adaptive.setSelected(cfg.adaptiveDecoder);

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(8); g.setPadding(new Insets(20));

        int r = 0;
        g.add(new Label("Character WPM"), 0, r);    g.add(wpm, 1, r++);
        g.add(new Label("Farnsworth WPM"), 0, r);   g.add(farns, 1, r++);
        g.add(new Label("Tone (Hz)"), 0, r);        g.add(tone, 1, r++);
        g.add(new Label("Volume"), 0, r);           g.add(volume, 1, r++);
        g.add(new Label("Arduino port"), 0, r);     g.add(arduinoPort, 1, r++);
        g.add(new Label("Arduino baud"), 0, r);     g.add(baud, 1, r++);
        g.add(new Label("Pi Zero UDP port"), 0, r); g.add(udpPort, 1, r++);
        g.add(new Label("Pi Zero BLE char"), 0, r); g.add(bleChar, 1, r++);
        g.add(new Label("Koch order"), 0, r);       g.add(kochOrder, 1, r++);
        g.add(new Label("Session length"), 0, r);   g.add(sessionLen, 1, r++);
        g.add(autoExport, 1, r++);
        g.add(adaptive, 1, r++);

        Button save = new Button("Save");
        save.setOnAction(e -> {
            cfg.wpm = wpm.getValue();
            cfg.farnsworthWpm = farns.getValue();
            cfg.toneFrequencyHz = tone.getValue();
            cfg.toneVolume = volume.getValue();
            cfg.arduinoSerialPort = arduinoPort.getText().trim();
            cfg.arduinoBaudRate = baud.getValue();
            cfg.pizeroUdpPort = udpPort.getValue();
            cfg.pizeroBleCharacteristic = bleChar.getText().trim();
            cfg.kochOrder = kochOrder.getText().trim();
            cfg.letterSessionCount = sessionLen.getValue();
            cfg.autoExportSessions = autoExport.isSelected();
            cfg.adaptiveDecoder = adaptive.isSelected();
            cfg.save(Paths.get("config", "app-config.json"));
        });
        Button back = new Button("Back");
        back.setOnAction(e -> HomeView.show(owner));

        Label title = new Label("Settings");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        VBox root = new VBox(12, title, g, new HBox(8, save, back));
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 720, 640);
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        owner.setScene(scene);
        owner.setTitle("Settings");
    }
}
