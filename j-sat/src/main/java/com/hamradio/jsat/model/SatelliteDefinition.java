package com.hamradio.jsat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Satellite capability definition from satellite-registry.json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SatelliteDefinition {

    public String       name;
    public String       type;
    public String       uplink;
    public String       downlink;
    public String       aprs;
    public long         uplinkHz;
    public long         downlinkHz;
    public long         aprsHz;
    public List<String> modes;
    public String       status;
    public String       notes;
    public boolean      enabled = true;
    public int          noradId;    // NORAD catalog number (0 = unknown)

    public boolean isLinearTransponder() {
        return type != null && type.toLowerCase().contains("linear");
    }

    public boolean isFm() {
        return type != null && (type.toLowerCase().contains("fm") ||
               (modes != null && modes.stream().anyMatch(m -> m.equalsIgnoreCase("FM Voice"))));
    }

    public boolean isAprs() {
        return aprsHz > 0 || (modes != null && modes.stream().anyMatch(m -> m.equalsIgnoreCase("APRS")));
    }

    @Override
    public String toString() { return name; }
}
