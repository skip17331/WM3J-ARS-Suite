package com.wm3j.jmap.service.pskreporter;

/**
 * A single reception report from PSK Reporter.
 */
public class PskSpot {

    String senderCallsign;
    String receiverCallsign;
    double senderLat;
    double senderLon;
    double receiverLat;
    double receiverLon;
    long   frequencyHz;
    String mode;
    int    snr;
    long   flowStartSeconds;

    public String getSenderCallsign()   { return senderCallsign; }
    public void   setSenderCallsign(String v)   { this.senderCallsign = v; }

    public String getReceiverCallsign() { return receiverCallsign; }
    public void   setReceiverCallsign(String v) { this.receiverCallsign = v; }

    public double getSenderLat()   { return senderLat; }
    public void   setSenderLat(double v)   { this.senderLat = v; }

    public double getSenderLon()   { return senderLon; }
    public void   setSenderLon(double v)   { this.senderLon = v; }

    public double getReceiverLat() { return receiverLat; }
    public void   setReceiverLat(double v) { this.receiverLat = v; }

    public double getReceiverLon() { return receiverLon; }
    public void   setReceiverLon(double v) { this.receiverLon = v; }

    public long   getFrequencyHz() { return frequencyHz; }
    public void   setFrequencyHz(long v)   { this.frequencyHz = v; }

    public String getMode()        { return mode; }
    public void   setMode(String v)        { this.mode = v; }

    public int    getSnr()         { return snr; }
    public void   setSnr(int v)    { this.snr = v; }

    public long   getFlowStartSeconds() { return flowStartSeconds; }
    public void   setFlowStartSeconds(long v) { this.flowStartSeconds = v; }

    public String getBand() {
        double mhz = frequencyHz / 1_000_000.0;
        if      (mhz <  2.5)  return "160m";
        else if (mhz <  5.0)  return "80m";
        else if (mhz <  6.5)  return "60m";
        else if (mhz <  8.0)  return "40m";
        else if (mhz < 11.5)  return "30m";
        else if (mhz < 16.0)  return "20m";
        else if (mhz < 19.5)  return "17m";
        else if (mhz < 22.5)  return "15m";
        else if (mhz < 26.0)  return "12m";
        else if (mhz < 40.0)  return "10m";
        else if (mhz < 55.0)  return "6m";
        else                   return "VHF+";
    }

    public long ageSeconds() {
        return System.currentTimeMillis() / 1000 - flowStartSeconds;
    }
}
