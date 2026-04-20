package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.RotorSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * HamlibRotorController — TCP client for the Hamlib rotctld daemon.
 *
 * When rotor backend = "HAMLIB" in j-hub.json, this controller:
 *   • Connects to rotctld at tcpHost:tcpPort (default localhost:4533)
 *   • Polls current azimuth + elevation at 1-second intervals
 *   • Publishes ROTOR_STATUS via MessageRouter on every change ≥ 0.5°
 *   • Accepts trackPosition(az, el) calls from SAT_ROTOR_CMD messages
 *   • Applies a ±1.5° dead-band to avoid hunting
 *
 * rotctld command protocol (line-based, persistent TCP connection):
 *   p             → get position → "<az>\n<el>\nRPRT 0"
 *   P <az> <el>   → set position → "RPRT 0"
 *   S             → stop         → "RPRT 0"
 */
public class HamlibRotorController {

    private static final Logger log = LoggerFactory.getLogger(HamlibRotorController.class);
    private static final double DEAD_BAND_DEG = 1.5;
    private static final int    POLL_MS       = 1000;

    private static final HamlibRotorController INSTANCE = new HamlibRotorController();
    public static HamlibRotorController getInstance() { return INSTANCE; }
    private HamlibRotorController() {}

    private MessageRouter router;

    private volatile String  host      = "localhost";
    private volatile int     port      = 4533;
    private volatile boolean running   = false;
    private volatile boolean connected = false;
    private volatile double  lastAz    = -1;
    private volatile double  lastEl    = -1;
    private volatile double  targetAz  = -1;
    private volatile double  targetEl  = -1;

    private Socket         socket;
    private BufferedReader reader;
    private OutputStream   out;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "hamlib-rotor");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> pollFuture;

    // ── Lifecycle ────────────────────────────────────────────────────

    public void setRouter(MessageRouter r) { this.router = r; }

    public void start(RotorSection cfg) {
        this.host    = cfg.tcpHost != null ? cfg.tcpHost : "localhost";
        this.port    = cfg.tcpPort > 0 ? cfg.tcpPort : 4533;
        this.running = true;
        log.info("HamlibRotorController starting — {}:{}", host, port);
        schedulePoll();
    }

    public void stop() {
        running = false;
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closeSocket);
        connected = false;
        log.info("HamlibRotorController stopped");
    }

    public void restart(RotorSection cfg) {
        if (cfg == null) { running = false; return; }
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closeSocket);
        connected = false;
        if ("HAMLIB".equals(cfg.backend)) {
            start(cfg);
        } else {
            running = false;
        }
    }

    // ── Status ───────────────────────────────────────────────

    public boolean isRunning()   { return running; }
    public boolean isConnected() { return connected; }
    public double  getLastAz()   { return lastAz < 0 ? 0 : lastAz; }
    public double  getLastEl()   { return lastEl < 0 ? 0 : lastEl; }

    // ── Commands ─────────────────────────────────────────────

    /**
     * Slew to the given azimuth and elevation.
     * Called by MessageRouter on SAT_ROTOR_CMD. Dead-band applied.
     */
    public void trackPosition(double az, double el) {
        if (!running) return;
        targetAz = az;
        targetEl = el;
        scheduler.execute(() -> {
            try {
                if (lastAz >= 0 &&
                    Math.abs(az - lastAz) < DEAD_BAND_DEG &&
                    Math.abs(el - lastEl) < DEAD_BAND_DEG) return;
                double clampedEl = Math.max(0, Math.min(90, el));
                sendCommand(String.format("P %.1f %.1f", az, clampedEl));
                log.debug("Rotor → AZ={} EL={}", az, clampedEl);
            } catch (IOException e) {
                log.warn("Rotor position command failed: {}", e.getMessage());
                closeSocket();
            }
        });
    }

    public void stopRotor() {
        if (!running) return;
        targetAz = -1; targetEl = -1;
        scheduler.execute(() -> {
            try { sendCommand("S"); }
            catch (IOException e) { closeSocket(); }
        });
    }

    // ── Poll loop ────────────────────────────────────────────

    private void schedulePoll() {
        if (pollFuture != null) pollFuture.cancel(false);
        pollFuture = scheduler.scheduleAtFixedRate(this::poll, 0, POLL_MS, TimeUnit.MILLISECONDS);
    }

    private void poll() {
        if (!running) return;
        try {
            String resp = sendCommand("p");
            double[] pos = parsePosition(resp);
            if (!connected) {
                connected = true;
                log.info("rotctld connected — {}:{}", host, port);
            }
            boolean changed = lastAz < 0
                    || Math.abs(pos[0] - lastAz) > 0.5
                    || Math.abs(pos[1] - lastEl) > 0.5;
            lastAz = pos[0];
            lastEl = pos[1];
            if (changed) publishRotorStatus();
        } catch (IOException e) {
            if (connected) {
                log.warn("rotctld connection lost: {}", e.getMessage());
                connected = false;
            }
            closeSocket();
        }
    }

    private void publishRotorStatus() {
        if (router == null) return;
        com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
        msg.addProperty("type",      "ROTOR_STATUS");
        msg.addProperty("bearing",   lastAz);
        msg.addProperty("elevation", lastEl);
        if (targetAz >= 0) {
            msg.addProperty("targetAz", targetAz);
            msg.addProperty("targetEl", targetEl);
        }
        msg.addProperty("source", "HAMLIB");
        router.publishRotorStatus(msg.toString());
    }

    // ── rotctld socket I/O ───────────────────────────────────

    private void ensureConnected() throws IOException {
        if (socket != null && !socket.isClosed() && socket.isConnected()) return;
        socket = new Socket(host, port);
        socket.setSoTimeout(3000);
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        out = socket.getOutputStream();
        log.debug("rotctld socket opened — {}:{}", host, port);
    }

    private String sendCommand(String cmd) throws IOException {
        ensureConnected();
        out.write((cmd + "\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("RPRT ")) {
                int code;
                try { code = Integer.parseInt(line.substring(5).trim()); }
                catch (NumberFormatException e) { code = -1; }
                if (code != 0) throw new IOException("rotctld RPRT " + code + " for: " + cmd);
                break;
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        return sb.toString().trim();
    }

    private void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null; reader = null; out = null;
    }

    // ── Parser ────────────────────────────────────────────────

    private static double[] parsePosition(String response) {
        String[] lines = response.split("\n");
        double az = 0, el = 0;
        if (lines.length >= 1) { try { az = Double.parseDouble(lines[0].trim()); } catch (Exception ignored) {} }
        if (lines.length >= 2) { try { el = Double.parseDouble(lines[1].trim()); } catch (Exception ignored) {} }
        return new double[]{az, el};
    }
}
