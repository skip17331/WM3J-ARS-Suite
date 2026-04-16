package com.hamradio.modem.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class AudioEngine {

    private static final Logger log = LoggerFactory.getLogger(AudioEngine.class);

    public static final float SAMPLE_RATE = 8000.0f;
    public static final int FRAME_SIZE = 1024;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int CHANNELS = 1;

    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            SAMPLE_RATE,
            16,
            CHANNELS,
            true,
            false
    );

    public static final String DEFAULT_DEVICE_ID = "DEFAULT";

    private final List<Consumer<float[]>> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean running = false;
    private volatile String preferredInputDeviceId = DEFAULT_DEVICE_ID;

    private Thread audioThread;
    private TargetDataLine line;

    public void addListener(Consumer<float[]> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<float[]> listener) {
        listeners.remove(listener);
    }

    public boolean isRunning() {
        return running;
    }

    public String getPreferredInputDeviceId() {
        return preferredInputDeviceId;
    }

    public void setPreferredInputDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            this.preferredInputDeviceId = DEFAULT_DEVICE_ID;
        } else {
            this.preferredInputDeviceId = deviceId.trim();
        }
    }

    public List<AudioInputDevice> getAvailableInputDevices() {
        List<AudioInputDevice> devices = new ArrayList<>();
        devices.add(new AudioInputDevice(
                DEFAULT_DEVICE_ID,
                "Default Input Device",
                "System default Java input device"
        ));

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) {
                    String id = buildMixerId(mixerInfo);
                    String label = buildMixerLabel(mixerInfo);
                    devices.add(new AudioInputDevice(id, mixerInfo.getName(), label));
                }
            } catch (Exception e) {
                log.debug("Skipping mixer '{}': {}", mixerInfo.getName(), e.getMessage());
            }
        }

        return Collections.unmodifiableList(devices);
    }

    public synchronized void start() throws LineUnavailableException {
        if (running) {
            return;
        }

        line = openInputLine(preferredInputDeviceId);
        line.open(AUDIO_FORMAT, FRAME_SIZE * BYTES_PER_SAMPLE * 4);
        line.start();

        running = true;

        audioThread = new Thread(this::captureLoop, "audio-capture");
        audioThread.setDaemon(true);
        audioThread.start();

        log.info("Audio input started using '{}'", describeSelectedDevice(preferredInputDeviceId));
    }

    public synchronized void stop() {
        running = false;

        if (audioThread != null) {
            try {
                audioThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            audioThread = null;
        }

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

        log.info("Audio input stopped");
    }

    private void captureLoop() {
        byte[] buffer = new byte[FRAME_SIZE * BYTES_PER_SAMPLE];

        while (running) {
            try {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) {
                    continue;
                }

                int sampleCount = bytesRead / BYTES_PER_SAMPLE;
                float[] samples = new float[sampleCount];

                for (int i = 0; i < sampleCount; i++) {
                    int lo = buffer[i * 2] & 0xFF;
                    int hi = buffer[i * 2 + 1];
                    short pcm = (short) ((hi << 8) | lo);
                    samples[i] = pcm / 32768.0f;
                }

                for (Consumer<float[]> listener : listeners) {
                    try {
                        listener.accept(samples);
                    } catch (Exception e) {
                        log.warn("Audio listener failed: {}", e.getMessage(), e);
                    }
                }

            } catch (Exception e) {
                if (running) {
                    log.warn("Audio capture loop error: {}", e.getMessage(), e);
                }
                break;
            }
        }

        running = false;
    }

    private TargetDataLine openInputLine(String deviceId) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);

        if (deviceId == null
                || deviceId.isBlank()
                || DEFAULT_DEVICE_ID.equalsIgnoreCase(deviceId)) {
            return (TargetDataLine) AudioSystem.getLine(info);
        }

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            String candidateId = buildMixerId(mixerInfo);
            if (Objects.equals(candidateId, deviceId)) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (!mixer.isLineSupported(info)) {
                    throw new LineUnavailableException("Selected input device does not support required audio format: " + buildMixerLabel(mixerInfo));
                }
                return (TargetDataLine) mixer.getLine(info);
            }
        }

        throw new LineUnavailableException("Input device not found: " + deviceId);
    }

    private String describeSelectedDevice(String deviceId) {
        if (deviceId == null || DEFAULT_DEVICE_ID.equals(deviceId)) {
            return "Default Input Device";
        }

        for (AudioInputDevice device : getAvailableInputDevices()) {
            if (device.id().equals(deviceId)) {
                return device.displayLabel();
            }
        }

        return deviceId;
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

    public record AudioInputDevice(String id, String shortName, String displayLabel) {
        @Override
        public String toString() {
            return displayLabel;
        }
    }
}
