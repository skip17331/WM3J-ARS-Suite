package com.hamradio.modem.ui;

import com.hamradio.modem.ModemService;
import com.hamradio.modem.model.HubSpot;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Collapsible right panel containing Log Entry and DX Spots tabs.
 * The MainWindow controls visibility by adjusting the SplitPane divider.
 */
public class RightPanel extends VBox {

    private final LogEntryPane logEntryPane;
    private final DxSpotPane   dxSpotPane;
    private final TabPane      tabPane;

    public RightPanel(ModemService service) {
        logEntryPane = new LogEntryPane(service);
        dxSpotPane   = new DxSpotPane(service);

        Tab logTab  = new Tab("Log Entry", logEntryPane);
        Tab spotTab = new Tab("DX Spots",  dxSpotPane);
        logTab.setClosable(false);
        spotTab.setClosable(false);

        tabPane = new TabPane(logTab, spotTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Wire: clicking a DX spot → prefill log + switch to Log tab
        dxSpotPane.setOnSpotSelected(spot -> {
            logEntryPane.prefillFromSpot(spot);
            tabPane.getSelectionModel().select(0);
        });

        getChildren().add(tabPane);
    }

    /** Call when a SPOT_SELECTED arrives from hub — prefills log and switches tab. */
    public void onHubSpotSelected(HubSpot spot) {
        logEntryPane.prefillFromSpot(spot);
        tabPane.getSelectionModel().select(0);
    }

    public void showSpotsTab() {
        tabPane.getSelectionModel().select(1);
    }

    public void showLogTab() {
        tabPane.getSelectionModel().select(0);
    }

    public LogEntryPane getLogEntryPane() { return logEntryPane; }
    public DxSpotPane   getDxSpotPane()   { return dxSpotPane;   }
}
