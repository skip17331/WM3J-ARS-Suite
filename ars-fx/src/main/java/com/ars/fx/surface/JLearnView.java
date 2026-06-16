package com.ars.fx.surface;

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

    public static Region build() {
        Region dock = Shell.dock("learn");
        List<Chapter> chapters = JLearnLibrary.chapters();
        int adv = 0; for (Chapter c : chapters) for (Section s : c.sections()) if (s.advanced()) adv++;
        String[][] stats = {{"CHAPTERS", String.valueOf(chapters.size())},
                {"SECTIONS", String.valueOf(JLearnLibrary.sectionCount())},
                {"ADVANCED", String.valueOf(adv), "learn"}};
        HBox top = Shell.topBar("learn", "learn", "J-Learn", "Reference & learning library", stats, null);

        // reading window (main content area)
        StackPane window = new StackPane(); window.setAlignment(Pos.TOP_LEFT); HBox.setHgrow(window, Priority.ALWAYS);
        window.setStyle("-fx-background-color:-ars-bg;");
        Consumer<Section> open = s -> window.getChildren().setAll(contentWindow(s));
        window.getChildren().setAll(welcome());

        // collapsible left chapter/section TOC drawer
        StackPane leftHolder = new StackPane(); leftHolder.setAlignment(Pos.TOP_LEFT);
        int[] collapsed = {0};
        String[] expanded = {chapters.isEmpty() ? "" : chapters.get(0).num()};   // open chapter (accordion)
        String[] selected = {null};                                              // current section id
        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> leftHolder.getChildren().setAll(collapsed[0] == 1
                ? collapsedRail(() -> { collapsed[0] = 0; rebuild[0].run(); })
                : chapterDrawer(chapters, expanded, selected, open, rebuild[0],
                        () -> { collapsed[0] = 1; rebuild[0].run(); }));
        rebuild[0].run();

        HBox center = new HBox(leftHolder, window); center.setFillHeight(true);
        return Shell.frame(dock, top, center, null);
    }

    // ---- left collapsible TOC ---------------------------------------------
    private static Region chapterDrawer(List<Chapter> chapters, String[] expanded, String[] selected,
                                        Consumer<Section> open, Runnable rebuild, Runnable onCollapse) {
        VBox p = new VBox(0); p.getStyleClass().add("j2-topics"); p.setMinWidth(272); p.setPrefWidth(272); p.setMaxWidth(272);
        Label title = lbl("CHAPTERS", "j2-sec"); HBox.setHgrow(title, Priority.ALWAYS); title.setMaxWidth(Double.MAX_VALUE);
        Label collapse = lbl("‹", "jm-collapse"); collapse.setStyle("-fx-cursor:hand;"); collapse.setOnMouseClicked(e -> onCollapse.run());
        HBox header = new HBox(8, title, collapse); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(2, 6, 6, 2));
        p.getChildren().add(header);

        VBox list = new VBox(2);
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
    private static Region collapsedRail(Runnable onExpand) {
        VBox p = new VBox(0); p.getStyleClass().add("j2-topics"); p.setMinWidth(40); p.setPrefWidth(40); p.setMaxWidth(40);
        p.setAlignment(Pos.TOP_CENTER); p.setPadding(new Insets(8, 0, 16, 0));
        Label expand = lbl("›", "jm-collapse"); expand.setStyle("-fx-cursor:hand;"); expand.setOnMouseClicked(e -> onExpand.run());
        Label vlabel = lbl("CHAPTERS", "j2-sec"); vlabel.setRotate(90);
        VBox vbox = new VBox(vlabel); vbox.setAlignment(Pos.CENTER); VBox.setMargin(vbox, new Insets(30, 0, 0, 0)); vbox.setMinWidth(120);
        StackPane vwrap = new StackPane(vbox); vwrap.setMinWidth(36); vwrap.setMaxWidth(36);
        p.getChildren().addAll(expand, vwrap);
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
    private static Region contentWindow(Section s) {
        VBox v = new VBox(9); v.setPadding(new Insets(28, 30, 36, 30)); v.setMaxWidth(900); v.setStyle("-fx-background-color:-ars-bg;");
        v.getChildren().add(lbl(s.chapter() + " · SECTION " + s.section() + (s.advanced() ? "   ·   ADVANCED ⚙" : ""), "j2-content-eyebrow"));
        v.getChildren().add(wrap(lbl(s.title(), "j2-h1")));
        String md = JLearnLibrary.body(s);
        if (md == null || md.isBlank()) {
            v.getChildren().add(wrap(lbl("This section's text isn't on disk yet — run J-Learn once to seed the content library (~/.j-learn/content).", "j2-content-body")));
            v.getChildren().add(lbl(s.path(), "j2-content-meta"));
        } else {
            for (Block b : parse(md)) v.getChildren().add(renderBlock(b));
        }
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
