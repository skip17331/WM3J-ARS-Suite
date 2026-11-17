package com.ars.fx.shell;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * A developer hook for driving a dev run from a script: screenshots of the app's own window and input into it, on any
 * desktop (X11 or Wayland, GNOME, KDE, Cinnamon) and without catching other windows. Off unless the app is started with
 * {@code -Dars.devhook=<folder>}; the packaged app never sets it.
 *
 * <p>A script drops {@code <name>.cmd} (one line) into the folder; the app runs it and writes {@code <name>.done} with
 * "ok ..." or "error ...". Coordinates are the app window's scene pixels, i.e. pixels of a {@code snap} picture.
 * <pre>
 *   snap OUT.png          the window as a PNG          size              "ok W H"
 *   click X Y | dbl X Y   left click / double click    drag X1 Y1 X2 Y2  press, move, release
 *   wheel X Y N           N notches (N &gt; 0 up)       type TEXT         into the focused control
 *   key NAME [N]          a JavaFX KeyCode N times (ENTER, BACK_SPACE, TAB, ESCAPE, F1 ...)
 *   nav SURFACE           Shell.navigate (hub, map, sat, log, digi, bridge, vault, learn, setup:...)
 * </pre>
 */
public final class DevHook {
    private DevHook() {}

    /** Start watching the folder named by {@code -Dars.devhook}, if any. */
    public static void install(Stage stage) {
        String dir = System.getProperty("ars.devhook", "").trim();
        if (dir.isEmpty() || dir.startsWith("${")) return;          // unset, or the pom's unfilled placeholder
        Path folder = Path.of(dir);
        try { Files.createDirectories(folder); } catch (IOException e) { System.err.println("[devhook] " + e); return; }
        Thread t = new Thread(() -> loop(stage, folder), "devhook");
        t.setDaemon(true);
        t.start();
        System.out.println("[devhook] watching " + folder.toAbsolutePath());
    }

    private static void loop(Stage stage, Path folder) {
        while (true) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder, "*.cmd")) {
                for (Path cmd : ds) {
                    String line = Files.readString(cmd, StandardCharsets.UTF_8).trim();
                    Files.deleteIfExists(cmd);
                    String result = runOnFx(stage, line);
                    String name = cmd.getFileName().toString().replaceFirst("\\.cmd$", "");
                    Path tmp = folder.resolve(name + ".tmp");
                    Files.writeString(tmp, result + "\n", StandardCharsets.UTF_8);
                    Files.move(tmp, folder.resolve(name + ".done"), java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (Exception e) { System.err.println("[devhook] " + e); }
            try { Thread.sleep(150); } catch (InterruptedException ie) { return; }
        }
    }

    private static String runOnFx(Stage stage, String line) {
        CompletableFuture<String> f = new CompletableFuture<>();
        Platform.runLater(() -> {
            try { f.complete(run(stage, line)); } catch (Exception e) { f.complete("error " + e); }
        });
        try { return f.get(15, TimeUnit.SECONDS); } catch (Exception e) { return "error timed out: " + line; }
    }

    /** One command, on the FX thread. Input is delivered as JavaFX events to the node under the point (or the focused
     *  node for keys), not through the system pointer: Wayland lets no app move the real pointer, and this way a test run
     *  never takes over the operator's mouse or keyboard. */
    static String run(Stage stage, String line) throws Exception {
        String[] a = line.split("\\s+", 2);
        String op = a[0], rest = a.length > 1 ? a[1] : "";
        Scene scene = stage.getScene();
        if (scene == null) return "error no window";
        double[] n = nums(rest);
        switch (op) {
            case "size" -> { return "ok " + Math.round(scene.getWidth()) + " " + Math.round(scene.getHeight()); }
            case "snap" -> {
                WritableImage img = scene.snapshot(null);
                Path out = Path.of(rest.trim());
                Files.write(out, png(img));
                return "ok " + out.toAbsolutePath() + " " + (int) img.getWidth() + "x" + (int) img.getHeight();
            }
            case "nav" -> { Shell.navigate(rest.trim()); return "ok"; }
            case "click", "dbl" -> {
                Node t = pick(scene, n[0], n[1]);
                mouse(scene, t, MouseEvent.MOUSE_MOVED, n[0], n[1], 0, false);
                for (int c = 1; c <= (op.equals("dbl") ? 2 : 1); c++) {
                    mouse(scene, t, MouseEvent.MOUSE_PRESSED, n[0], n[1], c, true);
                    mouse(scene, t, MouseEvent.MOUSE_RELEASED, n[0], n[1], c, false);
                    mouse(scene, t, MouseEvent.MOUSE_CLICKED, n[0], n[1], c, false);
                }
                return "ok " + describe(t);
            }
            case "drag" -> {
                Node t = pick(scene, n[0], n[1]);
                mouse(scene, t, MouseEvent.MOUSE_PRESSED, n[0], n[1], 1, true);
                for (int i = 1; i <= 10; i++)
                    mouse(scene, t, MouseEvent.MOUSE_DRAGGED, n[0] + (n[2] - n[0]) * i / 10, n[1] + (n[3] - n[1]) * i / 10, 0, true);
                mouse(scene, t, MouseEvent.MOUSE_RELEASED, n[2], n[3], 1, false);
                return "ok " + describe(t);
            }
            case "wheel" -> {
                Node t = pick(scene, n[0], n[1]);
                Point2D l = t.sceneToLocal(n[0], n[1]);
                double dy = 40 * n[2];                                   // one notch = 40 px; positive = up
                Event.fireEvent(t, new ScrollEvent(ScrollEvent.SCROLL, l.getX(), l.getY(), sx(scene, n[0]), sy(scene, n[1]),
                        false, false, false, false, false, false, 0, dy, 0, dy,
                        ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null));
                return "ok " + describe(t);
            }
            case "type" -> {
                Node t = focused(scene);
                for (char c : rest.toCharArray()) {
                    String ch = String.valueOf(c);
                    Event.fireEvent(t, new KeyEvent(KeyEvent.KEY_PRESSED, ch, ch, KeyCode.UNDEFINED, false, false, false, false));
                    Event.fireEvent(t, new KeyEvent(KeyEvent.KEY_TYPED, ch, "", KeyCode.UNDEFINED, false, false, false, false));
                    Event.fireEvent(t, new KeyEvent(KeyEvent.KEY_RELEASED, ch, ch, KeyCode.UNDEFINED, false, false, false, false));
                }
                return "ok " + describe(t);
            }
            case "key" -> {
                String[] k = rest.trim().split("\\s+");
                KeyCode code = KeyCode.valueOf(k[0].toUpperCase(java.util.Locale.ROOT));
                int times = k.length > 1 ? Integer.parseInt(k[1]) : 1;
                Node t = focused(scene);
                for (int i = 0; i < times; i++) {
                    Event.fireEvent(t, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
                    Event.fireEvent(t, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
                }
                return "ok " + describe(t);
            }
            default -> { return "error unknown command: " + op; }
        }
    }

    /** The top-most visible, mouse-sensitive node at a scene point. */
    static Node pick(Scene scene, double x, double y) {
        Node hit = pickIn(scene.getRoot(), x, y);
        return hit != null ? hit : scene.getRoot();
    }
    private static Node pickIn(Node n, double x, double y) {
        if (!n.isVisible() || n.isMouseTransparent() || n.isDisabled()) return null;
        Point2D l = n.sceneToLocal(x, y);
        if (l == null) return null;
        if (n instanceof Parent p) {
            List<Node> kids = p.getChildrenUnmodifiable();
            for (int i = kids.size() - 1; i >= 0; i--) {           // last child is drawn on top
                Node h = pickIn(kids.get(i), x, y);
                if (h != null) return h;
            }
        }
        return n.contains(l) ? n : null;
    }

    private static Node focused(Scene scene) { return scene.getFocusOwner() != null ? scene.getFocusOwner() : scene.getRoot(); }

    private static void mouse(Scene scene, Node t, javafx.event.EventType<MouseEvent> type, double x, double y, int clicks, boolean down) {
        Point2D l = t.sceneToLocal(x, y);
        if (type == MouseEvent.MOUSE_PRESSED && t.isFocusTraversable()) t.requestFocus();
        MouseEvent e = new MouseEvent(type, l.getX(), l.getY(), sx(scene, x), sy(scene, y),
                type == MouseEvent.MOUSE_MOVED ? MouseButton.NONE : MouseButton.PRIMARY, clicks,
                false, false, false, false, down, false, false, true, false, type == MouseEvent.MOUSE_CLICKED,
                new PickResult(t, x, y));
        Event.fireEvent(t, e);
    }
    private static double sx(Scene s, double x) { return s.getWindow().getX() + s.getX() + x; }
    private static double sy(Scene s, double y) { return s.getWindow().getY() + s.getY() + y; }

    private static String describe(Node t) {
        String id = t.getId() != null ? "#" + t.getId() : "";
        String txt = t instanceof javafx.scene.control.Labeled lb && lb.getText() != null && !lb.getText().isBlank() ? " \"" + lb.getText() + "\"" : "";
        return t.getClass().getSimpleName() + id + (t.getStyleClass().isEmpty() ? "" : "." + String.join(".", t.getStyleClass())) + txt;
    }

    private static double[] nums(String s) {
        String[] p = s.trim().isEmpty() ? new String[0] : s.trim().split("\\s+");
        double[] d = new double[4];
        for (int i = 0; i < Math.min(4, p.length); i++) { try { d[i] = Double.parseDouble(p[i]); } catch (NumberFormatException ignored) {} }
        return d;
    }

    /** A plain RGBA PNG (no AWT / javafx-swing needed). */
    static byte[] png(WritableImage img) throws IOException {
        int w = (int) img.getWidth(), h = (int) img.getHeight();
        int[] argb = new int[w * h];
        img.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
        return png(argb, w, h);
    }

    static byte[] png(int[] argb, int w, int h) throws IOException {
        byte[] raw = new byte[h * (1 + w * 4)];
        int p = 0;
        for (int y = 0; y < h; y++) {
            raw[p++] = 0;                                           // filter: none
            for (int x = 0; x < w; x++) {
                int c = argb[y * w + x];
                raw[p++] = (byte) (c >> 16); raw[p++] = (byte) (c >> 8); raw[p++] = (byte) c; raw[p++] = (byte) (c >>> 24);
            }
        }
        Deflater def = new Deflater(6);
        def.setInput(raw); def.finish();
        ByteArrayOutputStream z = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        while (!def.finished()) z.write(buf, 0, def.deflate(buf));
        def.end();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(ihdr);
        d.writeInt(w); d.writeInt(h); d.write(new byte[]{8, 6, 0, 0, 0});   // 8-bit RGBA
        chunk(out, "IHDR", ihdr.toByteArray());
        chunk(out, "IDAT", z.toByteArray());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        DataOutputStream d = new DataOutputStream(out);
        byte[] t = type.getBytes(StandardCharsets.US_ASCII);
        d.writeInt(data.length); d.write(t); d.write(data);
        CRC32 crc = new CRC32(); crc.update(t); crc.update(data);
        d.writeInt((int) crc.getValue());
    }
}
