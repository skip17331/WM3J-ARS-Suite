package com.hamradio.modem.ui;

import com.hamradio.modem.ModemService;
import com.hamradio.modem.model.HubMacro;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.Supplier;

/**
 * Horizontal bar of macro buttons.  Template variables expanded at click time:
 *   {MYCALL}  station callsign from prefs
 *   {CALL}    DX callsign from LogEntryPane callsign field
 *   {RST}     RST from LogEntryPane (default 599)
 *   {NAME}    OM (placeholder; not yet stored)
 *   {FREQ}    rig frequency in MHz
 *   {BAND}    band derived from rig frequency
 *   {MODE}    current modem mode
 */
public class MacroBar extends HBox {

    private final ModemService     service;
    private final Supplier<String> callsignFn;
    private final Supplier<String> rstFn;

    public MacroBar(ModemService service,
                    Supplier<String> callsignFn,
                    Supplier<String> rstFn) {
        this.service    = service;
        this.callsignFn = callsignFn;
        this.rstFn      = rstFn;

        setSpacing(5);
        setPadding(new Insets(5, 8, 5, 8));
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("jd-toolbar");

        service.setMacrosListener(list ->
            Platform.runLater(() -> rebuild(list)));

        rebuild(service.getMacros());
    }

    public void rebuild(List<HubMacro> macros) {
        getChildren().clear();

        if (macros.isEmpty()) {
            String[] labels = {"CQ", "Ans CQ", "QSO", "73 SK", "KN", "F1", "F2", "F3", "F4"};
            for (String label : labels) {
                Button btn = makeButton(label);
                btn.getStyleClass().add("macro-button-disabled");
                btn.setTooltip(new Tooltip("Waiting for hub\u2026"));
                btn.setDisable(true);
                getChildren().add(btn);
            }
        } else {
            for (HubMacro m : macros) {
                boolean programmable = "PROGRAMMABLE".equals(m.type);
                boolean empty        = m.text == null || m.text.isBlank();

                Button btn = makeButton(m.label != null ? m.label : m.key);
                if (empty) {
                    btn.getStyleClass().add("macro-button-disabled");
                    btn.setDisable(true);
                    btn.setTooltip(new Tooltip(
                        programmable ? "Programmable \u2014 edit via Hub web UI" : "(empty)"));
                } else if (programmable) {
                    btn.getStyleClass().add("macro-button-programmable");
                    btn.setTooltip(new Tooltip(m.text));
                    btn.setOnAction(e -> service.transmitMacro(expand(m.text)));
                } else {
                    btn.getStyleClass().add("macro-button");
                    btn.setTooltip(new Tooltip(m.text));
                    btn.setOnAction(e -> service.transmitMacro(expand(m.text)));
                }
                getChildren().add(btn);
            }
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);
    }

    // ---------------------------------------------------------------
    // Variable expansion
    // ---------------------------------------------------------------

    private String expand(String template) {
        long   hz   = service.getStatus().getRigFrequencyHz();
        String freq = hz > 0 ? "%.3f".formatted(hz / 1_000_000.0) : "--";
        String band = hz > 0 ? bandFromHz(hz) : "--";
        String mode = service.getStatus().getMode() != null
                      ? service.getStatus().getMode().name() : "--";
        String rst  = rstFn.get().trim();
        if (rst.isBlank()) rst = "599";

        return template
            .replace("{MYCALL}", service.getMyCall())
            .replace("{CALL}",   callsignFn.get().trim())
            .replace("{RST}",    rst)
            .replace("{NAME}",   "OM")
            .replace("{FREQ}",   freq)
            .replace("{BAND}",   band)
            .replace("{MODE}",   mode);
    }

    private String bandFromHz(long hz) {
        long k = hz / 1000;
        if (k >= 1800   && k <= 2000)   return "160m";
        if (k >= 3500   && k <= 4000)   return "80m";
        if (k >= 7000   && k <= 7300)   return "40m";
        if (k >= 10100  && k <= 10150)  return "30m";
        if (k >= 14000  && k <= 14350)  return "20m";
        if (k >= 18068  && k <= 18168)  return "17m";
        if (k >= 21000  && k <= 21450)  return "15m";
        if (k >= 24890  && k <= 24990)  return "12m";
        if (k >= 28000  && k <= 29700)  return "10m";
        if (k >= 50000  && k <= 54000)  return "6m";
        if (k >= 144000 && k <= 148000) return "2m";
        if (k >= 420000 && k <= 450000) return "70cm";
        return "--";
    }

    // ---------------------------------------------------------------
    // Factory
    // ---------------------------------------------------------------

    private Button makeButton(String label) {
        Button btn = new Button(label);
        btn.setMinWidth(72);
        btn.setPrefHeight(28);
        return btn;
    }
}
