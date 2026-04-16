package com.hamradio.modem.ui;

import com.hamradio.modem.ModemService;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Holds shared application state (icon, service reference) for J-Digi.
 */
public class ModemAppContext {

    private static Image appIcon;

    private final ModemService modemService;

    public ModemAppContext(ModemService modemService) {
        this.modemService = modemService;
    }

    public ModemService getModemService() {
        return modemService;
    }

    public static Image getAppIcon() {
        if (appIcon == null) {
            var stream = ModemAppContext.class.getResourceAsStream(
                "/com/hamradio/modem/icons/icon.png");
            if (stream != null) {
                appIcon = new Image(stream);
            }
        }
        return appIcon;
    }

    public static void applyIcon(Stage stage) {
        Image icon = getAppIcon();
        if (icon != null) {
            stage.getIcons().clear();
            stage.getIcons().add(icon);
        }
    }
}
