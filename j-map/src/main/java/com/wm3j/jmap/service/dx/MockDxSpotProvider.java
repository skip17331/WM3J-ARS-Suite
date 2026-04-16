package com.wm3j.jmap.service.dx;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock DX spot provider with a rotating set of realistic global spots.
 */
public class MockDxSpotProvider extends AbstractDataProvider<List<DxSpot>>
        implements DxSpotProvider {

    private final Random random = new Random();

    // Sample DX station callsigns with approximate coordinates
    private static final Object[][] DX_STATIONS = {
        {"VK2XYZ",   -33.9,  151.2, "VK",  "Australia"},
        {"JA1ABC",    35.7,  139.7, "JA",  "Japan"},
        {"ZL2XYZ",   -41.3,  174.8, "ZL",  "New Zealand"},
        {"DK5ABC",    52.5,   13.4, "DL",  "Germany"},
        {"G0ABC",     51.5,   -0.1, "G",   "England"},
        {"F5XYZ",     48.9,    2.4, "F",   "France"},
        {"UA3ABC",    55.8,   37.6, "UA",  "Russia"},
        {"PY2XYZ",   -23.5,  -46.6, "PY",  "Brazil"},
        {"LU3ABC",   -34.6,  -58.4, "LU",  "Argentina"},
        {"ZS6XYZ",   -25.7,   28.2, "ZS",  "South Africa"},
        {"VU2XYZ",    28.6,   77.2, "VU",  "India"},
        {"HS0XYZ",    13.8,  100.5, "HS",  "Thailand"},
        {"7Z1ABC",    24.7,   46.7, "HZ",  "Saudi Arabia"},
        {"EA5XYZ",    39.5,   -0.4, "EA",  "Spain"},
        {"I2ABC",     45.5,    9.2, "I",   "Italy"},
        {"OH2XYZ",    60.2,   24.9, "OH",  "Finland"},
        {"SM5ABC",    59.3,   18.1, "SM",  "Sweden"},
        {"OZ5XYZ",    55.7,   12.6, "OZ",  "Denmark"},
        {"PA3ABC",    52.4,    4.9, "PA",  "Netherlands"},
        {"TA3XYZ",    39.9,   32.9, "TA",  "Turkey"},
        {"A61ABC",    24.5,   54.4, "A6",  "UAE"},
        {"9M2XYZ",     3.1,  101.7, "9M2", "West Malaysia"},
        {"KH6ABC",    21.3, -157.8, "KH6", "Hawaii"},
        {"KL7XYZ",    61.2, -149.9, "KL7", "Alaska"},
        {"CE3ABC",   -33.5,  -70.6, "CE",  "Chile"},
        {"CX5XYZ",   -34.9,  -56.2, "CX",  "Uruguay"},
        {"OA4ABC",   -12.0,  -77.0, "OA",  "Peru"},
        {"HI8XYZ",    18.5,  -69.9, "HI",  "Dominican Republic"},
        {"XE2ABC",    23.6, -102.5, "XE",  "Mexico"},
        {"TF3XYZ",    64.1,  -21.9, "TF",  "Iceland"},
    };

    // Spotter callsigns
    private static final String[] SPOTTERS = {
        "W1AW", "K0ABC", "N5XYZ", "VE3ABC", "DL5XYZ",
        "G4ABC", "JA1XYZ", "VK4ABC", "OH2XYZ", "F5ABC"
    };

    // Typical amateur frequencies by band (kHz)
    private static final double[] FREQ_OPTIONS = {
        1830, 1840,           // 160m
        3500, 3775, 3900,     // 80m
        7030, 7074, 7150,     // 40m
        10106, 10115,         // 30m
        14074, 14225, 14280,  // 20m
        18100, 18130,         // 17m
        21074, 21255, 21340,  // 15m
        24915, 24940,         // 12m
        28074, 28500, 28900,  // 10m
        50125, 50313,         // 6m
    };

    @Override
    protected List<DxSpot> doFetch() throws DataProviderException {
        List<DxSpot> spots = new ArrayList<>();
        int numSpots = 25 + random.nextInt(20);

        for (int i = 0; i < numSpots; i++) {
            Object[] station = DX_STATIONS[random.nextInt(DX_STATIONS.length)];
            String spotter = SPOTTERS[random.nextInt(SPOTTERS.length)];
            double freq = FREQ_OPTIONS[random.nextInt(FREQ_OPTIONS.length)];
            // Add slight frequency variation
            freq += (random.nextInt(5) - 2) * 0.1;

            DxSpot spot = new DxSpot(
                spotter,
                (String) station[0],
                freq,
                Instant.now().minusSeconds(random.nextInt(1800)) // 0-30 min old
            );
            spot.setDxLat((Double) station[1]);
            spot.setDxLon((Double) station[2]);
            spot.setDxccPrefix((String) station[3]);
            spot.setDxccEntity((String) station[4]);

            // Random comment
            if (random.nextInt(3) == 0) {
                String[] comments = {"599", "59+", "Good signal", "Weak but copy", "Strong", "QSB"};
                spot.setComment(comments[random.nextInt(comments.length)]);
            }

            spots.add(spot);
        }

        return spots;
    }
}
