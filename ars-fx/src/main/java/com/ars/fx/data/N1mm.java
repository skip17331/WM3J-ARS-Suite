package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The messages of N1MM+'s external UDP broadcast (https://n1mmwp.hamdocs.com/appendices/external-udp-broadcasts/), built from this suite's
 * records so GridTracker, Log4OM and the other tools that follow an N1MM+ log can follow ours. Pure: no network, no files - {@link N1mmBroadcast}
 * sends them. Element names and units follow N1MM+'s document (frequencies in units of 10 Hz, timestamps {@code YYYY-MM-DD HH:MM:SS} in UTC,
 * {@code exchange1} / {@code ismultiplier1..3} where the document's own examples have typos).
 */
public final class N1mm {
    private N1mm() {}

    public static final String APP = "ARS-Suite";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** What a message needs to know about this station and the moment. */
    public record Ctx(String mycall, String stationName, String opCall, long freqHz, boolean newMult, String oldCall, int contestNr) {}

    /** The rig, for {@code <RadioInfo>}. */
    public record Radio(boolean connected, long freqHz, long txFreqHz, String mode, boolean split, boolean transmitting, int radioNr, String opCall, String radioName) {}

    // ---- the messages ------------------------------------------------------------------------------------------------

    public static String contactInfo(ContestPlugin p, QsoRecord q, Ctx c)    { return xml("contactinfo", contactFields(p, q, c, false)); }
    public static String contactReplace(ContestPlugin p, QsoRecord q, Ctx c) { return xml("contactreplace", contactFields(p, q, c, true)); }
    /** Sent when a call has been typed and Space pressed, before the QSO is logged. */
    public static String lookupInfo(ContestPlugin p, QsoRecord q, Ctx c)     { return xml("lookupinfo", contactFields(p, q, c, false)); }

    public static String contactDelete(ContestPlugin p, QsoRecord q, Ctx c) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("app", APP);
        f.put("timestamp", ts(q.getDateTimeUtc()));
        f.put("mycall", c.mycall());
        f.put("band", band(q.getBand()));
        f.put("call", up(q.getCallsign()));
        f.put("contestnr", String.valueOf(c.contestNr()));
        f.put("StationName", c.stationName());
        f.put("ID", id(p, q));
        return xml("contactdelete", f);
    }

    public static String appInfo(ContestPlugin p, Ctx c) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("app", APP);
        f.put("dbname", "contest.db");
        f.put("contestnr", String.valueOf(c.contestNr()));
        f.put("contestname", contestName(p));
        f.put("StationName", c.stationName());
        f.put("mycall", c.mycall());
        return xml("AppInfo", f);
    }

    public static String radioInfo(Radio r, Ctx c) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("app", APP);
        f.put("StationName", c.stationName());
        f.put("RadioNr", String.valueOf(r.radioNr()));
        f.put("Freq", String.valueOf(r.freqHz() / 10));
        f.put("TXFreq", String.valueOf((r.txFreqHz() > 0 ? r.txFreqHz() : r.freqHz()) / 10));
        f.put("Mode", radioMode(r.mode()));
        f.put("OpCall", r.opCall() == null || r.opCall().isBlank() ? c.mycall() : r.opCall());
        f.put("IsRunning", "False");
        f.put("FocusEntry", "0");
        f.put("EntryWindowHwnd", "0");
        f.put("Antenna", "0");
        f.put("Rotors", "");
        f.put("FocusRadioNr", String.valueOf(r.radioNr()));
        f.put("IsStereo", "False");
        f.put("IsSplit", bool(r.split()));
        f.put("ActiveRadioNr", String.valueOf(r.radioNr()));
        f.put("IsTransmitting", bool(r.transmitting()));
        f.put("FunctionKeyCaption", "");
        f.put("RadioName", r.radioName() == null ? "" : r.radioName());
        f.put("AuxAntSelected", "-1");
        f.put("AuxAntSelectedName", "");
        f.put("IsConnected", bool(r.connected()));
        return xml("RadioInfo", f);
    }

    /** The contest score, in N1MM+'s {@code <dynamicresults>} form. {@code cab} is the Cabrillo category settings (power, assisted, operator, band, mode, club). */
    public static String score(ContestPlugin p, List<QsoRecord> qsos, int score, Ctx c, Map<String, String> cab, String grid) {
        Map<String, int[]> byBandMode = new TreeMap<>();                  // "band|mode" -> {qsos, points}
        int[] total = new int[2];
        for (QsoRecord q : qsos) {
            if (q.isDupe()) continue;
            String key = band(q.getBand()) + "|" + modeClass(q.getMode());
            int[] n = byBandMode.computeIfAbsent(key, k -> new int[2]);
            n[0]++; n[1] += q.getPoints(); total[0]++; total[1] += q.getPoints();
        }
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<dynamicresults>\n");
        b.append("  <contest>").append(esc(contestName(p))).append("</contest>\n");
        b.append("  <call>").append(esc(c.mycall())).append("</call>\n");
        b.append("  <ops>").append(esc(c.opCall())).append("</ops>\n");
        b.append("  <class power=\"").append(attr(cab.get("power"))).append("\" assisted=\"").append(attr(cab.get("assisted")))
                .append("\" transmitter=\"ONE\" ops=\"").append(attr(cab.get("operator"))).append("\" bands=\"").append(attr(cab.get("band")))
                .append("\" mode=\"").append(attr(cab.get("mode"))).append("\" overlay=\"N/A\" />\n");
        b.append("  <club>").append(esc(cab.getOrDefault("club", ""))).append("</club>\n");
        String g6 = grid == null ? "" : grid.trim().toUpperCase(Locale.ROOT);
        b.append("  <qth dxcccountry=\"\" cqzone=\"\" iaruzone=\"\" arrlsection=\"\" stprvoth=\"\" grid6=\"").append(attr(g6.length() > 6 ? g6.substring(0, 6) : g6)).append("\" />\n");
        b.append("  <breakdown>\n");
        b.append("    <qso band=\"total\" mode=\"ALL\">").append(total[0]).append("</qso>\n");
        for (Map.Entry<String, int[]> e : byBandMode.entrySet()) {
            String[] bm = e.getKey().split("\\|");
            b.append("    <qso band=\"").append(attr(bm[0])).append("\" mode=\"").append(attr(bm[1])).append("\">").append(e.getValue()[0]).append("</qso>\n");
        }
        b.append("    <point band=\"total\" mode=\"ALL\">").append(total[1]).append("</point>\n");
        for (Map.Entry<String, int[]> e : byBandMode.entrySet()) {
            String[] bm = e.getKey().split("\\|");
            b.append("    <point band=\"").append(attr(bm[0])).append("\" mode=\"").append(attr(bm[1])).append("\">").append(e.getValue()[1]).append("</point>\n");
        }
        b.append("  </breakdown>\n");
        b.append("  <score>").append(score).append("</score>\n");
        b.append("  <timestamp>").append(ts(LocalDateTime.now(java.time.ZoneOffset.UTC))).append("</timestamp>\n");
        b.append("</dynamicresults>\n");
        return b.toString();
    }

    // ---- a spot, a forwarded contact, and the one message N1MM+ receives that we act on ---------------------------------

    /** A DX cluster spot, for {@code <spot>}. {@code action} is "add" or "delete"; {@code status} is one of N1MM+'s words (single mult, dupe, ...) or "". */
    public record SpotMsg(String dxCall, long freqHz, String spotter, String comment, String mode, LocalDateTime time, String action, String status) {}

    /**
     * The {@code <spot>} message. N1MM+'s document shows the frequency as 7061.2 for a 40 m CW spot, so it is written in kHz with one decimal
     * (the document does not name the unit), and the time as {@code yyyy-MM-dd HH:mm:ss} like every other message (its own example has typos).
     */
    public static String spot(SpotMsg m, Ctx c) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("app", APP);
        f.put("StationName", c.stationName());
        f.put("dxcall", up(m.dxCall()));
        f.put("frequency", String.format(Locale.ROOT, "%.1f", m.freqHz() / 1000.0));
        f.put("spottercall", up(m.spotter()));
        f.put("timestamp", ts(m.time()));
        f.put("action", "delete".equalsIgnoreCase(m.action()) ? "delete" : "add");
        f.put("mode", n1mmMode(m.mode()));
        f.put("comment", nz(m.comment()));
        f.put("status", nz(m.status()));
        f.put("statuslist", nz(m.status()));
        return xml("spot", f);
    }

    /** A contact this computer received from another one and is passing on ("All Computers"): the same message with IsOriginal false. */
    public static String contactForwarded(ContestPlugin p, QsoRecord q, Ctx c) {
        Map<String, String> f = contactFields(p, q, c, false);
        f.put("IsOriginal", "False");
        return xml("contactinfo", f);
    }

    /** What an external program (a waterfall, a bandmap) asks for in {@code <radio_setfrequency>}: radio number and frequency. */
    public record SetFrequency(int radioNr, long freqHz) {}

    private static final Pattern SF_ROOT = Pattern.compile("<\\s*radio_setfrequency\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SF_FREQ = Pattern.compile("<\\s*frequency\\s*>\\s*([0-9]{1,9}(?:\\.[0-9]{1,6})?)\\s*<\\s*/\\s*frequency\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SF_RADIO = Pattern.compile("<\\s*radionr\\s*>\\s*([0-9]{1,2})\\s*<\\s*/\\s*radionr\\s*>", Pattern.CASE_INSENSITIVE);

    /**
     * Reads a {@code <radio_setfrequency>} message. N1MM+'s document gives the example 14053.284 and does not name the unit; it is read as kHz.
     * Null for anything else, for a message with no usable frequency, and for one with a DOCTYPE or entity (it is read as text, never parsed
     * as XML, so a hostile packet cannot do more than be ignored).
     */
    public static SetFrequency parseSetFrequency(String xml) {
        if (xml == null || xml.length() > 4096 || !SF_ROOT.matcher(xml).find()) return null;
        String low = xml.toLowerCase(Locale.ROOT);
        if (low.contains("<!doctype") || low.contains("<!entity")) return null;
        Matcher fm = SF_FREQ.matcher(xml);
        if (!fm.find()) return null;
        double khz;
        try { khz = Double.parseDouble(fm.group(1)); } catch (NumberFormatException e) { return null; }
        long hz = Math.round(khz * 1000.0);
        if (hz < 100_000L || hz > 450_000_000L) return null;                 // 100 kHz to 450 MHz: nothing outside the radio's range is a QSY
        Matcher rm = SF_RADIO.matcher(xml);
        int radio = rm.find() ? Integer.parseInt(rm.group(1)) : 1;
        return new SetFrequency(radio, hz);
    }

    // ---- a contact, field by field -----------------------------------------------------------------------------------

    static Map<String, String> contactFields(ContestPlugin p, QsoRecord q, Ctx c, boolean replace) {
        Map<String, String> ex = exchange(p, q);                      // the received exchange, by N1MM element
        String call = up(q.getCallsign()), ts = ts(q.getDateTimeUtc());
        DxccInfo.Info info = call.isEmpty() ? null : safeDxcc(call);
        Map<String, String> f = new LinkedHashMap<>();
        f.put("app", APP);
        f.put("contestname", contestName(p));
        f.put("contestnr", String.valueOf(c.contestNr()));
        f.put("timestamp", ts);
        if (replace) { f.put("oldcall", c.oldCall() == null || c.oldCall().isBlank() ? call : up(c.oldCall())); f.put("oldtimestamp", ts); }
        f.put("mycall", c.mycall());
        f.put("band", band(q.getBand()));
        f.put("rxfreq", String.valueOf(c.freqHz() / 10));
        f.put("txfreq", String.valueOf(c.freqHz() / 10));
        f.put("operator", q.getOperator() == null || q.getOperator().isBlank() ? c.opCall() : q.getOperator());
        f.put("mode", n1mmMode(q.getMode()));
        f.put("call", call);
        f.put("countryprefix", countryPrefix(call));
        f.put("wpxprefix", wpxPrefix(call));
        f.put("stationprefix", wpxPrefix(c.mycall()));
        f.put("continent", info == null || info.continent() == null ? "" : info.continent());
        f.put("snt", nz(q.getRstSent()));
        f.put("sntnr", num(q.getSerialSent()));
        f.put("rcv", nz(q.getRstReceived()));
        f.put("rcvnr", num(q.getSerialReceived()));
        f.put("gridsquare", ex.getOrDefault("gridsquare", ""));
        f.put("exchange1", ex.getOrDefault("exchange1", ""));
        f.put("section", ex.getOrDefault("section", ""));
        f.put("comment", nz(q.getNotes()));
        f.put("qth", nz(q.getCounty()));
        f.put("name", ex.getOrDefault("name", ""));
        f.put("power", ex.getOrDefault("power", ""));
        f.put("misctext", ex.getOrDefault("misctext", ""));
        f.put("zone", num(ex.get("zone")));
        f.put("prec", ex.getOrDefault("prec", ""));
        f.put("ck", num(ex.get("ck")));
        f.put("ismultiplier1", c.newMult() ? "1" : "0");
        f.put("ismultiplier2", "0");
        f.put("ismultiplier3", "0");
        f.put("points", String.valueOf(q.getPoints()));
        f.put("radionr", "1");
        f.put("run1run2", "1");
        f.put("RoverLocation", "");
        f.put("RadioInterfaced", "0");
        f.put("NetworkedCompNr", "0");
        f.put("IsOriginal", "True");
        f.put("NetBiosName", c.stationName());
        f.put("IsRunQSO", "0");
        f.put("StationName", c.stationName());
        f.put("ID", id(p, q));
        f.put("IsClaimedQso", "1");
        return f;
    }

    /** The DXCC entity's own prefix for a call ("K" for any US call, "DL", "JA", "KH6" for Hawaii), as N1MM+ counts countries; "" when it cannot be placed. */
    static String countryPrefix(String call) {
        if (call == null || call.isBlank()) return "";
        try { CtyData.Match m = CtyData.current().lookup(call); return m == null || m.entity().primary() == null ? "" : m.entity().primary(); }
        catch (RuntimeException e) { return ""; }
    }

    /** The received exchange, by the N1MM element it belongs in: section, grid, name, power, prec, ck, zone; anything else in exchange1 then misctext. */
    static Map<String, String> exchange(ContestPlugin p, QsoRecord q) {
        Map<String, String> out = new LinkedHashMap<>();
        if (p == null || p.getEntryFields() == null) return out;
        StringBuilder rest = new StringBuilder();
        String first = null;
        for (FieldDef fd : p.getEntryFields()) {
            String id = fd.getId();
            if (id == null || id.equals("callsign") || id.equals("band") || id.equals("mode") || id.contains("_sent") || id.startsWith("rst_") || id.startsWith("serial_")) continue;
            String slot = p.fieldSlotColumn(id);
            String v = slot == null ? null : slotValue(q, slot);
            if (v == null || v.isBlank()) continue;
            v = v.trim();
            switch (id) {
                case "state_prov_rcvd", "state_rcvd", "sect_rcvd", "section" -> out.put("section", v);
                case "grid_rcvd" -> out.put("gridsquare", v);
                case "name_rcvd" -> out.put("name", v);
                case "power_rcvd" -> out.put("power", v);
                case "precedence" -> out.put("prec", v);
                case "check" -> out.put("ck", v);
                case "cq_zone" -> out.put("zone", v);
                default -> { if (first == null) first = v; else rest.append(rest.length() == 0 ? "" : " ").append(v); }
            }
        }
        if (first != null) out.put("exchange1", first);
        if (rest.length() > 0) out.put("misctext", rest.toString());
        return out;
    }

    static String slotValue(QsoRecord q, String slot) {
        return switch (slot) {
            case "field1" -> q.getContestField1();
            case "field2" -> q.getContestField2();
            case "field3" -> q.getContestField3();
            case "field4" -> q.getContestField4();
            case "field5" -> q.getContestField5();
            default -> null;
        };
    }

    private static DxccInfo.Info safeDxcc(String call) { try { return DxccInfo.of(call); } catch (Throwable t) { return null; } }

    // ---- small conversions -------------------------------------------------------------------------------------------

    /** N1MM+'s name for a band: the lower MHz figure ("3.5", "14", "144"), not "80m". Empty if unknown. */
    public static String band(String b) {
        if (b == null) return "";
        return switch (b.trim().toLowerCase(Locale.ROOT)) {
            case "160m", "160" -> "1.8";
            case "80m", "80" -> "3.5";
            case "60m", "60" -> "5";
            case "40m", "40" -> "7";
            case "30m", "30" -> "10";
            case "20m", "20" -> "14";
            case "17m", "17" -> "18";
            case "15m", "15" -> "21";
            case "12m", "12" -> "24";
            case "10m", "10" -> "28";
            case "6m", "6" -> "50";
            case "2m", "2" -> "144";
            case "1.25m" -> "222";
            case "70cm" -> "432";
            case "33cm" -> "902";
            case "23cm" -> "1.2G";
            default -> b.trim();
        };
    }

    /** The mode of a logged contact: CW, SSB, RTTY and the digital names pass through; USB / LSB / FM / AM are phone. */
    public static String n1mmMode(String m) {
        String t = m == null ? "" : m.trim().toUpperCase(Locale.ROOT);
        return switch (t) { case "USB", "LSB", "PH", "PHONE" -> "SSB"; default -> t; };
    }

    /** The mode of the rig for {@code <RadioInfo>}: N1MM+ distinguishes USB and LSB there. */
    public static String radioMode(String m) {
        String t = m == null ? "" : m.trim().toUpperCase(Locale.ROOT);
        return switch (t) { case "CWR" -> "CW"; case "PKTUSB" -> "USB"; case "PKTLSB" -> "LSB"; case "" -> "CW"; default -> t; };
    }

    /** CW, PH or DIG: the mode classes of the score breakdown. */
    static String modeClass(String m) {
        String t = m == null ? "" : m.trim().toUpperCase(Locale.ROOT);
        return switch (t) { case "CW" -> "CW"; case "SSB", "USB", "LSB", "FM", "AM", "PH" -> "PH"; default -> "DIG"; };
    }

    private static final Pattern WPX = Pattern.compile("^([A-Z0-9]*?[0-9])(?=[A-Z]+$)");
    /** The WPX prefix: the letters and digits up to the last digit of the first group (K2DE -> K2, VE3XYZ -> VE3, 9A1A -> 9A1). */
    public static String wpxPrefix(String call) {
        String core = ScpData.core(call);
        Matcher m = WPX.matcher(core);
        return m.find() ? m.group(1) : "";
    }

    /** A QSO's id: 32 hex characters, the same for the contact, its replacement and its deletion. */
    public static String id(ContestPlugin p, QsoRecord q) {
        String seed = "ars-suite|" + (p == null ? "" : p.getContestId()) + "|" + q.getId();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }

    static String contestName(ContestPlugin p) { return p == null || p.getContestId() == null ? "" : p.getContestId(); }
    private static String ts(LocalDateTime t) { return (t == null ? LocalDateTime.now(java.time.ZoneOffset.UTC) : t).format(TS); }
    private static String up(String s) { return s == null ? "" : s.trim().toUpperCase(Locale.ROOT); }
    private static String nz(String s) { return s == null ? "" : s.trim(); }
    /** A number element: the digits as typed, or 0. */
    private static String num(String s) { return s != null && s.trim().matches("\\d{1,9}") ? s.trim() : "0"; }
    private static String bool(boolean b) { return b ? "True" : "False"; }

    // ---- XML ---------------------------------------------------------------------------------------------------------

    static String xml(String root, Map<String, String> fields) {
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<").append(root).append(">\n");
        for (Map.Entry<String, String> e : fields.entrySet())
            b.append("  <").append(e.getKey()).append('>').append(esc(e.getValue())).append("</").append(e.getKey()).append(">\n");
        return b.append("</").append(root).append(">\n").toString();
    }

    static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                default -> { if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') b.append(ch); }     // control characters are not legal in XML
            }
        }
        return b.toString();
    }
    private static String attr(String s) { return esc(s == null ? "" : s).replace("\"", "&quot;"); }
}
