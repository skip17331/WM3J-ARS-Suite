package com.hamradio.jhub.civ;

import java.io.ByteArrayOutputStream;

/**
 * Pure helper for Icom CI-V frame construction, BCD frequency encoding,
 * and mode-byte translation. No I/O; the on-the-wire layer lives in
 * {@code CivRigController}.
 *
 * <p>Wire frame: {@code FE FE <toAddr> <fromAddr> <cmd> [subcmd] [data...] FD}.
 * The controller's own address is conventionally {@code 0xE0}. Frequencies
 * are five bytes of packed BCD, least-significant nibble first. Each byte
 * holds two decimal digits — high nibble is the tens, low nibble the units.
 *
 * <p>Examples
 * <ul>
 *   <li>14.225.000 Hz → bytes {@code 00 50 22 14 00} (LSD first)</li>
 *   <li>{@code FE FE 94 E0 03 FD} reads frequency on a rig at address 0x94</li>
 * </ul>
 */
public final class CivProtocol {

    public static final int TO_RIG_DEFAULT = 0x94;   // IC-7300 — typical
    public static final int FROM_CONTROLLER = 0xE0;  // standard controller addr
    public static final byte START = (byte) 0xFE;
    public static final byte END   = (byte) 0xFD;
    public static final byte ACK   = (byte) 0xFB;
    public static final byte NAK   = (byte) 0xFA;

    public static final int CMD_FREQ_GET = 0x03;
    public static final int CMD_MODE_GET = 0x04;
    public static final int CMD_FREQ_SET = 0x05;
    public static final int CMD_MODE_SET = 0x06;
    public static final int CMD_PTT      = 0x1C;    // subcmd 0x00 = PTT
    public static final int SUBCMD_PTT   = 0x00;

    private CivProtocol() {}

    // ── Frame construction ───────────────────────────────────────────

    /** Build a complete CI-V frame: {@code FE FE <to> <from> <cmd> [data] FD}. */
    public static byte[] frame(int toAddr, int cmd, byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(START & 0xFF);
        out.write(START & 0xFF);
        out.write(toAddr & 0xFF);
        out.write(FROM_CONTROLLER & 0xFF);
        out.write(cmd & 0xFF);
        if (data != null) {
            try { out.write(data); } catch (java.io.IOException ignored) {}
        }
        out.write(END & 0xFF);
        return out.toByteArray();
    }

    // ── BCD frequency ────────────────────────────────────────────────

    /** Encode Hz as 5 BCD bytes, least-significant nibble first. */
    public static byte[] freqToBcd(long hz) {
        if (hz < 0) hz = 0;
        byte[] b = new byte[5];
        long n = hz;
        for (int i = 0; i < 5; i++) {
            int units = (int) (n % 10); n /= 10;
            int tens  = (int) (n % 10); n /= 10;
            b[i] = (byte) ((tens << 4) | units);
        }
        return b;
    }

    /** Decode 5 BCD bytes (LSD first) starting at offset to Hz. */
    public static long bcdToFreq(byte[] data, int off) {
        long hz  = 0;
        long mul = 1;
        for (int i = 0; i < 5; i++) {
            int byt   = data[off + i] & 0xFF;
            int tens  = (byt >> 4) & 0x0F;
            int units = byt & 0x0F;
            hz  += units * mul; mul *= 10;
            hz  += tens  * mul; mul *= 10;
        }
        return hz;
    }

    // ── Mode codes ───────────────────────────────────────────────────
    // Icom mode bytes are uniform across the post-2000 line. Filter byte
    // is optional and we leave it out on set (the rig keeps its current).

    public static byte modeNameToByte(String mode) {
        if (mode == null) return 0x01;
        String m = mode.trim().toUpperCase();
        switch (m) {
            case "LSB":              return 0x00;
            case "USB":              return 0x01;
            case "AM":               return 0x02;
            case "CW":               return 0x03;
            case "RTTY":             return 0x04;
            case "FM":               return 0x05;
            case "WFM":              return 0x06;
            case "CW-R": case "CWR": return 0x07;
            case "RTTY-R": case "RTTYR": return 0x08;
            case "DV":               return 0x17;
            case "DD":               return 0x22;
            // Digital modes ride USB on Icoms unless the rig has a DATA mode
            case "FT8": case "FT4": case "PSK31": case "PSK": case "JS8":
            case "DATA": case "PKT":
                return 0x01;
            default:                 return 0x01;
        }
    }

    public static String modeByteToName(int b) {
        switch (b & 0xFF) {
            case 0x00: return "LSB";
            case 0x01: return "USB";
            case 0x02: return "AM";
            case 0x03: return "CW";
            case 0x04: return "RTTY";
            case 0x05: return "FM";
            case 0x06: return "WFM";
            case 0x07: return "CW-R";
            case 0x08: return "RTTY-R";
            case 0x17: return "DV";
            case 0x22: return "DD";
            default:   return "USB";
        }
    }

    // ── Address parsing ──────────────────────────────────────────────

    /** Parse a CI-V address from the config field — accepts "94" or "0x94" hex. */
    public static int parseAddress(String s) {
        if (s == null) return TO_RIG_DEFAULT;
        String t = s.trim().toLowerCase();
        if (t.isEmpty()) return TO_RIG_DEFAULT;
        if (t.startsWith("0x")) t = t.substring(2);
        try {
            int v = Integer.parseInt(t, 16);
            return v & 0xFF;
        } catch (NumberFormatException e) {
            return TO_RIG_DEFAULT;
        }
    }
}
