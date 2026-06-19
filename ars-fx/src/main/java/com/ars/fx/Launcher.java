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
        Shell.onThemeChanged = () -> Themes.applyTo(host, Shell.currentSurface);

        // Solo launch? `-Dars.config=<file>` or `--config <file>` runs one module (J-Map/J-Sat) on its own.
        com.ars.fx.data.SoloConfig cfg = resolveSoloConfig();
        if (cfg != null) { startSolo(stage, cfg); return; }

        // ── full dock app ──
        Shell.onNavigate = this::show;

        // The docked app is a CLIENT of the local background hub — the single owner of the Hamlib daemons
        // and the live feeds. Open the link first so the data clients see it and stay passive (their start()
        // calls no-op under an active RemoteLink); bring the hub up off-thread so a cold spawn doesn't stall
        // the UI — RemoteLink reconnects to it as soon as it is listening.
        com.ars.fx.data.RemoteLink.connect("ws://127.0.0.1:" + com.ars.fx.data.RemoteServer.port(), "dock");
        new Thread(com.ars.fx.HubServer::ensureRunning, "hub-ensure").start();

        scene = new Scene(host, 1460, 900);
        Theme.apply(scene, false);   // base css; scheme applied per-surface by show()
        show(System.getProperty("surface", "hub"));

        stage.setTitle("ARS Suite");
        stage.setScene(scene);
        stage.show();
    }

    /** Resolve a solo launch descriptor, or null for the normal dock app. Two forms:
     *  {@code --module <id>} / {@code -Dars.module=<id>} → a loose window attached to the local background hub;
     *  {@code --config <file>} / {@code -Dars.config=<file>} → a full JSON descriptor (local or LAN-remote). */
    private com.ars.fx.data.SoloConfig resolveSoloConfig() {
        String module = argValue("ars.module", "--module");
        if (module != null && !module.isBlank()) {
            try { return com.ars.fx.data.SoloConfig.local(module); }
            catch (IllegalArgumentException e) { System.err.println("[solo] " + e.getMessage()); javafx.application.Platform.exit(); return null; }
        }
        String path = argValue("ars.config", "--config");
        if (path == null || path.isBlank()) return null;
        try { return com.ars.fx.data.SoloConfig.load(java.nio.file.Path.of(path)); }
        catch (IllegalArgumentException e) { System.err.println("[solo] " + e.getMessage()); javafx.application.Platform.exit(); return null; }
    }

    /** A system property, else the value following {@code flag} in the raw args. */
    private String argValue(String sysProp, String flag) {
        String v = System.getProperty(sysProp);
        if (v != null) return v;
        java.util.List<String> raw = getParameters().getRaw();
        for (int i = 0; i < raw.size() - 1; i++) if (raw.get(i).equals(flag)) return raw.get(i + 1);
        return null;
    }

    /** Boot a single module (J-Map or J-Sat) in its own window per the launch descriptor. */
    private void startSolo(Stage stage, com.ars.fx.data.SoloConfig cfg) {
        Shell.solo = !cfg.dock;
        Shell.soloModule = cfg.module;                 // so J-Hub settings pages can offer "← Back to <module>"
        Shell.onNavigate = this::show;                 // single surface; dock is gone so this rarely fires
        cfg.applyStation();                            // QTH/call overrides for the module's backends

        // Attach as a client: open the link first (so the data clients stay passive), then ensure the local
        // hub is up off-thread. A LAN config points at the station's J-Hub; a local --module points at ours.
        if (cfg.isRemote()) com.ars.fx.data.RemoteLink.connect(cfg.remote, cfg.module);
        if (cfg.autoHub)    new Thread(com.ars.fx.HubServer::ensureRunning, "hub-ensure").start();

        scene = new Scene(host, cfg.window.width, cfg.window.height);
        Theme.apply(scene, false);   // base css; scheme applied per-surface by show()
        show(cfg.module);

        stage.setTitle(cfg.windowTitle());
        stage.setScene(scene);
        stage.setMaximized(cfg.window.maximized);
        stage.show();
    }

    private void show(String id) {
        Shell.currentSurface = id;
        if (id.equals("log") || id.equals("logc")) Shell.lastLogSurface = id;   // remember J-Log's mode
        host.getChildren().setAll(buildSurface(id));
        Themes.applyTo(host, id);          // effective scheme for this surface (module override or global)
    }

    /** Clean shutdown when the window closes: drop the hub link (so the hub's client refcount falls, and an
     *  auto hub can shut itself down once we were the last one), then force the JVM down. Several deps (the
     *  Java-WebSocket client, the embedded modem/WSJT-X engines, logback) run non-daemon threads that would
     *  otherwise keep the process alive after the last window closes. Daemons/server belong to the hub, not us. */
    @Override
    public void stop() {
        try { com.ars.fx.data.RemoteLink.disconnect(); } catch (Exception ignored) {}
        javafx.application.Platform.exit();
        System.exit(0);
    }

    /** Build a surface frame by id. */
    public static Region buildSurface(String id) {
        switch (id) {
            case "log":    return com.ars.fx.surface.JLogCockpit.build();
            case "logc":   return com.ars.fx.surface.JLogContest.build();
            case "hub":      return com.ars.fx.surface.JHubDashboard.build();
            case "hubcfg":   return com.ars.fx.surface.JHubDashboard.buildConfig();
            case "station":  return com.ars.fx.surface.JHubDashboard.buildStation();
            case "hubrotor": return com.ars.fx.surface.JHubDashboard.buildRotor();
            case "hubamp":   return com.ars.fx.surface.JHubDashboard.buildAmp();
            case "hubant":   return com.ars.fx.surface.JHubDashboard.buildAntSw();
            case "hubdata":  return com.ars.fx.surface.JHubDashboard.buildData();
            case "hubmods":  return com.ars.fx.surface.JHubDashboard.buildModules();
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
