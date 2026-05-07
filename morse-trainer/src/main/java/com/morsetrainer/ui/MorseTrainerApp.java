package com.morsetrainer.ui;

import com.morsetrainer.audio.MorsePlayer;
import com.morsetrainer.audio.ToneGenerator;
import com.morsetrainer.core.AppConfig;
import com.morsetrainer.core.Logger;
import javafx.application.Application;
import javafx.stage.Stage;

/** Application bootstrap. Holds the shared audio resources for the session. */
public class MorseTrainerApp extends Application {

    private ToneGenerator tone;
    private MorsePlayer player;

    @Override
    public void start(Stage stage) {
        AppConfig cfg = AppConfig.get();
        Logger.setLevel(Logger.Level.INFO);
        tone = new ToneGenerator(cfg);
        player = new MorsePlayer(tone, cfg);
        HomeView.wireSharedAudio(tone, player);

        HomeView.show(stage);
        stage.setOnCloseRequest(ev -> shutdown());
        stage.show();
    }

    private void shutdown() {
        if (player != null) player.shutdown();
        if (tone != null) tone.close();
    }

    public static void main(String[] args) { launch(args); }
}
