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
}
