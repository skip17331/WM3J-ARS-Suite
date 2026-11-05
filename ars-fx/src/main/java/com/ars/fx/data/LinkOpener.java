package com.ars.fx.data;

import java.net.URI;
import java.util.Locale;

/**
 * Opens a web link in the user's browser without ever blocking the JavaFX thread.
 * <p>
 * {@code java.awt.Desktop.browse()} is avoided on Linux: AWT's Desktop support pulls in its own GTK next to JavaFX's,
 * which can hang the whole session, and it fails silently when no handler is registered. There the system's
 * {@code xdg-open} is started instead (fire and forget). Only http(s) links are opened.
 */
public final class LinkOpener {
    private LinkOpener() {}

    public static void open(String url) {
        if (url == null) return;
        String u = url.trim();
        String l = u.toLowerCase(Locale.ROOT);
        if (!(l.startsWith("http://") || l.startsWith("https://"))) return;
        Thread t = new Thread(() -> launch(u), "open-link");
        t.setDaemon(true);
        t.start();
    }

    /** Starts the mail program with a mailto: link, off the UI thread; {@code onFail} runs on the FX thread if that did not work. */
    public static void mail(String mailto, Runnable onFail) {
        Thread t = new Thread(() -> {
            boolean ok = false;
            try {
                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                if (os.contains("linux") || os.contains("bsd")) {
                    Process p = new ProcessBuilder("xdg-open", mailto).redirectErrorStream(true)
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
                    ok = p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS) ? p.exitValue() == 0 : true;
                } else if (os.contains("mac")) {
                    ok = new ProcessBuilder("open", mailto).start().waitFor() == 0;
                } else if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.MAIL)) {
                    java.awt.Desktop.getDesktop().mail(URI.create(mailto)); ok = true;
                }
            } catch (Throwable e) { ok = false; }
            if (!ok && onFail != null) javafx.application.Platform.runLater(onFail);
        }, "open-mail");
        t.setDaemon(true);
        t.start();
    }

    private static void launch(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("linux") || os.contains("bsd")) {
                new ProcessBuilder("xdg-open", url)
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(URI.create(url));
            } else {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            }
        } catch (Throwable e) {
            System.err.println("[link] could not open " + url + ": " + e);
        }
    }
}
