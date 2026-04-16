package com.jlog.civ;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Full bidirectional CI-V engine for Icom transceivers.
 *
 * Supports:
 *   - Auto-polling (periodic status reads)
 *   - Event-driven updates (radio-initiated unsolicited frames)
 *   - PTT control
 *   - CW keying via CI-V
 *   - Frequency/mode read and write
 *   - VFO A/B selection and split control
 *   - Filter/bandwidth control
 */
public class CivEngine {

    private static final Logger log = LoggerFactory.getLogger(CivEngine.class);

    // CI-V frame markers
    public static final byte PREAMBLE = (byte) 0xFE;
    public static final byte END_MARK = (byte) 0xFD;
    public static final byte OK_REPLY = (byte) 0xFB;
    public static final byte NG_REPLY = (byte) 0xFA;
    public static final byte CTRL_ADR = (byte) 0xE0; // Controller address

    // Common CI-V command codes
    public static final byte CMD_FREQ      = 0x00;
    public static final byte CMD_MODE      = 0x01;
    public static final byte CMD_BAND_EDGE = 0x02;
    public static final byte CMD_READ_FREQ = 0x03;
    public static final byte CMD_READ_MODE = 0x04;
    public static final byte CMD_WRITE_FREQ= 0x05;
    public static final byte CMD_WRITE_MODE= 0x06;
    public static final byte CMD_VFO       = 0x07;
    public static final byte CMD_MEMORY    = 0x08;
    public static final byte CMD_SCAN      = 0x0E;
    public static final byte CMD_SPLIT     = 0x0F;
    public static final byte CMD_TUNE      = 0x1C;
    public static final byte CMD_PTT       = 0x1C;
    public static final byte CMD_CW        = 0x17;
    public static final byte CMD_READ_METER= 0x15;
    public static final byte CMD_FILTER    = 0x10;

    // Sub-command for PTT (cmd 0x1C sub 0x00)
    public static final byte SUB_PTT    = 0x00;
    public static final byte SUB_TUNE   = 0x01;
    public static final byte PTT_ON     = 0x01;
    public static final byte PTT_OFF    = 0x00;

    private SerialPort serialPort;
    private String     portName;
    private int        baudRate;
    private byte       radioAddress = (byte) 0x94; // IC-7300 default

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService poller;
    private ExecutorService reader;

    // State
    private volatile long   currentFrequencyHz = 0;
    private volatile String currentMode        = "";
    private volatile boolean pttActive         = false;

    // Listeners
    private Consumer<Long>   freqListener;
    private Consumer<String> modeListener;
    private Consumer<byte[]> rawFrameListener;

    // Frame assembly buffer
    private final byte[] rxBuf = new byte[256];
    private int rxLen = 0;

    // Singleton
    private static final CivEngine INSTANCE = new CivEngine();
    public static CivEngine getInstance() { return INSTANCE; }
    private CivEngine() {}

    // ---------------------------------------------------------------
    // Connection management
    // ---------------------------------------------------------------

    public boolean connect(String port, int baud, byte radioAddr) {
        this.portName     = port;
        this.baudRate     = baud;
        this.radioAddress = radioAddr;

        try {
            serialPort = new SerialPort(port);
            serialPort.openPort();
            serialPort.setParams(baud,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

            running.set(true);
            startReader();
            startPoller();
            log.info("CI-V connected on {} @{} baud, radio addr=0x{}", port, baud,
                String.format("%02X", radioAddr & 0xFF));
            return true;
        } catch (SerialPortException ex) {
            log.error("CI-V connect failed on {}", port, ex);
            return false;
        }
    }

    public void disconnect() {
        running.set(false);
        if (poller != null) poller.shutdownNow();
        if (reader  != null) reader.shutdownNow();
        try {
            if (serialPort != null && serialPort.isOpened()) serialPort.closePort();
        } catch (SerialPortException ex) {
            log.warn("Error closing serial port", ex);
        }
        log.info("CI-V disconnected");
    }

    public boolean isConnected() {
        return serialPort != null && serialPort.isOpened() && running.get();
    }

    // ---------------------------------------------------------------
    // Background reader thread — parses incoming frames
    // ---------------------------------------------------------------

    private void startReader() {
        reader = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "civ-reader");
            t.setDaemon(true);
            return t;
        });
        reader.submit(() -> {
            while (running.get()) {
                try {
                    byte[] bytes = serialPort.readBytes(1, 100);
                    if (bytes != null) {
                        for (byte b : bytes) {
                            rxBuf[rxLen++] = b;
                            if (b == END_MARK) {
                                processFrame(Arrays.copyOf(rxBuf, rxLen));
                                rxLen = 0;
                            }
                            if (rxLen >= rxBuf.length) rxLen = 0; // overflow protection
                        }
                    }
                } catch (SerialPortException ex) {
                    if (running.get()) log.warn("Serial read error", ex);
                } catch (Exception ex) {
                    if (running.get()) log.warn("Reader error", ex);
                }
            }
        });
    }

    /** Parse a complete CI-V frame: FE FE <radio> <ctrl> <cmd> [sub] [data]... FD */
    private void processFrame(byte[] frame) {
        if (frame.length < 5) return;
        if (frame[0] != PREAMBLE || frame[1] != PREAMBLE) return;
        if (frame[frame.length - 1] != END_MARK) return;

        byte toAddr   = frame[2];
        byte fromAddr = frame[3];
        byte cmd      = frame[4];

        log.debug("CI-V RX: {}", toHex(frame));

        if (rawFrameListener != null) rawFrameListener.accept(frame);

        // Unsolicited frequency update from radio
        if (cmd == CMD_FREQ && frame.length >= 10) {
            long freq = bcdToHz(frame, 5, 5);
            currentFrequencyHz = freq;
            if (freqListener != null) freqListener.accept(freq);
        }

        // Unsolicited mode update from radio
        if (cmd == CMD_MODE && frame.length >= 7) {
            String mode = modeCodeToString(frame[5]);
            currentMode = mode;
            if (modeListener != null) modeListener.accept(mode);
        }

        // Response to READ_FREQ (0x03 reply)
        if (cmd == CMD_READ_FREQ && frame.length >= 10) {
            long freq = bcdToHz(frame, 5, 5);
            currentFrequencyHz = freq;
            if (freqListener != null) freqListener.accept(freq);
        }

        // Response to READ_MODE (0x04 reply)
        if (cmd == CMD_READ_MODE && frame.length >= 7) {
            String mode = modeCodeToString(frame[5]);
            currentMode = mode;
            if (modeListener != null) modeListener.accept(mode);
        }
    }

    // ---------------------------------------------------------------
    // Auto-poller — periodically requests freq/mode
    // ---------------------------------------------------------------

    private void startPoller() {
        int intervalMs = 500; // poll every 500ms
        poller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "civ-poller");
            t.setDaemon(true);
            return t;
        });
        poller.scheduleAtFixedRate(() -> {
            try {
                sendCommand(radioAddress, CTRL_ADR, CMD_READ_FREQ);
                Thread.sleep(50);
                sendCommand(radioAddress, CTRL_ADR, CMD_READ_MODE);
            } catch (Exception ex) {
                if (running.get()) log.warn("Poller error", ex);
            }
        }, 1000, intervalMs, TimeUnit.MILLISECONDS);
    }

    // ---------------------------------------------------------------
    // Command send methods
    // ---------------------------------------------------------------

    /** Set VFO frequency. freqHz = frequency in Hz. */
    public void setFrequency(long freqHz) {
        byte[] bcd = hzToBcd(freqHz);
        sendCommandWithData(radioAddress, CTRL_ADR, CMD_WRITE_FREQ, bcd);
    }

    /** Set mode. mode = "LSB","USB","AM","FM","CW","RTTY","CW-R","FSK-R","DV","DD" */
    public void setMode(String mode) {
        byte code = modeStringToCode(mode);
        sendCommandWithData(radioAddress, CTRL_ADR, CMD_WRITE_MODE,
            new byte[]{code, 0x01}); // 0x01 = normal filter
    }

    /** PTT on/off. */
    public void setPtt(boolean on) {
        sendCommandWithData(radioAddress, CTRL_ADR, CMD_PTT,
            new byte[]{SUB_PTT, on ? PTT_ON : PTT_OFF});
        pttActive = on;
        log.debug("PTT {}", on ? "ON" : "OFF");
    }

    /** Send CW text via CI-V (radio must support CW keying). */
    public void sendCw(String text) {
        if (text == null || text.isEmpty()) return;
        // CI-V CW send: command 0x17, data = ASCII bytes of the text
        byte[] ascii = text.getBytes();
        sendCommandWithData(radioAddress, CTRL_ADR, CMD_CW, ascii);
        log.debug("CW send: {}", text);
    }

    /** Select VFO A or B. vfo: 0=A, 1=B */
    public void selectVfo(int vfo) {
        sendCommandWithData(radioAddress, CTRL_ADR, CMD_VFO, new byte[]{(byte) vfo});
    }

    /** Enable/disable split mode. */
    public void setSplit(boolean on) {
        sendCommandWithData(radioAddress, CTRL_ADR, CMD_SPLIT, new byte[]{on ? (byte) 0x01 : 0x00});
    }

    /** Send a raw CI-V command by hex string, e.g. "FE FE 94 E0 03 FD" */
    public void sendRawHex(String hexString) {
        byte[] bytes = parseHex(hexString);
        writeBytes(bytes);
    }

    /** Request current frequency (will trigger freqListener callback). */
    public void requestFrequency() {
        sendCommand(radioAddress, CTRL_ADR, CMD_READ_FREQ);
    }

    /** Request current mode (will trigger modeListener callback). */
    public void requestMode() {
        sendCommand(radioAddress, CTRL_ADR, CMD_READ_MODE);
    }

    // ---------------------------------------------------------------
    // Low-level frame builders
    // ---------------------------------------------------------------

    private void sendCommand(byte to, byte from, byte cmd) {
        byte[] frame = new byte[]{PREAMBLE, PREAMBLE, to, from, cmd, END_MARK};
        log.debug("CI-V TX: {}", toHex(frame));
        writeBytes(frame);
    }

    private void sendCommandWithData(byte to, byte from, byte cmd, byte[] data) {
        byte[] frame = new byte[5 + data.length + 1];
        frame[0] = PREAMBLE;
        frame[1] = PREAMBLE;
        frame[2] = to;
        frame[3] = from;
        frame[4] = cmd;
        System.arraycopy(data, 0, frame, 5, data.length);
        frame[frame.length - 1] = END_MARK;
        log.debug("CI-V TX: {}", toHex(frame));
        writeBytes(frame);
    }

    private void writeBytes(byte[] bytes) {
        if (!isConnected()) return;
        try {
            serialPort.writeBytes(bytes);
        } catch (SerialPortException ex) {
            log.error("Serial write error", ex);
        }
    }

    // ---------------------------------------------------------------
    // BCD / frequency conversion utilities
    // ---------------------------------------------------------------

    /** Convert 5-byte CI-V BCD to Hz. */
    private long bcdToHz(byte[] frame, int offset, int len) {
        long hz = 0;
        long mult = 1;
        for (int i = offset; i < offset + len; i++) {
            int b = frame[i] & 0xFF;
            hz += (b & 0x0F) * mult;
            mult *= 10;
            hz += ((b >> 4) & 0x0F) * mult;
            mult *= 10;
        }
        return hz;
    }

    /** Convert Hz to 5-byte CI-V BCD. */
    private byte[] hzToBcd(long hz) {
        byte[] bcd = new byte[5];
        String s = String.format("%010d", hz);
        for (int i = 0; i < 5; i++) {
            int lo = s.charAt(9 - 2 * i)     - '0';
            int hi = s.charAt(9 - 2 * i - 1) - '0';
            bcd[i] = (byte) ((hi << 4) | lo);
        }
        return bcd;
    }

    private String modeCodeToString(byte code) {
        return switch (code & 0xFF) {
            case 0x00 -> "LSB";
            case 0x01 -> "USB";
            case 0x02 -> "AM";
            case 0x03 -> "CW";
            case 0x04 -> "RTTY";
            case 0x05 -> "FM";
            case 0x06 -> "WFM";
            case 0x07 -> "CW-R";
            case 0x08 -> "RTTY-R";
            case 0x11 -> "DV";
            case 0x12 -> "DD";
            default   -> "?";
        };
    }

    private byte modeStringToCode(String mode) {
        return switch (mode.toUpperCase()) {
            case "LSB"    -> 0x00;
            case "USB"    -> 0x01;
            case "AM"     -> 0x02;
            case "CW"     -> 0x03;
            case "RTTY"   -> 0x04;
            case "FM"     -> 0x05;
            case "WFM"    -> 0x06;
            case "CW-R"   -> 0x07;
            case "FSK-R","RTTY-R" -> 0x08;
            case "DV"     -> 0x11;
            case "DD"     -> 0x12;
            default       -> 0x01;
        };
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b & 0xFF));
        return sb.toString().trim();
    }

    private byte[] parseHex(String hex) {
        String[] parts = hex.trim().split("\\s+");
        byte[] result = new byte[parts.length];
        for (int i = 0; i < parts.length; i++)
            result[i] = (byte) Integer.parseInt(parts[i], 16);
        return result;
    }

    // ---------------------------------------------------------------
    // State accessors & listener registration
    // ---------------------------------------------------------------

    public long   getCurrentFrequencyHz() { return currentFrequencyHz; }
    public String getCurrentMode()        { return currentMode; }
    public boolean isPttActive()          { return pttActive; }

    public void setFrequencyListener(Consumer<Long>   l) { this.freqListener = l; }
    public void setModeListener     (Consumer<String> l) { this.modeListener = l; }
    public void setRawFrameListener (Consumer<byte[]> l) { this.rawFrameListener = l; }

    // ---------------------------------------------------------------
    // Helper: infer band from frequency
    // ---------------------------------------------------------------
    public static String freqToBand(long hz) {
        return freqKhzToBand(hz / 1000.0);
    }
    public static String freqKhzToBand(double khz) {
        if (khz >= 1800 && khz < 2000)   return "160m";
        if (khz >= 3500 && khz < 4000)   return "80m";
        if (khz >= 7000 && khz < 7300)   return "40m";
        if (khz >= 10100 && khz < 10150) return "30m";
        if (khz >= 14000 && khz < 14350) return "20m";
        if (khz >= 18068 && khz < 18168) return "17m";
        if (khz >= 21000 && khz < 21450) return "15m";
        if (khz >= 24890 && khz < 24990) return "12m";
        if (khz >= 28000 && khz < 29700) return "10m";
        if (khz >= 50000 && khz < 54000) return "6m";
        if (khz >= 144000 && khz < 148000) return "2m";
        if (khz >= 430000 && khz < 440000) return "70cm";
        return "?";
    }
}
