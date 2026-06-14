package com.ars.fx.shell;

import com.ars.fx.mock.Mock;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared shell builders — 1:1 with suite-shell.jsx (SuiteDock, SuiteDrawer,
 * SuiteInstruments, top bar). Pure view code over mock data.
 */
public final class Shell {
    private Shell() {}

    /** Module hue name -> Color (glyph strokes are set in Java; CSS handles tile bg). */
    public static final Map<String,Color> HUE = Map.ofEntries(
        Map.entry("log",   Color.web("#ebb353")), Map.entry("map",    Color.web("#26c0cf")),
        Map.entry("digi",  Color.web("#b191ea")), Map.entry("bridge", Color.web("#6fa7ee")),
        Map.entry("sat",   Color.web("#6fc884")), Map.entry("vault",  Color.web("#dba475")),
        Map.entry("learn", Color.web("#ef7d83")), Map.entry("hub",    Color.web("#a2c4d0")),
        Map.entry("accent",Color.web("#31bdda")), Map.entry("t2",     Color.web("#97a4b2")));

    // ---- small helpers -----------------------------------------------------
    public static Label lbl(String text, String... classes) {
        Label l = new Label(text); l.getStyleClass().addAll(classes); return l;
    }
    static Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); VBox.setVgrow(r, Priority.ALWAYS); return r; }
    static Region hsp()    { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    /** A rounded hue-tinted icon tile holding a module glyph. */
    public static StackPane iconTile(String hue, String glyphId, double glyphSize, String... tileClasses) {
        Color c = HUE.getOrDefault(hue, HUE.get("t2"));
        StackPane tile = new StackPane(Glyphs.glyph(glyphId, glyphSize, c));
        tile.getStyleClass().addAll(tileClasses);
        if (!hue.equals("gear")) tile.getStyleClass().add(hue);
        return tile;
    }

    // ---- shell frame assembly: dock | (top + [center | rail]) -------------
    public static Region frame(Region dock, HBox top, Region center, Region rail) {
        HBox.setHgrow(center, Priority.ALWAYS);
        Region body = (rail == null) ? center : new HBox(center, rail);
        if (body instanceof HBox hb) hb.setFillHeight(true);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox modwin = new VBox(top, body); modwin.getStyleClass().add("sx-modwin");
        HBox.setHgrow(modwin, Priority.ALWAYS); VBox.setVgrow(modwin, Priority.ALWAYS);
        HBox root = new HBox(dock, modwin); root.getStyleClass().add("sx-shell"); root.setFillHeight(true);
        return root;
    }

    // ---- module dock (collapsed icon rail, hover-expands) ------------------
    public static Region dock(String activeId) {
        VBox dock = new VBox(); dock.getStyleClass().add("sx-dock");
        dock.setMinWidth(58); dock.setPrefWidth(58); dock.setMaxWidth(58);
        List<Node> expandOnly = new ArrayList<>();   // names + section labels + dots

        // J-Hub at top
        dock.getChildren().add(dockItem("hub", "J-Hub", "hub", true, activeId, expandOnly));
        Label sec = lbl("MODULES", "sx-dock-sec"); sec.setManaged(false); sec.setVisible(false); expandOnly.add(sec);
        dock.getChildren().add(sec);
        for (Mock.Mod m : Mock.MODULES)
            dock.getChildren().add(dockItem(m.id(), m.name(), m.hue(), m.running(), activeId, expandOnly));
        dock.getChildren().add(spacer());
        // Station settings foot
        VBox foot = new VBox(dockItemRaw("gear", "Station settings", "gear", false, activeId, expandOnly));
        foot.getStyleClass().add("sx-dock-foot");
        dock.getChildren().add(foot);

        dock.setOnMouseEntered(e -> { setExpanded(dock, expandOnly, true); });
        dock.setOnMouseExited(e -> { setExpanded(dock, expandOnly, false); });
        return dock;
    }
    private static void setExpanded(VBox dock, List<Node> nodes, boolean exp) {
        double w = exp ? 216 : 58;
        dock.setMinWidth(w); dock.setPrefWidth(w); dock.setMaxWidth(w);
        for (Node n : nodes) { n.setManaged(exp); n.setVisible(exp); }
    }
    private static HBox dockItem(String id, String name, String hue, boolean running, String active, List<Node> expandOnly) {
        return dockItemRaw(id, name, hue, running, active, expandOnly);
    }
    private static HBox dockItemRaw(String id, String name, String hue, boolean running, String active, List<Node> expandOnly) {
        StackPane ic = iconTile(hue, hue.equals("gear") ? "gear" : id, 20, "sx-dock-ic");
        if (!running && !id.equals(active)) ic.setOpacity(0.62);
        Label nm = lbl(name, "sx-dock-name"); nm.setManaged(false); nm.setVisible(false); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
        Region dot = new Region(); dot.getStyleClass().addAll("sx-dock-dot"); if (running) dot.getStyleClass().add("on");
        dot.setManaged(false); dot.setVisible(false);
        expandOnly.add(nm); if (!hue.equals("gear")) expandOnly.add(dot);
        HBox row = new HBox(12, ic, nm, dot); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("sx-dock-item");
        if (!hue.equals("gear")) row.getStyleClass().add(hue);
        if (id.equals(active)) row.getStyleClass().add("active");
        VBox.setMargin(row, new Insets(2, 9, 2, 9));
        return row;
    }

    // ---- top bar -----------------------------------------------------------
    public static HBox topBar(String hue, String glyphId, String name, String subtitle,
                              String[][] stats, String clock) {
        HBox top = new HBox(); top.getStyleClass().add("sx-top"); top.setAlignment(Pos.CENTER_LEFT);
        StackPane bic = iconTile(hue, glyphId, 18, "sx-brand-ic");
        VBox bt = new VBox(1, lbl(name, "sx-brand-nm"));
        if (subtitle != null && !subtitle.isBlank()) bt.getChildren().add(lbl(subtitle, "sx-brand-ct"));
        bt.setAlignment(Pos.CENTER_LEFT);
        HBox brand = new HBox(10, bic, bt); brand.setAlignment(Pos.CENTER_LEFT); brand.setPadding(new Insets(0, 20, 0, 0));
        top.getChildren().add(brand);
        for (String[] s : stats) {   // {key, value, optional class}
            VBox cell = new VBox(2, lbl(s[0], "sx-stat-k"),
                    lbl(s[1], s.length > 2 ? new String[]{"sx-stat-v", s[2]} : new String[]{"sx-stat-v"}));
            cell.getStyleClass().add("sx-stat"); cell.setAlignment(Pos.CENTER_LEFT);
            top.getChildren().add(cell);
        }
        top.getChildren().add(hsp());
        if (clock != null) {
            VBox c = new VBox(1, lbl("UTC", "sx-clock-k"), lbl(clock, "sx-clock-v"));
            c.setAlignment(Pos.CENTER_RIGHT); top.getChildren().add(c);
        }
        return top;
    }

    // ---- collapsible drawer ------------------------------------------------
    public static VBox drawer(String title, String hue, String glyphId, String summary, boolean open, Node body) {
        VBox dw = new VBox(); dw.getStyleClass().add("sx-dw"); if (open) dw.getStyleClass().add("open");
        StackPane ic = iconTile(hue, glyphId, 14, "sx-dw-ic");
        Label t = lbl(title, "sx-dw-t"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
        Label sum = lbl(summary == null ? "" : summary, "sx-dw-sum");
        Label cv = lbl("›", "sx-dw-cv");   // ›
        HBox head = new HBox(9, ic, t, sum, cv); head.setAlignment(Pos.CENTER_LEFT); head.getStyleClass().add("sx-dw-head");
        VBox bodyBox = new VBox(); bodyBox.getStyleClass().add("sx-dw-body"); bodyBox.getChildren().add(body);
        cv.setRotate(open ? 90 : 0); sum.setVisible(!open); sum.setManaged(!open);
        bodyBox.setManaged(open); bodyBox.setVisible(open);
        head.setOnMouseClicked(e -> {
            boolean nowOpen = !dw.getStyleClass().contains("open");
            dw.getStyleClass().remove("open"); if (nowOpen) dw.getStyleClass().add("open");
            bodyBox.setManaged(nowOpen); bodyBox.setVisible(nowOpen);
            sum.setVisible(!nowOpen); sum.setManaged(!nowOpen); cv.setRotate(nowOpen ? 90 : 0);
        });
        dw.getChildren().addAll(head, bodyBox);
        return dw;
    }

    /** Right-rail container that scrolls. */
    public static ScrollPane rail(Node... drawers) {
        VBox box = new VBox(9, drawers); box.getStyleClass().add("sx-rail");
        ScrollPane sp = new ScrollPane(box); sp.setFitToWidth(true); sp.getStyleClass().add("sx-rail-scroll");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setMinWidth(322); sp.setPrefWidth(322); sp.setMaxWidth(322);
        return sp;
    }

    // ---- standard instrument drawers --------------------------------------
    public static List<Node> instruments(int az) {
        String dir = new String[]{"N","NE","E","SE","S","SW","W","NW"}[(int)Math.round(((az%360+360)%360)/45.0)%8];
        String az3 = String.format("%03d", (az%360+360)%360);
        // Antenna · Rotor
        VBox roInfo = new VBox(0); roInfo.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(roInfo, Priority.ALWAYS);
        HBox head = new HBox(2, lbl(az3, "sx-ro-head"), lbl("°", "sx-ro-dir")); head.setAlignment(Pos.BOTTOM_LEFT);
        HBox turn = new HBox(5, btn("◀ CCW","sx-ro-turn-btn"), btn("Stop","sx-ro-turn-btn","stop"), btn("CW ▶","sx-ro-turn-btn"));
        for (Node n : turn.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        roInfo.getChildren().addAll(head, lbl(dir + " · short path", "sx-ro-dir"), turn);
        VBox.setMargin(turn, new Insets(10,0,0,0));
        HBox roTop = new HBox(13, compass(az, 88), roInfo); roTop.setAlignment(Pos.CENTER_LEFT);
        GridPane presets = new GridPane(); presets.setHgap(5); presets.setVgap(5);
        int i = 0; for (Object[] p : Mock.ROTOR_PRESETS) {
            Label b = btn(p[0]+" "+p[1]+"°", "sx-ro-preset"); GridPane.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
            presets.add(b, i%3, i/3); i++;
        }
        for (int col=0; col<3; col++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(100/3.0); presets.getColumnConstraints().add(cc); }
        VBox rotorBody = new VBox(11, roTop, presets);

        // Propagation
        VBox prop = new VBox(kv("Best band now","20m → EU",true), kv("MUF (3000 km)","28.4 MHz",false),
                kv("Gray line","SR 11:02 · SS 22:48",false), kv("Aurora","quiet",true));

        // Space weather
        GridPane swx = new GridPane(); swx.setHgap(7); swx.setVgap(7);
        int j=0; for (String[] s : Mock.SOLAR){ VBox c=new VBox(2,lbl(s[0],"sx-swx-k"),lbl(s[1],"sx-swx-v")); c.getStyleClass().add("sx-swx-c"); GridPane.setHgrow(c,Priority.ALWAYS); c.setMaxWidth(Double.MAX_VALUE); swx.add(c,j%4,j/4); j++; }
        for (int col=0; col<4; col++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(25); swx.getColumnConstraints().add(cc); }
        VBox bands = new VBox(4);
        for (String[] b : Mock.BANDCOND) bands.getChildren().add(bandRow(b[0],b[1],b[2]));
        VBox spaceBody = new VBox(9, swx, bands, lbl("NOAA SWPC · 14:30Z","sx-dw-stamp"));
        VBox.setMargin(swx, new Insets(0,0,2,0));

        // Weather
        HBox wxBig = new HBox(8, lbl("14°","sx-ro-head"), lbl("Partly cloudy · feels 12°","sx-ro-dir")); wxBig.setAlignment(Pos.BOTTOM_LEFT);
        VBox wxBody = new VBox(wxBig, kv("Wind","NW 12 · g21",false), kv("Humidity","48%",false), kv("Pressure","1018 hPa ↑",false));

        return List.of(
            drawer("Antenna · Rotor","sat","sat", az3+"° "+dir, false, rotorBody),
            drawer("Propagation","map","map","20m → EU", false, prop),
            drawer("Space weather","digi","digi","SFI 168 · K2", false, spaceBody),
            drawer("Weather · FN20","bridge","bridge","14° NW12", false, wxBody));
    }

    static HBox kv(String k, String v, boolean ok) {
        Label kl = lbl(k); kl.getStyleClass().add("k"); HBox.setHgrow(kl, Priority.ALWAYS); kl.setMaxWidth(Double.MAX_VALUE);
        Label vl = lbl(v); vl.getStyleClass().addAll("ars-mono"); vl.getStyleClass().add("v");
        if (ok) vl.setStyle("-fx-text-fill: -ars-ok;");
        HBox row = new HBox(kl, vl); row.getStyleClass().add("sx-kv"); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    static HBox bandRow(String band, String day, String night) {
        Label b = lbl(band, "sx-bc-bn"); HBox.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
        Label d = lbl(day, "sx-bc-c", day); d.setMinWidth(60); d.setAlignment(Pos.CENTER);
        Label n = lbl(night, "sx-bc-c", night); n.setMinWidth(60); n.setAlignment(Pos.CENTER);
        HBox row = new HBox(7, b, d, n); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    static Label btn(String text, String... classes) { Label l = lbl(text, classes); l.setAlignment(Pos.CENTER); return l; }

    // ---- rotor compass dial (port of ARSCompass) --------------------------
    public static Canvas compass(double az, double size) {
        Canvas cv = new Canvas(size, size);
        GraphicsContext g = cv.getGraphicsContext2D();
        double r = size/2 - 8, cx = size/2, cy = size/2;
        for (int i=0;i<36;i++){
            double a = Math.toRadians(i*10 - 90); boolean major = i%9==0;
            double r2 = r - (major?7:3.5);
            g.setStroke(Color.web("#4d5663")); g.setLineWidth(major?1.6:1);
            g.strokeLine(cx+Math.cos(a)*r, cy+Math.sin(a)*r, cx+Math.cos(a)*r2, cy+Math.sin(a)*r2);
        }
        g.setStroke(Color.web("#38434f")); g.setLineWidth(1.5); g.strokeOval(cx-r, cy-r, r*2, r*2);
        double rad = Math.toRadians(az-90), nx = cx+Math.cos(rad)*(r-6), ny = cy+Math.sin(rad)*(r-6);
        g.setStroke(Color.web("#6fc884")); g.setLineWidth(2.5); g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.strokeLine(cx, cy, nx, ny);
        g.setFill(Color.web("#6fc884")); g.fillOval(nx-3, ny-3, 6, 6);
        g.setFill(Color.web("#6a7684")); g.fillOval(cx-2.5, cy-2.5, 5, 5);
        return cv;
    }
}
