package com.ars.fx.shell;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Delete removes one character, not the whole box. Moving between fields selects all of the text (so typing replaces it), and Delete would
 * then wipe it. When everything is selected, Delete drops the selection and the caret goes to the start, so it deletes the first character.
 * Typing over a selected value still replaces it, and Backspace is left alone.
 */
public final class DeleteKey {
    private DeleteKey() {}

    public static void install(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() != KeyCode.DELETE || e.isShortcutDown() || e.isShiftDown() || e.isAltDown()) return;
            if (!(scene.getFocusOwner() instanceof TextInputControl t) || !t.isEditable()) return;
            if (selectsAll(t.getLength(), t.getSelection().getLength())) { t.deselect(); t.positionCaret(0); }
        });
    }

    /** True when the whole text, of more than one character, is selected. */
    static boolean selectsAll(int length, int selected) { return length > 1 && selected == length; }
}
