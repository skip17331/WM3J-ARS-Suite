package com.hamradio.modem.tx;

import com.hamradio.modem.audio.AudioEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioTxEngine {

    private static final Logger log = LoggerFactory.getLogger(AudioTxEngine.class);

    public static final String DEFAULT_DEVICE_ID = "DEFAULT";

    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            AudioEngine.SAMPLE_RATE,
            16,
            AudioEngine.CHANNELS,
            true,
            false
    );

    private static final int BUFFER_SAMPLES = 1024;
    private static final int BYTES_PER_SAMPLE = 2;

    private final RigControl rigControl;

    private volatile boolean transmitting;
    private volatile boolean cancelRequested;
    private volatile String lastError = "";
    private volatile String preferredOutputDeviceId = DEFAULT_DEVICE_ID;

    private Thread txThread;
    private SourceDataLine line;

    public AudioTxEngine() {
        this(new NoOpRigControl());
    }

    public AudioTxEngine(RigControl rigControl) {
        this.rigControl = Objects.requireNonNull(rigControl, "rigControl");
    }

    public boolean isTransmitting() {
        return transmitting;
    }

    public String getLastError() {
        return lastError;
    }

    public String getPreferredOutputDeviceId() {
        return preferredOutputDeviceId;
    }

    public void setPreferredOutputDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            this.preferredOutputDeviceId = DEFAULT_DEVICE_ID;
        } else {
            this.preferredOutputDeviceId = deviceId.trim();
        }
    }

    public List<AudioOutputDevice> getAvailableOutputDevices() {
        List<AudioOutputDevice> devices = new ArrayList<>();
        devices.add(new AudioOutputDevice(
                DEFAULT_DEVICE_ID,
                "Default Output Device",
                "System default Java output device"
        ));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) {
                    String id = buildMixerId(mixerInfo);
                    String label = buildMixerLabel(mixerInfo);
                    devices.add(new AudioOutputDevice(id, mixerInfo.getName(), label));
                }
            } catch (Exception e) {
                log.debug("Skipping output mixer '{}': {}", mixerInfo.getName(), e.getMessage());
            }
        }

        return Collections.unmodifiableList(devices);
    }

    public synchronized void transmit(SampleSource source,
                                      Runnable onStarted,
                                      Runnable onCompleted,
                                      Runnable onCancelled,
                                      java.util.function.Consumer<String> onError) {
        if (source == null) {
            throw new IllegalArgumentException("SampleSource cannot be null");
        }
        if (transmitting) {
            throw new IllegalStateException("Transmit already in progress");
        }

        cancelRequested = false;
        lastError = "";
        transmitting = true;

        txThread = new Thread(
                () -> runTransmit(source, onStarted, onCompleted, onCancelled, onError),
                "audio-tx"
        );
        txThread.setDaemon(true);
        txThread.start();
    }

    public synchronized void cancel() {
        if (!transmitting) {
            return;
        }

        cancelRequested = true;

        if (line != null) {
            try {
                line.stop();
            } catch (Exception ignored) {
            }
            try {
                line.flush();
            } catch (Exception ignored) {
            }
        }
    }

    private void runTransmit(SampleSource source,
                             Runnable onStarted,
                             Runnable onCompleted,
                             Runnable onCancelled,
                             java.util.function.Consumer<String> onError) {
        AtomicBoolean started = new AtomicBoolean(false);

        try {
            rigControl.setPtt(true);
            sleepQuietly(50L);

            line = openOutputLine(preferredOutputDeviceId);
            line.open(AUDIO_FORMAT, BUFFER_SAMPLES * BYTES_PER_SAMPLE * 4);
            line.start();

            started.set(true);
            if (onStarted != null) {
                onStarted.run();
            }

            log.info("TX audio output started using '{}'", describeSelectedDevice(preferredOutputDeviceId));

            float[] floatBuffer = new float[BUFFER_SAMPLES];
            byte[] pcmBuffer = new byte[BUFFER_SAMPLES * BYTES_PER_SAMPLE];

            while (!cancelRequested && !source.isFinished()) {
                int read = source.read(floatBuffer, 0, floatBuffer.length);
                if (read <= 0) {
                    break;
                }

                encodePcm16(floatBuffer, read, pcmBuffer);
                line.write(pcmBuffer, 0, read * BYTES_PER_SAMPLE);
            }

            if (!cancelRequested) {
                line.drain();
            }

            sleepQuietly(50L);
            rigControl.setPtt(false);

            if (cancelRequested) {
                if (onCancelled != null) {
                    onCancelled.run();
                }
            } else {
                if (onCompleted != null) {
                    onCompleted.run();
                }
            }

        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Transmit failed", e);

            try {
                rigControl.setPtt(false);
            } catch (Exception ignored) {
            }

            if (onError != null) {
                onError.accept(lastError);
            }
        } finally {
            try {
                source.close();
            } catch (Exception ignored) {
            }

            closeLine();

            transmitting = false;
            cancelRequested = false;

            if (!started.get()) {
                try {
                    rigControl.setPtt(false);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private SourceDataLine openOutputLine(String deviceId) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);

        if (deviceId == null
                || deviceId.isBlank()
                || DEFAULT_DEVICE_ID.equalsIgnoreCase(deviceId)) {
            return (SourceDataLine) AudioSystem.getLine(info);
        }

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            String candidateId = buildMixerId(mixerInfo);
            if (Objects.equals(candidateId, deviceId)) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (!mixer.isLineSupported(info)) {
                    throw new LineUnavailableException(
                            "Selected output device does not support required audio format: "
                                    + buildMixerLabel(mixerInfo)
                    );
                }
                return (SourceDataLine) mixer.getLine(info);
            }
        }

        throw new LineUnavailableException("Output device not found: " + deviceId);
    }

    private String describeSelectedDevice(String deviceId) {
        if (deviceId == null || DEFAULT_DEVICE_ID.equals(deviceId)) {
            return "Default Output Device";
        }

        for (AudioOutputDevice device : getAvailableOutputDevices()) {
            if (device.id().equals(deviceId)) {
                return device.displayLabel();
            }
        }

        return deviceId;
    }

    private void closeLine() {
        if (line != null) {
            try {
                line.stop();
            } catch (Exception ignored) {
            }
            try {
                line.flush();
            } catch (Exception ignored) {
            }
            try {
                line.close();
            } catch (Exception ignored) {
            }
            line = null;
        }
    }

    private void encodePcm16(float[] samples, int count, byte[] pcm) {
        for (int i = 0; i < count; i++) {
            float s = samples[i];
            if (s > 1.0f) s = 1.0f;
            if (s < -1.0f) s = -1.0f;

            short value = (short) Math.round(s * 32767.0);
            pcm[i * 2] = (byte) (value & 0xFF);
            pcm[i * 2 + 1] = (byte) ((value >>> 8) & 0xFF);
        }
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String buildMixerId(Mixer.Info info) {
        return info.getName() + " | " + info.getDescription() + " | " + info.getVendor() + " | " + info.getVersion();
    }

    private static String buildMixerLabel(Mixer.Info info) {
        return info.getName()
                + " — "
                + info.getDescription()
                + " — "
                + info.getVendor();
    }

    public record AudioOutputDevice(String id, String shortName, String displayLabel) {
        @Override
        public String toString() {
            return displayLabel;
        }
    }
}
