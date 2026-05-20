package com.hamradio.modem.ui;

import com.hamradio.modem.ModemService;
import com.hamradio.modem.model.HubMacro;
import com.jlog.util.MacroVariableEngine;
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
 *   {NAME}    DX operator name from LogEntryPane Name field (defaults to "OM" when blank)
 *   {FREQ}    rig frequency in MHz
 *   {BAND}    band derived from rig frequency
 *   {MODE}    current modem mode
 */
public class MacroBar extends HBox {

    private final ModemService     service;
    private final Supplier<String> callsignFn;
    private final Supplier<String> rstFn;
    private final Supplier<String> nameFn;

    public MacroBar(ModemService service,
                    Supplier<String> callsignFn,
                    Supplier<String> rstFn,
                    Supplier<String> nameFn) {
        this.service    = service;
        this.callsignFn = callsignFn;
        this.rstFn      = rstFn;
        this.nameFn     = nameFn;

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
        MacroVariableEngine.Context ctx = new MacroVariableEngine.Context();
        ctx.myCall      = service.getMyCall();
        ctx.dxCall      = callsignFn.get();
        ctx.rstSent     = rstFn.get();
        // ctx.name is the DX op's name; MacroVariableEngine substitutes "OM"
        // when the supplier returns blank, so users still get a sensible macro.
        ctx.name        = nameFn.get();
        ctx.frequencyHz = service.getStatus().getRigFrequencyHz();
        ctx.mode        = service.getStatus().getMode() != null
                          ? service.getStatus().getMode().name() : "";
        return MacroVariableEngine.substitute(template, ctx);
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
