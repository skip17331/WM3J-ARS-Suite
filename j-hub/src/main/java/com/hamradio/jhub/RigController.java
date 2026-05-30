package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.RigSection;
import com.hamradio.jhub.model.RigCapabilities;

/**
 * Common contract for a backend that drives the operator's rig.
 *
 * <p>Two implementations exist: {@link HamlibRigController} (TCP to a
 * {@code rigctld} daemon — the universal-but-installation-heavy path) and
 * {@link CivRigController} (direct Icom CI-V over a serial port — light
 * native option for the "I don't want Hamlib installed" crowd, and the
 * historically lowest-latency option on Icom rigs).
 *
 * <p>Exactly one backend is active at any time, determined by
 * {@code RigSection.backend}; the {@link RigControllers} dispatcher takes
 * care of switching. Call sites elsewhere in the codebase only see this
 * interface and never need to care which backend is live.
 */
public interface RigController {

    // ── Lifecycle ───────────────────────────────────────────────────

    void setRouter(MessageRouter router);
    void start(RigSection cfg);
    void stop();
    void restart(RigSection cfg);
    /** Force-close and reopen the rig connection. */
    void reconnect();

    // ── Status ──────────────────────────────────────────────────────

    boolean isRunning();
    boolean isConnected();
    long    getLastFreq();
    String  getLastMode();
    String  getLastError();
    /** True once the backend has decided this rig can't accept CW keying commands. */
    boolean isCwUnsupported();

    /** Capabilities of the connected rig, or {@code null} until the backend
     *  has probed them. Backends that don't query capabilities return null,
     *  which callers treat as "unknown → don't gate" (see
     *  {@link RigCapabilities#known}). */
    default RigCapabilities getCapabilities() { return null; }

    // ── Commands ────────────────────────────────────────────────────

    /** Tune the rig (set frequency in Hz, set mode by name e.g. "USB"/"CW"). */
    void tune(long freqHz, String mode);
    /** Swap (exchange) VFOs A and B. Backends without a native swap should
     *  log a warning and no-op rather than throw. */
    default void swapVfo() { /* default no-op; backends override */ }
    /** Enable / disable split-frequency operation. When enabled, the rig
     *  receives on the current VFO and transmits on the other (TX VFO).
     *  Backends without native split support no-op by default. */
    default void setSplit(boolean on) { /* default no-op; backends override */ }
    /** Select the active VFO by Hamlib name (e.g. "VFOA"/"VFOB"/"Main"/"Sub"/"MEM").
     *  Backends without VFO selection no-op by default. */
    default void setVfo(String vfo) { /* default no-op; backends override */ }
    /** Set the split (TX) frequency in Hz. No-op when the rig can't set split freq. */
    default void setSplitFreq(long freqHz) { /* default no-op */ }
    /** Set the split (TX) mode + passband (Hz; 0 = backend default width). */
    default void setSplitMode(String mode, int widthHz) { /* default no-op */ }
    /** Set the RIT offset in Hz (0 clears). No-op when unsupported. */
    default void setRit(int hz) { /* default no-op */ }
    /** Enable/disable RIT (Hamlib func RIT). No-op when unsupported. */
    default void setRitEnabled(boolean on) { /* default no-op */ }
    /** Set the XIT offset in Hz (0 clears). No-op when unsupported. */
    default void setXit(int hz) { /* default no-op */ }
    /** Enable/disable XIT (Hamlib func XIT). No-op when unsupported. */
    default void setXitEnabled(boolean on) { /* default no-op */ }
    /** Set RF output power as a 0.0–1.0 fraction (Hamlib level RFPOWER). */
    default void setRfPower(double fraction) { /* default no-op */ }
    /** Key (true) or un-key (false) the transmitter. No-op when PTT disabled in config. */
    void setPtt(boolean on);
    /** Set the rig keyer speed in WPM. No-op for backends that don't support CW. */
    void setKeyerSpeed(int wpm);
    /** Send the given text as CW via the rig keyer. No-op for backends that don't support CW. */
    void sendCw(String text, int wpm);
    /** Abort an in-flight CW transmission. No-op for backends that don't support CW. */
    void stopCw();
}
