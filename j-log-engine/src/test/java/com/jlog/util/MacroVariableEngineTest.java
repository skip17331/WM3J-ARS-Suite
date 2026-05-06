package com.jlog.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MacroVariableEngineTest {

    private static MacroVariableEngine.Context fullContext() {
        MacroVariableEngine.Context ctx = new MacroVariableEngine.Context();
        ctx.myCall       = "WM3J";
        ctx.dxCall       = "K1ABC";
        ctx.rstSent      = "599";
        ctx.rstReceived  = "579";
        ctx.name         = "Bob";
        ctx.exchange     = "FM19";
        ctx.serialNumber = 7;
        ctx.frequencyHz  = 14_225_000L;
        ctx.mode         = "CW";
        return ctx;
    }

    @Test
    void substitutesAllKnownVariables() {
        String template = "{CALL} DE {MYCALL} TU UR RST {RST_S} {RST_R} NAME {NAME} EXCH {EXCH} NR {NR} #{SERIAL} {FREQ} MHz {BAND} {MODE}";
        String expected = "K1ABC DE WM3J TU UR RST 599 579 NAME Bob EXCH FM19 NR 7 #007 14.225 MHz 20m CW";
        assertEquals(expected, MacroVariableEngine.substitute(template, fullContext()));
    }

    @Test
    void rstAliasMatchesRstSent() {
        // {RST} is a legacy alias for {RST_S} — used by existing j-digi templates.
        MacroVariableEngine.Context ctx = fullContext();
        ctx.rstSent = "569";
        assertEquals("RST 569", MacroVariableEngine.substitute("RST {RST}", ctx));
    }

    @Test
    void blankRstFieldsFallBackToFiveNine() {
        MacroVariableEngine.Context ctx = fullContext();
        ctx.rstSent     = "";
        ctx.rstReceived = "  ";
        assertEquals("599 / 599", MacroVariableEngine.substitute("{RST_S} / {RST_R}", ctx));
    }

    @Test
    void blankNameFallsBackToOM() {
        MacroVariableEngine.Context ctx = fullContext();
        ctx.name = "";
        assertEquals("HI OM", MacroVariableEngine.substitute("HI {NAME}", ctx));
    }

    @Test
    void unknownPlaceholdersAreLeftLiteral() {
        // Future variables shouldn't break old templates; unknown tokens pass through.
        String result = MacroVariableEngine.substitute("{CALL} {UNKNOWN_THING} done", fullContext());
        assertEquals("K1ABC {UNKNOWN_THING} done", result);
    }

    @Test
    void zeroSerialProducesEmptyString() {
        // Non-contest QSOs have no serial — substitution should not litter "0" or "000".
        MacroVariableEngine.Context ctx = fullContext();
        ctx.serialNumber = 0;
        assertEquals("nr  serial ", MacroVariableEngine.substitute("nr {NR} serial {SERIAL}", ctx));
    }

    @Test
    void serialIsZeroPaddedToThreeDigits() {
        MacroVariableEngine.Context ctx = fullContext();
        ctx.serialNumber = 42;
        assertEquals("042", MacroVariableEngine.substitute("{SERIAL}", ctx));
        ctx.serialNumber = 1234;
        assertEquals("1234", MacroVariableEngine.substitute("{SERIAL}", ctx));
    }

    @Test
    void zeroFrequencyProducesDashes() {
        MacroVariableEngine.Context ctx = fullContext();
        ctx.frequencyHz = 0L;
        assertEquals("-- on --", MacroVariableEngine.substitute("{FREQ} on {BAND}", ctx));
    }

    @Test
    void frequencyOutsideHamBandsProducesUnknownBand() {
        MacroVariableEngine.Context ctx = fullContext();
        ctx.frequencyHz = 12_345_000L; // not a ham band
        assertEquals("12.345 / --", MacroVariableEngine.substitute("{FREQ} / {BAND}", ctx));
    }

    @Test
    void dxCallIsTrimmed() {
        // Operators often leave a trailing space after typing the call — substitution
        // must not propagate it into the keyed CW.
        MacroVariableEngine.Context ctx = fullContext();
        ctx.dxCall = "  K1ABC  ";
        assertEquals("K1ABC", MacroVariableEngine.substitute("{CALL}", ctx));
    }

    @Test
    void nullTemplateReturnsNull() {
        assertNull(MacroVariableEngine.substitute(null, fullContext()));
    }

    @Test
    void nullContextReturnsTemplateUnchanged() {
        assertEquals("{CALL}", MacroVariableEngine.substitute("{CALL}", null));
    }

    @Test
    void nullContextFieldsBecomeEmpty() {
        // Defensive: if AppConfig hands us a null callsign, we must not NPE.
        MacroVariableEngine.Context ctx = new MacroVariableEngine.Context();
        ctx.dxCall = null; ctx.myCall = null;
        assertEquals(" DE ", MacroVariableEngine.substitute("{CALL} DE {MYCALL}", ctx));
    }

    @Test
    void bandFromHzCoversTheStandardHfBands() {
        assertEquals("160m", MacroVariableEngine.bandFromHz( 1_900_000L));
        assertEquals("80m",  MacroVariableEngine.bandFromHz( 3_750_000L));
        assertEquals("40m",  MacroVariableEngine.bandFromHz( 7_150_000L));
        assertEquals("20m",  MacroVariableEngine.bandFromHz(14_200_000L));
        assertEquals("15m",  MacroVariableEngine.bandFromHz(21_300_000L));
        assertEquals("10m",  MacroVariableEngine.bandFromHz(28_500_000L));
        assertEquals("6m",   MacroVariableEngine.bandFromHz(50_125_000L));
        assertEquals("2m",   MacroVariableEngine.bandFromHz(146_520_000L));
        assertEquals("70cm", MacroVariableEngine.bandFromHz(432_000_000L));
        assertEquals("--",   MacroVariableEngine.bandFromHz(  9_000_000L));
    }
}
