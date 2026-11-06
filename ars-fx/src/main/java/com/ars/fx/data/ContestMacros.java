package com.ars.fx.data;

import java.util.List;
import java.util.Locale;

/**
 * The standard F1-F8 messages for a contest, built on two placeholders the contest page fills in at send time: {@code {EXCH}}, what you
 * send in this contest (report, serial, zone, state ... in the contest's own order), and {@code {NR}}, your next serial number. The
 * station placeholders ({@code {MYCALL}}) and the worked call ({@code {CALL}}) work as in every macro.
 */
public final class ContestMacros {
    private ContestMacros() {}

    public static final String DEFAULT_CQ = "CQ TEST {MYCALL}";

    /** CQ, the exchange, TU, call + exchange (search and pounce), your call, again?, their call?, QRZ?. */
    public static List<MacroConfig.Macro> standard(String cq) {
        String cqText = cq == null || cq.isBlank() ? DEFAULT_CQ : cq.trim();
        return List.of(
                new MacroConfig.Macro("CQ", cqText, ""),
                new MacroConfig.Macro("Exch", "{EXCH}", ""),
                new MacroConfig.Macro("TU", "TU {MYCALL}", ""),
                new MacroConfig.Macro("S&P", "{CALL} {EXCH}", ""),
                new MacroConfig.Macro("My Call", "{MYCALL}", ""),
                new MacroConfig.Macro("Agn?", "AGN", ""),
                new MacroConfig.Macro("Call?", "{CALL} ?", ""),
                new MacroConfig.Macro("QRZ?", "QRZ? {MYCALL}", ""));
    }

    /** CW cut numbers for the report: 599 is sent as 5NN. Other tokens are left as they are. */
    public static String cut(String exchange) {
        if (exchange == null) return "";
        StringBuilder out = new StringBuilder();
        for (String tok : exchange.trim().split("\\s+")) {
            if (out.length() > 0) out.append(' ');
            out.append(tok.equals("599") ? "5NN" : tok);
        }
        return out.toString().toUpperCase(Locale.ROOT);
    }
}
