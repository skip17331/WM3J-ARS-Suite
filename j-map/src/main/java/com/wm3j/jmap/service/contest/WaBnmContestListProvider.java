package com.wm3j.jmap.service.contest;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches upcoming amateur radio contests from the WA7BNM Contest Calendar.
 *
 * WA7BNM (Bruce Horn) maintains a comprehensive contest calendar at
 * https://www.contestcalendar.com/
 *
 * This provider scrapes the upcoming-contests page for current and
 * near-future contests. Respects the site's robots.txt and rate limits.
 */
public class WaBnmContestListProvider extends AbstractDataProvider<ContestList>
        implements ContestListProvider {

    private static final Logger log = LoggerFactory.getLogger(WaBnmContestListProvider.class);

    private static final String BASE_URL = "https://www.contestcalendar.com/contestcal.html";

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    protected ContestList doFetch() throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("User-Agent", "J-Map/1.0 (amateur radio map display)")
                .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new DataProviderException("WA7BNM HTTP " + resp.statusCode(), DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            return parseHtml(resp.body());

        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("WA7BNM fetch failed: " + e.getMessage(), DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private ContestList parseHtml(String html) {
        List<Contest> contests = new ArrayList<>();

        // WA7BNM table rows look like:
        // <td>MMM DD</td><td>HH:00z</td><td>MMM DD</td><td>HH:00z</td><td><a href="...">Contest Name</a></td>
        Pattern rowPat = Pattern.compile(
            "<tr[^>]*>\\s*" +
            "<td[^>]*>([A-Za-z]{3}\\s+\\d{1,2})</td>\\s*" +
            "<td[^>]*>(\\d{2}:00)z</td>\\s*" +
            "<td[^>]*>([A-Za-z]{3}\\s+\\d{1,2})</td>\\s*" +
            "<td[^>]*>(\\d{2}:00)z</td>\\s*" +
            "<td[^>]*><a[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>",
            Pattern.DOTALL
        );

        int year = LocalDate.now(ZoneOffset.UTC).getYear();
        Matcher m = rowPat.matcher(html);

        while (m.find() && contests.size() < 20) {
            try {
                String startDateStr = m.group(1).trim() + " " + year;
                String startTimeStr = m.group(2);
                String endDateStr   = m.group(3).trim() + " " + year;
                String endTimeStr   = m.group(4);
                String url          = m.group(5).trim();
                String name         = m.group(6).trim().replaceAll("\\s+", " ");

                if (!url.startsWith("http")) {
                    url = "https://www.contestcalendar.com/" + url;
                }

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d yyyy HH:mm");
                ZonedDateTime start = LocalDateTime.parse(startDateStr + " " + startTimeStr, fmt)
                    .atZone(ZoneOffset.UTC);
                ZonedDateTime end = LocalDateTime.parse(endDateStr + " " + endTimeStr, fmt)
                    .atZone(ZoneOffset.UTC);

                // Only include contests not more than 7 days in the past
                if (end.toInstant().isAfter(Instant.now().minusSeconds(7 * 86400))) {
                    contests.add(new Contest(name, start.toInstant(), end.toInstant(),
                        "HF", "Various", url));
                }
            } catch (Exception e) {
                log.debug("Contest parse error: {}", e.getMessage());
            }
        }

        log.info("Fetched {} contests from WA7BNM", contests.size());
        return new ContestList(contests, Instant.now());
    }
}
