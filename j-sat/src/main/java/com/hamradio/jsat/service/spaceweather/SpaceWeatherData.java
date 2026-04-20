package com.hamradio.jsat.service.spaceweather;

import java.time.Instant;

/**
 * Space weather data bundle from NOAA SWPC feeds.
 */
public class SpaceWeatherData {

    // Kp index (0–9)
    public double kp              = 0.0;
    public String kpLabel         = "Kp 0.0";

    // Solar wind (DSCOVR / ACE)
    public double solarWindSpeedKmS = 0.0;
    public double solarWindDensity  = 0.0;
    public double imfBt             = 0.0;
    public double imfBz             = 0.0;

    // X-ray flux
    public double xrayFluxWm2    = 0.0;
    public String xrayClass      = "---";

    // Proton flux
    public double protonFlux     = 0.0;

    // Aurora oval probability 0–100 for observer lat
    public double auroraProbability = 0.0;

    // Geomagnetic Kp forecast (next 8 periods)
    public double[] kpForecast   = new double[0];

    public Instant fetchedAt     = Instant.now();

    /** Color string for Kp gauge (JavaFX CSS hex). */
    public String kpColor() {
        if (kp >= 7) return "#ff2244";
        if (kp >= 5) return "#ff8800";
        if (kp >= 3) return "#ffdd00";
        return "#44cc44";
    }

    /** Color string for IMF Bz (negative = favorable for aurora). */
    public String bzColor() {
        if (imfBz < -10) return "#ff2244";
        if (imfBz < -5)  return "#ff8800";
        if (imfBz < 0)   return "#ffdd00";
        return "#44cc44";
    }

    public String xrayColor() {
        if (xrayClass.startsWith("X")) return "#ff2244";
        if (xrayClass.startsWith("M")) return "#ff8800";
        if (xrayClass.startsWith("C")) return "#ffdd00";
        return "#44cc44";
    }
}
