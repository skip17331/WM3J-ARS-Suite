package com.ars.fx.shell;

import com.ars.fx.data.AmpClient;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.RotorClient;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Status readouts for the hamlib-controlled devices other than the rig (the rig has its own cell in the hero bar): the rotor, the satellite
 * rotor and the amplifier. Each shows only when its "Show status in hero bar" switch is on, on its own Setup page, so a station without
 * a rotor or an amp sees nothing extra. One set serves both the module bars ({@link HeroBar}) and the J-Hub dashboard's banner.
 *
 * <p>{@link #KEY_RIG} is here too so every switch lives in one place; the rig cell stays on by default (it always was in the bar).
 */
public final class HeroDevices {
    private HeroDevices() {}

    public static final String KEY_RIG = "hero.show.rig", KEY_ROTOR = "hero.show.rotor", KEY_SAT = "hero.show.satrotor", KEY_AMP = "hero.show.amp";

    public static boolean showRig() { return HubConfig.getBool(KEY_RIG, true); }

    /** The cells to add to a hero bar, and one call that brings them up to date (the bar calls it from its own one-second tick). */
    public record Set(List<VBox> cells, Runnable update) {}

    /**
     * @param valueClass the style class of a cell's main reading: {@code hero-val} in the module bars (which scale it), {@code hero-rig} on the dashboard
     */
    public static Set build(String valueClass) {
        List<VBox> cells = new ArrayList<>();
        List<Runnable> updates = new ArrayList<>();
        if (HubConfig.getBool(KEY_ROTOR, false)) rotor("ROTOR", RotorClient.getInstance(), valueClass, cells, updates);
        if (HubConfig.getBool(KEY_SAT, false) && RotorClient.satellite().isEnabled()) rotor("SAT ROTOR", RotorClient.satellite(), valueClass, cells, updates);
        if (HubConfig.getBool(KEY_AMP, false)) amp(valueClass, cells, updates);
        return new Set(cells, () -> { for (Runnable r : updates) r.run(); });
    }

    private static VBox cell(String key, Label val, Label sub) {
        VBox c = new VBox(2, lbl(key, "hero-k"), val, sub);
        c.getStyleClass().add("hero-stat");
        return c;
    }

    private static void rotor(String key, RotorClient ro, String valueClass, List<VBox> cells, List<Runnable> updates) {
        ro.start();                                           // idles under a hub link: the state then arrives from the hub
        Label val = lbl("—", valueClass, "rotor"), sub = lbl("rotor offline", "hero-rig-sub");
        cells.add(cell(key, val, sub));
        updates.add(() -> {
            RotorClient.State s = ro.last();
            boolean on = s != null && s.connected();
            val.setText(on ? String.format("%03.0f°", ro.headingDeg()) : "—");
            sub.setText(on ? RotorClient.cardinal(ro.headingDeg()) + (ro.hasElevation() ? String.format("  ·  el %.0f°", s.el()) : "")
                           : ro.statusText());
        });
    }

    private static void amp(String valueClass, List<VBox> cells, List<Runnable> updates) {
        AmpClient am = AmpClient.getInstance();
        am.start();                                           // the amp is polled straight from its own ampctld, hub or not
        Label val = lbl("—", valueClass, "amp"), sub = lbl("amp offline", "hero-rig-sub");
        cells.add(cell("AMP", val, sub));
        updates.add(() -> {
            AmpClient.State s = am.last();
            boolean on = s != null && s.connected();
            val.setText(on ? s.mode().toUpperCase() : "—");
            sub.setText(on ? (s.swr() > 0 ? String.format("SWR %.1f:1", s.swr()) : "ampctld") : "amp offline");
        });
    }
}
