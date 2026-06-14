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

/** J-Vault station inventory & estate planning (handoff shot 07), mock data. */
public final class JVaultView {
    private JVaultView() {}

    public static Region build() {
        Region dock = Shell.dock("vault");
        String[][] stats = {{"ITEMS","12"},{"TOTAL VALUE","$18,900","vault"},{"ASSIGNED","9/12"}};
        HBox top = Shell.topBar("vault","vault","J-Vault","Station inventory & estate planning", stats, null);
        VBox backup = new VBox(0, lbl("Last backup","sx-clock-k"), lbl("2026-06-12","sx-clock-v")); backup.setAlignment(Pos.CENTER_RIGHT);
        top.getChildren().add(backup);

        VBox center = new VBox(18, summary(), searchRow(), table());
        center.setPadding(new Insets(18, 24, 24, 24)); center.setStyle("-fx-background-color:-ars-bg;");
        ScrollPane sp = new ScrollPane(center); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setStyle("-fx-background-color:-ars-bg;");
        HBox.setHgrow(sp, Priority.ALWAYS);
        Region rail = Shell.rail(railDrawers());
        return Shell.frame(dock, top, sp, rail);
    }

    private static Region summary() {
        HBox h = new HBox(16);
        for (String[] s : Mock.VAULT_SUMMARY) {
            Label v = lbl(s[1], "jv-sum-v"); if (s[3].equals("y")) v.getStyleClass().add("amber");
            HBox vu = new HBox(2, v); vu.setAlignment(Pos.BOTTOM_LEFT); if (!s[2].isEmpty()) vu.getChildren().add(lbl(s[2], "jv-sum-v-of"));
            VBox card = new VBox(6, lbl(s[0], "jv-sum-k"), vu); card.getStyleClass().add("jv-sum");
            HBox.setHgrow(card, Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE);
            h.getChildren().add(card);
        }
        return h;
    }
    private static Region searchRow() {
        Label search = lbl("🔍  Search inventory…", "jv-search"); search.setMinWidth(220);
        HBox cats = new HBox(7); cats.setAlignment(Pos.CENTER_LEFT);
        for (int i=0;i<Mock.VAULT_CATS.length;i++){ Label c = lbl(Mock.VAULT_CATS[i], "jv-cat"); if (i==0) c.getStyleClass().add("on"); cats.getChildren().add(c); }
        HBox row = new HBox(12, search, cats); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static final double[] VC = {112, -1, 92, 112, 230};
    private static Region table() {
        VBox tbl = new VBox();
        tbl.getChildren().add(headerRow());
        for (String[] it : Mock.VAULT_ITEMS) tbl.getChildren().add(itemRow(it));
        return tbl;
    }
    private static Region headerRow() {
        HBox r = new HBox(12); r.getStyleClass().add("jv-th"); r.setAlignment(Pos.CENTER_LEFT);
        String[] cols = {"CATEGORY","ITEM","VALUE","CONDITION","DISPOSITION"};
        for (int i=0;i<cols.length;i++) r.getChildren().add(cell(lbl(cols[i]), i));
        return r;
    }
    private static Region itemRow(String[] it) {
        HBox r = new HBox(12); r.getStyleClass().add("jv-row"); r.setAlignment(Pos.CENTER_LEFT);
        VBox item = new VBox(1, lbl(it[1],"jv-item-nm"), lbl(it[2],"jv-item-sub"));
        Label cond = lbl(it[4], "jv-cond"); if (!it[4].equals("good")) cond.getStyleClass().add(it[4]);
        Region dot = new Region(); dot.getStyleClass().add("jv-disp-dot"); dot.setStyle("-fx-background-color:"+it[5]+";");
        HBox disp = new HBox(8, dot, lbl(it[6],"jv-disp")); disp.setAlignment(Pos.CENTER_LEFT);
        r.getChildren().addAll(cell(catTag(it[0]),0), cell(item,1), cell(lbl(it[3],"jv-val"),2), cell(cond,3), cell(disp,4));
        return r;
    }
    private static Label catTag(String c){ Label l = lbl(c,"jv-cat-tag"); return l; }
    private static Region cell(Node n, int i) {
        HBox c = new HBox(n); c.setAlignment(Pos.CENTER_LEFT);
        if (i==0) c.setAlignment(Pos.CENTER_LEFT);
        if (VC[i] < 0){ HBox.setHgrow(c, Priority.ALWAYS); c.setMaxWidth(Double.MAX_VALUE);} else { c.setMinWidth(VC[i]); c.setPrefWidth(VC[i]); c.setMaxWidth(VC[i]); }
        return c;
    }

    private static Node[] railDrawers() {
        // Item detail
        VBox detail = new VBox(11);
        detail.getChildren().add(new VBox(1, lbl("Icom IC-7610","jv-dname"), lbl("Transceiver · 2021","jv-dsub")));
        GridPane g = new GridPane(); g.setHgap(20); g.setVgap(11);
        g.add(dkv("VALUE","$2,800",false),0,0); g.add(dkv("CONDITION","excellent",true),1,0);
        g.add(dkv("SERIAL","0203145",false),0,1); g.add(dkv("YEAR","2021",false),1,1);
        for (int c=0;c<2;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(50); g.getColumnConstraints().add(cc); }
        detail.getChildren().add(g);
        Label note = lbl("Primary HF rig. Manual + box in closet.","jv-note"); note.setWrapText(true); note.setMaxWidth(Double.MAX_VALUE);
        detail.getChildren().add(note);
        VBox dispBox = new VBox(3, lbl("ESTATE DISPOSITION","jv-dispbox-k"), lbl("Son — Alex (KC2ABC)","jv-dispbox-v")); dispBox.getStyleClass().add("jv-dispbox");
        detail.getChildren().add(dispBox);
        // Estate planning
        VBox docs = new VBox();
        for (String[] d : Mock.VAULT_DOCS) {
            Label icon = lbl("📄"); icon.setStyle("-fx-font-size:16px;");
            VBox txt = new VBox(1, lbl(d[0],"jv-doc-t"), lbl(d[1],"jv-doc-s")); HBox.setHgrow(txt, Priority.ALWAYS);
            HBox row = new HBox(11, icon, txt, lbl(d[2],"jv-doc-d")); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jv-doc");
            docs.getChildren().add(row);
        }
        return new Node[]{
            Shell.drawer("Item detail","vault","vault","", true, detail),
            Shell.drawer("Estate planning","learn","learn","", true, docs),
            Shell.drawer("Disposition summary","sat","sat","9 assigned", false, new VBox()),
        };
    }
    private static VBox dkv(String k, String v, boolean ex){ Label vl = lbl(v,"jv-dv"); if (ex) vl.getStyleClass().add("excellent"); return new VBox(2, lbl(k,"jv-dk"), vl); }
}
