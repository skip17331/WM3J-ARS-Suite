package com.hamradio.jbridge.model;

/**
 * WsjtxStatus — WSJT-X Status message (UDP type 1).
 *
 * Published to j-hub as WSJTX_STATUS whenever WSJT-X status changes.
 * Frequency is in Hz to match j-hub wire conventions (same as RigStatus).
 */
public class WsjtxStatus {

    private long    dialFrequency;   // Hz — matches j-hub RigStatus.frequency convention
    private String  mode;
    private String  dxCall;
    private String  report;
    private int     txDf;
    private boolean transmitting;
    private boolean decoding;
    private boolean txEnabled;
    private boolean txWatchdog;
    private String  dxGrid;
    private String  band;            // derived e.g. "20m"

    public WsjtxStatus() {}

    public long    getDialFrequency()              { return dialFrequency; }
    public void    setDialFrequency(long v)        { this.dialFrequency = v; }

    public String  getMode()                       { return mode; }
    public void    setMode(String v)               { this.mode = v; }

    public String  getDxCall()                     { return dxCall; }
    public void    setDxCall(String v)             { this.dxCall = v; }

    public String  getReport()                     { return report; }
    public void    setReport(String v)             { this.report = v; }

    public int     getTxDf()                       { return txDf; }
    public void    setTxDf(int v)                  { this.txDf = v; }

    public boolean isTransmitting()                { return transmitting; }
    public void    setTransmitting(boolean v)      { this.transmitting = v; }

    public boolean isDecoding()                    { return decoding; }
    public void    setDecoding(boolean v)          { this.decoding = v; }

    public boolean isTxEnabled()                   { return txEnabled; }
    public void    setTxEnabled(boolean v)         { this.txEnabled = v; }

    public boolean isTxWatchdog()                  { return txWatchdog; }
    public void    setTxWatchdog(boolean v)        { this.txWatchdog = v; }

    public String  getDxGrid()                     { return dxGrid; }
    public void    setDxGrid(String v)             { this.dxGrid = v; }

    public String  getBand()                       { return band; }
    public void    setBand(String v)               { this.band = v; }
}
