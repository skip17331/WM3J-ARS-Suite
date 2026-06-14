package com.ars.fx;

import com.ars.fx.shell.Shell;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * ARS Suite preview host. Navigation happens through the shell itself — the
 * left module dock (and the J-Hub nav) switch surfaces via Shell.navigate; the
 * top-bar Theme control flips light/dark. Run: mvn -q javafx:run
 * Start surface: -Dsurface=hub|hubcfg|log|map|sat|digi|bridge|vault|learn.
 */
public class Launcher extends Application {

    private final StackPane host = new StackPane();
    private Scene scene;
    private boolean light = false;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        Shell.onNavigate = this::show;
        Shell.onToggleTheme = this::toggleTheme;

        scene = new Scene(host, 1460, 900);
        Theme.apply(scene, light);
        show(System.getProperty("surface", "hub"));

        stage.setTitle("ARS Suite");
        stage.setScene(scene);
        stage.show();
    }

    private void show(String id) {
        host.getChildren().setAll(buildSurface(id));
    }

    private void toggleTheme() {
        light = !light;
        if (light) host.getStyleClass().add("ars-light");
        else host.getStyleClass().remove("ars-light");
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
