package com.ars.fx;

import javafx.scene.Scene;
import javafx.scene.text.Font;

/** Loads the bundled fonts and the ars.css stylesheet onto a Scene. */
public final class Theme {
    private Theme() {}
    private static boolean fontsLoaded = false;

    public static void loadFonts() {
        if (fontsLoaded) return;
        fontsLoaded = true;
        for (String f : new String[]{"IBMPlexSans-Regular.ttf","IBMPlexSans-Bold.ttf",
                                     "JetBrainsMono-Regular.ttf","JetBrainsMono-Bold.ttf"}) {
            try (var in = Theme.class.getResourceAsStream("/com/ars/fx/fonts/" + f)) {
                if (in != null) Font.loadFont(in, 12);
            } catch (Exception ignored) {}
        }
    }

    public static void apply(Scene scene, boolean light) {
        loadFonts();
        scene.getStylesheets().add(Theme.class.getResource("/com/ars/fx/css/ars.css").toExternalForm());
        if (light) scene.getRoot().getStyleClass().add("ars-light");
    }
}
