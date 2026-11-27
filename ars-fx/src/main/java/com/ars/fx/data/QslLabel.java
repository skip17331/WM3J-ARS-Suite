package com.ars.fx.data;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

/**
 * QSL label printing for the J-Log "Print QSL label" switch. Two transports:
 * <ul>
 *   <li><b>driver</b> — draws the label with Java 2D and prints it through the operating system's printer driver
 *       (CUPS on Linux/macOS, the Windows spooler) on a custom page the size of the label stock. Works for any
 *       printer that has a driver: Brother QL, DYMO LabelWriter, Zebra, Rollo, ...</li>
 *   <li><b>zpl</b> — Zebra printers only: sends native ZPL II, either to a network printer (host:9100) or as a raw job
 *       to a queue ({@code lp -o raw}). Crisp text and no driver rasterising.</li>
 * </ul>
 * Settings live in {@link HubConfig} under {@code label.*} and are edited in J-Hub ▸ Setup ▸ Label printers.
 */
public final class QslLabel {
    private QslLabel() {}

    // ---- printer models & label stock -------------------------------------------------------------

    /** Label stock: {@code wMm} × {@code hMm} is the physical label, short or long edge as the printer feeds it. */
    public record Size(String id, String label, double wMm, double hMm) {}
    /** A printer model. {@code zpl} = also speaks ZPL II natively; {@code sizes} = stock it ships/sells for. */
    public record Model(String id, String maker, String label, boolean zpl, int dpi, List<Size> sizes) {}

    private static Size in(String id, String label, double wIn, double hIn) {
        return new Size(id, label, wIn * 25.4, hIn * 25.4);
    }

    private static final List<Size> BROTHER = List.of(
            new Size("dk11209", "62 × 29 mm — DK-11209 small address", 62, 29),
            new Size("dk11201", "29 × 90 mm — DK-11201 standard address", 29, 90),
            new Size("dk11208", "38 × 90 mm — DK-11208 large address", 38, 90),
            new Size("dk11202", "62 × 100 mm — DK-11202 shipping", 62, 100),
            new Size("dk11204", "17 × 54 mm — DK-11204 multi-purpose", 17, 54),
            new Size("dk22205", "62 × 40 mm — DK-22205 continuous (cut to length)", 62, 40));
    private static final List<Size> DYMO = List.of(
            new Size("30252", "28 × 89 mm — 30252 address", 28, 89),
            new Size("30321", "36 × 89 mm — 30321 large address", 36, 89),
            new Size("30336", "25 × 54 mm — 30336 small multi-purpose", 25, 54),
            new Size("30334", "32 × 57 mm — 30334 medium multi-purpose", 32, 57),
            new Size("30256", "59 × 102 mm — 30256 shipping", 59, 102));
    private static final List<Size> DYMO_4XL = List.of(
            new Size("1744907", "104 × 159 mm — 4×6 in shipping", 104, 159),
            new Size("30256", "59 × 102 mm — 30256 shipping", 59, 102),
            new Size("30252", "28 × 89 mm — 30252 address", 28, 89));
    private static final List<Size> THERMAL = List.of(
            in("4x6",    "4 × 6 in", 4, 6),
            in("4x3",    "4 × 3 in", 4, 3),
            in("4x2",    "4 × 2 in", 4, 2),
            in("3x2",    "3 × 2 in", 3, 2),
            in("3x1",    "3 × 1 in", 3, 1),
            in("2.25x1.25", "2.25 × 1.25 in", 2.25, 1.25),
            in("2x1",    "2 × 1 in", 2, 1));

    public static final Size CUSTOM = new Size("custom", "Custom size (set below)", 0, 0);

    public static final List<Model> MODELS = List.of(
            new Model("brother-ql810",    "Brother", "Brother QL-810",       false, 300, BROTHER),
            new Model("brother-ql810w",   "Brother", "Brother QL-810W",      false, 300, BROTHER),
            new Model("brother-ql800",    "Brother", "Brother QL-800",       false, 300, BROTHER),
            new Model("brother-ql820nwb", "Brother", "Brother QL-820NWB",    false, 300, BROTHER),
            new Model("brother-ql700",    "Brother", "Brother QL-700",       false, 300, BROTHER),
            new Model("dymo-lw550",       "DYMO",    "DYMO LabelWriter 550", false, 300, DYMO),
            new Model("dymo-lw550t",      "DYMO",    "DYMO LabelWriter 550 Turbo", false, 300, DYMO),
            new Model("dymo-lw5xl",       "DYMO",    "DYMO LabelWriter 5XL", false, 300, DYMO_4XL),
            new Model("dymo-lw450",       "DYMO",    "DYMO LabelWriter 450", false, 300, DYMO),
            new Model("zebra-zd421",      "Zebra",   "Zebra ZD421",          true,  203, THERMAL),
            new Model("zebra-zd411",      "Zebra",   "Zebra ZD411",          true,  203, THERMAL),
            new Model("zebra-zd621",      "Zebra",   "Zebra ZD621",          true,  203, THERMAL),
            new Model("zebra-zd220",      "Zebra",   "Zebra ZD220",          true,  203, THERMAL),
            new Model("zebra-gk420d",     "Zebra",   "Zebra GK420d / GX420d", true, 203, THERMAL),
            new Model("rollo-x1040",      "Rollo",   "Rollo X1040 / Rollo Wireless", false, 203, THERMAL),
            new Model("generic",          "Other",   "Other label printer (system driver)", false, 300, THERMAL));

    public static Model model(String id) {
        for (Model m : MODELS) if (m.id().equals(id)) return m;
        return MODELS.get(0);
    }
    /** The model's sizes plus "Custom". */
    public static List<Size> sizesFor(Model m) {
        List<Size> l = new ArrayList<>(m.sizes()); l.add(CUSTOM); return l;
    }

    // ---- settings ---------------------------------------------------------------------------------

    public static final String KEY_PRINT_ON_SAVE = "label.printOnSave";
    public static boolean printOnSave()          { return HubConfig.getBool(KEY_PRINT_ON_SAVE, false); }
    public static void setPrintOnSave(boolean b) { HubConfig.setBool(KEY_PRINT_ON_SAVE, b); }

    public static Model currentModel() { return model(HubConfig.get("label.model", MODELS.get(0).id())); }
    /** "zpl" only when the model supports it AND the operator chose it; otherwise "driver". */
    public static String mode(Model m) {
        return m.zpl() && "zpl".equals(HubConfig.get("label.mode", m.zpl() ? "zpl" : "driver")) ? "zpl" : "driver";
    }
    public static Size currentSize(Model m) {
        String id = HubConfig.get("label.size", "");
        if (CUSTOM.id().equals(id)) {
            double w = num(HubConfig.get("label.customW", "62")), h = num(HubConfig.get("label.customH", "29"));
            return new Size("custom", "Custom", w > 0 ? w : 62, h > 0 ? h : 29);
        }
        for (Size s : m.sizes()) if (s.id().equals(id)) return s;
        return m.sizes().get(0);
    }
    private static double num(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; } }

    /** Names of the printers the OS knows about (empty if none / headless). */
    public static List<String> systemPrinters() {
        List<String> out = new ArrayList<>();
        try { for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) out.add(ps.getName()); }
        catch (Throwable ignored) {}
        return out;
    }

    // ---- the label itself -------------------------------------------------------------------------

    /** What goes on the label. */
    public record Qso(String call, String dateTimeUtc, String freq, String band, String mode,
                      String rstSent, String rstRcvd, String name, String qth, String exchange) {
        /** A normal-log QSO: no contest exchange. */
        public Qso(String call, String dateTimeUtc, String freq, String band, String mode,
                   String rstSent, String rstRcvd, String name, String qth) {
            this(call, dateTimeUtc, freq, band, mode, rstSent, rstRcvd, name, qth, "");
        }
    }

    /** A normal-log row (the log browser) as label content, for a reprint. */
    public static Qso fromFull(JLogDb.QsoFull q) {
        return new Qso(q.call(), q.datetimeUtc(), q.freq(), q.band(), q.mode(), q.rstSent(), q.rstRcvd(), q.name(), q.qth());
    }

    /** One printed line: its text, its share of the label height, and whether it is bold. */
    public record Line(String text, double weight, boolean bold, List<String> cells, double[] cols, boolean right, double scale) {
        /** A plain centred line of text. */
        public Line(String text, double weight, boolean bold) { this(text, weight, bold, null, null, false, 1.0); }
        /** A line set at {@code scale} times its fitted size and pushed to the right edge. */
        public static Line corner(String text, double weight, double scale) { return new Line(text, weight, true, null, null, true, scale); }
        /** A row of boxed cells (the QSL table); {@code cols} are the cells' shares of the width, summing to 1, shared by every row of the table. */
        public boolean isRow() { return cells != null; }
    }
    private static Line row(List<String> cells, double[] cols, double weight, boolean bold) {
        return new Line(String.join(" | ", cells), weight, bold, cells, cols, false, 1.0);
    }
    /** Column shares for a table: each as wide as its longer entry (header or value) plus room around it. */
    private static double[] columnShares(List<String> head, List<String> vals) {
        double[] w = new double[head.size()]; double sum = 0;
        for (int i = 0; i < w.length; i++) { w[i] = Math.max(head.get(i).length(), vals.get(i).length()) + 2.5; sum += w[i]; }
        for (int i = 0; i < w.length; i++) w[i] /= sum;
        return w;
    }

    private static String s(String v) { return v == null ? "" : v.trim(); }

    /** The QSL label's optional parts: id, checkbox text, and whether it is on by default. The call is always printed. */
    public record Field(String id, String label, boolean on) {}
    public static final List<Field> QSL_FIELDS = List.of(
            new Field("date", "Date", true), new Field("time", "UTC time", true), new Field("band", "Band", true),
            new Field("freq", "Frequency (MHz)", false), new Field("mode", "Mode", true),
            new Field("rst", "RST", true), new Field("sign", "\"73 and Good DX!\"", true));
    public static boolean show(String id) {
        for (Field f : QSL_FIELDS) if (f.id().equals(id)) return HubConfig.getBool("label.qsl." + id, f.on());
        return false;
    }
    /** Which labels a save prints: the QSL confirmation and / or the address label (from QRZ.com). */
    public static boolean printQsl()     { return HubConfig.getBool("label.print.qsl", true); }
    public static boolean printAddress() { return HubConfig.getBool("label.print.address", false); }

    /** The QSL label: a boxed table (To callsign | Date | UTC | Band | Mode | RST), the QSO under it, then "73 and Good DX!". Columns follow the Setup ticks. */
    public static List<Line> layout(Qso q) {
        String dt = s(q.dateTimeUtc());
        if (dt.isEmpty()) dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(Instant.now());
        String date = dt.length() >= 10 ? dt.substring(0, 10) : dt, time = dt.length() >= 16 ? dt.substring(11, 16) + " Z" : "";
        List<String> head = new ArrayList<>(), vals = new ArrayList<>();
        head.add("To callsign"); vals.add(s(q.call()).toUpperCase(Locale.ROOT));
        if (show("date")) { head.add("Date");  vals.add(date); }
        if (show("time")) { head.add("UTC");   vals.add(time); }
        if (show("freq")) { head.add("MHz");   vals.add(s(q.freq())); }
        if (show("band")) { head.add("Band");  vals.add(s(q.band()).toUpperCase(Locale.ROOT)); }
        if (show("mode")) { head.add("Mode");  vals.add(s(q.mode()).toUpperCase(Locale.ROOT)); }
        if (show("rst"))  { head.add("RST");   vals.add(s(q.rstSent())); }
        double[] cols = columnShares(head, vals);
        List<Line> l = new ArrayList<>();
        l.add(row(head, cols, 1.0, false));
        l.add(row(vals, cols, 1.7, true));
        if (show("sign")) l.add(Line.corner("73 and Good DX!", 1.0, 0.5));    // half size, lower right corner
        return l;
    }

    /** The address label: line 1 the name, line 2 "ARS <callsign>", then the mailing address (street, then city / state / zip, and the country when not the US). */
    public static List<Line> addressLayout(CallsignLookup.Address a) {
        List<Line> l = new ArrayList<>();
        l.add(new Line(s(a.name()).isEmpty() ? s(a.call()).toUpperCase(Locale.ROOT) : s(a.name()), 1.4, true));
        l.add(new Line("ARS " + s(a.call()).toUpperCase(Locale.ROOT), 1.4, true));
        if (!s(a.line1()).isEmpty()) l.add(new Line(s(a.line1()), 1.2, false));
        String csz = (s(a.line2()) + (s(a.state()).isEmpty() ? "" : (s(a.line2()).isEmpty() ? "" : ", ") + s(a.state())) + (s(a.zip()).isEmpty() ? "" : " " + s(a.zip()))).trim();
        if (!csz.isEmpty()) l.add(new Line(csz, 1.2, false));
        String country = s(a.country());
        if (!country.isEmpty() && !country.equalsIgnoreCase("United States")) l.add(new Line(country.toUpperCase(Locale.ROOT), 1.2, true));
        return l;
    }

    // ---- entry point ------------------------------------------------------------------------------

    /** Result of a print attempt: ok + a one-line message for the operator. */
    public record Result(boolean ok, String message) {}

    /** Prints on a background thread and reports back on that thread via {@code done} (hop to the FX thread there). */
    public static void printAsync(Qso q, java.util.function.Consumer<Result> done) {
        Thread t = new Thread(() -> {
            Result r;
            try { r = print(q); } catch (Throwable e) { r = new Result(false, "Label not printed: " + e.getMessage()); }
            if (done != null) done.accept(r);
        }, "qsl-label");
        t.setDaemon(true); t.start();
    }

    /** Prints the label(s) chosen in Setup for this QSO (blocking): the QSL confirmation, and / or an address label looked up on QRZ.com. */
    public static Result print(Qso q) throws Exception {
        boolean qsl = printQsl(), addr = printAddress();
        if (!qsl && !addr) return new Result(false, "No label type is ticked in Setup > Label printers");
        List<String> msg = new ArrayList<>();
        boolean ok = true;
        if (qsl) { Result r = printLines("QSL label " + s(q.call()), layout(q)); ok &= r.ok(); msg.add(r.message()); }
        if (addr) {
            CallsignLookup.Address a = CallsignLookup.qrzAddress(q.call());
            if (a == null) { ok = false; msg.add("No address label: " + CallsignLookup.qrzAddressError); }
            else { Result r = printLines("Address label " + s(q.call()), addressLayout(a)); ok &= r.ok(); msg.add(r.message()); }
        }
        return new Result(ok, String.join("; ", msg));
    }

    /** Prints one label made of these lines with the saved printer settings. */
    private static Result printLines(String jobName, List<Line> lines) throws Exception {
        Model m = currentModel();
        Size sz = currentSize(m);
        String printer = HubConfig.get("label.printer", "").trim();
        int copies = Math.max(1, Math.min(20, (int) num(HubConfig.get("label.copies", "1"))));
        if ("zpl".equals(mode(m))) {
            String host = HubConfig.get("label.host", "").trim();
            if (host.isEmpty() && printer.isEmpty()) return new Result(false, "No label printer set — choose one in Setup ▸ Label printers");
            int dpi = (int) num(HubConfig.get("label.dpi", String.valueOf(m.dpi())));
            sendZpl(zpl(lines, sz, dpi > 0 ? dpi : 203, copies), host, printer);
            return new Result(true, "Label sent to " + (host.isEmpty() ? printer : host));
        }
        if (printer.isEmpty()) return new Result(false, "No label printer set — choose one in Setup ▸ Label printers");
        printDriver(jobName, lines, sz, printer, copies);
        return new Result(true, "Label sent to " + printer);
    }

    /** Sample label for the Setup "Test print" button. */
    public static Qso sample() {
        return new Qso("K1ABC", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(Instant.now()),
                "14.074", "20m", "FT8", "-10", "-12", "Sample", "Baltimore MD");
    }

    /** Prints sample labels of the types ticked in Setup (the address label uses a made-up address, so no QRZ.com lookup is made). */
    public static Result printTest() throws Exception {
        boolean qsl = printQsl(), addr = printAddress();
        if (!qsl && !addr) return new Result(false, "No label type is ticked");
        List<String> msg = new ArrayList<>(); boolean ok = true;
        if (qsl) { Result r = printLines("QSL label test", layout(sample())); ok &= r.ok(); msg.add(r.message()); }
        if (addr) { Result r = printLines("Address label test", addressLayout(sampleAddress())); ok &= r.ok(); msg.add(r.message()); }
        return new Result(ok, String.join("; ", msg));
    }
    public static void printTestAsync(java.util.function.Consumer<Result> done) {
        Thread t = new Thread(() -> {
            Result r;
            try { r = printTest(); } catch (Throwable e) { r = new Result(false, "Label not printed: " + e.getMessage()); }
            if (done != null) done.accept(r);
        }, "qsl-label-test");
        t.setDaemon(true); t.start();
    }

    /** Sample address for the preview and the test print. */
    public static CallsignLookup.Address sampleAddress() {
        return new CallsignLookup.Address("K1ABC", "Pat Sample", "123 Main Street", "Baltimore", "MD", "21201", "United States");
    }

    // ---- driver path (Java 2D) --------------------------------------------------------------------

    /** True when the stock is a narrow strip: the text then runs along its long edge (the page is fed short edge across). */
    public static boolean isStrip(Size sz) {
        double shortMm = Math.min(sz.wMm(), sz.hMm()), longMm = Math.max(sz.wMm(), sz.hMm());
        return longMm > shortMm * 1.5;
    }

    private static final double PT_PER_MM = 72.0 / 25.4;

    private static void printDriver(String jobName, List<Line> lines, Size sz, String printerName, int copies) throws Exception {
        PrintService svc = null;
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null))
            if (ps.getName().equals(printerName)) { svc = ps; break; }
        if (svc == null) throw new IllegalStateException("printer \"" + printerName + "\" not found");

        // Page = the label stock, short edge across: roll drivers expect portrait pages.
        double shortMm = Math.min(sz.wMm(), sz.hMm()), longMm = Math.max(sz.wMm(), sz.hMm());
        boolean strip = longMm > shortMm * 1.5;                 // text runs along the long edge of narrow strips
        double pageW = (strip ? shortMm : sz.wMm()) * PT_PER_MM, pageH = (strip ? longMm : sz.hMm()) * PT_PER_MM;
        Paper paper = new Paper();
        paper.setSize(pageW, pageH);
        paper.setImageableArea(0, 0, pageW, pageH);
        PageFormat pf = new PageFormat();
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);

        Printable body = (g, fmt, idx) -> {
            if (idx > 0) return Printable.NO_SUCH_PAGE;
            draw((Graphics2D) g, lines, pageW, pageH, strip);
            return Printable.PAGE_EXISTS;
        };
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setPrintService(svc);
        job.setJobName(jobName);
        job.setPrintable(body, pf);
        job.setCopies(copies);
        try { job.print(); } catch (PrinterException e) { throw new Exception(e.getMessage(), e); }
    }

    /** Draws the lines top-to-bottom in the page, rotated 90° first for narrow strip labels. */
    private static void draw(Graphics2D g, List<Line> lines, double pageW, double pageH, boolean rotate) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        AffineTransform saved = g.getTransform();
        double w = pageW, h = pageH;
        if (rotate) { g.translate(pageW, 0); g.rotate(Math.PI / 2); w = pageH; h = pageW; }   // new x runs down the page
        double pad = 2.5 * PT_PER_MM;
        double usableW = w - 2 * pad, usableH = h - 2 * pad;
        double total = 0; for (Line l : lines) total += l.weight();
        g.setColor(Color.BLACK);
        double y = pad;
        for (Line l : lines) {
            double band = usableH * l.weight() / total;
            if (l.isRow()) {                                   // a boxed table row: each cell outlined, its text centred in it, one font size across the row
                double x = pad;
                float size = (float) (band * 0.7);
                for (int i = 0; i < l.cells().size(); i++)
                    size = Math.min(size, fit(g, new Line(l.cells().get(i), 0, l.bold()), l.cols()[i] * usableW - 4, band * 0.7).getSize2D());
                Font rf = new Font(Font.SANS_SERIF, l.bold() ? Font.BOLD : Font.PLAIN, 100).deriveFont(size);
                g.setFont(rf); FontMetrics rm = g.getFontMetrics();
                g.setStroke(new java.awt.BasicStroke(1f));
                for (int i = 0; i < l.cells().size(); i++) {
                    double cw = l.cols()[i] * usableW;
                    g.draw(new java.awt.geom.Rectangle2D.Double(x, y, cw, band));
                    String t = l.cells().get(i);
                    g.drawString(t, (float) (x + (cw - rm.stringWidth(t)) / 2), (float) (y + (band - rm.getHeight()) / 2 + rm.getAscent()));
                    x += cw;
                }
                y += band;
                continue;
            }
            Font f = fit(g, l, usableW, band * 0.82);
            if (l.scale() != 1.0) f = f.deriveFont((float) (f.getSize2D() * l.scale()));
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            double baseline = y + (band - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(l.text(), (float) (l.right() ? pad + usableW - fm.stringWidth(l.text()) : pad + (usableW - fm.stringWidth(l.text())) / 2), (float) baseline);
            y += band;
        }
        g.setTransform(saved);
    }

    private static Font fit(Graphics g, Line l, double maxW, double maxH) {
        int style = l.bold() ? Font.BOLD : Font.PLAIN;
        float size = (float) Math.max(5, maxH);
        Font f = new Font(Font.SANS_SERIF, style, 100).deriveFont(size);
        int w = g.getFontMetrics(f).stringWidth(l.text());
        if (w > maxW && w > 0) f = f.deriveFont((float) Math.max(5, size * maxW / w));
        return f;
    }

    // ---- ZPL path ---------------------------------------------------------------------------------

    static String zpl(Qso q, Size sz, int dpi, int copies) { return zpl(layout(q), sz, dpi, copies); }

    static String zpl(List<Line> lines, Size sz, int dpi, int copies) {
        double dotsPerMm = dpi / 25.4;
        int w = (int) Math.round(sz.wMm() * dotsPerMm), h = (int) Math.round(sz.hMm() * dotsPerMm);
        int pad = (int) Math.round(2.5 * dotsPerMm);
        int usableW = w - 2 * pad, usableH = h - 2 * pad;
        double total = 0; for (Line l : lines) total += l.weight();
        StringBuilder z = new StringBuilder("^XA^CI28^PW").append(w).append("^LL").append(h).append("^LH0,0\n");
        double y = pad;
        for (Line l : lines) {
            double band = usableH * l.weight() / total;
            if (l.isRow()) {                                   // boxed cells, text centred in each, one font size across the row
                int fh = (int) (band * 0.7);
                for (int i = 0; i < l.cells().size(); i++)
                    fh = (int) Math.min(fh, l.cols()[i] * usableW / (Math.max(1, l.cells().get(i).length()) * (l.bold() ? 0.68 : 0.6)));
                fh = Math.max(12, fh);
                double x = pad;
                for (int i = 0; i < l.cells().size(); i++) {
                    int cw = (int) Math.round(l.cols()[i] * usableW);
                    z.append("^FO").append((int) x).append(',').append((int) y).append("^GB").append(cw).append(',').append((int) band).append(",1^FS\n")
                     .append("^FO").append((int) x).append(',').append((int) (y + (band - fh) / 2))
                     .append("^FB").append(cw).append(",1,0,C,0^A0N,").append(fh).append(',').append(fh)
                     .append("^FD").append(zplEscape(l.cells().get(i))).append("^FS\n");
                    x += cw;
                }
                y += band;
                continue;
            }
            String txt = zplEscape(l.text());
            // ZPL font 0 is proportional; ~0.6 em per character is a safe fit estimate
            int fh = (int) Math.max(12, Math.min(band * 0.82, usableW / (Math.max(1, txt.length()) * 0.6)) * l.scale());
            int top = (int) (y + (band - fh) / 2);
            z.append("^FO").append(pad).append(',').append(top)
             .append("^FB").append(usableW).append(",1,0,").append(l.right() ? "R" : "C").append(",0")                 // centred in the usable width
             .append("^A0N,").append(fh).append(',').append(fh)
             .append("^FD").append(txt).append("^FS\n");
            y += band;
        }
        z.append("^PQ").append(copies).append("^XZ\n");
        return z.toString();
    }

    private static String zplEscape(String t) {
        return t.replace("^", " ").replace("~", " ").replace("\\", " ");     // ZPL control characters
    }

    private static void sendZpl(String zpl, String host, String queue) throws Exception {
        byte[] data = zpl.getBytes(StandardCharsets.UTF_8);
        if (!host.isEmpty()) {
            String hostName = host; int port = 9100;
            int c = host.lastIndexOf(':');
            if (c > 0 && c < host.length() - 1) { try { port = Integer.parseInt(host.substring(c + 1)); hostName = host.substring(0, c); } catch (NumberFormatException ignored) {} }
            try (Socket sock = new Socket()) {
                sock.connect(new InetSocketAddress(hostName, port), 4000);
                OutputStream os = sock.getOutputStream();
                os.write(data); os.flush();
            }
            return;
        }
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (!windows) {
            Process p = new ProcessBuilder("lp", "-d", queue, "-o", "raw").redirectErrorStream(true).start();
            try (OutputStream os = p.getOutputStream()) { os.write(data); }
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (p.waitFor() != 0) throw new IllegalStateException("lp failed: " + out);
            return;
        }
        PrintService svc = null;
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null))
            if (ps.getName().equals(queue)) { svc = ps; break; }
        if (svc == null) throw new IllegalStateException("printer \"" + queue + "\" not found");
        javax.print.DocPrintJob job = svc.createPrintJob();
        job.print(new javax.print.SimpleDoc(data, javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE, null), null);
    }
}
