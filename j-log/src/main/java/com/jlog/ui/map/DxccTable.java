package com.jlog.ui.map;

import com.jlog.ui.map.DxccMap.EntityInfo;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Continent-grouped DXCC entity list, designed to overlay a translucent
 * {@link DxccMap}. Rows are color-coded by worked status and emit
 * hover/click callbacks so the map underneath can highlight in sync.
 *
 * <p>The continent column doubles as a section grouping (sorted by
 * continent → prefix). Worked rows pick up the {@code dxcc-row-worked}
 * CSS class; the row currently under the mouse or selected picks up
 * {@code dxcc-row-current}.
 */
public class DxccTable extends TableView<DxccTable.Row> {

    /** Continent code order (matches CQ/IARU convention). */
    private static final Map<String, Integer> CONTINENT_ORDER = Map.of(
            "NA", 0, "SA", 1, "EU", 2, "AF", 3, "AS", 4, "OC", 5, "AN", 6);

    private static final Map<String, String> CONTINENT_NAMES = Map.of(
            "NA", "North America",
            "SA", "South America",
            "EU", "Europe",
            "AF", "Africa",
            "AS", "Asia",
            "OC", "Oceania",
            "AN", "Antarctica");

    private final Set<String> worked = new HashSet<>();
    private String currentPrefix;
    private Consumer<String> onRowClicked = id -> {};
    private Consumer<String> onRowHovered = id -> {};

    public DxccTable(Map<String, EntityInfo> entities) {
        getStyleClass().add("dxcc-table");
        setEditable(false);
        setPlaceholder(new javafx.scene.control.Label("No DXCC entities loaded"));

        // Translucent background so the map underneath shows through.
        // Real opacity is set on the cells/rows via CSS — root just doesn't
        // paint a solid fill of its own.
        setStyle("-fx-background-color: transparent;");

        TableColumn<Row, String> cContinent = new TableColumn<>("Continent");
        cContinent.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().continentLabel));
        cContinent.setPrefWidth(120);

        TableColumn<Row, String> cPrefix = new TableColumn<>("Prefix");
        cPrefix.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().prefix));
        cPrefix.setPrefWidth(70);

        TableColumn<Row, String> cName = new TableColumn<>("Entity");
        cName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name));
        cName.setPrefWidth(220);

        getColumns().addAll(cContinent, cPrefix, cName);

        // Sort by continent (custom order), then prefix.
        ObservableList<Row> rows = FXCollections.observableArrayList();
        for (EntityInfo info : entities.values()) {
            rows.add(new Row(info));
        }
        rows.sort(Comparator
                .<Row>comparingInt(r -> CONTINENT_ORDER.getOrDefault(r.continent, 99))
                .thenComparing(r -> r.prefix));
        setItems(rows);

        // Color rows by worked / current state, and emit callbacks.
        setRowFactory(tv -> {
            TableRow<Row> tr = new TableRow<>() {
                @Override protected void updateItem(Row item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("dxcc-row-worked", "dxcc-row-current");
                    if (empty || item == null) return;
                    if (item.prefix.equals(currentPrefix)) getStyleClass().add("dxcc-row-current");
                    else if (worked.contains(item.prefix)) getStyleClass().add("dxcc-row-worked");
                }
            };
            tr.setOnMouseEntered(ev -> {
                Row r = tr.getItem();
                if (r != null) onRowHovered.accept(r.prefix);
            });
            tr.setOnMouseClicked(ev -> {
                Row r = tr.getItem();
                if (r != null) onRowClicked.accept(r.prefix);
            });
            return tr;
        });
    }

    public void setAllWorked(Collection<String> ids) {
        worked.clear();
        if (ids != null) worked.addAll(ids);
        refresh();
    }

    /** Highlight a single entity and scroll it into view. No-op if absent. */
    public void setCurrent(String prefix) {
        this.currentPrefix = prefix;
        refresh();
        if (prefix == null) return;
        for (int i = 0; i < getItems().size(); i++) {
            if (prefix.equals(getItems().get(i).prefix)) {
                scrollTo(Math.max(0, i - 3));
                return;
            }
        }
    }

    public void setOnRowClicked(Consumer<String> cb) { this.onRowClicked = cb != null ? cb : id -> {}; }
    public void setOnRowHovered(Consumer<String> cb) { this.onRowHovered = cb != null ? cb : id -> {}; }

    /** Backing row model. */
    public static class Row {
        final String prefix;
        final String name;
        final String continent;
        final String continentLabel;
        Row(EntityInfo info) {
            this.prefix = info.prefix();
            this.name = info.name();
            this.continent = info.continent();
            this.continentLabel = CONTINENT_NAMES.getOrDefault(continent, continent);
        }
    }

    /** Continent-ordered prefix list (used by callers that want to iterate rows). */
    public static Map<String, EntityInfo> sortedByContinent(Map<String, EntityInfo> entities) {
        Map<String, EntityInfo> out = new LinkedHashMap<>();
        entities.values().stream()
                .sorted(Comparator
                        .<EntityInfo>comparingInt(e -> CONTINENT_ORDER.getOrDefault(e.continent(), 99))
                        .thenComparing(EntityInfo::prefix))
                .forEach(e -> out.put(e.prefix(), e));
        return out;
    }
}
