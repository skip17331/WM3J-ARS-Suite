package com.ars.fx.data;

import com.hamradio.modem.ModemService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** J-Digi follows the J-Hub rig and transmit settings, now and as they change. */
class DigiEngineSyncTest {
    @Test
    void modemFollowsJHubSettings() throws Exception {
        String oldHome = System.getProperty("user.home");
        System.setProperty("user.home", Files.createTempDirectory("digi-sync").toString());   // never the user's real config
        try {
            HubConfig.reload();
            ModemService svc = DigiEngine.get();
            Assumptions.assumeTrue(svc != null, "the modem engine is not available here");

            // defaults: J-Hub's PTT method is CAT (→ Hamlib), CW goes through the audio keyer at 25 wpm
            assertEquals("HAMLIB", svc.getPttMethod());
            assertEquals("AUDIO", svc.getCwKeyer());
            assertEquals(25, svc.getCwWpm());

            HubConfig.set("rig.ptt", "VOX");
            assertEquals("VOX", svc.getPttMethod());
            HubConfig.set("rig.ptt", "RTS");              // RTS / DTR are keyed by the rigctld daemon → Hamlib
            assertEquals("HAMLIB", svc.getPttMethod());

            HubConfig.set("tx.cwKeyer", "Rig keyer");
            assertEquals("HAMLIB", svc.getCwKeyer());
            HubConfig.set("tx.cwKeyer", "Audio");
            assertEquals("AUDIO", svc.getCwKeyer());

            HubConfig.set("tx.cwWpm", "18");
            assertEquals(18, svc.getCwWpm());
            HubConfig.set("tx.cwWpm", "99");              // clamped to the modem's 5–60 range
            assertEquals(60, svc.getCwWpm());
            HubConfig.set("tx.cwWpm", "not a number");    // unreadable → the saved speed is kept
            assertEquals(60, svc.getCwWpm());

            assertFalse(svc.isLocalSkimmerEnabled());      // the local CW skimmer is off until J-Hub turns it on
            HubConfig.setBool("digi.localSkimmer", true);
            assertTrue(svc.isLocalSkimmerEnabled());
            HubConfig.setBool("digi.localSkimmer", false);
            assertFalse(svc.isLocalSkimmerEnabled());
        } finally {
            System.setProperty("user.home", oldHome);
            HubConfig.reload();
        }
    }
}
