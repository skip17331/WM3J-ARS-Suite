package com.wm3j.jmap.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Slim bottom bar reminding operators where to find the setup page.
 */
public class SetupHintBar extends HBox {

    public SetupHintBar(int port) {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(2, 16, 2, 16));
        setStyle("-fx-background-color: #050508; -fx-border-color: #0d1a33; -fx-border-width: 1 0 0 0;");

        Label hint = new Label(
            "⚙  Setup & Configuration:  http://localhost:8081  •  F / F11 = fullscreen");
        hint.setStyle("-fx-font-size: 0.77em; -fx-text-fill: #334455;");

        getChildren().add(hint);
    }
}
