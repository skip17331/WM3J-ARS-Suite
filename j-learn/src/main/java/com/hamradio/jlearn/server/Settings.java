package com.hamradio.jlearn.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * J-Learn settings persisted at {@code ~/.j-learn/settings.json}. Tiny by
 * design — the only knob today is the listen port. The file is created on
 * first run with defaults so users have something to edit.
 */
public final class Settings {

    public int port = 8082;

    public static Settings load() {
        Path file = settingsFile();
        if (Files.exists(file)) {
            try {
                String body = Files.readString(file);
                JsonObject obj = new Gson().fromJson(body, JsonObject.class);
                Settings s = new Settings();
                if (obj != null && obj.has("port")) s.port = obj.get("port").getAsInt();
                return s;
            } catch (Exception ignore) {
                // Fall through to defaults; bad settings file shouldn't kill us.
            }
        }
        Settings defaults = new Settings();
        try {
            Files.createDirectories(file.getParent());
            JsonObject seed = new JsonObject();
            seed.addProperty("port", defaults.port);
            Files.writeString(file, new Gson().toJson(seed));
        } catch (IOException ignore) {
            // Best-effort — defaults still work in memory.
        }
        return defaults;
    }

    private static Path settingsFile() {
        return Path.of(System.getProperty("user.home"), ".j-learn", "settings.json");
    }
}
