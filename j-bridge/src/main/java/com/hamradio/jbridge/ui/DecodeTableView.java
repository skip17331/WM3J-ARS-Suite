package com.hamradio.jbridge.ui;

import com.hamradio.jbridge.model.WsjtxDecode;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * DecodeTableView — scrollable WSJT-X decode list.
 * Columns: Time | dB | DT | Freq | Message | Country | Brg | Dist | ✓
 * Row coloring via CSS classes: row-worked, row-needed, row-cq.
 */
public class DecodeTableView extends VBox {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC);

    private final TableView<WsjtxDecode>      table;
    private final ObservableList<WsjtxDecode> items = FXCollections.observableArrayList();

    private int     maxHistory = 200;
    private boolean autoScroll = true;

    @SuppressWarnings("unchecked")
    public DecodeTableView() {
        table = new TableView<>(items);
        table.setPlaceholder(new Label("Waiting for WSJT-X\u2026"));
        table.getStyleClass().add("decode-table");

        TableColumn<WsjtxDecode, String> colTime = col("Time", 52,
                d -> d.getTimestamp() != null ? TIME_FMT.format(d.getTimestamp()) : "----");
        TableColumn<WsjtxDecode, String> colSnr  = col("dB", 40,
                d -> String.format("%+d", d.getSnr()));
        TableColumn<WsjtxDecode, String> colDt   = col("DT", 38,
                d -> String.format("%.1f", d.getDeltaTime()));
        TableColumn<WsjtxDecode, String> colFreq = col("Freq", 48,
                d -> String.valueOf(d.getDeltaFrequency()));
        TableColumn<WsjtxDecode, String> colMsg  = col("Message", 230,
                d -> d.getMessage() != null ? d.getMessage() : "");
        TableColumn<WsjtxDecode, String> colCtry = col("Country", 120,
                d -> d.getCountry() != null ? d.getCountry() : "");
        TableColumn<WsjtxDecode, String> colBrg  = col("Brg\u00b0", 46,
                d -> d.getDistanceKm() > 0 ? String.format("%03.0f\u00b0", d.getBearing()) : "");
        TableColumn<WsjtxDecode, String> colDist = col("Dist", 60,
                d -> d.getDistanceKm() > 0 ? String.format("%.0f km", d.getDistanceKm()) : "");
        TableColumn<WsjtxDecode, String> colStar = col("\u2713", 26,
                d -> switch (d.getWorkedStatus()) {
                    case "worked" -> "\u2713";
                    case "needed" -> "\u2605";
                    default       -> "\u00b7";
                });

        table.getColumns().addAll(colTime, colSnr, colDt, colFreq, colMsg,
                                  colCtry, colBrg, colDist, colStar);

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(WsjtxDecode item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-worked", "row-needed", "row-cq");
                if (empty || item == null) return;
                switch (item.getWorkedStatus()) {
                    case "worked" -> getStyleClass().add("row-worked");
                    case "needed" -> getStyleClass().add("row-needed");
                }
                if (item.isCqCall()) getStyleClass().add("row-cq");
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
    }

    public void addDecode(WsjtxDecode decode) {
        items.add(0, decode);
        if (items.size() > maxHistory) items.remove(maxHistory, items.size());
        if (autoScroll) table.scrollTo(0);
    }

    public void clear()                    { items.clear(); }
    public void setMaxHistory(int n)       { this.maxHistory = n; }
    public void setAutoScroll(boolean v)   { this.autoScroll = v; }
    public TableView<WsjtxDecode> getTable() { return table; }

    private TableColumn<WsjtxDecode, String> col(String header, double width,
            java.util.function.Function<WsjtxDecode, String> fn) {
        TableColumn<WsjtxDecode, String> c = new TableColumn<>(header);
        c.setPrefWidth(width);
        c.setSortable(false);
        c.setCellValueFactory(cell -> new SimpleStringProperty(fn.apply(cell.getValue())));
        return c;
    }
}
