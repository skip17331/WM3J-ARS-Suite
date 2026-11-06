package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Sends the log to other programs over UDP in N1MM+'s format ({@link N1mm}), so GridTracker, Log4OM and the like can follow it live.
 * Off until the operator turns it on in J-Hub &#9656; Data (it puts the call, band, mode and exchange of each QSO on the network, to the
 * addresses given there and nowhere else). Contacts added, edited and erased in the contest log; the call as it is typed (a lookup); the
 * rig's frequency and mode, on change and every ten seconds; the contest on opening; and optionally the score. Sending never blocks the
 * logger and a failure never reaches the operator.
 */
public final class N1mmBroadcast {
    private N1mmBroadcast() {}

    public static final String KEY_ENABLED = "n1mm.enabled", KEY_DEST = "n1mm.dest", KEY_CONTACTS = "n1mm.contacts",
            KEY_RADIO = "n1mm.radio", KEY_LOOKUP = "n1mm.lookup", KEY_SCORE = "n1mm.score",
            KEY_SPOTS = "n1mm.spots", KEY_NORMAL = "n1mm.normal", KEY_ALLCOMP = "n1mm.allComputers";
    /** The message kinds that can be sent to an address of their own: the setting is {@code n1mm.dest.<kind>}, blank = the main address. */
    public static final List<String> KINDS = List.of("contacts", "lookup", "radio", "score", "spots", "appinfo");
    public static final String DEFAULT_DEST = "127.0.0.1:12060";
    public static final int DEFAULT_PORT = 12060, CONTEST_NR = 1;

    private static java.util.function.BiConsumer<String, String> senderOverride;      // tests: (kind, xml)
    static void setSenderForTest(Consumer<String> s) { senderOverride = s == null ? null : (k, x) -> s.accept(x); }
    static void setKindSenderForTest(java.util.function.BiConsumer<String, String> s) { senderOverride = s; }

    private static final ScheduledExecutorService POOL = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "n1mm-udp"); t.setDaemon(true); return t;
    });
    private static volatile boolean started;
    private static volatile RigClient.State lastRadio;
    private static volatile long lastRadioSent, lastScoreSent;

    public static boolean enabled() { return HubConfig.getBool(KEY_ENABLED, false); }
    private static boolean on(String key, boolean def) { return enabled() && HubConfig.getBool(key, def); }

    // ---- setup -------------------------------------------------------------------------------------------------------

    /** Where to send: {@code ip:port} pairs separated by spaces; a bare address gets the default port; anything unreadable is skipped. */
    public static List<InetSocketAddress> destinations(String text) {
        List<InetSocketAddress> out = new ArrayList<>();
        if (text == null) return out;
        for (String tok : text.trim().split("[\\s,;]+")) {
            if (tok.isEmpty()) continue;
            String host = tok; int port = DEFAULT_PORT;
            int i = tok.lastIndexOf(':');
            if (i > 0 && tok.indexOf(':') == i) {                       // host:port (an IPv6 literal has more than one colon and is not accepted)
                host = tok.substring(0, i);
                try { port = Integer.parseInt(tok.substring(i + 1)); } catch (NumberFormatException e) { continue; }
            }
            if (port < 1 || port > 65535 || !host.matches("[A-Za-z0-9.\\-]+")) continue;
            try { out.add(new InetSocketAddress(InetAddress.getByName(host), port)); } catch (Exception ignored) { /* unknown host: skipped */ }
        }
        return out;
    }

    /** The addresses for a kind of message: its own setting if it has one that names at least one address, else the main one. */
    public static List<InetSocketAddress> destinationsFor(String kind) {
        String own = HubConfig.get(KEY_DEST + "." + kind, "").trim();
        if (!own.isEmpty()) { List<InetSocketAddress> l = destinations(own); if (!l.isEmpty()) return l; }
        return destinations(HubConfig.get(KEY_DEST, DEFAULT_DEST));
    }

    /** Starts the rig-frequency reports. Call once at start-up; sending stays off until the operator enables it. */
    public static synchronized void init() {
        if (started) return;
        started = true;
        RigClient.getInstance().addListener(s -> { lastRadio = s; radioIfChanged(s); });
        POOL.scheduleWithFixedDelay(() -> { try { RigClient.State s = lastRadio; if (s != null) sendRadio(s); } catch (Throwable ignored) { } }, 10, 10, TimeUnit.SECONDS);
        ContestState.addListener(() -> { try { score(); } catch (Throwable ignored) { } });
        ClusterClient.getInstance().addListener("n1mm-spots", cc -> { try { spotsHeard(cc.spots()); } catch (Throwable ignored) { } });
        HubConfig.addChangeListener(k -> { if (k != null && k.startsWith("n1mm.")) N1mmListener.apply(); });
        N1mmListener.apply();
    }

    // ---- the messages ------------------------------------------------------------------------------------------------

    public static void contactAdded(ContestPlugin p, QsoRecord q, boolean newMult) {
        if (!on(KEY_CONTACTS, true)) return;
        send("contacts", N1mm.contactInfo(p, q, ctx(q.getBand(), newMult, null)));
    }
    public static void contactEdited(ContestPlugin p, QsoRecord q, String oldCall) {
        if (!on(KEY_CONTACTS, true)) return;
        send("contacts", N1mm.contactReplace(p, q, ctx(q.getBand(), false, oldCall)));
    }
    public static void contactDeleted(ContestPlugin p, QsoRecord q) {
        if (!on(KEY_CONTACTS, true)) return;
        send("contacts", N1mm.contactDelete(p, q, ctx(q.getBand(), false, null)));
    }
    /** The call typed so far, sent when Space is pressed (before the QSO is logged). */
    public static void lookup(ContestPlugin p, QsoRecord candidate) {
        if (!on(KEY_LOOKUP, true) || candidate.getCallsign() == null || candidate.getCallsign().isBlank()) return;
        send("lookup", N1mm.lookupInfo(p, candidate, ctx(candidate.getBand(), false, null)));
    }
    /** The contest being worked, when its page opens. */
    public static void appInfo(ContestPlugin p) {
        if (!enabled() || p == null) return;
        send("appinfo", N1mm.appInfo(p, ctx("", false, null)));
    }
    /** The score so far; at most once every two seconds, and only if the operator asked for it. */
    public static void score() {
        ContestPlugin p = ContestState.plugin();
        if (!on(KEY_SCORE, false) || p == null || ContestState.score() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastScoreSent < 2000) return;
        lastScoreSent = now;
        Map<String, String> cab = new LinkedHashMap<>();
        cab.put("operator", HubConfig.get("contest.cab.operator", "SINGLE-OP"));
        cab.put("power", HubConfig.get("contest.cab.power", "HIGH"));
        cab.put("assisted", HubConfig.get("contest.cab.assisted", "NON-ASSISTED"));
        cab.put("band", HubConfig.get("contest.cab.band", "ALL"));
        cab.put("mode", HubConfig.get("contest.cab.mode", "MIXED"));
        cab.put("club", HubConfig.get("contest.cab.club", ""));
        send("score", N1mm.score(p, ContestState.qsos(), ContestState.score().score(), ctx("", false, null), cab, HubConfig.grid()));
    }

    // ---- spots, the normal log, and contacts passed on from other computers -----------------------------------------------

    private static volatile long lastSpotMs = System.currentTimeMillis();            // only spots that arrive after start-up (or after the switch is turned on)

    /** Sends each cluster spot not sent before, oldest first. Returns how many went out. With spots switched off it only notes the newest, so turning them on does not send the backlog. */
    static int spotsHeard(List<ClusterClient.Spot> spots) {
        long newest = lastSpotMs;
        for (ClusterClient.Spot s : spots) newest = Math.max(newest, s.arrivalMs());
        if (!on(KEY_SPOTS, false)) { lastSpotMs = newest; return 0; }
        List<ClusterClient.Spot> fresh = new ArrayList<>();
        for (ClusterClient.Spot s : spots) if (s.arrivalMs() > lastSpotMs) fresh.add(s);
        fresh.sort(java.util.Comparator.comparingLong(ClusterClient.Spot::arrivalMs));
        lastSpotMs = newest;
        int n = 0;
        for (ClusterClient.Spot s : fresh) {
            if (n >= 40) break;                                                     // a burst after a reconnect is not worth flooding the network with
            java.time.LocalDateTime t = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(s.arrivalMs()), java.time.ZoneOffset.UTC);
            send("spots", N1mm.spot(new N1mm.SpotMsg(s.call(), s.freqHz(), s.de(), s.comment(), s.mode(), t, "add", ""), ctx(s.band(), false, null)));
            n++;
        }
        return n;
    }

    /** A QSO of the normal log, for the messages below. {@code dbId} is its row, which keeps the same ID through an edit. */
    public record NormalQso(long dbId, String utc, String call, String freqMhz, String band, String mode, String rstSent, String rstRcvd, String name, String comment) {}

    /** A QSO read back from the normal log. */
    public static NormalQso of(JLogDb.QsoFull q) {
        return new NormalQso(q.id(), q.datetimeUtc(), q.call(), q.freq(), q.band(), q.mode(), q.rstSent(), q.rstRcvd(), q.name(), q.notes());
    }

    static QsoRecord record(NormalQso n) {
        QsoRecord r = new QsoRecord();
        r.setId(n.dbId());
        r.setCallsign(n.call()); r.setBand(n.band()); r.setMode(n.mode());
        r.setRstSent(n.rstSent()); r.setRstReceived(n.rstRcvd()); r.setNotes(n.comment());
        try { r.setDateTimeUtc(java.time.LocalDateTime.parse(n.utc().trim().replace('T', ' '), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))); }
        catch (RuntimeException e) { r.setDateTimeUtc(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)); }
        return r;
    }
    private static N1mm.Ctx ctxFor(NormalQso n, String oldCall) {
        N1mm.Ctx base = ctx(n.band(), false, oldCall);
        try { double mhz = Double.parseDouble(n.freqMhz().trim()); if (mhz > 0) return new N1mm.Ctx(base.mycall(), base.stationName(), base.opCall(), Math.round(mhz * 1e6), false, oldCall, base.contestNr()); }
        catch (RuntimeException ignored) { /* the band's usual frequency */ }
        return base;
    }
    /** The normal log has no contest, so these go out with an empty contest name and no exchange: enough for a program that marks calls worked. */
    public static void normalAdded(NormalQso n) {
        if (!on(KEY_NORMAL, true) || !on(KEY_CONTACTS, true)) return;
        send("contacts", N1mm.contactInfo(null, record(n), ctxFor(n, null)));
    }
    public static void normalEdited(NormalQso n, String oldCall) {
        if (!on(KEY_NORMAL, true) || !on(KEY_CONTACTS, true)) return;
        send("contacts", N1mm.contactReplace(null, record(n), ctxFor(n, oldCall)));
    }
    public static void normalDeleted(NormalQso n) {
        if (!on(KEY_NORMAL, true) || !on(KEY_CONTACTS, true)) return;
        send("contacts", N1mm.contactDelete(null, record(n), ctxFor(n, null)));
    }

    /** A contest QSO that came in from another computer on the contest network, passed on as N1MM+'s "All Computers" does. Off by default. */
    public static void forwarded(ContestPlugin p, QsoRecord q) {
        if (!on(KEY_CONTACTS, true) || !on(KEY_ALLCOMP, false) || q == null) return;
        send("contacts", N1mm.contactForwarded(p, q, ctx(q.getBand(), false, null)));
    }

    private static void radioIfChanged(RigClient.State s) {
        RigClient.State prev = lastRadioSentState;
        boolean changed = prev == null || prev.freqHz() != s.freqHz() || prev.connected() != s.connected()
                || !java.util.Objects.equals(prev.mode(), s.mode()) || prev.split() != s.split() || prev.ptt() != s.ptt();
        if (changed && System.currentTimeMillis() - lastRadioSent >= 250) sendRadio(s);        // no more than four a second while tuning
    }
    private static volatile RigClient.State lastRadioSentState;
    private static void sendRadio(RigClient.State s) {
        if (!on(KEY_RADIO, true) || s == null || !s.connected() || s.freqHz() <= 0) return;
        lastRadioSent = System.currentTimeMillis(); lastRadioSentState = s;
        send("radio", N1mm.radioInfo(new N1mm.Radio(true, s.freqHz(), s.split() && s.vfoBHz() > 0 ? s.vfoBHz() : s.freqHz(), s.mode(), s.split(), s.ptt(), 1, HubConfig.call(), ""),
                ctx("", false, null)));
    }

    // ---- sending -----------------------------------------------------------------------------------------------------

    private static N1mm.Ctx ctx(String band, boolean newMult, String oldCall) {
        RigClient.State rig = RigClient.getInstance().last();
        long hz = rig != null && rig.connected() && rig.freqHz() > 0 ? rig.freqHz() : 0;
        if (hz == 0 && band != null) { Long b = RigClient.bandHz(band.replaceAll("[^0-9]", "")); if (b != null) hz = b; }     // no rig: the band's usual spot
        String call = HubConfig.call();
        return new N1mm.Ctx(call, stationName(), call, hz, newMult, oldCall, CONTEST_NR);
    }

    /** The computer's name, as N1MM+ gives it (upper case, at most 15 characters). */
    static String stationName() {
        String n = System.getenv("COMPUTERNAME");
        if (n == null || n.isBlank()) n = System.getenv("HOSTNAME");
        if (n == null || n.isBlank()) n = "ARS-SUITE";
        n = n.toUpperCase(Locale.ROOT);
        return n.length() > 15 ? n.substring(0, 15) : n;
    }

    /** Queues a message for every destination. Returns at once; a failure is swallowed (a dead address must never stop the logger). */
    static void send(String kind, String xml) {
        if (senderOverride != null) { senderOverride.accept(kind, xml); return; }
        List<InetSocketAddress> to = destinationsFor(kind);
        if (to.isEmpty()) return;
        byte[] data = xml.getBytes(StandardCharsets.UTF_8);
        POOL.execute(() -> {
            try (DatagramSocket s = new DatagramSocket()) {
                s.setBroadcast(true);
                for (InetSocketAddress a : to) {
                    try { s.send(new DatagramPacket(data, data.length, a)); } catch (Exception ignored) { /* one bad destination does not stop the others */ }
                }
            } catch (Exception ignored) { /* no network: nothing to do */ }
        });
    }
}
