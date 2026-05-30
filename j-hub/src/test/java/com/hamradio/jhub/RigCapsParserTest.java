package com.hamradio.jhub;

import com.hamradio.jhub.model.RigCapabilities;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RigCapsParser}, fed canned rigctld dump bodies the
 * same way {@code HamlibRigController.sendCommand} delivers them (extended
 * protocol, RPRT terminator already stripped).
 */
class RigCapsParserTest {

    private static String res(String name) {
        try (InputStream in = RigCapsParserTest.class.getResourceAsStream("/rigcaps/" + name)) {
            assertNotNull(in, "missing test resource: " + name);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ── Full-featured dummy backend (model 1): everything on ─────────

    @Test
    void dummyCaps_fullyParsed() {
        RigCapabilities c = RigCapsParser.parse(res("dummy_caps.txt"), null);
        assertTrue(c.known);
        assertEquals(1, c.model);
        assertEquals("Dummy", c.modelName);
        assertEquals("Hamlib", c.mfgName);

        assertTrue(c.modes.contains("USB"));
        assertTrue(c.modes.contains("LSB"));
        assertTrue(c.modes.contains("CW"));
        assertTrue(c.modes.contains("FM"));
        assertTrue(c.vfoList.contains("VFOA"));
        assertTrue(c.vfoList.contains("VFOB"));

        assertTrue(c.setFreq);
        assertTrue(c.setMode);
        assertTrue(c.setVfo);
        assertTrue(c.getVfo);        // dummy CAN report current VFO
        assertTrue(c.splitVfo);
        assertTrue(c.splitFreq);
        assertTrue(c.splitMode);
        assertTrue(c.ritOffset);
        assertTrue(c.xitOffset);
        assertTrue(c.ritFunc);       // RIT present in Set functions
        assertTrue(c.xitFunc);

        assertTrue(c.getStrength);   // STRENGTH in Get level
        assertTrue(c.getRfPower);    // RFPOWER in Get level
        assertTrue(c.setRfPower);    // RFPOWER in Set level

        // All gating accessors permissive
        assertTrue(c.canSplit());
        assertTrue(c.canRit());
        assertTrue(c.canXit());
        assertTrue(c.canShowSMeter());
        assertTrue(c.canSetRfPower());
    }

    @Test
    void dummyState_txRangesGateKeypad() {
        RigCapabilities c = RigCapsParser.parse(null, res("dummy_state.txt"));
        assertFalse(c.txRanges.isEmpty(), "TX ranges should parse from dump_state");
        // Dummy TX range is 150 kHz .. 1.5 GHz
        assertTrue(c.canTransmitAt(14_200_000L), "20m must be inside dummy TX range");
        assertTrue(c.canTransmitAt(145_000_000L), "2m must be inside dummy TX range");
        assertFalse(c.canTransmitAt(100_000L), "100 kHz is below the rig's lowest TX edge");
    }

    // ── Limited FM rig: gating must turn controls OFF ────────────────

    @Test
    void limitedFmRig_gatesUnsupportedControls() {
        RigCapabilities c = RigCapsParser.parse(res("limited_fm_caps.txt"), null);
        assertTrue(c.known);
        assertEquals(9999, c.model);

        // Split / RIT / XIT all unsupported → gated off
        assertFalse(c.canSplit());
        assertFalse(c.canSplitFreq());
        assertFalse(c.canSplitMode());
        assertFalse(c.canRit());
        assertFalse(c.canXit());

        // Power: read-only meter present, but SET is not
        assertTrue(c.canShowRfPower());
        assertTrue(c.canShowSMeter());
        assertFalse(c.canSetRfPower());

        // Mode list is FM/WFM only
        assertTrue(c.hasMode("FM"));
        assertFalse(c.hasMode("USB"));
        assertFalse(c.hasMode("CW"));
    }

    /** Many Icoms report "Can get VFO: N" — the pane must not rely on it. */
    @Test
    void icomStyle_noCurrentVfo() {
        RigCapabilities c = RigCapsParser.parse(res("limited_fm_caps.txt"), null);
        assertFalse(c.getVfo);
        assertFalse(c.canGetVfo());
    }

    // ── Unknown / fallback: never gate (IC-746 must not regress) ─────

    @Test
    void unknownCaps_fallBackToEverythingEnabled() {
        RigCapabilities c = RigCapsParser.parse(null, null);
        assertFalse(c.known);
        assertTrue(c.canSplit());
        assertTrue(c.canRit());
        assertTrue(c.canXit());
        assertTrue(c.canShowSMeter());
        assertTrue(c.canSetRfPower());
        assertTrue(c.canTransmitAt(14_200_000L));
        assertTrue(c.canTransmitAt(50_000L));     // unknown → don't gate keypad
        assertTrue(c.hasMode("USB"));             // unknown → don't gate modes
    }

    @Test
    void emptyAndGarbageBody_isUnknown() {
        assertFalse(RigCapsParser.parse("", "").known);
        assertFalse(RigCapsParser.parse("garbage line\nanother\n", null).known);
    }

    // ── Regex trap: RFPOWER_METER must NOT count as RFPOWER ──────────

    @Test
    void rfPowerMeterDoesNotFalseMatchRfPower() {
        String caps =
            "Caps dump for model: 7\n" +
            "Mode list: USB LSB\n" +
            "Get level: STRENGTH(-54..60/0) RFPOWER_METER(0..1/0) RFPOWER_METER_WATTS(0..0/0)\n" +
            "Set level: AF(0..1/0)\n" +
            "Can set Frequency:\tY\n";
        RigCapabilities c = RigCapsParser.parse(caps, null);
        assertTrue(c.getStrength);
        assertFalse(c.getRfPower, "RFPOWER_METER must not satisfy the bare RFPOWER token");
        assertFalse(c.setRfPower);
    }

    @Test
    void rfPowerBareTokenMatches() {
        String caps =
            "Mode list: USB\n" +
            "Get level: RFPOWER(0..1/0) RFPOWER_METER(0..1/0)\n" +
            "Set level: RFPOWER(0..1/0)\n";
        RigCapabilities c = RigCapsParser.parse(caps, null);
        assertTrue(c.getRfPower);
        assertTrue(c.setRfPower);
    }
}
