package com.ars.fx.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Finding the host without typing its address. A host station announces itself on the local network every five seconds (station name, port and
 * contest, nothing else - never the key); a station that is to join and has no address listens for a few seconds and picks the host it hears.
 * The announcement is a single UDP datagram to the broadcast address of each network the computer is on, so it does not leave the local network.
 */
public final class NetDiscovery {
    private NetDiscovery() {}

    public static final int PORT = 12071;
    private static final String MAGIC = "ARS-NET1";

    /** A host that announced itself. */
    public record Found(String station, String host, int port, String contest) {
        public String label() { return station + " (" + host + ":" + port + ")" + (contest == null || contest.isEmpty() ? "" : " - " + contest); }
    }

    // ---- the message -------------------------------------------------------------------------------------------------

    static String beacon(String station, int port, String contest) {
        return MAGIC + "|" + clean(station) + "|" + port + "|" + clean(contest == null ? "" : contest);
    }
    private static String clean(String s) { return s.replace('|', ' ').trim(); }

    /** The host in an announcement received from {@code from}, or null for anything else. */
    static Found parse(String text, InetAddress from) {
        if (text == null || from == null || text.length() > 200) return null;
        String[] p = text.split("\\|", -1);
        if (p.length != 4 || !MAGIC.equals(p[0]) || p[1].isBlank()) return null;
        try {
            int port = Integer.parseInt(p[2].trim());
            if (port < 1 || port > 65535) return null;
            return new Found(p[1].trim(), from.getHostAddress(), port, p[3].trim());
        } catch (NumberFormatException e) { return null; }
    }

    // ---- announcing --------------------------------------------------------------------------------------------------

    /** Keeps announcing until closed. */
    public static final class Announcer implements AutoCloseable {
        private final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "net-announce"); t.setDaemon(true); return t; });
        Announcer(Supplier<String> station, IntSupplier port, Supplier<String> contest, List<InetSocketAddress> extraTargets, int targetPort, boolean broadcast) {
            pool.scheduleWithFixedDelay(() -> {
                try {
                    byte[] data = beacon(station.get(), port.getAsInt(), contest.get()).getBytes(StandardCharsets.UTF_8);
                    try (DatagramSocket s = new DatagramSocket()) {
                        s.setBroadcast(true);
                        List<InetSocketAddress> to = new ArrayList<>(extraTargets);
                        if (broadcast) for (InetAddress b : broadcastAddresses()) to.add(new InetSocketAddress(b, targetPort));
                        for (InetSocketAddress a : to) { try { s.send(new DatagramPacket(data, data.length, a)); } catch (Exception ignored) { /* one network down */ } }
                    }
                } catch (Throwable ignored) { /* no network: nothing to announce on */ }
            }, 0, 5, TimeUnit.SECONDS);
        }
        @Override public void close() { pool.shutdownNow(); }
    }

    /** Starts announcing this host on every local network. */
    public static Announcer announce(Supplier<String> station, IntSupplier port, Supplier<String> contest) {
        return new Announcer(station, port, contest, List.of(), PORT, true);
    }

    /** As above, to chosen addresses and port (tests). */
    static Announcer announceTo(Supplier<String> station, IntSupplier port, Supplier<String> contest, List<InetSocketAddress> targets) {
        return new Announcer(station, port, contest, targets, PORT, false);
    }

    /** The broadcast address of each network interface that has one. */
    static List<InetAddress> broadcastAddresses() {
        List<InetAddress> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                ni.getInterfaceAddresses().forEach(ia -> { if (ia.getBroadcast() != null) out.add(ia.getBroadcast()); });
            }
        } catch (Exception ignored) { /* none */ }
        return out;
    }

    // ---- listening ---------------------------------------------------------------------------------------------------

    /** Listens for {@code ms} milliseconds and returns the hosts heard, one per station name. */
    public static List<Found> scan(int ms) { return scan(ms, PORT); }

    static List<Found> scan(int ms, int port) {
        Map<String, Found> found = new LinkedHashMap<>();
        try (DatagramSocket s = new DatagramSocket(null)) {
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(port));
            long end = System.currentTimeMillis() + ms;
            byte[] buf = new byte[512];
            while (true) {
                long left = end - System.currentTimeMillis();
                if (left <= 0) break;
                s.setSoTimeout((int) Math.min(left, 1000));
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    s.receive(p);
                    Found f = parse(new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8), p.getAddress());
                    if (f != null) found.put(f.station(), f);
                } catch (java.net.SocketTimeoutException ignored) { /* keep listening until the time is up */ }
            }
        } catch (Exception e) {
            return new ArrayList<>(found.values());
        }
        return new ArrayList<>(found.values());
    }
}
