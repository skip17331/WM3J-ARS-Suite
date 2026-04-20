package com.hamradio.modem;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.dsp.FftAnalyzer;
import com.hamradio.modem.hub.HubClient;
import com.hamradio.modem.hub.HubMessageListener;
import com.hamradio.modem.mode.ModeManager;
import com.hamradio.modem.mode.RttyMode;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.HubMacro;
import com.hamradio.modem.model.HubRigStatus;
import com.hamradio.modem.model.HubSpot;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.ModemStatus;
import com.hamradio.modem.model.RotorStatus;
import com.hamradio.modem.model.SignalSnapshot;
import com.hamradio.modem.tx.AudioTxEngine;
import com.hamradio.modem.tx.CwTransmitter;
import com.hamradio.modem.tx.DigitalTransmitter;
import com.hamradio.modem.tx.DominoExTransmitter;
import com.hamradio.modem.tx.Mfsk16Transmitter;
import com.hamradio.modem.tx.OliviaTransmitter;
import com.hamradio.modem.tx.Psk31Transmitter;
import com.hamradio.modem.tx.RttyTransmitter;
import com.hamradio.modem.tx.TxState;
import com.hamradio.modem.tx.WavFileWriter;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class ModemService implements HubMessageListener {

    private static final Logger log = LoggerFactory.getLogger(ModemService.class);
    private static final Gson GSON = new Gson();

    private static final String DEFAULT_HUB_URL = "ws://127.0.0.1:8080";
    private static final ModeType DEFAULT_MODE = ModeType.RTTY;

    private static final int MAX_DECODE_HISTORY = 100;
    private static final long DUPLICATE_SUPPRESS_MS = 1500L;
    private static final long CONNECT_TIMEOUT_MS = 3000L;

    private static final Preferences PREFS = Preferences.userNodeForPackage(ModemService.class);
    private static final String PREF_HUB_URL     = "hub.url";
    private static final String PREF_MODE         = "modem.mode";
    private static final String PREF_RTTY_REVERSE = "rtty.reverse";
    private static final String PREF_AUDIO_INPUT  = "audio.input.device";
    private static final String PREF_AUDIO_OUTPUT = "audio.output.device";
    private static final String PREF_MY_CALL      = "station.callsign";

    private final AudioEngine audioEngine = new AudioEngine();
    private final AudioTxEngine audioTxEngine = new AudioTxEngine();
    private final FftAnalyzer fftAnalyzer =
            new FftAnalyzer(AudioEngine.FRAME_SIZE, AudioEngine.SAMPLE_RATE);
    private final ModeManager modeManager = new ModeManager();
    private final ModemStatus status = new ModemStatus();
    private final Deque<String> decodeHistory = new ArrayDeque<>();

    private final DigitalTransmitter rttyTransmitter   = new RttyTransmitter();
    private final DigitalTransmitter psk31Transmitter  = new Psk31Transmitter();
    private final DigitalTransmitter cwTransmitter     = new CwTransmitter();
    private final DigitalTransmitter oliviaTransmitter = new OliviaTransmitter();
    private final DigitalTransmitter mfsk16Transmitter   = new Mfsk16Transmitter();
    private final DigitalTransmitter dominoExTransmitter = new DominoExTransmitter();

    private Consumer<SignalSnapshot> spectrumListener     = s -> {};
    private Consumer<String>        decodeListener       = s -> {};
    private Consumer<ModemStatus>   statusListener       = s -> {};
    private Consumer<List<HubMacro>> macrosListener      = l -> {};
    private Consumer<HubSpot>       spotListener         = s -> {};
    private Consumer<HubSpot>       spotSelectedListener = s -> {};
    private Consumer<RotorStatus>   rotorListener        = r -> {};
    /** Fires on the FX thread when j-hub delivers station identity via JHUB_WELCOME. */
    private Consumer<String[]>      stationListener      = a -> {};  // [callsign, grid, tz]
    private Consumer<Integer>       fontSizeListener     = s -> {};

    private final List<HubMacro> macros = new ArrayList<>();
    private final List<HubSpot>  spots  = new ArrayList<>();
    private volatile double rotorBearing = -1;

    // Station identity — populated from j-hub JHUB_WELCOME; prefs used as fallback
    private volatile String hubCallsign  = null;
    private volatile String hubGridSquare = null;

    private HubClient hubClient;
    private boolean shuttingDown = false;

    private String lastPublishedDecodeLine = "";
    private long lastPublishedDecodeAtMs = 0L;

    private volatile CountDownLatch connectLatch;

    public ModemService() {
        status.setHubUrl(loadSavedHubUrl());
        status.setMode(loadSavedMode());
        status.setTxMode(status.getMode());
        status.setTxState(TxState.IDLE);
        status.setTxStatusText("Idle");

        boolean savedReverse = PREFS.getBoolean(PREF_RTTY_REVERSE, false);
        modeManager.getRttyMode().setReverse(savedReverse);
        status.setRttyReverse(savedReverse);

        String savedInputId = PREFS.get(PREF_AUDIO_INPUT, AudioEngine.DEFAULT_DEVICE_ID);
        audioEngine.setPreferredInputDeviceId(savedInputId);

        String savedOutputId = PREFS.get(PREF_AUDIO_OUTPUT, AudioTxEngine.DEFAULT_DEVICE_ID);
        audioTxEngine.setPreferredOutputDeviceId(savedOutputId);

        audioEngine.addListener(this::handleAudioFrame);
    }

    public void startup() throws Exception {
        connectHubBlocking(status.getHubUrl());
        startAudio();
    }

    /**
     * Thread-safe variant of {@link #startup()} that swallows exceptions and
     * reports failures to the decode log. Intended to be called from a background
     * thread immediately after the main window is shown.
     */
    public void startupSafe() {
        try {
            startup();
        } catch (Exception e) {
            postDecodeLine("Hub connection failed: " + e.getMessage() + " — use Setup to reconnect");
            log.warn("Startup failed: {}", e.getMessage());
        }
    }

    public void shutdown() {
        shuttingDown = true;
        cancelTransmit();
        stopAudio();
        disconnectHub();
    }

    public ModemStatus getStatus() {
        return status;
    }

    public void setSpectrumListener(Consumer<SignalSnapshot> spectrumListener) {
        this.spectrumListener = spectrumListener != null ? spectrumListener : s -> {};
    }

    public void setDecodeListener(Consumer<String> decodeListener) {
        this.decodeListener = decodeListener != null ? decodeListener : s -> {};
    }

    public void setStatusListener(Consumer<ModemStatus> statusListener) {
        this.statusListener = statusListener != null ? statusListener : s -> {};
    }

    public void setMacrosListener(Consumer<List<HubMacro>> l)    { this.macrosListener      = l != null ? l : list -> {}; }
    public void setSpotListener(Consumer<HubSpot> l)            { this.spotListener         = l != null ? l : s -> {}; }
    public void setSpotSelectedListener(Consumer<HubSpot> l)    { this.spotSelectedListener = l != null ? l : s -> {}; }
    public void setRotorListener(Consumer<RotorStatus> l)       { this.rotorListener        = l != null ? l : r -> {}; }
    public void setStationListener(Consumer<String[]> l)        { this.stationListener      = l != null ? l : a -> {}; }
    public void setFontSizeListener(Consumer<Integer> l)        { this.fontSizeListener     = l != null ? l : s -> {}; }

    public List<HubMacro> getMacros()    { return Collections.unmodifiableList(macros); }
    public List<HubSpot>  getSpots()     { return Collections.unmodifiableList(spots);  }
    public double getRotorBearing()      { return rotorBearing; }
    /** Returns callsign from j-hub config if available, otherwise falls back to local prefs. */
    public String getMyCall() {
        return hubCallsign != null && !hubCallsign.isBlank()
               ? hubCallsign
               : PREFS.get(PREF_MY_CALL, "NOCALL");
    }
    public String getMyGrid() { return hubGridSquare != null ? hubGridSquare : ""; }
    public void   setMyCall(String call) {
        PREFS.put(PREF_MY_CALL, call != null ? call.trim().toUpperCase() : "NOCALL");
        flushPrefsQuietly("myCall");
    }
    public void transmitMacro(String text) { transmitText(text); }

    public List<AudioEngine.AudioInputDevice> getAvailableAudioInputDevices() {
        return audioEngine.getAvailableInputDevices();
    }

    public String getSelectedAudioInputDevice() {
        return audioEngine.getPreferredInputDeviceId();
    }

    public void setAudioInputDevice(String deviceId) {
        String selected = (deviceId == null || deviceId.isBlank())
                ? AudioEngine.DEFAULT_DEVICE_ID
                : deviceId.trim();

        boolean wasRunning = audioEngine.isRunning();

        if (wasRunning) {
            stopAudio();
        }

        audioEngine.setPreferredInputDeviceId(selected);
        PREFS.put(PREF_AUDIO_INPUT, selected);
        flushPrefsQuietly("audio input device");

        postDecodeLine("Audio input set to: " + selected);

        if (wasRunning) {
            try {
                startAudio();
            } catch (LineUnavailableException e) {
                postDecodeLine("Failed to restart audio: " + e.getMessage());
            }
        }
    }

    public List<AudioTxEngine.AudioOutputDevice> getAvailableAudioOutputDevices() {
        return audioTxEngine.getAvailableOutputDevices();
    }

    public String getSelectedAudioOutputDevice() {
        return audioTxEngine.getPreferredOutputDeviceId();
    }

    public void setAudioOutputDevice(String deviceId) {
        if (audioTxEngine.isTransmitting() || status.isTransmitting()) {
            postDecodeLine("Cannot change TX output device while transmitting");
            return;
        }

        String selected = (deviceId == null || deviceId.isBlank())
                ? AudioTxEngine.DEFAULT_DEVICE_ID
                : deviceId.trim();

        audioTxEngine.setPreferredOutputDeviceId(selected);
        PREFS.put(PREF_AUDIO_OUTPUT, selected);
        flushPrefsQuietly("audio output device");

        postDecodeLine("TX output set to: " + selected);
    }

    public void connectHubBlocking(String url) throws Exception {
        String trimmed = normalizeHubUrl(url);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Hub URL cannot be blank");
        }

        disconnectHub();

        status.setHubUrl(trimmed);
        fireStatus();

        connectLatch = new CountDownLatch(1);
        hubClient = new HubClient(new URI(trimmed), "j-digi", "0.1.0", this);
        hubClient.connect();

        boolean connected = connectLatch.await(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!connected || !status.isHubConnected()) {
            disconnectHub();
            throw new IllegalStateException("Could not connect to hub at " + trimmed);
        }

        saveHubUrl(trimmed);
    }

    public void reconnectHubFromSetup(String url) {
        try {
            connectHubBlocking(url);
            postDecodeLine("Hub connected: " + status.getHubUrl());
        } catch (Exception ex) {
            postDecodeLine("Reconnect failed: " + ex.getMessage());
        }
    }

    public void disconnectHub() {
        if (hubClient != null) {
            try {
                hubClient.close();
            } catch (Exception ignored) {
            } finally {
                hubClient = null;
            }
        }

        status.setHubConnected(false);
        fireStatus();
    }

    public void startAudio() throws LineUnavailableException {
        if (audioEngine.isRunning()) {
            status.setAudioRunning(true);
            fireStatus();
            return;
        }

        audioEngine.start();
        status.setAudioRunning(true);
        fireStatus();
        postDecodeLine("Audio started");
    }

    public void stopAudio() {
        if (!audioEngine.isRunning()) {
            status.setAudioRunning(false);
            fireStatus();
            return;
        }

        audioEngine.stop();
        status.setAudioRunning(false);
        fireStatus();
        postDecodeLine("Audio stopped");
    }

    public void setMode(ModeType mode) {
        if (mode == null) {
            return;
        }

        if (status.getMode() != mode) {
            status.setMode(mode);
            saveMode(mode);

            if (!status.isTransmitting()) {
                status.setTxMode(mode);
                if (status.getTxState() == null || status.getTxState() == TxState.IDLE) {
                    status.setTxStatusText("Idle");
                }
            }

            fireStatus();
            postDecodeLine("Mode changed to " + mode);
        }
    }

    public void setRttyReverse(boolean reverse) {
        RttyMode rtty = modeManager.getRttyMode();
        rtty.setReverse(reverse);
        status.setRttyReverse(reverse);
        PREFS.putBoolean(PREF_RTTY_REVERSE, reverse);
        flushPrefsQuietly("RTTY reverse");
        fireStatus();
        postDecodeLine("RTTY reverse " + (reverse ? "enabled" : "disabled"));
    }

    public String getHubUrl() {
        return status.getHubUrl();
    }

    public boolean isTransmitting() {
        return status.isTransmitting();
    }

    public void transmitText(String text) {
        if (text == null || text.isBlank()) {
            postDecodeLine("Cannot transmit: no text entered");
            return;
        }

        if (audioTxEngine.isTransmitting() || status.isTransmitting()) {
            postDecodeLine("Cannot transmit: transmitter busy");
            return;
        }

        ModeType txMode = status.getMode();
        DigitalTransmitter transmitter = transmitterForMode(txMode);

        if (transmitter == null) {
            postDecodeLine("Transmit not implemented yet for mode: " + txMode);
            return;
        }

        status.setTxMode(txMode);
        status.setTxTextPreview(buildTxPreview(text));
        status.setTxState(TxState.STARTING);
        status.setTxStatusText("Starting " + txMode + " TX");
        status.setTransmitting(true);
        fireStatus();

        try {
            audioTxEngine.transmit(
                    transmitter.createSampleSource(text),
                    () -> Platform.runLater(() -> {
                        status.setTxState(TxState.TRANSMITTING);
                        status.setTxStatusText(txMode + " TX active");
                        fireStatus();
                        postDecodeLine("TX started: " + txMode);
                    }),
                    () -> Platform.runLater(() -> {
                        status.setTxState(TxState.COMPLETE);
                        status.setTxStatusText("TX complete");
                        status.setTransmitting(false);
                        fireStatus();
                        postDecodeLine("TX complete: " + txMode);
                    }),
                    () -> Platform.runLater(() -> {
                        status.setTxState(TxState.CANCELLED);
                        status.setTxStatusText("TX cancelled");
                        status.setTransmitting(false);
                        fireStatus();
                        postDecodeLine("TX cancelled");
                    }),
                    error -> Platform.runLater(() -> {
                        status.setTxState(TxState.ERROR);
                        status.setTxStatusText("TX error: " + error);
                        status.setTransmitting(false);
                        fireStatus();
                        postDecodeLine("TX error: " + error);
                    })
            );
        } catch (Exception e) {
            status.setTxState(TxState.ERROR);
            status.setTxStatusText("TX error: " + e.getMessage());
            status.setTransmitting(false);
            fireStatus();
            postDecodeLine("TX start failed: " + e.getMessage());
        }
    }

    public void saveTransmitWav(String text, Path path) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("No TX text entered");
        }
        if (path == null) {
            throw new IllegalArgumentException("No output path selected");
        }
        if (status.isTransmitting() || audioTxEngine.isTransmitting()) {
            throw new IllegalStateException("Cannot save WAV while transmitter is active");
        }

        ModeType txMode = status.getMode();
        DigitalTransmitter transmitter = transmitterForMode(txMode);

        if (transmitter == null) {
            throw new UnsupportedOperationException("WAV export not implemented yet for mode: " + txMode);
        }

        status.setTxMode(txMode);
        status.setTxTextPreview(buildTxPreview(text));
        status.setTxState(TxState.STARTING);
        status.setTxStatusText("Saving " + txMode + " TX WAV");
        fireStatus();

        try {
            WavFileWriter.writeMono16(
                    path,
                    AudioEngine.SAMPLE_RATE,
                    transmitter.createSampleSource(text)
            );

            status.setTxState(TxState.COMPLETE);
            status.setTxStatusText("TX WAV saved");
            status.setTransmitting(false);
            fireStatus();

            postDecodeLine("TX WAV saved: " + path.toAbsolutePath());
        } catch (Exception e) {
            status.setTxState(TxState.ERROR);
            status.setTxStatusText("WAV save error: " + e.getMessage());
            status.setTransmitting(false);
            fireStatus();

            postDecodeLine("TX WAV save failed: " + e.getMessage());
            throw e;
        }
    }

    public void cancelTransmit() {
        if (!audioTxEngine.isTransmitting() && !status.isTransmitting()) {
            return;
        }

        status.setTxState(TxState.STOPPING);
        status.setTxStatusText("Stopping TX");
        fireStatus();

        audioTxEngine.cancel();
    }

    public void sendLogDraft(String callsign,
                             String mode,
                             String band,
                             long frequency,
                             String rstSent,
                             String rstReceived,
                             String exchange,
                             String notes,
                             double confidence) {

        if (hubClient == null || !hubClient.isOpen()) {
            postDecodeLine("Cannot send log draft: hub not connected");
            return;
        }

        try {
            hubClient.sendLogDraft(
                    callsign,
                    mode,
                    band,
                    frequency,
                    rstSent,
                    rstReceived,
                    exchange,
                    notes,
                    confidence
            );

            postDecodeLine("LOG_ENTRY_DRAFT sent: "
                    + (callsign == null || callsign.isBlank() ? "<no call>" : callsign));

        } catch (Exception e) {
            log.warn("Failed to send log draft: {}", e.getMessage(), e);
            postDecodeLine("Failed to send log draft: " + e.getMessage());
        }
    }

    private DigitalTransmitter transmitterForMode(ModeType mode) {
        if (mode == null) {
            return null;
        }

        return switch (mode) {
            case RTTY   -> rttyTransmitter;
            case PSK31  -> psk31Transmitter;
            case CW     -> cwTransmitter;
            case OLIVIA -> oliviaTransmitter;
            case MFSK16   -> mfsk16Transmitter;
            case DOMINOEX -> dominoExTransmitter;
            case AX25     -> null;
        };
    }

    private String loadSavedHubUrl() {
        String saved = PREFS.get(PREF_HUB_URL, DEFAULT_HUB_URL);
        return normalizeHubUrl(saved);
    }

    private void saveHubUrl(String url) {
        String normalized = normalizeHubUrl(url);
        if (normalized.isEmpty()) {
            return;
        }

        PREFS.put(PREF_HUB_URL, normalized);
        flushPrefsQuietly("hub URL");
    }

    private ModeType loadSavedMode() {
        String saved = PREFS.get(PREF_MODE, DEFAULT_MODE.name());
        try {
            return ModeType.valueOf(saved);
        } catch (Exception e) {
            log.warn("Invalid saved mode '{}', using default {}", saved, DEFAULT_MODE);
            return DEFAULT_MODE;
        }
    }

    private void saveMode(ModeType mode) {
        if (mode == null) {
            return;
        }

        PREFS.put(PREF_MODE, mode.name());
        flushPrefsQuietly("mode");
    }

    private void flushPrefsQuietly(String what) {
        try {
            PREFS.flush();
        } catch (Exception e) {
            log.warn("Failed to persist {}: {}", what, e.getMessage());
        }
    }

    private String normalizeHubUrl(String url) {
        if (url == null) {
            return DEFAULT_HUB_URL;
        }

        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_HUB_URL;
        }

        return trimmed;
    }

    private void handleAudioFrame(float[] samples) {
        if (status.isTransmitting()) {
            return;
        }

        try {
            FftAnalyzer.SpectrumResult result = fftAnalyzer.analyze(samples);

            SignalSnapshot snapshot = new SignalSnapshot(
                    samples,
                    result.magnitudes(),
                    result.rms(),
                    result.peakFrequencyHz(),
                    AudioEngine.SAMPLE_RATE
            );

            status.setRms(result.rms());
            status.setPeakFrequencyHz(result.peakFrequencyHz());
            status.setSnr(result.snrDb());

            updateModeDiagnostics();

            Platform.runLater(() -> {
                spectrumListener.accept(snapshot);
                fireStatus();
            });

            Optional<DecodeMessage> maybe =
                    modeManager.process(status.getMode(), snapshot, status.getRigFrequencyHz());

            updateModeDiagnostics();

            maybe.ifPresent(this::publishDecode);

        } catch (Exception e) {
            log.warn("Audio frame processing failed: {}", e.getMessage(), e);
            Platform.runLater(() ->
                    decodeListener.accept("Audio processing error: " + e.getMessage()));
        }
    }

    private void updateModeDiagnostics() {
        if (status.getMode() == ModeType.RTTY) {
            RttyMode rtty = modeManager.getRttyMode();
            status.setRttyReverse(rtty.isReverse());
            status.setRttyMarkPower(rtty.getLastMarkPower());
            status.setRttySpacePower(rtty.getLastSpacePower());
            status.setRttyDominance(rtty.getLastDominance());
        } else {
            status.setRttyMarkPower(0.0);
            status.setRttySpacePower(0.0);
            status.setRttyDominance(0.0);
        }
    }

    private void publishDecode(DecodeMessage decode) {
        String line = "%s | %s | %.1f Hz | SNR %.1f dB | conf %.2f".formatted(
                decode.getMode(),
                decode.getText(),
                decode.getOffsetHz(),
                decode.getSnr(),
                decode.getConfidence()
        );

        long now = System.currentTimeMillis();
        boolean duplicate = line.equals(lastPublishedDecodeLine)
                && (now - lastPublishedDecodeAtMs) < DUPLICATE_SUPPRESS_MS;

        if (duplicate) {
            return;
        }

        lastPublishedDecodeLine = line;
        lastPublishedDecodeAtMs = now;

        if (decodeHistory.size() >= MAX_DECODE_HISTORY) {
            decodeHistory.removeFirst();
        }
        decodeHistory.addLast(line);

        Platform.runLater(() -> decodeListener.accept(line));

        if (hubClient != null && hubClient.isOpen()) {
            try {
                hubClient.sendDecode(
                        decode.getMode().name(),
                        decode.getText(),
                        decode.getFrequencyHz(),
                        decode.getOffsetHz(),
                        decode.getSnr(),
                        decode.getConfidence()
                );
            } catch (Exception e) {
                log.warn("Failed to send decode to hub: {}", e.getMessage());
            }
        }
    }

    private void postDecodeLine(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        if (decodeHistory.size() >= MAX_DECODE_HISTORY) {
            decodeHistory.removeFirst();
        }
        decodeHistory.addLast(text);

        Platform.runLater(() -> decodeListener.accept(text));
    }

    private String buildTxPreview(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80) + "...";
    }

    private void fireStatus() {
        statusListener.accept(status);
    }

    @Override
    public void onConnected() {
        status.setHubConnected(true);
        fireStatus();

        CountDownLatch latch = connectLatch;
        if (latch != null) {
            latch.countDown();
        }

        Platform.runLater(() -> decodeListener.accept("Hub connected"));
    }

    @Override
    public void onDisconnected() {
        status.setHubConnected(false);
        fireStatus();

        CountDownLatch latch = connectLatch;
        if (latch != null) {
            latch.countDown();
        }

        if (!shuttingDown) {
            Platform.runLater(() -> decodeListener.accept("Hub disconnected - waiting for reconnect"));
        }
    }

    @Override
    public void onMessage(JsonObject message) {
        Platform.runLater(() -> handleHubMessage(message));
    }

    private void handleHubMessage(JsonObject msg) {
        String type = msg.has("type") ? msg.get("type").getAsString() : "";

        switch (type) {
            case "JHUB_WELCOME" -> {
                // Extract global station config from j-hub so we don't need our own copy
                if (msg.has("station")) {
                    com.google.gson.JsonObject st = msg.getAsJsonObject("station");
                    if (st.has("callsign")) {
                        String call = st.get("callsign").getAsString().trim().toUpperCase();
                        if (!call.isBlank()) hubCallsign = call;
                    }
                    if (st.has("gridSquare"))
                        hubGridSquare = st.get("gridSquare").getAsString().trim().toUpperCase();
                    String tz = st.has("timezone") ? st.get("timezone").getAsString() : "UTC";
                    stationListener.accept(new String[]{ getMyCall(), getMyGrid(), tz });
                }
                decodeListener.accept("Connected to J-Hub — station: " + getMyCall());
            }

            case "RIG_STATUS" -> {
                HubRigStatus rig = GSON.fromJson(msg, HubRigStatus.class);
                status.setRigFrequencyHz(rig.frequency);
                status.setRigMode(rig.mode != null ? rig.mode : "");
                fireStatus();
            }

            case "SPOT" -> {
                HubSpot spot = GSON.fromJson(msg, HubSpot.class);
                if (spots.size() >= 200) spots.remove(spots.size() - 1);
                spots.add(0, spot);
                spotListener.accept(spot);
            }

            case "SPOT_SELECTED" -> {
                HubSpot spot = GSON.fromJson(msg, HubSpot.class);
                if (msg.has("frequency")) {
                    status.setRigFrequencyHz(msg.get("frequency").getAsLong());
                }
                if (msg.has("mode")) {
                    status.setRigMode(msg.get("mode").getAsString());
                }
                fireStatus();
                spotSelectedListener.accept(spot);
            }

            case "MACRO_LIST" -> {
                macros.clear();
                if (msg.has("macros")) {
                    com.google.gson.reflect.TypeToken<List<HubMacro>> token =
                        new com.google.gson.reflect.TypeToken<>(){};
                    List<HubMacro> received = GSON.fromJson(msg.getAsJsonArray("macros"), token);
                    if (received != null) macros.addAll(received);
                }
                macrosListener.accept(Collections.unmodifiableList(macros));
            }

            case "ROTOR_STATUS" -> {
                RotorStatus rotor = GSON.fromJson(msg, RotorStatus.class);
                rotorBearing = rotor.bearing;
                rotorListener.accept(rotor);
            }

            case "APP_LIST" -> {} // no-op

            case "CONFIG_UPDATE" -> {
                if (msg.has("settings")) {
                    com.google.gson.JsonObject settings = msg.getAsJsonObject("settings");
                    if (settings.has("fontSize")) {
                        fontSizeListener.accept(settings.get("fontSize").getAsInt());
                    }
                }
            }

            case "SHUTDOWN" -> {
                // J-Hub is shutting down — exit cleanly
                log.info("SHUTDOWN command received from j-hub — exiting");
                postDecodeLine("Shutdown command received from j-hub — closing");
                Platform.runLater(Platform::exit);
            }

            default -> {
                // ignore
            }
        }
    }

    @Override
    public void onError(String errorMessage) {
        String safeMessage = (errorMessage == null || errorMessage.isBlank())
                ? "unknown error"
                : errorMessage;

        Platform.runLater(() -> decodeListener.accept("Hub error: " + safeMessage));
        log.warn("Hub error: {}", safeMessage);

        CountDownLatch latch = connectLatch;
        if (latch != null) {
            latch.countDown();
        }
    }
}
