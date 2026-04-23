package com.jwae.app;

import com.jlog.db.QtcDao;
import com.jlog.model.Qtc;
import com.jlog.util.AppConfig;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Minimal controller for the j-wae skeleton: shows the QTCs recorded for the
 * currently-active contest. Future work: peer-QSO tracking, 10-QTC-per-peer
 * limit enforcement, send/receive flows driven from a live WAE QSO, Cabrillo
 * export including QTC lines.
 */
public class MainController implements javafx.fxml.Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Label                  lblContest;
    @FXML private TableView<Qtc>         qtcTable;
    @FXML private TableColumn<Qtc,String> colDir;
    @FXML private TableColumn<Qtc,String> colPeer;
    @FXML private TableColumn<Qtc,String> colTime;
    @FXML private TableColumn<Qtc,String> colCall;
    @FXML private TableColumn<Qtc,String> colSerial;
    @FXML private Label                  lblCount;

    private final ObservableList<Qtc> rows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colDir   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDirection()));
        colPeer  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPeerCall()));
        colTime  .setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getQtcTimeUtc() != null ? c.getValue().getQtcTimeUtc().format(FMT) : ""));
        colCall  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getQtcCall()));
        colSerial.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getQtcSerial()));
        qtcTable.setItems(rows);

        String contestId = AppConfig.getInstance().getActiveContestId();
        lblContest.setText(contestId == null || contestId.isBlank()
            ? "No contest active — open j-log and select a WAE contest"
            : "Active contest: " + contestId);

        refresh();
    }

    @FXML private void refresh() {
        String contestId = AppConfig.getInstance().getActiveContestId();
        if (contestId == null || contestId.isBlank()) {
            rows.clear();
            lblCount.setText("0 QTCs");
            return;
        }
        try {
            rows.setAll(QtcDao.getInstance().fetchByContest(contestId));
            lblCount.setText(rows.size() + " QTCs");
        } catch (Exception e) {
            rows.clear();
            lblCount.setText("error: " + e.getMessage());
        }
    }
}
