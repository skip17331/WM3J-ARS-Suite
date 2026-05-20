package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.RigSection;

/**
 * Tiny dispatcher that picks the active {@link RigController} based on
 * {@code RigSection.backend}.
 *
 * <p>The suite is single-rig: only one backend is alive at any time. Call
 * sites elsewhere ({@link MessageRouter}, {@link WebConfigServer}, etc.)
 * route through {@link #active()} so they never need to switch on the
 * backend name themselves — they just call {@code tune()}, {@code setPtt()},
 * etc. on whatever's live.
 *
 * <p>{@link #restart(RigSection)} owns the cross-backend switch: it stops
 * the previously-running controller (if it isn't the new one) and starts
 * the new one. Both singletons live for the lifetime of the JVM so the
 * "wrong" one is always present-but-idle.
 */
public final class RigControllers {

    private RigControllers() {}

    /** The controller that matches the current {@code backend}; never null. */
    public static RigController active() {
        String backend = ConfigManager.getInstance().getConfig().rig.backend;
        if ("CI_V".equals(backend))   return CivRigController.getInstance();
        // HAMLIB is the historical default; "NONE" returns the Hamlib singleton
        // too (it's not running, so all status getters return idle / false).
        return HamlibRigController.getInstance();
    }

    /** Convenience: like {@link #active()} but won't try to drive a backend
     *  that's set to NONE — returns null instead. Use when "no backend at
     *  all" needs to be visible to the caller (e.g. PTT REST endpoint
     *  returns 409 instead of silently no-opping). */
    public static RigController activeOrNull() {
        String backend = ConfigManager.getInstance().getConfig().rig.backend;
        if ("NONE".equals(backend)) return null;
        return active();
    }

    /**
     * Apply a new {@link RigSection}: stop the previously-active backend if
     * it differs from the new one, then start the new one. The stop is
     * idempotent (a no-op if the controller never started) so callers can
     * use this on every save without worrying about edge cases.
     */
    public static void restart(RigSection cfg) {
        if (cfg == null) {
            HamlibRigController.getInstance().stop();
            CivRigController.getInstance().stop();
            return;
        }
        // Stop the off-backends first (no-op if they weren't running).
        if (!"HAMLIB".equals(cfg.backend)) HamlibRigController.getInstance().stop();
        if (!"CI_V".equals(cfg.backend))   CivRigController.getInstance().stop();
        // Hand the new config to the matching controller; its own restart()
        // knows how to handle "same backend, different parameters" cleanly.
        if ("HAMLIB".equals(cfg.backend)) HamlibRigController.getInstance().restart(cfg);
        if ("CI_V".equals(cfg.backend))   CivRigController.getInstance().restart(cfg);
    }

    /** Wire both backends to the {@link MessageRouter} at bootstrap. */
    public static void setRouter(MessageRouter r) {
        HamlibRigController.getInstance().setRouter(r);
        CivRigController.getInstance().setRouter(r);
    }

    /** Start whichever backend matches {@code cfg}; both stay configured. */
    public static void startActive(RigSection cfg) {
        if ("HAMLIB".equals(cfg.backend)) HamlibRigController.getInstance().start(cfg);
        else if ("CI_V".equals(cfg.backend)) CivRigController.getInstance().start(cfg);
    }

    /** Stop whichever backend is currently running. */
    public static void stopAll() {
        HamlibRigController.getInstance().stop();
        CivRigController.getInstance().stop();
    }
}
