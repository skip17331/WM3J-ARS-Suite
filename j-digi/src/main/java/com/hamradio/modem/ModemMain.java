package com.hamradio.modem;

import com.hamradio.modem.ui.ModemAppContext;
import com.hamradio.modem.ui.SplashScreen;
import javafx.application.Application;
import javafx.stage.Stage;

public class ModemMain extends Application {
    private ModemService modemService;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("J-Digi");
        try {
            com.jlog.db.DatabaseManager.getInstance().initAll();
        } catch (Exception ignored) {}
        modemService = new ModemService();
        ModemAppContext context = new ModemAppContext(modemService);
        ModemAppContext.applyIcon(primaryStage);
        SplashScreen splash = new SplashScreen(primaryStage, context);
        splash.show();
    }

    @Override
    public void stop() {
        if (modemService != null) {
            modemService.stopAudio();
            modemService.disconnectHub();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
