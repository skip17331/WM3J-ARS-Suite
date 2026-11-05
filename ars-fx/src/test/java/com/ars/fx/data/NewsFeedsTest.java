package com.ars.fx.data;

import com.ars.fx.data.FeedParser.Item;
import com.ars.fx.data.NewsFeeds.Feed;
import com.ars.fx.data.NewsFeeds.Headline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NewsFeedsTest {
    static final String RSS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/"><channel><title>x</title>
            <item><title><![CDATA[Space Station &amp; ham <b>contacts</b> &#8211; new schedule]]></title>
              <link>https://example.org/a</link><pubDate>Wed, 04 Nov 2026 12:30:00 +0000</pubDate></item>
            <item><title>Plain &amp; simple &#x2019;73&#x2019;</title><link>http://example.org/b</link><dc:date>2026-11-03T08:00:00Z</dc:date></item>
            <item><title>Bad link</title><link>javascript:alert(1)</link><pubDate>Wed, 04 Nov 2026 12:30:00 +0000</pubDate></item>
            <item><title></title><link>https://example.org/c</link></item>
            </channel></rss>""";
    static final String ATOM = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom"><title>blog</title>
            <entry><title>Atom post</title><link rel="self" href="https://example.org/self"/><link rel="alternate" href="https://example.org/post1"/>
              <published>2026-11-02T10:00:00-05:00</published><updated>2026-11-04T10:00:00Z</updated></entry>
            </feed>""";

    @AfterEach
    void reset() { FeedFetcher.setDirForTest(null); FeedFetcher.setHttpForTest(null); }

    @Test
    void parsesRssHeadlinesCleansThemAndDropsBadLinks() {
        List<Item> items = FeedParser.parse(RSS);
        assertEquals(2, items.size(), "the javascript: link and the untitled item are dropped");
        assertEquals("Space Station & ham contacts – new schedule", items.get(0).title());
        assertEquals("https://example.org/a", items.get(0).link());
        assertEquals(Instant.parse("2026-11-04T12:30:00Z"), items.get(0).published());
        assertEquals("Plain & simple ’73’", items.get(1).title());
        assertEquals(Instant.parse("2026-11-03T08:00:00Z"), items.get(1).published(), "dc:date works too");
    }

    @Test
    void nonBreakingSpacesAreJustSpaces() {
        List<Item> items = FeedParser.parse("<rss><channel><item><title>RSGB Outreach Team Chair&#160;</title><link>https://e.org/1</link></item></channel></rss>");
        assertEquals("RSGB Outreach Team Chair", items.get(0).title());
        assertEquals("a b", FeedParser.clean("a  b"));
    }

    @Test
    void parsesAtomUsingTheAlternateLink() {
        List<Item> items = FeedParser.parse(ATOM);
        assertEquals(1, items.size());
        assertEquals("https://example.org/post1", items.get(0).link());
        assertEquals(Instant.parse("2026-11-02T15:00:00Z"), items.get(0).published(), "published wins over updated");
    }

    @Test
    void hostileOrBrokenFeedsGiveNothingAndTouchNothing() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><rss><channel><item><title>&x;</title><link>https://e.org/</link></item></channel></rss>";
        assertTrue(FeedParser.parse(xxe).isEmpty(), "DOCTYPE / external entities are refused");
        assertTrue(FeedParser.parse("<html><body>not a feed</body></html>").isEmpty());
        assertTrue(FeedParser.parse("<rss><channel><item>").isEmpty());
        assertTrue(FeedParser.parse(null).isEmpty());
    }

    @Test
    void catalogIsSound() {
        Set<String> ids = new HashSet<>();
        long defaults = 0;
        for (Feed f : NewsFeeds.CATALOG) {
            assertTrue(ids.add(f.id()), "duplicate id " + f.id());
            assertTrue(f.url().startsWith("https://"), f.id() + " should be https");
            assertTrue(f.site().startsWith("http"), f.id());
            if (f.defaultOn()) defaults++;
        }
        assertEquals(7, defaults, "the societies-and-news set is on by default");
    }

    @Test
    void newestFirstWithSourceAndTextFilters() {
        Instant t = Instant.parse("2026-11-05T00:00:00Z");
        List<Headline> all = List.of(
                new Headline("ARRL", "Old news", "https://a/1", t.minusSeconds(86400 * 3)),
                new Headline("AMSAT", "New satellite launch", "https://a/2", t.minusSeconds(3600)),
                new Headline("ARRL", "Undated item", "https://a/3", null),
                new Headline("ARRL", "Newest ARRL", "https://a/4", t.minusSeconds(60)));
        assertEquals(List.of("Newest ARRL", "New satellite launch", "Old news", "Undated item"),
                NewsFeeds.latest(all, null, "", 10).stream().map(Headline::title).toList());
        assertEquals(2, NewsFeeds.latest(all, null, "", 2).size());
        assertEquals(3, NewsFeeds.latest(all, "ARRL", "", 10).size());
        assertEquals(List.of("New satellite launch"), NewsFeeds.latest(all, null, "SATELLITE", 10).stream().map(Headline::title).toList());
        assertEquals("2h", NewsFeeds.age(t.minusSeconds(7200), t));
        assertEquals("3d", NewsFeeds.age(t.minusSeconds(86400 * 3), t));
        assertEquals("", NewsFeeds.age(null, t));
    }

    @Test
    void loadMergesFeedsAndReportsTheBrokenOnes(@TempDir Path tmp) {
        FeedFetcher.setDirForTest(tmp);
        FeedFetcher.setHttpForTest(u -> {
            if (u.contains("good")) return RSS;
            if (u.contains("atom")) return ATOM;
            if (u.contains("junk")) return "<html>not a feed</html>";
            throw new RuntimeException("HTTP 403");
        });
        List<Feed> feeds = List.of(
                new Feed("a", "Good", "https://good.example/feed", "", NewsFeeds.Group.OFFICIAL, true),
                new Feed("b", "Atom", "https://atom.example/feed", "", NewsFeeds.Group.OFFICIAL, true),
                new Feed("c", "Junk", "https://junk.example/feed", "", NewsFeeds.Group.OFFICIAL, true),
                new Feed("d", "Blocked", "https://blocked.example/feed", "", NewsFeeds.Group.OFFICIAL, true));
        NewsFeeds.Result r = NewsFeeds.load(feeds, false, Instant.parse("2026-11-05T12:00:00Z"));
        assertEquals(3, r.headlines().size(), "two from the RSS, one from the Atom");
        assertTrue(r.problems().containsKey("Junk"));
        assertTrue(r.problems().get("Blocked").contains("403"));
        assertFalse(r.problems().containsKey("Good"));
    }

    @Test
    void anOverlongFeedIsCappedPerSource(@TempDir Path tmp) {
        FeedFetcher.setDirForTest(tmp);
        StringBuilder sb = new StringBuilder("<rss><channel>");
        for (int i = 0; i < 40; i++) sb.append("<item><title>t").append(i).append("</title><link>https://e.org/").append(i).append("</link></item>");
        sb.append("</channel></rss>");
        FeedFetcher.setHttpForTest(u -> sb.toString());
        NewsFeeds.Result r = NewsFeeds.load(List.of(new Feed("a", "Big", "https://big.example/f", "", NewsFeeds.Group.OFFICIAL, true)), false, Instant.now());
        assertEquals(NewsFeeds.PER_FEED, r.headlines().size());
    }
}
