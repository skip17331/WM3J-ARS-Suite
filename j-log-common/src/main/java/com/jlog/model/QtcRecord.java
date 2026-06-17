package com.jlog.model;

import java.time.LocalDateTime;

/**
 * One QTC line within a WAE-DC QTC series (Rule §7). Every correctly
 * transferred QTC is worth one point to the sender and one to the
 * receiver; the suite credits one point per stored row for the entrant.
 *
 * <p>{@code direction} is "RX" (we received this QTC from the partner)
 * or "TX" (we sent it to the partner). {@code seriesNo}/{@code seriesSize}
 * form the QTC-GRP ("3/7"). The reported-QSO fields are the contents of
 * the QTC itself.
 */
public class QtcRecord {

    private long id;
    private String contestId;
    private String partnerCall;     // the QTC RX or TX partner (the other station)
    private String direction;       // "RX" | "TX"
    private int    seriesNo;        // progressive series number (the "3" in 3/7)
    private int    seriesSize;      // QTCs in the series (the "7" in 3/7)
    private String qtcQrg;          // frequency of the QTC transmission
    private String qtcBand;
    private String qtcMode;
    private LocalDateTime qtcDateTimeUtc;
    private String qsoTime;         // reported QSO time, e.g. "1332"
    private String qsoCall;         // reported QSO callsign
    private String qsoSerial;       // reported QSO serial number

    public long   getId()                  { return id; }
    public void   setId(long v)            { this.id = v; }
    public String getContestId()           { return contestId; }
    public void   setContestId(String v)   { this.contestId = v; }
    public String getPartnerCall()         { return partnerCall; }
    public void   setPartnerCall(String v) { this.partnerCall = v != null ? v.toUpperCase().trim() : ""; }
    public String getDirection()           { return direction; }
    public void   setDirection(String v)   { this.direction = v; }
    public int    getSeriesNo()            { return seriesNo; }
    public void   setSeriesNo(int v)       { this.seriesNo = v; }
    public int    getSeriesSize()          { return seriesSize; }
    public void   setSeriesSize(int v)     { this.seriesSize = v; }
    public String getQtcQrg()              { return qtcQrg; }
    public void   setQtcQrg(String v)      { this.qtcQrg = v; }
    public String getQtcBand()             { return qtcBand; }
    public void   setQtcBand(String v)     { this.qtcBand = v; }
    public String getQtcMode()             { return qtcMode; }
    public void   setQtcMode(String v)     { this.qtcMode = v; }
    public LocalDateTime getQtcDateTimeUtc()        { return qtcDateTimeUtc; }
    public void   setQtcDateTimeUtc(LocalDateTime v){ this.qtcDateTimeUtc = v; }
    public String getQsoTime()             { return qsoTime; }
    public void   setQsoTime(String v)     { this.qsoTime = v; }
    public String getQsoCall()             { return qsoCall; }
    public void   setQsoCall(String v)     { this.qsoCall = v != null ? v.toUpperCase().trim() : ""; }
    public String getQsoSerial()           { return qsoSerial; }
    public void   setQsoSerial(String v)   { this.qsoSerial = v; }
}
