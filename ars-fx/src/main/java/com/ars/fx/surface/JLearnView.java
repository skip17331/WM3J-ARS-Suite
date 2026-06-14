package com.ars.fx.surface;

import com.ars.fx.mock.Mock;
import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import static com.ars.fx.shell.Shell.lbl;

/** J-Learn reference & learning library (handoff shot 08), mock data. */
public final class JLearnView {
    private JLearnView() {}

    public static Region build() {
        Region dock = Shell.dock("learn");
        String[][] stats = {{"DECKS","8"},{"DUE TODAY","63","learn"},{"MASTERED","66%"}};
        HBox top = Shell.topBar("learn","learn","J-Learn","Reference & learning library", stats, null);
        VBox streak = new VBox(0, lbl("Streak","streak-k"), lbl("12 days","streak-v")); streak.setAlignment(Pos.CENTER_RIGHT);
        top.getChildren().add(streak);

        HBox center = new HBox(topics(), deckArea()); center.setFillHeight(true);
        Region rail = Shell.rail(railDrawers());
        return Shell.frame(dock, top, center, rail);
    }

    private static Region topics() {
        VBox p = new VBox(2); p.getStyleClass().add("j2-topics"); p.setMinWidth(250); p.setPrefWidth(250); p.setMaxWidth(250);
        p.getChildren().add(lbl("TOPICS","j2-sec"));
        for (String[] t : Mock.LEARN_TOPICS) {
            Label nm = lbl(t[0],"j2-topic-nm"); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(8, nm, lbl(t[1],"j2-topic-ct")); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("j2-topic");
            if (t[2].equals("y")) row.getStyleClass().add("on");
            p.getChildren().add(row);
        }
        return p;
    }

    private static Region deckArea() {
        HBox h1 = new HBox(10, lbl("All topics","j2-h1"), lbl("· 8 decks · 63 cards due","j2-h1-sub")); h1.setAlignment(Pos.BOTTOM_LEFT);
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(16);
        for (int i=0;i<Mock.LEARN_DECKS.length;i++){ Region card = deckCard(Mock.LEARN_DECKS[i]); GridPane.setHgrow(card, Priority.ALWAYS); grid.add(card, i%2, i/2); }
        for (int c=0;c<2;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(50); grid.getColumnConstraints().add(cc); }
        VBox v = new VBox(18, h1, grid); v.setPadding(new Insets(18, 22, 24, 22)); v.setStyle("-fx-background-color:-ars-bg;");
        ScrollPane sp = new ScrollPane(v); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setStyle("-fx-background-color:-ars-bg;");
        HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }
    private static Region deckCard(String[] d) {
        String hue = switch (d[0]) { case "GUIDE" -> "map"; case "REFERENCE" -> "log"; default -> "learn"; };
        String glyph = switch (d[0]) { case "GUIDE" -> "map"; case "REFERENCE" -> "log"; default -> "learn"; };
        Node ic = Shell.iconTile(hue, glyph, 16, "sx-dw-ic");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox top = new HBox(ic, sp, lbl(d[0],"j2-badge")); top.setAlignment(Pos.CENTER_LEFT);
        double frac = Integer.parseInt(d[5]) / 100.0;
        Region pr = prog(frac);
        Label stat = lbl(d[3] + "/" + d[4] + " cards · " + d[5] + "%","j2-deck-stat"); HBox.setHgrow(stat, Priority.ALWAYS); stat.setMaxWidth(Double.MAX_VALUE);
        Label due = lbl(d[6], "j2-deck-due"); if (d[7].equals("y")) due.getStyleClass().add("ok");
        HBox bot = new HBox(8, stat, due); bot.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(11, top, lbl(d[1],"j2-deck-title"), lbl(d[2],"j2-deck-sub"), pr, bot);
        card.getStyleClass().add("j2-deck"); card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }
    private static Region prog(double frac) {
        Region track = new Region(); track.getStyleClass().add("j2-prog");
        Region fill = new Region(); fill.getStyleClass().add("j2-prog-fill");
        Pane p = new Pane(track, fill); p.setMinHeight(5); p.setMaxHeight(5); p.setMaxWidth(Double.MAX_VALUE);
        p.widthProperty().addListener((o,ov,nv)->{ double w=nv.doubleValue(); track.resizeRelocate(0,0,w,5); fill.resizeRelocate(0,0,w*frac,5); });
        return p;
    }

    private static Node[] railDrawers() {
        VBox ref = new VBox(8);
        ref.getChildren().add(lbl("OPERATING PRACTICE","j2-ref-eyebrow"));
        ref.getChildren().add(lbl("Q-code quick reference","j2-ref-title"));
        ref.getChildren().add(lbl("4 min read · saved offline","j2-ref-meta"));
        Label body = lbl("Q-codes are three-letter signals beginning with Q, used to convey common phrases quickly — essential on CW where every character counts. Each works as both a statement and a question.","j2-ref-body");
        body.setWrapText(true); body.setMaxWidth(Double.MAX_VALUE); ref.getChildren().add(body);
        ref.getChildren().add(lbl("MOST-USED ON THE AIR","j2-ref-sec"));
        VBox q = new VBox(0);
        for (String[] c : Mock.QCODES) {
            Label code = lbl(c[0],"j2-q"); code.setMinWidth(46);
            Label def = lbl(c[1],"j2-qd"); def.setWrapText(true); HBox.setHgrow(def, Priority.ALWAYS); def.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(10, code, def); row.setAlignment(Pos.TOP_LEFT); row.setPadding(new Insets(5,0,5,0));
            q.getChildren().add(row);
        }
        ref.getChildren().add(q);
        ref.getChildren().add(lbl("AS A QUESTION VS. STATEMENT","j2-ref-sec"));
        Label body2 = lbl("Append a question mark on CW or raise inflection on phone to ask: \"QRZ?\" means \"Is this frequency in use?\" while \"QRL\" states \"The frequency is in use.\"","j2-ref-body");
        body2.setWrapText(true); body2.setMaxWidth(Double.MAX_VALUE); ref.getChildren().add(body2);

        VBox quick = new VBox(7, qref("Q-code quick reference","QRZ · QSB · QRM · QSY…"), qref("RST signal reporting","Readability · Strength · Tone"));
        return new Node[]{
            Shell.drawer("Reference reader","learn","learn","", true, ref),
            Shell.drawer("Quick reference","map","map","", true, quick),
        };
    }
    private static VBox qref(String t, String s){ return new VBox(1, lbl(t,"jv-doc-t"), lbl(s,"jv-doc-s")); }
}
