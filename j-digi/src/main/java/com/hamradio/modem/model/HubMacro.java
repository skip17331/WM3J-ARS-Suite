package com.hamradio.modem.model;

public class HubMacro {
    public String key;    // "CQ", "ANS_CQ", "F1", etc.
    public String label;  // button display text
    public String text;   // template text; supports {MYCALL} {CALL} {RST} {NAME} {BAND} {FREQ} {MODE}
    public String type;   // "FIXED" | "PROGRAMMABLE"
}
