package com.jlog.macro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * VoiceKeyer — plays a WAV file through the default audio output device using
 * javax.sound.sampled. Replaces the JavaFX AudioClip path used by the original
 * VOICE_PLAY macro action; AudioClip cannot be aborted mid-clip and provides
 * no end-of-playback callback, both of which the voice keyer needs.
 *
 * <p>Singleton. Only one playback may be active at a time — {@link #play(File,
 * Runnable, Consumer)} returns false if a playback is already running, so
 * callers (MacroEngine) can implement the QUEUE / REPLACE / IGNORE
 * concurrent-macro modes themselves.
 *
 * <p>Each playback runs on a daemon thread so it never blocks the FX or
 * macro-executor threads. Aborting via {@link #stop()} closes the data line
 * promptly; the thread finishes naturally and the {@code onDone} callback is
 * <strong>not</strong> invoked when stopped (callers know they triggered it).
 */
public class VoiceKeyer {

    private static final Logger log = LoggerFactory.getLogger(VoiceKeyer.class);
    private static final VoiceKeyer INSTANCE = new VoiceKeyer();
    public  static VoiceKeyer getInstance() { return INSTANCE; }
    private VoiceKeyer() {}

    private final AtomicBoolean playing = new AtomicBoolean(false);
    private volatile SourceDataLine currentLine;
    private volatile Thread playbackThread;

    public boolean isPlaying() { return playing.get(); }

    /**
     * Begin playing {@code wav}. Returns false (and does nothing) if another
     * playback is already in flight — callers should {@link #stop()} first.
     *
     * @param wav      WAV file to play; must be readable
     * @param onDone   invoked on the playback thread when the file finishes
     *                 naturally (NOT called on stop())
     * @param onError  invoked on the playback thread if an exception occurs
     */
    public boolean play(File wav, Runnable onDone, Consumer<Exception> onError) {
        if (wav == null) return false;
        if (!playing.compareAndSet(false, true)) return false;

        playbackThread = new Thread(() -> {
            SourceDataLine line = null;
            try (AudioInputStream in = AudioSystem.getAudioInputStream(wav)) {
                AudioFormat fmt = in.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(fmt);
                line.start();
                currentLine = line;

                byte[] buf = new byte[4096];
                int n;
                while (playing.get() && (n = in.read(buf)) > 0) {
                    line.write(buf, 0, n);
                }
                // Block until the device has actually emitted everything we wrote
                // (otherwise the post-roll PTT-off would clip the tail).
                if (playing.get()) {
                    line.drain();
                    if (onDone != null) onDone.run();
                }
            } catch (Exception e) {
                log.warn("Voice playback failed: {}", e.getMessage());
                if (onError != null) onError.accept(e);
            } finally {
                if (line != null) {
                    try { line.stop();  } catch (Exception ignored) {}
                    try { line.close(); } catch (Exception ignored) {}
                }
                currentLine = null;
                playing.set(false);
            }
        }, "voice-keyer-play");
        playbackThread.setDaemon(true);
        playbackThread.start();
        return true;
    }

    /**
     * Abort any in-flight playback. Idempotent — no-op when nothing is playing.
     * The playback thread sees {@code playing == false} on the next loop
     * iteration and exits without invoking the {@code onDone} callback.
     */
    public void stop() {
        if (!playing.get()) return;
        playing.set(false);
        SourceDataLine line = currentLine;
        if (line != null) {
            try { line.stop();  } catch (Exception ignored) {}
            try { line.flush(); } catch (Exception ignored) {}
            try { line.close(); } catch (Exception ignored) {}
        }
    }
}
