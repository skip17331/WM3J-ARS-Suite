package com.ars.fx.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the headlines out of an RSS 2.0 or Atom feed: title, link and date of each item - nothing else is kept. Feeds
 * are untrusted text from the internet, so the XML parser refuses DOCTYPEs and external entities, and only http(s)
 * links are passed on.
 */
public final class FeedParser {
    private FeedParser() {}

    public record Item(String title, String link, Instant published) {}

    public static List<Item> parse(String xml) {
        List<Item> out = new ArrayList<>();
        if (xml == null || xml.isBlank()) return out;
        Document doc;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);          // no entity tricks
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());   // quiet: a bad feed is just empty
            doc = b.parse(new ByteArrayInputStream(xml.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return out;
        }
        NodeList nodes = doc.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String n = local(el);
            if (!n.equals("item") && !n.equals("entry")) continue;
            String title = clean(text(el, "title"));
            String link = n.equals("entry") ? atomLink(el) : clean(text(el, "link"));
            if (link.isEmpty()) link = clean(text(el, "guid"));
            if (title.isEmpty() || !webLink(link)) continue;
            Instant when = date(firstNonEmpty(text(el, "pubDate"), text(el, "published"), text(el, "updated"), text(el, "date")));
            out.add(new Item(title, link, when));
        }
        return out;
    }

    private static String local(Node n) { String s = n.getNodeName(); int c = s.indexOf(':'); return c >= 0 ? s.substring(c + 1) : s; }

    /** Text of the first direct child with this local name. */
    private static String text(Element parent, String name) {
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling())
            if (c instanceof Element e && local(e).equals(name)) return e.getTextContent() == null ? "" : e.getTextContent().trim();
        return "";
    }
    private static String atomLink(Element entry) {
        String fallback = "";
        for (Node c = entry.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (!(c instanceof Element e) || !local(e).equals("link")) continue;
            String href = e.getAttribute("href"), rel = e.getAttribute("rel");
            if (rel.isEmpty() || rel.equals("alternate")) return href.trim();
            if (fallback.isEmpty()) fallback = href.trim();
        }
        return fallback;
    }
    private static String firstNonEmpty(String... s) { for (String x : s) if (x != null && !x.isBlank()) return x; return ""; }

    static boolean webLink(String l) { String t = l.toLowerCase(Locale.ROOT); return t.startsWith("http://") || t.startsWith("https://"); }

    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern NUM = Pattern.compile("&#(x?)([0-9a-fA-F]+);");
    /** Headline text: tags dropped, entities decoded (feeds often double-encode them), whitespace squeezed. */
    static String clean(String s) {
        if (s == null) return "";
        String t = TAG.matcher(s).replaceAll(" ");
        Matcher m = NUM.matcher(t);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String rep;
            try {
                int cp = Integer.parseInt(m.group(2), m.group(1).isEmpty() ? 10 : 16);
                rep = Character.isValidCodePoint(cp) ? new String(Character.toChars(cp)) : "";
            } catch (NumberFormatException e) { rep = ""; }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        t = sb.toString().replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
        return t.replace(' ', ' ').replaceAll("\\s+", " ").strip();     // non-breaking spaces are just spaces
    }

    static Instant date(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        try { return ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); } catch (Exception ignored) {}
        try { return OffsetDateTime.parse(t).toInstant(); } catch (Exception ignored) {}
        try { return Instant.parse(t); } catch (Exception ignored) {}
        try { return java.time.LocalDateTime.parse(t).toInstant(java.time.ZoneOffset.UTC); } catch (Exception ignored) {}
        try { return java.time.LocalDate.parse(t.substring(0, 10)).atStartOfDay().toInstant(java.time.ZoneOffset.UTC); } catch (Exception ignored) {}
        return null;
    }
}
