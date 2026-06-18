package com.ars.fx;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.DaemonManager;
import com.ars.fx.data.HeardByClient;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.RemoteServer;
import com.ars.fx.data.RigClient;
import com.ars.fx.data.RotorClient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Headless background "hub" for loose (un-docked) operation. It owns the things a
 * single dock JVM normally owns — the managed Hamlib daemons and the live data
 * clients (DX-cluster spots, RBN "heard by", rig, rotor) — and exposes them over
 * the {@link RemoteServer} WebSocket so each loose module window (launched as its
 * own JVM with {@code --module <id>}) attaches via {@code ws://127.0.0.1:8090}.
 *
 * <p>This process shows a <em>system-tray icon</em> so it is visible and quittable,
 * but it never opens a window of its own: the tray menu just launches the requested
 * app. On desktops with no usable tray (e.g. GNOME/Wayland, which drops the legacy
 * tray) it degrades to a plain headless process — quit with Ctrl-C / kill.
 *
 * <p>Run directly: {@code java -cp ars-fx.jar com.ars.fx.HubServer}. Normally you do
 * not: a loose launcher ({@code ars-fx --module log}) calls {@link #ensureRunning()},
 * which spawns this if the sharing port is not already up.
 */
public final class HubServer {

    private HubServer() {}

    /** Loose-capable modules shown in the tray "open" menu: {tray label, module id}. */
    private static final String[][] MENU = {
        {"J-Log",   "log"},
        {"J-Map",   "map"},
        {"J-Sat",   "sat"},
        {"J-Digi",  "digi"},
        {"J-Vault", "vault"},
        {"J-Learn", "learn"},
    };

    private static final CountDownLatch ALIVE = new CountDownLatch(1);

    public static void main(String[] args) {
        int port = RemoteServer.port();

        // Refuse to start a second hub on the same machine — the daemons/port are single-owner.
        if (portOpen(port)) {
            System.out.println("[hub] a sharing hub is already listening on :" + port + " — nothing to do");
            return;
        }

        // Own the shared backends: daemons first (so the rig/rotor clients find them), then the clients,
        // then publish state. Mirrors what the dock app does, minus any UI.
        try { DaemonManager.ensureAll(); } catch (Exception e) { System.err.println("[hub] daemons: " + e.getMessage()); }
        ClusterClient.getInstance().start();
        HeardByClient.getInstance().start();
        RigClient.getInstance().start();
        RotorClient.getInstance().start();
        RemoteServer.startIfEnabled();

        Runtime.getRuntime().addShutdownHook(new Thread(HubServer::cleanup, "hub-shutdown"));

        if (!installTray(port)) {
            System.out.println("[hub] no system tray available — running headless on :" + port
                    + " (stop with Ctrl-C)");
        }

        try { ALIVE.await(); } catch (InterruptedException ignored) {}
        cleanup();
        System.exit(0);
    }

    /** Ensure a hub is listening on the sharing port, spawning one (detached) if not. Used by loose launchers. */
    public static void ensureRunning() {
        int port = RemoteServer.port();
        if (portOpen(port)) return;
        try {
            new ProcessBuilder(javaCmd("com.ars.fx.HubServer"))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (Exception e) {
            System.err.println("[hub] could not start background hub: " + e.getMessage());
            return;
        }
        for (int i = 0; i < 30 && !portOpen(port); i++) {   // wait up to ~3 s for the port to come up
            try { Thread.sleep(100); } catch (InterruptedException e) { return; }
        }
    }

    // ── tray ────────────────────────────────────────────────────────────────

    private static boolean installTray(int port) {
        if (Boolean.getBoolean("ars.noTray") || !SystemTray.isSupported()) return false;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            PopupMenu menu = new PopupMenu();

            MenuItem header = new MenuItem("ARS Suite Hub · sharing :" + port);
            header.setEnabled(false);
            menu.add(header);
            menu.addSeparator();
            for (String[] m : MENU) {
                MenuItem item = new MenuItem("Open " + m[0]);
                final String id = m[1];
                item.addActionListener(e -> openModule(id));
                menu.add(item);
            }
            menu.addSeparator();
            MenuItem quit = new MenuItem("Quit Hub");
            quit.addActionListener(e -> ALIVE.countDown());
            menu.add(quit);

            TrayIcon icon = new TrayIcon(trayImage(tray), "ARS Suite Hub — sharing on :" + port, menu);
            icon.setImageAutoSize(true);
            tray.add(icon);
            System.out.println("[hub] tray icon installed — sharing on :" + port);
            return true;
        } catch (Exception e) {
            System.err.println("[hub] tray unavailable: " + e.getMessage());
            return false;
        }
    }

    /** A small rounded badge with a "J" — no bundled asset needed. */
    private static BufferedImage trayImage(SystemTray tray) {
        Dimension d = tray.getTrayIconSize();
        int w = Math.max(16, d.width), h = Math.max(16, d.height);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x10, 0x18, 0x22));
        g.fillRoundRect(0, 0, w - 1, h - 1, w / 3, h / 3);
        g.setColor(new Color(0x35, 0xC4, 0x9A));
        g.setFont(new Font("SansSerif", Font.BOLD, h - 4));
        String s = "J";
        int sw = g.getFontMetrics().stringWidth(s);
        int sh = g.getFontMetrics().getAscent();
        g.drawString(s, (w - sw) / 2, (h + sh) / 2 - 2);
        g.dispose();
        return img;
    }

    /** Launch one module as its own JVM, attached to this hub. */
    private static void openModule(String id) {
        try {
            new ProcessBuilder(javaCmd("com.ars.fx.Main", "--module", id))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (Exception e) {
            System.err.println("[hub] could not open " + id + ": " + e.getMessage());
        }
    }

    // ── process / port helpers ────────────────────────────────────────────────

    /** Build a {@code java ... <mainClass> <appArgs>} command re-using this JVM's java binary + classpath. */
    private static List<String> javaCmd(String mainClass, String... appArgs) {
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        cmd.add("-cp");
        cmd.add(selfClasspath());
        cmd.add(mainClass);
        for (String a : appArgs) cmd.add(a);
        return cmd;
    }

    /** The shipped fat jar (single entry on the classpath), or the full dev classpath when run from classes. */
    private static String selfClasspath() {
        try {
            File f = new File(HubServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (f.isFile()) return f.getPath();              // packaged: the ars-fx fat jar
        } catch (Exception ignored) { /* fall through */ }
        return System.getProperty("java.class.path");        // dev: classes + all deps
    }

    private static boolean portOpen(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 200);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static synchronized void cleanup() {
        try { RemoteServer.shutdown(); } catch (Exception ignored) {}
        try { DaemonManager.stopAll(); } catch (Exception ignored) {}
    }

    static { HubConfig.call(); }   // touch config early so a bad ~/.j-hub fails loudly at startup
}
