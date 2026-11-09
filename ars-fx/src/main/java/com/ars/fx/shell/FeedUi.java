package com.ars.fx.shell;

import com.ars.fx.data.TelnetFeed;
import com.ars.fx.data.TelnetFeed.Status;

/** One wording and colour scheme for the live telnet feeds (DX cluster, RBN) in every module. */
public final class FeedUi {
    private FeedUi() {}

    /** CSS colour variable for a status: green connected, amber connecting, grey disconnected. */
    public static String color(Status s) {
        return switch (s) { case CONNECTED -> "-ars-ok"; case CONNECTING -> "-ars-warn"; default -> "-ars-t4"; };
    }

    /** Inline style for a status dot. */
    public static String dot(Status s) { return "-fx-background-color:" + color(s) + ";-fx-background-radius:99;"; }

    /** Colour a status dot, and let it breathe softly while the feed is connected. */
    public static void paint(javafx.scene.layout.Region dot, Status s) { dot.setStyle(dot(s)); Fx.pulse(dot, s == Status.CONNECTED); }

    /** "Connected · dxc.ve7cc.net". */
    public static String line(TelnetFeed f) { return f.status().label() + " · " + f.endpoint(); }

    /** Symbol + status + endpoint, for the controls drawer: "● Connected · host", "◌ Connecting… · host", "○ Disconnected · host". */
    public static String symbolLine(TelnetFeed f) {
        String sym = switch (f.status()) { case CONNECTED -> "●"; case CONNECTING -> "◌"; default -> "○"; };
        return sym + " " + line(f);
    }

    /** Text for an empty spot list, by status. {@code what} is e.g. "cluster". */
    public static String emptyList(TelnetFeed f, String what) {
        return switch (f.status()) {
            case CONNECTED -> "Connected — waiting for spots…";
            case CONNECTING -> "Connecting to the " + what + "…" + (f.detail().isEmpty() ? "" : " " + f.detail());
            default -> "Disconnected — press Connect to start the " + what + " feed";
        };
    }

    /** Label for the one connect/disconnect button. */
    public static String action(TelnetFeed f) {
        return switch (f.status()) { case CONNECTED -> "Disconnect"; case CONNECTING -> "Stop"; default -> "Connect"; };
    }

    /** What the button does: stop wanting a connection, or ask for one. */
    public static void toggle(TelnetFeed f) { if (f.wantConnected()) f.disconnect(); else f.connect(); }
}
