package com.ars.fx.surface;

import javafx.scene.paint.Color;

/** J-Map's map colours for one colour scheme (the theme button's schemes, by key). */
record MapPalette(Color ocean, Color land, Color coast, Color border, Color state, Color ice, Color iceCoast,
                  Color line, Color lineText, Color text, Color halo, Color night, boolean light) {

    private static MapPalette of(String ocean, String land, String coast, String border, String ice, boolean light) {
        Color o = Color.web(ocean), l = Color.web(land);
        Color fg = light ? Color.web("#1c2733") : Color.web("#e8eef4");
        Color b = Color.web(border);
        return new MapPalette(o, l, Color.web(coast), b.deriveColor(0, 1, 1, 0.55), b.deriveColor(0, 1, 1, 0.5),
                Color.web(ice), light ? Color.web("#7f95a8", 0.8) : Color.web("#ffffff", 0.8),
                light ? Color.web("#1c2733", 0.28) : Color.web("#ffffff", 0.30),
                light ? Color.web("#1c2733", 0.75) : Color.web("#dbe6f0", 0.8),
                fg, light ? Color.web(land, 0.85) : Color.web(ocean, 0.85),
                light ? Color.web("#1b4a85", 0.34) : Color.web("#04060a", 0.5), light);
    }

    static MapPalette forScheme(String key) {
        return switch (key == null ? "dark" : key) {
            case "light"  -> of("#b7d6ea", "#ece6d2", "#8da07f", "#a58f5c", "#ffffff", true);
            case "ocean"  -> of("#0b4a6f", "#2f8c86", "#8fdcd2", "#e3dca8", "#e4f1f7", false);
            case "forest" -> of("#12362f", "#4b7d3c", "#a6d38f", "#d6c58a", "#e2eee4", false);
            case "amber"  -> of("#2b2112", "#80602a", "#d9b46a", "#f1dca0", "#f1e8d2", false);
            case "rose"   -> of("#35203a", "#8a4c68", "#e2a2bd", "#f0d2a8", "#f1e4ee", false);
            case "nord"   -> of("#2c3e55", "#5f7f6c", "#a3be8c", "#ebcb8b", "#eceff4", false);
            default       -> of("#12395c", "#3f7a4f", "#8fc79a", "#d6c58a", "#dbe8f0", false);
        };
    }
}
