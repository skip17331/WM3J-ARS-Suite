package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HubConfigTest {
    @Test
    void changeListenersHearSetRemoveAndReload() throws Exception {
        String oldHome = System.getProperty("user.home");
        Path home = Files.createTempDirectory("hubconfig-test");
        System.setProperty("user.home", home.toString());          // keep the user's real config untouched
        try {
            HubConfig.reload();
            List<String> heard = new ArrayList<>();
            HubConfig.addChangeListener(heard::add);

            HubConfig.set("rig.ptt", "VOX");
            HubConfig.setBool("rig.startDaemon", true);
            HubConfig.remove("rig.ptt");
            HubConfig.reload();

            assertEquals(List.of("rig.ptt", "rig.startDaemon", "rig.ptt", "*"), heard);
        } finally {
            System.setProperty("user.home", oldHome);
            HubConfig.reload();
        }
    }

    /** The hub and each app window are separate processes with their own copy of the file; a save from an older copy must not undo what another one wrote. */
    @Test
    void aSaveKeepsWhatAnotherProcessWroteSinceThisOneLoaded() throws Exception {
        String oldHome = System.getProperty("user.home");
        Path home = Files.createTempDirectory("hubconfig-test");
        System.setProperty("user.home", home.toString());
        try {
            HubConfig.reload();
            HubConfig.set("theme.scheme", "dark");

            // another process changes the file behind this one's back
            Path f = home.resolve(".j-hub").resolve("config.json");
            com.google.gson.JsonObject disk = com.google.gson.JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            disk.addProperty("theme.scheme", "light");
            disk.addProperty("theme.module.map", "ocean");
            Files.writeString(f, disk.toString());

            // this one, still holding its older copy, saves something unrelated
            HubConfig.set("prop.target", "SX200H");
            HubConfig.reload();

            assertEquals("light", HubConfig.get("theme.scheme", ""));
            assertEquals("ocean", HubConfig.get("theme.module.map", ""));
            assertEquals("SX200H", HubConfig.get("prop.target", ""));
        } finally {
            System.setProperty("user.home", oldHome);
            HubConfig.reload();
        }
    }

    @Test
    void wholeNumberSettingsFallBackWhenMissingOrNotNumbers() throws Exception {
        String oldHome = System.getProperty("user.home");
        Path home = Files.createTempDirectory("hubconfig-test");
        System.setProperty("user.home", home.toString());
        try {
            HubConfig.reload();
            assertEquals(7, HubConfig.getInt("map.projection", 7));
            HubConfig.setInt("map.projection", 2);
            assertEquals(2, HubConfig.getInt("map.projection", 7));
            HubConfig.set("map.projection", "wide");
            assertEquals(7, HubConfig.getInt("map.projection", 7));
        } finally {
            System.setProperty("user.home", oldHome);
            HubConfig.reload();
        }
    }
}
