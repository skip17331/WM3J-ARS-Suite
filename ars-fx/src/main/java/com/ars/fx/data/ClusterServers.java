package com.ars.fx.data;

import java.util.List;

/** The public DX clusters offered in J-Hub's pick list. Text in the list reads "W3LPL · dxc.w3lpl.net:7373". */
public final class ClusterServers {
    private ClusterServers() {}

    public static final String DEFAULT_ADDRESS = "dxc.ve7cc.net:23";
    private static final String SEP = " · ";

    public record Server(String name, String address) {
        public String label() { return name + SEP + address; }
    }

    public static final List<Server> ALL = List.of(
            new Server("VE7CC", DEFAULT_ADDRESS),
            new Server("W3LPL", "dxc.w3lpl.net:7373"),
            new Server("NC7J", "dxc.nc7j.com:7373"),
            new Server("K1TTT", "dxc.k1ttt.net:7373"),
            new Server("W1NR", "dxc.w1nr.net:7300"),
            new Server("W1DX", "dxusa.net:7300"),
            new Server("HamQTH", "hamqth.com:7300"));

    /** The list text for an address: its name if it is one of ours, otherwise the address itself. */
    public static String label(String address) {
        for (Server s : ALL) if (s.address().equalsIgnoreCase(address)) return s.label();
        return address;
    }

    /** "W3LPL · dxc.w3lpl.net:7373" or a typed "host:port" → the address. */
    public static String addressOf(String text) {
        if (text == null) return "";
        int i = text.lastIndexOf(SEP);
        return (i >= 0 ? text.substring(i + SEP.length()) : text).trim();
    }

    /** The host of an address. */
    public static String host(String address) {
        int c = address.lastIndexOf(':');
        return c > 0 ? address.substring(0, c) : address;
    }

    /** The port of an address, or -1 when it has none, so the current port is kept. */
    public static int port(String address) {
        int c = address.lastIndexOf(':');
        if (c <= 0) return -1;
        try { return Integer.parseInt(address.substring(c + 1).trim()); } catch (NumberFormatException e) { return -1; }
    }
}
