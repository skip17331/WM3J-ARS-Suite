package com.ars.fx.surface;

import com.ars.fx.shell.AutoPanel;
import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

import static com.ars.fx.shell.Shell.lbl;

/**
 * J-Hub ▸ Setup — every settings page on one screen. A second left menu (the same hover-open, pinnable drawer J-Learn uses
 * for its chapters) lists the sections in groups; choosing one swaps the page on the right without leaving the screen.
 */
public final class JHubSetup {
    private JHubSetup() {}

    private record Item(String id, String label) {}
    private record Group(String title, Item... items) {}

    /** The menu, in the order an operator sets a station up: who you are, the radio gear, where data comes from, the log, the app. */
    private static final Group[] GROUPS = {
        new Group("STATION",
            new Item("station", "Station")),
        new Group("RADIO",
            new Item("rig", "Rig control"), new Item("rotor", "Rotor control"), new Item("amp", "Amplifier"),
            new Item("antsw", "Antenna switch"), new Item("tx", "Transmit & macros")),
        new Group("DATA & SPOTTING",
            new Item("cluster", "DX cluster & RBN"), new Item("digital", "WSJT-X / digital"),
            new Item("lookup", "Callsign lookup"), new Item("sat", "Satellites")),
        new Group("LOGBOOK",
            new Item("logdb", "Log database & backup"), new Item("upload", "Online upload"),
            new Item("export", "Export & Cabrillo")),
        new Group("SYSTEM",
            new Item("modules", "Modules"), new Item("share", "Remote sharing"), new Item("learn", "J-Learn content")),
    };

    private static boolean known(String id) {
        if (id == null) return false;
        for (Group g : GROUPS) for (Item i : g.items()) if (i.id().equals(id)) return true;
        return false;
    }

    /** The Setup screen, opened on {@code initial} (a section id from the menu; anything else opens Station). */
    public static Region build(String initial) {
        String[] cur = {known(initial) ? initial : "station"};
        Region dock = Shell.dock("hub", JHubDashboard.hubConfigSection("setup"));   // null in a solo window

        StackPane content = new StackPane(); content.setAlignment(Pos.TOP_LEFT); HBox.setHgrow(content, Priority.ALWAYS);
        content.setStyle("-fx-background-color:-ars-bg;");
        StackPane menuHolder = new StackPane(); menuHolder.setAlignment(Pos.TOP_LEFT);

        Runnable[] rebuildMenu = new Runnable[1];
        Runnable showPage = () -> {
            ScrollPane sp = new ScrollPane(JHubDashboard.setupPage(cur[0]));
            sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle("-fx-background-color:-ars-bg;");
            content.getChildren().setAll(sp);
        };
        Consumer<String> select = id -> { cur[0] = id; showPage.run(); rebuildMenu[0].run(); };
        rebuildMenu[0] = () -> menuHolder.getChildren().setAll(
                AutoPanel.wrap("setup", false, collapsedRail(), menu(cur[0], select, rebuildMenu[0]), 252, () -> false));
        rebuildMenu[0].run();
        showPage.run();

        HBox row = new HBox(); row.setFillHeight(true); row.setMinHeight(0); VBox.setVgrow(row, Priority.ALWAYS);
        if (dock != null) row.getChildren().add(dock);
        row.getChildren().addAll(menuHolder, content);
        VBox root = new VBox(JHubDashboard.workspaceBar(), row);
        root.setStyle("-fx-background-color:-ars-bg;"); root.setMinHeight(0);
        return root;
    }

    private static Region menu(String selected, Consumer<String> select, Runnable rebuild) {
        VBox p = new VBox(0); p.getStyleClass().add("j2-topics");
        p.setMinWidth(252); p.setPrefWidth(252); p.setMaxWidth(252);
        Label title = lbl("SETUP", "j2-sec"); HBox.setHgrow(title, Priority.ALWAYS); title.setMaxWidth(Double.MAX_VALUE);
        HBox header = new HBox(8, title, AutoPanel.pinButton("setup", rebuild));
        header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(2, 6, 6, 2));
        p.getChildren().add(header);

        if (Shell.solo) {   // a solo window has no dock to go back through
            Label back = lbl("← Back to " + com.ars.fx.data.SoloConfig.name(Shell.soloModule), "jhub-nav-sub");
            back.setStyle("-fx-cursor:hand;");
            back.setOnMouseClicked(e -> Shell.navigate(Shell.soloModule));
            p.getChildren().add(back);
        }

        VBox list = new VBox(2);
        for (Group g : GROUPS) {
            list.getChildren().add(lbl(g.title(), "jhs-group"));
            for (Item it : g.items()) {
                Label l = lbl(it.label(), "jhs-item");
                l.setMaxWidth(Double.MAX_VALUE);
                if (it.id().equals(selected)) l.getStyleClass().add("on");
                l.setOnMouseClicked(e -> select.accept(it.id()));
                list.getChildren().add(l);
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
        Label vlabel = lbl("SETUP", "j2-sec"); vlabel.setRotate(90);
        javafx.scene.Group vgroup = new javafx.scene.Group(vlabel);   // sized to the rotated label, so it can't spill over the dock
        VBox.setMargin(vgroup, new Insets(30, 0, 0, 0));
        p.getChildren().addAll(expand, vgroup);
        return p;
    }
}
