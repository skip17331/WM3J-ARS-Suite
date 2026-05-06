package com.jlog.macro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VoiceRecorder — captures audio from the default input device into a WAV
 * file. Used by the per-macro "hold to record" button on the Setup screen.
 *
 * <p>Singleton; only one recording at a time. Recording happens on a daemon
 * thread that streams the {@link TargetDataLine} into {@link AudioSystem#write}.
 *
 * <p>Format is fixed at 22050 Hz / 16-bit / mono — adequate dynamic range for
 * SSB voice keying while keeping file sizes small (~44 KB/s). Most contest
 * exchanges fit in a single 100 KB file.
 */
public class VoiceRecorder {

    private static final Logger log = LoggerFactory.getLogger(VoiceRecorder.class);
    private static final VoiceRecorder INSTANCE = new VoiceRecorder();
    public  static VoiceRecorder getInstance() { return INSTANCE; }
    private VoiceRecorder() {}

    /** 22050 Hz mono 16-bit signed PCM little-endian — see class javadoc. */
    public static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            22050f, 16, 1, 2, 22050f, false);

    private final AtomicBoolean recording = new AtomicBoolean(false);
    private volatile TargetDataLine line;
    private volatile Thread recordThread;
    private volatile File outputFile;

    public boolean isRecording() { return recording.get(); }

    /** The standard directory for user-recorded voice macros: ~/.j-log/voice/. */
    public static Path defaultVoiceDir() {
        return Paths.get(System.getProperty("user.home"), ".j-log", "voice");
    }

    /**
     * Begin recording into {@code outputWav}. Returns false if a recording is
     * already in progress, or if the input device can't be opened.
     */
    public boolean start(File outputWav) {
        if (outputWav == null) return false;
        if (!recording.compareAndSet(false, true)) return false;

        try {
            // Make sure the parent directory exists — first-run callers won't
            // have ~/.j-log/voice/ until we create it.
            File parent = outputWav.getParentFile();
            if (parent != null) Files.createDirectories(parent.toPath());

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                throw new IllegalStateException("No microphone supports " + FORMAT);
            }
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(FORMAT);
            line.start();
            outputFile = outputWav;
        } catch (Exception e) {
            log.warn("Voice recording start failed: {}", e.getMessage());
            recording.set(false);
            return false;
        }

        recordThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(line)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
            } catch (Exception e) {
                log.warn("Voice recording write failed: {}", e.getMessage());
            } finally {
                try { line.stop();  } catch (Exception ignored) {}
                try { line.close(); } catch (Exception ignored) {}
                line = null;
                recording.set(false);
            }
        }, "voice-recorder");
        recordThread.setDaemon(true);
        recordThread.start();
        return true;
    }

    /**
     * Stop the current recording and flush the WAV file to disk. Blocks
     * briefly (up to 500 ms) for the writer thread to finalize the WAV
     * header. Returns the file that was written, or null if not recording.
     */
    public File stop() {
        if (!recording.get()) return null;
        TargetDataLine l = line;
        if (l != null) {
            try { l.stop(); } catch (Exception ignored) {}
        }
        Thread t = recordThread;
        if (t != null) {
            try { t.join(500); }
            catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        return outputFile;
    }
}
