package com.hamradio.modem.ui;

import com.google.gson.JsonObject;
import com.hamradio.modem.ModemService;
import com.hamradio.modem.model.HubSpot;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Collapsible right panel: Log Entry, Contest, and DX Spots tabs.
 * The Contest tab activates when J-Log broadcasts CONTEST_ACTIVE via J-Hub.
 */
public class RightPanel extends VBox {

    private final LogEntryPane     logEntryPane;
    private final ContestEntryPane contestEntryPane;
    private final DxSpotPane       dxSpotPane;
    private final TabPane          tabPane;
    private final Tab              contestTab;

    // Tab index constants
    private static final int TAB_LOG     = 0;
    private static final int TAB_CONTEST = 1;
    private static final int TAB_SPOTS   = 2;

    public RightPanel(ModemService service) {
        logEntryPane     = new LogEntryPane(service);
        contestEntryPane = new ContestEntryPane(service);
        dxSpotPane       = new DxSpotPane(service);

        Tab logTab  = new Tab("Log Entry", logEntryPane);
        contestTab  = new Tab("Contest",   contestEntryPane);
        Tab spotTab = new Tab("DX Spots",  dxSpotPane);
        logTab.setClosable(false);
        contestTab.setClosable(false);
        spotTab.setClosable(false);

        tabPane = new TabPane(logTab, contestTab, spotTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Wire: DX spot click → prefill log + switch to Log tab
        dxSpotPane.setOnSpotSelected(spot -> {
            logEntryPane.prefillFromSpot(spot);
            tabPane.getSelectionModel().select(TAB_LOG);
        });

        // Wire contest schema from ModemService
        service.setContestListener(this::onContestSchema);

        getChildren().add(tabPane);
    }

    // ---------------------------------------------------------------
    // Contest activation
    // ---------------------------------------------------------------

    private void onContestSchema(JsonObject schema) {
        if (schema == null) {
            contestEntryPane.deactivate();
            // Switch away from contest tab if it was active
            if (tabPane.getSelectionModel().getSelectedIndex() == TAB_CONTEST) {
                tabPane.getSelectionModel().select(TAB_LOG);
            }
        } else {
            contestEntryPane.activate(schema);
            tabPane.getSelectionModel().select(TAB_CONTEST);
        }
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /** Fill contest pane from a decoded line (e.g. double-click in RX area). */
    public void fillContestFromDecode(String text, String mode) {
        contestEntryPane.fillFromDecode(text, mode);
        if (!contestEntryPane.getContestId().isBlank()) {
            tabPane.getSelectionModel().select(TAB_CONTEST);
        }
    }

    /** Call when a SPOT_SELECTED arrives from hub — prefills log and switches tab. */
    public void onHubSpotSelected(HubSpot spot) {
        logEntryPane.prefillFromSpot(spot);
        tabPane.getSelectionModel().select(TAB_LOG);
    }

    public void showSpotsTab()   { tabPane.getSelectionModel().select(TAB_SPOTS); }
    public void showLogTab()     { tabPane.getSelectionModel().select(TAB_LOG);   }
    public void showContestTab() { tabPane.getSelectionModel().select(TAB_CONTEST); }

    public LogEntryPane     getLogEntryPane()     { return logEntryPane;     }
    public ContestEntryPane getContestEntryPane() { return contestEntryPane; }
    public DxSpotPane       getDxSpotPane()       { return dxSpotPane;       }
}
