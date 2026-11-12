package com.ars.fx.surface;

import com.ars.fx.data.CallsignLookup;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.JLogDb;
import com.ars.fx.shell.Shell;
import com.ars.fx.shell.Tips;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Locale;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Log a QSO from J-Map without leaving it. Clicking a spot opens a floating card filled in from the spot; pinning moves
 * the same card into a drawer in the right column, where it stays and clears itself after each QSO. Always writes to
 * the normal log: contest logging stays in J-Log.
 */
final class MapQsoCard {
    private MapQsoCard() {}

    private static final String TITLE = "New QSO", PIN_KEY = "map.qsoPinned";
    private static final String FLOATING_STYLE = "-fx-background-color:-ars-surface-1;-fx-background-radius:12;"
            + "-fx-border-color:-ars-border;-fx-border-width:1;-fx-border-radius:12;-fx-padding:12 14 14 14;";

    private static VBox card;
    private static Label info;
    private static Label pin, status;
    private static TextField call, freq, mode, rstSent, rstRcvd, name, qth, grid, comment, iota, sota, pota;
    private static CheckBox cQsl, cLotw, cEqsl;      // confirmations, as on the full J-Log form
    private static boolean pinned = HubConfig.getBool(PIN_KEY, false);
    private static final VBox slot = new VBox();       // the place in the right column; empty unless pinned

    static boolean isPinned() { return pinned; }

    /** The right-column slot: holds the drawer while the card is pinned. */
    static Region drawerSlot() { refreshSlot(); return slot; }

    /** The button over the map, for a station found by tuning rather than from a spot. */
    static Node addButton() {
        Label b = lbl("+ QSO", "jhub-clearbtn"); b.setStyle("-fx-cursor:hand;");
        Tips.tip(b, "Log a QSO to the normal log");
        b.setOnMouseClicked(e -> { build(); clear(); show(b); });
        b.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(b, Pos.TOP_LEFT);
        StackPane.setMargin(b, new Insets(12, 0, 0, 12));
        return b;
    }

    /** Fill the form from a spot and show it: "Log this" on the info toast, or any spot click while docked. */
    static void open(Node source, JMapView.MapSpot s) {
        build();
        clear();
        call.setText(s.call());
        if (s.freqHz() > 0) freq.setText(String.format(Locale.US, "%.3f", s.freqHz() / 1e6));
        mode.setText(SpotAction.logMode(s.mode(), s.freqHz()));
        info.setText(DxInfoCard.summary(s));
        info.setManaged(true); info.setVisible(true);
        lookupArrived(s.call(), DxInfoCard.answerFor(s.call()));
        show(source);
    }

    /** A callsign lookup answered: fill what the operator hasn't typed, if the form is still on that station. */
    static void lookupArrived(String forCall, CallsignLookup.Result r) {
        if (r == null || card == null || !forCall.equalsIgnoreCase(call.getText())) return;
        fillIfEmpty(name, r.name()); fillIfEmpty(qth, r.city()); fillIfEmpty(grid, r.grid());
    }

    private static void show(Node source) {
        if (pinned) {
            Shell.openDrawer(TITLE);
            refreshSlot();
            com.ars.fx.shell.AutoPanel.reveal("rail");        // does nothing while the column is pinned open
        } else {
            float_(source);
        }
        focusFirstEmpty();
    }

    /** Where J-Map's pop-ups sit: down the right edge of the map, under the view buttons and clear of the globe, which is centred in the map. When
     *  the PANELS column is pinned open it takes the space at the edge, so they move left of it. */
    static Insets popupMargin() {
        return new Insets(205, com.ars.fx.shell.AutoPanel.isPinned("rail") ? 350 : 62, 0, 0);
    }

    private static void float_(Node source) {
        Parent root = source.getScene() == null ? null : source.getScene().getRoot();
        if (!(root instanceof StackPane host) || host.getChildren().contains(card)) return;
        card.setStyle(FLOATING_STYLE);
        card.setMaxSize(320, Region.USE_PREF_SIZE);
        StackPane.setAlignment(card, Pos.TOP_RIGHT);
        StackPane.setMargin(card, popupMargin());                // the strip at the right edge of the map, off the globe, clear of the right column
        host.getChildren().add(card);
    }

    private static void close() {
        if (card != null && card.getParent() instanceof StackPane p) p.getChildren().remove(card);
    }

    private static void togglePin() {
        pinned = !pinned;
        HubConfig.setBool(PIN_KEY, pinned);
        pin.setText(pinned ? "Unpin" : "Pin");
        if (pinned) {
            close();
            Shell.openDrawer(TITLE);
            refreshSlot();
            com.ars.fx.shell.AutoPanel.setPinned("rail", true);   // a docked form needs its column on screen
        } else {
            Node anchor = slot;                                   // still in the scene once the drawer is emptied
            refreshSlot();
            if (anchor.getScene() != null) float_(anchor);
        }
        focusFirstEmpty();
    }

    private static void refreshSlot() {
        slot.setManaged(pinned); slot.setVisible(pinned);
        slot.getChildren().clear();
        if (!pinned) return;
        build();
        card.setStyle("");
        card.setTranslateX(0); card.setTranslateY(0);
        card.setMaxSize(Double.MAX_VALUE, Region.USE_PREF_SIZE);
        slot.getChildren().add(Shell.drawer(TITLE, "log", "log", "", false, card));
    }

    private static void build() {
        if (card != null) return;
        call = field(""); freq = field("MHz"); mode = field("USB");
        rstSent = field(""); rstRcvd = field("");
        name = field("name"); qth = field("city / QTH"); grid = field("FN31"); comment = field("notes, QSL info…");
        iota = field("NA-005"); sota = field("W7W/LC-001"); pota = field("US-0001");      // award references of the other station
        cQsl = new CheckBox("QSL"); cLotw = new CheckBox("LoTW"); cEqsl = new CheckBox("eQSL");
        Tips.tip(cQsl, "Paper QSL card received"); Tips.tip(cLotw, "Confirmed on LoTW"); Tips.tip(cEqsl, "Confirmed on eQSL");
        info = lbl("", "dx-info-v"); info.setWrapText(true);
        status = lbl("", "jm-spot-meta");

        pin = lbl(pinned ? "Unpin" : "Pin", "jhub-clearbtn"); pin.setStyle("-fx-cursor:hand;");
        Tips.tip(pin, "Keep this card in the right column");
        pin.setOnMouseClicked(e -> togglePin());
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = new HBox(8, lbl("Normal log", "jm-spot-meta"), gap, pin); head.setAlignment(Pos.CENTER_LEFT);
        draggable(head);

        Label clear = lbl("Clear", "jhub-clearbtn"), log = lbl("Log QSO ⏎", "jl-logbtn");
        clear.setStyle("-fx-cursor:hand;"); log.setStyle("-fx-cursor:hand;");
        clear.setOnMouseClicked(e -> dismiss());
        log.setOnMouseClicked(e -> logIt());
        Region gap2 = new Region(); HBox.setHgrow(gap2, Priority.ALWAYS);
        HBox buttons = new HBox(10, status, gap2, clear, log); buttons.setAlignment(Pos.CENTER_LEFT);

        card = new VBox(8, head, info,
                labeled("CALLSIGN", call),
                row(labeled("FREQ (MHZ)", freq), labeled("MODE", mode)),
                row(labeled("RST SENT", rstSent), labeled("RST RCVD", rstRcvd)),
                labeled("NAME", name),
                row(labeled("QTH", qth), labeled("GRID", grid)),
                row(labeled("IOTA", iota), labeled("POTA PARK", pota)),
                labeled("SOTA SUMMIT", sota),
                RefHints.attach(call, grid, iota, sota, pota),
                new VBox(3, lbl("CONFIRMED", "jl-flabel"), new HBox(10, cQsl, cLotw, cEqsl)),
                labeled("COMMENT", comment),
                buttons);
        // a filter, because a text field swallows Esc before it could bubble up here; while sending, the scene's Esc stops TX first
        card.addEventFilter(KeyEvent.KEY_PRESSED, e -> { if (e.getCode() == KeyCode.ESCAPE) { dismiss(); e.consume(); } });
    }

    /** Clear: a floating card goes away, a pinned one stays ready for the next QSO. */
    private static void dismiss() {
        clear();
        if (!pinned) close();
    }

    private static void logIt() {
        String c = call.getText();
        if (c == null || c.isBlank()) { call.requestFocus(); return; }
        String f = freq.getText();
        JLogDb.Refs refs = new JLogDb.Refs(iota.getText().trim(), sota.getText().trim(), pota.getText().trim(), 0, 0,
                cQsl.isSelected(), cLotw.isSelected(), cEqsl.isSelected());
        boolean ok = JLogDb.insertAt(null, c, f, JLogDb.bandFor(f), mode.getText(), rstSent.getText(), rstRcvd.getText(),
                name.getText(), qth.getText(), grid.getText(), comment.getText(), null, null, 0, refs);
        if (!ok) { status.setText("Not saved"); return; }
        JLogDb.QsoFull saved = JLogDb.byId(JLogDb.maxId());
        if (saved != null) com.ars.fx.data.N1mmBroadcast.normalAdded(com.ars.fx.data.N1mmBroadcast.of(saved));
        SpotAction.takePending();                    // this contact is done; J-Log must not refill it on its next open
        clear();
        status.setText("Logged " + c.trim().toUpperCase());
        PauseTransition after = new PauseTransition(Duration.seconds(pinned ? 4 : 1.2));
        after.setOnFinished(e -> { status.setText(""); if (!pinned && call.getText().isBlank()) close(); });
        after.play();
        call.requestFocus();
    }

    private static void clear() {
        for (TextField t : new TextField[]{call, freq, mode, name, qth, grid, comment, iota, sota, pota}) t.clear();
        cQsl.setSelected(false); cLotw.setSelected(false); cEqsl.setSelected(false);
        rstSent.setText("59"); rstRcvd.setText("59");
        info.setText(""); info.setManaged(false); info.setVisible(false);
        status.setText("");
    }

    private static void fillIfEmpty(TextField t, String v) {
        if (v != null && !v.isBlank() && t.getText().isBlank()) t.setText(v.trim());
    }

    /** Spots land on the report fields; a blank card lands on the callsign. */
    private static void focusFirstEmpty() {
        TextField target = call.getText().isBlank() ? call : rstSent;
        Platform.runLater(() -> { target.requestFocus(); target.selectAll(); });
    }

    private static void draggable(Node handle) {
        double[] from = new double[2];
        handle.setOnMousePressed(e -> { from[0] = e.getSceneX() - card.getTranslateX(); from[1] = e.getSceneY() - card.getTranslateY(); });
        handle.setOnMouseDragged(e -> {
            if (pinned) return;
            card.setTranslateX(e.getSceneX() - from[0]);
            card.setTranslateY(e.getSceneY() - from[1]);
        });
        handle.setStyle("-fx-cursor:move;");
    }

    private static TextField field(String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt); t.getStyleClass().add("jhub-mini-field"); t.setMaxWidth(Double.MAX_VALUE);
        t.setOnAction(e -> logIt());
        return Shell.caps(t);
    }

    private static VBox labeled(String label, TextField f) { return new VBox(3, lbl(label, "jl-flabel"), f); }

    private static HBox row(VBox a, VBox b) {
        HBox.setHgrow(a, Priority.ALWAYS); HBox.setHgrow(b, Priority.ALWAYS);
        return new HBox(10, a, b);
    }
}
