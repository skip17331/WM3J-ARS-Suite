package com.hamradio.jhub.callsign;

import java.time.Instant;

/** Normalized callsign lookup result from any supported provider. */
public class CallsignResult {

    public String  callsign;
    public boolean found;
    public String  source;          // "qrz" | "hamqth" | "hamdb" | "callook"

    // Identity
    public String  name;
    public String  firstName;
    public String  lastName;

    // Address
    public String  addr1;
    public String  addr2;
    public String  city;
    public String  state;
    public String  country;
    public String  zip;

    // Location
    public double  lat;
    public double  lon;
    public String  grid;            // Maidenhead locator

    // License
    public String  licenseClass;
    public String  licenseExpires;

    // Contact / extras
    public String  email;
    public String  imageUrl;
    public String  bio;
    public String  qslVia;
    public boolean lotwActive;
    public boolean eqslActive;

    // Metadata
    public String  error;
    public String  fetchedAt;

    public static CallsignResult notFound(String callsign, String reason) {
        CallsignResult r = new CallsignResult();
        r.callsign  = callsign;
        r.found     = false;
        r.error     = reason;
        r.fetchedAt = Instant.now().toString();
        return r;
    }
}
