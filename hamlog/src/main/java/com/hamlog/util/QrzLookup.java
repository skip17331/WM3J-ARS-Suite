package com.hamlog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * QRZ.com XML API callsign lookup.
 *
 * Usage:
 *   QrzLookup lookup = new QrzLookup(username, password);
 *   Map<String,String> data = lookup.lookup("W1AW");
 *
 * Returns map with keys: name, country, state, county, lat, lon,
 *   grid, email, class, addr1, addr2, zip
 */
public class QrzLookup {

    private static final Logger log = LoggerFactory.getLogger(QrzLookup.class);
    private static final String API_URL = "https://xmldata.qrz.com/xml/current/";

    private final String username;
    private final String password;
    private String sessionKey;

    public QrzLookup(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /** Lookup a callsign. Returns empty map on failure. */
    public Map<String, String> lookup(String callsign) {
        Map<String, String> result = new HashMap<>();
        try {
            if (sessionKey == null) login();
            if (sessionKey == null) return result;

            String url = API_URL + "?s=" + sessionKey + ";callsign=" + callsign;
            Document doc = fetchXml(url);
            if (doc == null) return result;

            // Check for session error / re-login
            NodeList sessions = doc.getElementsByTagName("Session");
            if (sessions.getLength() > 0) {
                Node err = ((Element) sessions.item(0)).getElementsByTagName("Error").item(0);
                if (err != null && err.getTextContent().contains("Session")) {
                    sessionKey = null;
                    login();
                    return lookup(callsign);
                }
            }

            NodeList callNodes = doc.getElementsByTagName("Callsign");
            if (callNodes.getLength() == 0) return result;
            Element call = (Element) callNodes.item(0);

            extractText(call, "fname",   result, "fname");
            extractText(call, "name",    result, "name");
            extractText(call, "country", result, "country");
            extractText(call, "state",   result, "state");
            extractText(call, "county",  result, "county");
            extractText(call, "lat",     result, "lat");
            extractText(call, "lon",     result, "lon");
            extractText(call, "grid",    result, "grid");
            extractText(call, "email",   result, "email");
            extractText(call, "class",   result, "class");
            extractText(call, "addr1",   result, "addr1");
            extractText(call, "addr2",   result, "addr2");
            extractText(call, "zip",     result, "zip");

            // Combine fname + name
            String fname = result.getOrDefault("fname", "");
            String lname = result.getOrDefault("name",  "");
            result.put("fullname", (fname + " " + lname).trim());

            log.debug("QRZ lookup {}: {}", callsign, result);
        } catch (Exception ex) {
            log.warn("QRZ lookup failed for {}: {}", callsign, ex.getMessage());
        }
        return result;
    }

    private void login() throws Exception {
        String url = API_URL + "?username=" + URLEncoder.encode(username, "UTF-8") +
                     ";password=" + URLEncoder.encode(password, "UTF-8") + ";agent=HamLog-1.0";
        Document doc = fetchXml(url);
        if (doc == null) return;
        NodeList keys = doc.getElementsByTagName("Key");
        if (keys.getLength() > 0) {
            sessionKey = keys.item(0).getTextContent().trim();
            log.info("QRZ login successful");
        } else {
            log.warn("QRZ login failed — check credentials");
        }
    }

    private Document fetchXml(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "HamLog/1.0");
        try (InputStream is = conn.getInputStream()) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(is);
        }
    }

    private void extractText(Element parent, String tag, Map<String,String> map, String key) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() > 0) map.put(key, nl.item(0).getTextContent().trim());
    }

    /** Compute great-circle bearing from (lat1,lon1) to (lat2,lon2) in degrees. */
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double rlat1 = Math.toRadians(lat1);
        double rlat2 = Math.toRadians(lat2);
        double y = Math.sin(dLon) * Math.cos(rlat2);
        double x = Math.cos(rlat1) * Math.sin(rlat2)
                 - Math.sin(rlat1) * Math.cos(rlat2) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (brng + 360) % 360;
    }

    /** Compute great-circle distance in km. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
