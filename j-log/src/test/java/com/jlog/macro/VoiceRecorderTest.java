package com.jlog.macro;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class VoiceRecorderTest {

    @Test
    void defaultVoiceDirIsUnderHomeDotJlog() {
        Path expected = Path.of(System.getProperty("user.home"), ".j-log", "voice");
        assertEquals(expected, VoiceRecorder.defaultVoiceDir());
    }

    @Test
    void notRecordingByDefault() {
        VoiceRecorder.getInstance().stop(); // defensive — singleton may carry state
        assertFalse(VoiceRecorder.getInstance().isRecording());
    }

    @Test
    void startNullFileReturnsFalse() {
        assertFalse(VoiceRecorder.getInstance().start(null));
        assertFalse(VoiceRecorder.getInstance().isRecording());
    }

    @Test
    void stopWhenNotRecordingReturnsNull() {
        assertNull(VoiceRecorder.getInstance().stop());
    }

    @Test
    void recordRoundTripWritesNonEmptyWav(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        // Skip on headless systems with no microphone available.
        assumeTrue(AudioSystem.isLineSupported(new DataLine.Info(TargetDataLine.class, VoiceRecorder.FORMAT)),
                "no audio input device available in this environment");

        File out = tmp.resolve("test_recording.wav").toFile();
        boolean started = VoiceRecorder.getInstance().start(out);
        assertTrue(started, "recorder should start when a mic is available");
        assertTrue(VoiceRecorder.getInstance().isRecording());

        // Capture a brief buffer of audio.
        Thread.sleep(150);

        File written = VoiceRecorder.getInstance().stop();
        assertNotNull(written, "stop() should return the output file");
        // Even silence produces a non-trivial WAV (44-byte header + samples).
        assertTrue(Files.size(written.toPath()) > 44, "WAV should contain header + audio data");
        assertFalse(VoiceRecorder.getInstance().isRecording());
    }

    @Test
    void startCreatesParentDir(@org.junit.jupiter.api.io.TempDir Path tmp) {
        // Skip when no mic — start() opens the line before creating the dir, so we'd
        // never reach the dir-creation path on a headless box.
        assumeTrue(AudioSystem.isLineSupported(new DataLine.Info(TargetDataLine.class, VoiceRecorder.FORMAT)),
                "no audio input device available in this environment");

        Path nested = tmp.resolve("a").resolve("b").resolve("c").resolve("rec.wav");
        boolean started = VoiceRecorder.getInstance().start(nested.toFile());
        try {
            assertTrue(started);
            assertTrue(Files.isDirectory(nested.getParent()));
        } finally {
            VoiceRecorder.getInstance().stop();
        }
    }
}
