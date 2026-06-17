package com.jlog.civ;

/**
 * CI-V connection configuration (persisted in config.db).
 */
public class CivConfig {

    private String  portName    = "/dev/ttyUSB0";
    private int     baudRate    = 19200;
    private int     radioAddress = 0x94;   // IC-7300 default
    private boolean autoConnect = false;
    private int     pollIntervalMs = 500;

    public String  getPortName()        { return portName; }
    public void    setPortName(String p){ this.portName = p; }

    public int     getBaudRate()        { return baudRate; }
    public void    setBaudRate(int b)   { this.baudRate = b; }

    public int     getRadioAddress()    { return radioAddress; }
    public void    setRadioAddress(int a){ this.radioAddress = a; }

    public boolean isAutoConnect()      { return autoConnect; }
    public void    setAutoConnect(boolean a){ this.autoConnect = a; }

    public int     getPollIntervalMs()  { return pollIntervalMs; }
    public void    setPollIntervalMs(int ms){ this.pollIntervalMs = ms; }
}
