package com.wm3j.jmap.service.zones;

/**
 * Simplified zone lookup service.
 *
 * Returns approximate CQ zone, ITU zone, and ARRL section for any lat/lon.
 * This is a geographic approximation; exact zone boundaries require full
 * polygon datasets (DXCC entities, ARRL section maps).
 *
 * Maidenhead grid square calculation is exact.
 */
public class ZoneLookupService {

    /**
     * Compute the Maidenhead grid square (6-character) for a given lat/lon.
     */
    public String toGridSquare(double lat, double lon) {
        // Normalize
        double adjLon = lon + 180.0;
        double adjLat = lat + 90.0;

        int field1 = (int) (adjLon / 20.0);
        int field2 = (int) (adjLat / 10.0);
        int square1 = (int) ((adjLon % 20.0) / 2.0);
        int square2 = (int) (adjLat % 10.0);
        int sub1    = (int) ((adjLon % 2.0) / (2.0 / 24.0));
        int sub2    = (int) ((adjLat % 1.0) / (1.0 / 24.0));

        return "" +
            (char) ('A' + field1) +
            (char) ('A' + field2) +
            (char) ('0' + square1) +
            (char) ('0' + square2) +
            (char) ('a' + sub1) +
            (char) ('a' + sub2);
    }

    /**
     * Approximate CQ zone (1–40) for a lat/lon.
     * Based on ITU Region, latitude band, and longitude band.
     */
    public int toCqZone(double lat, double lon) {
        // North America
        if (lon >= -170 && lon <= -50 && lat >= 0) {
            if (lat > 75) return 1;                          // Arctic NA
            if (lat > 60) {
                if (lon < -100) return 1;                    // NW Canada/Alaska
                return 2;                                     // NE Canada
            }
            if (lat > 40) {
                if (lon < -115) return 3;                    // W USA / BC
                if (lon < -85) return 4;                     // Central USA
                return 5;                                     // E USA / Maritime
            }
            if (lat > 20) {
                if (lon < -85) return 6;                     // Mexico / Central Am
                return 8;                                     // Caribbean
            }
            return 8;                                        // Caribbean
        }

        // South America
        if (lon >= -85 && lon <= -30 && lat < 15 && lat > -60) {
            if (lat > 0) return 9;                           // NW South America
            if (lat > -15 && lon > -55) return 11;          // NE Brazil
            if (lat > -15) return 10;                        // W South America
            if (lat > -35) return 11;                        // Central South America
            if (lat > -50) return 12;                        // S South America
            return 13;                                        // Tierra del Fuego
        }

        // Europe
        if (lon >= -30 && lon <= 40 && lat >= 35) {
            if (lat > 70) return 18;                         // N Scandinavia / Svalbard → use 18 (Norwa overlap)
            if (lat > 60) return 18;                         // Scandinavia
            if (lat > 50) {
                if (lon < 0) return 14;                      // UK / Ireland
                if (lon < 20) return 14;                     // W Europe (France, etc.)
                return 15;                                    // C Europe
            }
            if (lon < 10) return 14;                         // Spain/Portugal
            if (lon < 25) return 15;                         // S/SE Europe
            return 20;                                        // E Europe
        }

        // Africa
        if (lon >= -20 && lon <= 55 && lat >= -40 && lat <= 38) {
            if (lat > 20) return 33;                         // N Africa
            if (lat > 0 && lon < 20) return 35;             // W Africa
            if (lat > 0) return 34;                          // E Africa
            if (lat > -20) return 36;                        // Central Africa
            return 38;                                        // S Africa
        }

        // Middle East
        if (lon >= 25 && lon <= 65 && lat >= 10 && lat <= 45) {
            return 21;
        }

        // Russia / Central Asia
        if (lon >= 40 && lon <= 180 && lat >= 50) {
            if (lon < 80) return 17;                         // W Siberia / Ural
            if (lon < 120) return 18;                        // C Siberia
            if (lon < 150) return 19;                        // E Siberia
            return 25;                                        // Far East Russia
        }

        // South Asia / India
        if (lon >= 60 && lon <= 100 && lat >= 5 && lat < 35) {
            return 26;
        }

        // Southeast Asia
        if (lon >= 95 && lon <= 145 && lat >= -10 && lat < 30) {
            return 26;
        }

        // China / East Asia
        if (lon >= 100 && lon <= 145 && lat >= 20 && lat < 50) {
            return 24;
        }

        // Japan / Korea
        if (lon >= 125 && lon <= 150 && lat >= 30 && lat < 50) {
            return 25;
        }

        // Australia / Oceania
        if (lon >= 110 && lon <= 160 && lat >= -50 && lat < 0) {
            return 29;
        }

        // New Zealand
        if (lon >= 165 && lon <= 180 && lat >= -50 && lat < -30) {
            return 32;
        }

        // Pacific Islands
        if (lon >= 160 || lon <= -140) {
            if (lat > 0) return 31;
            return 28;
        }

        // Antarctica
        if (lat < -60) return 38;

        return 1; // fallback
    }

    /**
     * Approximate ITU zone (1–90) for a lat/lon.
     */
    public int toItuZone(double lat, double lon) {
        // Rough latitude/longitude grid → ITU zone
        // ITU zones are organized by bands of latitude and regions

        // Zone 1: N Canada / Greenland (lat>70, W)
        if (lat > 75 && lon < -60) return 2;
        if (lat > 60 && lon >= -170 && lon < -100) return 1;
        if (lat > 60 && lon >= -100 && lon < -52) return 2;

        // N America main
        if (lat >= 40 && lat <= 75 && lon >= -130 && lon < -100) return 3;
        if (lat >= 40 && lat <= 75 && lon >= -100 && lon < -70) return 4;
        if (lat >= 40 && lat <= 75 && lon >= -70 && lon < -52) return 5;

        // Central / S America
        if (lat >= 25 && lat < 40 && lon >= -120 && lon < -85) return 6;
        if (lat >= 0 && lat < 25 && lon >= -120 && lon < -85) return 7;
        if (lat >= 0 && lat < 40 && lon >= -85 && lon < -55) return 8;
        if (lat >= -20 && lat < 0 && lon >= -85 && lon < -55) return 9;
        if (lat >= -40 && lat < -20 && lon >= -85 && lon < -55) return 10;
        if (lat < -40 && lon >= -85 && lon < -55) return 16;

        // Europe
        if (lat >= 60 && lon >= -30 && lon < 40) return 18;
        if (lat >= 40 && lat < 60 && lon >= -30 && lon < 0) return 27;
        if (lat >= 40 && lat < 60 && lon >= 0 && lon < 20) return 28;
        if (lat >= 40 && lat < 60 && lon >= 20 && lon < 40) return 29;
        if (lat >= 30 && lat < 40 && lon >= -10 && lon < 40) return 37;

        // N Africa
        if (lat >= 20 && lat < 40 && lon >= -20 && lon < 20) return 33;
        if (lat >= 20 && lat < 40 && lon >= 20 && lon < 55) return 34;

        // W Africa
        if (lat >= 0 && lat < 20 && lon >= -20 && lon < 20) return 35;
        if (lat >= -10 && lat < 20 && lon >= 20 && lon < 45) return 36;

        // S Africa
        if (lat < 0 && lon >= -20 && lon < 45) return 38;

        // Middle East
        if (lat >= 20 && lat < 45 && lon >= 40 && lon < 65) return 21;

        // Russia / C Asia
        if (lat >= 55 && lon >= 40 && lon < 80) return 29;
        if (lat >= 55 && lon >= 80 && lon < 120) return 30;
        if (lat >= 55 && lon >= 120 && lon < 160) return 31;
        if (lat >= 55 && lon >= 160) return 35;

        // South Asia
        if (lat >= 5 && lat < 35 && lon >= 60 && lon < 95) return 41;

        // SE Asia
        if (lat >= 0 && lat < 25 && lon >= 95 && lon < 130) return 49;

        // China
        if (lat >= 25 && lat < 55 && lon >= 80 && lon < 130) return 33;

        // Japan / Korea
        if (lat >= 30 && lat < 50 && lon >= 125 && lon < 148) return 25;

        // Australia
        if (lat >= -45 && lat < -10 && lon >= 110 && lon < 155) return 55;

        // NZ
        if (lat >= -50 && lat < -30 && lon >= 165) return 60;

        // Pacific
        if (lat >= 0 && (lon > 160 || lon < -130)) return 62;
        if (lat < 0 && (lon > 160 || lon < -130)) return 63;

        // Antarctica
        if (lat < -65) return 67;

        return 1; // fallback
    }

    /**
     * Approximate ARRL section for North American stations; "DX" for others.
     */
    public String toArrlSection(double lat, double lon) {
        // Only assign ARRL sections in North America
        if (lon < -50 && lon > -170 && lat > 24 && lat < 75) {
            return naArrlSection(lat, lon);
        }
        return "DX";
    }

    private String naArrlSection(double lat, double lon) {
        // Canada
        if (lat > 55) {
            if (lon < -100) return "NT";   // Northwest Territories / Yukon
            if (lon < -85) return "MB";    // Manitoba / Saskatchewan
            return "QC";                    // Quebec / Labrador
        }
        if (lat > 49) {
            if (lon < -115) return "BC";   // British Columbia
            if (lon < -102) return "AB";   // Alberta / Saskatchewan
            if (lon < -90) return "MB";    // Manitoba
            if (lon < -79) return "ON";    // Ontario
            return "QC";                    // Quebec / Maritime Provinces
        }

        // Pacific Northwest
        if (lat > 42 && lon < -116) {
            if (lat > 47) return "WWA";    // Western WA
            if (lon < -120) return "WWA";
            return "EWA";                   // Eastern WA / OR
        }

        // California
        if (lon < -114 && lat > 32 && lat < 42) {
            if (lat > 37) {
                if (lon < -122) return "SF";    // San Francisco
                return "SJV";                    // San Joaquin Valley
            }
            if (lon < -117) return "LAX";    // Los Angeles area
            return "SDG";                     // San Diego
        }

        // Mountain states
        if (lon >= -114 && lon < -104 && lat >= 37 && lat < 49) {
            if (lat > 44) return "ID";       // Idaho / Montana
            if (lon < -109) return "UT";     // Utah / NV
            return "CO";                      // Colorado
        }

        // Southwest
        if (lon < -104 && lat < 37) {
            if (lat < 32) return "AZ";       // Arizona / NM border
            if (lon < -110) return "AZ";
            return "NM";
        }

        // Texas
        if (lon >= -106 && lon < -94 && lat >= 26 && lat < 37) {
            if (lat < 30) return "STX";      // South Texas
            if (lon < -100) return "WTX";    // West Texas
            if (lon < -97) return "NTX";     // North Texas
            return "STX";
        }

        // Midwest
        if (lon >= -104 && lon < -82 && lat >= 37 && lat < 50) {
            if (lon < -97) return "KS";      // Kansas / Nebraska
            if (lon < -93) return "MO";      // Missouri / Iowa
            if (lon < -87) return "IL";      // Illinois / Indiana
            return "OH";                      // Ohio
        }

        // Deep South
        if (lon >= -94 && lon < -75 && lat >= 25 && lat < 37) {
            if (lon < -89) return "MS";      // Louisiana / Mississippi
            if (lon < -83) return "TN";      // Tennessee / Kentucky / Alabama
            return "SC";                      // Georgia / Carolina
        }

        // Northeast
        if (lon >= -82 && lon < -67 && lat >= 37 && lat < 47) {
            if (lat > 44) return "VT";       // Vermont / NH / ME
            if (lat > 42) return "CT";       // Connecticut / MA / RI
            if (lon < -74) return "WNY";     // Western NY / PA
            return "NJ";                      // NJ / DE / MD
        }

        // Florida
        if (lon >= -88 && lon < -79 && lat >= 24 && lat < 31) {
            return "NFL";
        }

        return "W1";  // fallback
    }

    /**
     * Approximate lat/lon for a callsign based on its ITU prefix.
     * Returns null if the prefix is unrecognized.
     * Coordinates are the geographic centre of the DXCC entity / region.
     */
    public double[] callsignToLatLon(String callsign) {
        if (callsign == null || callsign.isBlank()) return null;
        String cs = callsign.toUpperCase().replaceAll("[^A-Z0-9/]", "");
        // Strip portable suffix (e.g. W1AW/3 → W1AW)
        int slash = cs.indexOf('/');
        if (slash > 0) cs = cs.substring(0, slash);

        // Try longest prefix match first (up to 3 characters)
        for (int len = Math.min(3, cs.length()); len >= 1; len--) {
            double[] ll = prefixLatLon(cs.substring(0, len));
            if (ll != null) return ll;
        }
        return null;
    }

    private static double[] prefixLatLon(String p) {
        return switch (p) {
            // North America
            case "W", "K", "N", "AA","AB","AC","AD","AE","AF","AG","AI","AK","AL","AM","AN",
                 "W1","W2","W3","W4","W5","W6","W7","W8","W9","W0",
                 "K1","K2","K3","K4","K5","K6","K7","K8","K9","K0",
                 "N1","N2","N3","N4","N5","N6","N7","N8","N9","N0"
                 -> new double[]{38.0, -97.0};     // USA centre
            case "VE","VA","VY" -> new double[]{56.0, -96.0};  // Canada
            case "XE","XF","XG","XH","XI" -> new double[]{23.0, -102.0}; // Mexico
            case "TI" -> new double[]{10.0, -84.0};   // Costa Rica
            case "HP" -> new double[]{9.0, -80.0};    // Panama
            case "YN" -> new double[]{12.5, -85.5};   // Nicaragua
            case "HR" -> new double[]{15.0, -86.5};   // Honduras
            case "TG" -> new double[]{15.5, -90.5};   // Guatemala
            case "HH" -> new double[]{19.0, -72.0};   // Haiti
            case "HI" -> new double[]{19.0, -70.5};   // Dominican Republic
            case "KP4","WP4","NP4" -> new double[]{18.2, -66.5}; // Puerto Rico
            case "CO" -> new double[]{22.0, -79.0};   // Cuba
            // South America
            case "PY","PP","PQ","PR","PS","PT","PU","PV","PW","ZV","ZW","ZX","ZY","ZZ"
                 -> new double[]{-10.0, -53.0};    // Brazil
            case "LU","L2","L3","L4","L5","L6","L7","L8","L9"
                 -> new double[]{-34.0, -64.0};    // Argentina
            case "CE" -> new double[]{-33.0, -70.5}; // Chile
            case "OA" -> new double[]{-10.0, -76.0}; // Peru
            case "HK" -> new double[]{4.0, -74.0};   // Colombia
            case "YV" -> new double[]{8.0, -66.0};   // Venezuela
            // Europe
            case "G","M","2E","GW","GI","GM","GD","GU","GJ"
                 -> new double[]{52.5, -2.0};      // UK
            case "F","TM" -> new double[]{46.5, 2.5};   // France
            case "DL","DA","DB","DC","DD","DE","DF","DG","DH","DI","DJ","DK","DM","DN","DO","DP","DQ","DR"
                 -> new double[]{51.0, 10.0};      // Germany
            case "I","IQ","IR","IS","IW","IX","IY","IZ"
                 -> new double[]{42.5, 12.5};      // Italy
            case "EA","EB","EC","ED","EE","EF","EG","EH"
                 -> new double[]{40.0, -4.0};      // Spain
            case "CT","CR","CS" -> new double[]{39.5, -8.0};  // Portugal
            case "SP","SN","SO","SQ","SR" -> new double[]{52.0, 20.0};  // Poland
            case "OK","OL" -> new double[]{50.0, 15.5};  // Czech Republic
            case "OM" -> new double[]{48.7, 19.5};  // Slovakia
            case "OE" -> new double[]{47.5, 14.0};  // Austria
            case "HB" -> new double[]{47.0, 8.0};   // Switzerland
            case "PA","PB","PC","PD","PE","PF","PG","PH","PI"
                 -> new double[]{52.3, 5.3};        // Netherlands
            case "ON","OO","OP","OQ","OR","OS","OT" -> new double[]{50.5, 4.5};  // Belgium
            case "LX" -> new double[]{49.7, 6.2};   // Luxembourg
            case "SM","SA","SB","SC","SD","SE","SF","SG","SH","SI","SJ","SK","SL"
                 -> new double[]{60.0, 15.0};       // Sweden
            case "LA","LB","LC","LD","LE","LF","LG","LH","LI","LJ","LK","LL","LM","LN"
                 -> new double[]{62.0, 10.0};       // Norway
            case "OH","OF","OG","OI" -> new double[]{64.0, 26.0};  // Finland
            case "OZ","5Q" -> new double[]{56.0, 10.0};  // Denmark
            case "TF" -> new double[]{65.0, -18.0}; // Iceland
            case "EI","EJ" -> new double[]{53.0, -8.0};  // Ireland
            case "UA","UB","UC","UD","UE","UF","UG","UH","UI"
                 -> new double[]{60.0, 50.0};       // Russia (EU/AS)
            case "RA","RB","RC","RD","RE","RF","RG","RH","RI","RJ","RK","RL","RM",
                 "RN","RO","RP","RQ","RR","RS","RT","RU","RV","RW","RX","RY","RZ"
                 -> new double[]{60.0, 50.0};       // Russia
            case "ES" -> new double[]{59.0, 25.0};  // Estonia
            case "YL" -> new double[]{57.0, 25.0};  // Latvia
            case "LY" -> new double[]{55.5, 24.0};  // Lithuania
            case "HA","HG" -> new double[]{47.0, 19.0};  // Hungary
            case "YO","YP","YQ","YR" -> new double[]{45.5, 25.0};  // Romania
            case "LZ" -> new double[]{42.7, 25.4};  // Bulgaria
            case "SV","SW","SX","SY","SZ","J4"
                 -> new double[]{38.0, 22.0};       // Greece
            case "YU","YZ","4N" -> new double[]{44.0, 20.5};  // Serbia
            case "S5" -> new double[]{46.0, 15.0};  // Slovenia
            case "9A" -> new double[]{45.5, 16.5};  // Croatia
            case "E7" -> new double[]{44.0, 17.5};  // Bosnia
            case "Z3" -> new double[]{41.6, 21.7};  // North Macedonia
            case "ZA" -> new double[]{41.3, 20.0};  // Albania
            // Middle East / Asia
            case "4X","4Z" -> new double[]{31.5, 35.0};  // Israel
            case "TA","TC","YM" -> new double[]{39.0, 35.0};  // Turkey
            case "A4" -> new double[]{22.0, 57.5};  // Oman
            case "A6" -> new double[]{24.0, 54.0};  // UAE
            case "HZ","7Z" -> new double[]{24.0, 45.0};  // Saudi Arabia
            case "9K" -> new double[]{29.3, 47.7};  // Kuwait
            case "YA" -> new double[]{33.0, 65.0};  // Afghanistan
            case "AP" -> new double[]{30.0, 70.0};  // Pakistan
            case "VU","AT","AU","AV","AW" -> new double[]{20.0, 77.0};  // India
            case "JA","JB","JC","JD","JE","JF","JG","JH","JI","JJ","JK","JL","JM","JN","JO","JP","JQ","JR","JS"
                 -> new double[]{36.0, 138.0};      // Japan
            case "HL","6K","6L","6M","6N" -> new double[]{37.5, 127.5};  // South Korea
            case "BY","BA","BD","BG","BH","BI","BJ","BL","BM","BN","BO","BP","BQ","BR","BS","BT","BU","BW","BX"
                 -> new double[]{35.0, 105.0};      // China
            case "BV" -> new double[]{25.0, 121.5}; // Taiwan
            case "HS","E2" -> new double[]{15.0, 100.0};  // Thailand
            case "XW" -> new double[]{18.0, 103.0}; // Laos
            case "XV","3W" -> new double[]{16.0, 107.0};  // Vietnam
            case "S2","S3" -> new double[]{24.0, 90.0};   // Bangladesh
            case "4S" -> new double[]{7.9, 80.7};   // Sri Lanka
            case "9V","S6" -> new double[]{1.3, 103.8};  // Singapore
            case "YB","YC","YD","YE","YF","YG","YH" -> new double[]{-5.0, 120.0};  // Indonesia
            case "9M" -> new double[]{3.5, 109.5};  // Malaysia
            case "DU","4D","4E","4F","4G","4H","4I"
                 -> new double[]{13.0, 122.0};      // Philippines
            // Africa
            case "ZS","ZR","ZT","ZU" -> new double[]{-29.0, 25.0};  // South Africa
            case "EA8","EH8" -> new double[]{28.0, -15.5}; // Canary Islands
            case "CN" -> new double[]{32.0, -6.0};  // Morocco
            case "7X" -> new double[]{28.0, 3.0};   // Algeria
            case "TS" -> new double[]{34.0, 9.0};   // Tunisia
            case "5B" -> new double[]{35.0, 33.0};  // Cyprus
            case "SU" -> new double[]{26.0, 30.0};  // Egypt
            case "ST" -> new double[]{15.0, 30.0};  // Sudan
            case "ET" -> new double[]{9.0, 40.0};   // Ethiopia
            case "5Z" -> new double[]{1.0, 38.0};   // Kenya
            case "Z2","Z21" -> new double[]{-20.0, 30.0}; // Zimbabwe
            case "3DA" -> new double[]{-26.5, 31.5}; // Swaziland
            case "V5" -> new double[]{-22.0, 17.0}; // Namibia
            case "ZD8" -> new double[]{-15.9, -5.7}; // Ascension Is.
            // Oceania
            case "VK","AX" -> new double[]{-27.0, 133.0};  // Australia
            case "ZL","ZK","ZM" -> new double[]{-41.0, 174.0};  // New Zealand
            case "KH6","NH6","WH6","AH6" -> new double[]{20.7, -157.0}; // Hawaii
            case "KL","NL","WL" -> new double[]{64.0, -153.0};  // Alaska
            default -> null;
        };
    }

    /**
     * Lookup full zone info for a lat/lon.
     */
    public ZoneInfo lookup(double lat, double lon) {
        int cq  = toCqZone(lat, lon);
        int itu = toItuZone(lat, lon);
        String arrl = toArrlSection(lat, lon);
        String dxcc = approximateDxcc(lat, lon);
        return new ZoneInfo(cq, itu, arrl, dxcc);
    }

    private String approximateDxcc(double lat, double lon) {
        // Very coarse country/entity approximation for display purposes
        if (lat > 24 && lat < 50 && lon > -125 && lon < -66) return "United States";
        if (lat > 49 && lat < 75 && lon > -141 && lon < -52) return "Canada";
        if (lat > 60 && lon > -26 && lon < -13) return "Iceland";
        if (lat > 49 && lon > -11 && lon < 2 && lat < 62) return "United Kingdom";
        if (lat > 41 && lat < 52 && lon > -5 && lon < 10) return "France";
        if (lat > 47 && lat < 55 && lon > 6 && lon < 15) return "Germany";
        if (lat > 35 && lat < 47 && lon > 6 && lon < 19) return "Italy";
        if (lat > 36 && lat < 44 && lon > -9 && lon < 4) return "Spain";
        if (lat > 55 && lat < 71 && lon > 4 && lon < 32) return "Scandinavia";
        if (lat > 55 && lon > 32 && lon < 60) return "Russia (EU)";
        if (lat > 55 && lon > 60 && lon < 140) return "Russia (AS)";
        if (lat > 18 && lat < 55 && lon > 73 && lon < 135) return "China";
        if (lat > 30 && lat < 46 && lon > 129 && lon < 146) return "Japan";
        if (lat > 6 && lat < 37 && lon > 68 && lon < 98) return "India";
        if (lat > 23 && lat < 38 && lon > 31 && lon < 56) return "Middle East";
        if (lat > -35 && lat < -10 && lon > 113 && lon < 154) return "Australia";
        if (lat > -8 && lat < 38 && lon > -17 && lon < 42) return "Africa";
        if (lat > -5 && lat < 15 && lon > -85 && lon < -60) return "Central America";
        if (lat > -56 && lat < 12 && lon > -82 && lon < -34) return "South America";
        return "Unknown";
    }
}
