package com.jlog.util;

import java.util.*;

/**
 * BandPlan — US FCC Part 97 amateur band allocations and validation.
 *
 * Validates frequency + mode combinations and exposes segment data
 * for the band plan viewer. All frequencies are in kHz internally.
 *
 * Validation returns the most critical issue found (error > warning > ok).
 */
public final class BandPlan {

    private BandPlan() {}

    // ── Mode groups (controls what's allowed in a segment) ────────────

    public enum ModeGroup {
        CW_ONLY,   // CW only (no phone, no broad digital)
        CW_DATA,   // CW + RTTY/Data (no phone/SSB)
        PHONE,     // All modes: phone, CW, digital
        FM,        // FM/repeater sub-band
        SATELLITE, // Satellite operations
        USB_ONLY;  // USB phone only (60m FCC rule)

        public String label() {
            return switch (this) {
                case CW_ONLY   -> "CW Only";
                case CW_DATA   -> "CW / Data";
                case PHONE     -> "Phone / CW";
                case FM        -> "FM / Phone";
                case SATELLITE -> "Satellite";
                case USB_ONLY  -> "USB Phone Only";
            };
        }

        public String styleClass() {
            return switch (this) {
                case CW_ONLY   -> "bp-cw";
                case CW_DATA   -> "bp-data";
                case PHONE     -> "bp-phone";
                case FM        -> "bp-fm";
                case SATELLITE -> "bp-satellite";
                case USB_ONLY  -> "bp-usb-only";
            };
        }
    }

    // ── Minimum license class for a segment ──────────────────────────

    public enum LicenseClass {
        EXTRA("Extra"),
        GENERAL("General+"),
        ALL("All (incl. Tech)");

        public final String label;
        LicenseClass(String label) { this.label = label; }

        public String styleClass() {
            return switch (this) {
                case EXTRA   -> "bp-extra";
                case GENERAL -> "bp-general";
                case ALL     -> "bp-all-class";
            };
        }
    }

    // ── Band segment ─────────────────────────────────────────────────

    public record BandSegment(
        double      startKhz,
        double      endKhz,
        String      description,
        ModeGroup   modeGroup,
        LicenseClass minLicense,
        String      notes
    ) {
        public String freqRangeLabel() {
            return String.format("%.3f\u2013%.3f", startKhz / 1000.0, endKhz / 1000.0);
        }
    }

    // ── Validation result ─────────────────────────────────────────────

    public enum Severity { OK, WARNING, ERROR }

    public record ValidationResult(Severity severity, String message) {
        public static ValidationResult ok()              { return new ValidationResult(Severity.OK, ""); }
        public static ValidationResult warn(String msg)  { return new ValidationResult(Severity.WARNING, msg); }
        public static ValidationResult error(String msg) { return new ValidationResult(Severity.ERROR, msg); }
        public boolean isOk()      { return severity == Severity.OK; }
        public boolean isWarning() { return severity == Severity.WARNING; }
        public boolean isError()   { return severity == Severity.ERROR; }
    }

    // ── Band plan data ─────────────────────────────────────────────────

    private static final Map<String, List<BandSegment>> PLAN = new LinkedHashMap<>();
    private static final double SSB_BW_KHZ = 3.0; // assumed occupied SSB bandwidth

    static {
        // ── 160m ──────────────────────────────────────────────────────
        band("160m",
            seg(1800, 2000, "CW / Phone / Data",     ModeGroup.PHONE,     LicenseClass.GENERAL,
                "FT8: 1.840 · SSB: 1.900 MHz"));

        // ── 80m ───────────────────────────────────────────────────────
        band("80m",
            seg(3500, 3525, "CW",                    ModeGroup.CW_ONLY,   LicenseClass.EXTRA,
                "CW calling: 3.500 MHz"),
            seg(3525, 3600, "CW / Data",              ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "FT8: 3.573 · FT4: 3.576 MHz"),
            seg(3600, 3700, "Phone / CW (Extra)",     ModeGroup.PHONE,     LicenseClass.EXTRA,
                "LSB DX: 3.600–3.700 MHz"),
            seg(3700, 4000, "Phone / CW",             ModeGroup.PHONE,     LicenseClass.GENERAL,
                "LSB calling: 3.900 · AM: 3.885 MHz"));

        // ── 60m (5 USB channels) ──────────────────────────────────────
        band("60m",
            seg(5330, 5407, "USB Phone (5 channels)", ModeGroup.USB_ONLY,  LicenseClass.GENERAL,
                "5.332 · 5.348 · 5.358.5 · 5.373 · 5.405 MHz"));

        // ── 40m ───────────────────────────────────────────────────────
        band("40m",
            seg(7000,  7025, "CW",                   ModeGroup.CW_ONLY,   LicenseClass.EXTRA,
                "CW calling: 7.023 MHz"),
            seg(7025,  7125, "CW / Data",             ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "FT8: 7.074 · FT4: 7.048 · JS8: 7.078"),
            seg(7125,  7175, "Phone / CW (Extra)",    ModeGroup.PHONE,     LicenseClass.EXTRA,
                "LSB DX window"),
            seg(7175,  7300, "Phone / CW",            ModeGroup.PHONE,     LicenseClass.GENERAL,
                "LSB calling: 7.290 · AM: 7.290 MHz"));

        // ── 30m ───────────────────────────────────────────────────────
        band("30m",
            seg(10100, 10150, "CW / Data (no phone)", ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "FT8: 10.136 · FT4: 10.140 · WSPR: 10.140"));

        // ── 20m ───────────────────────────────────────────────────────
        band("20m",
            seg(14000, 14025, "CW",                   ModeGroup.CW_ONLY,   LicenseClass.EXTRA,
                "CW QRP: 14.060 MHz"),
            seg(14025, 14150, "CW / Data",             ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "FT8: 14.074 · FT4: 14.080 · JS8: 14.078"),
            seg(14150, 14175, "Phone / CW (Extra)",    ModeGroup.PHONE,     LicenseClass.EXTRA,
                "USB DX window"),
            seg(14175, 14350, "Phone / CW",            ModeGroup.PHONE,     LicenseClass.GENERAL,
                "USB calling: 14.225 · AM: 14.286 MHz"));

        // ── 17m ───────────────────────────────────────────────────────
        band("17m",
            seg(18068, 18110, "CW / Data",             ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "FT8: 18.100 · FT4: 18.104 MHz"),
            seg(18110, 18168, "Phone / CW",            ModeGroup.PHONE,     LicenseClass.GENERAL,
                "USB calling: 18.130 MHz"));

        // ── 15m ───────────────────────────────────────────────────────
        band("15m",
            seg(21000, 21025, "CW",                    ModeGroup.CW_ONLY,   LicenseClass.EXTRA,
                "CW calling: 21.025 MHz"),
            seg(21025, 21200, "CW / Data",              ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "FT8: 21.074 · FT4: 21.140 MHz"),
            seg(21200, 21225, "Phone / CW (Extra)",     ModeGroup.PHONE,     LicenseClass.EXTRA,
                "USB DX window"),
            seg(21225, 21450, "Phone / CW",             ModeGroup.PHONE,     LicenseClass.GENERAL,
                "USB calling: 21.285 MHz"));

        // ── 12m ───────────────────────────────────────────────────────
        band("12m",
            seg(24890, 24930, "CW / Data",              ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "FT8: 24.915 · FT4: 24.919 MHz"),
            seg(24930, 24990, "Phone / CW",             ModeGroup.PHONE,     LicenseClass.GENERAL,
                "USB calling: 24.950 MHz"));

        // ── 10m ───────────────────────────────────────────────────────
        band("10m",
            seg(28000, 28300, "CW / Data",              ModeGroup.CW_DATA,   LicenseClass.ALL,
                "FT8: 28.074 · FT4: 28.180 · CW QRP: 28.060"),
            seg(28300, 28500, "Phone / CW",             ModeGroup.PHONE,     LicenseClass.ALL,
                "USB calling: 28.400 MHz (Novice/Tech)"),
            seg(28500, 29300, "Phone / CW",             ModeGroup.PHONE,     LicenseClass.GENERAL,
                "USB calling: 28.600 · AM: 29.000 MHz"),
            seg(29300, 29510, "Satellite",              ModeGroup.SATELLITE, LicenseClass.GENERAL,
                "Satellite uplink/downlink"),
            seg(29510, 29700, "FM Simplex",             ModeGroup.FM,        LicenseClass.GENERAL,
                "FM calling: 29.600 MHz"));

        // ── 6m ────────────────────────────────────────────────────────
        band("6m",
            seg(50000,  50100,  "CW / Weak Signal",    ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "CW beacon calling: 50.060 · EME: 50.080"),
            seg(50100,  50300,  "CW / SSB DX",         ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "SSB calling: 50.125 · FT8: 50.313"),
            seg(50300,  51000,  "All Modes",            ModeGroup.PHONE,     LicenseClass.GENERAL,
                "FM calling: 50.400 · FT8: 50.313"),
            seg(51000,  54000,  "FM / Repeaters",       ModeGroup.FM,        LicenseClass.GENERAL,
                "FM simplex: 52.525 · 53.000 MHz"));

        // ── 2m ────────────────────────────────────────────────────────
        band("2m",
            seg(144000, 144100, "CW / EME",             ModeGroup.CW_ONLY,   LicenseClass.ALL,
                "EME/moonbounce · CW calling: 144.100"),
            seg(144100, 144300, "CW / SSB",             ModeGroup.CW_DATA,   LicenseClass.ALL,
                "SSB calling: 144.200 · FT8: 144.174"),
            seg(144300, 145000, "All Modes",             ModeGroup.PHONE,     LicenseClass.ALL,
                "FM simplex: 144.360 · APRS: 144.390"),
            seg(145000, 148000, "FM / Repeaters",        ModeGroup.FM,        LicenseClass.ALL,
                "FM calling: 146.520 · Packet: 145.010"));

        // ── 70cm ──────────────────────────────────────────────────────
        band("70cm",
            seg(420000, 426000, "All Modes",             ModeGroup.PHONE,     LicenseClass.GENERAL,
                ""),
            seg(426000, 432000, "ATV / All Modes",       ModeGroup.PHONE,     LicenseClass.GENERAL,
                "ATV (Amateur TV)"),
            seg(432000, 432100, "CW / EME",              ModeGroup.CW_ONLY,   LicenseClass.GENERAL,
                "Moonbounce calling: 432.010"),
            seg(432100, 433000, "All Modes / SSB",       ModeGroup.CW_DATA,   LicenseClass.GENERAL,
                "SSB calling: 432.100 · FT8: 432.174"),
            seg(433000, 435000, "FM / Repeaters",        ModeGroup.FM,        LicenseClass.ALL,
                "FM calling: 433.000 MHz"),
            seg(435000, 438000, "Satellite",             ModeGroup.SATELLITE, LicenseClass.ALL,
                "AO-7, AO-8 passband"),
            seg(438000, 450000, "FM / Phone / Mixed",    ModeGroup.FM,        LicenseClass.ALL,
                "FM repeaters: 447.000 MHz"));
    }

    // ── Public accessors ─────────────────────────────────────────────

    /** All band names in standard order. */
    public static List<String> allBands() {
        return new ArrayList<>(PLAN.keySet());
    }

    /** All segments for a given band, or empty list if unknown. */
    public static List<BandSegment> getSegments(String band) {
        return band == null ? List.of() : PLAN.getOrDefault(band, List.of());
    }

    /**
     * Validate a frequency (in kHz) + mode combination.
     * Returns the most critical issue found; ValidationResult.ok() if all is well.
     *
     * @param freqKhz  operating frequency in kHz (e.g., 14225.0)
     * @param mode     mode string matching cbMode values (USB, LSB, CW, FT8, etc.)
     */
    public static ValidationResult validate(double freqKhz, String mode) {
        if (freqKhz <= 0 || mode == null || mode.isBlank()) return ValidationResult.ok();

        // Find the segment this frequency falls in
        BandSegment seg = findSegment(freqKhz);
        if (seg == null) {
            return ValidationResult.error(
                String.format("%.3f MHz is outside all amateur band allocations", freqKhz / 1000.0));
        }

        String m = mode.toUpperCase();
        boolean isPhone   = Set.of("USB","LSB","AM","FM").contains(m);
        boolean isSSB     = "USB".equals(m) || "LSB".equals(m);
        boolean isDigital = Set.of("FT8","FT4","RTTY","PSK31","OLIVIA","DV").contains(m);

        // ── Mode-for-segment checks (errors) ─────────────────────────

        if (isPhone && seg.modeGroup() == ModeGroup.CW_ONLY) {
            return ValidationResult.error(String.format(
                "Phone/SSB not permitted in CW-only segment %s MHz",
                seg.freqRangeLabel()));
        }

        if (isPhone && seg.modeGroup() == ModeGroup.CW_DATA) {
            return ValidationResult.error(String.format(
                "Phone/SSB not permitted in CW/Data segment %s MHz",
                seg.freqRangeLabel()));
        }

        // 60m: USB only (FCC rule)
        if (seg.modeGroup() == ModeGroup.USB_ONLY) {
            if ("LSB".equals(m)) {
                return ValidationResult.error("60m requires USB per FCC Part 97.305(d)");
            }
            if (isDigital && !Set.of("FT8","FT4","RTTY","PSK31").contains(m)) {
                return ValidationResult.warn("60m digital modes require specific EIRP limits — check Part 97.303(h)");
            }
        }

        // ── SSB edge proximity checks (warnings) ─────────────────────

        if (isSSB) {
            // Find the phone-allowed segment this freq is in (must be PHONE or USB_ONLY)
            double segStart = seg.startKhz();
            double segEnd   = seg.endKhz();

            if ("USB".equals(m)) {
                // Occupied BW extends upward from carrier
                if (freqKhz + SSB_BW_KHZ > segEnd) {
                    return ValidationResult.warn(String.format(
                        "USB at %.3f MHz: signal may extend %.0f kHz above segment edge (%.3f MHz)",
                        freqKhz / 1000.0, (freqKhz + SSB_BW_KHZ) - segEnd, segEnd / 1000.0));
                }
                // Also warn about band edge specifically
                double bandEnd = lastSegment(seg, freqKhz).endKhz();
                if (freqKhz + SSB_BW_KHZ > bandEnd) {
                    return ValidationResult.error(String.format(
                        "USB at %.3f MHz: signal extends %.0f kHz beyond band edge (%.3f MHz) — out of band!",
                        freqKhz / 1000.0, (freqKhz + SSB_BW_KHZ) - bandEnd, bandEnd / 1000.0));
                }
                // Wrong sideband advisory
                if (freqKhz < 10000) {
                    return ValidationResult.warn(
                        "Below 10 MHz: LSB is the conventional sideband for phone");
                }
            }

            if ("LSB".equals(m)) {
                // Occupied BW extends downward from carrier
                if (freqKhz - SSB_BW_KHZ < segStart) {
                    return ValidationResult.warn(String.format(
                        "LSB at %.3f MHz: signal may extend %.0f kHz below segment edge (%.3f MHz)",
                        freqKhz / 1000.0, segStart - (freqKhz - SSB_BW_KHZ), segStart / 1000.0));
                }
                double bandStart = firstSegment(seg, freqKhz).startKhz();
                if (freqKhz - SSB_BW_KHZ < bandStart) {
                    return ValidationResult.error(String.format(
                        "LSB at %.3f MHz: signal extends %.0f kHz below band edge (%.3f MHz) — out of band!",
                        freqKhz / 1000.0, bandStart - (freqKhz - SSB_BW_KHZ), bandStart / 1000.0));
                }
                // Wrong sideband advisory
                if (freqKhz >= 10000) {
                    return ValidationResult.warn(
                        "Above 10 MHz: USB is the conventional sideband for phone");
                }
            }
        }

        // ── Digital in CW-only advisory ──────────────────────────────

        if (isDigital && seg.modeGroup() == ModeGroup.CW_ONLY) {
            return ValidationResult.warn(String.format(
                "Wide digital modes not typical in CW-only sub-band %s MHz",
                seg.freqRangeLabel()));
        }

        return ValidationResult.ok();
    }

    // ── Internal helpers ─────────────────────────────────────────────

    private static BandSegment findSegment(double freqKhz) {
        for (List<BandSegment> segs : PLAN.values()) {
            for (BandSegment s : segs) {
                if (freqKhz >= s.startKhz() && freqKhz <= s.endKhz()) return s;
            }
        }
        return null;
    }

    /** Return the last contiguous segment in the same band as seg. */
    private static BandSegment lastSegment(BandSegment seg, double freqKhz) {
        for (List<BandSegment> segs : PLAN.values()) {
            if (segs.contains(seg)) return segs.get(segs.size() - 1);
        }
        return seg;
    }

    /** Return the first contiguous segment in the same band as seg. */
    private static BandSegment firstSegment(BandSegment seg, double freqKhz) {
        for (List<BandSegment> segs : PLAN.values()) {
            if (segs.contains(seg)) return segs.get(0);
        }
        return seg;
    }

    // ── Builder helpers ───────────────────────────────────────────────

    private static void band(String name, BandSegment... segs) {
        PLAN.put(name, List.of(segs));
    }

    private static BandSegment seg(double start, double end, String desc,
                                    ModeGroup mg, LicenseClass lc, String notes) {
        return new BandSegment(start, end, desc, mg, lc, notes);
    }
}
