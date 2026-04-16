package com.hamradio.digitalbridge.ui;

import com.hamradio.digitalbridge.model.WsjtxDecode;
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
 *
 * Columns: Time | dB | DT | Freq | Message | Country | Brg | Dist | ✓
 *
 * Colour coding (matches j-log DxSpot workedStatus convention):
 *   worked   → #a6e3a1 green   (Catppuccin green, same as j-hub status panel)
 *   needed   → #f38ba8 red     (Catppuccin red)
 *   unknown  → #cdd6f4 default (Catppuccin text)
 *   CQ calls → bold
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
        table.setStyle("-fx-background-color: #1e1e2e;");
        table.setPlaceholder(new Label("Waiting for WSJT-X…"));
        table.getStyleClass().add("decode-table");

        // ── Column definitions ────────────────────────────────────────────────

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
        TableColumn<WsjtxDecode, String> colBrg  = col("Brg°", 46,
                d -> d.getDistanceKm() > 0
                        ? String.format("%03.0f°", d.getBearing()) : "");
        TableColumn<WsjtxDecode, String> colDist = col("Dist", 60,
                d -> d.getDistanceKm() > 0
                        ? String.format("%.0f km", d.getDistanceKm()) : "");
        TableColumn<WsjtxDecode, String> colStar = col("✓", 26,
                d -> switch (d.getWorkedStatus()) {
                    case "worked"  -> "✓";
                    case "needed"  -> "★";
                    default        -> "·";
                });

        table.getColumns().addAll(colTime, colSnr, colDt, colFreq, colMsg,
                                  colCtry, colBrg, colDist, colStar);

        // ── Row factory — colour coding ────────────────────────────────────────

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(WsjtxDecode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }

                // Base style
                String fg = switch (item.getWorkedStatus()) {
                    case "worked" -> "#a6e3a1"; // Catppuccin green
                    case "needed" -> "#f38ba8"; // Catppuccin red
                    default       -> "#cdd6f4"; // Catppuccin text
                };
                String bold = item.isCqCall() ? "-fx-font-weight: bold;" : "";
                setStyle("-fx-text-fill: " + fg + "; " + bold);
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
        setStyle("-fx-background-color: #1e1e2e;");
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Add a decode row to the top of the list. Must be on FX thread. */
    public void addDecode(WsjtxDecode decode) {
        items.add(0, decode);
        if (items.size() > maxHistory) items.remove(maxHistory, items.size());
        if (autoScroll) table.scrollTo(0);
    }

    /** Remove all entries. */
    public void clear() { items.clear(); }

    public void setMaxHistory(int n)      { this.maxHistory = n; }
    public void setAutoScroll(boolean v)  { this.autoScroll = v; }
    public TableView<WsjtxDecode> getTable() { return table; }

    // ── Helper ────────────────────────────────────────────────────────────────

    private TableColumn<WsjtxDecode, String> col(String header, double width,
            java.util.function.Function<WsjtxDecode, String> fn) {
        TableColumn<WsjtxDecode, String> c = new TableColumn<>(header);
        c.setPrefWidth(width);
        c.setSortable(false);
        c.setCellValueFactory(cell -> new SimpleStringProperty(fn.apply(cell.getValue())));
        return c;
    }
}
