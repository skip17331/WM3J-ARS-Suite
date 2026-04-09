package com.hamclock.service.propagation;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;
import com.hamclock.service.propagation.PropagationData.BandCondition;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches propagation data from the HamQSL / N0NBH Solar Data XML API.
 *
 * Source: http://www.hamqsl.com/solarxml.php
 * Format: XML with solar flux, sunspots, A/K index, band conditions
 */
public class HamQslPropagationProvider extends AbstractDataProvider<PropagationData>
        implements PropagationDataProvider {

    private static final String API_URL = "http://www.hamqsl.com/solarxml.php";
    private static final int TIMEOUT_MS = 12_000;

    @Override
    protected PropagationData doFetch() throws DataProviderException {
        String xml = fetchXml();
        return parseXml(xml);
    }

    private String fetchXml() throws DataProviderException {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "HamClockClone/1.0");

            if (conn.getResponseCode() != 200) {
                throw new DataProviderException("HTTP " + conn.getResponseCode(),
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("HamQSL fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private PropagationData parseXml(String xml) throws DataProviderException {
        try {
            PropagationData data = new PropagationData();

            double sfi = parseXmlDouble(xml, "solarflux");
            double kp  = parseXmlDouble(xml, "kindex");
            int aIdx   = (int) parseXmlDouble(xml, "aindex");
            double muf = parseXmlDouble(xml, "muf");
            double fot = muf * 0.85;

            data.setSfi(sfi);
            data.setKp(kp);
            data.setMuf(muf);
            data.setFot(fot);
            data.setLuf(parseXmlDouble(xml, "luf"));

            // Parse band conditions from XML <calculatedconditions> block
            parseBandConditions(xml, data);

            return data;
        } catch (Exception e) {
            throw new DataProviderException("HamQSL parse failed: " + e.getMessage(),
                DataProviderException.ErrorCode.PARSE_ERROR, e);
        }
    }

    private void parseBandConditions(String xml, PropagationData data) {
        // HamQSL XML format: <band name="80m-40m" time="day">Good</band>
        Pattern bandPat = Pattern.compile(
            "<band\\s+name=\"([^\"]+)\"\\s+time=\"([^\"]+)\">([^<]+)</band>");
        Matcher m = bandPat.matcher(xml);

        while (m.find()) {
            String bandRange = m.group(1);
            String time      = m.group(2);
            String condition = m.group(3).trim();

            if (!"day".equalsIgnoreCase(time)) continue; // Use daytime conditions

            BandCondition bc = parseBandCondition(condition);

            // HamQSL groups bands: "80m-40m", "30m-15m", "12m-10m"
            switch (bandRange) {
                case "80m-40m" -> {
                    data.setBandCondition("80m", bc);
                    data.setBandCondition("60m", bc);
                    data.setBandCondition("40m", bc);
                }
                case "30m-15m" -> {
                    data.setBandCondition("30m", bc);
                    data.setBandCondition("20m", bc);
                    data.setBandCondition("17m", bc);
                    data.setBandCondition("15m", bc);
                }
                case "12m-10m" -> {
                    data.setBandCondition("12m", bc);
                    data.setBandCondition("10m", bc);
                }
                default -> log.warn("Unknown band range: {}", bandRange);
            }
        }
    }

    private BandCondition parseBandCondition(String s) {
        return switch (s.toUpperCase()) {
            case "EXCELLENT" -> BandCondition.EXCELLENT;
            case "GOOD"      -> BandCondition.GOOD;
            case "FAIR"      -> BandCondition.FAIR;
            case "POOR"      -> BandCondition.POOR;
            case "CLOSED"    -> BandCondition.CLOSED;
            default          -> BandCondition.FAIR;
        };
    }

    private double parseXmlDouble(String xml, String tag) {
        Pattern p = Pattern.compile("<" + tag + ">([\\d.]+)</" + tag + ">");
        Matcher m = p.matcher(xml);
        if (m.find()) return Double.parseDouble(m.group(1));
        return 0.0;
    }
}
