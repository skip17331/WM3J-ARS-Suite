package com.hamradio.digitalbridge.model;

import java.time.Instant;

/**
 * WsjtxQsoLogged — WSJT-X QSO Logged message (UDP type 5).
 *
 * Published to j-hub as WSJTX_QSO_LOGGED.  Also used to add the callsign
 * to the local worked list so decode rows update their colour immediately.
 */
public class WsjtxQsoLogged {

    private Instant timestamp;
    private String  dxCall;
    private String  dxGrid;
    private long    dialFrequency;   // Hz
    private String  mode;
    private String  band;
    private String  rstSent;
    private String  rstReceived;
    private String  txPower;
    private String  comments;
    private String  name;
    private Instant timeOn;
    private Instant timeOff;
    private String  operatorCall;
    private String  myCall;
    private String  myGrid;
    private String  exchangeSent;
    private String  exchangeReceived;
    private String  propagationMode;

    // Enrichment set locally before publish
    private String country;
    private String continent;
    private int    dxcc;

    public WsjtxQsoLogged() {}

    public Instant getTimestamp()                      { return timestamp; }
    public void    setTimestamp(Instant v)             { this.timestamp = v; }

    public String  getDxCall()                         { return dxCall; }
    public void    setDxCall(String v)                 { this.dxCall = v; }

    public String  getDxGrid()                         { return dxGrid; }
    public void    setDxGrid(String v)                 { this.dxGrid = v; }

    public long    getDialFrequency()                  { return dialFrequency; }
    public void    setDialFrequency(long v)            { this.dialFrequency = v; }

    public String  getMode()                           { return mode; }
    public void    setMode(String v)                   { this.mode = v; }

    public String  getBand()                           { return band; }
    public void    setBand(String v)                   { this.band = v; }

    public String  getRstSent()                        { return rstSent; }
    public void    setRstSent(String v)                { this.rstSent = v; }

    public String  getRstReceived()                    { return rstReceived; }
    public void    setRstReceived(String v)            { this.rstReceived = v; }

    public String  getTxPower()                        { return txPower; }
    public void    setTxPower(String v)                { this.txPower = v; }

    public String  getComments()                       { return comments; }
    public void    setComments(String v)               { this.comments = v; }

    public String  getName()                           { return name; }
    public void    setName(String v)                   { this.name = v; }

    public Instant getTimeOn()                         { return timeOn; }
    public void    setTimeOn(Instant v)                { this.timeOn = v; }

    public Instant getTimeOff()                        { return timeOff; }
    public void    setTimeOff(Instant v)               { this.timeOff = v; }

    public String  getOperatorCall()                   { return operatorCall; }
    public void    setOperatorCall(String v)           { this.operatorCall = v; }

    public String  getMyCall()                         { return myCall; }
    public void    setMyCall(String v)                 { this.myCall = v; }

    public String  getMyGrid()                         { return myGrid; }
    public void    setMyGrid(String v)                 { this.myGrid = v; }

    public String  getExchangeSent()                   { return exchangeSent; }
    public void    setExchangeSent(String v)           { this.exchangeSent = v; }

    public String  getExchangeReceived()               { return exchangeReceived; }
    public void    setExchangeReceived(String v)       { this.exchangeReceived = v; }

    public String  getPropagationMode()                { return propagationMode; }
    public void    setPropagationMode(String v)        { this.propagationMode = v; }

    public String  getCountry()                        { return country; }
    public void    setCountry(String v)                { this.country = v; }

    public String  getContinent()                      { return continent; }
    public void    setContinent(String v)              { this.continent = v; }

    public int     getDxcc()                           { return dxcc; }
    public void    setDxcc(int v)                      { this.dxcc = v; }
}
