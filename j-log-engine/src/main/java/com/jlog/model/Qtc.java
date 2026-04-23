package com.jlog.model;

import java.time.LocalDateTime;

/**
 * Worked-All-Europe QTC record — a "passed QSO" transmitted between an EU
 * and a DX station during a WAE contest. A QTC is a summary of an earlier
 * QSO: the time, callsign, and serial number of that prior contact.
 *
 * A QTC carries its own scoring (1 point on top of the regular QSO) but
 * does not add a new multiplier. Up to 10 QTCs may be exchanged per QSO
 * and the same QTC cannot be sent to the same peer twice.
 *
 * {@code direction} is either "SENT" (you transmitted the QTC to the peer)
 * or "RCVD" (you received it from the peer). {@code peerCall} is the station
 * on the other end of the exchange. {@code qtcCall}/{@code qtcSerial} identify
 * the QSO being referenced. {@code qsoId} links back to the parent QSO row if
 * known (so QTC sends can be grouped with the hosting QSO).
 */
public class Qtc {
    private long          id;
    private String        contestId;
    private String        direction;     // "SENT" | "RCVD"
    private String        peerCall;
    private LocalDateTime qtcTimeUtc;
    private String        qtcCall;
    private String        qtcSerial;
    private Long          qsoId;

    public long getId()                          { return id; }
    public void setId(long v)                    { this.id = v; }
    public String getContestId()                 { return contestId; }
    public void setContestId(String v)           { this.contestId = v; }
    public String getDirection()                 { return direction; }
    public void setDirection(String v)           { this.direction = v; }
    public String getPeerCall()                  { return peerCall; }
    public void setPeerCall(String v)            { this.peerCall = v; }
    public LocalDateTime getQtcTimeUtc()         { return qtcTimeUtc; }
    public void setQtcTimeUtc(LocalDateTime v)   { this.qtcTimeUtc = v; }
    public String getQtcCall()                   { return qtcCall; }
    public void setQtcCall(String v)             { this.qtcCall = v; }
    public String getQtcSerial()                 { return qtcSerial; }
    public void setQtcSerial(String v)           { this.qtcSerial = v; }
    public Long getQsoId()                       { return qsoId; }
    public void setQsoId(Long v)                 { this.qsoId = v; }
}
