package com.hamclock.service.rotor;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Fetches rotor position from an IP-connected Arduino via WebSocket.
 *
 * Performs a minimal WebSocket handshake then reads the first text frame.
 * For production use, a full WebSocket library (Tyrus, Java-WebSocket) is recommended.
 *
 * Arduino endpoint: ws://<host>:<port>/rotor
 *
 * Expected frame payload: JSON or plain azimuth value.
 *
 * This implementation is a simplified "bare-bones" WebSocket client
 * sufficient for the Arduino use case (single frame, no fragmentation).
 */
public class ArduinoRotorWebSocketProvider extends AbstractDataProvider<RotorData>
        implements RotorProvider {

    private final String host;
    private final int port;
    private static final int TIMEOUT_MS = 5_000;

    public ArduinoRotorWebSocketProvider(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected RotorData doFetch() throws DataProviderException {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            InputStream  in  = socket.getInputStream();

            // WebSocket handshake
            String key = generateWebSocketKey();
            sendHandshake(out, key);
            readHandshakeResponse(in);

            // Read one WebSocket frame
            String payload = readTextFrame(in);
            return parsePayload(payload);

        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException(
                "WebSocket rotor error at " + host + ":" + port + " — " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private String generateWebSocketKey() {
        byte[] random = new byte[16];
        new java.util.Random().nextBytes(random);
        return Base64.getEncoder().encodeToString(random);
    }

    private void sendHandshake(OutputStream out, String key) throws IOException {
        String handshake =
            "GET /rotor HTTP/1.1\r\n" +
            "Host: " + host + ":" + port + "\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: " + key + "\r\n" +
            "Sec-WebSocket-Version: 13\r\n\r\n";
        out.write(handshake.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void readHandshakeResponse(InputStream in) throws IOException, DataProviderException {
        // Read HTTP response headers until blank line
        StringBuilder sb = new StringBuilder();
        int prev = -1;
        int b;
        while ((b = in.read()) != -1) {
            sb.append((char) b);
            if (prev == '\r' && b == '\n' && sb.toString().endsWith("\r\n\r\n")) break;
            prev = b;
        }
        String response = sb.toString();
        if (!response.contains("101")) {
            throw new DataProviderException(
                "WebSocket handshake failed, response: " + response.substring(0, Math.min(80, response.length())),
                DataProviderException.ErrorCode.NETWORK_ERROR);
        }
    }

    private String readTextFrame(InputStream in) throws IOException, DataProviderException {
        // WebSocket frame: byte0 (FIN+opcode), byte1 (MASK+length), payload
        int byte0 = in.read();
        int byte1 = in.read();
        if (byte0 == -1 || byte1 == -1) {
            throw new DataProviderException("WebSocket connection closed prematurely",
                DataProviderException.ErrorCode.NETWORK_ERROR);
        }

        int opcode = byte0 & 0x0F;
        if (opcode != 0x01) { // 0x01 = text frame
            throw new DataProviderException("Expected text frame (opcode 1), got: " + opcode,
                DataProviderException.ErrorCode.PARSE_ERROR);
        }

        boolean masked = (byte1 & 0x80) != 0;
        long payloadLen = byte1 & 0x7F;

        if (payloadLen == 126) {
            payloadLen = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (payloadLen == 127) {
            // 8-byte length (not expected from Arduino)
            payloadLen = 0;
            for (int i = 0; i < 8; i++) payloadLen = (payloadLen << 8) | (in.read() & 0xFF);
        }

        byte[] maskKey = new byte[4];
        if (masked) in.read(maskKey);

        byte[] payload = new byte[(int) payloadLen];
        int read = 0;
        while (read < payload.length) {
            int r = in.read(payload, read, payload.length - read);
            if (r == -1) break;
            read += r;
        }

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskKey[i % 4];
            }
        }

        return new String(payload, StandardCharsets.UTF_8);
    }

    private RotorData parsePayload(String payload) throws DataProviderException {
        payload = payload.trim();
        try {
            if (payload.startsWith("{")) {
                double az = 0, el = 0;
                boolean moving = false;
                // Minimal JSON extraction
                if (payload.contains("\"azimuth\"")) {
                    az = extractDouble(payload, "azimuth");
                }
                if (payload.contains("\"elevation\"")) {
                    el = extractDouble(payload, "elevation");
                }
                if (payload.contains("\"moving\"")) {
                    moving = payload.contains("\"moving\":true");
                }
                RotorData data = new RotorData(az, el);
                data.setInMotion(moving);
                data.setConnected(true);
                return data;
            } else {
                double az = Double.parseDouble(payload);
                RotorData data = new RotorData(az);
                data.setConnected(true);
                return data;
            }
        } catch (Exception e) {
            throw new DataProviderException(
                "Failed to parse WebSocket rotor payload: " + payload,
                DataProviderException.ErrorCode.PARSE_ERROR, e);
        }
    }

    private double extractDouble(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return 0.0;
        int colon = json.indexOf(':', idx);
        int end = json.indexOf(',', colon);
        if (end < 0) end = json.indexOf('}', colon);
        return Double.parseDouble(json.substring(colon + 1, end).trim());
    }
}
