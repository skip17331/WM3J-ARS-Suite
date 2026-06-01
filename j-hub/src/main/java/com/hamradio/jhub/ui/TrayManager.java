package com.hamradio.jhub.ui;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Desktop;
import java.net.URI;

/**
 * System-tray (notification-area) integration for j-Hub.
 *
 * <p>j-Hub starts minimized to the tray: the broker services run, but the
 * status window is hidden until the operator restores it. The tray icon's
 * menu offers Show/Hide, Open web UI, and Quit. Closing the window hides it
 * back to the tray rather than exiting — only Quit shuts the broker down.</p>
 *
 * <p>Uses AWT {@link SystemTray} (java.desktop), which needs an XEmbed system
 * tray. Cinnamon/XFCE/KDE provide one; GNOME does not. When no tray is
 * available, {@link #install} returns false and the caller falls back to
 * showing the window normally so j-Hub is never left with no way to reach it.</p>
 */
public final class TrayManager {

    private static final Logger log = LoggerFactory.getLogger(TrayManager.class);

    private final Stage stage;
    private TrayIcon trayIcon;
    private MenuItem showHideItem;

    public TrayManager(Stage stage) {
        this.stage = stage;
    }

    /** Build + register the tray icon. Returns false if no system tray is
     *  available (caller should then just show the window). */
    public boolean install() {
        if (!SystemTray.isSupported()) {
            log.warn("System tray not supported on this platform — j-Hub will show its window instead.");
            return false;
        }
        // AWT must stay alive in the background for the tray icon to persist.
        java.awt.Toolkit.getDefaultToolkit();
        System.setProperty("java.awt.headless", "false");

        try {
            Image image = loadTrayImage();
            PopupMenu popup = new PopupMenu();

            showHideItem = new MenuItem("Show j-Hub");
            showHideItem.addActionListener(e -> Platform.runLater(this::toggleWindow));
            popup.add(showHideItem);

            MenuItem webItem = new MenuItem("Open Web UI…");
            webItem.addActionListener(e -> openWebUi());
            popup.add(webItem);

            popup.addSeparator();

            MenuItem quitItem = new MenuItem("Quit j-Hub");
            quitItem.addActionListener(e -> Platform.runLater(Platform::exit));
            popup.add(quitItem);

            trayIcon = new TrayIcon(image, "j-Hub — WM3J ARS Suite", popup);
            trayIcon.setImageAutoSize(true);
            // Double-click (or single activation) restores the window.
            trayIcon.addActionListener(e -> Platform.runLater(this::showWindow));

            SystemTray.getSystemTray().add(trayIcon);
            log.info("j-Hub tray icon installed.");
            return true;
        } catch (AWTException ex) {
            log.warn("Could not add j-Hub tray icon: {}", ex.getMessage());
            return false;
        }
    }

    /** Show the window (restore from tray) and bring it to front. */
    public void showWindow() {
        if (stage == null) return;
        if (!stage.isShowing()) stage.show();
        stage.setIconified(false);
        stage.toFront();
        stage.requestFocus();
        updateShowHideLabel();
    }

    /** Hide the window back to the tray (broker keeps running). */
    public void hideWindow() {
        if (stage != null) stage.hide();
        updateShowHideLabel();
    }

    private void toggleWindow() {
        if (stage != null && stage.isShowing()) hideWindow();
        else showWindow();
    }

    private void updateShowHideLabel() {
        if (showHideItem == null) return;
        boolean showing = stage != null && stage.isShowing();
        showHideItem.setLabel(showing ? "Hide j-Hub" : "Show j-Hub");
    }

    private void openWebUi() {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("http://localhost:8081"));
            } else {
                new ProcessBuilder("xdg-open", "http://localhost:8081").start();
            }
        } catch (Exception ex) {
            log.warn("Could not open j-Hub web UI: {}", ex.getMessage());
        }
    }

    private Image loadTrayImage() {
        try (var in = getClass().getResourceAsStream("/icons/icon.png")) {
            if (in != null) return javax.imageio.ImageIO.read(in);
        } catch (Exception ex) {
            log.warn("Could not load tray icon image: {}", ex.getMessage());
        }
        // Fallback: a tiny blank image so the tray entry still appears.
        return Toolkit.getDefaultToolkit().createImage(new byte[0]);
    }

    /** Remove the tray icon (called on shutdown). */
    public void remove() {
        if (trayIcon != null && SystemTray.isSupported()) {
            try { SystemTray.getSystemTray().remove(trayIcon); }
            catch (Exception ignored) {}
        }
    }
}
