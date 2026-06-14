package com.ars.fx.surface;

import com.ars.fx.mock.Mock;
import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Sat satellite tracking (handoff shot 06), mock data. */
public final class JSatView {
    private JSatView() {}

    public static Region build() {
        Region dock = Shell.dock("sat");
        String[][] stats = {{"TRACKING","AO-91","accent"},{"AZ / EL","096° / 31°"},{"NEXT","NOW"},{"ROTOR","● auto-track"}};
        HBox top = Shell.topBar("sat","sat","J-Sat","Satellite tracking · AO-91", stats, "17:00:42");

        HBox center = new HBox(satPanel(), skyCol()); center.setFillHeight(true);
        Region rail = Shell.rail(railDrawers());
        return Shell.frame(dock, top, center, rail);
    }

    // ---- left satellite list ----------------------------------------------
    private static Region satPanel() {
        VBox p = new VBox(9); p.getStyleClass().add("js-panel"); p.setMinWidth(280); p.setPrefWidth(280); p.setMaxWidth(280);
        Node ic = Shell.iconTile("sat","sat",13,"sx-dw-ic");
        HBox head = new HBox(8, lbl("SATELLITES · 6","js-sec")); head.setAlignment(Pos.CENTER_LEFT);
        p.getChildren().add(head);
        for (String[] s : Mock.SATS) p.getChildren().add(satCard(s));
        return p;
    }
    private static Region satCard(String[] s) {
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label pill = lbl(s[2], "js-pill"); if (!s[3].isEmpty()) pill.getStyleClass().add(s[3]);
        HBox top = new HBox(8, lbl(s[0],"js-sat-nm"), lbl(s[1],"js-sat-type"), sp, pill); top.setAlignment(Pos.CENTER_LEFT);
        HBox stats = new HBox(18, stat("Max","el "+s[4]+"°"), stat("Dur",s[5]), stat("Dn",s[6])); stats.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(stats, new Insets(9,0,0,0));
        VBox card = new VBox(top, stats); card.getStyleClass().add("js-sat"); if (s[3].equals("aos")) card.getStyleClass().add("sel");
        return card;
    }
    private static VBox stat(String k, String v){ return new VBox(2, lbl(k,"js-sk"), lbl(v,"js-sv")); }

    // ---- center sky plot + telemetry --------------------------------------
    private static Region skyCol() {
        HBox title = new HBox(8, lbl("AO-91","js-title"), lbl("FM · ascending · max el 47°","js-title-sub")); title.setAlignment(Pos.CENTER_LEFT);
        title.setPadding(new Insets(14,0,8,0));
        StackPane sky = new StackPane(skyPlot(620, 560)); sky.setStyle("-fx-background-color:-ars-bg;"); VBox.setVgrow(sky, Priority.ALWAYS);
        HBox tel = new HBox(12); tel.setAlignment(Pos.CENTER); tel.setPadding(new Insets(0,0,22,0));
        for (String[] t : Mock.SAT_TELEMETRY) {
            Label v = lbl(t[1], "js-tel-v"); if (t[3].equals("y")) v.getStyleClass().add("neg");
            HBox vu = new HBox(3, v, lbl(t[2], "js-tel-u")); vu.setAlignment(Pos.BOTTOM_LEFT);
            VBox cell = new VBox(3, lbl(t[0], "js-tel-k"), vu); cell.getStyleClass().add("js-tel"); cell.setMinWidth(120);
            tel.getChildren().add(cell);
        }
        VBox col = new VBox(0, title, sky, tel); col.setAlignment(Pos.TOP_CENTER); col.setStyle("-fx-background-color:-ars-bg;");
        HBox.setHgrow(col, Priority.ALWAYS); col.setPadding(new Insets(0,20,0,20));
        return col;
    }

    private static Canvas skyPlot(double w, double h) {
        Canvas cv = new Canvas(w, h);
        GraphicsContext g = cv.getGraphicsContext2D();
        double cx = w/2, cy = h/2, R = Math.min(w,h)/2 - 36;
        Color border = Color.web("#28313d"), t3 = Color.web("#6a7684"), sat = Color.web("#6fc884");

        // elevation rings: horizon (0), 30, 60
        g.setStroke(border); g.setLineWidth(1);
        g.strokeOval(cx-R, cy-R, R*2, R*2);
        double r30 = R*(60/90.0), r60 = R*(30/90.0);
        g.strokeOval(cx-r30, cy-r30, r30*2, r30*2);
        g.strokeOval(cx-r60, cy-r60, r60*2, r60*2);
        // spokes
        for (int i=0;i<8;i++){ double a=Math.toRadians(i*45-90); g.setStroke(Color.web("#28313d",0.5)); g.strokeLine(cx,cy,cx+Math.cos(a)*R,cy+Math.sin(a)*R); }
        // labels
        g.setFill(t3); g.setFont(Font.font("JetBrains Mono", 11)); g.setTextAlign(TextAlignment.CENTER);
        g.fillText("zenith", cx, cy+4);
        g.fillText("30°", cx, cy-r30+13); g.fillText("60°", cx, cy-r60+13);
        g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12));
        g.fillText("N", cx, cy-R-12); g.fillText("S", cx, cy+R+18);
        g.setTextAlign(TextAlignment.LEFT);  g.fillText("E", cx+R+8, cy+4);
        g.setTextAlign(TextAlignment.RIGHT); g.fillText("W", cx-R-8, cy+4);

        // pass track: AOS az 30 -> max el 47 -> LOS az 165
        g.setStroke(sat); g.setLineWidth(2); g.setLineDashes(5,5);
        double startAz=30, endAz=165, maxEl=47;
        g.beginPath();
        for (int i=0;i<=60;i++){
            double t=i/60.0; double el=Math.sin(t*Math.PI)*maxEl; double az=startAz+(endAz-startAz)*t;
            double r=R*((90-el)/90.0), a=Math.toRadians(az-90);
            double x=cx+Math.cos(a)*r, y=cy+Math.sin(a)*r;
            if (i==0) g.moveTo(x,y); else g.lineTo(x,y);
        }
        g.stroke(); g.setLineDashes(null);
        // AOS / LOS horizon ticks
        for (double az : new double[]{startAz, endAz}){ double a=Math.toRadians(az-90); g.setFill(t3); g.fillOval(cx+Math.cos(a)*R-2.5, cy+Math.sin(a)*R-2.5, 5,5); }
        // live position: el 31, az 96
        double el=31, az=96, r=R*((90-el)/90.0), a=Math.toRadians(az-90);
        double x=cx+Math.cos(a)*r, y=cy+Math.sin(a)*r;
        g.setStroke(Color.web("#6fc884",0.35)); g.setLineWidth(4); g.strokeOval(x-6,y-6,12,12);
        g.setFill(sat); g.fillOval(x-5,y-5,10,10);
        g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.setTextAlign(TextAlignment.LEFT);
        g.fillText("AO-91", x+11, y+4);
        return cv;
    }

    // ---- right rail --------------------------------------------------------
    private static Node[] railDrawers() {
        VBox doppler = new VBox(9,
            radioBox("DOWNLINK","145.960 +1.2 kHz","145.9612"),
            radioBox("UPLINK","435.250 -0.4 kHz","435.2496"),
            Shell.kv("Mode","FM",false), kvGreen("Doppler track","full · auto"), kvGreen("TX inhibit < 5° el","on"));
        VBox np = new VBox();
        for (String[] p : Mock.NEXT_PASSES) {
            Label nm = lbl(p[0],"js-np-nm"); Label el = lbl(p[1],"js-np-el");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            HBox row = new HBox(9, nm, el, sp, lbl(p[2],"js-np-t")); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("js-np");
            np.getChildren().add(row);
        }
        List<Node> rail = new ArrayList<>();
        rail.add(Shell.drawer("Radio · Doppler","bridge","bridge","145.9612", true, doppler));
        rail.add(Shell.drawer("Next passes","sat","sat","SO-50 +18m", true, np));
        rail.addAll(Shell.instruments(96));   // Antenna·Rotor, Propagation, Space weather, Weather
        return rail.toArray(new Node[0]);
    }
    private static Region radioBox(String k, String sub, String v) {
        VBox left = new VBox(2, lbl(k,"js-radio-k"), lbl(sub,"js-radio-sub"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox box = new HBox(left, sp, lbl(v,"js-radio-v")); box.getStyleClass().add("js-radio"); box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
    private static HBox kvGreen(String k, String v) {
        Label kl = lbl(k); kl.getStyleClass().add("k"); HBox.setHgrow(kl, Priority.ALWAYS); kl.setMaxWidth(Double.MAX_VALUE);
        Label vl = lbl(v); vl.getStyleClass().addAll("ars-mono","v"); vl.setStyle("-fx-text-fill:-ars-sat;");
        HBox row = new HBox(kl, vl); row.getStyleClass().add("sx-kv"); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
