package com.ars.fx;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev launcher / surface previewer for the ARS Suite JavaFX rewrite.
 * A picker bar across the top switches the surface shown below; a Theme
 * button toggles light/dark live. Run with: mvn -q javafx:run
 */
public class Launcher extends Application {

    /** {surface id, tab label}. */
    private static final String[][] SURFACES = {
        {"hub","J-Hub"}, {"hubcfg","Config"}, {"log","J-Log"}, {"map","J-Map"},
        {"sat","J-Sat"}, {"digi","J-Digi"}, {"bridge","J-Bridge"}, {"vault","J-Vault"}, {"learn","J-Learn"},
    };

    private final StackPane host = new StackPane();   // holds the active surface
    private final List<Button> tabs = new ArrayList<>();
    private Scene scene;
    private boolean light = false;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        HBox picker = buildPicker();
        VBox root = new VBox(picker, host);
        VBox.setVgrow(host, Priority.ALWAYS);
        root.setStyle("-fx-background-color:-ars-bg;");

        scene = new Scene(root, 1460, 900);
        Theme.apply(scene, light);

        String start = System.getProperty("surface", "hub");
        show(start);

        stage.setTitle("ARS Suite — JavaFX preview");
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildPicker() {
        HBox bar = new HBox(8); bar.getStyleClass().add("picker"); bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label brand = new Label("ARS Suite ·"); brand.getStyleClass().add("picker-lbl");
        bar.getChildren().add(brand);
        for (String[] s : SURFACES) {
            Button b = new Button(s[1]); b.getStyleClass().add("picker-btn");
            b.setOnAction(e -> show(s[0]));
            tabs.add(b); bar.getChildren().add(b);
        }
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button theme = new Button("◑ Theme"); theme.getStyleClass().add("picker-btn");
        theme.setOnAction(e -> toggleTheme());
        bar.getChildren().addAll(sp, theme);
        return bar;
    }

    private void show(String id) {
        Node surface = buildSurface(id);
        host.getChildren().setAll(surface);
        // highlight the active tab
        int idx = 0;
        for (int i = 0; i < SURFACES.length; i++) if (SURFACES[i][0].equals(id)) idx = i;
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).getStyleClass().remove("on");
            if (i == idx) tabs.get(i).getStyleClass().add("on");
        }
    }

    private void toggleTheme() {
        light = !light;
        if (light) scene.getRoot().getStyleClass().add("ars-light");
        else scene.getRoot().getStyleClass().remove("ars-light");
    }

    /** Build a surface frame by id. */
    public static Region buildSurface(String id) {
        switch (id) {
            case "log":    return com.ars.fx.surface.JLogCockpit.build();
            case "hub":    return com.ars.fx.surface.JHubDashboard.build();
            case "hubcfg": return com.ars.fx.surface.JHubDashboard.buildConfig();
            case "map":    return com.ars.fx.surface.JMapView.build();
            case "sat":    return com.ars.fx.surface.JSatView.build();
            case "digi":   return com.ars.fx.surface.JDigiView.build();
            case "bridge": return com.ars.fx.surface.JBridgeView.build();
            case "vault":  return com.ars.fx.surface.JVaultView.build();
            case "learn":  return com.ars.fx.surface.JLearnView.build();
            default:       return com.ars.fx.surface.JHubDashboard.build();
        }
    }
}
