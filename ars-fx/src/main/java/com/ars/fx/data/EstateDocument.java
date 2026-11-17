package com.ars.fx.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * J-Vault's printable estate document: one self-contained HTML page, written for the operator's family rather than for a ham, that
 * opens in the browser to be printed or saved as PDF. In order: what to do (and not do) first, the first-call contacts with their
 * phone numbers, a summary of the equipment and its estimated value, the inventory by category, anything already sold, and a ruled
 * notes area.
 *
 * <p>{@link #html} is a pure function of its inputs, so it is tested without a database. The file holds names, phone numbers and the
 * station's inventory, so {@link #save} keeps it readable by the owner only ({@link PrivateFiles}).
 */
public final class EstateDocument {
    private EstateDocument() {}

    /** Who the document is about, from Setup > Station. Blank parts are left out. */
    public record Station(String call, String name, String qth) {
        public static Station fromSettings() {
            return new Station(HubConfig.call(), HubConfig.get("station.name", "").trim(), HubConfig.get("station.qth", "").trim());
        }
    }

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    /** The document without license details (no FRN, class, memberships). */
    public static String html(Station st, List<JVaultDb.Item> items, List<JVaultDb.Contact> contacts, LocalDate date) {
        return html(st, JVaultDb.LicenseInfo.empty(), items, contacts, date);
    }

    /**
     * The document for these items and contacts, prepared on {@code date}. For a US call sign, or when an FRN is given, it adds the
     * FCC license-cancellation and ARRL Silent Key sections (procedures as ARRL describes them in November 2026; each section points to
     * the page that keeps them current).
     */
    public static String html(Station st, JVaultDb.LicenseInfo li, List<JVaultDb.Item> items, List<JVaultDb.Contact> contacts, LocalDate date) {
        if (li == null) li = JVaultDb.LicenseInfo.empty();
        List<JVaultDb.Item> kept = new ArrayList<>(), sold = new ArrayList<>();
        for (JVaultDb.Item i : items) ("sold".equalsIgnoreCase(nz(i.disposition())) ? sold : kept).add(i);
        Map<String, List<JVaultDb.Item>> byCat = new LinkedHashMap<>();           // items() is already in category order
        for (JVaultDb.Item i : kept) byCat.computeIfAbsent(blank(i.category()) ? "Other" : i.category(), k -> new ArrayList<>()).add(i);
        double total = 0; for (JVaultDb.Item i : kept) total += i.estimatedValue();

        String call = nz(st.call()).toUpperCase(Locale.ROOT);
        String who = String.join(" · ", nonBlank(call, nz(st.name()), nz(st.qth())));
        boolean us = !blank(li.frn()) || isUsCall(call);
        StringBuilder b = new StringBuilder(16_384);
        b.append("<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">\n")
         .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
         .append("<title>").append(esc(blank(call) ? "Radio station" : call)).append(" — estate information</title>\n")
         .append("<style>").append(CSS).append("</style></head><body>\n")
         .append("<button class=\"print\" onclick=\"window.print()\">Print or save as PDF</button>\n")
         .append("<header><h1>Amateur radio station — estate information</h1>\n");
        if (!who.isEmpty()) b.append("<p class=\"who\">").append(esc(who)).append("</p>\n");
        b.append("<p class=\"date\">Prepared ").append(esc(date.format(LONG_DATE))).append(" with J-Vault (WM3J ARS Suite)</p></header>\n");

        // ---- what to do first -------------------------------------------------
        b.append("<section class=\"family\"><h2>For the family: what to do first</h2><ol>\n")
         .append("<li><b>There is no rush.</b> Radio equipment keeps. Take your time before selling or giving anything away, and be wary of anyone who offers to clear the station quickly for cash.</li>\n")
         .append("<li><b>Call the people listed under First-call contacts</b>, in the order shown. They know this equipment, what it is worth and how to take it down safely.</li>\n")
         .append("<li><b>Do not climb a tower or take antennas down yourself.</b> Towers, masts and antennas are dangerous to work on. Ask a first-call contact or a professional tower crew.</li>\n")
         .append("<li><b>Switch off and unplug</b> the radios and power supplies. Large batteries store a lot of energy; leave them to someone who knows how to handle them.</li>\n")
         .append("<li><b>The values here are the owner's own estimates</b>, not appraisals. Used prices vary; a first-call contact can advise.</li>\n")
         .append("<li><b>The radio license</b>").append(blank(call) ? "" : " (" + esc(call) + ")");
        if (us) b.append(" should be canceled with the FCC, and ARRL told so the name is listed among the Silent Keys. Both are explained step by step below, under <i>Canceling the radio license</i> and <i>Reporting to ARRL</i>; a first-call contact can do them for you.</li>\n");
        else b.append(" should be reported to the licensing authority so it can be canceled. A first-call contact or the local radio club can help.</li>\n");
        b
         .append("<li><b>The station logbook</b> is kept on the computer by this program. Ask a first-call contact to export a copy before the computer is cleared or sold.</li>\n")
         .append("</ol></section>\n");

        // ---- first-call contacts ------------------------------------------------
        b.append("<section><h2>First-call contacts</h2>\n");
        if (contacts.isEmpty()) {
            b.append("<p class=\"empty\">No first-call contacts have been entered. A local amateur radio club can help with the station.</p>\n");
        } else {
            b.append("<table class=\"contacts\"><thead><tr><th>#</th><th>Name</th><th>Callsign</th><th>Relationship</th><th>Phone</th><th>E-mail</th><th>Interested in</th></tr></thead><tbody>\n");
            int n = 1;
            for (JVaultDb.Contact c : contacts)
                b.append("<tr><td>").append(n++).append("</td><td><b>").append(esc(c.name())).append("</b></td><td>").append(esc(nz(c.callsign()).toUpperCase(Locale.ROOT)))
                 .append("</td><td>").append(esc(c.relationship())).append("</td><td class=\"nowrap\">").append(esc(c.phone()))
                 .append("</td><td>").append(esc(c.email())).append("</td><td>").append(esc(c.itemsWanted())).append("</td></tr>\n");
            b.append("</tbody></table>\n");
        }
        b.append("</section>\n");

        if (us) { fccSection(b, call, nz(st.name()), li); arrlSection(b, li); }

        // ---- summary -------------------------------------------------------------
        b.append("<section><h2>Summary</h2>\n<p class=\"total\">").append(kept.size()).append(kept.size() == 1 ? " item" : " items")
         .append(", estimated total <b>").append(total > 0 ? money(total) : "not given").append("</b></p>\n");
        if (!byCat.isEmpty()) {
            b.append("<table class=\"summary\"><thead><tr><th>Category</th><th class=\"num\">Items</th><th class=\"num\">Estimated value</th></tr></thead><tbody>\n");
            for (Map.Entry<String, List<JVaultDb.Item>> e : byCat.entrySet()) {
                double v = 0; for (JVaultDb.Item i : e.getValue()) v += i.estimatedValue();
                b.append("<tr><td>").append(esc(e.getKey())).append("</td><td class=\"num\">").append(e.getValue().size())
                 .append("</td><td class=\"num\">").append(v > 0 ? money(v) : "—").append("</td></tr>\n");
            }
            b.append("</tbody></table>\n");
        }
        b.append("</section>\n");

        // ---- inventory by category ----------------------------------------------
        b.append("<section><h2>Equipment</h2>\n");
        if (byCat.isEmpty()) b.append("<p class=\"empty\">No equipment has been entered.</p>\n");
        for (Map.Entry<String, List<JVaultDb.Item>> e : byCat.entrySet()) {
            b.append("<h3>").append(esc(e.getKey())).append("</h3>\n");
            itemTable(b, e.getValue());
        }
        b.append("</section>\n");

        if (!sold.isEmpty()) {
            b.append("<section><h2>Already sold</h2><p class=\"hint\">These are no longer in the station and are not counted above.</p>\n");
            itemTable(b, sold);
            b.append("</section>\n");
        }

        b.append("<section class=\"notes\"><h2>Notes</h2>");
        for (int i = 0; i < 8; i++) b.append("<div class=\"line\"></div>");
        b.append("</section>\n<footer>").append(esc(blank(call) ? "" : call + " · ")).append("prepared ").append(esc(date.format(LONG_DATE)))
         .append(" · keep this with the will or other important papers, and print a new copy when the equipment changes.</footer>\n")
         .append("</body></html>\n");
        return b.toString();
    }

    /** Canceling a deceased licensee's FCC license, as ARRL describes it (arrl.org/cancelling-a-license, November 2026). */
    private static void fccSection(StringBuilder b, String call, String name, JVaultDb.LicenseInfo li) {
        b.append("<section class=\"steps\"><h2>Canceling the radio license (FCC)</h2>\n<table class=\"facts\"><tbody>\n");
        fact(b, "Call sign", blank(call) ? "not recorded" : call);
        if (!blank(name)) fact(b, "Licensee", name);
        fact(b, "FCC Registration Number (FRN)", blank(li.frn()) ? "not recorded (the FCC's license search shows it with the call sign)" : li.frn());
        if (!blank(li.licenseClass())) fact(b, "License class", li.licenseClass());
        if (!blank(li.expires())) fact(b, "License expires", li.expires());
        if (!blank(li.passwordWhere())) fact(b, "FCC login (CORES / ULS) password is kept", li.passwordWhere());
        b.append("</tbody></table>\n<ol>\n")
         .append("<li><b>Write a short request</b> giving the licensee's name and call sign").append(blank(li.frn()) ? "" : ", the FRN above,")
         .append(" and why you are asking, for example <i>\"He was my father; he passed away on 1/1/2026.\"</i> <b>Print it and sign it by hand</b>, then scan it: the FCC needs a request that has been physically signed.</li>\n")
         .append("<li><b>Proof of death:</b> a copy of the <b>death certificate</b>, or the <b>complete</b> newspaper obituary that shows the date of death. A link to an obituary on the web is not enough. Scan it too.</li>\n")
         .append("<li>On the FCC's Support Services page, ").append(link("https://www.fcc.gov/wireless/available-support-services", "fcc.gov/wireless/available-support-services"))
         .append(", choose <b>Submit a help request</b>, fill in the form and attach the signed request and the proof. The FCC does not accept requests by mail or fax.</li>\n")
         .append("<li><b>Allow one to two weeks.</b> Then look up the call sign in the FCC's license search (ULS, ")
         .append(link("https://www.fcc.gov/wireless/systems-utilities/universal-licensing-system", "fcc.gov/wireless/systems-utilities/universal-licensing-system"))
         .append("): its status will read <b>CANCELED</b>, effective from the date of death.</li>\n</ol>\n")
         .append("<p class=\"hint\">Questions: FCC Licensing Support Center, 877-480-3201 (Monday to Friday, 8 am to 6 pm Eastern), or ")
         .append(link("https://www.fcc.gov/wtbhelp", "fcc.gov/wtbhelp"))
         .append(". The FCC changes its procedures from time to time; ARRL keeps current instructions at ")
         .append(link("https://www.arrl.org/cancelling-a-license", "arrl.org/cancelling-a-license"))
         .append(". A close relative may later be able to apply to keep the call sign; the same page explains how.</p>\n</section>\n");
    }

    /** Reporting a Silent Key to ARRL, as ARRL describes it (arrl.org/silent-key-submission-guidelines, November 2026). */
    private static void arrlSection(StringBuilder b, JVaultDb.LicenseInfo li) {
        b.append("<section class=\"steps\"><h2>Reporting to ARRL (Silent Key)</h2>\n");
        if (!blank(li.arrlMember()) || !blank(li.clubs())) {
            b.append("<table class=\"facts\"><tbody>\n");
            if (!blank(li.arrlMember())) fact(b, "ARRL member number", li.arrlMember());
            if (!blank(li.clubs())) fact(b, "Other clubs to tell", li.clubs());
            b.append("</tbody></table>\n");
        }
        b.append("<ol>\n<li><b>E-mail sk@arrl.org</b> with the name, call sign and date of death")
         .append(blank(li.arrlMember()) ? "" : " and the member number above")
         .append(". Or fill in ARRL's <b>Report a Silent Key</b> form (")
         .append(link("https://www.arrl.org/silent-key-submission-guidelines", "arrl.org/silent-key-submission-guidelines"))
         .append(") and e-mail it, fax it to 1-860-594-0303, or mail it to ARRL, 225 Main Street, Newington, CT 06111-1400.</li>\n")
         .append("<li><b>Include one proof of death:</b> an obituary notice that shows the date of death, a copy of the death certificate, or a letter from the family attorney or the estate executor.</li>\n")
         .append("<li><b>Anyone may send it:</b> family, a friend or another amateur, so a first-call contact can do it for you.</li>\n");
        if (!blank(li.clubs())) b.append("<li>Let the clubs listed above know as well.</li>\n");
        b.append("</ol>\n<p class=\"hint\">The name appears in the Silent Keys column of ARRL's magazine, QST, after four to five months. Reporting to ARRL does <b>not</b> cancel the FCC license; that is the separate step above. Questions: ARRL, 1-860-594-0200 (toll-free 1-888-277-5289), sk@arrl.org.</p>\n</section>\n");
    }

    private static void fact(StringBuilder b, String k, String v) {
        b.append("<tr><th>").append(esc(k)).append("</th><td>").append(esc(v)).append("</td></tr>\n");
    }
    /** A link that still reads on paper: the visible text is the address. */
    private static String link(String href, String text) { return "<a href=\"" + esc(href) + "\">" + esc(text) + "</a>"; }

    /** A US amateur call (K, N, W or AA-AL prefix, one digit, 1-3 letter suffix), ignoring a portable / suffix. */
    static boolean isUsCall(String call) {
        if (blank(call)) return false;
        String c = call.trim().toUpperCase(Locale.ROOT);
        int slash = c.indexOf('/');
        if (slash > 0) c = c.substring(0, slash);
        return c.matches("(?:[KNW][A-Z]{0,2}|A[A-L])[0-9][A-Z]{1,3}");
    }

    private static void itemTable(StringBuilder b, List<JVaultDb.Item> items) {
        b.append("<table class=\"items\"><thead><tr><th>Item</th><th>Serial no.</th><th>Acquired</th><th class=\"num\">Paid</th>")
         .append("<th class=\"num\">Est. value</th><th>Condition</th><th>Where</th><th>Notes</th></tr></thead><tbody>\n");
        for (JVaultDb.Item i : items) {
            String where = String.join(", ", nonBlank(nz(i.installStatus()), nz(i.location())));
            b.append("<tr><td><b>").append(esc(blank(i.title()) ? "(unnamed)" : i.title())).append("</b></td><td>").append(esc(i.serial()))
             .append("</td><td class=\"nowrap\">").append(esc(i.dateAcquired())).append("</td><td class=\"num\">").append(i.purchasePrice() > 0 ? money(i.purchasePrice()) : "")
             .append("</td><td class=\"num\">").append(i.estimatedValue() > 0 ? money(i.estimatedValue()) : "").append("</td><td>").append(esc(i.disposition()))
             .append("</td><td>").append(esc(where)).append("</td><td>").append(esc(i.notes())).append("</td></tr>\n");
        }
        b.append("</tbody></table>\n");
    }

    /** {@code ~/.j-vault/estate/}, where the documents are saved. */
    public static Path folder() { return Paths.get(System.getProperty("user.home"), ".j-vault", "estate"); }

    /** Write the document into {@code dir} as {@code estate-<call>-<date-time>.html}, owner-only, and return its path. */
    public static Path save(Path dir, String call, String html, LocalDateTime now) throws java.io.IOException {
        Files.createDirectories(dir);
        String c = nz(call).trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("^-|-$", "");
        Path f = dir.resolve("estate-" + (c.isEmpty() ? "" : c + "-") + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".html");
        Files.writeString(f, html);
        PrivateFiles.restrict(f);                     // names, phone numbers and the inventory
        return f;
    }

    private static final String CSS = """
        :root { color-scheme: light; }
        body { font-family: Arial, Helvetica, sans-serif; color: #111; background: #fff; max-width: 980px; margin: 24px auto; padding: 0 16px; line-height: 1.4; }
        h1, h2, h3 { font-family: Georgia, "Times New Roman", serif; }
        h1 { font-size: 26px; margin: 0 0 4px; }
        h2 { font-size: 19px; border-bottom: 2px solid #111; padding-bottom: 3px; margin: 26px 0 10px; page-break-after: avoid; }
        h3 { font-size: 15px; margin: 18px 0 6px; page-break-after: avoid; }
        .who { font-size: 17px; margin: 2px 0; } .date, .hint, footer { color: #555; font-size: 13px; }
        .family { border: 2px solid #111; padding: 4px 16px 8px; margin-top: 18px; } .family li { margin: 5px 0; }
        table { width: 100%; border-collapse: collapse; font-size: 12.5px; }
        .items { table-layout: fixed; } .items td { overflow-wrap: anywhere; }      /* every category's table lines up */
        .items th:nth-child(1) { width: 19%; } .items th:nth-child(2) { width: 11%; } .items th:nth-child(3) { width: 10%; }
        .items th:nth-child(4) { width: 8%; }  .items th:nth-child(5) { width: 9%; }  .items th:nth-child(6) { width: 10%; }
        .items th:nth-child(7) { width: 15%; } .items th:nth-child(8) { width: 18%; }
        th, td { border: 1px solid #999; padding: 4px 6px; text-align: left; vertical-align: top; }
        th { background: #eee; } thead { display: table-header-group; } tr { page-break-inside: avoid; }
        .num { text-align: right; white-space: nowrap; } .nowrap { white-space: nowrap; }
        .contacts td { font-size: 13.5px; } .summary { width: auto; min-width: 50%; } .total { font-size: 15px; }
        .empty { font-style: italic; }
        .notes .line { border-bottom: 1px solid #999; height: 30px; }
        .facts { width: auto; min-width: 60%; margin-bottom: 8px; } .facts th { width: 260px; background: #f4f4f4; } .facts td { font-size: 14px; font-weight: bold; }
        .steps li { margin: 6px 0; } a { color: #111; }
        footer { margin-top: 24px; border-top: 1px solid #999; padding-top: 6px; }
        .print { float: right; font: 14px Arial, sans-serif; padding: 7px 14px; cursor: pointer; }
        @page { margin: 15mm; }
        @media print { body { margin: 0; max-width: none; } .print { display: none; } }
        """;

    private static String money(double v) { return "$" + String.format(Locale.US, "%,.0f", v); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String nz(String s) { return s == null ? "" : s.trim(); }
    private static List<String> nonBlank(String... parts) { List<String> out = new ArrayList<>(); for (String p : parts) if (!blank(p)) out.add(p); return out; }
    static String esc(String s) {
        if (s == null) return "";
        StringBuilder o = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) switch (ch) {
            case '&' -> o.append("&amp;"); case '<' -> o.append("&lt;"); case '>' -> o.append("&gt;");
            case '"' -> o.append("&quot;"); case '\'' -> o.append("&#39;"); default -> o.append(ch);
        }
        return o.toString();
    }
}
