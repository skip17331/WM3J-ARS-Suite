package com.ars.fx.surface;

import com.ars.fx.data.ContestConfigBridge;
import com.ars.fx.data.ContestState;
import com.ars.fx.shell.Shell;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.PluginLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** Contest chooser — pick which contest to log. Lists the engine's bundled plugins. */
public final class ContestPicker {
    private ContestPicker() {}

    public static Region build() {
        ContestConfigBridge.bootstrap();
        Region dock = Shell.dock("log");
        String utc = java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        HBox top = Shell.topBar("log", "log", "J-Log", "Contest · choose a contest",
                new String[][]{{"CONTESTS", String.valueOf(plugins().size())}}, utc);

        TextField search = new TextField(); search.setPromptText("Search contests…");
        search.getStyleClass().add("jhub-mini-field"); search.setMaxWidth(360);
        Label normal = lbl("← Normal log", "jl-clearbtn"); normal.setStyle("-fx-cursor:hand;");
        normal.setOnMouseClicked(e -> Shell.navigate("log"));
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = new HBox(12, lbl("Choose a contest", "jhub-card-title"), gap, search, normal);
        head.setAlignment(Pos.CENTER_LEFT); head.setPadding(new Insets(14, 18, 8, 18));

        VBox list = new VBox(7); list.setPadding(new Insets(0, 18, 18, 18));
        fill(list, "");
        search.textProperty().addListener((o, a, b) -> fill(list, b));

        ScrollPane sp = new ScrollPane(list); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);
        VBox center = new VBox(0, head, sp); VBox.setVgrow(sp, Priority.ALWAYS);

        Region rail = Shell.rail(Shell.leadDrawers(0).toArray(new Node[0]));
        return (Region) Shell.frame(dock, top, center, rail);
    }

    private static void fill(VBox list, String q) {
        list.getChildren().clear();
        String needle = q == null ? "" : q.trim().toLowerCase();
        int n = 0;
        for (ContestPlugin p : plugins()) {
            String hay = (p.getContestName() + " " + p.getContestId()).toLowerCase();
            if (!needle.isEmpty() && !hay.contains(needle)) continue;
            list.getChildren().add(card(p));
            n++;
        }
        if (n == 0) list.getChildren().add(lbl("No contests match \"" + q + "\"", "jl-cp-empty"));
    }

    private static Node card(ContestPlugin p) {
        Label name = lbl(p.getContestName(), "jl-skim-cl"); HBox.setHgrow(name, Priority.ALWAYS); name.setMaxWidth(Double.MAX_VALUE);
        Label id = lbl(p.getContestId(), "jl-skim-meta");
        VBox txt = new VBox(2, name, lbl(p.getExchangeFormat() == null ? "" : p.getExchangeFormat(), "jl-skim-meta"));
        HBox.setHgrow(txt, Priority.ALWAYS); txt.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(11, Shell.iconTile("log", "log", 13, "sx-dw-ic"), txt, id);
        row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jl-pick");
        row.setStyle("-fx-cursor:hand;-fx-background-color:-ars-surface-1;-fx-background-radius:8;-fx-border-color:-ars-border;-fx-border-width:1;-fx-border-radius:8;-fx-padding:9 12;");
        row.setOnMouseClicked(e -> { ContestState.setActive(p.getContestId()); Shell.navigate("logc"); });
        return row;
    }

    private static List<ContestPlugin> plugins() {
        try {
            List<ContestPlugin> ps = new ArrayList<>(PluginLoader.getInstance().getAvailablePlugins());
            ps.sort((a, b) -> a.getContestName().compareToIgnoreCase(b.getContestName()));
            return ps;
        } catch (Exception e) { return List.of(); }
    }
}
