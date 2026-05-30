package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.RigSection;
import com.hamradio.jhub.model.RigCapabilities;
import com.hamradio.jhub.model.RigStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end test of {@link HamlibRigController} against a live {@code rigctld}
 * dummy backend (model 1). Auto-skips when {@code rigctld} isn't on PATH so it
 * never breaks a CI box without Hamlib installed.
 *
 * <p>Drives the controller and reads results back from {@link StateCache},
 * which both {@code publishRigStatus} and {@code publishRigCaps} populate.
 */
class HamlibRigControllerIntegrationTest {

    private static final int PORT = 45321;
    private Process rigctld;
    private final HamlibRigController ctrl = HamlibRigController.getInstance();

    private static boolean rigctldOnPath() {
        for (String dir : System.getenv("PATH").split(":")) {
            if (new java.io.File(dir, "rigctld").canExecute()) return true;
        }
        return false;
    }

    private void startRigctld() throws IOException {
        rigctld = new ProcessBuilder("rigctld", "-m", "1", "-t", String.valueOf(PORT))
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        // Wait for the listener to come up.
        waitUntil(() -> {
            try (Socket s = new Socket()) { s.connect(new InetSocketAddress("127.0.0.1", PORT), 200); return true; }
            catch (IOException e) { return false; }
        }, 5000, "rigctld to start listening");
    }

    @AfterEach
    void tearDown() {
        ctrl.stop();
        if (rigctld != null) rigctld.destroy();
    }

    private static void waitUntil(BooleanSupplier cond, long timeoutMs, String what) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (cond.getAsBoolean()) return;
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
        fail("Timed out waiting for " + what);
    }

    private RigSection cfg() {
        RigSection c = new RigSection();
        c.backend       = "HAMLIB";
        c.hamlibHost    = "127.0.0.1";
        c.hamlibPort    = PORT;
        c.manageRigctld = false;     // we spawn it ourselves
        c.pollRateMs    = 200;
        c.enablePtt     = true;      // so PTT-related paths don't short-circuit
        return c;
    }

    @Test
    void connectsProbesCapsAndPolls() throws IOException {
        assumeTrue(rigctldOnPath(), "rigctld not installed — skipping integration test");
        startRigctld();

        ctrl.setRouter(MessageRouter.getInstance());
        ctrl.start(cfg());

        // Capabilities published once on connect
        waitUntil(() -> StateCache.getInstance().getLastRigCaps() != null, 5000, "RIG_CAPS");
        RigCapabilities caps = StateCache.getInstance().getLastRigCaps();
        assertTrue(caps.known, "dummy caps should parse as known");
        assertTrue(caps.modes.contains("FM"));
        assertTrue(caps.canSplit());
        assertTrue(caps.canRit());
        assertTrue(caps.canShowSMeter());
        assertFalse(caps.txRanges.isEmpty(), "dump_state TX ranges should be present");

        // Snapshot poll published (dummy defaults: 145 MHz FM on Main)
        waitUntil(() -> StateCache.getInstance().getLastRigStatus() != null, 5000, "RIG_STATUS");
        RigStatus st = StateCache.getInstance().getLastRigStatus();
        assertEquals("HAMLIB", st.source);
        assertTrue(st.frequency > 0, "frequency should poll non-zero");
        assertFalse(st.vfo.isBlank(), "get_rig_info should yield an active VFO");
        assertNotEquals(Integer.MIN_VALUE, st.sMeterDb, "S-meter should be read (dummy reports STRENGTH)");
    }

    @Test
    void tuneRitXitSplitRoundTrip() throws IOException {
        assumeTrue(rigctldOnPath(), "rigctld not installed — skipping integration test");
        startRigctld();
        ctrl.setRouter(MessageRouter.getInstance());
        ctrl.start(cfg());
        waitUntil(() -> StateCache.getInstance().getLastRigStatus() != null, 5000, "first RIG_STATUS");

        // Tune
        ctrl.tune(14_205_000L, "USB");
        waitUntil(() -> {
            RigStatus s = StateCache.getInstance().getLastRigStatus();
            return s.frequency == 14_205_000L && "USB".equalsIgnoreCase(s.mode);
        }, 3000, "tune to take effect");

        // RIT offset + enable (setters force an aux re-read next cycle)
        ctrl.setRit(100);
        ctrl.setRitEnabled(true);
        waitUntil(() -> {
            RigStatus s = StateCache.getInstance().getLastRigStatus();
            return s.rit == 100 && s.ritOn;
        }, 3000, "RIT to reflect");

        // XIT
        ctrl.setXit(-250);
        ctrl.setXitEnabled(true);
        waitUntil(() -> {
            RigStatus s = StateCache.getInstance().getLastRigStatus();
            return s.xit == -250 && s.xitOn;
        }, 3000, "XIT to reflect");

        // Split on → get_rig_info should report split true
        ctrl.setSplit(true);
        waitUntil(() -> StateCache.getInstance().getLastRigStatus().split, 3000, "split to engage");
    }
}
