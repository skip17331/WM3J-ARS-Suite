package com.hamradio.jhub.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Read-only SQLite browser for the three databases owned by the logging side
 * of the suite ({@code ~/.j-log/j-log.db}, {@code contest.db}, {@code config.db}).
 * Opens connections with {@code mode=ro} so j-hub can never corrupt a log that
 * j-log / j-digi have open for writing. Intended for debugging, ad-hoc queries,
 * and "what did I record last weekend" browsing without leaving the hub UI.
 */
public class DbBrowser {

    private static final Logger log = LoggerFactory.getLogger(DbBrowser.class);

    private final Stage   owner;
    private final Stage   dialog = new Stage();
    private final ComboBox<String> dbSelect    = new ComboBox<>();
    private final ComboBox<String> tableSelect = new ComboBox<>();
    private final TableView<List<String>> resultTable = new TableView<>();
    private final TextArea sqlArea = new TextArea();
    private final Label    status  = new Label("");
    private final Path     dataDir = Paths.get(System.getProperty("user.home"), ".j-log");

    public DbBrowser(Stage owner) { this.owner = owner; }

    public void show() {
        dialog.initOwner(owner);
        dialog.setTitle("j-Hub DB Browser (read-only)");

        dbSelect.setItems(FXCollections.observableArrayList("j-log.db", "contest.db", "config.db"));
        dbSelect.setOnAction(e -> onDbSelect());

        tableSelect.setOnAction(e -> onTableSelect());
        tableSelect.setPrefWidth(200);

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> onTableSelect());

        HBox top = new HBox(8,
            new Label("Database:"), dbSelect,
            new Label("Table:"),    tableSelect,
            refresh);
        top.setPadding(new Insets(8));

        sqlArea.setPrefRowCount(3);
        sqlArea.setPromptText("Optional: paste a SELECT query (read-only, SELECT only)");
        sqlArea.setStyle("-fx-font-family: monospace;");

        Button runSql = new Button("Run SELECT");
        runSql.setOnAction(e -> runCustomSql());

        Button clearSql = new Button("Clear");
        clearSql.setOnAction(e -> { sqlArea.clear(); onTableSelect(); });

        HBox sqlBar = new HBox(8, runSql, clearSql, status);
        sqlBar.setPadding(new Insets(0, 8, 8, 8));

        resultTable.setPlaceholder(new Label("Select a database and a table, or write a SELECT above."));

        VBox root = new VBox(4, top, sqlArea, sqlBar, resultTable);
        VBox.setVgrow(resultTable, Priority.ALWAYS);

        dialog.setScene(new Scene(root, 900, 600));
        dialog.show();

        dbSelect.getSelectionModel().selectFirst();
    }

    // -----------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------

    private void onDbSelect() {
        String db = dbSelect.getValue();
        if (db == null) return;
        try (Connection c = openReadOnly(db)) {
            List<String> tables = listTables(c);
            tableSelect.setItems(FXCollections.observableArrayList(tables));
            if (!tables.isEmpty()) tableSelect.getSelectionModel().selectFirst();
            status.setText(tables.size() + " table" + (tables.size() == 1 ? "" : "s"));
        } catch (SQLException e) {
            status.setText("open failed: " + e.getMessage());
            tableSelect.setItems(FXCollections.observableArrayList());
        }
    }

    private void onTableSelect() {
        String db    = dbSelect.getValue();
        String table = tableSelect.getValue();
        if (db == null || table == null) return;
        runQuery(db, "SELECT * FROM \"" + table + "\" LIMIT 1000");
    }

    private void runCustomSql() {
        String sql = sqlArea.getText() == null ? "" : sqlArea.getText().trim();
        if (sql.isEmpty()) { onTableSelect(); return; }
        if (!sql.toLowerCase().startsWith("select") && !sql.toLowerCase().startsWith("pragma")) {
            new Alert(AlertType.WARNING,
                "Only SELECT (and PRAGMA) queries are allowed in the read-only browser.").showAndWait();
            return;
        }
        String db = dbSelect.getValue();
        if (db == null) return;
        runQuery(db, sql);
    }

    // -----------------------------------------------------------------
    // Query execution
    // -----------------------------------------------------------------

    private void runQuery(String db, String sql) {
        try (Connection c = openReadOnly(db);
             Statement  s = c.createStatement();
             ResultSet  rs = s.executeQuery(sql)) {

            resultTable.getColumns().clear();
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 0; i < cols; i++) {
                final int idx = i;
                TableColumn<List<String>, String> col = new TableColumn<>(md.getColumnLabel(i + 1));
                col.setCellValueFactory(cell -> new SimpleStringProperty(
                    idx < cell.getValue().size() ? cell.getValue().get(idx) : ""));
                col.setPrefWidth(120);
                resultTable.getColumns().add(col);
            }

            ObservableList<List<String>> rows = FXCollections.observableArrayList();
            int rowCount = 0;
            while (rs.next()) {
                List<String> row = new ArrayList<>(cols);
                for (int i = 1; i <= cols; i++) {
                    String v = rs.getString(i);
                    row.add(v == null ? "" : v);
                }
                rows.add(row);
                rowCount++;
            }
            resultTable.setItems(rows);
            status.setText(rowCount + " row" + (rowCount == 1 ? "" : "s"));
        } catch (SQLException e) {
            log.warn("query failed: {}", e.getMessage());
            status.setText("query failed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // SQLite glue
    // -----------------------------------------------------------------

    private Connection openReadOnly(String dbFile) throws SQLException {
        Path path = dataDir.resolve(dbFile);
        if (!path.toFile().exists())
            throw new SQLException("not found: " + path);
        // Read-only mode — never write, never upgrade schema, can't block j-log.
        String url = "jdbc:sqlite:file:" + path + "?mode=ro";
        java.util.Properties props = new java.util.Properties();
        return DriverManager.getConnection(url, props);
    }

    private List<String> listTables(Connection c) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }
}
