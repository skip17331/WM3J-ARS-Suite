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

    public static void main(String[] args) {
        // helper: `--write-sample map|sat [file]` emits a starter launch JSON without booting the UI
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--write-sample")) {
                String mod = (i + 1 < args.length && args[i + 1].equalsIgnoreCase("sat")) ? "sat" : "map";
                String out = (i + 2 < args.length) ? args[i + 2] : ("j-" + mod + "-solo.json");
                try { java.nio.file.Files.writeString(java.nio.file.Path.of(out), com.ars.fx.data.SoloConfig.sample(mod));
                      System.out.println("wrote sample launch file: " + out); }
                catch (Exception e) { System.err.println("could not write sample: " + e.getMessage()); }
                return;
            }
        }
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Shell.onToggleTheme = this::toggleTheme;

        // Solo launch? `-Dars.config=<file>` or `--config <file>` runs one module (J-Map/J-Sat) on its own.
        com.ars.fx.data.SoloConfig cfg = resolveSoloConfig();
        if (cfg != null) { startSolo(stage, cfg); return; }

        // ── full dock app ──
        Shell.onNavigate = this::show;

        // spawn any Hamlib daemons the operator asked J-Hub to manage
        com.ars.fx.data.DaemonManager.ensureAll();
        // share live state with any solo J-Map / J-Sat on the LAN (Pi, 2nd PC)
        com.ars.fx.data.RemoteServer.startIfEnabled();

        scene = new Scene(host, 1460, 900);
        Theme.apply(scene, light);
        show(System.getProperty("surface", "hub"));

        stage.setTitle("ARS Suite");
        stage.setScene(scene);
        stage.show();
    }

    /** Resolve a solo launch descriptor from -Dars.config / --config, or null for the normal dock app. */
    private com.ars.fx.data.SoloConfig resolveSoloConfig() {
        String path = System.getProperty("ars.config");
        if (path == null) {
            java.util.List<String> raw = getParameters().getRaw();
            for (int i = 0; i < raw.size() - 1; i++) if (raw.get(i).equals("--config")) { path = raw.get(i + 1); break; }
        }
        if (path == null || path.isBlank()) return null;
        try { return com.ars.fx.data.SoloConfig.load(java.nio.file.Path.of(path)); }
        catch (IllegalArgumentException e) { System.err.println("[solo] " + e.getMessage()); javafx.application.Platform.exit(); return null; }
    }

    /** Boot a single module (J-Map or J-Sat) in its own window per the launch descriptor. */
    private void startSolo(Stage stage, com.ars.fx.data.SoloConfig cfg) {
        Shell.solo = !cfg.dock;
        Shell.onNavigate = this::show;                 // single surface; dock is gone so this rarely fires
        cfg.applyStation();                            // QTH/call overrides for the module's backends

        if (cfg.isRemote()) com.ars.fx.data.RemoteLink.connect(cfg.remote);   // live feed from the station's J-Hub

        scene = new Scene(host, cfg.window.width, cfg.window.height);
        Theme.apply(scene, light);
        show(cfg.module);

        stage.setTitle(cfg.windowTitle());
        stage.setScene(scene);
        stage.setMaximized(cfg.window.maximized);
        stage.show();
    }

    private void show(String id) {
        Shell.currentSurface = id;
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
            case "hub":      return com.ars.fx.surface.JHubDashboard.build();
            case "hubcfg":   return com.ars.fx.surface.JHubDashboard.buildConfig();
            case "station":  return com.ars.fx.surface.JHubDashboard.buildStation();
            case "hubrotor": return com.ars.fx.surface.JHubDashboard.buildRotor();
            case "hubamp":   return com.ars.fx.surface.JHubDashboard.buildAmp();
            case "hubant":   return com.ars.fx.surface.JHubDashboard.buildAntSw();
            case "hubdata":  return com.ars.fx.surface.JHubDashboard.buildData();
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
