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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    /** Auto-spawned hubs (via {@link #ensureRunning()}) shut down when the last client leaves; a hub launched
     *  directly ({@code ars-fx --hub}) is "sticky" and only stops on Quit. Set from {@code -Dars.hub.auto}. */
    private static volatile boolean auto;
    private static volatile boolean hadClient = false;
    private static final ScheduledExecutorService LINGER =
            Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "hub-linger"); t.setDaemon(true); return t; });
    private static volatile ScheduledFuture<?> pendingShutdown;

    public static void main(String[] args) {
        if (args.length > 0 && "--stop".equals(args[0])) {            // ars-fx.sh --stop-hub
            System.out.println(stopRunning() ? "[hub] told the hub to quit" : "[hub] no hub is running on :" + RemoteServer.port());
            System.exit(0);
        }
        if (args.length > 0 && "--kill".equals(args[0])) {            // ars-fx.sh --kill-hub: ask nicely, then force
            System.out.println(killRunning());
            System.exit(0);
        }
        int port = RemoteServer.port();
        auto = Boolean.getBoolean("ars.hub.auto");

        // Refuse to start a second hub on the same machine — the daemons/port are single-owner. The lock file closes the
        // race where two clients spawn a hub at the same moment (both would pass the port check, since the port is
        // only bound after the daemons and feeds are up); the port check still catches a hub from an older build.
        if (!acquireSingleInstanceLock()) {
            System.out.println("[hub] another hub process is starting or running — nothing to do");
            return;
        }
        if (portOpen(port)) {
            System.out.println("[hub] a sharing hub is already listening on :" + port + " — nothing to do");
            return;
        }
        writePidFile();

        // Own the shared backends: daemons first (so the rig/rotor clients find them), then the clients,
        // then publish state. This process is the sole owner; every window (loose or docked) is a client.
        com.ars.fx.data.Bands.applyRegion();
        AppIcons.closeSplash();                          // a headless hub shows no window, so never leave a splash up
        try { DaemonManager.ensureAll(); } catch (Exception e) { System.err.println("[hub] daemons: " + e.getMessage()); }
        ClusterClient.getInstance().applyConfig();   // server + login from J-Hub ▸ Data (was: hard-coded defaults only)
        ClusterClient.getInstance().start();
        HeardByClient.getInstance().start();
        RigClient.getInstance().start();
        RotorClient.getInstance().start();
        RotorClient.satellite().start();                 // idles until the satellite rotor is switched on
        RemoteServer.onClientCount = HubServer::onClientCount;   // wire refcount before the server accepts clients
        RemoteServer.onReconfig    = HubServer::reconcile;
        RemoteServer.onQuit        = () -> { System.out.println("[hub] quit requested by a window"); ALIVE.countDown(); };
        RemoteServer.startHub();
        com.ars.fx.web.WebClientServer.init();             // the phone page, if the operator turned it on
        if (!auto) scheduleStartupIdleCheck();   // a hub started on its own must not hold the cluster for nobody

        Runtime.getRuntime().addShutdownHook(new Thread(HubServer::cleanup, "hub-shutdown"));

        if (!installTray(port)) {
            System.out.println("[hub] no system tray available — running headless on :" + port
                    + " (stop with Ctrl-C)" + (auto ? " — auto: exits when the last client closes" : ""));
        }

        try { ALIVE.await(); } catch (InterruptedException ignored) {}
        armWatchdog(10);                                 // cleanup or exit can hang (a daemon that will not stop): do not let it hold the hub up
        cleanup();
        System.exit(0);
    }

    // ── kill switch ───────────────────────────────────────────────────────────

    private static File pidFile() { return new File(new File(System.getProperty("user.home"), ".j-hub"), "hub.pid"); }

    private static void writePidFile() {
        try {
            File f = pidFile();
            f.getParentFile().mkdirs();
            java.nio.file.Files.writeString(f.toPath(), Long.toString(ProcessHandle.current().pid()));
            f.deleteOnExit();
        } catch (Exception ignored) { /* the kill switch then falls back to the lock-free port check */ }
    }

    /** If the hub has not exited {@code seconds} after a quit was asked for, stop the JVM outright (halt skips the shutdown hooks that may be what is stuck). */
    private static void armWatchdog(int seconds) {
        Thread t = new Thread(() -> {
            try { Thread.sleep(seconds * 1000L); } catch (InterruptedException ignored) { return; }
            System.err.println("[hub] did not unload within " + seconds + " s — halting");
            try { pidFile().delete(); } catch (Throwable ignored) { }
            ProcessHandle.current().descendants().forEach(ProcessHandle::destroyForcibly);   // halt skips the hooks that stop rigctld: do it here so none is left holding the serial port
            Runtime.getRuntime().halt(1);
        }, "hub-watchdog");
        t.setDaemon(true);
        t.start();
    }

    /** The tray's "Kill Hub (force)": one short try at an orderly cleanup (so the transmitter is un-keyed and the daemons stop), then halt. */
    private static void forceQuit() {
        armWatchdog(3);
        ALIVE.countDown();
    }

    /** {@code ars-fx.sh --kill-hub}: ask the hub to quit and wait for it to go; if it has not, kill its process (the pid in ~/.j-hub/hub.pid,
     *  checked to still be a HubServer so a stale file can never take down something else). Returns a line to print. */
    static String killRunning() {
        int port = RemoteServer.port();
        long pid = -1;
        try { pid = Long.parseLong(java.nio.file.Files.readString(pidFile().toPath()).trim()); } catch (Exception ignored) { }
        if (portOpen(port)) stopRunning();
        for (int i = 0; i < 40 && alive(pid, port); i++) try { Thread.sleep(250); } catch (InterruptedException e) { break; }
        if (!alive(pid, port)) return "[hub] stopped";
        java.util.Optional<ProcessHandle> h = pid > 0 ? ProcessHandle.of(pid) : java.util.Optional.empty();
        if (h.isPresent() && isHub(h.get())) {
            java.util.List<ProcessHandle> kids = h.get().descendants().toList();      // the hub's rigctld / rotctld: killing the hub alone leaves them holding the serial port
            h.get().destroy();                                              // SIGTERM: runs the shutdown hook
            for (int i = 0; i < 12 && h.get().isAlive(); i++) try { Thread.sleep(250); } catch (InterruptedException e) { break; }
            if (h.get().isAlive()) h.get().destroyForcibly();               // SIGKILL
            for (int i = 0; i < 8 && h.get().isAlive(); i++) try { Thread.sleep(250); } catch (InterruptedException e) { break; }
            for (ProcessHandle k : kids) if (k.isAlive()) k.destroyForcibly();
            try { pidFile().delete(); } catch (Throwable ignored) { }
            return h.get().isAlive() ? "[hub] could not be killed (pid " + pid + ")" : "[hub] killed (pid " + pid + (kids.isEmpty() ? ")" : ") and its " + kids.size() + " helper process(es)");
        }
        return portOpen(port) ? "[hub] still listening on :" + port + " but no hub pid is recorded — stop it by hand (the process running com.ars.fx.HubServer)"
                              : "[hub] stopped";
    }

    private static boolean alive(long pid, int port) {
        if (portOpen(port)) return true;
        return pid > 0 && ProcessHandle.of(pid).map(h -> h.isAlive() && isHub(h)).orElse(false);
    }

    private static boolean isHub(ProcessHandle h) {
        return h.info().commandLine().map(c -> c.contains("com.ars.fx.HubServer")).orElse(false);
    }

    /** Asks the hub running on this machine to quit (the hub only obeys a connection from the loopback address). True if one was reached. */
    public static boolean stopRunning() {
        int port = RemoteServer.port();
        if (!portOpen(port)) return false;
        try {
            org.java_websocket.client.WebSocketClient c = new org.java_websocket.client.WebSocketClient(new java.net.URI("ws://127.0.0.1:" + port)) {
                @Override public void onOpen(org.java_websocket.handshake.ServerHandshake h) { send("{\"t\":\"hub.quit\"}"); }
                @Override public void onMessage(String m) {}
                @Override public void onClose(int code, String reason, boolean remote) {}
                @Override public void onError(Exception e) {}
            };
            if (!c.connectBlocking(2, java.util.concurrent.TimeUnit.SECONDS)) return false;
            Thread.sleep(400);                                      // let the message out before the socket goes
            c.close();
            return true;
        } catch (Exception e) { return false; }
    }

    /** Ensure a hub is listening on the sharing port, spawning one (detached) if not. Used by every client
     *  window at launch. The spawn is tagged {@code -Dars.hub.auto=true} so it self-terminates once the last
     *  client disconnects (vs. a hub launched directly, which stays up until Quit). */
    public static void ensureRunning() {
        int port = RemoteServer.port();
        if (portOpen(port)) return;
        try {
            List<String> cmd = javaCmd("com.ars.fx.HubServer");
            cmd.add(cmd.size() - 1, "-Dars.hub.auto=true");   // JVM arg, before the main class
            new ProcessBuilder(cmd)
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

    // ── client refcount → linger shutdown ─────────────────────────────────────

    /** Driven by {@link RemoteServer} on every connect/disconnect. When the last client leaves (windows and LAN peers
     *  both count) a short linger timer starts, and a returning client cancels it — so swapping modules doesn't
     *  churn the connections. If it runs out, an auto hub exits; a sticky hub (it only stops on Quit) stays up
     *  but hangs up the DX-cluster / RBN telnet connections and leaves them down: the operator reconnects by hand. */
    private static synchronized void onClientCount(int n) {
        if (n > 0) { hadClient = true; cancelLinger(); }
        else if (hadClient) scheduleLinger();
    }
    private static synchronized void scheduleLinger() {
        cancelLinger();
        int grace = lingerSeconds();
        System.out.println("[hub] last client left — " + (auto ? "stopping" : "hanging up the cluster feeds")
                + " in " + grace + "s unless one returns");
        if (clusterWanted()) notice("Last window closed",
                "Disconnecting from the DX cluster in " + grace + " s. Open ARS Suite again to keep it connected.");
        pendingShutdown = LINGER.schedule(() -> {
            if (RemoteServer.clientCount() != 0) return;
            if (auto) { System.out.println("[hub] no clients after grace — stopping"); ALIVE.countDown(); }
            else dropFeeds();
        }, grace, TimeUnit.SECONDS);
    }

    /** A sticky hub that nobody ever attaches to (started with {@code --hub}, window never opened) drops the feeds too. */
    private static void scheduleStartupIdleCheck() {
        LINGER.schedule(() -> { if (!hadClient && RemoteServer.clientCount() == 0) dropFeeds(); }, 30, TimeUnit.SECONDS);
    }

    // The telnet feeds the hub owns.
    private static final com.ars.fx.data.TelnetFeed[] FEEDS = {
            ClusterClient.getInstance(), HeardByClient.getInstance() };

    /** Nothing is attached: hang up the DX-cluster / RBN connections. They stay down until the operator presses
     *  Connect (a disconnect pauses the feed, so it does not retry on its own). */
    private static void dropFeeds() {
        boolean was = clusterWanted();
        for (com.ars.fx.data.TelnetFeed f : FEEDS) f.disconnect();
        System.out.println("[hub] nothing attached — cluster feeds disconnected");
        if (was) notice("DX cluster disconnected", "Nothing is using the hub. Press Connect in J-Hub to reconnect.");
    }

    private static boolean clusterWanted() {
        for (com.ars.fx.data.TelnetFeed f : FEEDS) if (f.wantConnected()) return true;
        return false;
    }

    private static volatile TrayIcon trayIcon;

    /** A tray balloon from the hub (the only thing still on screen once the last window has closed). No-op without a tray. */
    private static void notice(String title, String message) {
        TrayIcon t = trayIcon;
        if (t != null) try { t.displayMessage(title, message, TrayIcon.MessageType.INFO); } catch (Exception ignored) {}
    }

    // Held for the life of the process; the OS releases it if the hub dies, so a crash never blocks the next hub.
    private static java.nio.channels.FileChannel lockChannel;
    private static java.nio.channels.FileLock lockHandle;

    /** True if this process now owns the single-hub lock. Fails open (true) if the lock file can't be used at all. */
    private static boolean acquireSingleInstanceLock() {
        try {
            File dir = new File(System.getProperty("user.home"), ".j-hub");
            dir.mkdirs();
            lockChannel = java.nio.channels.FileChannel.open(new File(dir, "hub.lock").toPath(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
            lockHandle = lockChannel.tryLock();            // null when another process holds it
            return lockHandle != null;
        } catch (java.nio.channels.OverlappingFileLockException e) {
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static void cancelLinger() {
        ScheduledFuture<?> f = pendingShutdown;
        pendingShutdown = null;
        if (f != null) f.cancel(false);
    }
    private static int lingerSeconds() {
        try { return Math.max(0, Integer.parseInt(HubConfig.get("hub.lingerSeconds", "8").trim())); }
        catch (Exception e) { return 8; }
    }

    /** A client edited config in its own JVM and nudged us: re-read config from disk, then reconcile the
     *  backends we own — managed daemons, the rig/rotor/amp client connections, and the LAN-share bind. */
    private static void reconcile() {
        HubConfig.reload();
        try { DaemonManager.applyAll(); } catch (Exception e) { System.err.println("[hub] daemons: " + e.getMessage()); }
        RigClient.getInstance().reconnect();
        RotorClient.getInstance().reconnect();
        RotorClient.satellite().reconnect();
        com.ars.fx.data.AmpClient.getInstance().reconnect();
        ClusterClient.getInstance().applyConfig();
        HeardByClient.getInstance().setCall(HubConfig.call());
        RemoteServer.applyIfBindChanged();
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
            MenuItem kill = new MenuItem("Kill Hub (force)");       // for a hub that will not unload
            kill.addActionListener(e -> forceQuit());
            menu.add(kill);

            java.awt.Image mark = AppIcons.tray(tray.getTrayIconSize().width);
            TrayIcon icon = new TrayIcon(mark != null ? mark : trayImage(tray), "ARS Suite Hub — sharing on :" + port, menu);
            icon.setImageAutoSize(true);
            tray.add(icon);
            trayIcon = icon;
            System.out.println("[hub] tray icon installed — sharing on :" + port);
            return true;
        } catch (Exception e) {
            System.err.println("[hub] tray unavailable: " + e.getMessage());
            return false;
        }
    }

    /** Fallback only (the app mark could not be read): a small rounded badge with a "J". */
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
        try { com.ars.fx.web.WebClientServer.stop(); } catch (Throwable ignored) { }
        try { com.ars.fx.data.TxController.shutdown(); } catch (Exception ignored) {}   // never leave the transmitter keyed
        for (com.ars.fx.data.TelnetFeed f : FEEDS) try { f.disconnect(); } catch (Exception ignored) {}   // hang up on the cluster before exiting
        try { RemoteServer.shutdown(); } catch (Exception ignored) {}
        try { DaemonManager.stopAll(); } catch (Exception ignored) {}
    }

    static { HubConfig.call(); }   // touch config early so a bad ~/.j-hub fails loudly at startup
}
