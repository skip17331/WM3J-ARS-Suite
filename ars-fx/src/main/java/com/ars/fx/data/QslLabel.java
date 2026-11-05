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

    public static boolean printOnSave()          { return HubConfig.getBool("label.printOnSave", false); }
    public static void setPrintOnSave(boolean b) { HubConfig.setBool("label.printOnSave", b); }

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
                      String rstSent, String rstRcvd, String name, String qth) {}

    private record Line(String text, double weight, boolean bold) {}

    private static String s(String v) { return v == null ? "" : v.trim(); }

    private static List<Line> layout(Qso q) {
        String dt = s(q.dateTimeUtc());
        if (dt.isEmpty()) dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(Instant.now());
        String when = (dt.length() >= 16 ? dt.substring(0, 16) : dt) + "Z";
        String fb = (s(q.freq()).isEmpty() ? "" : s(q.freq()) + " MHz  ") + (s(q.band()).isEmpty() ? "" : s(q.band()) + "  ") + s(q.mode());
        String rst = (s(q.rstSent()).isEmpty() && s(q.rstRcvd()).isEmpty()) ? "" : "RST " + s(q.rstSent()) + " / " + s(q.rstRcvd());
        String who = (s(q.name()) + (s(q.name()).isEmpty() || s(q.qth()).isEmpty() ? "" : " · ") + s(q.qth())).trim();
        String me = HubConfig.call();
        String sig = "TNX QSO · 73" + (me.isEmpty() ? "" : " de " + me);

        List<Line> l = new ArrayList<>();
        l.add(new Line("QSL  " + s(q.call()).toUpperCase(Locale.ROOT), 3.0, true));
        l.add(new Line(when, 1.2, false));
        if (!fb.isBlank()) l.add(new Line(fb.trim(), 1.2, false));
        if (!rst.isEmpty()) l.add(new Line(rst, 1.0, false));
        if (!who.isEmpty()) l.add(new Line(who, 1.0, false));
        l.add(new Line(sig, 1.0, true));
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

    /** Prints the label with the saved settings (blocking). */
    public static Result print(Qso q) throws Exception {
        Model m = currentModel();
        Size sz = currentSize(m);
        String printer = HubConfig.get("label.printer", "").trim();
        int copies = Math.max(1, Math.min(20, (int) num(HubConfig.get("label.copies", "1"))));
        if ("zpl".equals(mode(m))) {
            String host = HubConfig.get("label.host", "").trim();
            if (host.isEmpty() && printer.isEmpty()) return new Result(false, "No label printer set — choose one in Setup ▸ Label printers");
            int dpi = (int) num(HubConfig.get("label.dpi", String.valueOf(m.dpi())));
            sendZpl(zpl(q, sz, dpi > 0 ? dpi : 203, copies), host, printer);
            return new Result(true, "Label sent to " + (host.isEmpty() ? printer : host));
        }
        if (printer.isEmpty()) return new Result(false, "No label printer set — choose one in Setup ▸ Label printers");
        printDriver(q, sz, printer, copies);
        return new Result(true, "Label sent to " + printer);
    }

    /** Sample label for the Setup "Test print" button. */
    public static Qso sample() {
        return new Qso("K1ABC", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(Instant.now()),
                "14.074", "20m", "FT8", "-10", "-12", "Sample", "Baltimore MD");
    }

    // ---- driver path (Java 2D) --------------------------------------------------------------------

    private static final double PT_PER_MM = 72.0 / 25.4;

    private static void printDriver(Qso q, Size sz, String printerName, int copies) throws Exception {
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

        List<Line> lines = layout(q);
        Printable body = (g, fmt, idx) -> {
            if (idx > 0) return Printable.NO_SUCH_PAGE;
            draw((Graphics2D) g, lines, pageW, pageH, strip);
            return Printable.PAGE_EXISTS;
        };
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setPrintService(svc);
        job.setJobName("QSL label " + s(q.call()));
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
            Font f = fit(g, l, usableW, band * 0.82);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            double baseline = y + (band - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(l.text(), (float) (pad + (usableW - fm.stringWidth(l.text())) / 2), (float) baseline);
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

    static String zpl(Qso q, Size sz, int dpi, int copies) {
        double dotsPerMm = dpi / 25.4;
        int w = (int) Math.round(sz.wMm() * dotsPerMm), h = (int) Math.round(sz.hMm() * dotsPerMm);
        int pad = (int) Math.round(2.5 * dotsPerMm);
        int usableW = w - 2 * pad, usableH = h - 2 * pad;
        List<Line> lines = layout(q);
        double total = 0; for (Line l : lines) total += l.weight();
        StringBuilder z = new StringBuilder("^XA^CI28^PW").append(w).append("^LL").append(h).append("^LH0,0\n");
        double y = pad;
        for (Line l : lines) {
            double band = usableH * l.weight() / total;
            String txt = zplEscape(l.text());
            // ZPL font 0 is proportional; ~0.6 em per character is a safe fit estimate
            int fh = (int) Math.max(12, Math.min(band * 0.82, usableW / (Math.max(1, txt.length()) * 0.6)));
            int top = (int) (y + (band - fh) / 2);
            z.append("^FO").append(pad).append(',').append(top)
             .append("^FB").append(usableW).append(",1,0,C,0")                 // centred in the usable width
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
