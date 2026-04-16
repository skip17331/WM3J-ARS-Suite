package com.wm3j.jmap.service.rotor;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Fetches rotor position from an IP-connected Arduino via UDP.
 *
 * Sends a query packet "ROTOR?\n" and expects a plain-text or JSON response.
 *
 * Example Arduino response: "135.5" or {"azimuth":135.5,"elevation":0.0}
 */
public class ArduinoRotorUdpProvider extends AbstractDataProvider<RotorData>
        implements RotorProvider {

    private final String host;
    private final int port;
    private static final int TIMEOUT_MS = 3_000;
    private static final int BUFFER_SIZE = 256;

    public ArduinoRotorUdpProvider(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected RotorData doFetch() throws DataProviderException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            // Send query
            byte[] query = "ROTOR?\n".getBytes(StandardCharsets.UTF_8);
            InetAddress addr = InetAddress.getByName(host);
            DatagramPacket sendPacket = new DatagramPacket(query, query.length, addr, port);
            socket.send(sendPacket);

            // Receive response
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(recvPacket);

            String response = new String(recvPacket.getData(), 0,
                recvPacket.getLength(), StandardCharsets.UTF_8).trim();

            return parseResponse(response);

        } catch (DataProviderException e) {
            throw e;
        } catch (java.net.SocketTimeoutException e) {
            throw new DataProviderException(
                "UDP timeout waiting for rotor response from " + host + ":" + port,
                DataProviderException.ErrorCode.TIMEOUT, e);
        } catch (Exception e) {
            throw new DataProviderException(
                "UDP rotor error: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private RotorData parseResponse(String response) throws DataProviderException {
        try {
            if (response.startsWith("{")) {
                // Quick JSON parse for azimuth
                double az = extractJsonDouble(response, "azimuth");
                double el = extractJsonDouble(response, "elevation");
                RotorData data = new RotorData(az, el);
                data.setConnected(true);
                return data;
            } else {
                double az = Double.parseDouble(response);
                RotorData data = new RotorData(az);
                data.setConnected(true);
                return data;
            }
        } catch (Exception e) {
            throw new DataProviderException(
                "Failed to parse UDP rotor response: " + response,
                DataProviderException.ErrorCode.PARSE_ERROR, e);
        }
    }

    private double extractJsonDouble(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return 0.0;
        int colon = json.indexOf(':', idx);
        int end = json.indexOf(',', colon);
        if (end < 0) end = json.indexOf('}', colon);
        String val = json.substring(colon + 1, end).trim();
        return Double.parseDouble(val);
    }
}
