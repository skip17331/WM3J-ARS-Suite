package com.hamradio.modem.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * RigControl backed by Hamlib's {@code rigctld} TCP daemon (default
 * localhost:4532). Works with any rig Hamlib supports — Yaesu,
 * Kenwood, Elecraft, Icom (as an alternative to {@link CivRigControl}),
 * etc.
 *
 * <p>Protocol used is the plain (non-extended) rigctld command set:
 * <pre>
 *   T 1\n   → PTT on   → reply "RPRT 0\n" on success
 *   T 0\n   → PTT off  → reply "RPRT 0\n" on success
 *   RPRT -N → error code N (mapped to RuntimeException)
 * </pre>
 *
 * <p>A fresh TCP connection is opened for each PTT toggle. This adds
 * ~1 ms of latency but guarantees that a previously-crashed rigctld
 * (or a re-started one with a different rig configured) doesn't cause
 * subsequent commands to silently fail.
 */
public final class HamlibRigControl implements RigControl {

    private static final Logger log = LoggerFactory.getLogger(HamlibRigControl.class);

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int    DEFAULT_PORT = 4532;
    private static final int   CONNECT_TIMEOUT_MS = 2000;
    private static final int   READ_TIMEOUT_MS    = 2000;

    private final String host;
    private final int    port;

    public HamlibRigControl() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public HamlibRigControl(String host, int port) {
        this.host = host == null || host.isBlank() ? DEFAULT_HOST : host.trim();
        this.port = port > 0 ? port : DEFAULT_PORT;
    }

    public String host() { return host; }
    public int    port() { return port; }

    @Override
    public void setPtt(boolean on) throws Exception {
        String command = on ? "T 1\n" : "T 0\n";
        String reply = sendCommand(command);
        parseRprt(reply, command);
        log.debug("rigctld T {} ok", on ? 1 : 0);
    }

    /** Send a raw rigctld command (must end with {@code \n}) and return
     *  the first line of the reply. Visible for tests. */
    String sendCommand(String command) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            w.write(command);
            w.flush();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String reply = in.readLine();
            return reply == null ? "" : reply;
        }
    }

    /** Translate a rigctld {@code RPRT N} reply into success-or-throw.
     *  Visible for tests. */
    static void parseRprt(String reply, String command) throws IOException {
        if (reply == null || !reply.startsWith("RPRT")) {
            throw new IOException("rigctld returned malformed reply '" + reply
                + "' for command '" + command.trim() + "'");
        }
        String numberPart = reply.substring(4).trim();
        int code;
        try { code = Integer.parseInt(numberPart); }
        catch (NumberFormatException e) {
            throw new IOException("rigctld returned non-numeric RPRT '" + reply + "'");
        }
        if (code != 0) {
            throw new IOException("rigctld error " + code
                + " for command '" + command.trim() + "'");
        }
    }
}
