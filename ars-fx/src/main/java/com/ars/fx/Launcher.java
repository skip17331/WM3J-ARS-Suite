package com.ars.fx;

import com.ars.fx.shell.Shell;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * Dev launcher / surface previewer for the ARS Suite JavaFX rewrite.
 * Run with -Dsurface=map|log|sat|hub (default map). Surfaces are added as
 * they are built; until then a surface shows the shared shell skeleton.
 */
public class Launcher extends Application {

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        String surface = System.getProperty("surface", "map");
        Region root = buildSurface(surface);
        Scene scene = new Scene(root, 1440, 820);
        Theme.apply(scene, Boolean.getBoolean("light"));
        stage.setTitle("ARS Suite — " + surface);
        stage.setScene(scene);
        stage.show();
    }

    /** Assemble a surface frame. Center content is filled in per-surface as built. */
    public static Region buildSurface(String id) {
        switch (id) {
            case "log": return com.ars.fx.surface.JLogCockpit.build();
            case "hub": return com.ars.fx.surface.JHubDashboard.build();
            case "hubcfg": return com.ars.fx.surface.JHubDashboard.buildConfig();
            case "map": return com.ars.fx.surface.JMapView.build();
            case "sat": return com.ars.fx.surface.JSatView.build();
            case "digi": return com.ars.fx.surface.JDigiView.build();
            case "bridge": return com.ars.fx.surface.JBridgeView.build();
            default: break;
        }
        Region dock = Shell.dock(id);
        HBox top;
        String[][] stats;
        Region center = new Region();
        center.setStyle("-fx-background-color:-ars-bg;");
        HBox.setHgrow(center, Priority.ALWAYS);
        Region rail = Shell.rail(Shell.instruments(50).toArray(new Node[0]));
        switch (id) {
            case "map" -> {
                stats = new String[][]{{"SPOTS / HR","342"},{"SHOWN","22"},{"GRAY LINE","SR 11:02 · SS 22:48"},{"BEAM","050°","accent"}};
                top = Shell.topBar("map","map","J-Map","Propagation & spots · FN20", stats, "17:00:14");
            }
            case "sat" -> {
                stats = new String[][]{{"TRACKING","AO-91","accent"},{"AZ / EL","096° / 31°"},{"NEXT","NOW"},{"ROTOR","auto-track"}};
                top = Shell.topBar("sat","sat","J-Sat","Satellite tracking · AO-91", stats, "17:00:42");
            }
            case "log" -> {
                stats = new String[][]{{"QSOS","1,284"},{"MULTS","13"},{"SCORE","16,692","amber"}};
                top = Shell.topBar("log","log","J-Log","CQ WW DX · Zones", stats, "17:00:03");
            }
            default -> {
                stats = new String[][]{{"STATUS","—"}};
                top = Shell.topBar("hub","hub","J-Hub","WM3J", stats, "14:42:07");
            }
        }
        return Shell.frame(dock, top, center, rail);
    }
}
