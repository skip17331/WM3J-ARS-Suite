package com.jlog.controller;

import com.jlog.util.DxccLanguageMap;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * User-selectable translator. Adds checkbox controls for each
 * supported language so the operator can show or hide columns on
 * demand. English is always visible.
 */
public class UserTranslationWindow extends TranslationWindow {

    public UserTranslationWindow() {
        super("Phrase Translator — User Selected");

        // English is implicit; let the operator toggle the other three.
        Set<String> initialSelected = new LinkedHashSet<>(DxccLanguageMap.ALL);
        setVisibleLanguages(initialSelected);

        languageBar.getChildren().add(new Label("Show:"));
        addToggle("Spanish",    DxccLanguageMap.ES);
        addToggle("German",     DxccLanguageMap.DE);
        addToggle("Portuguese", DxccLanguageMap.PT);
    }

    private void addToggle(String label, String code) {
        CheckBox cb = new CheckBox(label);
        cb.setSelected(visibleLangs.contains(code));
        cb.setOnAction(e -> {
            Set<String> updated = new LinkedHashSet<>(visibleLangs);
            if (cb.isSelected()) updated.add(code);
            else                 updated.remove(code);
            setVisibleLanguages(updated);
        });
        languageBar.getChildren().add(cb);
    }
}
