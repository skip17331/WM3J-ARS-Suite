package com.jlog.bandplan;

import java.util.Objects;

/**
 * A frequency range within a band that carries a single dominant
 * activity (CW, DATA, PHONE, etc.). Immutable value object — built by
 * {@link BandplanLoader} from the bundled JSON.
 *
 * <p>Frequencies are in Hz to avoid floating-point drift when
 * comparing rig-readout frequencies against segment boundaries.
 */
public final class BandSegment {

    private final long     lowerHz;
    private final long     upperHz;
    private final Activity activity;
    private final String   label;

    public BandSegment(long lowerHz, long upperHz, Activity activity, String label) {
        if (lowerHz > upperHz) {
            throw new IllegalArgumentException(
                "lowerHz must be <= upperHz: " + lowerHz + " > " + upperHz);
        }
        this.lowerHz  = lowerHz;
        this.upperHz  = upperHz;
        this.activity = Objects.requireNonNull(activity, "activity");
        this.label    = label == null || label.isBlank() ? activity.name() : label.trim();
    }

    public long     lowerHz()  { return lowerHz;  }
    public long     upperHz()  { return upperHz;  }
    public Activity activity() { return activity; }
    public String   label()    { return label;    }

    /** True if {@code hz} falls inside this segment (inclusive of bounds). */
    public boolean contains(long hz) {
        return hz >= lowerHz && hz <= upperHz;
    }

    @Override public String toString() {
        return String.format("[%.3f-%.3f kHz %s — %s]",
                lowerHz / 1000.0, upperHz / 1000.0, activity, label);
    }
}
