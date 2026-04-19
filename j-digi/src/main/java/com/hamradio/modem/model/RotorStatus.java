package com.hamradio.modem.model;

public class RotorStatus {
    public String type      = "ROTOR_STATUS";
    public double bearing;    // degrees true, 0–360
    public double elevation;  // degrees, 0–90 (for EME / satellite)
    public String timestamp;  // ISO-8601 UTC
}
