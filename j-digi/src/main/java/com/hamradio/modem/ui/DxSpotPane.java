package com.hamradio.modem.ui;

import com.hamradio.modem.ModemService;
import com.hamradio.modem.model.HubSpot;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * DX spot table received from j-hub.
 * Double-clicking a row fires the onSpotSelected callback (→ prefills LogEntryPane).
 */
public class DxSpotPane extends VBox {

    private static final int MAX_SPOTS = 200;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ObservableList<HubSpot> spots = FXCollections.observableArrayList();
    private Consumer<HubSpot> onSpotSelected = s -> {};

    public DxSpotPane(ModemService service) {
        setSpacing(4);
        setPadding(new Insets(6));

        TableView<HubSpot> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Label header = new Label("DX Spots");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> spots.clear());

        javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(8, header, clearBtn);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        getChildren().addAll(titleRow, table);

        // Receive new spots from service
        service.setSpotListener(spot ->
            javafx.application.Platform.runLater(() -> {
                if (spots.size() >= MAX_SPOTS) spots.remove(spots.size() - 1);
                spots.add(0, spot);
            }));

        // Receive spot-selected from other apps (e.g. j-log)
        service.setSpotSelectedListener(spot ->
            javafx.application.Platform.runLater(() -> onSpotSelected.accept(spot)));

        // Seed with any spots already cached
        spots.addAll(service.getSpots());
    }

    public void setOnSpotSelected(Consumer<HubSpot> cb) {
        this.onSpotSelected = cb != null ? cb : s -> {};
    }

    // ---------------------------------------------------------------
    // Table construction
    // ---------------------------------------------------------------

    private TableView<HubSpot> buildTable() {
        TableView<HubSpot> table = new TableView<>(spots);
        table.setPlaceholder(new Label("No spots yet — connect to hub"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getColumns().addAll(
            col("DX",      120, s -> s.spotted != null ? s.spotted : ""),
            col("Freq",    80,  s -> s.frequency > 0
                                      ? "%.1f".formatted(s.frequency / 1000.0)
                                      : ""),
            col("Mode",    55,  s -> s.mode != null ? s.mode : ""),
            col("Country", 110, s -> s.country != null ? s.country : ""),
            col("Time",    50,  s -> s.timestamp != null ? formatTime(s.timestamp) : "")
        );

        // Double-click → prefill + switch tab
        table.setRowFactory(tv -> {
            TableRow<HubSpot> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()
                        && e.getClickCount() == 2
                        && e.getButton() == MouseButton.PRIMARY) {
                    onSpotSelected.accept(row.getItem());
                }
            });
            return row;
        });

        return table;
    }

    @SuppressWarnings("unchecked")
    private TableColumn<HubSpot, String> col(String title,
                                              double prefWidth,
                                              java.util.function.Function<HubSpot, String> fn) {
        TableColumn<HubSpot, String> c = new TableColumn<>(title);
        c.setPrefWidth(prefWidth);
        c.setCellValueFactory(cd -> new SimpleStringProperty(fn.apply(cd.getValue())));
        return c;
    }

    private String formatTime(String iso) {
        try {
            return LocalDateTime.ofInstant(Instant.parse(iso), ZoneId.systemDefault())
                                .format(TIME_FMT);
        } catch (Exception e) {
            return "";
        }
    }
}
