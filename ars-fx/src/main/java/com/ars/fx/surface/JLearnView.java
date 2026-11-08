package com.ars.fx.surface;

import com.ars.fx.data.AntennaCalcs;
import com.ars.fx.data.JLearnLibrary;
import com.ars.fx.data.JLearnLibrary.Chapter;
import com.ars.fx.data.JLearnLibrary.Section;
import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

import static com.ars.fx.shell.Shell.lbl;

/**
 * J-Learn reference library — the old web-UI layout: a collapsible chapter/section
 * table-of-contents on the left and a reading window that displays the selected
 * section's content. No right rail.
 */
public final class JLearnView {
    private JLearnView() {}

    private static boolean workshopRequested, morseRequested;
    /** The next J-Learn screen opens on the Antenna Workshop (the dock's Antenna Workshop entry). */
    public static void requestWorkshop() { workshopRequested = true; }
    /** The next J-Learn screen opens on the Morse Trainer (the dock's Morse Trainer entry). */
    public static void requestMorse() { morseRequested = true; }

    public static Region build() {
        com.ars.fx.data.LearnContent.seedIfMissing();   // first use: copy the chapters bundled in the jar to ~/.j-learn/content
        // the dock entry that opened this screen stays lit: Morse Trainer / Antenna Workshop, else J-Learn itself
        Region dock = Shell.dock(morseRequested ? "morse" : workshopRequested ? "workshop" : "learn");
        List<Chapter> chapters = JLearnLibrary.chapters();
        int adv = 0; for (Chapter c : chapters) for (Section s : c.sections()) if (s.advanced()) adv++;
        String[][] stats = {{"CHAPTERS", String.valueOf(chapters.size())},
                {"SECTIONS", String.valueOf(JLearnLibrary.sectionCount())},
                {"ADVANCED", String.valueOf(adv), "learn"}};
        HBox top = Shell.topBar("learn", "learn", "J-Learn", "Reference & learning library", stats, null);

        // reading window (main content area)
        StackPane window = new StackPane(); window.setAlignment(Pos.TOP_LEFT); HBox.setHgrow(window, Priority.ALWAYS);
        window.setStyle("-fx-background-color:-ars-bg;");
        boolean[] selectedMorse = {false};          // true while the Morse Trainer, not a chapter section, is showing
        boolean[] selectedWs = {false};             // true while the Antenna Workshop is showing
        Runnable[] openWsRef = new Runnable[1];     // (calc id is carried in wsCalc)
        String[] wsCalc = {null};
        List<Section> flat = new java.util.ArrayList<>();
        for (Chapter c : chapters) flat.addAll(c.sections());
        @SuppressWarnings("unchecked") Consumer<String>[] gotoRef = new Consumer[1];
        Consumer<Section> open = s -> { selectedMorse[0] = false; selectedWs[0] = false;
            int at = flat.indexOf(s);
            window.getChildren().setAll(contentWindow(s, at > 0 ? flat.get(at - 1) : null, at >= 0 && at < flat.size() - 1 ? flat.get(at + 1) : null,
                    t -> gotoRef[0].accept(t.id()), () -> { wsCalc[0] = AntennaCalcs.forSection(s.id()).id(); openWsRef[0].run(); })); };
        window.getChildren().setAll(welcome());
        String[] expanded = {chapters.isEmpty() ? "" : chapters.get(0).num()};   // open chapter (accordion)
        String[] selected = {null};                                              // current section id

        // the Morse Trainer lives in J-Learn too: its entry sits above the chapters and opens in the reading window
        Runnable[] rebuildRef = new Runnable[1];
        Runnable openMorse = () -> {
            selectedMorse[0] = true; selectedWs[0] = false;
            window.getChildren().setAll(com.ars.fx.morse.ui.MorseHome.build(pane -> window.getChildren().setAll(pane)));
            if (rebuildRef[0] != null) rebuildRef[0].run();
        };

        // the Antenna Workshop (recommender + calculators) also lives here; section links inside it jump to chapter text
        java.util.function.Consumer<String> gotoSection = id -> {
            for (Chapter c : chapters) for (Section sec : c.sections()) if (sec.id().equals(id)) {
                expanded[0] = c.num(); selected[0] = sec.id(); open.accept(sec); rebuildRef[0].run(); return; }
        };
        gotoRef[0] = gotoSection;
        openWsRef[0] = () -> {
            selectedMorse[0] = false; selectedWs[0] = true; selected[0] = null;
            window.getChildren().setAll(AntennaWorkshopView.build(gotoSection, wsCalc[0]));
            wsCalc[0] = null;
            rebuildRef[0].run();
        };

        // collapsible left chapter/section TOC drawer
        StackPane leftHolder = new StackPane(); leftHolder.setAlignment(Pos.TOP_LEFT);
        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> leftHolder.getChildren().setAll(com.ars.fx.shell.AutoPanel.wrap("learn", false, collapsedRail(),
                chapterDrawer(chapters, expanded, selected, selectedMorse, selectedWs, open, openMorse, () -> { wsCalc[0] = null; openWsRef[0].run(); }, rebuild[0]), 272, () -> false));
        rebuildRef[0] = rebuild[0];
        rebuild[0].run();

        if (workshopRequested) { workshopRequested = false; openWsRef[0].run(); }
        if (morseRequested) { morseRequested = false; openMorse.run(); }
        HBox center = new HBox(leftHolder, window); center.setFillHeight(true);
        return Shell.frame(dock, top, center, null);
    }

    // ---- left collapsible TOC ---------------------------------------------
    private static Region chapterDrawer(List<Chapter> chapters, String[] expanded, String[] selected, boolean[] morseOn,
                                        boolean[] wsOn, Consumer<Section> open, Runnable openMorse, Runnable openWs, Runnable rebuild) {
        VBox p = new VBox(0); p.getStyleClass().add("j2-topics"); p.setMinWidth(272); p.setPrefWidth(272); p.setMaxWidth(272);
        Label title = lbl("CHAPTERS", "j2-sec"); HBox.setHgrow(title, Priority.ALWAYS); title.setMaxWidth(Double.MAX_VALUE);
        HBox header = new HBox(8, title, com.ars.fx.shell.AutoPanel.pinButton("learn", rebuild)); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(2, 6, 6, 2));
        p.getChildren().add(header);

        VBox list = new VBox(2);
        Label morseNm = lbl("Morse Trainer", "j2-topic-nm"); HBox.setHgrow(morseNm, Priority.ALWAYS); morseNm.setMaxWidth(Double.MAX_VALUE);
        HBox morseRow = new HBox(8, lbl("♪", "j2-caret"), morseNm, lbl("practice", "j2-topic-ct"));
        morseRow.setAlignment(Pos.CENTER_LEFT); morseRow.getStyleClass().add("j2-topic"); if (morseOn[0]) morseRow.getStyleClass().add("open");
        com.ars.fx.shell.Tips.tip(morseRow, "Practise CW: letters, groups, QSOs and sending with a key");
        morseRow.setOnMouseClicked(e -> openMorse.run());
        list.getChildren().add(morseRow);
        Label wsNm = lbl("Antenna Workshop", "j2-topic-nm"); HBox.setHgrow(wsNm, Priority.ALWAYS); wsNm.setMaxWidth(Double.MAX_VALUE);
        HBox wsRow = new HBox(8, lbl("📡", "j2-caret"), wsNm, lbl("design", "j2-topic-ct"));
        wsRow.setAlignment(Pos.CENTER_LEFT); wsRow.getStyleClass().add("j2-topic"); if (wsOn[0]) wsRow.getStyleClass().add("open");
        com.ars.fx.shell.Tips.tip(wsRow, "Antenna recommender and design calculators — dipoles, EFHW, traps, loops, Yagis, plus the formula tools");
        wsRow.setOnMouseClicked(e -> openWs.run());
        list.getChildren().add(wsRow);
        for (Chapter c : chapters) {
            boolean openCh = c.num().equals(expanded[0]);
            Label nm = lbl(c.num() + " · " + c.title(), "j2-topic-nm"); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
            Label caret = lbl(openCh ? "▾" : "▸", "j2-caret");
            HBox row = new HBox(8, caret, nm, lbl(String.valueOf(c.sections().size()), "j2-topic-ct"));
            row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("j2-topic"); if (openCh) row.getStyleClass().add("open");
            row.setOnMouseClicked(e -> { expanded[0] = openCh ? "" : c.num(); rebuild.run(); });
            list.getChildren().add(row);
            if (openCh) {
                for (Section s : c.sections()) {
                    Label sl = lbl(s.section() + " · " + s.title() + (s.advanced() ? "  ⚙" : ""), "j2-seclink");
                    sl.setWrapText(true); sl.setMaxWidth(Double.MAX_VALUE);
                    if (s.id().equals(selected[0])) sl.getStyleClass().add("on");
                    sl.setOnMouseClicked(e -> { selected[0] = s.id(); open.accept(s); rebuild.run(); });
                    list.getChildren().add(sl);
                }
            }
        }
        ScrollPane sp = new ScrollPane(list); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;"); VBox.setVgrow(sp, Priority.ALWAYS);
        p.getChildren().add(sp);
        return p;
    }
    private static Region collapsedRail() {
        VBox p = new VBox(0); p.getStyleClass().add("j2-topics"); p.setMinWidth(40); p.setPrefWidth(40); p.setMaxWidth(40);
        p.setAlignment(Pos.TOP_CENTER); p.setPadding(new Insets(8, 0, 16, 0));
        Label expand = lbl("›", "jm-collapse");
        Label vlabel = lbl("CHAPTERS", "j2-sec"); vlabel.setRotate(90);
        javafx.scene.Group vgroup = new javafx.scene.Group(vlabel);   // sized to the rotated label, so it can't spill over the dock
        VBox.setMargin(vgroup, new Insets(30, 0, 0, 0));
        p.getChildren().addAll(expand, vgroup);
        return p;
    }

    // ---- reading window ----------------------------------------------------
    private static Region welcome() {
        VBox v = new VBox(10,
                lbl("REFERENCE LIBRARY", "j2-content-eyebrow"),
                lbl("Pick a section to start reading", "j2-h1"),
                wrap(lbl("Open a chapter in the left drawer and choose a section. J-Learn loads the text from your local library (~/.j-learn/content) and remembers nothing you don't ask it to.", "j2-content-body")));
        v.setPadding(new Insets(28, 30, 28, 30)); v.setMaxWidth(880); v.setStyle("-fx-background-color:-ars-bg;");
        ScrollPane sp = new ScrollPane(v); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;-fx-background:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }
    /** Previous / next section buttons, so reading on never needs a trip back to the chapter list. */
    private static Region pager(Section prev, Section next, Consumer<Section> go) {
        javafx.scene.control.Button b = new javafx.scene.control.Button("◀ Back"), f = new javafx.scene.control.Button("Next ▶");
        b.getStyleClass().add("ars-btn"); f.getStyleClass().add("ars-btn");
        b.setDisable(prev == null); f.setDisable(next == null);
        if (prev != null) { b.setOnAction(e -> go.accept(prev)); com.ars.fx.shell.Tips.tip(b, prev.section() + " · " + prev.title()); }
        if (next != null) { f.setOnAction(e -> go.accept(next)); com.ars.fx.shell.Tips.tip(f, next.section() + " · " + next.title()); }
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox h = new HBox(8, b, gap, f); h.setAlignment(Pos.CENTER); h.setPadding(new Insets(4, 0, 4, 0));
        return h;
    }
    private static Region contentWindow(Section s, Section prev, Section next, Consumer<Section> go, Runnable openCalc) {
        VBox v = new VBox(9); v.setPadding(new Insets(28, 30, 36, 30)); v.setMaxWidth(900); v.setStyle("-fx-background-color:-ars-bg;");
        v.getChildren().add(pager(prev, next, go));
        v.getChildren().add(lbl(s.chapter() + " · SECTION " + s.section() + (s.advanced() ? "   ·   ADVANCED ⚙" : ""), "j2-content-eyebrow"));
        v.getChildren().add(wrap(lbl(s.title(), "j2-h1")));
        if (AntennaCalcs.forSection(s.id()) != null) {
            javafx.scene.control.Button b = new javafx.scene.control.Button("▶ Open in Antenna Workshop calculator");
            b.getStyleClass().addAll("ars-btn", "primary"); b.setOnAction(e -> openCalc.run());
            v.getChildren().add(b);
        }
        String md = JLearnLibrary.body(s);
        if (md == null || md.isBlank()) {
            v.getChildren().add(wrap(lbl("This section's text isn't available. Open J-Hub ▸ Data ▸ J-LEARN CONTENT and press Update files (or Restore bundled copy) to refresh the library.", "j2-content-body")));
            v.getChildren().add(lbl(s.path(), "j2-content-meta"));
        } else {
            for (Block b : parse(md)) v.getChildren().add(renderBlock(b));
        }
        v.getChildren().add(pager(prev, next, go));
        ScrollPane sp = new ScrollPane(v); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;-fx-background:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ---- lightweight markdown → block model -------------------------------
    private enum Kind { HEADING, PARA, BULLET, CODE, TABLE }
    private record Block(Kind kind, String text, List<String> lines, List<String[]> rows) {}

    /** Parse a section body into rendered blocks: headings, paragraphs, bullets, fenced code, pipe tables. */
    private static List<Block> parse(String md) {
        List<Block> out = new java.util.ArrayList<>();
        String[] lines = md.split("\n", -1);
        StringBuilder para = new StringBuilder();
        java.util.function.Consumer<List<Block>> flushPara = o -> {
            if (para.length() > 0) { o.add(new Block(Kind.PARA, para.toString(), null, null)); para.setLength(0); }
        };
        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].strip();
            if (t.startsWith("```")) {                                   // fenced code
                flushPara.accept(out);
                List<String> code = new java.util.ArrayList<>();
                for (i++; i < lines.length && !lines[i].strip().startsWith("```"); i++) code.add(lines[i]);
                out.add(new Block(Kind.CODE, null, code, null));
                continue;
            }
            if (t.startsWith("|") && i + 1 < lines.length && isTableSep(lines[i + 1])) {   // pipe table
                flushPara.accept(out);
                List<String[]> rows = new java.util.ArrayList<>();
                rows.add(splitRow(lines[i]));                            // header
                i += 2;                                                  // skip header + separator
                for (; i < lines.length && lines[i].strip().startsWith("|"); i++) rows.add(splitRow(lines[i]));
                i--;
                out.add(new Block(Kind.TABLE, null, null, rows));
                continue;
            }
            if (t.isEmpty()) { flushPara.accept(out); continue; }
            if (t.startsWith("#")) { flushPara.accept(out); out.add(new Block(Kind.HEADING, inline(t.replaceAll("^#+\\s*", "")), null, null)); continue; }
            if (t.matches("-{3,}")) { flushPara.accept(out); continue; }                    // horizontal rule
            if (t.startsWith("- ") || t.startsWith("* ")) {                                 // bullet
                flushPara.accept(out);
                out.add(new Block(Kind.BULLET, inline(t.replaceAll("^[-*]\\s+", "")), null, null));
                continue;
            }
            if (para.length() > 0) para.append(' ');
            para.append(inline(t.replaceAll("^>\\s*", "")));
        }
        flushPara.accept(out);
        return out;
    }
    private static boolean isTableSep(String line) {
        String t = line.strip();
        return t.contains("-") && t.replace("\\|", "").matches("[\\s:|\\-]+");
    }
    private static String[] splitRow(String line) {
        String t = line.strip();
        if (t.startsWith("|")) t = t.substring(1);
        if (t.endsWith("|")) t = t.substring(0, t.length() - 1);
        String[] parts = t.split("(?<!\\\\)\\|", -1);
        for (int i = 0; i < parts.length; i++) parts[i] = inline(parts[i].replace("\\|", "|").strip());
        return parts;
    }
    /** Strip inline markdown: bold, code ticks, links → text. */
    private static String inline(String s) {
        return s.replace("**", "").replace("`", "").replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
    }

    private static Region renderBlock(Block b) {
        switch (b.kind()) {
            case HEADING: return wrap(lbl(b.text(), "j2-content-h"));
            case BULLET: {
                HBox h = new HBox(8, lbl("•", "j2-content-body"), wrap(lbl(b.text(), "j2-content-body")));
                h.setAlignment(Pos.TOP_LEFT); h.setPadding(new Insets(0, 0, 0, 6)); HBox.setHgrow(h.getChildren().get(1), Priority.ALWAYS);
                return h;
            }
            case CODE: {
                Label code = lbl(String.join("\n", b.lines()), "j2-code"); code.setWrapText(false);
                VBox box = new VBox(code); box.getStyleClass().add("j2-code-box"); box.setMaxWidth(Double.MAX_VALUE);
                return box;
            }
            case TABLE: return tableNode(b.rows());
            default: return wrap(lbl(b.text(), "j2-content-body"));
        }
    }
    private static Region tableNode(List<String[]> rows) {
        int cols = rows.stream().mapToInt(r -> r.length).max().orElse(1);
        GridPane g = new GridPane(); g.getStyleClass().add("j2-table"); g.setMaxWidth(Double.MAX_VALUE);
        for (int c = 0; c < cols; c++) { ColumnConstraints cc = new ColumnConstraints(); cc.setHgrow(Priority.ALWAYS); cc.setFillWidth(true); cc.setPercentWidth(100.0 / cols); g.getColumnConstraints().add(cc); }
        for (int r = 0; r < rows.size(); r++) {
            String[] cells = rows.get(r);
            for (int c = 0; c < cols; c++) {
                Label l = lbl(c < cells.length ? cells[c] : "", r == 0 ? "j2-th" : "j2-td"); l.setWrapText(true); l.setMaxWidth(Double.MAX_VALUE);
                HBox cell = new HBox(l); cell.getStyleClass().add(r == 0 ? "j2-th-cell" : "j2-td-cell");
                if (r > 0 && r % 2 == 0) cell.getStyleClass().add("alt");
                cell.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(cell, Priority.ALWAYS); GridPane.setFillWidth(cell, true);
                g.add(cell, c, r);
            }
        }
        return g;
    }
    private static Label wrap(Label l) { l.setWrapText(true); l.setMaxWidth(Double.MAX_VALUE); return l; }
}
