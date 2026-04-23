package com.wm3j.jmap.service.propagation;

import com.wm3j.jmap.service.astronomy.SolarPositionService;
import com.wm3j.jmap.service.solar.SolarData;

import java.time.ZonedDateTime;

/**
 * Computes a global MUF grid purely from solar data and geometry.
 * No network calls — deterministic given the same inputs.
 */
public class PropagationModelService {

    private static final int LON_CELLS = 72;
    private static final int LAT_CELLS = 36;
    private static final int LON_STEP  = 5;
    private static final int LAT_STEP  = 5;

    private final SolarPositionService solarPositionService;

    public PropagationModelService(SolarPositionService solarPositionService) {
        this.solarPositionService = solarPositionService;
    }

    public PropagationModelData compute(double qthLat, double qthLon,
                                        SolarData solar, ZonedDateTime utc) {
        double sfi = (solar != null) ? solar.getSfi() : 80.0;
        double foF2max = Math.max(2.0, 3.0 + (sfi - 70.0) * 0.04);

        double[][] grid = new double[LON_CELLS][LAT_CELLS];

        for (int li = 0; li < LON_CELLS; li++) {
            double cellLon = -180.0 + li * LON_STEP + LON_STEP / 2.0;
            for (int lj = 0; lj < LAT_CELLS; lj++) {
                double cellLat = -90.0 + lj * LAT_STEP + LAT_STEP / 2.0;

                double midLat = (qthLat + cellLat) / 2.0;
                double midLon = (qthLon + cellLon) / 2.0;

                double zenith = solarPositionService.computeZenithAngle(midLat, midLon, utc);
                double zenithRad = Math.toRadians(zenith);
                double dayFactor = Math.max(0.0, Math.cos(zenithRad));
                double foF2 = foF2max * (0.25 + 0.75 * dayFactor);
                foF2 = Math.max(2.0, foF2);

                double distKm = haversine(qthLat, qthLon, cellLat, cellLon);
                double secFactor;
                if      (distKm < 100)  secFactor = 1.0;
                else if (distKm < 500)  secFactor = 1.5;
                else if (distKm < 2000) secFactor = 3.0;
                else if (distKm < 4000) secFactor = 3.6;
                else                    secFactor = 4.2;

                grid[li][lj] = foF2 * secFactor;
            }
        }

        PropagationModelData data = new PropagationModelData();
        data.setMufGrid(grid);
        data.setQthLat(qthLat);
        data.setQthLon(qthLon);
        return data;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
