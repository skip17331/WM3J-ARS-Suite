package com.ars.fx.data;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Listens on UDP 13064 for N1MM+'s {@code <radio_setfrequency>} message, the one a waterfall or band map sends to QSY the radio when you
 * click a signal. Off until the operator turns it on (J-Hub &#9656; Data), and it listens on this computer's loopback address only, so
 * nothing on the network can reach it. A request is acted on only if it is for radio 1, the rig is connected and not transmitting, and
 * the frequency is inside an amateur band; anything else is ignored.
 */
public final class N1mmListener {
    private N1mmListener() {}

    public static final String KEY_LISTEN = "n1mm.listen", KEY_PORT = "n1mm.listenPort", KEY_LAN = "n1mm.listenLan", KEY_FROM = "n1mm.listenFrom";
    public static final int DEFAULT_PORT = 13064;

    private static DatagramSocket socket;
    private static int boundPort;
    private static boolean boundLan;

    /** Start or stop the listener to match the settings. Safe to call whenever a setting changes. */
    public static synchronized void apply() {
        boolean want = HubConfig.getBool(KEY_LISTEN, false);
        int port = port();
        boolean lan = HubConfig.getBool(KEY_LAN, false);
        if (want && socket != null && boundPort == port && boundLan == lan) return;
        stop();
        if (want) start(port, lan, N1mmListener::qsy);
    }

    static int port() {
        try { int p = Integer.parseInt(HubConfig.get(KEY_PORT, String.valueOf(DEFAULT_PORT)).trim()); if (p >= 1024 && p <= 65535) return p; }
        catch (NumberFormatException ignored) { /* the default */ }
        return DEFAULT_PORT;
    }

    /** Opens the socket and starts reading. Returns false if the port could not be taken. */
    static synchronized boolean start(int port, Consumer<N1mm.SetFrequency> onRequest) { return start(port, false, onRequest); }

    /** As above; {@code lan} listens on every address of this computer instead of the loopback only. */
    static synchronized boolean start(int port, boolean lan, Consumer<N1mm.SetFrequency> onRequest) {
        stop();
        try {
            DatagramSocket s = new DatagramSocket(null);
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(lan ? new InetSocketAddress(port).getAddress() : InetAddress.getLoopbackAddress(), port));
            socket = s; boundPort = s.getLocalPort(); boundLan = lan;
            Thread t = new Thread(() -> read(s, lan, onRequest), "n1mm-listen");
            t.setDaemon(true); t.start();
            return true;
        } catch (Exception e) {
            socket = null;
            return false;
        }
    }

    static synchronized void stop() {
        if (socket != null) { socket.close(); socket = null; }
    }

    /** The port the listener is on, or -1 when it is off. */
    static synchronized int boundPort() { return socket == null ? -1 : boundPort; }

    /**
     * Whether a packet from {@code from} may be acted on. The loopback always may. From the network only when the listener was switched on
     * for it, and then only from an address on a private network (never the internet) and, if the operator named senders, only from those.
     */
    static boolean allowed(InetAddress from, boolean lan, String senders) {
        if (from == null) return false;
        if (from.isLoopbackAddress()) return true;
        if (!lan) return false;
        if (!(from.isSiteLocalAddress() || from.isLinkLocalAddress())) return false;
        String list = senders == null ? "" : senders.trim();
        if (list.isEmpty()) return true;
        String ip = from.getHostAddress();
        for (String tok : list.split("[\\s,;]+")) if (tok.equals(ip)) return true;
        return false;
    }

    private static void read(DatagramSocket s, boolean lan, Consumer<N1mm.SetFrequency> onRequest) {
        byte[] buf = new byte[4096];
        while (!s.isClosed()) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                s.receive(p);
                if (!allowed(p.getAddress(), lan, HubConfig.get(KEY_FROM, ""))) continue;
                N1mm.SetFrequency f = N1mm.parseSetFrequency(new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8));
                if (f != null) onRequest.accept(f);
            } catch (Exception e) {
                if (s.isClosed()) return;                 // stopped
            }
        }
    }

    /** What to do with a request: tune the rig, if it is safe to. Returns whether it did. */
    static boolean qsy(N1mm.SetFrequency f) {
        try {
            if (f == null || f.radioNr() != 1) return false;                         // there is one radio here
            RigClient.State rig = RigClient.getInstance().last();
            if (rig == null || !rig.connected() || rig.ptt()) return false;          // never while transmitting
            if (Bands.of(f.freqHz()).isEmpty()) return false;                        // only inside an amateur band
            RigClient.getInstance().setFreqHz(f.freqHz());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
