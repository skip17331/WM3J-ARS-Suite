package com.hamclock.service.zones;

/**
 * Zone membership for a given lat/lon coordinate.
 */
public record ZoneInfo(int cqZone, int ituZone, String arrlSection, String dxccEntity) {

    public static final ZoneInfo UNKNOWN = new ZoneInfo(0, 0, "?", "Unknown");

    @Override
    public String toString() {
        return "CQ:" + cqZone + " ITU:" + ituZone + " " + arrlSection;
    }
}
