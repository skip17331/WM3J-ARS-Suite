package com.ars.fx.surface;

import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import static com.ars.fx.shell.Shell.lbl;
import static com.ars.fx.surface.JMapView.*;
import static com.ars.fx.surface.JMapCanvases.*;

/** J-Map's left controls panel and the small controls it is built from. Split out of {@link JMapView}. */
final class JMapControls {
    private JMapControls() {}

    // ---- left controls panel ----------------------------------------------
    /** The controls column is taller than a normal window; it must scroll rather than stretch the whole frame. */
    static Region scrollLeft(Region panel) {
        ScrollPane sp = new ScrollPane(panel); sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setMinWidth(270); sp.setPrefWidth(270); sp.setMaxWidth(270);
        sp.setStyle("-fx-background-color:transparent;");
        return sp;
    }
    static Region leftPanel(int projection, java.util.function.IntConsumer onProjection, Runnable rebuild) {
        VBox root = new VBox(0); root.getStyleClass().add("jm-panel");
        VBox p = new VBox(0);                  // the body of the drawer being built; section(...) files it into root and starts the next
        Label title = lbl("MAP CONTROLS", "jm-sec"); title.setStyle("-fx-padding:0 0 0 0;"); HBox.setHgrow(title, Priority.ALWAYS); title.setMaxWidth(Double.MAX_VALUE);
        HBox header = new HBox(8, title, com.ars.fx.shell.AutoPanel.pinButton("map", rebuild)); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(0, 0, 2, 0));
        root.getChildren().add(header);
        // fixed-zoom region picker — single click. Picking a region snaps the blue
        // marble to that window AND switches the projection to Rectangular, so it
        // takes effect immediately even from the azimuthal view.
        FlowPane regions = new FlowPane(6, 6);
        Label[] rchips = new Label[REGION_NAMES.length];
        Runnable paintChips = () -> {
            for (int i = 0; i < rchips.length; i++) {
                boolean on = i == mapRegion;
                rchips[i].setStyle("-fx-cursor:hand; -fx-padding:4 10 4 10; -fx-background-radius:6; -fx-font-size:11px;"
                        + (on ? "-fx-background-color:-ars-map; -fx-text-fill:#06121a; -fx-font-weight:bold;"
                              : "-fx-background-color:-ars-surface-4; -fx-text-fill:-ars-t2;"));
            }
        };
        for (int i = 0; i < REGION_NAMES.length; i++) {
            final int idx = i;
            Label chip = new Label(REGION_NAMES[i]);
            rchips[i] = chip;
            chip.setOnMouseClicked(e -> { mapRegion = idx; com.ars.fx.data.HubConfig.setInt("map.region", idx); paintChips.run(); highlightProjection(1); onProjection.accept(1); });
            regions.getChildren().add(chip);
        }
        paintChips.run();
        regions.setPadding(new Insets(2, 0, 0, 0));
        p.getChildren().add(regions);
        section(root, p, "Map region", REGION_NAMES[Math.max(0, Math.min(mapRegion, REGION_NAMES.length - 1))], false); p = new VBox(0);
        // bands
        HBox bandsHead = new HBox(lbl("BANDS","jm-sec")); Region bsp = new Region(); HBox.setHgrow(bsp, Priority.ALWAYS);
        bandsHead.getChildren().addAll(bsp, lbl("ALL","jm-sec-all")); bandsHead.setAlignment(Pos.CENTER_LEFT);
        p.getChildren().add(bandsHead);
        GridPane bands = new GridPane(); bands.setHgap(6); bands.setVgap(6);
        java.util.List<HBox> bandChips = new java.util.ArrayList<>(); java.util.List<String> bandKeys = new java.util.ArrayList<>();
        Runnable restyleBands = () -> {
            boolean any = !spotBands.isEmpty();
            for (int i = 0; i < bandChips.size(); i++) {
                boolean sel = spotBands.contains(bandKeys.get(i));
                bandChips.get(i).setOpacity(!any || sel ? 1.0 : 0.4);
            }
        };
        for (int i=0;i<MAP_BANDS.length;i++){
            String[] b = MAP_BANDS[i];
            String key = b[0] + "m";                          // Mock bands are bare ("20"); cluster bands are "20m"
            Region dot = new Region(); dot.getStyleClass().add("jm-band-dot"); dot.setStyle("-fx-background-color:"+b[1]+";");
            HBox chip = new HBox(7, dot, lbl(b[0],"jm-band-n")); chip.getStyleClass().add("jm-band"); chip.setAlignment(Pos.CENTER_LEFT);
            GridPane.setHgrow(chip, Priority.ALWAYS); chip.setMaxWidth(Double.MAX_VALUE); chip.setStyle("-fx-cursor:hand;");
            chip.setOnMouseClicked(e -> { if (!spotBands.remove(key)) spotBands.add(key); com.ars.fx.data.HubConfig.set("map.bands", String.join(",", spotBands)); restyleBands.run(); refreshSpots(); });
            bandChips.add(chip); bandKeys.add(key);
            bands.add(chip, i%3, i/3);
        }
        for (int c=0;c<3;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(100/3.0); bands.getColumnConstraints().add(cc); }
        restyleBands.run();
        p.getChildren().add(bands);
        p.getChildren().add(lbl("MODE","jm-sec"));
        p.getChildren().add(segSel(new String[]{"All","CW","Phone","Digi"}, spotMode, i -> { spotMode = i; com.ars.fx.data.HubConfig.setInt("map.spotMode", i); refreshSpots(); }));
        Label ageLbl = lbl("SPOT AGE ≤ " + spotAgeMax + " MIN","jm-sec");
        javafx.animation.PauseTransition saveSpotAge = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        saveSpotAge.setOnFinished(e -> com.ars.fx.data.HubConfig.setInt("map.spotAge", spotAgeMax));
        p.getChildren().add(ageLbl);
        p.getChildren().add(sliderI((spotAgeMax - 1) / 59.0, f -> {
            spotAgeMax = (int) Math.max(1, Math.round(1 + f * 59));
            ageLbl.setText("SPOT AGE ≤ " + spotAgeMax + " MIN"); refreshSpots();
            saveSpotAge.playFromStart();                      // the slider fires on every pixel: save once it settles
        }));
        Label pn = lbl("Path now: fade closed bands, ring open ones", "jm-tog-label"); HBox.setHgrow(pn, Priority.ALWAYS); pn.setMaxWidth(Double.MAX_VALUE);
        com.ars.fx.shell.Tips.tip(pn, "Our own propagation estimate from the solar flux, sunspots and K index: a spot on a band that is probably closed from here fades, one that is likely open gets a green ring. An estimate, not a measurement. Needs your location set.");
        HBox pnr = new HBox(pn, toggleSwitch(com.ars.fx.data.PathNow.shown(), v -> { com.ars.fx.data.HubConfig.set("map.pathNow", String.valueOf(v)); repaintMap(); refreshSpots(); }));
        pnr.setAlignment(Pos.CENTER_LEFT); pnr.setPadding(new Insets(7, 0, 7, 0));
        p.getChildren().add(pnr);
        section(root, p, "Spot filters", "", false); p = new VBox(0);
        for (int oi = 0; oi < OVERLAYS.length; oi++) {
            final int idx = oi;
            Label t = lbl((String) OVERLAYS[oi][0], "jm-tog-label"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
            if (OVERLAYS[oi].length > 1) com.ars.fx.shell.Tips.tip(t, (String) OVERLAYS[oi][1]);
            HBox row = new HBox(t, toggleSwitch(overlayFlag(idx), v -> { setOverlayFlag(idx, v); repaintMap(); }));
            row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(7, 0, 7, 0));
            p.getChildren().add(row);
        }
        section(root, p, "Map overlays", "", false); p = new VBox(0);
        // ham-to-ham tracks
        javafx.scene.control.TextField trA = new javafx.scene.control.TextField(), trB = new javafx.scene.control.TextField();
        trA.setPromptText("from: call, prefix or grid"); trB.setPromptText("to: call, prefix or grid");
        for (javafx.scene.control.TextField f : new javafx.scene.control.TextField[]{trA, trB}) f.getStyleClass().add("jhub-mini-field");
        Label trMsg = lbl("", "jm-tog-label"); trMsg.setWrapText(true);
        Label trDraw = lbl("Draw", "jl-logbtn"), trClear = lbl("Clear", "jl-clearbtn");
        trDraw.setStyle("-fx-cursor:hand;"); trClear.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(trDraw, "Draw the great-circle path between two stations, with its distance and beam heading. Also from a spot: click a spot, then \"Show path\" to see where the spotter heard it from.");
        Runnable drawTrack = () -> {
            com.ars.fx.data.MapTracks.Track t = com.ars.fx.data.MapTracks.add(trA.getText(), trB.getText());
            trMsg.setText(t == null ? "Cannot place one of those (a call, a prefix such as ZS, or a grid such as FN31)." : t.label());
            repaintMap();
        };
        trDraw.setOnMouseClicked(e -> drawTrack.run());
        trA.setOnAction(e -> trB.requestFocus()); trB.setOnAction(e -> drawTrack.run());
        trClear.setOnMouseClicked(e -> { com.ars.fx.data.MapTracks.clear(); trMsg.setText(""); repaintMap(); });
        HBox trBtns = new HBox(14, trDraw, trClear); trBtns.setAlignment(Pos.CENTER_LEFT); trBtns.setPadding(new Insets(4, 0, 4, 0));
        p.getChildren().addAll(trA, trB, trBtns, trMsg);
        section(root, p, "Ham-to-ham tracks", "", false); p = new VBox(0);
        // NCDXF/IARU International Beacon Project cycle
        Label bt = lbl("Show beacon cycle","jm-tog-label"); HBox.setHgrow(bt, Priority.ALWAYS); bt.setMaxWidth(Double.MAX_VALUE);
        beaconInfo = lbl(showBeacons ? beaconLine() : "", "jm-tog-label"); beaconInfo.setWrapText(true);
        beaconInfo.setStyle("-fx-text-fill:#ffd84a;-fx-font-weight:bold;");
        HBox btr = new HBox(bt, toggleSwitch(showBeacons, v -> { showBeacons = v; beaconInfo.setText(v ? beaconLine() : ""); repaintMap(); qsyToBeacon(); }));
        btr.setAlignment(Pos.CENTER_LEFT); btr.setPadding(new Insets(7, 0, 4, 0));
        p.getChildren().add(btr);
        beaconSeg = (HBox) segSel(BEACON_BANDS, beaconBand, i -> { beaconBand = i; beaconInfo.setText(showBeacons ? beaconLine() : ""); refreshBeaconHeard(); repaintMap(); qsyToBeacon(); });
        p.getChildren().add(beaconSeg);
        p.getChildren().add(beaconInfo);
        Label qt = lbl("Auto QSY the rig","jm-tog-label"); HBox.setHgrow(qt, Priority.ALWAYS); qt.setMaxWidth(Double.MAX_VALUE);
        com.ars.fx.shell.Tips.tip(qt, "Tune the rig to the beacon frequency (CW) when you pick a band or switch the cycle on. Needs the rig connected; never while transmitting.");
        HBox qtr = new HBox(qt, toggleSwitch(com.ars.fx.data.HubConfig.getBool("map.beaconQsy", false), v -> { com.ars.fx.data.HubConfig.set("map.beaconQsy", String.valueOf(v)); qsyToBeacon(); }));
        qtr.setAlignment(Pos.CENTER_LEFT); qtr.setPadding(new Insets(4, 0, 7, 0));
        p.getChildren().add(qtr);
        Label hp = lbl("Hop bands each 3 minutes", "jm-tog-label"); HBox.setHgrow(hp, Priority.ALWAYS); hp.setMaxWidth(Double.MAX_VALUE);
        com.ars.fx.shell.Tips.tip(hp, "Moves to the next band every 3-minute cycle (20, 17, 15, 12, 10 m), so one watch hears every beacon on every band in 15 minutes. With Auto QSY the rig follows.");
        HBox hpr = new HBox(hp, toggleSwitch(beaconHop(), v -> { com.ars.fx.data.HubConfig.set("map.beaconHop", String.valueOf(v)); if (v) beaconWatch(); }));
        hpr.setAlignment(Pos.CENTER_LEFT); hpr.setPadding(new Insets(4, 0, 4, 0));
        Label au = lbl("Mark heard by the S-meter", "jm-tog-label"); HBox.setHgrow(au, Priority.ALWAYS); au.setMaxWidth(Double.MAX_VALUE);
        com.ars.fx.shell.Tips.tip(au, "Marks the beacon on the air as heard when the rig is on the beacon frequency in CW and its S-meter holds S3 or more for three seconds. Needs a rig that reports its S-meter. You can always click a beacon on the map to mark or unmark it.");
        HBox aur = new HBox(au, toggleSwitch(beaconAuto(), v -> com.ars.fx.data.HubConfig.set("map.beaconAuto", String.valueOf(v))));
        aur.setAlignment(Pos.CENTER_LEFT); aur.setPadding(new Insets(4, 0, 4, 0));
        beaconHeardInfo = lbl("", "jm-tog-label"); beaconHeardInfo.setWrapText(true);
        refreshBeaconHeard();
        Label hear = lbl("I hear it", "jl-logbtn"), logBtn = lbl("Heard log", "jl-clearbtn"), clearHeard = lbl("Clear", "jl-clearbtn");
        for (Label l : new Label[]{hear, logBtn, clearHeard}) l.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(hear, "Marks the beacon that is on the air now, on the band shown, as heard.");
        com.ars.fx.shell.Tips.tip(logBtn, "Writes the beacons you have heard, with the UTC time, to a CSV file and opens it.");
        hear.setOnMouseClicked(e -> { com.ars.fx.data.BeaconHeard.mark(BEACONS[activeBeacon(beaconBand)][0], BEACON_BANDS[beaconBand], java.time.Instant.now().getEpochSecond()); refreshBeaconHeard(); repaintMap(); });
        logBtn.setOnMouseClicked(e -> {
            try {
                java.nio.file.Path f = java.nio.file.Paths.get(System.getProperty("user.home"), ".j-map", "beacons-heard.csv");
                java.nio.file.Files.createDirectories(f.getParent());
                java.nio.file.Files.writeString(f, com.ars.fx.data.BeaconHeard.csv(), java.nio.charset.StandardCharsets.UTF_8);
                com.ars.fx.data.LinkOpener.openFile(f, () -> { });
            } catch (java.io.IOException ex) { beaconHeardInfo.setText("Could not write the log: " + ex.getMessage()); }
        });
        clearHeard.setOnMouseClicked(e -> { com.ars.fx.data.BeaconHeard.clear(); refreshBeaconHeard(); repaintMap(); });
        HBox heardBtns = new HBox(14, hear, logBtn, clearHeard); heardBtns.setAlignment(Pos.CENTER_LEFT); heardBtns.setPadding(new Insets(4, 0, 6, 0));
        p.getChildren().addAll(hpr, aur, beaconHeardInfo, heardBtns);
        section(root, p, "NCDXF beacons", "", false); p = new VBox(0);

        // DX-cluster spot paths: spotted station to spotter
        Label sp = lbl("Draw spotted \u2192 spotter for each shown spot","jm-tog-label"); HBox.setHgrow(sp, Priority.ALWAYS); sp.setMaxWidth(Double.MAX_VALUE);
        HBox spr = new HBox(sp, toggleSwitch(showSpotPaths, v -> { showSpotPaths = v; repaintMap(); }));
        spr.setAlignment(Pos.CENTER_LEFT); spr.setPadding(new Insets(7, 0, 4, 0));
        com.ars.fx.shell.Tips.tip(spr, "Draw a line from each spotted station to the station that spotted it, in the band's colour. It follows the mode and band filters, fades with the spot's age and goes after 10 minutes.");
        Label spClear = lbl("Clear", "jl-clearbtn"); spClear.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(spClear, "Wipe the spot paths and start again: only spots that arrive from now on get a line.");
        spClear.setOnMouseClicked(e -> { spotPathsSince = System.currentTimeMillis(); selectedSpotCall = null; repaintMap(); });
        HBox spBtns = new HBox(14, spClear); spBtns.setAlignment(Pos.CENTER_LEFT); spBtns.setPadding(new Insets(0, 0, 6, 0));
        p.getChildren().addAll(spr, spBtns);
        section(root, p, "Spot paths (DX cluster)", "", false); p = new VBox(0);

        // RBN "who's hearing me" overlay
        Label ht = lbl("Show RBN spots of " + (myCall.isEmpty() ? "my call" : myCall.toUpperCase()),"jm-tog-label"); HBox.setHgrow(ht, Priority.ALWAYS); ht.setMaxWidth(Double.MAX_VALUE);
        HBox htr = new HBox(ht, toggleSwitch(showHeardBy, v -> { showHeardBy = v; repaintMap(); }));
        htr.setAlignment(Pos.CENTER_LEFT); htr.setPadding(new Insets(7, 0, 4, 0));
        p.getChildren().add(htr);
        section(root, p, "Who's hearing me (RBN)", "", false); p = new VBox(0);

        // Session QSOs: an arc from the station to each QSO logged since ARS Suite started
        Label sq = lbl("Draw an arc to each QSO logged this session", "jm-tog-label"); sq.setWrapText(true); HBox.setHgrow(sq, Priority.ALWAYS); sq.setMaxWidth(Double.MAX_VALUE);
        HBox sqr = new HBox(sq, toggleSwitch(showSession, v -> { showSession = v; com.ars.fx.data.HubConfig.setBool("map.sessionQsos", v); repaintMap(); }));
        sqr.setAlignment(Pos.CENTER_LEFT); sqr.setPadding(new Insets(7, 0, 4, 0));
        sessionInfo = lbl(sessionText(), "jm-tog-label"); sessionInfo.setWrapText(true);
        Label sqClear = lbl("Clear", "jhub-clearbtn"); sqClear.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(sqClear, "Forget this session's QSOs on the map (the log is not touched)");
        sqClear.setOnMouseClicked(e -> com.ars.fx.data.SessionQsos.clear());
        HBox sqInfoRow = new HBox(10, sessionInfo, sqClear); sqInfoRow.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(sessionInfo, Priority.ALWAYS); sessionInfo.setMaxWidth(Double.MAX_VALUE);
        p.getChildren().addAll(sqr, sqInfoRow);
        section(root, p, "Session QSOs", "", false);
        // a QSO logged anywhere in the app updates the count and, when the switch is on, the map
        com.ars.fx.data.SessionQsos.setListener(() -> javafx.application.Platform.runLater(() -> {
            if (sessionInfo != null) sessionInfo.setText(sessionText());
            if (showSession) repaintMap();
        }));

        return root;
    }
    /** File one finished group of controls into the panel as a drawer (open or closed as the operator last left it; {@code openFirst} is only the very first time). */
    private static void section(VBox root, VBox body, String title, String summary, boolean openFirst) {
        if (openFirst) Shell.openDrawerByDefault(title);
        body.setPadding(new Insets(2, 0, 6, 0));
        root.getChildren().add(Shell.drawer(title, "map", "map", summary, openFirst, body));
    }
    /** Interactive slider — click/drag sets the value and reports the fraction. */
    static Region sliderI(double frac, java.util.function.DoubleConsumer onChange) {
        double[] f = {frac};
        Region track = new Region(); track.setMinHeight(5); track.setMaxHeight(5); track.setStyle("-fx-background-color:-ars-surface-4; -fx-background-radius:99;");
        Region fill = new Region(); fill.setMinHeight(5); fill.setMaxHeight(5); fill.setStyle("-fx-background-color:-ars-map; -fx-background-radius:99;");
        Region knob = new Region(); knob.setMinSize(15, 15); knob.setMaxSize(15, 15); knob.setStyle("-fx-background-color:-ars-map; -fx-background-radius:99; -fx-border-color:-ars-surface-1; -fx-border-width:2; -fx-border-radius:99;");
        Pane bar = new Pane(track, fill, knob); bar.setMinHeight(16); bar.setPrefHeight(16); bar.setStyle("-fx-cursor:hand;");
        Runnable layout = () -> { double w = bar.getWidth(); track.resizeRelocate(0, 6, w, 5); fill.resizeRelocate(0, 6, w * f[0], 5); knob.relocate(Math.max(0, w * f[0] - 7), 1); };
        bar.widthProperty().addListener((o, a, b) -> layout.run());
        java.util.function.DoubleConsumer setX = px -> { double w = bar.getWidth(); if (w <= 0) return; f[0] = Math.max(0, Math.min(1, px / w)); layout.run(); onChange.accept(f[0]); };
        bar.setOnMousePressed(e -> setX.accept(e.getX()));
        bar.setOnMouseDragged(e -> setX.accept(e.getX()));
        VBox.setMargin(bar, new Insets(8, 0, 4, 0));
        return bar;
    }
    static Region toggleSwitch(boolean initial, java.util.function.Consumer<Boolean> onChange) {
        boolean[] on = {initial};
        Region knob = new Region(); knob.getStyleClass().add("jm-switch-knob");
        StackPane sw = new StackPane(knob); sw.getStyleClass().add("jm-switch"); com.ars.fx.shell.Tips.tip(sw, "Click to switch on or off"); sw.setMaxSize(36, 20); sw.setPadding(new Insets(0, 3, 0, 3));
        Runnable paint = () -> { sw.getStyleClass().remove("on"); if (on[0]) sw.getStyleClass().add("on"); StackPane.setAlignment(knob, on[0] ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); };
        paint.run(); sw.setStyle("-fx-cursor:hand;");
        sw.setOnMouseClicked(e -> { on[0] = !on[0]; paint.run(); onChange.accept(on[0]); });
        return sw;
    }
    /** Collapsed left column — a thin rail with a re-open control. */
    static Region leftRail() {
        VBox p = new VBox(0); p.getStyleClass().add("jm-panel"); p.setMinWidth(40); p.setPrefWidth(40); p.setMaxWidth(40);
        p.setAlignment(Pos.TOP_CENTER); p.setPadding(new Insets(16, 0, 16, 0));
        Label expand = lbl("›", "jm-collapse");
        Label vlabel = lbl("CONTROLS", "jm-sec"); vlabel.setRotate(90);
        javafx.scene.Group vgroup = new javafx.scene.Group(vlabel);   // sized to the rotated label, so it can't spill over the dock
        VBox.setMargin(vgroup, new Insets(26, 0, 0, 0));
        p.getChildren().addAll(expand, vgroup);
        return p;
    }
    /** Interactive segmented control reporting the chosen index. */
    static Region segSel(String[] opts, int on, java.util.function.IntConsumer onSelect) {
        HBox h = new HBox(2); h.getStyleClass().add("jm-seg");
        Label[] btns = new Label[opts.length];
        for (int i=0;i<opts.length;i++){
            final int idx = i;
            Label b = lbl(opts[i],"jm-seg-btn"); if(i==on) b.getStyleClass().add("on"); HBox.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
            b.setOnMouseClicked(e -> { for (Label x : btns) x.getStyleClass().remove("on"); b.getStyleClass().add("on"); onSelect.accept(idx); });
            btns[i] = b; h.getChildren().add(b);
        }
        return h;
    }
}
