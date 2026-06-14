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

/** J-Hub unified workspace — cockpit dashboard (handoff shot 01), mock data. */
public final class JHubDashboard {
    private JHubDashboard() {}

    public static Region build()       { return page("dashboard", dashboardCenter()); }
    public static Region buildConfig() { return page("rig", configCenter()); }

    private static Region page(String activeConf, Region centerContent) {
        Region nav = hubNav(activeConf);
        Runnable toggle = () -> { boolean vis = !nav.isVisible(); nav.setManaged(vis); nav.setVisible(vis); };
        ScrollPane sp = new ScrollPane(centerContent); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(nav, sp); row.setFillHeight(true); VBox.setVgrow(row, Priority.ALWAYS);
        VBox root = new VBox(workspaceBar(toggle), row); root.setStyle("-fx-background-color:-ars-bg;");
        return root;
    }

    private static Region dashboardCenter() {
        VBox center = new VBox(20, header(), threeCol());
        center.setPadding(new Insets(18, 24, 24, 24));
        center.setStyle("-fx-background-color:-ars-bg;");
        return center;
    }

    // ---- top workspace bar -------------------------------------------------
    private static Region workspaceBar(Runnable onMenu) {
        Label ham = lbl("☰", "jhub-bar-ham"); ham.setStyle("-fx-cursor:hand;"); ham.setOnMouseClicked(e -> onMenu.run());
        Region dot = chip("jhub-bar-dot", 9);
        HBox brand = new HBox(8, ham, dot, lbl("J-Hub", "jhub-bar-nm"), lbl("· WM3J", "jhub-bar-sub"));
        brand.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label theme = lbl("◑ Theme", "jhub-theme"); theme.setStyle("-fx-cursor:hand;"); theme.setOnMouseClicked(e -> Shell.toggleTheme());
        HBox bar = new HBox(brand, sp, lbl("localhost:8081 · up 4h 12m", "jhub-bar-sub"), theme);
        bar.setSpacing(14); bar.getStyleClass().add("jhub-bar"); bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ---- left nav ----------------------------------------------------------
    private static Region hubNav(String activeConf) {
        VBox nav = new VBox(); nav.getStyleClass().add("jhub-nav"); nav.setMinWidth(256); nav.setPrefWidth(256); nav.setMaxWidth(256);
        HBox head = new HBox(10, lbl("☰", "jhub-bar-ham"), lbl("J-Hub", "jhub-nav-headnm")); head.setAlignment(Pos.CENTER_LEFT); head.getStyleClass().add("jhub-nav-head");
        nav.getChildren().addAll(head, lbl("OPERATE", "jhub-nav-sec"));
        for (Mock.Mod m : Mock.MODULES) nav.getChildren().add(navItem(m, m.id().equals("log")));
        nav.getChildren().add(lbl("J-HUB", "jhub-nav-sec"));
        Region dash = confItem("◎", "Dashboard", null, activeConf.equals("dashboard"));
        dash.setOnMouseClicked(e -> Shell.navigate("hub"));
        nav.getChildren().add(dash);
        nav.getChildren().add(confItem("▣", "Station", "›", false));
        nav.getChildren().add(confItem("◉", "Hardware", "⌄", false));
        for (String s : new String[]{"Rig control","Rotor control","Amplifier","Antenna switch","Antenna workshop"}) {
            Label l = lbl(s, "jhub-nav-sub");
            if (s.equals("Rig control") && activeConf.equals("rig")) l.getStyleClass().add("active");
            if (s.equals("Rig control")) l.setOnMouseClicked(e -> Shell.navigate("hubcfg"));
            VBox.setMargin(l, new Insets(0, 9, 0, 9));
            nav.getChildren().add(l);
        }
        nav.getChildren().add(confItem("▤", "Data", "›", false));
        return nav;
    }
    private static Region navItem(Mock.Mod m, boolean active) {
        Node ic = Shell.iconTile(m.hue(), m.id(), 18, "sx-dock-ic");
        Label nm = lbl(m.name(), "jhub-nav-nm");
        Label st = lbl(m.running() ? "running" : m.sub(), "jhub-nav-st"); if (m.running()) st.getStyleClass().add("run");
        VBox txt = new VBox(1, nm, st);
        HBox row = new HBox(11, ic, txt); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jhub-nav-item");
        if (active && m.running()) row.getStyleClass().addAll("active","run");
        VBox.setMargin(row, new Insets(1, 9, 1, 9));
        row.setOnMouseClicked(e -> Shell.navigate(m.id()));
        return row;
    }
    private static Region confItem(String icon, String name, String caret, boolean active) {
        Label ic = lbl(icon, "jhub-nav-cic");
        Label nm = lbl(name, "jhub-nav-cnm"); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(11, ic, nm); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jhub-nav-conf");
        if (caret != null) row.getChildren().add(lbl(caret, "jhub-nav-caret"));
        if (active) row.getStyleClass().add("active");
        VBox.setMargin(row, new Insets(1, 9, 1, 9));
        return row;
    }

    // ---- center header -----------------------------------------------------
    private static Region header() {
        HBox crumb = new HBox(7, lbl("J-Hub", "jhub-crumb"), lbl("›", "jhub-crumb"), lbl("Dashboard", "jhub-crumb-b"));
        crumb.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        VBox run = new VBox(0, lbl("3/7 modules", "jhub-modrun"), lbl("running", "jhub-modrun")); run.setAlignment(Pos.CENTER_RIGHT);
        HBox crumbRow = new HBox(crumb, sp, run); crumbRow.setAlignment(Pos.CENTER_LEFT);

        HBox profs = new HBox(12); profs.setAlignment(Pos.CENTER_LEFT);
        for (String[] p : Mock.PROFILES) {
            Region d = chip("jhub-mini-dot", 8); d.setStyle("-fx-background-color:-ars-" + p[1] + ";");
            HBox chip = new HBox(8, d, lbl(p[0], "jhub-prof-nm")); chip.getStyleClass().add("jhub-prof"); chip.setAlignment(Pos.CENTER_LEFT);
            profs.getChildren().add(chip);
        }
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Region cdot = chip("jhub-mini-dot", 8); cdot.setStyle("-fx-background-color:-ars-ok;");
        HBox clu = new HBox(7, lbl("Cluster", "jhub-crumb"), cdot, lbl("342/hr", "jhub-bar-sub"),
                lbl("UTC", "jhub-crumb"), lbl("14:42:07", "sx-clock-v")); clu.setAlignment(Pos.CENTER_LEFT);
        HBox profRow = new HBox(profs, sp2, clu); profRow.setAlignment(Pos.CENTER_LEFT);
        return new VBox(14, crumbRow, profRow);
    }

    // ---- 3-column area -----------------------------------------------------
    private static HBox threeColRef = new HBox();
    private static HBox threeCol() {
        HBox box = new HBox(20, miniLog(), dxCluster(), railCol());
        box.setFillHeight(true); VBox.setVgrow(box, Priority.ALWAYS);
        threeColRef = box;
        return box;
    }

    private static Region miniLog() {
        // header
        Node ic = Shell.iconTile("log", "log", 16, "sx-dw-ic");
        VBox ttl = new VBox(1, lbl("J-Log · new QSO", "jhub-card-title"), lbl("1,284 today · 38/hr · CQ WW DX", "jhub-card-sub"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox head = new HBox(11, ic, ttl, sp, lbl("Open full J-Log →", "jhub-open")); head.getStyleClass().add("jhub-card-head"); head.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(13);
        body.setPadding(new Insets(14, 16, 16, 16));
        body.getChildren().add(lbl("Enter a callsign for DXCC, beam heading & worked-before…", "jhub-mini-hint"));
        body.getChildren().add(fieldRow(new String[]{"CALLSIGN","FREQ (MHZ)","MODE"}, new String[]{"—","14.074.00","USB"}, new double[]{-1,120,90}, "call"));
        body.getChildren().add(fieldRow(new String[]{"RST ↑","RST ↓","NAME"}, new String[]{"59","59",""}, new double[]{70,70,-1}, ""));
        body.getChildren().add(fieldRow(new String[]{"QTH","GRID"}, new String[]{"",""}, new double[]{-1,120}, ""));
        VBox cmt = labeled("COMMENT", input("notes, QSL info…", "jhub-mini-field", -1));
        HBox.setHgrow(cmt, Priority.ALWAYS);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        HBox cmtRow = new HBox(12, cmt, sp2, lbl("Clear", "jhub-clearbtn"), lbl("Log QSO ⏎", "jl-logbtn"));
        cmtRow.setAlignment(Pos.BOTTOM_LEFT);
        body.getChildren().add(cmtRow);

        // today's log
        Label tlh = lbl("☰  Today's log"); tlh.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;");
        Label tln = lbl("· 7 shown"); tln.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;");
        HBox tlbar = new HBox(8, tlh, tln); tlbar.getStyleClass().add("jhub-card-head"); tlbar.setAlignment(Pos.CENTER_LEFT);
        VBox tbl = new VBox();
        tbl.getChildren().add(logRow(new String[]{"UTC","CALL","FREQ","BAND","MODE","RST"}, true));
        for (String[] q : Mock.TODAY_LOG) tbl.getChildren().add(logRow(q, false));

        VBox card = new VBox(head, body, tlbar, tbl); card.getStyleClass().add("jhub-card");
        HBox.setHgrow(card, Priority.ALWAYS); card.setMinWidth(420);
        return card;
    }
    private static final double[] LOGC = {52,-1,86,48,52,46};
    private static HBox logRow(String[] c, boolean header) {
        HBox r = new HBox(10); r.setAlignment(Pos.CENTER_LEFT); r.setPadding(new Insets(header?9:8, 16, header?9:8, 16));
        r.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;");
        for (int i=0;i<c.length;i++){
            Label l;
            if (header) { l = lbl(c[i]); l.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;"); }
            else if (i==1) l = lbl(c[i], "jl-tr-call");
            else if (i==4) { Label m = lbl(c[i], "jl-mode"); wrapCol(r,m,i); continue; }
            else l = lbl(c[i], "jl-tr-mono");
            wrapCol(r,l,i);
        }
        return r;
    }
    private static void wrapCol(HBox r, Node n, int i){
        HBox cell = new HBox(n); cell.setAlignment(Pos.CENTER_LEFT);
        if (LOGC[i]<0){ HBox.setHgrow(cell, Priority.ALWAYS); cell.setMaxWidth(Double.MAX_VALUE);} else { cell.setMinWidth(LOGC[i]); cell.setPrefWidth(LOGC[i]); cell.setMaxWidth(LOGC[i]); }
        r.getChildren().add(cell);
    }

    private static Region dxCluster() {
        Node ic = Shell.iconTile("hub", "hub", 16, "sx-dw-ic");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Region cdot = chip("jhub-mini-dot", 8); cdot.setStyle("-fx-background-color:-ars-ok;");
        HBox src = new HBox(6, cdot, lbl("dxc.ve7cc.net", "jhub-clu-de")); src.setAlignment(Pos.CENTER_LEFT);
        HBox head = new HBox(11, ic, lbl("DX Cluster", "jhub-card-title"), sp, src); head.getStyleClass().add("jhub-card-head"); head.setAlignment(Pos.CENTER_LEFT);

        HBox flt = new HBox(8, lbl("All ▾","jhub-clu-flt"), lbl("All ▾","jhub-clu-flt"), lbl("Needed","jhub-clu-flt"));
        Region fsp = new Region(); HBox.setHgrow(fsp, Priority.ALWAYS); flt.getChildren().addAll(fsp, lbl("+ Spot","jhub-clu-flt"));
        flt.setAlignment(Pos.CENTER_LEFT); flt.setPadding(new Insets(12, 16, 12, 16));

        VBox rows = new VBox();
        for (String[] s : Mock.HUB_CLUSTER) rows.getChildren().add(cluRow(s));
        ScrollPane rsp = new ScrollPane(rows); rsp.setFitToWidth(true); rsp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); rsp.setStyle("-fx-background-color:transparent;"); VBox.setVgrow(rsp, Priority.ALWAYS);

        VBox card = new VBox(head, flt, rsp); card.getStyleClass().add("jhub-card"); HBox.setHgrow(card, Priority.ALWAYS); card.setMinWidth(360);
        return card;
    }
    private static Region cluRow(String[] s) {
        Label fq = lbl(s[0], "jhub-clu-fq"); fq.setMinWidth(64);
        HBox top = new HBox(9, fq, lbl(s[1], "jhub-clu-call"));
        if (s[2].equals("y")) top.getChildren().add(lbl("NEED", "jhub-need"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS); top.getChildren().addAll(sp, lbl(s[7], "jhub-clu-age"));
        top.setAlignment(Pos.CENTER_LEFT);
        String tail = "de " + s[5] + (s[6].isEmpty() ? "" : "  · " + s[6]);
        HBox bot = new HBox(9, lbl(s[3], "jhub-clu-mode"), lbl(s[4], "jhub-clu-de"), lbl(tail, "jhub-clu-de"));
        bot.setAlignment(Pos.CENTER_LEFT); HBox.setMargin(bot, new Insets(3,0,0,0));
        VBox v = new VBox(0, top, bot); v.getStyleClass().add("jhub-clu");
        return v;
    }

    // ---- drawer rail (rig + rotor + space wx) ------------------------------
    private static Region railCol() {
        VBox col = new VBox(11, Shell.drawer("Rig control", "bridge", "bridge", "", true, rigBody()),
                Shell.drawer("Rotor control", "bridge", "bridge", "", true, rotorBody()),
                Shell.drawer("Space weather", "digi", "digi", "SFI 168 · K2", false, new VBox()));
        col.setMinWidth(322); col.setPrefWidth(322); col.setMaxWidth(322);
        return col;
    }
    private static Region rigBody() {
        HBox fq = new HBox(6, lbl("14.074.00", "jhub-rig-fq"), lbl("MHz", "jhub-rig-u")); fq.setAlignment(Pos.BOTTOM_LEFT);
        HBox chips = new HBox(8, lbl("USB ▾","jhub-pill"), lbl("20m","jhub-pill","grey"), lbl("VFO A","jhub-pill","grey"));
        HBox steps = new HBox(0); String[] st = {"‹","10","100","1k","5k","›"};
        for (int i=0;i<st.length;i++){ Label b = lbl(st[i], "jhub-step"); if (st[i].equals("1k")) b.getStyleClass().add("on"); HBox.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); HBox.setMargin(b,new Insets(0,2,0,2)); steps.getChildren().add(b); }
        // s-meter
        HBox seg = new HBox(2); seg.setAlignment(Pos.CENTER_LEFT);
        for (int i=0;i<16;i++){ Region s = new Region(); s.getStyleClass().addAll("jhub-sm-seg", i<9?"on":"off"); HBox.setHgrow(s,Priority.ALWAYS); s.setMaxWidth(Double.MAX_VALUE); seg.getChildren().add(s); }
        HBox scale = new HBox(); for (String k : new String[]{"S1","","","55","","59","","+20","","+40"}){ Label l=lbl(k,"jhub-sm-k"); HBox.setHgrow(l,Priority.ALWAYS); l.setMaxWidth(Double.MAX_VALUE); scale.getChildren().add(l);}
        VBox sm = new VBox(3, seg, scale);
        VBox pw = new VBox(2, lbl("POWER","jhub-pw-k"), lbl("92 W","jhub-pw-v"));
        Region psp = new Region(); HBox.setHgrow(psp, Priority.ALWAYS);
        VBox swr = new VBox(2, lbl("SWR","jhub-pw-k"), lbl("1.2:1","jhub-pw-v")); swr.setAlignment(Pos.CENTER_RIGHT);
        HBox power = new HBox(pw, psp, swr);
        GridPane bands = new GridPane(); bands.setHgap(5); bands.setVgap(5);
        for (int i=0;i<Mock.RIG_BANDS.length;i++){ Label b = lbl(Mock.RIG_BANDS[i], "jhub-band"); if (Mock.RIG_BANDS[i].equals("20")) b.getStyleClass().add("on"); GridPane.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); bands.add(b, i%5, i/5); }
        for (int c=0;c<5;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(20); bands.getColumnConstraints().add(cc); }
        return new VBox(12, fq, chips, steps, sm, power, bands);
    }
    private static Region rotorBody() {
        VBox info = new VBox(0); HBox.setHgrow(info, Priority.ALWAYS);
        HBox h = new HBox(2, lbl("045","sx-ro-head"), lbl("°","sx-ro-dir")); h.setAlignment(Pos.BOTTOM_LEFT);
        HBox turn = new HBox(5, btn("◀ CCW","sx-ro-turn-btn"), btn("Stop","sx-ro-turn-btn","stop"), btn("CW ▶","sx-ro-turn-btn"));
        for (Node n : turn.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        info.getChildren().addAll(h, lbl("NE · short path","sx-ro-dir"), turn); VBox.setMargin(turn, new Insets(10,0,0,0));
        HBox top = new HBox(13, Shell.compass(45, 88), info); top.setAlignment(Pos.CENTER_LEFT);
        String[][] pr = {{"Europe","50"},{"Africa","95"},{"S. Am","155"},{"Carib","135"},{"Asia","330"},{"Oceania","250"}};
        GridPane g = new GridPane(); g.setHgap(5); g.setVgap(5);
        for (int i=0;i<pr.length;i++){ Label b = btn(pr[i][0]+" "+pr[i][1]+"°", "sx-ro-preset"); GridPane.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); g.add(b,i%3,i/3); }
        for (int c=0;c<3;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(100/3.0); g.getColumnConstraints().add(cc); }
        return new VBox(11, top, g);
    }

    // ---- config page (shot 09) --------------------------------------------
    private static Region configCenter() {
        HBox crumb = new HBox(7, lbl("J-Hub","jhub-crumb"), lbl("›","jhub-crumb"), lbl("Hardware","jhub-crumb"),
                lbl("›","jhub-crumb"), lbl("Rig control","cfg-title")); crumb.setAlignment(Pos.BOTTOM_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox top = new HBox(crumb, sp, lbl("← Dashboard","cfg-link")); top.setAlignment(Pos.BOTTOM_LEFT);
        Label desc = lbl("CAT control for your transceiver — connection, PTT, and how J-Hub follows the radio.","cfg-desc");

        HBox cells = new HBox(0,
            statCell("STATUS","● CONNECTED","",true), statCell("FREQUENCY","14.074.00","MHz",false),
            statCell("MODE","USB","",false), statCell("POLL","12","ms",false));
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);

        VBox conn = section("CONNECTION",
            cfgRow("Transceiver model", null, select("Icom IC-7610")),
            cfgRow("Interface", null, seg(new String[]{"USB","Serial","Network"}, 0)),
            cfgRow("Port", null, select("/dev/cu.SLAB_USBtoUART")),
            cfgRow("Baud rate", null, select("9600")),
            cfgRow("CI-V address", "Icom civ address (hex)", input("0x98")),
            cfgRow("Poll interval", null, cfgSlider(0.15, "12 ms")));
        VBox ptt = section("PTT & KEYING",
            cfgRow("PTT method", null, seg(new String[]{"CAT","RTS","DTR","VOX"}, 0)),
            cfgRow("CW keyer", null, select("Winkeyer USB")),
            cfgRow("TX delay", null, cfgSlider(0.30, "30 ms")));
        VBox beh = section("BEHAVIOR", cfgRow("Auto band-follow", null, switchNode(true)));

        VBox v = new VBox(20, top, desc, cells, conn, ptt, beh);
        v.setPadding(new Insets(18, 28, 40, 28)); v.setMaxWidth(1080); v.setStyle("-fx-background-color:-ars-bg;");
        return v;
    }
    private static VBox statCell(String k, String val, String unit, boolean ok) {
        Label v = lbl(val, "cfg-sv"); if (ok) v.getStyleClass().add("ok");
        HBox vu = new HBox(4, v); vu.setAlignment(Pos.BOTTOM_LEFT); if (!unit.isEmpty()) vu.getChildren().add(lbl(unit, "cfg-sval-u"));
        VBox cell = new VBox(4, lbl(k, "cfg-sk"), vu); cell.setMinWidth(150); return cell;
    }
    private static VBox section(String name, Node... rows) {
        VBox card = new VBox(rows); card.getStyleClass().add("cfg-card");
        if (rows.length > 0) rows[rows.length-1].setStyle("-fx-border-width:0;");
        return new VBox(6, lbl(name, "cfg-sec"), card);
    }
    private static HBox cfgRow(String title, String hint, Node control) {
        VBox lblBox = new VBox(2, lbl(title, "cfg-lt")); if (hint != null) lblBox.getChildren().add(lbl(hint, "cfg-lh"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(20, lblBox, sp, control); row.getStyleClass().add("cfg-row"); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    private static Label select(String value) { Label l = lbl(value + "   ▾", "cfg-select"); l.setMinWidth(220); l.setAlignment(Pos.CENTER_LEFT); return l; }
    private static Label input(String value) { Label l = lbl(value, "cfg-input"); l.setMinWidth(220); l.setAlignment(Pos.CENTER_RIGHT); return l; }
    private static Region seg(String[] opts, int on) {
        HBox h = new HBox(2); h.getStyleClass().add("cfg-seg");
        for (int i=0;i<opts.length;i++){ Label b = lbl(opts[i], "cfg-seg-btn"); if (i==on) b.getStyleClass().add("on"); h.getChildren().add(b); }
        return h;
    }
    private static Region cfgSlider(double frac, String val) {
        double W = 220;
        Region track = new Region(); track.setStyle("-fx-background-color:-ars-surface-4;-fx-background-radius:99;"); track.setPrefSize(W,5); track.relocate(0,6);
        Region fill = new Region(); fill.setStyle("-fx-background-color:-ars-accent;-fx-background-radius:99;"); fill.setPrefSize(W*frac,5); fill.relocate(0,6);
        Region knob = new Region(); knob.setStyle("-fx-background-color:-ars-accent;-fx-background-radius:99;-fx-border-color:-ars-surface-1;-fx-border-width:2;-fx-border-radius:99;"); knob.setPrefSize(15,15); knob.relocate(W*frac-7,1);
        Pane bar = new Pane(track, fill, knob); bar.setMinSize(W,16); bar.setPrefSize(W,16); bar.setMaxSize(W,16);
        Label v = lbl(val, "cfg-sval-u"); v.setMinWidth(46); v.setAlignment(Pos.CENTER_RIGHT);
        HBox h = new HBox(14, bar, v); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
    private static Region switchNode(boolean on) {
        Region knob = new Region(); knob.getStyleClass().add("jm-switch-knob");
        StackPane sw = new StackPane(knob); sw.getStyleClass().add("jm-switch"); if (on) sw.getStyleClass().add("on");
        sw.setStyle(on ? "-fx-background-color:-ars-accent; -fx-border-color:-ars-accent;" : "");
        sw.setPadding(new Insets(0,3,0,3)); StackPane.setAlignment(knob, on ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); sw.setMaxSize(36,20);
        return sw;
    }

    // ---- small helpers -----------------------------------------------------
    private static Region chip(String cls, double sz){ Region r = new Region(); r.getStyleClass().add(cls); r.setMinSize(sz,sz); r.setPrefSize(sz,sz); r.setMaxSize(sz,sz); return r; }
    private static Label btn(String t, String... c){ Label l = lbl(t, c); l.setAlignment(Pos.CENTER); return l; }
    private static VBox labeled(String label, Region inp){ VBox v = new VBox(5, lbl(label,"jl-flabel"), inp); v.setAlignment(Pos.TOP_LEFT); return v; }
    private static Label input(String text, String cls, double w){ Label l = lbl(text, cls); if (w>0){ l.setMinWidth(w); l.setPrefWidth(w);} else { l.setMaxWidth(Double.MAX_VALUE);} l.setAlignment(Pos.CENTER_LEFT); return l; }
    private static HBox fieldRow(String[] labels, String[] vals, double[] widths, String firstClass) {
        HBox row = new HBox(12); row.setAlignment(Pos.BOTTOM_LEFT);
        for (int i=0;i<labels.length;i++){
            Label in = input(vals[i], "jhub-mini-field", widths[i]);
            if (i==0 && !firstClass.isEmpty()) in.getStyleClass().add("call");
            VBox f = labeled(labels[i], in);
            if (widths[i]<0){ HBox.setHgrow(f, Priority.ALWAYS); f.setMaxWidth(Double.MAX_VALUE); in.setMaxWidth(Double.MAX_VALUE);}
            row.getChildren().add(f);
        }
        return row;
    }
}
