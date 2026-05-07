package com.morsetrainer.hardware.arduino;

import com.fazecast.jSerialComm.SerialPort;
import com.morsetrainer.core.AppConfig;
import com.morsetrainer.core.Logger;
import com.morsetrainer.decoder.KeyEvent;
import com.morsetrainer.hardware.KeyEventSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Arduino-based USB keyer reader. Expects the Arduino to send line-based
 * packets over serial, one per event:
 *
 *   Straight key:
 *     DOWN <millis>
 *     UP   <millis>
 *
 *   Iambic paddle (firmware-generated elements):
 *     ELEM DIT <millis>
 *     ELEM DAH <millis>
 *
 *   Simplified tokens (host-timestamped):
 *     DITDOWN, DITUP, DAHDOWN, DAHUP, DOWN, UP
 *
 *   Diagnostic / heartbeat (logged, no event emitted):
 *     READY <mode> <wpm>
 *     PING  <millis>
 *     OK    ...
 *     ERR   ...
 *
 * For ELEM packets, the parser synthesizes a clean DOWN/UP pair of the
 * appropriate dit or dah length so the upstream {@link com.morsetrainer.decoder.TimingDecoder}
 * sees a well-formed mark. The dit length is derived from the configured
 * keyer WPM (1200 / wpm).
 *
 * Tolerant of dropped lines and unknown packet types; the timing decoder
 * upstream handles any remaining noise gracefully.
 */
public class ArduinoKeyer implements KeyEventSource {

    private final String portName;
    private final int baudRate;
    private SerialPort port;
    private Thread reader;
    private volatile boolean running = false;

    public ArduinoKeyer(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }

    public ArduinoKeyer() {
        this(AppConfig.get().arduinoSerialPort, AppConfig.get().arduinoBaudRate);
    }

    @Override
    public void start(Consumer<KeyEvent> consumer) {
        if (running) return;
        if (portName == null || portName.isBlank()) {
            Logger.info("Arduino keyer: no serial port configured, source disabled");
            return;
        }
        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(baudRate, 8, 1, 0);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        if (!port.openPort()) {
            Logger.warn("Arduino keyer: cannot open %s", portName);
            return;
        }
        running = true;
        reader = new Thread(() -> readLoop(consumer), "arduino-keyer");
        reader.setDaemon(true);
        reader.start();
        Logger.info("Arduino keyer connected on %s @ %d baud", portName, baudRate);
    }

    private void readLoop(Consumer<KeyEvent> consumer) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(port.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = br.readLine()) != null) {
                for (KeyEvent ev : parse(line.trim())) {
                    consumer.accept(ev);
                }
            }
        } catch (Exception e) {
            if (running) Logger.warn("Arduino keyer read error: %s", e.getMessage());
        }
    }

    /**
     * Parse one serial line into 0..N KeyEvents.
     *  - DOWN/UP and simplified tokens emit a single event.
     *  - ELEM DIT/DAH emit a DOWN/UP pair separated by the configured dit or dah length.
     *  - Status packets (READY, PING, OK, ERR) are logged and produce no events.
     *  - Unrecognised lines produce no events.
     */
    static List<KeyEvent> parse(String line) {
        if (line.isEmpty()) return Collections.emptyList();

        // Status / heartbeat packets: log and ignore.
        if (line.startsWith("READY") || line.startsWith("PING")
                || line.startsWith("OK") || line.startsWith("ERR")) {
            Logger.info("Arduino: %s", line);
            return Collections.emptyList();
        }

        // ELEM DIT|DAH <millis>  -> synthesize down/up pair
        if (line.startsWith("ELEM ")) {
            String[] parts = line.split("\\s+");
            if (parts.length < 3) return Collections.emptyList();
            try {
                long t = Long.parseLong(parts[2]);
                int wpm = AppConfig.get().wpm;
                int ditMs = Math.max(1, 1200 / Math.max(1, wpm));
                int durMs = parts[1].equals("DAH") ? ditMs * 3 : ditMs;
                return List.of(KeyEvent.down(t), KeyEvent.up(t + durMs));
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }
        }

        // DOWN <millis> | UP <millis>
        if (line.startsWith("DOWN ") || line.startsWith("UP ")) {
            String[] parts = line.split("\\s+");
            try {
                long t = Long.parseLong(parts[1]);
                return List.of(parts[0].equals("DOWN") ? KeyEvent.down(t) : KeyEvent.up(t));
            } catch (NumberFormatException e) { return Collections.emptyList(); }
        }

        // Simplified tokens (host generates timestamp)
        long now = System.currentTimeMillis();
        return switch (line) {
            case "DITDOWN", "DAHDOWN", "DOWN" -> List.of(KeyEvent.down(now));
            case "DITUP",   "DAHUP",   "UP"   -> List.of(KeyEvent.up(now));
            default -> Collections.emptyList();
        };
    }

    @Override
    public void stop() {
        running = false;
        if (reader != null) reader.interrupt();
        if (port != null && port.isOpen()) port.closePort();
    }

    @Override public String name() { return "Arduino @ " + portName; }
    @Override public boolean isConnected() { return port != null && port.isOpen(); }
}
