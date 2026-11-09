package com.ars.fx.shell;

import com.ars.fx.data.HubConfig;
import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Side panels that fold to a thin strip and slide open while the mouse is over them; Pin keeps one open in the layout.
 * An open panel floats over the content, so hovering never resizes or repaints the map beside it.
 *
 * <p>State lives here by key because surfaces rebuild (J-Sat every ~2 s, and every module on a switch).
 */
public final class AutoPanel {
    private AutoPanel() {}

    private static final double STRIP_WIDTH = 40;
    private static final Duration OPEN_DELAY = Duration.millis(160);    // so passing over a strip on the way to the dock doesn't open it
    private static final Duration CLOSE_DELAY = Duration.millis(450);
    private static final Duration REVEAL_TIME = Duration.seconds(8);

    private record Slot(Pane holder, Region panel) {}

    /** Every copy of a panel that has been built under one key. A surface that is no longer on screen can keep rebuilding itself
     *  (J-Sat does, every ~2 s), so the copy the user is hovering is not necessarily the newest one: state changes go to all of them. */
    private static final Map<String, java.util.List<Slot>> SLOTS = new HashMap<>();
    private static final int MAX_DETACHED = 6;
    private static final Map<String, Boolean> OPEN = new HashMap<>();
    private static final Map<String, Runnable> REBUILD = new HashMap<>();
    private static final Map<String, PauseTransition> OPENING = new HashMap<>();
    private static final Map<String, PauseTransition> CLOSING = new HashMap<>();

    public static boolean isPinned(String key) { return HubConfig.getBool(configKey(key), false); }
    private static String configKey(String key) { return "panel.pinned." + key; }

    /** One live pin state per panel, which every Pin / Unpin button for that panel follows. A header can be built once and outlive
     *  many pin changes (J-Map's right-hand column), or the panel can be pinned from elsewhere (the QSO card docks itself), so a
     *  caption chosen when the button was built goes stale. */
    private static final Map<String, javafx.beans.property.BooleanProperty> PIN_STATE = new HashMap<>();
    private static javafx.beans.property.BooleanProperty pinState(String key) {
        return PIN_STATE.computeIfAbsent(key, k -> new javafx.beans.property.SimpleBooleanProperty(isPinned(k)));
    }
    /** The one place a pin is stored: the saved setting and the live state the buttons follow change together. */
    private static void store(String key, boolean pinned) {
        HubConfig.setBool(configKey(key), pinned);
        pinState(key).set(pinned);
    }

    public static void setPinned(String key, boolean pinned) {
        if (pinned == isPinned(key)) { pinState(key).set(pinned); return; }
        store(key, pinned);
        Runnable rebuild = REBUILD.get(key);
        if (rebuild != null) rebuild.run();
    }

    /** The Pin / Unpin button for a panel's header. {@code rebuild} puts the panel back through {@link #wrap}. */
    public static Label pinButton(String key, Runnable rebuild) {
        REBUILD.put(key, rebuild);
        pinState(key).set(isPinned(key));                      // in step with the saved setting
        Label b = lbl("", "jhub-clearbtn");
        b.textProperty().bind(javafx.beans.binding.Bindings.when(pinState(key)).then("Unpin").otherwise("Pin"));
        Runnable caption = () -> Tips.tip(b, "Unpin".equals(b.getText()) ? "Let this panel collapse when the mouse leaves it" : "Keep this panel open");
        b.textProperty().addListener((o, was, now) -> caption.run());
        caption.run();
        b.setStyle("-fx-cursor:hand;");
        b.setOnMouseClicked(e -> {
            boolean pin = !isPinned(key);
            OPEN.put(key, !pin);                              // unpinning under the mouse: stay open until it leaves
            store(key, pin);
            rebuild.run();
            if (!pin) scheduleClose(key, CLOSE_DELAY);
        });
        return b;
    }

    /**
     * What to place in the layout for a panel: the panel itself when pinned, otherwise a strip it floats out of.
     * {@code tooNarrow} drops a pinned panel back to floating when the window can't spare the room.
     */
    public static Region wrap(String key, boolean rightSide, Region strip, Region panel, double width, BooleanSupplier tooNarrow) {
        panel.setEffect(null);
        panel.setVisible(true);
        panel.setManaged(true);
        watchTyping(key, panel);
        if (isPinned(key) && !tooNarrow.getAsBoolean()) {
            SLOTS.remove(key);
            return panel;
        }
        Pane holder = new Pane(strip, panel) {
            @Override protected void layoutChildren() {
                double h = getHeight();
                strip.resizeRelocate(0, 0, STRIP_WIDTH, h);
                panel.resizeRelocate(rightSide ? STRIP_WIDTH - width : 0, 0, width, h);
            }
        };
        panel.setManaged(false);
        holder.setMinSize(STRIP_WIDTH, 0);
        holder.setPrefWidth(STRIP_WIDTH);
        holder.setMaxWidth(STRIP_WIDTH);
        DropShadow shadow = new DropShadow(14, Color.rgb(0, 0, 0, 0.35));
        panel.setEffect(shadow);
        register(key, new Slot(holder, panel));
        show(key);
        holder.parentProperty().addListener((o, was, now) -> { if (now != null) show(key); });   // the wrapper isn't known until it is placed

        holder.setOnMouseEntered(e -> {
            stop(CLOSING, key);
            if (OPEN.getOrDefault(key, false)) return;
            PauseTransition t = timer(OPENING, key, OPEN_DELAY);
            t.setOnFinished(ev -> setOpen(key, true));
            t.playFromStart();
        });
        holder.setOnMouseExited(e -> {
            stop(OPENING, key);
            if (OPEN.getOrDefault(key, false)) scheduleClose(key, CLOSE_DELAY);
        });
        return holder;
    }

    /** Open an unpinned panel for a moment without the mouse, because something in it just changed. */
    public static void reveal(String key) {
        if (live(key) == null) return;
        setOpen(key, true);
        scheduleClose(key, REVEAL_TIME);
    }

    private static void scheduleClose(String key, Duration after) {
        PauseTransition t = timer(CLOSING, key, after);
        t.setOnFinished(e -> {
            Slot s = live(key);
            if (s != null && (s.holder().isHover() || typingIn(key, s))) {
                t.setDuration(CLOSE_DELAY);
                t.playFromStart();                            // still in use: look again shortly
            } else {
                setOpen(key, false);
            }
        });
        t.playFromStart();
    }

    /** When the user last pressed a key, or clicked a text field, in each panel. Keyboard focus alone does not mean someone is typing: JavaFX gives
     *  the first text field in a panel the focus the moment the panel is revealed (if nothing else has it), which on J-Map's left column held it
     *  open for good. */
    private static final Map<String, Long> LAST_TYPED = new HashMap<>();
    private static final long TYPING_GRACE_MS = 30_000;

    private static void watchTyping(String key, Region panel) {
        if (panel.getProperties().putIfAbsent("autopanel.typing", Boolean.TRUE) != null) return;   // the same panel is wrapped again on each pin change
        panel.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> LAST_TYPED.put(key, System.currentTimeMillis()));
        panel.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            for (Object t = e.getTarget(); t instanceof Node n; t = n.getParent()) if (n instanceof TextInputControl) { LAST_TYPED.put(key, System.currentTimeMillis()); return; }
        });
    }

    private static boolean typingIn(String key, Slot s) {
        if (System.currentTimeMillis() - LAST_TYPED.getOrDefault(key, 0L) > TYPING_GRACE_MS) return false;
        Node focus = s.holder().getScene().getFocusOwner();
        if (!(focus instanceof TextInputControl)) return false;
        for (Parent p = focus.getParent(); p != null; p = p.getParent()) if (p == s.panel()) return true;
        return false;
    }

    private static void setOpen(String key, boolean open) {
        OPEN.put(key, open);
        show(key);
    }

    private static void register(String key, Slot slot) {
        java.util.List<Slot> list = SLOTS.computeIfAbsent(key, k -> new java.util.ArrayList<>());
        // keep the copies that are on screen; of the rest (rebuilt but not attached, or since removed) keep only the newest few
        long detached = list.stream().filter(x -> x.holder().getScene() == null).count();
        for (java.util.Iterator<Slot> it = list.iterator(); it.hasNext() && detached >= MAX_DETACHED; ) {
            if (it.next().holder().getScene() == null) { it.remove(); detached--; }
        }
        list.add(slot);
    }

    /** The copy of this panel that is on screen, or null. */
    private static Slot live(String key) {
        java.util.List<Slot> list = SLOTS.get(key);
        if (list != null) for (Slot s : list) if (s.holder().getScene() != null) return s;
        return null;
    }

    private static void show(String key) {
        java.util.List<Slot> list = SLOTS.get(key);
        if (list == null) return;
        for (Slot s : new java.util.ArrayList<>(list)) show(key, s);
    }

    private static void show(String key, Slot s) {
        boolean open = OPEN.getOrDefault(key, false);
        s.panel().setVisible(open);
        s.holder().setViewOrder(open ? -1 : 0);               // over whatever sits beside it in the layout
        // The holder usually sits in a wrapper pane that is itself a sibling of the content, and view order only reorders
        // within one parent, so the wrapper has to come forward too or the open panel slides out underneath the content.
        Parent wrapper = s.holder().getParent();
        if (wrapper != null) wrapper.setViewOrder(open ? -1 : 0);
        s.holder().requestLayout();
    }

    private static PauseTransition timer(Map<String, PauseTransition> timers, String key, Duration d) {
        PauseTransition t = timers.computeIfAbsent(key, k -> new PauseTransition());
        t.stop();
        t.setDuration(d);
        return t;
    }

    private static void stop(Map<String, PauseTransition> timers, String key) {
        PauseTransition t = timers.get(key);
        if (t != null) t.stop();
    }
}
