package com.jlog.controller;

import com.jlog.util.DxccLanguageMap;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

import java.util.Set;

/**
 * DXCC-driven translator. Selects which language columns are visible
 * based on the active callsign's DXCC entity via {@link DxccLanguageMap}.
 *
 * <p>Two ways to drive the entity:
 * <ul>
 *   <li>Call {@link #applyEntity(String)} from elsewhere in J-Log (the
 *       Normal- and Contest-log controllers do this when the operator
 *       focuses a row, so the translator keeps up with whoever is
 *       currently being worked).</li>
 *   <li>Type a DXCC name directly into the top-bar field — useful when
 *       the operator wants to look up phrases for an upcoming sked
 *       without having to populate the QSO form first.</li>
 * </ul>
 */
public class DxccTranslationWindow extends TranslationWindow {

    private final TextField entityField;
    private final Label     entityVerdict;

    public DxccTranslationWindow() {
        super("Phrase Translator — DXCC Driven");

        languageBar.getChildren().add(new Label("DXCC entity:"));

        entityField = new TextField();
        entityField.setPromptText("Spain / Germany / Brazil / United States …");
        entityField.setPrefColumnCount(28);
        entityField.setTooltip(new Tooltip(
            "Type a DXCC entity name to select target columns. " +
            "Falls back to English-only when no mapping matches."));
        entityField.setOnAction(e -> applyEntity(entityField.getText()));
        languageBar.getChildren().add(entityField);

        entityVerdict = new Label("");
        entityVerdict.setStyle("-fx-text-fill: var(--subtext1, #88aacc);");
        languageBar.getChildren().add(entityVerdict);

        // Default: English-only until the operator drives an entity in.
        applyEntity(null);
    }

    /**
     * Drive the visible column set from a DXCC entity name. Safe to call
     * from any thread — bounces to the FX Application Thread internally.
     */
    public void applyEntity(String entity) {
        Runnable apply = () -> {
            entityField.setText(entity == null ? "" : entity);
            Set<String> langs = DxccLanguageMap.languagesFor(entity);
            setVisibleLanguages(langs);
            if (entity == null || entity.isBlank()) {
                entityVerdict.setText("(no entity — English only)");
            } else {
                String list = String.join(", ", langs);
                entityVerdict.setText("→ " + list);
            }
        };
        if (Platform.isFxApplicationThread()) apply.run();
        else Platform.runLater(apply);
    }
}
