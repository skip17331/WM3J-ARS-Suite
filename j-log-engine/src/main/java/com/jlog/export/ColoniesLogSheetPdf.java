package com.jlog.export;

import com.jlog.model.QsoRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the Original 13 Colonies Special Event certificate-request
 * log sheet as a one-page US-Letter PDF. Auto-fills the per-station
 * DATE / UTC / FREQ / RS(T) / MODE columns from the QSOs the operator
 * has logged for each callsign.
 *
 * <p>Output matches the layout of the organizer's official sheet
 * (Tony N4ATJ, PO Box 622 McAdenville NC 28101). The title is plain
 * text rather than the official tri-flag artwork — the operator can
 * print, sign, attach payment, and mail.
 */
public final class ColoniesLogSheetPdf {

    /** Per the 2025 organizer sheet. */
    private static final List<String[]> ROWS = List.of(
        new String[]{"K2A", "NY"}, new String[]{"K2B", "VA"},
        new String[]{"K2C", "RI"}, new String[]{"K2D", "CT"},
        new String[]{"K2E", "DE"}, new String[]{"K2F", "MD"},
        new String[]{"K2G", "GA"}, new String[]{"K2H", "MA"},
        new String[]{"K2I", "NJ"}, new String[]{"K2J", "NC"},
        new String[]{"K2K", "NH"}, new String[]{"K2L", "SC"},
        new String[]{"K2M", "PA"},
        new String[]{"WM3PEN",  ""},
        new String[]{"GB13COL", ""},
        new String[]{"TM13COL", ""});

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    /** Header rendering options. Operator-supplied fields filled by the user
     *  pre-print; this lets us populate them if known, or leave them blank. */
    public static final class OperatorInfo {
        public String name;
        public String callsign;
        public String address;
        public String city;
        public String stateOrCountry;
        public String zipOrPostal;
    }

    private ColoniesLogSheetPdf() {}

    /**
     * Write the PDF to {@code outputPath}.
     *
     * @param firstQsoPerCall map of station callsign → the QSO row to put
     *                        on the sheet (typically the first or best QSO
     *                        with that station). Callsigns not in this map
     *                        get blank rows.
     * @param operator        optional operator info; pass {@code null} for
     *                        blank name/address fields.
     */
    public static void write(Path outputPath,
                             Map<String, QsoRecord> firstQsoPerCall,
                             OperatorInfo operator) throws IOException {
        Map<String, QsoRecord> qsos = firstQsoPerCall != null
            ? firstQsoPerCall : new LinkedHashMap<>();

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream c = new PDPageContentStream(doc, page)) {
                drawTitle(c);
                drawTable(c, qsos);
                drawEndorsements(c);
                drawFooterAndOperator(c, operator);
            }
            doc.save(outputPath.toFile());
        }
    }

    // ----------------------------------------------------------------
    //  Layout — coords measured from bottom-left, US Letter is 612x792
    // ----------------------------------------------------------------

    private static final float PAGE_W = 612, PAGE_H = 792;
    private static final float MARGIN = 36;

    private static final float TITLE_Y         = PAGE_H - 60;
    private static final float TABLE_TOP_Y     = TITLE_Y - 30;
    private static final float ROW_H           = 28;
    private static final float HEADER_ROW_H    = 22;
    private static final float ENDORSE_Y       = TABLE_TOP_Y - HEADER_ROW_H - ROW_H * 16 - 24;
    private static final float FOOTER_Y        = ENDORSE_Y - 48;
    private static final float OPERATOR_Y      = FOOTER_Y - 32;

    // Five-column data area + a 90 px station-label column on the left
    private static final float STATION_COL_W = 90;
    private static final float[] COL_W = { 90,  90,  85,  85,  80 };  // DATE, UTC, FREQ, RS(T), MODE
    private static final String[] HEADERS = { "DATE", "UTC", "FREQ", "RS(T)", "MODE" };

    private static void drawTitle(PDPageContentStream c) throws IOException {
        String title = "Original 13 Colonies Special Event — Certificate Request Log Sheet";
        c.beginText();
        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
        // Center the title — width is approximate; 14pt Helv-Bold avg ≈ 7.4 px/char
        float titleW = title.length() * 7.4f;
        c.newLineAtOffset((PAGE_W - titleW) / 2f, TITLE_Y);
        c.showText(title);
        c.endText();
    }

    private static void drawTable(PDPageContentStream c,
                                  Map<String, QsoRecord> qsos) throws IOException {
        float left = MARGIN;
        float top  = TABLE_TOP_Y;

        // Header row
        drawHeaderRow(c, left, top);

        // Body rows
        float y = top - HEADER_ROW_H;
        for (String[] row : ROWS) {
            String label = row[0] + (row[1].isEmpty() ? "" : " - " + row[1]);
            drawBodyRow(c, left, y, label, qsos.get(row[0]));
            y -= ROW_H;
        }
    }

    private static void drawHeaderRow(PDPageContentStream c, float left, float top) throws IOException {
        // Empty station-label cell
        rect(c, left, top - HEADER_ROW_H, STATION_COL_W, HEADER_ROW_H);
        float x = left + STATION_COL_W;
        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
        for (int i = 0; i < HEADERS.length; i++) {
            rect(c, x, top - HEADER_ROW_H, COL_W[i], HEADER_ROW_H);
            // Center text in cell
            float tw = HEADERS[i].length() * 6.5f;
            text(c, x + (COL_W[i] - tw) / 2f, top - HEADER_ROW_H + 7, HEADERS[i]);
            x += COL_W[i];
        }
    }

    private static void drawBodyRow(PDPageContentStream c, float left, float topOfRow,
                                    String label, QsoRecord qso) throws IOException {
        rect(c, left, topOfRow - ROW_H, STATION_COL_W, ROW_H);
        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        text(c, left + 6, topOfRow - ROW_H + 10, label);

        // Data cells
        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        String[] cells = qsoCells(qso);
        float x = left + STATION_COL_W;
        for (int i = 0; i < COL_W.length; i++) {
            rect(c, x, topOfRow - ROW_H, COL_W[i], ROW_H);
            if (cells[i] != null && !cells[i].isEmpty()) {
                text(c, x + 4, topOfRow - ROW_H + 10, cells[i]);
            }
            x += COL_W[i];
        }
    }

    private static String[] qsoCells(QsoRecord q) {
        if (q == null) return new String[]{ "", "", "", "", "" };
        LocalDateTime t = q.getDateTimeUtc();
        String date = t == null ? "" : t.format(DATE_FMT);
        String utc  = t == null ? "" : t.format(TIME_FMT);
        // Prefer kHz/MHz freq if present, else fall back to band.
        String freq = nonBlank(q.getFrequency(), q.getBand());
        String rst  = nonBlank(q.getRstSent(), q.getRstReceived());
        String mode = q.getMode() == null ? "" : q.getMode();
        return new String[]{ date, utc, freq, rst, mode };
    }

    private static void drawEndorsements(PDPageContentStream c) throws IOException {
        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
        text(c, MARGIN, ENDORSE_Y, "ENDORSEMENTS");

        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        String[] line1 = { "ALL QRP", "VET", "AMER. LEGION", "NRA", "VFW", "CW" };
        float x = MARGIN + 95;
        for (String label : line1) {
            checkbox(c, x, ENDORSE_Y);
            text(c, x + 12, ENDORSE_Y, label);
            x += 12 + label.length() * 5.6f + 10;
        }
        String[] line2 = { "MOBILE", "DIGITAL", "SATELLITE" };
        x = MARGIN + 95;
        float y = ENDORSE_Y - 16;
        for (String label : line2) {
            checkbox(c, x, y);
            text(c, x + 12, y, label);
            x += 12 + label.length() * 5.6f + 30;
        }
    }

    private static void drawFooterAndOperator(PDPageContentStream c,
                                              OperatorInfo op) throws IOException {
        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        text(c, MARGIN, FOOTER_Y,
            "WE REQUEST A DONATION OF $5.00, CASH, CHECK, OR MONEY ORDER");
        text(c, MARGIN, FOOTER_Y - 12,
            "PLEASE SEND YOUR REQUEST TO:  TONY — N4ATJ — PO BOX 622, MCADENVILLE, NC 28101");

        // Operator info fields
        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
        text(c, MARGIN,           OPERATOR_Y,       "NAME");
        text(c, MARGIN + 220,     OPERATOR_Y,       "CALL SIGN");
        text(c, MARGIN,           OPERATOR_Y - 22,  "ADDRESS");
        text(c, MARGIN + 220,     OPERATOR_Y - 22,  "CITY");
        text(c, MARGIN,           OPERATOR_Y - 44,  "STATE / PROV / COUNTRY");
        text(c, MARGIN + 220,     OPERATOR_Y - 44,  "ZIP / POSTAL CODE");

        // Underlines for the fields
        for (int row = 0; row < 3; row++) {
            float yy = OPERATOR_Y - 22 * row - 3;
            line(c, MARGIN + 130, yy, MARGIN + 210, yy);   // left value box
            line(c, MARGIN + 295, yy, MARGIN + 540, yy);   // right value box
        }

        if (op != null) {
            c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            if (op.name      != null) text(c, MARGIN + 134, OPERATOR_Y,       op.name);
            if (op.callsign  != null) text(c, MARGIN + 299, OPERATOR_Y,       op.callsign);
            if (op.address   != null) text(c, MARGIN + 134, OPERATOR_Y - 22,  op.address);
            if (op.city      != null) text(c, MARGIN + 299, OPERATOR_Y - 22,  op.city);
            if (op.stateOrCountry != null) text(c, MARGIN + 134, OPERATOR_Y - 44, op.stateOrCountry);
            if (op.zipOrPostal    != null) text(c, MARGIN + 299, OPERATOR_Y - 44, op.zipOrPostal);
        }

        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 9);
        text(c, MARGIN, OPERATOR_Y - 64,
            "We hope you enjoyed the special event — please have a happy Independence Day!");
    }

    // ----------------------------------------------------------------
    //  Drawing primitives
    // ----------------------------------------------------------------

    private static void rect(PDPageContentStream c, float x, float y, float w, float h) throws IOException {
        c.setLineWidth(0.6f);
        c.addRect(x, y, w, h);
        c.stroke();
    }

    private static void line(PDPageContentStream c, float x1, float y1, float x2, float y2) throws IOException {
        c.setLineWidth(0.6f);
        c.moveTo(x1, y1); c.lineTo(x2, y2);
        c.stroke();
    }

    private static void text(PDPageContentStream c, float x, float y, String s) throws IOException {
        c.beginText();
        c.newLineAtOffset(x, y);
        c.showText(s);
        c.endText();
    }

    private static void checkbox(PDPageContentStream c, float x, float y) throws IOException {
        c.setLineWidth(0.6f);
        c.addRect(x, y, 9, 9);
        c.stroke();
    }

    private static String nonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return "";
    }
}
