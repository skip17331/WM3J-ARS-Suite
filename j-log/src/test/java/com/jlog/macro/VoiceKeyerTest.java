package com.jlog.macro;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class VoiceKeyerTest {

    @Test
    void notPlayingByDefault() {
        // Singleton state may carry between tests; just ensure stop+isPlaying agree.
        VoiceKeyer.getInstance().stop();
        assertFalse(VoiceKeyer.getInstance().isPlaying());
    }

    @Test
    void stopIsIdempotent() {
        VoiceKeyer.getInstance().stop();
        VoiceKeyer.getInstance().stop();
        assertFalse(VoiceKeyer.getInstance().isPlaying());
    }

    @Test
    void playNullFileReturnsFalse() {
        assertFalse(VoiceKeyer.getInstance().play(null, null, null));
    }

    @Test
    void playMissingFileSetsErrorAndClearsState() throws Exception {
        // Non-existent path — playback thread should fail in getAudioInputStream
        // and the onError callback fires, leaving isPlaying() == false.
        File missing = new File("/tmp/this-file-should-not-exist-" + System.nanoTime() + ".wav");
        boolean[] errored = { false };
        boolean started = VoiceKeyer.getInstance().play(missing, null, e -> errored[0] = true);
        assertTrue(started, "play() should accept the call even if the file is bad");
        // Give the playback thread a moment to fail
        for (int i = 0; i < 50 && VoiceKeyer.getInstance().isPlaying(); i++) Thread.sleep(20);
        assertFalse(VoiceKeyer.getInstance().isPlaying(), "playback flag should reset after failure");
        assertTrue(errored[0], "onError should have been invoked");
    }

    @Test
    void playRealWavRoundTrip(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        // Skip when no playback device is available (e.g. headless CI box).
        AudioFormat fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050f, 16, 1, 2, 22050f, false);
        assumeTrue(AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, fmt)),
                "no audio output device available in this environment");

        // Generate ~100 ms of silence and write it to a real WAV file.
        int frames = (int) (fmt.getFrameRate() * 0.1);
        byte[] silence = new byte[frames * fmt.getFrameSize()];
        Path wav = tmp.resolve("silence.wav");
        try (AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(silence), fmt, frames)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wav.toFile());
        }
        assertTrue(Files.size(wav) > 0);

        boolean[] done = { false };
        boolean started = VoiceKeyer.getInstance().play(wav.toFile(), () -> done[0] = true, null);
        assertTrue(started);
        // Wait up to 2.5 s for playback to finish and the onDone callback to fire.
        for (int i = 0; i < 50 && !done[0]; i++) Thread.sleep(50);
        assertTrue(done[0], "onDone should fire when playback completes naturally");
        // onDone fires inside the try block; the playing flag clears in finally.
        // Wait briefly for the thread to fully unwind before asserting.
        for (int i = 0; i < 50 && VoiceKeyer.getInstance().isPlaying(); i++) Thread.sleep(20);
        assertFalse(VoiceKeyer.getInstance().isPlaying());
    }
}
