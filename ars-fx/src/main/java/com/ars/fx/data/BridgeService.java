package com.ars.fx.data;

import com.hamradio.jbridge.WsjtxUdpListener;
import com.hamradio.jbridge.model.WsjtxDecode;
import com.hamradio.jbridge.model.WsjtxStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Process-wide holder for the real J-Bridge WSJT-X UDP listener
 * (com.hamradio:j-bridge). Binds the listener's decode/status/connection
 * callbacks once, keeps a rolling buffer of recent decodes + the last status,
 * and fans out a single refresh signal to the surface. Receive-only: it listens
 * for WSJT-X broadcasts on UDP 2237 (override with -Dwsjtx.port).
 */
public final class BridgeService {
    private BridgeService() {}

    private static WsjtxUdpListener listener;
    private static boolean started;
    private static final Deque<WsjtxDecode> decodes = new ArrayDeque<>();
    private static volatile WsjtxStatus status;
    private static volatile boolean connected;
    private static volatile String sourceApp = "", version = "";
    private static volatile Runnable sink = () -> {};

    public static synchronized void start() {
        if (started) return;
        started = true;
        try {
            listener = new WsjtxUdpListener(port(), "0.0.0.0");
            listener.setOnStatus(s -> { status = s; fire(); });
            listener.setOnDecode(d -> {
                synchronized (decodes) { decodes.addFirst(d); while (decodes.size() > 60) decodes.removeLast(); }
                fire();
            });
            listener.setOnConnectionChange((c, app, ver) -> { connected = c; sourceApp = app == null ? "" : app; version = ver == null ? "" : ver; fire(); });
            listener.setOnClear(() -> { synchronized (decodes) { decodes.clear(); } fire(); });
            listener.start();
        } catch (Throwable t) {
            listener = null;
        }
    }

    /** UI refresh callback — wrap your Platform.runLater(refresh) here. Replaces any previous sink. */
    public static void setSink(Runnable r) { sink = r != null ? r : () -> {}; }

    public static boolean running()    { return listener != null && listener.isRunning(); }
    public static boolean connected()  { return connected; }
    public static String sourceApp()   { return sourceApp; }
    public static String version()     { return version; }
    public static WsjtxStatus status() { return status; }
    public static int port()           { return Integer.getInteger("wsjtx.port", 2237); }
    public static List<WsjtxDecode> decodes() { synchronized (decodes) { return new ArrayList<>(decodes); } }

    private static void fire() { Runnable r = sink; if (r != null) r.run(); }
}
