package com.wm3j.jmap.service.pskreporter;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches reception reports from the PSK Reporter XML API.
 * Queries spots where the operator's callsign appears as either sender or receiver.
 */
public class LivePskReporterProvider extends AbstractDataProvider<PskReporterData>
        implements PskReporterProvider {

    private static final String BASE_URL =
        "https://retrieve.pskreporter.info/query?receiverCallsign=&senderCallsign="
        + "&fCall=%s&rptlimit=200&flowStartSeconds=-1800&modify=grid";

    private static final int TIMEOUT_MS = 20_000;

    private final String callsign;

    public LivePskReporterProvider(String callsign) {
        this.callsign = callsign;
    }

    @Override
    protected PskReporterData doFetch() throws DataProviderException {
        if (callsign == null || callsign.isBlank()) {
            throw new DataProviderException("No callsign configured for PSK Reporter",
                DataProviderException.ErrorCode.NOT_CONFIGURED);
        }

        String urlStr = String.format(BASE_URL, callsign.toUpperCase());
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/xml");

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new DataProviderException("PSK Reporter HTTP " + code,
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            try (InputStream is = conn.getInputStream()) {
                return parseXml(is);
            }
        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("PSK Reporter fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private PskReporterData parseXml(InputStream is) throws DataProviderException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entities for safety
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);

            List<PskSpot> spots = new ArrayList<>();
            NodeList nodes = doc.getElementsByTagName("receptionReport");

            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                PskSpot spot = new PskSpot();

                spot.setSenderCallsign(attr(el, "senderCallsign"));
                spot.setReceiverCallsign(attr(el, "receiverCallsign"));
                spot.setMode(attr(el, "mode"));

                String freq = attr(el, "frequency");
                if (!freq.isEmpty()) {
                    try { spot.setFrequencyHz(Long.parseLong(freq)); }
                    catch (NumberFormatException ignored) {}
                }

                String snrStr = attr(el, "sNR");
                if (!snrStr.isEmpty()) {
                    try { spot.setSnr(Integer.parseInt(snrStr)); }
                    catch (NumberFormatException ignored) {}
                }

                String flowStr = attr(el, "flowStartSeconds");
                if (!flowStr.isEmpty()) {
                    try { spot.setFlowStartSeconds(Long.parseLong(flowStr)); }
                    catch (NumberFormatException ignored) {}
                }

                // The modify=grid parameter adds senderLocator / receiverLocator with lat/lon
                String senderLoc   = attr(el, "senderLocator");
                String receiverLoc = attr(el, "receiverLocator");

                double[] sLL = locatorToLatLon(senderLoc);
                double[] rLL = locatorToLatLon(receiverLoc);
                spot.setSenderLat(sLL[0]);
                spot.setSenderLon(sLL[1]);
                spot.setReceiverLat(rLL[0]);
                spot.setReceiverLon(rLL[1]);

                spots.add(spot);
            }

            PskReporterData data = new PskReporterData();
            data.setSpots(spots);
            data.setFetchTime(Instant.now());
            return data;

        } catch (Exception e) {
            throw new DataProviderException("PSK Reporter XML parse failed: " + e.getMessage(),
                DataProviderException.ErrorCode.PARSE_ERROR, e);
        }
    }

    private String attr(Element el, String name) {
        String v = el.getAttribute(name);
        return v != null ? v.trim() : "";
    }

    private double[] locatorToLatLon(String loc) {
        if (loc == null || loc.length() < 4) return new double[]{0, 0};
        loc = loc.toUpperCase();
        double lon = (loc.charAt(0) - 'A') * 20.0 - 180.0;
        double lat = (loc.charAt(1) - 'A') * 10.0 -  90.0;
        lon += (loc.charAt(2) - '0') * 2.0;
        lat += (loc.charAt(3) - '0') * 1.0;
        if (loc.length() >= 6) {
            lon += (loc.charAt(4) - 'A') * (2.0 / 24.0) + (1.0 / 24.0);
            lat += (loc.charAt(5) - 'A') * (1.0 / 24.0) + (0.5 / 24.0);
        } else {
            lon += 1.0;
            lat += 0.5;
        }
        return new double[]{lat, lon};
    }
}
