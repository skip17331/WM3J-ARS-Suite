package com.ars.fx.data;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

class TmpShotTest {
    static void str(ByteBuffer b, String s) { byte[] x = s.getBytes(StandardCharsets.UTF_8); b.putInt(x.length); b.put(x); }

    static void shot(String id, Path out) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                StackPane host = new StackPane();
                Scene sc = new Scene(host, 1460, 900);
                com.ars.fx.Theme.apply(sc, false);
                host.getChildren().setAll(com.ars.fx.Launcher.buildSurface(id));
                com.ars.fx.Themes.applyTo(host, id);
                host.applyCss(); host.layout();
                new Thread(() -> { try { Thread.sleep(1500); } catch (Exception e) {}
                    Platform.runLater(() -> { try {
                        host.applyCss(); host.layout();
                        WritableImage img = sc.snapshot(null);
                        PixelReader pr = img.getPixelReader();
                        int w = (int) img.getWidth(), h = (int) img.getHeight();
                        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) bi.setRGB(x, y, pr.getArgb(x, y));
                        ImageIO.write(bi, "png", out.toFile());
                        done.complete(null);
                    } catch (Throwable t) { done.completeExceptionally(t); } });
                }).start();
            } catch (Throwable t) { done.completeExceptionally(t); }
        });
        done.get(30, TimeUnit.SECONDS);
    }

    @Test void shots() throws Exception {
        Path outDir = Path.of(System.getProperty("shot.dir"));
        Path home = Files.createTempDirectory("shot");
        System.setProperty("user.home", home.toString());
        System.setProperty("wsjtx.port", "22372");
        try { Platform.startup(() -> {}); } catch (IllegalStateException ok) {}
        HubConfig.reload();
        HubConfig.set("station.call", "WM3J"); HubConfig.set("station.grid", "FN20");

        // fake rigctld: 14.074 MHz USB
        ServerSocket ss = new ServerSocket(0);
        Thread srv = new Thread(() -> { try { while (true) { Socket c = ss.accept();
            new Thread(() -> { try (c; BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
                                    PrintWriter out = new PrintWriter(c.getOutputStream(), true)) {
                String l; while ((l = in.readLine()) != null) { l = l.trim();
                    if (l.equals("+f")) out.print("Frequency: 14074000\nRPRT 0\n");
                    else if (l.equals("+m")) out.print("Mode: USB\nPassband: 2400\nRPRT 0\n");
                    else if (l.equals("+t")) out.print("PTT: 0\nRPRT 0\n");
                    else out.print("RPRT -11\n"); out.flush(); } } catch (IOException ignored) {} }).start(); } } catch (IOException ignored) {} });
        srv.setDaemon(true); srv.start();
        System.setProperty("rig.host", "127.0.0.1"); System.setProperty("rig.port", String.valueOf(ss.getLocalPort()));
        DigiEngine.get(); RigClient.getInstance().reconnect(); RigClient.getInstance().start();

        BridgeService.start();
        try (DatagramSocket sock = new DatagramSocket()) {
            InetAddress lo = InetAddress.getByName("127.0.0.1");
            ByteBuffer st = ByteBuffer.allocate(256);
            st.putInt(0xadbccbda); st.putInt(2); st.putInt(1); str(st, "WSJT-X");
            st.putLong(14_074_000L); str(st, "FT8"); str(st, ""); str(st, ""); str(st, "FT8");
            st.put((byte) 0); st.put((byte) 0); st.put((byte) 1); st.putInt(1500); st.putInt(1500);
            str(st, ""); st.put((byte) 0); str(st, ""); st.put((byte) 0);
            sock.send(new DatagramPacket(st.array(), st.position(), lo, 22372));
            Thread.sleep(300);
            String[] msgs = {"CQ DL1ABC JO31", "CQ DX JA1XYZ PM95", "W3ABC K3XYZ RR73", "CQ VK3ABC QF22", "CQ EA8XX IL18", "CQ PY2ZZ GG66", "N0CALL ZS6AA -12"};
            for (int i = 0; i < msgs.length; i++) {
                ByteBuffer dc = ByteBuffer.allocate(256);
                dc.putInt(0xadbccbda); dc.putInt(2); dc.putInt(2); str(dc, "WSJT-X");
                dc.put((byte) 1); dc.putInt(43_200_000 + i * 15000); dc.putInt(-8 - i); dc.putDouble(0.2 + i / 10.0); dc.putInt(900 + i * 130);
                str(dc, "~"); str(dc, msgs[i]); dc.put((byte) 0); dc.put((byte) 0);
                sock.send(new DatagramPacket(dc.array(), dc.position(), lo, 22372));
            }
        }
        Thread.sleep(2500);
        for (String id : new String[]{"bridge", "digi", "hubcfg", "hubdata"}) shot(id, outDir.resolve(id + ".png"));
    }
}
