package com.hamclock.ui.windows;

import com.hamclock.app.ServiceRegistry;
import com.hamclock.service.contest.Contest;
import com.hamclock.service.contest.ContestList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Floating contest calendar window.
 * Font sizes use em — scales with root font size setting.
 */
public class ContestListWindow extends FloatingWindow {

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("MM/dd HH:mm'z'").withZone(ZoneOffset.UTC);

    private final ServiceRegistry services;
    public ContestListWindow(ServiceRegistry services) {
        super("🏆  CONTEST CALENDAR", 300);
        this.services = services;
        update();
    }

    @Override
    public void update() {
        contentBox.getChildren().clear();

        ContestList list = services.contestListProvider.getCached();
        if (list == null) {
            contentBox.getChildren().add(muted("No contest data"));
            return;
        }

        List<Contest> all = new ArrayList<>(list.getActive());
        all.addAll(list.getUpcoming());

        if (all.isEmpty()) {
            contentBox.getChildren().add(muted("No contests in next 24h"));
            return;
        }

        for (Contest c : all) {
            contentBox.getChildren().add(buildRow(c));
        }

        contentBox.getChildren().add(muted("Updated " + TIME_FMT.format(list.getFetchedAt())));
    }

    private VBox buildRow(Contest c) {
        VBox row = new VBox(1);
        row.setPadding(new Insets(4, 0, 4, 0));
        row.setStyle("-fx-border-color: #1e2d50; -fx-border-width: 0 0 1 0;");

        boolean active = c.isActive();
        String nameColor = active ? "#00cc66" : "#ccd6f6";
        String prefix    = active ? "▶ " : "  ";

        Label name = new Label(prefix + c.getName());
        name.setStyle("-fx-font-size: 0.85em; -fx-font-weight: bold; -fx-text-fill: " + nameColor + ";");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);

        Label time = new Label(TIME_FMT.format(c.getStartTime()) + " – " + TIME_FMT.format(c.getEndTime()));
        time.setStyle("-fx-font-size: 0.69em; -fx-text-fill: #4a5580;");

        Label info = new Label(c.getModes() + "  " + c.getBands());
        info.setStyle("-fx-font-size: 0.69em; -fx-text-fill: #2a7fff;");

        row.getChildren().addAll(name, time, info);
        return row;
    }

    private Label muted(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #4a5580;");
        return l;
    }
}
