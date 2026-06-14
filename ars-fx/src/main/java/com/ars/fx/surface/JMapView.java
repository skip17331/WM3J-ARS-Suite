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
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Map azimuthal propagation map (handoff shot 03), mock data. */
public final class JMapView {
    private JMapView() {}

    public static Region build() {
        Region dock = Shell.dock("map");
        String[][] stats = {{"SPOTS / HR","342"},{"SHOWN","22"},{"GRAY LINE","SR 11:02 · SS 22:48"},{"BEAM","050°","accent"}};
        HBox top = Shell.topBar("map","map","J-Map","Propagation & spots · FN20", stats, "17:00:14");

        HBox center = new HBox(leftPanel(), mapArea()); center.setFillHeight(true);
        Region rail = Shell.rail(railDrawers());
        return Shell.frame(dock, top, center, rail);
    }

    // ---- left controls panel ----------------------------------------------
    private static Region leftPanel() {
        VBox p = new VBox(0); p.getStyleClass().add("jm-panel"); p.setMinWidth(270); p.setPrefWidth(270); p.setMaxWidth(270);
        p.getChildren().add(lbl("PROJECTION","jm-sec"));
        p.getChildren().add(seg(new String[]{"Azimuthal","Rectangular"}, 0));
        // bands
        HBox bandsHead = new HBox(lbl("BANDS","jm-sec")); Region bsp = new Region(); HBox.setHgrow(bsp, Priority.ALWAYS);
        bandsHead.getChildren().addAll(bsp, lbl("ALL","jm-sec-all")); bandsHead.setAlignment(Pos.CENTER_LEFT);
        p.getChildren().add(bandsHead);
        GridPane bands = new GridPane(); bands.setHgap(6); bands.setVgap(6);
        for (int i=0;i<Mock.MAP_BANDS.length;i++){
            String[] b = Mock.MAP_BANDS[i];
            Region dot = new Region(); dot.getStyleClass().add("jm-band-dot"); dot.setStyle("-fx-background-color:"+b[1]+";");
            HBox chip = new HBox(7, dot, lbl(b[0],"jm-band-n")); chip.getStyleClass().add("jm-band"); chip.setAlignment(Pos.CENTER_LEFT);
            GridPane.setHgrow(chip, Priority.ALWAYS); chip.setMaxWidth(Double.MAX_VALUE);
            bands.add(chip, i%3, i/3);
        }
        for (int c=0;c<3;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(100/3.0); bands.getColumnConstraints().add(cc); }
        p.getChildren().add(bands);
        p.getChildren().add(lbl("MODE","jm-sec"));
        p.getChildren().add(seg(new String[]{"All","CW","Phone","Digi"}, 0));
        p.getChildren().add(lbl("SPOT AGE ≤ 20 MIN","jm-sec"));
        p.getChildren().add(slider(0.85));
        p.getChildren().add(lbl("OVERLAYS","jm-sec"));
        for (Object[] o : Mock.OVERLAYS) {
            Label t = lbl((String)o[0],"jm-tog-label"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(t, toggle((boolean)o[1])); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(7,0,7,0));
            p.getChildren().add(row);
        }
        p.getChildren().add(lbl("BEAM HEADING · 050°","jm-sec"));
        p.getChildren().add(slider(0.14));
        return p;
    }
    private static Region seg(String[] opts, int on) {
        HBox h = new HBox(2); h.getStyleClass().add("jm-seg");
        for (int i=0;i<opts.length;i++){ Label b = lbl(opts[i],"jm-seg-btn"); if(i==on) b.getStyleClass().add("on"); HBox.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); h.getChildren().add(b); }
        return h;
    }
    private static Region toggle(boolean on) {
        Region knob = new Region(); knob.getStyleClass().add("jm-switch-knob");
        StackPane sw = new StackPane(knob); sw.getStyleClass().add("jm-switch"); if(on) sw.getStyleClass().add("on");
        sw.setPadding(new Insets(0,3,0,3)); StackPane.setAlignment(knob, on ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        sw.setMaxSize(36,20);
        return sw;
    }
    private static Region slider(double frac) {
        Region track = new Region(); track.setMinHeight(5); track.setMaxHeight(5); track.setStyle("-fx-background-color:-ars-surface-4; -fx-background-radius:99;"); HBox.setHgrow(track, Priority.ALWAYS); track.setMaxWidth(Double.MAX_VALUE);
        Region fill = new Region(); fill.setMinHeight(5); fill.setMaxHeight(5); fill.setStyle("-fx-background-color:-ars-map; -fx-background-radius:99;");
        Region knob = new Region(); knob.setMinSize(15,15); knob.setMaxSize(15,15); knob.setStyle("-fx-background-color:-ars-map; -fx-background-radius:99; -fx-border-color:-ars-surface-1; -fx-border-width:2; -fx-border-radius:99;");
        Pane bar = new Pane(track, fill, knob); bar.setMinHeight(16); bar.setPrefHeight(16);
        bar.widthProperty().addListener((o,ov,nv)->{ double w=nv.doubleValue(); track.resizeRelocate(0,6,w,5); fill.resizeRelocate(0,6,w*frac,5); knob.relocate(Math.max(0,w*frac-7),1); });
        VBox.setMargin(bar, new Insets(8,0,4,0));
        return bar;
    }

    // ---- azimuthal map -----------------------------------------------------
    private static Region mapArea() {
        Canvas cv = azimuthal(786, 760);
        // top-right legend
        VBox tr = new VBox(2,
            rt("22 spots · ≤"), rt("20m"), rt("rings ="), rt("distance"), rt("spokes ="), rt("bearing"));
        tr.setAlignment(Pos.TOP_RIGHT); tr.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(tr, Pos.TOP_RIGHT); StackPane.setMargin(tr, new Insets(14,18,0,0));
        // bottom-left legend card
        VBox card = new VBox(5, lbl("Azimuthal · centered FN20","jm-legend-b"),
            legRow("#26c0cf","your station  / beam"), legRow("ring","needed DX"), legRow("#eebb58","sunlit (gray line)"));
        card.getStyleClass().add("jm-legend-card");
        card.setMaxSize(220, Region.USE_PREF_SIZE);
        StackPane.setAlignment(card, Pos.BOTTOM_LEFT); StackPane.setMargin(card, new Insets(0,0,20,16));
        StackPane area = new StackPane(cv, tr, card); area.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(area, Priority.ALWAYS);
        return area;
    }
    private static Label rt(String s){ return lbl(s, "jm-legend"); }
    private static HBox legRow(String color, String text){
        Region d = new Region(); d.setMinSize(9,9); d.setMaxSize(9,9);
        if (color.equals("ring")) d.setStyle("-fx-background-radius:99; -fx-border-color:-ars-warn; -fx-border-width:1.5; -fx-border-radius:99;");
        else d.setStyle("-fx-background-color:"+color+"; -fx-background-radius:99;");
        HBox r = new HBox(8, d, lbl(text,"jm-legend-row")); r.setAlignment(Pos.CENTER_LEFT); return r;
    }

    private static Canvas azimuthal(double w, double h) {
        Canvas cv = new Canvas(w, h);
        GraphicsContext g = cv.getGraphicsContext2D();
        double cx = w/2, cy = h/2 + 4, R = Math.min(w, h)/2 - 70;
        Color border = Color.web("#28313d"), faint = Color.web("#28313d"), t3 = Color.web("#6a7684"), t4 = Color.web("#4d5663");

        // night disk
        g.setFill(Color.web("#000000", 0.22)); g.fillOval(cx-R*0.42, cy-R*0.42, R*0.84, R*0.84);
        g.setStroke(Color.web("#4d5663", 0.6)); g.setLineWidth(1); g.setLineDashes(4,4);
        g.strokeOval(cx-R*0.42, cy-R*0.42, R*0.84, R*0.84); g.setLineDashes(null);

        // range rings
        double[] rr = {0.34, 0.67, 1.0}; String[] rl = {"5k km","10k km","15k km"};
        g.setStroke(border); g.setLineWidth(1);
        for (int i=0;i<3;i++){ double r=R*rr[i]; g.strokeOval(cx-r, cy-r, r*2, r*2); }
        g.setFont(Font.font("JetBrains Mono", 9.5)); g.setFill(t4); g.setTextAlign(TextAlignment.LEFT);
        for (int i=0;i<3;i++){ g.fillText(rl[i], cx+4, cy-R*rr[i]+12); }

        // spokes + compass labels
        String[] comp = {"N","30","60","E","120","150","S","210","240","W","300","330"};
        g.setLineWidth(1);
        for (int i=0;i<12;i++){
            double a = Math.toRadians(i*30 - 90);
            g.setStroke(Color.web("#28313d", 0.55));
            g.strokeLine(cx, cy, cx+Math.cos(a)*R, cy+Math.sin(a)*R);
            g.setFill(t3); g.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.CENTER);
            g.fillText(comp[i], cx+Math.cos(a)*(R+18), cy+Math.sin(a)*(R+18)+4);
        }

        // continents
        g.setFill(Color.web("#4d5663")); g.setFont(Font.font("IBM Plex Sans", FontWeight.NORMAL, 13)); g.setTextAlign(TextAlignment.CENTER);
        Object[][] cont = {{"ASIA",0.0,0.86},{"EUROPE",58.0,0.86},{"AFRICA",110.0,0.86},{"S. AMERICA",180.0,0.86},{"N.A.",312.0,0.86},{"OCEANIA",250.0,0.86}};
        for (Object[] c : cont){ double a=Math.toRadians((double)c[1]-90); double r=R*(double)c[2]; g.fillText((String)c[0], cx+Math.cos(a)*r, cy+Math.sin(a)*r); }

        // beam wedge toward 050°
        double beam = Math.toRadians(50-90), spread = Math.toRadians(11);
        g.setFill(Color.web("#31bdda", 0.16));
        g.beginPath(); g.moveTo(cx, cy);
        g.lineTo(cx+Math.cos(beam-spread)*R, cy+Math.sin(beam-spread)*R);
        g.lineTo(cx+Math.cos(beam+spread)*R, cy+Math.sin(beam+spread)*R);
        g.closePath(); g.fill();

        // spots
        for (Object[] s : Mock.AZ_SPOTS){
            String call=(String)s[0]; double br=(double)s[1], dist=(double)s[2]; Color col=Color.web((String)s[3]);
            double a=Math.toRadians(br-90), x=cx+Math.cos(a)*dist*R, y=cy+Math.sin(a)*dist*R;
            g.setFill(col); g.fillOval(x-4.5, y-4.5, 9, 9);
            g.setFill(Color.web("#e8eef4")); g.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.LEFT);
            g.fillText(call, x+8, y+4);
        }
        // me (center)
        g.setFill(Color.web("#31bdda")); g.fillOval(cx-5, cy-5, 10, 10);
        g.setStroke(Color.web("#31bdda", 0.35)); g.setLineWidth(3); g.strokeOval(cx-5, cy-5, 10, 10);
        g.setFill(Color.web("#31bdda")); g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.fillText("WM3J", cx+9, cy+4);
        return cv;
    }

    // ---- right rail --------------------------------------------------------
    private static Node[] railDrawers() {
        VBox spots = new VBox();
        for (String[] s : Mock.DX_LIST) {
            Region dot = new Region(); dot.getStyleClass().add("jm-spot-dot"); dot.setStyle("-fx-background-color:-ars-accent;");
            Label fq = lbl(s[0], "jm-spot-fq"); fq.setMinWidth(54);
            VBox txt = new VBox(1, hcall(s[1], s[4]), lbl(s[2] + " · " + s[3], "jm-spot-meta")); HBox.setHgrow(txt, Priority.ALWAYS);
            HBox row = new HBox(11, dot, fq, txt); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jm-spot-row");
            spots.getChildren().add(row);
        }
        List<Node> rail = new ArrayList<>();
        rail.add(Shell.drawer("DX Spots","map","map","22 spots", true, spots));
        rail.add(Shell.drawer("DX station info","accent","hub","", false, lbl("Select a spot to see DXCC, beam heading & distance.","jm-spot-meta")));
        List<Node> inst = Shell.instruments(50);
        rail.add(inst.get(0)); rail.add(inst.get(1));   // Antenna·Rotor, Propagation
        return rail.toArray(new Node[0]);
    }
    private static Node hcall(String call, String need){
        HBox h = new HBox(7, lbl(call, "jm-spot-call")); h.setAlignment(Pos.CENTER_LEFT);
        if (need.equals("y")) h.getChildren().add(lbl("NEED","jhub-need"));
        return h;
    }
}
