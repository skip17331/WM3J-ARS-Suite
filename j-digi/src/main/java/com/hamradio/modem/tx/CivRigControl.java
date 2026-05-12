package com.hamradio.modem.tx;

import com.jlog.civ.CivEngine;

import java.util.Objects;

/**
 * RigControl that talks directly to an Icom rig over CI-V via the
 * shared {@link com.jlog.civ.CivEngine} from {@code j-log-engine}. Use
 * this when you already have a CI-V session open (for rig polling /
 * frequency tracking) and don't want to also run {@code rigctld}.
 */
public final class CivRigControl implements RigControl {

    private final CivEngine civ;

    public CivRigControl(CivEngine civ) {
        this.civ = Objects.requireNonNull(civ, "civ");
    }

    @Override
    public void setPtt(boolean on) {
        civ.setPtt(on);
    }
}
