package com.ars.fx.surface;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.SpotNeeds;
import com.ars.fx.data.JLogDb;
import com.ars.fx.data.RigClient;
import com.ars.fx.data.RotorClient;
import com.ars.fx.data.AmpClient;
import com.ars.fx.shell.Shell;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import static com.ars.fx.shell.Shell.lbl;
import static com.ars.fx.surface.JHubDashboard.*;

/** J-Hub ▸ Setup: the pages (station, rig, rotor, amplifier, data, appearance, modules ...) and the controls they are built from. Split out of {@link JHubDashboard}. */
final class JHubSetupPages {
    private JHubSetupPages() {}

    /** The content of one J-Hub setup section, for {@link JHubSetup} (one screen, section menu on the left). */
    static Region setupPage(String id) {
        return switch (id) {
            case "station"  -> stationCenter();
            case "rig"      -> configCenter();
            case "rotor"    -> rotorCenter();
            case "amp"      -> amplifierCenter();
            case "antsw"    -> antennaSwitchCenter();
            case "modules"  -> modulesCenter();
            case "appearance" -> appearanceCenter();
            case "sat"      -> JHubSatellites.center();
            default         -> dataPage(id);     // tx, cluster, digital, lookup, logdb, upload, export, share, learn
        };
    }

    // ---- config page (shot 09) --------------------------------------------
    static Region configCenter() {
        HBox crumb = new HBox(7, lbl("J-Hub","jhub-crumb"), lbl("›","jhub-crumb"), lbl("Hardware","jhub-crumb"),
                lbl("›","jhub-crumb"), lbl("Rig control","cfg-title")); crumb.setAlignment(Pos.BOTTOM_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label back = lbl("← Dashboard","cfg-link"); back.setStyle("-fx-cursor:hand;"); back.setOnMouseClicked(e -> Shell.navigate("hub"));
        HBox top = new HBox(crumb, sp, back); top.setAlignment(Pos.BOTTOM_LEFT);
        Label desc = lbl("CAT control for your transceiver — connection, PTT, and how J-Hub follows the radio.","cfg-desc");

        com.ars.fx.data.RigClient.getInstance().start();
        HBox cells = liveStrip(new String[]{"STATUS", "FREQUENCY", "MODE", "RIGCTLD"}, () -> {
            com.ars.fx.data.RigClient rc = com.ars.fx.data.RigClient.getInstance();
            com.ars.fx.data.RigClient.State st = rc.last();
            boolean on = st != null && st.connected();
            return new String[][]{
                {on ? "● CONNECTED" : "○ OFFLINE", "", on ? "1" : "0"},
                {on && st.freqHz() > 0 ? com.ars.fx.data.RigClient.fmtFreq(st.freqHz()) : "—", on && st.freqHz() > 0 ? "MHz" : "", "0"},
                {on && st.mode() != null && !st.mode().isBlank() ? st.mode() : "—", "", "0"},
                {rc.endpoint(), "", "0"}};
        });

        VBox conn = section("CONNECTION",
            cfgRow("Transceiver maker", "from the Hamlib installed on this computer (rigctl --list)", hamlibMaker("rig")),
            cfgRow("Transceiver model", "choosing one fills in the Hamlib model # below", hamlibModel("rig")),
            cfgRow("Hamlib model #", "set by the model above; type a number to use a model that is not listed", hamlibNumber("rig")),
            cfgRow("Interface", null, cSeg("rig.interface", new String[]{"USB","Serial","Network"}, 0)),
            cfgRow("Port", null, cInputPrompt("rig.port", "e.g. /dev/ttyUSB0")),
            cfgRow("Baud rate", null, cSelect("rig.baud", "9600", BAUDS)),
            cfgRow("CI-V address", "Icom civ address (hex)", cInputPrompt("rig.civ", "e.g. 0x98")),
            cfgRow("Poll interval", "how often J-Hub asks rigctld for frequency, mode and meters (250–5000 ms)", cInputApply("rig.poll", "800", () -> {})),
            cfgRow("rigctld host", "Hamlib rig daemon J-Hub connects to", cInputApply("rig.rigctldHost", "127.0.0.1", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("rigctld port", null, cInputApply("rig.rigctldPort", "4532", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())));
        VBox ptt = section("PTT & KEYING",
            cfgRow("PTT method", "CAT keys over the CAT link. RTS / DTR key that line of the serial port (needs Start rigctld for me). VOX keys nothing — the radio or interface senses the audio",
                    cSegApply("rig.ptt", new String[]{"CAT","RTS","DTR","VOX"}, 0, () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("CW keyer", null, cInputPrompt("rig.keyer", "e.g. Winkeyer USB")));
        VBox beh = section("BEHAVIOR",
            cfgRow("Auto band-follow", "the New QSO frequency, band and mode follow the radio while it is connected (a digital rig mode leaves the mode as typed)", cToggle("rig.autoBandFollow", true)),
            cfgRow("Start rigctld for me", "spawn the daemon with the model/serial above", cToggleApply("rig.startDaemon", false, on -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            daemonStatusRow("rig"),
            cfgRow("Show status in hero bar", "the rig's frequency, mode and connection in the banner at the top of every screen", cToggle(com.ars.fx.shell.HeroDevices.KEY_RIG, true)));

        VBox v = new VBox(20, top, desc, cells, conn, ptt, beh);
        v.setPadding(new Insets(18, 28, 40, 28)); v.setMaxWidth(1080); v.setStyle("-fx-background-color:-ars-bg;");
        return v;
    }
    static Region stationCenter() {
        HBox crumb = new HBox(7, lbl("J-Hub","jhub-crumb"), lbl("›","jhub-crumb"), lbl("Station","cfg-title"));
        crumb.setAlignment(Pos.BOTTOM_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label back = lbl("← Dashboard","cfg-link"); back.setStyle("-fx-cursor:hand;"); back.setOnMouseClicked(e -> Shell.navigate("hub"));
        HBox top = new HBox(crumb, sp, back); top.setAlignment(Pos.BOTTOM_LEFT);
        Label desc = lbl("Your station identity & location — shared with every module for grids, beam headings and award tracking.","cfg-desc");

        VBox callCell = statCell("CALLSIGN", "", "", true), gridCell = statCell("GRID", "", "", false),
             zoneCell = statCell("ITU / CQ", "", "", false), dxccCell = statCell("DXCC", "", "", false);
        stationStrip = new VBox[]{callCell, gridCell, zoneCell, dxccCell};
        refreshStationStrip();
        startStationStripListener();
        HBox cells = new HBox(0, callCell, gridCell, zoneCell, dxccCell);
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);

        VBox id = section("IDENTITY",
            cfgRow("Callsign", null, cInputApply("station.call", "", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Operator name", null, cInputPrompt("station.name", "your first name")),
            cfgRow("QTH", "city, state", cInputPrompt("station.qth", "e.g. Baltimore MD")),
            cfgRow("Grid square", "6-char Maidenhead locator", cInput("station.grid", "")));
        VBox loc = section("LOCATION",
            cfgRow("Latitude", "decimal degrees (N+)", cInputPrompt("station.lat", "decimal degrees, N+")),
            cfgRow("Longitude", "decimal degrees (E+)", cInputPrompt("station.lon", "decimal degrees, E+")),
            cfgRow("Elevation", "height above sea level — shown in the unit chosen under Units", elevationField()));
        VBox zones = section("ZONES & AWARDS",
            cfgRow("ITU zone", null, cInputPrompt("station.ituZone", "your ITU zone")),
            cfgRow("CQ zone", null, cInputPrompt("station.cqZone", "your CQ zone")),
            cfgRow("IARU region", null, cSegApply("station.iaru", new String[]{"R1","R2","R3"}, 1, com.ars.fx.data.Bands::applyRegion)),
            cfgRow("Default TX power", "pre-fills the power on a new QSO in J-Log", cUnit("station.txPower", "100", "W")));

        VBox units = section("UNITS",
            cfgRow("Units", "Metric: km · °C · km/h · hPa · m      Standard (US): mi · °F · mph · inHg · ft", unitSystemSeg()));
        Label checkMsg = lbl("", "cfg-lh");
        Label checkBtn = lbl("Check now", "sx-theme"); checkBtn.setStyle("-fx-cursor:hand;");
        checkBtn.setOnMouseClicked(e -> {
            checkMsg.setText("Checking…");
            Thread t = new Thread(() -> {
                String msg;
                try {
                    var r = com.ars.fx.data.UpdateChecker.check(true);
                    msg = r == null ? "You're up to date (v" + com.ars.fx.AppVersion.current() + ")." : r.tag() + " is available"
                            + (r.publishedDate().isEmpty() ? "." : " (released " + r.publishedDate() + ").");
                    if (com.ars.fx.data.UpdateApplier.isGitCheckout())      // newer work that is merged but not released
                        msg += " " + com.ars.fx.data.UpdateChecker.unreleasedNote(com.ars.fx.data.UpdateChecker.unreleasedChanges(r == null ? "v" + com.ars.fx.AppVersion.current() : r.tag()));
                } catch (Exception ex) { msg = "Couldn't reach GitHub: " + ex.getMessage(); }
                String m = msg;
                javafx.application.Platform.runLater(() -> checkMsg.setText(m));
            }, "update-check-manual");
            t.setDaemon(true); t.start();
        });
        HBox checkRow = new HBox(12, checkBtn, checkMsg); checkRow.setAlignment(Pos.CENTER_LEFT);
        VBox updates = section("UPDATES",
            cfgRow("Version", "this build", lbl("v" + com.ars.fx.AppVersion.current(), "cfg-sv")),
            cfgRow("Check for updates", "once a day, from GitHub — nothing about you is sent", cToggle("update.check", true)),
            cfgRow("Refresh country files", "weekly, from country-files.com — keeps DXCC prefixes and zones current", cToggle("cty.update", true)),
            cfgRow("Check now", null, checkRow));
        VBox v = new VBox(20, top, desc, cells, id, loc, units, updates, zones);
        v.setPadding(new Insets(18, 28, 40, 28)); v.setMaxWidth(1080); v.setStyle("-fx-background-color:-ars-bg;");
        return v;
    }

    /** A status strip whose values are re-read from {@code vals} once a second while it is on screen: one {value, unit, "1" = ok}
     *  per key. The strips on the hardware pages show what the rig / rotor / amp / feeds are doing now, not what they were
     *  when the page was opened. */
    static HBox liveStrip(String[] keys, java.util.function.Supplier<String[][]> vals) { return liveStrip(keys, vals, 150); }
    static HBox liveStrip(String[] keys, java.util.function.Supplier<String[][]> vals, double cellWidth) {
        VBox[] cells = new VBox[keys.length];
        for (int i = 0; i < keys.length; i++) { cells[i] = statCell(keys[i], "", "", false); cells[i].setMinWidth(cellWidth); }
        HBox row = new HBox(0, cells); row.getStyleClass().add("cfg-status"); row.setAlignment(Pos.CENTER_LEFT);
        Runnable paint = () -> {
            String[][] v;
            try { v = vals.get(); } catch (RuntimeException ex) { return; }
            for (int i = 0; i < cells.length && i < v.length; i++) {
                Label val = stripValue(cells[i]);
                val.setText(v[i][0]);
                val.getStyleClass().remove("ok");
                if ("1".equals(v[i][2])) val.getStyleClass().add("ok");
                HBox vu = (HBox) cells[i].getChildren().get(1);
                if (v[i][1].isEmpty()) { if (vu.getChildren().size() > 1) vu.getChildren().remove(1); }
                else if (vu.getChildren().size() > 1) ((Label) vu.getChildren().get(1)).setText(v[i][1]);
                else vu.getChildren().add(lbl(v[i][1], "cfg-sval-u"));
            }
        };
        paint.run();
        com.ars.fx.shell.Fx.everySecondWhileShown(row, paint);
        return row;
    }

    // ---- Station page status strip: follows the saved settings as they are edited ----------------------------
    static VBox[] stationStrip;
    static boolean stationStripListening;
    static Label stripValue(VBox cell) { return (Label) ((HBox) cell.getChildren().get(1)).getChildren().get(0); }
    static Label stripUnit(VBox cell) { HBox vu = (HBox) cell.getChildren().get(1); return vu.getChildren().size() > 1 ? (Label) vu.getChildren().get(1) : null; }
    static String orDash(String s) { return s == null || s.isBlank() ? "—" : s.trim(); }
    static void refreshStationStrip() {
        VBox[] c = stationStrip;
        if (c == null) return;
        String call = com.ars.fx.data.HubConfig.call();
        stripValue(c[0]).setText(orDash(call));
        stripValue(c[1]).setText(orDash(com.ars.fx.data.HubConfig.grid()).toUpperCase());
        stripValue(c[2]).setText(orDash(com.ars.fx.data.HubConfig.get("station.ituZone", "")) + " / " + orDash(com.ars.fx.data.HubConfig.get("station.cqZone", "")));
        // the DXCC entity follows the callsign: number and country, or a dash when the call can't be resolved
        com.ars.fx.data.DxccInfo.Info info = call == null || call.isBlank() ? null : com.ars.fx.data.DxccInfo.of(call);
        stripValue(c[3]).setText(info == null || info.dxcc() <= 0 ? "—" : String.valueOf(info.dxcc()));
        Label u = stripUnit(c[3]);
        if (u == null) { u = lbl("", "cfg-sval-u"); ((HBox) c[3].getChildren().get(1)).getChildren().add(u); }
        u.setText(info == null || info.country() == null ? "" : info.country());
    }
    static void startStationStripListener() {
        if (stationStripListening) return;
        stationStripListening = true;
        com.ars.fx.data.HubConfig.addChangeListener(key -> {
            if (key == null || !(key.startsWith("station.") || key.equals("*"))) return;
            javafx.application.Platform.runLater(JHubSetupPages::refreshStationStrip);
        });
    }

    /** Metric / Standard switch. Rebuilds the page so the elevation field and other labels follow at once. */
    static Region unitSystemSeg() {
        int on = com.ars.fx.data.Units.standard() ? 1 : 0;
        return segSel(new String[]{"Metric", "Standard (US)"}, on, i -> {
            com.ars.fx.data.Units.System next = i == 1 ? com.ars.fx.data.Units.System.STANDARD : com.ars.fx.data.Units.System.METRIC;
            if (next == com.ars.fx.data.Units.system()) return;
            double m = com.ars.fx.data.Units.heightToMeters(parseD(com.ars.fx.data.HubConfig.get("station.elev", "0")));   // keep the same real height
            com.ars.fx.data.Units.set(next);
            com.ars.fx.data.HubConfig.set("station.elev", String.valueOf(Math.round(com.ars.fx.data.Units.heightFromMeters(m))));
            com.ars.fx.data.RemoteLink.nudgeReconfig();
            Shell.navigate("station");
        });
    }
    static double parseD(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; } }

    /** Station elevation, stored and shown in the chosen height unit (ft or m). */
    static Region elevationField() { return cUnit("station.elev", "0", com.ars.fx.data.Units.heightUnit() + " AMSL"); }
    // ---- shared config-page chrome ----------------------------------------
    static HBox cfgTop(String... crumbs) {
        HBox crumb = new HBox(7); crumb.setAlignment(Pos.BOTTOM_LEFT);
        for (int i = 0; i < crumbs.length; i++) {
            crumb.getChildren().add(lbl(crumbs[i], i == crumbs.length - 1 ? "cfg-title" : "jhub-crumb"));
            if (i < crumbs.length - 1) crumb.getChildren().add(lbl("›", "jhub-crumb"));
        }
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label back = lbl("← Dashboard", "cfg-link"); back.setStyle("-fx-cursor:hand;"); back.setOnMouseClicked(e -> Shell.navigate("hub"));
        HBox top = new HBox(crumb, sp, back); top.setAlignment(Pos.BOTTOM_LEFT);
        return top;
    }
    static Region cfgPage(Node... rows) {
        VBox v = new VBox(20, rows); v.setPadding(new Insets(18, 28, 40, 28)); v.setMaxWidth(1080); v.setStyle("-fx-background-color:-ars-bg;");
        return v;
    }

    static Region rotorCenter() {
        com.ars.fx.data.RotorClient.getInstance().start();
        com.ars.fx.data.RotorClient.satellite().start();
        VBox conn = rotorConnection("rotor.", false);
        VBox beh = section("BEHAVIOR",
            cfgRow("Rotor range", "360\u00b0 turns once; 450\u00b0 has a 90\u00b0 overlap (e.g. G-1000DXA): passes carry on past 360\u00b0 instead of unwinding", cSegApply("rotor.range", new String[]{"360\u00b0","450\u00b0"}, 0, () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Overlap / wrap", "450\u00b0 rotors only: where it overlaps. North = travel 0 to 450\u00b0; South = -180 to +270\u00b0", cSegApply("rotor.wrap", new String[]{"North","South"}, 0, () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Park position", "the Park button on the rotor panel", cUnit("rotor.park", "0", "\u00b0 az")),
            cfgRow("Elevation rotor (az/el)", "satellite tracking also points the elevation (0-90\u00b0); leave off for an azimuth-only rotor such as an HF beam's", cToggleApply("rotor.hasElevation", false, on -> { })),
            cfgRow("Dead-band", "satellite tracking skips moves smaller than this, on either axis (both rotors)", cUnit("rotor.deadband", "2", "\u00b0")),
            cfgRow("Auto-track satellites (J-Sat)", "same switch as Track with rotor in J-Sat; it moves the satellite rotor when one is switched on, otherwise this rotor", cToggle("rotor.autoTrack", false)),
            cfgRow("Show status in hero bar", "the rotor's heading and connection in the banner at the top of every screen", cToggle(com.ars.fx.shell.HeroDevices.KEY_ROTOR, false)));
        VBox presets = section("BEAM PRESETS", presetRows());

        // the optional second rotor, for satellites
        Label satNote = lbl("A second rotor for satellites, such as an az/el rotator on the satellite antenna while the rotor above turns an HF beam. J-Sat's tracking drives it; everything else (spot clicks, the beam presets, the J-Map beam) stays on the rotor above. It has its own rotctld: give it its own port (this page can start it for you).", "cfg-desc");
        satNote.setWrapText(true);
        VBox satOn = section("SATELLITE ROTOR",
            cfgRow("Use a satellite rotor", "off: J-Sat tracking uses the rotor above", cToggleApply("rotor2.enabled", false, on -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Show status in hero bar", "the satellite rotor's heading and connection in the banner (only while it is switched on above)", cToggle(com.ars.fx.shell.HeroDevices.KEY_SAT, false)));
        VBox satConn = rotorConnection("rotor2.", true);
        VBox satBeh = section("SATELLITE ROTOR BEHAVIOR",
            cfgRow("Rotor range", "360\u00b0 turns once; 450\u00b0 has a 90\u00b0 overlap: passes carry on past 360\u00b0 instead of unwinding", cSegApply("rotor2.range", new String[]{"360\u00b0","450\u00b0"}, 0, () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Overlap / wrap", "450\u00b0 rotors only: where it overlaps. North = travel 0 to 450\u00b0; South = -180 to +270\u00b0", cSegApply("rotor2.wrap", new String[]{"North","South"}, 0, () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Park position", "the Park button on the satellite rotor panel", cUnit("rotor2.park", "0", "\u00b0 az")),
            cfgRow("Elevation rotor (az/el)", "on: a pass also points the elevation (0-90\u00b0); off if this rotor only turns", cToggleApply("rotor2.hasElevation", true, on -> { })));

        return cfgPage(cfgTop("J-Hub","Hardware","Rotor control"),
            lbl("Az/el rotor control via Hamlib rotctld \u2014 connection, presets and park position. An optional second rotor can track satellites.","cfg-desc"),
            rotorStrip(com.ars.fx.data.RotorClient.getInstance()), conn, beh, presets,
            satNote, satOn, rotorStrip(com.ars.fx.data.RotorClient.satellite()), satConn, satBeh);
    }

    /** The live status strip for one rotor: status, azimuth, elevation, and where its rotctld is. */
    static HBox rotorStrip(com.ars.fx.data.RotorClient ro) {
        return liveStrip(new String[]{"STATUS", "AZIMUTH", "ELEVATION", "ROTCTLD"}, () -> {
            boolean on = ro.last().connected();
            return new String[][]{
                {on ? "\u25cf CONNECTED" : !ro.isEnabled() ? "\u25cb NOT SWITCHED ON" : ro.isConfigured() ? "\u25cb ROTCTLD OFFLINE" : "\u25cb NOT SET UP", "", on ? "1" : "0"},
                {on ? String.format("%03.0f", ro.headingDeg()) : "\u2014", on ? "\u00b0" : "", "0"},
                {on ? String.format("%.0f", ro.last().el()) : "\u2014", on ? "\u00b0" : "", "0"},
                {ro.endpoint(), "", "0"}};
        });
    }

    /** The CONNECTION section for the main rotor ({@code p} = "rotor.") or the satellite rotor ("rotor2."): the same rows under its own keys. */
    static VBox rotorConnection(String p, boolean sat) {
        return section(sat ? "SATELLITE ROTOR CONNECTION" : "CONNECTION",
            cfgRow("Rotor maker", "from the Hamlib installed on this computer (rotctl --list)", hamlibMaker(p.substring(0, p.length() - 1))),
            cfgRow("Rotor model", "choosing one fills in the Hamlib model # below", hamlibModel(p.substring(0, p.length() - 1))),
            cfgRow("Hamlib model #", "set by the model above; type a number to use a model that is not listed", hamlibNumber(p.substring(0, p.length() - 1))),
            cfgRow("Interface", null, cSeg(p + "interface", new String[]{"USB","Serial","Network"}, 1)),
            cfgRow("Serial port", null, cInput(p + "port", sat ? "/dev/ttyUSB1" : "/dev/ttyUSB0")),
            cfgRow("Baud rate", null, cSelect(p + "baud", "9600", BAUDS)),
            cfgRow("rotctld host", "Hamlib rotor daemon", cInputApply(p + "host", "127.0.0.1", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("rotctld port", sat ? "its own port, not the main rotor's" : null, cInputApply(p + "rotctldPort", sat ? "4534" : "4533", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Start rotctld for me", null, cToggleApply(p + "startDaemon", false, on -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            daemonStatusRow(p.substring(0, p.length() - 1)));
    }

    /** A status line under a "Start ... for me" switch: what the daemon is doing now, and the watchdog's last action. It updates every second while shown. */
    static HBox daemonStatusRow(String kind) {
        Label l = lbl("", "cfg-lh"); l.setWrapText(true); l.setMaxWidth(460);
        Runnable paint = () -> {
            com.ars.fx.data.DaemonStatus.Line s = com.ars.fx.data.DaemonStatus.line(kind);
            l.setText(s.text());
            l.setStyle("warn".equals(s.level()) ? "-fx-text-fill:-ars-warn;" : "");
        };
        paint.run();
        com.ars.fx.shell.Fx.everySecondWhileShown(l, paint);
        return cfgRow("Daemon status", "what J-Hub's daemon is doing, and the last thing the watchdog did", l);
    }
    static Region amplifierCenter() {
        com.ars.fx.data.AmpClient.getInstance().start();
        HBox cells = liveStrip(new String[]{"STATUS", "BAND", "SWR", "FORWARD", "AMPCTLD"}, () -> {
            com.ars.fx.data.AmpClient am = com.ars.fx.data.AmpClient.getInstance();
            com.ars.fx.data.AmpClient.State st = am.last();
            boolean on = st != null && st.connected();
            com.ars.fx.data.RigClient.State rig = com.ars.fx.data.RigClient.getInstance().last();
            String band = rig != null && rig.connected() && rig.freqHz() > 0 ? com.ars.fx.data.Bands.of(rig.freqHz()) : "";
            return new String[][]{
                {on ? "● " + st.mode().toUpperCase() : "○ OFFLINE", "", on ? "1" : "0"},
                {band.isEmpty() ? "—" : band, "", "0"},
                {on && st.swr() > 0 ? String.format("%.1f:1", st.swr()) : "—", "", "0"},
                {on && st.pwr() > 0 ? (am.overLimit() ? "⚠ " : "") + Math.round(st.pwr()) : "—", on && st.pwr() > 0 ? "W" + (am.overLimit() ? " over " + Math.round(AmpClient.maxPowerW()) : "") : "", "0"},
                {am.endpoint(), "", "0"}};
        });
        VBox conn = section("CONNECTION",
            cfgRow("Amplifier maker", "from the Hamlib installed on this computer (ampctl --list)", hamlibMaker("amp")),
            cfgRow("Amplifier model", "choosing one fills in the Hamlib model # below", hamlibModel("amp")),
            cfgRow("Hamlib model #", "set by the model above; type a number to use a model that is not listed", hamlibNumber("amp")),
            cfgRow("Interface", null, cSeg("amp.interface", new String[]{"USB","Serial","Network"}, 0)),
            cfgRow("Serial port", null, cInputPrompt("amp.port", "e.g. /dev/ttyACM0")),
            cfgRow("Baud rate", null, cSelect("amp.baud", "38400", BAUDS)),
            cfgRow("ampctld host", "Hamlib amp daemon", cInputApply("amp.host", "127.0.0.1", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("ampctld port", null, cInputApply("amp.ampctldPort", "4531", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Start ampctld for me", null, cToggleApply("amp.startDaemon", false, on -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            daemonStatusRow("amp"));
        VBox beh = section("BEHAVIOR",
            cfgRow("Follow rig band", "when the radio changes band, send its frequency to the amplifier", cToggle("amp.followBand", true)),
            cfgRow("Max power", "forward power over this is flagged on the dashboard and here; it does not change the amplifier. Also fills the {AMPMAXPOWER} macro", cUnit("amp.maxPower", "1000", "W")),
            cfgRow("Standby on TX inhibit", "put the amplifier in Standby while the rig is disconnected", cToggleApply("amp.standbyOnInhibit", true, on -> AmpClient.getInstance().applyPower())),
            cfgRow("Operate / Standby", "sent to the amplifier; it is re-sent when the amplifier reconnects, and Standby is used while the rig is disconnected (see above)", cSegApply("amp.opStandby", new String[]{"Operate","Standby"}, 1, () -> AmpClient.getInstance().applyPower())),
            cfgRow("Show status in hero bar", "the amplifier's operate / standby state and SWR in the banner at the top of every screen", cToggle(com.ars.fx.shell.HeroDevices.KEY_AMP, false)));
        return cfgPage(cfgTop("J-Hub","Hardware","Amplifier"),
            lbl("Linear amplifier control via Hamlib ampctld — band-follow, power limit and standby.","cfg-desc"),
            cells, conn, beh);
    }
    static Region antennaSwitchCenter() {
        HBox cells = liveStrip(new String[]{"STATUS", "ACTIVE", "ANTENNA", "PORTS"}, () -> {
            int named = 0;
            for (int i = 1; i <= 8; i++) if (!com.ars.fx.data.HubConfig.get("ant.port" + i, "").isBlank()) named++;
            return new String[][]{
                {"○ NO CONTROL", "", "0"}, {"—", "", "0"}, {"—", "", "0"},
                {named == 0 ? "—" : String.valueOf(named), named == 0 ? "" : "named", "0"}};
        });
        VBox conn = section("CONNECTION",
            cfgRow("Switch model", null, cInputPrompt("ant.model", "e.g. 4O3A Antenna Genius")),
            cfgRow("Interface", null, cSeg("ant.interface", new String[]{"USB","Serial","Network"}, 2)),
            cfgRow("Host / IP", null, cInputPrompt("ant.host", "e.g. 192.168.1.50")),
            cfgRow("Port", null, cInput("ant.port", "9007")),
            cfgRow("Follow rig band", "select antenna by band", cToggle("ant.followBand", true)));
        String[][] ports = {{"1","160m dipole","160m"},{"2","80m vertical","80m"},{"3","20m beam","20–10m"},
                {"4","40m delta loop","40m"},{"5","6m yagi","6m"},{"6","RX loop","RX only"}};
        Node[] rows = new Node[ports.length];
        for (int i = 0; i < ports.length; i++) rows[i] = cfgRow("Port " + ports[i][0], "bands " + ports[i][2], cInput("ant.port" + ports[i][0], ports[i][1]));
        VBox ant = section("ANTENNA PORTS", rows);
        return cfgPage(cfgTop("J-Hub","Hardware","Antenna switch"),
            lbl("Port names for your antenna switch, saved for reference and macros. Switch control is not built in yet.","cfg-desc"),
            cells, conn, ant);
    }
    /** One part of what used to be the single long Data page; each now has its own entry in the Setup menu. */
    static Region dataPage(String which) {
        HBox cells = liveStrip(new String[]{"CLUSTER", "RBN", "WSJT-X", "LOG DB"}, () -> {
            com.ars.fx.data.ClusterClient cc = com.ars.fx.data.ClusterClient.getInstance();
            com.ars.fx.data.HeardByClient hb = com.ars.fx.data.HeardByClient.getInstance();
            boolean wsjtx = com.ars.fx.data.BridgeService.connected();
            return new String[][]{
                {cc.status().label(), cc.endpoint(), cc.isConnected() ? "1" : "0"},
                {hb.status().label(), hb.endpoint(), hb.isConnected() ? "1" : "0"},
                {wsjtx ? "● linked" : "○ waiting", "UDP " + com.ars.fx.data.BridgeService.port(), wsjtx ? "1" : "0"},
                {com.ars.fx.data.LogDbInfo.pretty(com.ars.fx.data.LogDbInfo.dataDir()), "", "0"}};
        }, 260);
        Runnable applyCluster = () -> {
            String srv = com.ars.fx.data.HubConfig.get("data.clusterServer", "dxc.ve7cc.net:23").trim();
            String login = com.ars.fx.data.HubConfig.get("data.clusterLogin", com.ars.fx.data.HubConfig.call());
            String host = srv; int port = 23; int c = srv.lastIndexOf(':');
            if (c > 0) { host = srv.substring(0, c); try { port = Integer.parseInt(srv.substring(c + 1).trim()); } catch (Exception ignored) {} }
            ClusterClient.getInstance().setEndpoint(host, port, login);
        };
        VBox cluster = section("DX CLUSTER",
            cfgRow("Cluster server", "pick one, or type host:port and press Enter; one runs at a time, and a live connection moves to the new server",
                    tagged(clusterPicker(() -> com.ars.fx.data.HubConfig.get("data.clusterLogin", com.ars.fx.data.HubConfig.call())), "data.clusterServer")),
            cfgRow("Login callsign", null, cInputApply("data.clusterLogin", com.ars.fx.data.HubConfig.call(), applyCluster)),
            cfgRow("Auto-connect on start", "off by default — connect from the cluster panel when you want spots", cToggle("data.clusterAutoConnect", false)),
            cfgRow("Connection tries", "give up after this many failed attempts (0 = keep trying); Connect starts a fresh count",
                    cInputApply("data.clusterMaxTries", "5", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Band filter", null, cSeg("data.clusterBand", new String[]{"All","HF","VHF+"}, 0)),
            cfgRow("Mode filter", null, cSeg("data.clusterMode", new String[]{"All","CW","Phone","Digi"}, 0)),
            cfgRow("RBN connection tries", "give up after this many failed attempts (0 = keep trying)",
                    cInputApply("data.rbnMaxTries", "5", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("RBN auto-connect on start", "off by default — \"who's hearing me\" skimmer spots; Connect from the Heard By panel",
                    cToggle("data.rbnAutoConnect", false)),
            cfgRow("Spot alerts", "a card in the corner (and a chime) when a spot arrives that you want; the same call on the same band alerts once every 30 minutes",
                    cToggle(com.ars.fx.data.SpotAlerts.KEY_ON, false)),
            cfgRow("Alert on needed spots", "spots tagged NEW ONE / NEW BAND / NEW MODE (a new multiplier on the contest page)",
                    cToggle(com.ars.fx.data.SpotAlerts.KEY_NEEDED, true)),
            cfgRow("Watch list", "calls or prefixes to alert on, separated by commas: K1ABC, VP8*, 3Y0J",
                    cInput(com.ars.fx.data.SpotAlerts.KEY_WATCH, "")),
            cfgRow("Alert chime", null, cToggle(com.ars.fx.data.SpotAlerts.KEY_SOUND, true)),
            cfgRow("Alert on Club Log Most Wanted", "alert on spots of entities in the Most Wanted top N (0 = off; needs the Club Log token below)",
                    cInput(com.ars.fx.data.SpotAlerts.KEY_MW_TOP, "0")),
            cfgRow("Club Log token", "for Club Log's Most Wanted ranking: create one at Club Log ▸ Settings ▸ API (it starts cl_). The list is fetched once a month",
                    mostWantedControls()),
            cfgRow("Club Log DXCC exceptions", "calls Club Log places by hand for a date range (DXpeditions, special events), checked before the prefix tables. Uses the Club Log API key from Online log upload; fetched once a day",
                    clubLogCtyControls()));
        VBox wsjt = section("WSJT-X / DIGITAL",
            cfgRow("WSJT-X UDP port", "set the same port in WSJT-X ▸ Settings ▸ Reporting; applied as soon as you leave the field", cInputApply("data.wsjtxPort", "2237", com.ars.fx.data.BridgeService::restart)),
            cfgRow("Local CW skimmer", "J-Digi listens to the whole audio passband for CW, picks out callsigns it is confident of and adds them to the spot list as \"-SKM\" spots. Needs the rig connected (Rig control) and uses some CPU",
                    cToggle("digi.localSkimmer", false)),
            cfgRow("Auto-log FT8/FT4 QSOs", "adds each QSO WSJT-X logs to the J-Log database (skips one already there)", cToggle("data.autoLogFt8", false)));
        VBox logbook = section("LOGBOOK & EXPORT",
            cfgRow("Cabrillo / CSV charset", "ASCII drops accents and replaces other characters with ?",  cSeg("data.charset", new String[]{"UTF-8","ASCII"}, 0)),
            cfgRow("LoTW / eQSL auto-upload", "after each QSO you log, send the ones not yet sent to LoTW and eQSL, whichever is switched on in the Upload section below. Only QSOs logged after this version are sent this way; older ones go through the Upload section. A failed upload is tried again after the next QSO", cToggle("data.lotwAutoUpload", false)),
            cfgRow("Super Check Partial", "the contest call check: downloads the call list from supercheckpartial.com when a contest page is opened - MASTER.SCP (about 350 KB), or MASTERSS / MASTERDX / MASUSVE (100-260 KB) for Sweepstakes and ARRL DX - at most once a day for each, and only when it has changed. Nothing about you or your log is sent", cToggle(com.ars.fx.data.ScpStore.KEY_AUTO, true)));
        VBox remote = section("REMOTE SHARING (SOLO J-MAP / J-SAT)",
            cfgRow("Share with solo modules on the LAN", "lets a standalone J-Map / J-Sat (2nd PC, shack Pi) use this station's spots, rig + rotor. Off: only this computer can reach the hub. On: any device on your network can see the spots and tune / rotate, so turn it on only on a network you trust",
                    cToggleApply("remote.serverEnabled", false, on -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("WebSocket port", "the solo module connects to ws://<this-pc>:<port>",
                    cInputApply("remote.port", "8090", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())));
        VBox phone = section("PHONE WEB CLIENT",
            cfgRow("Phone web client", "a page your phone opens on your own network to log QSOs (POTA / SOTA), watch the DX spots and see the rig. Off until you turn it on. It is plain HTTP, so use it on your own network only; to reach it from outside, use a VPN. QSOs typed while the signal is gone wait on the phone and are sent, with their real times, when it returns", cToggle(com.ars.fx.web.WebClientServer.KEY_ENABLED, false)),
            cfgRow("Port", "the page is at http://<this computer>:<port>/", cInput(com.ars.fx.web.WebClientServer.KEY_PORT, String.valueOf(com.ars.fx.web.WebClientServer.DEFAULT_PORT))),
            cfgRow("Access token", "the phone must give this; it is made for you the first time. Change it to lock out a phone that should no longer log", cInput(com.ars.fx.web.WebClientServer.KEY_TOKEN, com.ars.fx.web.WebClientServer.enabled() ? com.ars.fx.web.WebClientServer.token() : "")),
            cfgRow("Address to open", com.ars.fx.web.WebClientServer.enabled() ? "open this on the phone once; it keeps the token" : "turn the client on first",
                    lbl(phoneUrl(), "cfg-lh")),
            cfgRow("Tune the rig from the phone", "lets the Spots page tune the rig: only while it is connected and not transmitting, and only inside an amateur band", cToggle(com.ars.fx.web.WebClientServer.KEY_CONTROL, false)));
        VBox n1mm = section("N1MM-COMPATIBLE BROADCAST (GRIDTRACKER, LOG4OM …)",
            cfgRow("Broadcast the log", "sends each contest QSO (call, band, mode, exchange) over UDP in N1MM+'s format so other programs can follow your log live. Off until you turn it on, and it goes only to the addresses below", cToggle(com.ars.fx.data.N1mmBroadcast.KEY_ENABLED, false)),
            cfgRow("Send to", "ip:port pairs separated by spaces. 127.0.0.1:12060 is this PC; a subnet broadcast address such as 192.168.1.255:12060 reaches every PC on the network", cInput(com.ars.fx.data.N1mmBroadcast.KEY_DEST, com.ars.fx.data.N1mmBroadcast.DEFAULT_DEST)),
            cfgRow("Contacts", "QSOs added, edited and erased", cToggle(com.ars.fx.data.N1mmBroadcast.KEY_CONTACTS, true)),
            cfgRow("Lookups", "the call as you type it, when you press Space", cToggle(com.ars.fx.data.N1mmBroadcast.KEY_LOOKUP, true)),
            cfgRow("Radio", "the rig's frequency and mode, when they change and every 10 seconds", cToggle(com.ars.fx.data.N1mmBroadcast.KEY_RADIO, true)),
            cfgRow("Score", "the contest score, for N1MM+ score reporting (the categories come from Contest / Cabrillo below)", cToggle(com.ars.fx.data.N1mmBroadcast.KEY_SCORE, false)),
            cfgRow("Spots", "each DX cluster spot as it arrives, in N1MM+'s spot message (a busy cluster is many packets a minute)", cToggle(com.ars.fx.data.N1mmBroadcast.KEY_SPOTS, false)),
            cfgRow("Normal log", "QSOs you log in the normal log, as plain contacts with no contest, so a program that marks calls worked sees them too", cToggle(com.ars.fx.data.N1mmBroadcast.KEY_NORMAL, true)),
            cfgRow("All computers", "contacts that arrive from other computers on the contest network, passed on the way N1MM+'s All Computers does. Leave it off unless a program needs every station's QSOs", cToggle(com.ars.fx.data.N1mmBroadcast.KEY_ALLCOMP, false)),
            cfgRow("Receive QSY", "listen on this computer only (UDP 13064) for a waterfall or band map that clicks to tune the rig. Acts only while the rig is connected and not transmitting, and only inside an amateur band", cToggle(com.ars.fx.data.N1mmListener.KEY_LISTEN, false)),
            cfgRow("QSY from the network", "also hear a waterfall or band map on another computer on your network. Only private addresses are accepted, never the internet", cToggle(com.ars.fx.data.N1mmListener.KEY_LAN, false)),
            cfgRow("QSY only from", "IP addresses allowed to tune the rig, separated by spaces (blank = any computer on your network)", cInput(com.ars.fx.data.N1mmListener.KEY_FROM, "")),
            cfgRow("Contacts to", "an address for contacts alone (blank = the address above)", cInput(com.ars.fx.data.N1mmBroadcast.KEY_DEST + ".contacts", "")),
            cfgRow("Spots to", "an address for spots alone (blank = the address above)", cInput(com.ars.fx.data.N1mmBroadcast.KEY_DEST + ".spots", "")),
            cfgRow("Radio to", "an address for the rig's frequency alone (blank = the address above)", cInput(com.ars.fx.data.N1mmBroadcast.KEY_DEST + ".radio", "")),
            cfgRow("Score to", "an address for the score alone (blank = the address above)", cInput(com.ars.fx.data.N1mmBroadcast.KEY_DEST + ".score", "")));
        VBox network = section("CONTEST NETWORK (MULTI-OPERATOR)",
            cfgRow("Network mode", "Off, or Host (this station is the one the others join) or Join (connect to the host). Every station keeps its own full copy of the log, so one that loses the host keeps logging and catches up when it is back", cSelect(com.ars.fx.net.ContestNetwork.KEY_MODE, "Off", "Off", "Host", "Join")),
            cfgRow("Shared key", "the same on every station, at least 4 characters. It is never sent, only a proof that you know it. The QSOs themselves cross your LAN unencrypted, so use this on a network you trust", networkKeyField()),
            cfgRow("Host address", "Join only: the host station's IP address or name, such as 192.168.1.20. Leave it blank to find the host on the network by itself (the host announces its name and port, never the key)", cInput(com.ars.fx.net.ContestNetwork.KEY_HOST, "")),
            cfgRow("Announce this host", "Host only: tell the local network this host is here, so a station joining with no address can find it", cToggle(com.ars.fx.net.ContestNetwork.KEY_ANNOUNCE, true)),
            cfgRow("Port", "the host listens on it and the others connect to it (default 12070); open it in the host's firewall", cInput(com.ars.fx.net.ContestNetwork.KEY_PORT, String.valueOf(com.ars.fx.net.ContestNetwork.DEFAULT_PORT))),
            cfgRow("Station name", "this computer's name on the network, unique on it (default: the computer's name). It is recorded on each QSO you log", cInput(com.ars.fx.net.ContestNetwork.KEY_STATION, "")),
            cfgRow("Operator", "who is at this station (default: your call), shown to the other stations", cInput(com.ars.fx.net.ContestNetwork.KEY_OP, "")));
        VBox contest = section("CONTEST / CABRILLO",
            cfgRow("Operator category", null, cSelect("contest.cab.operator", "SINGLE-OP", new String[]{"SINGLE-OP","MULTI-OP","CHECKLOG"})),
            cfgRow("Power", "QSO parties use it for their power multiplier (QRP / LOW / HIGH as each party defines them)",
                    cSelect("contest.cab.power", "HIGH", new String[]{"HIGH","LOW","QRP"})),
            cfgRow("Assisted", null, cSelect("contest.cab.assisted", "NON-ASSISTED", new String[]{"NON-ASSISTED","ASSISTED"})),
            // the Cabrillo 3.0 values, so a typo cannot break the header (a value saved before stays in the list until changed)
            cfgRow("Band category", null, cSelect("contest.cab.band", "ALL", new String[]{"ALL","160M","80M","40M","20M","15M","10M","6M","4M",
                    "2M","222","432","902","1.2G","2.3G","3.4G","5.7G","10G","24G","47G","75G","122G","134G","241G","LIGHT","VHF-3-BAND","VHF-FM-ONLY"})),
            cfgRow("Mode category", null, cSelect("contest.cab.mode", "MIXED", new String[]{"MIXED","CW","SSB","DIGI","RTTY","FM"})),
            cfgRow("Station", "QSO parties use it: MOBILE, ROVER and PORTABLE entries get the county bonuses and re-work stations from each new county; MD-DC multiplies by it",
                    cSelect("contest.cab.station", "FIXED", new String[]{"FIXED","MOBILE","PORTABLE","ROVER","ROVER-LIMITED","ROVER-UNLIMITED",
                    "EXPEDITION","HQ","SCHOOL","EXPLORER","DISTRIBUTED"})),
            cfgRow("Club", null, cInput("contest.cab.club", "")),
            cfgRow("Mixed-mode contests open in", "QSO parties, Field Day, VHF and others that allow CW and phone. Auto follows the rig, else SSB", cSelect("contest.defaultMode", "Auto", "Auto", "SSB", "CW")),
            cfgRow("Sweepstakes precedence", "your sent SS precedence (A/B/U/M/Q/S)", cInput("contest.ss.precedence", "")),
            cfgRow("Sweepstakes check", "year first licensed (2 digits)", cInput("contest.ss.check", "")),
            cfgRow("Sweepstakes section", "your ARRL/RAC section", cInput("contest.ss.section", "")));
        return switch (which) {
            case "tx" -> cfgPage(cfgTop("J-Hub", "Hardware", "Transmit & macros"),
                lbl("CW speed, keyer, PTT delay and voice output, and the F-key macros used by J-Log and J-Digi.", "cfg-desc"),
                txSection(), macroSection());
            case "cluster" -> cfgPage(cfgTop("J-Hub", "Data & spotting", "DX cluster & RBN"),
                lbl("Where spots come from: the DX cluster server and login, and the Reverse Beacon Network (\"who is hearing me\").", "cfg-desc"),
                cells, cluster);
            case "digital" -> cfgPage(cfgTop("J-Hub", "Data & spotting", "WSJT-X / digital"),
                lbl("The WSJT-X / JTDX link, the local CW skimmer and automatic logging of digital QSOs.", "cfg-desc"),
                cells, wsjt);
            case "lookup" -> cfgPage(cfgTop("J-Hub", "Data & spotting", "Callsign lookup"),
                lbl("Callbook providers J-Log and J-Map ask for a station's name, QTH and grid, tried top to bottom.", "cfg-desc"),
                lookupSection());
            case "logdb" -> cfgPage(cfgTop("J-Hub", "Logbook", "Log database & backup"),
                lbl("Which database files J-Log writes to, the contest databases, and ADIF / CSV backup.", "cfg-desc"),
                cells, logDatabaseSection(), JHubContestDbs.build(), callHistorySection(), backupSection());
            case "upload" -> cfgPage(cfgTop("J-Hub", "Logbook", "Online upload"),
                lbl("Push your log to LoTW, eQSL, QRZ Logbook and Club Log.", "cfg-desc"),
                uploadSection());
            case "labels" -> cfgPage(cfgTop("J-Hub", "Logbook", "Label printers"),
                lbl("Print a QSL label for each QSO as it is logged. Tick \"Print QSL label\" on the J-Log form to turn it on.", "cfg-desc"),
                labelSection());
            case "export" -> cfgPage(cfgTop("J-Hub", "Logbook", "Export & Cabrillo"),
                lbl("Export character set and the entry details written into Cabrillo contest logs.", "cfg-desc"),
                logbook, phone, n1mm, network, contest);
            case "share" -> cfgPage(cfgTop("J-Hub", "System", "Remote sharing"),
                lbl("Let a solo J-Map / J-Sat on another computer use this station's spots, rig and rotor.", "cfg-desc"),
                remote);
            case "learn" -> cfgPage(cfgTop("J-Hub", "System", "J-Learn content"),
                lbl("Update the J-Learn chapters separately from the program.", "cfg-desc"),
                JHubLearnContent.build());
            default -> cfgPage(cfgTop("J-Hub", "Setup"), lbl("Pick a section from the menu on the left.", "cfg-desc"));
        };
    }

    /** Which database files J-Log writes to, with their sizes and a way to open the folder. The export and
     *  upload tools follow directly below on this page. */
    static VBox logDatabaseSection() {
        com.ars.fx.data.ContestState.ensureLoaded();   // brings the contest engine up so "in use" is the real file
        java.nio.file.Path normal = com.ars.fx.data.LogDbInfo.normalPath(), contest = com.ars.fx.data.LogDbInfo.contestPath();
        Label nCount = lbl(JLogDb.count() + " QSOs · " + com.ars.fx.data.LogDbInfo.size(normal), "cfg-lt");
        com.ars.fx.data.ContestState.ensureLoaded();
        var plug = com.ars.fx.data.ContestState.plugin();
        Label cCount = lbl((plug == null ? "no contest selected" : plug.getContestName() + " · " + com.ars.fx.data.ContestState.qsos().size() + " QSOs")
                + " · " + com.ars.fx.data.LogDbInfo.size(contest), "cfg-lt");
        Label status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        Label open = cfgBtn("Open folder");
        open.setOnMouseClicked(e -> status.setText(com.ars.fx.data.LogDbInfo.openFolder(com.ars.fx.data.LogDbInfo.dataDir())
                ? "Opened " + com.ars.fx.data.LogDbInfo.pretty(com.ars.fx.data.LogDbInfo.dataDir())
                : "Could not open the folder — it is " + com.ars.fx.data.LogDbInfo.dataDir()));
        Label toLog = cfgBtn("Open J-Log"); toLog.setOnMouseClicked(e -> Shell.navigate("logc"));
        HBox btns = new HBox(10, open, toLog); btns.setAlignment(Pos.CENTER_LEFT);
        Region bsp = new Region(); HBox.setHgrow(bsp, Priority.ALWAYS);
        HBox actions = new HBox(20, new VBox(2, lbl("Database folder", "cfg-lt"),
                lbl("Back up, export and upload are in the sections below", "cfg-lh")), bsp, btns);
        actions.getStyleClass().add("cfg-row"); actions.setAlignment(Pos.CENTER_LEFT);
        return section("LOG DATABASE",
            cfgRow("Normal log", com.ars.fx.data.LogDbInfo.normalLine(), nCount),
            cfgRow("Contest log (in use)", com.ars.fx.data.LogDbInfo.contestLine(), cCount),
            actions,
            new HBox(status) {{ getStyleClass().add("cfg-row"); }});
    }

    /** J-Hub ▸ Modules — turn each module on or off (reflected live in the dock + nav). */
    /** Setup ▸ Appearance: the whole-interface size and the dashboard look. */
    static Region appearanceCenter() {
        String[] looks = new String[com.ars.fx.UiStyle.DASHBOARD_STYLES.size()];
        for (int i = 0; i < looks.length; i++) looks[i] = com.ars.fx.UiStyle.DASHBOARD_STYLES.get(i).label();
        String curLook = com.ars.fx.UiStyle.dashboard().label();
        int on = 0; for (int i = 0; i < looks.length; i++) if (looks[i].equals(curLook)) on = i;
        Region lookSeg = segSel(looks, on, i -> {
            com.ars.fx.data.HubConfig.set(com.ars.fx.UiStyle.DASHBOARD_KEY, com.ars.fx.UiStyle.DASHBOARD_STYLES.get(i).key());
            // live: rebuild Setup on this same section (re-applies the stylesheet); the dashboard and other screens are rebuilt each time they are opened
            javafx.application.Platform.runLater(() -> Shell.navigate("setup:appearance"));
        });
        Label theme = lbl("◑ " + com.ars.fx.Themes.byKey(com.ars.fx.Themes.global()).label(), "sx-theme");
        theme.setStyle("-fx-cursor:hand;");
        theme.setOnMouseClicked(e -> theme.setText("◑ " + Shell.cycleGlobalTheme().label()));
        VBox colour = section("COLOUR",
            cfgRow("Colour scheme", "click to switch the scheme for the whole suite", theme),
            cfgRow("Reduce effects", "no sliding, flashing or glowing when something new arrives (new spots, changed counts, live lights)", cToggle("ui.reduceEffects", false)));
        VBox size = section("SIZE",
            cfgRow("Interface size", "scales the whole window together (text, widths, drawers); applies at once. Pop-up windows keep their own size",
                    cSeg(com.ars.fx.UiStyle.SCALE_KEY, com.ars.fx.UiStyle.SCALES, com.ars.fx.UiStyle.scaleDefaultIndex())));
        VBox layout = section("DASHBOARD LAYOUT",
            cfgRow("Stack cards in rows", "New QSO, DX Cluster and the Recent log each get a row. Off puts New QSO and DX Cluster side by side when there is room. Open the dashboard again to see a change",
                    cToggle("ui.dashboard.rows", true)),
            cfgRow("DX Cluster in the right column", "the DX Cluster and its controls go to the top of the PANELS column (one line per spot; hover a spot for the spotter and comment), and the Recent log takes the cluster's place under New QSO. Pin PANELS to keep the cluster in view. Open the dashboard again to see a change",
                    cToggle(CLUSTER_RIGHT_KEY, false)));
        VBox look = section("LOOK",
            cfgRow("Style", "Classic is the original look. A change shows at once; pick Classic to go back at any time", lookSeg));
        return cfgPage(cfgTop("J-Hub", "System", "Appearance"),
                lbl("How the suite looks. The style applies to every screen and window; Classic is the original look.", "cfg-desc"),
                colour, size, layout, look);
    }

    static Region modulesCenter() {
        java.util.List<Node> rows = new java.util.ArrayList<>();
        for (com.ars.fx.data.ModuleConfig.Mod m : com.ars.fx.data.ModuleConfig.all()) {
            Node tile = Shell.iconTile(m.hue(), m.id(), 13, "sx-dw-ic");
            Label nm = lbl(m.name(), "k"); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
            VBox txt = new VBox(1, nm, lbl(com.ars.fx.data.ModuleConfig.blurb(m.id()), "jhub-card-sub"));
            HBox.setHgrow(txt, Priority.ALWAYS); txt.setMaxWidth(Double.MAX_VALUE);
            HBox left = new HBox(11, tile, txt); left.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(left, Priority.ALWAYS); left.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(12, left,
                    cToggleApply("module." + m.id() + ".enabled", true, on -> Shell.navigate("setup:modules")));
            row.getStyleClass().add("sx-kv"); row.setAlignment(Pos.CENTER_LEFT);
            rows.add(row);
        }
        VBox list = section("MODULES", rows.toArray(new Node[0]));
        return cfgPage(cfgTop("J-Hub", "Modules"),
                lbl("Turn modules on or off. Disabled modules are hidden from the dock and the J-Hub nav — J-Hub itself is always on.", "cfg-desc"),
                list);
    }
    /** Online log upload — LoTW (TQSL-signed), eQSL, QRZ Logbook, Club Log. */
    static VBox uploadSection() {
        Label status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        java.util.function.Consumer<com.ars.fx.data.LogUpload.Result> report = r ->
                status.setText((r.ok() ? "✓ " : "✗ ") + r.message());

        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        // TQSL location (global, for LoTW)
        String detected = com.ars.fx.data.LogUpload.resolveTqsl();
        TextField tqsl = macroField(com.ars.fx.data.UploadConfig.tqslPath(), 290);
        tqsl.setPromptText(detected != null ? "auto-detected: " + detected : "path to tqsl (TrustedQSL not found on PATH)");
        tqsl.textProperty().addListener((o, a, b) -> com.ars.fx.data.UploadConfig.setTqslPath(tqsl.getText()));
        card.getChildren().add(cfgRow("TrustedQSL (tqsl) path", detected != null ? "detected — override only if needed" : "required for LoTW — install from arrl.org/lotw", tqsl));

        for (com.ars.fx.data.UploadConfig.Service s : com.ars.fx.data.UploadConfig.SERVICES) {
            CheckBox en = new CheckBox(); en.setSelected(com.ars.fx.data.UploadConfig.enabled(s.id()));
            en.selectedProperty().addListener((o, a, b) -> com.ars.fx.data.UploadConfig.setEnabled(s.id(), en.isSelected()));
            HBox fields = new HBox(8); fields.setAlignment(Pos.CENTER_RIGHT);
            for (com.ars.fx.data.UploadConfig.Field f : s.fields()) {
                TextField tf = f.secret() ? new PasswordField() : new TextField();        // passwords are masked
                tf.setText(com.ars.fx.data.UploadConfig.field(s.id(), f.key()));
                tf.getStyleClass().add("jhub-mini-field"); tf.setPromptText(f.label()); tf.setMinWidth(120); tf.setMaxWidth(150);
                saveWhenSettled(tf, () -> { String v = tf.getText(); return () -> com.ars.fx.data.UploadConfig.setField(s.id(), f.key(), v); });
                if (s.id().equals("eqsl") && f.secret()) {            // eQSL's download refuses a '#' in the password (its server, not us)
                    Runnable hashNote = () -> { if (eqslHashNote(tf.getText()) != null) status.setText(eqslHashNote(tf.getText())); };
                    tf.textProperty().addListener((o, a, b) -> hashNote.run());
                    hashNote.run();
                }
                fields.getChildren().add(tf);
            }
            Label up = cfgBtn("Upload"); up.setOnMouseClicked(e -> {
                status.setText("⋯ uploading to " + s.label() + "…");
                final String id = s.id();
                flushSaves();
                Thread t = new Thread(() -> {
                    waitForSaves();                                                   // a password typed just now is the one used
                    com.ars.fx.data.LogUpload.Result r = switch (id) {
                        case "lotw" -> com.ars.fx.data.LogUpload.lotw();
                        case "eqsl" -> com.ars.fx.data.LogUpload.eqsl();
                        case "qrz_logbook" -> com.ars.fx.data.LogUpload.qrzLogbook();
                        case "clublog" -> com.ars.fx.data.LogUpload.clublog();
                        case "wavelog" -> com.ars.fx.data.LogUpload.wavelog();
                        default -> new com.ars.fx.data.LogUpload.Result(false, 0, "unknown service");
                    };
                    javafx.application.Platform.runLater(() -> report.accept(r));
                }, "log-upload");
                t.setDaemon(true); t.start();
            });
            if (s.id().equals("lotw") || s.id().equals("eqsl")) {
                Label dl = cfgBtn("Get confirmations");
                dl.setOnMouseClicked(e -> {
                    status.setText("\u22ef asking " + s.label() + " for your confirmations\u2026");
                    final String id = s.id();
                    flushSaves();
                    Thread t = new Thread(() -> {
                        waitForSaves();
                        com.ars.fx.data.ConfirmSync.Result r = id.equals("lotw") ? com.ars.fx.data.ConfirmSync.lotw() : com.ars.fx.data.ConfirmSync.eqsl();
                        javafx.application.Platform.runLater(() -> status.setText((r.ok() ? "\u2713 " : "\u2717 ") + r.service() + ": " + r.message()));
                    }, "confirm-sync");
                    t.setDaemon(true); t.start();
                });
                fields.getChildren().add(dl);
            }
            fields.getChildren().addAll(up, en);
            card.getChildren().add(cfgRow(s.label(), s.hint(), fields));
        }
        if (!card.getChildren().isEmpty()) card.getChildren().get(card.getChildren().size() - 1).setStyle("-fx-border-width:0;");
        HBox statusRow = new HBox(status); statusRow.getStyleClass().add("cfg-row"); statusRow.setStyle("-fx-border-width:0;");
        return new VBox(6, lbl("ONLINE LOG UPLOAD", "cfg-sec"),
                lbl("Push your log to the online confirmation services. LoTW is signed with your TrustedQSL certificate.", "cfg-lh"),
                card, statusRow);
    }
    /** Callsign-lookup providers (QRZ / HamCall / Callook / HamQTH / QRZCQ), tried top-to-bottom. */
    static VBox lookupSection() {
        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        for (com.ars.fx.data.LookupConfig.Prov p : com.ars.fx.data.LookupConfig.PROVIDERS) {
            com.ars.fx.data.LookupConfig.Setting cur = com.ars.fx.data.LookupConfig.get(p.id());
            CheckBox en = new CheckBox(); en.setSelected(cur.enabled());
            TextField user = null, secret = null, path = null;
            HBox controls = new HBox(8); controls.setAlignment(Pos.CENTER_RIGHT);
            if (p.creds()) {
                user = macroField(cur.user(), 130); user.setPromptText("user / call");
                secret = new PasswordField(); secret.setText(cur.secret()); secret.getStyleClass().add("jhub-mini-field"); secret.setPromptText("password / key"); secret.setMinWidth(150); secret.setMaxWidth(150);
                controls.getChildren().addAll(user, secret);
            } else if (p.path()) {
                path = macroField(cur.extra(), 290); path.setPromptText("~/callbook.csv  (call,name,city,state,county,country,grid)");
                controls.getChildren().add(path);
            }
            controls.getChildren().add(en);
            final TextField fu = user, fs = secret, fp = path;
            java.util.function.Supplier<Runnable> persist = () -> {                 // read on the FX thread, saved on the save thread
                boolean on = en.isSelected();
                String u = fu == null ? "" : fu.getText(), sc = fs == null ? "" : fs.getText(), x = fp == null ? "" : fp.getText();
                return () -> com.ars.fx.data.LookupConfig.set(p.id(), on, u, sc, x);
            };
            en.selectedProperty().addListener((o, a, b) -> SAVE.execute(persist.get()));
            if (fu != null) saveWhenSettled(fu, persist);
            if (fs != null) saveWhenSettled(fs, persist);
            if (fp != null) saveWhenSettled(fp, persist);
            card.getChildren().add(cfgRow(p.label(), p.hint(), controls));
        }
        if (!card.getChildren().isEmpty()) card.getChildren().get(card.getChildren().size() - 1).setStyle("-fx-border-width:0;");
        return new VBox(6, lbl("CALLSIGN LOOKUP", "cfg-sec"),
                lbl("Tried top-to-bottom; first hit wins. J-Log autofills name / QTH / grid / state / county from the CALLSIGN field.", "cfg-lh"),
                card);
    }
    /** J-Hub ▸ Setup ▸ Label printers: model, label stock, destination, and a test print. */
    static VBox labelSection() {
        Label status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        VBox dyn = new VBox();                        // rows that depend on the chosen model; rebuilt on change

        ComboBox<com.ars.fx.data.QslLabel.Model> model = new ComboBox<>();
        model.getItems().addAll(com.ars.fx.data.QslLabel.MODELS);
        model.setConverter(new javafx.util.StringConverter<>() {
            public String toString(com.ars.fx.data.QslLabel.Model m) { return m == null ? "" : m.label(); }
            public com.ars.fx.data.QslLabel.Model fromString(String s) { return null; }
        });
        model.setValue(com.ars.fx.data.QslLabel.currentModel()); model.setMinWidth(260);

        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            com.ars.fx.data.QslLabel.Model m = model.getValue();
            dyn.getChildren().clear();
            boolean zpl = m.zpl() && "zpl".equals(com.ars.fx.data.QslLabel.mode(m));

            if (m.zpl()) {
                ComboBox<String> mode = new ComboBox<>(); mode.getItems().addAll("Native ZPL (recommended)", "System driver");
                mode.setValue(zpl ? mode.getItems().get(0) : mode.getItems().get(1)); mode.setMinWidth(260);
                mode.valueProperty().addListener((o, a, b) -> {
                    com.ars.fx.data.HubConfig.set("label.mode", b.startsWith("Native") ? "zpl" : "driver"); rebuild[0].run(); });
                dyn.getChildren().add(cfgRow("Print method", "ZPL is sent straight to the printer; the driver rasterises the label", mode));
            }

            ComboBox<com.ars.fx.data.QslLabel.Size> size = new ComboBox<>();
            size.getItems().addAll(com.ars.fx.data.QslLabel.sizesFor(m));
            size.setConverter(new javafx.util.StringConverter<>() {
                public String toString(com.ars.fx.data.QslLabel.Size s) { return s == null ? "" : s.label(); }
                public com.ars.fx.data.QslLabel.Size fromString(String s) { return null; }
            });
            com.ars.fx.data.QslLabel.Size cur = com.ars.fx.data.QslLabel.currentSize(m);
            String curId = com.ars.fx.data.HubConfig.get("label.size", "");
            for (com.ars.fx.data.QslLabel.Size s : size.getItems()) if (s.id().equals(curId.isEmpty() ? cur.id() : curId)) size.setValue(s);
            if (size.getValue() == null) size.setValue(size.getItems().get(0));
            size.setMinWidth(320);
            size.valueProperty().addListener((o, a, b) -> {
                if (b != null) { com.ars.fx.data.HubConfig.set("label.size", b.id()); rebuild[0].run(); } });
            dyn.getChildren().add(cfgRow("Label stock", "must match the roll loaded in the printer", size));
            if ("custom".equals(size.getValue().id()))
                dyn.getChildren().add(cfgRow("Custom size (mm)", "width × height of the label",
                        new HBox(8, cInput("label.customW", "62"), lbl("×", "cfg-lh"), cInput("label.customH", "29"))));

            if (zpl) {
                dyn.getChildren().add(cfgRow("Network address", "printer IP or host[:port] — port 9100 if omitted; leave blank to use a queue below",
                        cInputPrompt("label.host", "192.168.1.50")));
                dyn.getChildren().add(cfgRow("Print resolution", "dots per inch of the print head",
                        cSelect("label.dpi", String.valueOf(m.dpi()), "203", "300", "600")));
            }
            java.util.List<String> sys = com.ars.fx.data.QslLabel.systemPrinters();
            ComboBox<String> pr = new ComboBox<>(); pr.getItems().addAll(sys); pr.setEditable(true); pr.setMinWidth(260);
            pr.setValue(com.ars.fx.data.HubConfig.get("label.printer", ""));
            pr.valueProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set("label.printer", b == null ? "" : b));
            pr.getEditor().textProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set("label.printer", b == null ? "" : b));
            dyn.getChildren().add(cfgRow(zpl ? "System printer queue" : "Printer",
                    sys.isEmpty() ? "no printers found — add the printer in your OS settings first" : "installed printers (CUPS / Windows)", pr));
            dyn.getChildren().add(cfgRow("Copies", null, cSelect("label.copies", "1", "1", "2", "3", "4", "5")));
        };

        model.valueProperty().addListener((o, a, b) -> {
            if (b == null) return;
            com.ars.fx.data.HubConfig.set("label.model", b.id());
            com.ars.fx.data.HubConfig.remove("label.size"); com.ars.fx.data.HubConfig.remove("label.mode");
            com.ars.fx.data.HubConfig.remove("label.dpi");
            rebuild[0].run();
        });
        rebuild[0].run();

        CheckBox auto = new CheckBox(); auto.setSelected(com.ars.fx.data.QslLabel.printOnSave());
        auto.selectedProperty().addListener((o, a, b) -> com.ars.fx.data.QslLabel.setPrintOnSave(b));
        Label test = cfgBtn("Test print");
        test.setOnMouseClicked(e -> {
            status.setText("⋯ printing test label…");
            com.ars.fx.data.QslLabel.printAsync(com.ars.fx.data.QslLabel.sample(), r ->
                    javafx.application.Platform.runLater(() -> status.setText((r.ok() ? "✓ " : "✗ ") + r.message())));
        });

        card.getChildren().addAll(
                cfgRow("Print a label when a QSO is saved", "same switch as \"Print QSL label\" on the J-Log form", auto),
                cfgRow("Printer model", "Brother QL, DYMO LabelWriter, Zebra, Rollo — or any printer with a system driver", model),
                dyn,
                cfgRow("Test", "prints a sample label with the settings above", test));
        return new VBox(6, lbl("QSL LABEL PRINTER", "cfg-sec"), card, status);
    }
    /** Call history: what each station usually sends (state, section, zone, name), learned from the logs or imported. */
    static VBox callHistorySection() {
        Label count = lbl("", "cfg-lt"), status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        Runnable refresh = () -> {
            Thread t = new Thread(() -> { int n = com.ars.fx.data.CallHistory.count(); javafx.application.Platform.runLater(() -> count.setText(n + (n == 1 ? " station" : " stations"))); }, "call-history-count");
            t.setDaemon(true); t.start();
        };
        refresh.run();
        boolean[] busy = {false};
        java.util.function.BiConsumer<String, java.util.function.Supplier<com.ars.fx.data.CallHistory.Result>> run = (what, job) -> {
            if (busy[0]) return;
            busy[0] = true; status.setText(what + " …");
            Thread t = new Thread(() -> {
                com.ars.fx.data.CallHistory.Result r = job.get();
                javafx.application.Platform.runLater(() -> { busy[0] = false; refresh.run();
                    status.setText(r.ok() ? "✓ " + r.calls() + " station" + (r.calls() == 1 ? "" : "s") + " added or updated" : "✗ " + r.error()); });
            }, "call-history-job");
            t.setDaemon(true); t.start();
        };
        Label build = cfgBtn("Build from my logs");
        build.setOnMouseClicked(e -> run.accept("Reading your contest and normal logs", com.ars.fx.data.CallHistory::rebuildFromLogs));
        Label imp = cfgBtn("Import file…");
        imp.setOnMouseClicked(e -> {
            FileChooser fc = new FileChooser(); fc.setTitle("Import a call history file");
            fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Call history (CSV / text)", "*.csv", "*.txt", "*.dat"), new FileChooser.ExtensionFilter("All files", "*.*"));
            java.io.File f = fc.showOpenDialog(imp.getScene() != null ? imp.getScene().getWindow() : null);
            if (f != null) run.accept("Importing " + f.getName(), () -> com.ars.fx.data.CallHistory.importFile(f.toPath()));
        });
        Label clear = cfgBtn("Clear");
        clear.setOnMouseClicked(e -> { if (busy[0]) return; com.ars.fx.data.CallHistory.clear(); refresh.run(); status.setText("Call history cleared"); });
        HBox btns = new HBox(10, build, imp, clear); btns.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(0);
        card.getStyleClass().add("cfg-card");
        card.getChildren().addAll(
            cfgRow("Remembered", "each station's usual state, section, zone, grid and name — fills the exchange when you type a call, and tags cluster spots NEW MULT? in contests where the multiplier is in the exchange", count),
            cfgRow("Fill it", "your own QSOs always win; an imported file is CSV with a Call column (N1MM+ call history files work)", btns),
            new HBox(status) {{ getStyleClass().add("cfg-row"); setStyle("-fx-border-width:0;"); }});
        return new VBox(6, lbl("CALL HISTORY", "cfg-sec"), card);
    }
    /** Real ADIF / CSV backup export of the J-Log database. */
    static VBox backupSection() {
        TextField dir = new TextField(com.ars.fx.data.LogExport.defaultBackupDir().toString());
        dir.getStyleClass().add("jhub-mini-field"); dir.setMinWidth(280); dir.setMaxWidth(Double.MAX_VALUE);
        Label status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        java.util.function.Consumer<com.ars.fx.data.LogExport.Result> report = r ->
                status.setText(r.ok() ? "✓ Exported " + r.count() + " QSO" + (r.count() == 1 ? "" : "s") + " → " + r.path()
                                      : "✗ Export failed: " + r.error());
        Label adi = cfgBtn("Export ADIF"); adi.setOnMouseClicked(e -> report.accept(com.ars.fx.data.LogExport.backup(dir.getText(), "adi")));
        Label csv = cfgBtn("Export CSV");  csv.setOnMouseClicked(e -> report.accept(com.ars.fx.data.LogExport.backup(dir.getText(), "csv")));
        HBox btns = new HBox(10, adi, csv); btns.setAlignment(Pos.CENTER_LEFT);

        Label imp = cfgBtn("Import ADIF…");
        boolean[] busy = {false};
        imp.setOnMouseClicked(e -> {
            if (busy[0]) return;
            FileChooser fc = new FileChooser(); fc.setTitle("Import ADIF into the normal log");
            fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("ADIF files", "*.adi", "*.adif", "*.ADI", "*.ADIF"), new FileChooser.ExtensionFilter("All files", "*.*"));
            java.io.File f = fc.showOpenDialog(imp.getScene().getWindow());
            if (f == null) return;
            busy[0] = true; status.setText("Importing " + f.getName() + " …");
            Thread t = new Thread(() -> {
                com.ars.fx.data.LogImport.Result r = com.ars.fx.data.LogImport.importAdif(f.toPath());
                javafx.application.Platform.runLater(() -> { busy[0] = false;
                    status.setText(!r.ok() ? "✗ Import failed: " + r.error()
                            : "✓ Imported " + r.imported() + " QSO" + (r.imported() == 1 ? "" : "s") + " from " + f.getName()
                              + (r.duplicates() > 0 ? " · " + r.duplicates() + " already in the log, skipped" : "")
                              + (r.failed() > 0 ? " · " + r.failed() + " records unreadable" : "")); });
            }, "adif-import");
            t.setDaemon(true); t.start();
        });
        com.ars.fx.shell.Tips.tip(imp, "Add the QSOs from an ADIF file to the normal log; ones already logged are skipped");

        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        card.getChildren().add(cfgRow("Backup folder", "timestamped j-log-YYYYMMDD-HHMMSS files", dir));
        HBox exportRow = new HBox(20, new VBox(2, lbl("Export now", "cfg-lt"), lbl("ADIF for LoTW / eQSL / QRZ / Club Log · CSV for spreadsheets", "cfg-lh")));
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS); exportRow.getChildren().addAll(esp, btns);
        exportRow.getStyleClass().add("cfg-row"); exportRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(exportRow);
        HBox importRow = new HBox(20, new VBox(2, lbl("Import", "cfg-lt"), lbl("ADIF from another logger or a backup → the normal log (duplicates are skipped)", "cfg-lh")));
        Region isp = new Region(); HBox.setHgrow(isp, Priority.ALWAYS); importRow.getChildren().addAll(isp, imp);
        importRow.getStyleClass().add("cfg-row"); importRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(importRow);
        HBox statusRow = new HBox(status); statusRow.getStyleClass().add("cfg-row"); statusRow.setStyle("-fx-border-width:0;");
        card.getChildren().add(statusRow);
        return new VBox(6, lbl("BACKUP, EXPORT & IMPORT", "cfg-sec"), card);
    }
    static Label cfgBtn(String text) {
        Label b = lbl(text, "cfg-select"); b.setStyle("-fx-cursor:hand; -fx-padding:6 14 6 14; -fx-background-color:-ars-accent; -fx-text-fill:#06121a; -fx-font-weight:bold; -fx-background-radius:7;");
        return b;
    }
    /** F1–F8 station macros used by J-Log: CW/RTTY/digital send the text, phone plays the audio file. */
    /** What the F-key macros need to key the radio: CW speed, and the sound output voice macros play to. */
    static VBox txSection() {
        javafx.scene.control.ComboBox<String> dev = new javafx.scene.control.ComboBox<>();
        dev.getItems().add("System default"); dev.getItems().addAll(com.ars.fx.data.TxController.outputDevices());
        String cur = com.ars.fx.data.HubConfig.get("tx.voiceDevice", "").trim();
        dev.setValue(cur.isEmpty() || !dev.getItems().contains(cur) ? "System default" : cur);
        dev.setMinWidth(260); dev.setMaxWidth(320);
        dev.valueProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set("tx.voiceDevice", b == null || b.equals("System default") ? "" : b));
        return section("TRANSMIT (F-KEY MACROS)",
            cfgRow("CW speed", "F-key macros go to the rig's own keyer (Hamlib send_morse) — the rig must support it. J-Digi uses this speed for CW as well", cUnit("tx.cwWpm", "25", "wpm")),
            cfgRow("J-Digi CW keyer", "Audio: J-Digi makes the tone and keys the rig with the PTT method (Rig control). Rig keyer: the text goes to the rig's own keyer over Hamlib — no audio path needed",
                    cSeg("tx.cwKeyer", new String[]{"Audio","Rig keyer"}, 0)),
            cfgRow("PTT delay", "seconds between keying the rig and the audio starting, and again between the audio ending and dropping the key (0 – 2, e.g. 0.3)",
                    cUnit("tx.pttDelay", "0.2", "s")),
            cfgRow("Voice audio output", "where voice macros play — choose the sound card that feeds the radio. PTT is keyed through the rig (Rig control)", dev));
    }

    static VBox macroSection() {
        TextField[] lastMacroText = {null};
        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        // column header
        Label hk = lbl("KEY", "cfg-lh"); hk.setMinWidth(34);
        Label hl = lbl("LABEL", "cfg-lh"); hl.setMinWidth(110);
        Label ht = lbl("CW / RTTY TEXT  ·  fields are filled in when sent", "cfg-lh"); HBox.setHgrow(ht, Priority.ALWAYS); ht.setMaxWidth(Double.MAX_VALUE);
        Label ha = lbl("SSB VOICE FILE", "cfg-lh"); ha.setMinWidth(210);
        HBox header = new HBox(10, hk, hl, ht, ha, spacer(82)); header.getStyleClass().add("cfg-row"); header.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(header);

        for (int i = 0; i < com.ars.fx.data.MacroConfig.count(); i++) {
            final int idx = i;
            com.ars.fx.data.MacroConfig.Macro m = com.ars.fx.data.MacroConfig.get(i);
            Label fk = lbl("F" + (i + 1), "cfg-lt"); fk.setMinWidth(34);
            TextField labelF = macroField(m.label(), 110);
            TextField textF  = com.ars.fx.shell.Shell.caps(macroField(m.text(), -1));   // macro text is sent in upper case
            textF.focusedProperty().addListener((o, was, now) -> { if (now) lastMacroText[0] = textF; });   // where a clicked tag is inserted
            TextField audioF = macroField(m.audio(), 210); audioF.setPromptText("none — text only");
            Label browse = lbl("Browse…", "cfg-select"); browse.setMinWidth(82); browse.setStyle("-fx-cursor:hand;");

            Runnable persist = () -> com.ars.fx.data.MacroConfig.set(idx, labelF.getText(), textF.getText(), audioF.getText());
            labelF.textProperty().addListener((o, a, b) -> persist.run());
            textF.textProperty().addListener((o, a, b) -> persist.run());
            audioF.textProperty().addListener((o, a, b) -> persist.run());
            browse.setOnMouseClicked(e -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Voice-keyer audio for F" + (idx + 1));
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio (WAV / AIFF / AU)", "*.wav", "*.aif", "*.aiff", "*.au"));
                java.io.File cur = com.ars.fx.data.MacroConfig.resolve(audioF.getText());
                if (cur != null && cur.getParentFile() != null && cur.getParentFile().isDirectory()) fc.setInitialDirectory(cur.getParentFile());
                java.io.File f = fc.showOpenDialog(browse.getScene() != null ? browse.getScene().getWindow() : null);
                if (f != null) audioF.setText(f.getAbsolutePath());
            });
            HBox row = new HBox(10, fk, labelF, textF, audioF, browse); row.getStyleClass().add("cfg-row"); row.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(row);
        }
        if (!card.getChildren().isEmpty()) card.getChildren().get(card.getChildren().size() - 1).setStyle("-fx-border-width:0;");
        // the tags: every setting on the Station / Rig / Rotor / Amplifier / Antenna pages, with what it is worth right now
        Label hintLbl = lbl("Click a tag to put it into the macro text box you are editing. {CALL} is the station you are working.", "cfg-lh");
        hintLbl.setWrapText(true); hintLbl.setMaxWidth(Double.MAX_VALUE);
        VBox palette = new VBox(10, hintLbl);
        String curGroup = null; javafx.scene.layout.FlowPane flow = null;
        for (com.ars.fx.data.MacroConfig.Tag g : com.ars.fx.data.MacroConfig.TAGS) {
            if (!g.group().equals(curGroup)) {
                curGroup = g.group();
                flow = new javafx.scene.layout.FlowPane(6, 6);
                palette.getChildren().addAll(lbl(curGroup, "cfg-lh"), flow);
            }
            String v = com.ars.fx.data.MacroConfig.value(g);
            Label chip = lbl(g.tag() + (v.isEmpty() ? "  ⚠ not set" : "  " + v.toUpperCase()), "jl-clearbtn");
            chip.setStyle("-fx-cursor:hand;-fx-padding:3 8;-fx-background-color:-ars-surface-2;-fx-background-radius:6;"
                    + (v.isEmpty() ? "-fx-text-fill:-ars-warn;" : ""));
            com.ars.fx.shell.Tips.tip(chip, g.tag() + " — " + g.desc() + "\n" + (v.isEmpty() ? "Not set yet: a macro using it will not be sent until you fill it in." : "Currently: " + v.toUpperCase()));
            chip.setOnMouseClicked(e -> {
                TextField tf = lastMacroText[0];
                if (tf == null) { hintLbl.setText("Click into a macro's text box first, then click the tag."); return; }
                tf.insertText(tf.getCaretPosition(), g.tag()); tf.requestFocus();
            });
            flow.getChildren().add(chip);
        }
        VBox tagsCard = new VBox(palette); tagsCard.getStyleClass().add("cfg-card"); tagsCard.setPadding(new Insets(12, 16, 12, 16));
        Label rebuild = cfgBtn("Rebuild from station data");
        rebuild.setOnMouseClicked(e -> Confirm.show(rebuild, "Rebuild the macros?",
                "Puts the standard macro texts back on F1–F8, written with {MYCALL}, {CALL} and the other fields so they use your Station data. "
                        + "Your voice-file choices are kept; any text you edited is replaced.",
                new Confirm.Choice("Cancel", false, null),
                new Confirm.Choice("Rebuild", true, () -> { com.ars.fx.data.MacroConfig.resetToDefaults(); Shell.navigate("setup:tx"); })));
        HBox tools = new HBox(14, new Region(), rebuild); tools.setAlignment(Pos.CENTER_RIGHT); HBox.setHgrow(tools.getChildren().get(0), Priority.ALWAYS);
        Label macroDesc = lbl("Shared with J-Log. CW/RTTY/digital modes send the text message; SSB and other phone modes play the voice file. "
                + "A field is filled from J-Hub ▸ Station (and Rig control) when the macro is sent — a macro that needs one you have not filled in is not sent.", "cfg-lh");
        macroDesc.setWrapText(true); macroDesc.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, lbl("STATION MACROS · F1–F8", "cfg-sec"), macroDesc, tools, card,
                lbl("MACRO TAGS", "cfg-sec"), tagsCard);
    }
    static TextField macroField(String val, double w) {
        TextField t = new TextField(val == null ? "" : val);
        t.getStyleClass().add("jhub-mini-field");
        if (w > 0) { t.setMinWidth(w); t.setMaxWidth(w); } else { HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE); }
        return t;
    }
    static Region spacer(double w) { Region r = new Region(); r.setMinWidth(w); r.setMaxWidth(w); return r; }
    static Node[] presetRows() {
        java.util.List<com.ars.fx.data.RotorClient.Preset> ps = com.ars.fx.data.RotorClient.PRESETS;
        Node[] rows = new Node[ps.size()];
        for (int i = 0; i < rows.length; i++)
            rows[i] = cfgRow(ps.get(i).name(), "shown as " + ps.get(i).shortName() + " on the rotor panel", cUnit("rotor.preset." + ps.get(i).key(), String.valueOf(ps.get(i).defaultAz()), "° az"));
        return rows;
    }

    static VBox statCell(String k, String val, String unit, boolean ok) {
        Label v = lbl(val, "cfg-sv"); if (ok) v.getStyleClass().add("ok");
        HBox vu = new HBox(4, v); vu.setAlignment(Pos.BOTTOM_LEFT); if (!unit.isEmpty()) vu.getChildren().add(lbl(unit, "cfg-sval-u"));
        VBox cell = new VBox(4, lbl(k, "cfg-sk"), vu); cell.setMinWidth(150); return cell;
    }
    static VBox section(String name, Node... rows) {
        VBox card = new VBox(rows); card.getStyleClass().add("cfg-card");
        if (rows.length > 0) rows[rows.length-1].setStyle("-fx-border-width:0;");
        return new VBox(6, lbl(name, "cfg-sec"), card);
    }
    /** The first address to give the phone, or a hint when the client is off or no network address was found. */
    static String phoneUrl() {
        if (!com.ars.fx.web.WebClientServer.enabled()) return "";
        java.util.List<String> u = com.ars.fx.web.WebClientServer.urls();
        return u.isEmpty() ? "no network address found" : String.join("\n", u);
    }

    static HBox cfgRow(String title, String hint, Node control) {
        Object tag = control.getProperties().get("macroTag");
        if (tag != null) hint = (hint == null ? "" : hint + "  ·  ") + "macro tag " + tag;      // every setting a macro can use says so
        VBox lblBox = new VBox(2, lbl(title, "cfg-lt")); if (hint != null) lblBox.getChildren().add(lbl(hint, "cfg-lh"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(20, lblBox, sp, control); row.getStyleClass().add("cfg-row"); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    // ---- config-bound interactive controls (persist to HubConfig) ----------
    static final String[] BAUDS = {"1200","2400","4800","9600","19200","38400","57600","115200"};
    /** Free-text field bound to a HubConfig key. */
    /** The contest network's shared key: kept in the system keyring, so not bound to a config key like the other fields. */
    /**
     * The note Setup shows under Online upload when an eQSL password contains '#', or null. eQSL's download interface
     * (Get confirmations) fails with one although the suite sends it correctly encoded; the website and the upload are fine.
     */
    static String eqslHashNote(String password) {
        return password != null && password.contains("#")
                ? "eQSL: a # in your password stops Get confirmations (eQSL's download refuses it; uploading and the eQSL website "
                  + "are fine). To download confirmations, change your eQSL password on eqsl.cc to one without # and enter it here."
                : null;
    }

    static PasswordField networkKeyField() {
        PasswordField t = new PasswordField(); t.setText(com.ars.fx.net.ContestNetwork.sharedKey()); t.getStyleClass().add("jhub-mini-field");
        t.setMinWidth(220); t.setMaxWidth(260); t.setAlignment(Pos.CENTER_RIGHT);
        saveWhenSettled(t, () -> { String v = t.getText(); return () -> {
            boolean kept = com.ars.fx.net.ContestNetwork.storeKey(v);                // the keyring, off the FX thread
            javafx.application.Platform.runLater(() -> com.ars.fx.net.ContestNetwork.keyStored(v, kept));   // the config change (restarts the network) on the FX thread
        }; });
        return t;
    }

    /** One thread for settings saves, so they happen in the order typed and never on the FX thread. */
    static final java.util.concurrent.ExecutorService SAVE = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "settings-save"); t.setDaemon(true); return t;
    });
    /** Fields typed in whose save has not started yet. FX thread only. */
    private static final java.util.Map<javafx.animation.PauseTransition, java.util.function.Supplier<Runnable>> PENDING = new java.util.LinkedHashMap<>();

    /**
     * Save a field once typing settles: 0.7 s after the last key, on Enter, or when the field is left, on the {@link #SAVE}
     * thread. {@code snapshot} runs on the FX thread and returns the save to run with the values it read. Passwords and keys
     * go to the system keyring, which runs a command for every write (a PowerShell process on Windows): saving on every
     * keystroke, as these fields used to, froze typing for about a second per character.
     */
    static void saveWhenSettled(TextInputControl t, java.util.function.Supplier<Runnable> snapshot) {
        javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(700));
        Runnable fire = () -> { if (PENDING.remove(p) != null) { p.stop(); SAVE.execute(snapshot.get()); } };
        p.setOnFinished(e -> fire.run());
        t.textProperty().addListener((o, a, b) -> { PENDING.put(p, snapshot); p.playFromStart(); });
        t.focusedProperty().addListener((o, was, now) -> { if (!now) fire.run(); });
        if (t instanceof TextField tf) tf.addEventHandler(javafx.event.ActionEvent.ACTION, e -> fire.run());
    }

    /** FX thread: start every pending save now (before an action that reads the settings). */
    static void flushSaves() {
        for (var e : new java.util.ArrayList<>(PENDING.entrySet())) { e.getKey().stop(); SAVE.execute(e.getValue().get()); }
        PENDING.clear();
    }

    /** Background thread: wait until every save started so far has finished (at most 20 s). */
    static void waitForSaves() {
        try { SAVE.submit(() -> { }).get(20, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignored) { }
    }
    static TextField cInput(String key, String def) {
        TextField t = new TextField(com.ars.fx.data.HubConfig.get(key, def)); t.getStyleClass().add("jhub-mini-field");
        t.setMinWidth(220); t.setMaxWidth(260); t.setAlignment(Pos.CENTER_RIGHT);
        t.textProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        return tagged(t, key);
    }
    /** Marks a settings control with the macro tag its value is available as: the row shows it, and the control's
     *  tooltip names it. A no-op for settings that have no tag. */
    static <T extends Node> T tagged(T n, String key) {
        com.ars.fx.data.MacroConfig.Tag g = com.ars.fx.data.MacroConfig.tagForKey(key);
        if (g == null) return n;
        n.getProperties().put("macroTag", g.tag());
        Object prev = n.getProperties().get("tip");
        com.ars.fx.shell.Tips.tip(n, (prev instanceof String ps && !ps.isBlank() ? ps + "\n" : "") + "Macro tag " + g.tag() + " — put it in a macro to send this value");
        return n;
    }
    /** Free-text field bound to a HubConfig key whose example is shown as grey prompt text — the field is genuinely
     *  empty until the operator types, so what is displayed is what is stored (and what macros will send). */
    static TextField cInputPrompt(String key, String example) {
        TextField t = new TextField(com.ars.fx.data.HubConfig.get(key, "")); t.getStyleClass().add("jhub-mini-field");
        t.setPromptText(example);
        t.setMinWidth(220); t.setMaxWidth(260); t.setAlignment(Pos.CENTER_RIGHT);
        t.textProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        return tagged(t, key);
    }
    /** Free-text field bound to a HubConfig key that also runs {@code apply} on commit (focus-lost / Enter). */
    static TextField cInputApply(String key, String def, Runnable apply) {
        TextField t = cInput(key, def);
        t.focusedProperty().addListener((o, was, now) -> { if (!now) apply.run(); });
        t.setOnAction(e -> apply.run());
        return tagged(t, key);
    }
    /** Numeric/short field + unit, bound to a HubConfig key. */
    static Region cUnit(String key, String def, String unit) {
        TextField t = new TextField(com.ars.fx.data.HubConfig.get(key, def)); t.getStyleClass().add("jhub-mini-field");
        t.setMinWidth(120); t.setMaxWidth(120); t.setAlignment(Pos.CENTER_RIGHT);
        t.textProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        HBox h = new HBox(8, t, lbl(unit, "cfg-sval-u")); h.setAlignment(Pos.CENTER_LEFT); return tagged(h, key);
    }
    /** Dropdown bound to a HubConfig key (current value injected if not in the list). */
    static Region cSelect(String key, String def, String... opts) {
        ComboBox<String> c = new ComboBox<>(); c.getItems().addAll(opts);
        String cur = com.ars.fx.data.HubConfig.get(key, def);
        if (!c.getItems().contains(cur)) c.getItems().add(0, cur);
        c.setValue(cur); c.setMinWidth(220); c.setMaxWidth(260);
        c.valueProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        return tagged(c, key);
    }

    // ---- Hamlib model pickers: maker dropdown narrows the model dropdown; the model sets <kind>.model and <kind>.hamlibModel ----
    static final java.util.Map<String, ComboBox<String>> HL_MODEL_BOX = new java.util.HashMap<>();
    static final java.util.Map<String, TextField> HL_NUM_FIELD = new java.util.HashMap<>();
    static final java.util.Map<String, ComboBox<String>> HL_MAKER_BOX = new java.util.HashMap<>();

    static Region hamlibMaker(String kind) {
        ComboBox<String> c = new ComboBox<>(); c.setMinWidth(220); c.setMaxWidth(260);
        java.util.List<com.ars.fx.data.HamlibModels.Model> all = com.ars.fx.data.HamlibModels.list(kind);
        c.getItems().addAll(all.stream().map(com.ars.fx.data.HamlibModels.Model::mfg).distinct().toList());
        if (all.isEmpty()) { c.setPromptText("Hamlib not found"); }
        com.ars.fx.data.HamlibModels.Model cur = com.ars.fx.data.HamlibModels.find(kind, com.ars.fx.data.HubConfig.get(kind + ".hamlibModel", ""));
        if (cur != null) c.setValue(cur.mfg());
        c.valueProperty().addListener((o, a, b) -> {
            ComboBox<String> mb = HL_MODEL_BOX.get(kind);
            if (mb == null || b == null) return;
            mb.getItems().setAll(all.stream().filter(m -> m.mfg().equals(b)).map(com.ars.fx.data.HamlibModels.Model::label).toList());
            mb.setValue(null);
        });
        HL_MAKER_BOX.put(kind, c);
        return c;
    }
    static Region hamlibModel(String kind) {
        ComboBox<String> c = new ComboBox<>(); c.setMinWidth(220); c.setMaxWidth(260); c.setVisibleRowCount(14);
        java.util.List<com.ars.fx.data.HamlibModels.Model> all = com.ars.fx.data.HamlibModels.list(kind);
        com.ars.fx.data.HamlibModels.Model cur = com.ars.fx.data.HamlibModels.find(kind, com.ars.fx.data.HubConfig.get(kind + ".hamlibModel", ""));
        if (cur != null) {
            c.getItems().addAll(all.stream().filter(m -> m.mfg().equals(cur.mfg())).map(com.ars.fx.data.HamlibModels.Model::label).toList());
            c.setValue(cur.label());
        } else if (all.isEmpty()) c.setPromptText("Hamlib not found");
        else c.setPromptText("pick a maker first");
        c.valueProperty().addListener((o, a, b) -> {
            if (b == null) return;
            ComboBox<String> mk = HL_MAKER_BOX.get(kind);
            for (com.ars.fx.data.HamlibModels.Model m : all) {
                if (!m.label().equals(b) || (mk != null && mk.getValue() != null && !mk.getValue().equals(m.mfg()))) continue;
                com.ars.fx.data.HubConfig.set(kind + ".model", m.mfg() + " " + m.name());
                com.ars.fx.data.HubConfig.set(kind + ".hamlibModel", String.valueOf(m.number()));
                TextField nf = HL_NUM_FIELD.get(kind);
                if (nf != null) nf.setText(String.valueOf(m.number()));
                break;
            }
        });
        HL_MODEL_BOX.put(kind, c);
        return tagged(c, kind + ".model");
    }
    static TextField hamlibNumber(String kind) {
        TextField t = cInput(kind + ".hamlibModel", "");
        HL_NUM_FIELD.put(kind, t);
        return t;
    }
    /** Pick list of DX clusters. Choosing one saves it and moves the single cluster connection there. */
    static ComboBox<String> clusterPicker(java.util.function.Supplier<String> login) {
        ComboBox<String> c = new ComboBox<>(); c.setEditable(true);
        for (com.ars.fx.data.ClusterServers.Server s : com.ars.fx.data.ClusterServers.ALL) c.getItems().add(s.label());
        String cur = com.ars.fx.data.ClusterServers.label(com.ars.fx.data.HubConfig.get("data.clusterServer", com.ars.fx.data.ClusterServers.DEFAULT_ADDRESS).trim());
        if (!c.getItems().contains(cur)) c.getItems().add(0, cur);
        c.setValue(cur); c.setMinWidth(220);
        com.ars.fx.shell.Tips.tip(c, "Choose a DX cluster, or type host:port and press Enter");
        c.valueProperty().addListener((o, a, b) -> {
            String address = com.ars.fx.data.ClusterServers.addressOf(b);
            if (address.isEmpty() || address.equals(com.ars.fx.data.ClusterServers.addressOf(a))) return;
            com.ars.fx.data.HubConfig.set("data.clusterServer", address);
            ClusterClient.getInstance().setEndpoint(com.ars.fx.data.ClusterServers.host(address), com.ars.fx.data.ClusterServers.port(address), login.get());
        });
        return c;
    }
    /** Segmented control bound to a HubConfig key (stores the option string). */
    static Region cSeg(String key, String[] opts, int def) {
        String cur = com.ars.fx.data.HubConfig.get(key, opts[def]);
        int on = 0; for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) on = i;
        return tagged(segSel(opts, on, i -> com.ars.fx.data.HubConfig.set(key, opts[i])), key);
    }
    /** Segmented control bound to a HubConfig key that also runs {@code apply} after each change. */
    static Region cSegApply(String key, String[] opts, int def, Runnable apply) {
        String cur = com.ars.fx.data.HubConfig.get(key, opts[def]);
        int on = 0; for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) on = i;
        return tagged(segSel(opts, on, i -> { com.ars.fx.data.HubConfig.set(key, opts[i]); apply.run(); }), key);
    }
    /** Toggle bound to a HubConfig boolean key. */
    static Region cToggle(String key, boolean def) {
        return tagged(switchToggle(com.ars.fx.data.HubConfig.getBool(key, def), v -> com.ars.fx.data.HubConfig.setBool(key, v)), key);
    }
    /** Toggle bound to a HubConfig boolean key that also runs {@code apply} with the new state. */
    static Region cToggleApply(String key, boolean def, java.util.function.Consumer<Boolean> apply) {
        return tagged(switchToggle(com.ars.fx.data.HubConfig.getBool(key, def), v -> { com.ars.fx.data.HubConfig.setBool(key, v); apply.accept(v); }), key);
    }

    // ---- small helpers -----------------------------------------------------
    static Region chip(String cls, double sz){ Region r = new Region(); r.getStyleClass().add(cls); r.setMinSize(sz,sz); r.setPrefSize(sz,sz); r.setMaxSize(sz,sz); return r; }
    static Label btn(String t, String... c){ Label l = lbl(t, c); l.setAlignment(Pos.CENTER); return l; }
    static VBox labeled(String label, Region inp){ VBox v = new VBox(5, lbl(label,"jl-flabel"), inp); v.setAlignment(Pos.TOP_LEFT); return v; }
}
