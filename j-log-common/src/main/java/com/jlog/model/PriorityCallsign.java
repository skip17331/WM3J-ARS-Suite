package com.jlog.model;

/**
 * A callsign on the operator's priority watch list. When a DX spot
 * arrives whose callsign matches an entry here (case-insensitive,
 * suffix-stripped), the j-log priority alert service fires a banner
 * and/or audible alert.
 */
public class PriorityCallsign {

    private long    id;
    private String  callsign;
    private String  note;
    private boolean muted;
    private boolean audible = true;
    private boolean banner  = true;

    public PriorityCallsign() {}

    public PriorityCallsign(String callsign, String note) {
        this.callsign = callsign;
        this.note     = note;
    }

    public long    getId()       { return id; }
    public String  getCallsign() { return callsign; }
    public String  getNote()     { return note; }
    public boolean isMuted()     { return muted; }
    public boolean isAudible()   { return audible; }
    public boolean isBanner()    { return banner; }

    public void setId(long id)                { this.id = id; }
    public void setCallsign(String c)         { this.callsign = c; }
    public void setNote(String n)             { this.note = n; }
    public void setMuted(boolean m)           { this.muted = m; }
    public void setAudible(boolean a)         { this.audible = a; }
    public void setBanner(boolean b)          { this.banner = b; }
}
