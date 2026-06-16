package com.ars.fx.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * TCP client for the Hamlib ampctld amplifier daemon (default 127.0.0.1:4531).
 * Polls power state + SWR + forward power; emits a {@link State} to a listener.
 * Endpoint resolves from HubConfig (amp.host / amp.ampctldPort), -Damp.* overrides.
 * Same shape as {@link RotorClient}; degrades to OFFLINE when ampctld is absent.
 */
public final class AmpClient {
    private static final AmpClient INSTANCE = new AmpClient();
    public static AmpClient getInstance() { return INSTANCE; }
    private AmpClient() {}

    private static final long POLL_MS = 900;

    public record State(boolean connected, int powerStat, double swr, double pwr) {
        public static final State OFFLINE = new State(false, -1, 0, 0);
        public String mode() { return powerStat == 2 ? "operate" : powerStat == 1 ? "standby" : powerStat == 0 ? "off" : "—"; }
    }

    private volatile String host = resolveHost();
    private volatile int    port = resolvePort();
    private static String resolveHost() { return System.getProperty("amp.host", HubConfig.get("amp.host", "127.0.0.1")); }
    private static int resolvePort() {
        String p = System.getProperty("amp.port");
        try { return p != null ? Integer.parseInt(p.trim()) : Integer.parseInt(HubConfig.get("amp.ampctldPort", "4531").trim()); }
        catch (Exception e) { return 4531; }
    }
    /** Re-read the endpoint from config and drop the socket so the poll loop reconnects. */
    public void reconnect() { host = resolveHost(); port = resolvePort(); closeSocket(); }

    private volatile Consumer<State> listener = s -> {};
    private volatile State last = State.OFFLINE;
    private volatile boolean started = false;
    private Socket socket; private BufferedReader reader; private OutputStream out;

    public State last() { return last; }
    public String endpoint() { return host + ":" + port; }
    public void setListener(Consumer<State> l) { this.listener = l == null ? s -> {} : l; emit(last); }

    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(this::loop, "amp-poll");
        t.setDaemon(true);
        t.start();
    }

    private void loop() {
        while (true) {
            try {
                int ps = (int) Math.round(parseNum(send("\\get_powerstat")));
                double swr = tryLevel("SWR");
                double pwr = tryLevel("PWR");
                emit(new State(true, ps, swr, pwr));
            } catch (Exception e) {
                closeSocket();
                emit(State.OFFLINE);
            }
            try { Thread.sleep(POLL_MS); } catch (InterruptedException ie) { return; }
        }
    }
    private double tryLevel(String lvl) { try { return parseNum(send("\\get_level " + lvl)); } catch (Exception e) { return 0; } }

    private void emit(State s) {
        last = s;
        Consumer<State> l = listener;
        try { javafx.application.Platform.runLater(() -> l.accept(s)); }
        catch (IllegalStateException noFx) { /* toolkit not up */ }
    }

    private void ensureConnected() throws IOException {
        if (socket != null && !socket.isClosed() && socket.isConnected()) return;
        Socket s = new Socket();
        try { s.connect(new InetSocketAddress(host, port), 3000); }
        catch (java.net.SocketTimeoutException te) { try { s.close(); } catch (IOException ig) {} throw new java.net.ConnectException("timed out " + host + ":" + port); }
        s.setSoTimeout(3000);
        socket = s;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        out = socket.getOutputStream();
    }
    private synchronized String send(String cmd) throws IOException {
        ensureConnected();
        out.write((cmd + "\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("RPRT ")) {
                int code; try { code = Integer.parseInt(line.substring(5).trim()); } catch (NumberFormatException e) { code = -1; }
                if (code != 0) throw new IOException("RPRT " + code + " for: " + cmd);
                break;
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        return sb.toString().trim();
    }
    private synchronized void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null; reader = null; out = null;
    }
    private static double parseNum(String response) {
        for (String raw : response.split("\n")) {
            String s = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1).trim() : raw.trim();
            if (s.isEmpty()) continue;
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }
}
