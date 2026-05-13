package com.hamradio.modem.tx;

/**
 * RigControl that issues no key command. This <em>is</em> the VOX /
 * audio-sensing PTT path — j-digi outputs audio, and whatever is
 * between j-digi and the antenna (the rig's built-in VOX, or an
 * interface like SignaLink / DigiRig that does audio-sense PTT on its
 * own board) handles the keying.
 *
 * <p>Operator responsibilities:
 * <ul>
 *   <li>Enable VOX on the rig (or have an interface that does it).</li>
 *   <li>Set VOX gain so room noise / monitor audio doesn't trigger.</li>
 *   <li>For pure CW the rig probably won't VOX on a sidetone alone —
 *       use {@code HAMLIB} mode instead.</li>
 * </ul>
 *
 * <p>Kept under the {@code NoOpRigControl} name for source-stability;
 * conceptually this is "VOX mode."
 */
public class NoOpRigControl implements RigControl {
    @Override
    public void setPtt(boolean on) {
        // VOX: rig / audio-sense interface keys itself off the audio.
    }
}
