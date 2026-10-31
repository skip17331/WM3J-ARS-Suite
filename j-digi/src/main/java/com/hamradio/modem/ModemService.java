package com.hamradio.modem;

import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.dsp.FftAnalyzer;
import com.hamradio.modem.mode.ModeManager;
import com.hamradio.modem.mode.RttyMode;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.ModemStatus;
import com.hamradio.modem.model.SignalSnapshot;
import com.hamradio.modem.tx.AudioTxEngine;
import com.hamradio.modem.tx.CwTransmitter;
import com.hamradio.modem.tx.DigitalTransmitter;
import com.hamradio.modem.tx.DominoExTransmitter;
import com.hamradio.modem.tx.HamlibCwKeyer;
import com.jlog.bandplan.BandplanLoader;
import com.hamradio.modem.tx.HamlibRigControl;
import com.hamradio.modem.tx.Mfsk16Transmitter;
import com.hamradio.modem.tx.NoOpRigControl;
import com.hamradio.modem.tx.OliviaTransmitter;
import com.hamradio.modem.tx.Psk31Transmitter;
import com.hamradio.modem.tx.RigControl;
import com.hamradio.modem.tx.RttyTransmitter;
import com.hamradio.modem.tx.TxState;
import com.hamradio.modem.tx.WavFileWriter;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class ModemService {

    private static final Logger log = LoggerFactory.getLogger(ModemService.class);

    private static final ModeType DEFAULT_MODE = ModeType.RTTY;

    private static final int MAX_DECODE_HISTORY = 100;
    private static final long DUPLICATE_SUPPRESS_MS = 1500L;

    private static final Preferences PREFS = Preferences.userNodeForPackage(ModemService.class);
    private static final String PREF_MODE         = "modem.mode";
    private static final String PREF_RTTY_REVERSE = "rtty.reverse";
    private static final String PREF_AUDIO_INPUT  = "audio.input.device";
    private static final String PREF_AUDIO_OUTPUT = "audio.output.device";
    private static final String PREF_PTT_METHOD   = "ptt.method";          // VOX | HAMLIB
    private static final String PREF_PTT_HAMLIB_HOST = "ptt.hamlib.host";
    private static final String PREF_PTT_HAMLIB_PORT = "ptt.hamlib.port";
    private static final String PREF_CW_KEYER     = "cw.keyer";            // AUDIO | HAMLIB
    private static final String PREF_CW_WPM       = "cw.wpm";
    private static final String PREF_LOCAL_SKIMMER   = "skimmer.local.enabled";

    private final AudioEngine audioEngine = new AudioEngine();
    private final AudioTxEngine audioTxEngine = new AudioTxEngine();
    private final FftAnalyzer fftAnalyzer =
            new FftAnalyzer(AudioEngine.FRAME_SIZE, AudioEngine.SAMPLE_RATE);
    /** Multi-channel CW detector. Off by default; toggled by
     *  the "Local CW Skimmer" setting. */
    private final com.hamradio.modem.dsp.LocalSkimmer localSkimmer =
            new com.hamradio.modem.dsp.LocalSkimmer(AudioEngine.FRAME_SIZE, AudioEngine.SAMPLE_RATE);
    /** Phase A multi-channel CW decoder — runs one {@code CwMode} per
     *  skimmer-detected carrier. Phase B added callsign scoring;
     *  Phase C wires those promotions to the SPOT publisher (instantiated
     *  further down once {@code status} is available). */
    private final com.hamradio.modem.dsp.MultiCarrierDecoder multiCarrierDecoder =
            new com.hamradio.modem.dsp.MultiCarrierDecoder(AudioEngine.SAMPLE_RATE);
    /** Latest skimmer snapshot, refreshed every audio frame so the
     *  multi-channel decoder can drive its lifecycle off it. */
    private volatile com.hamradio.modem.dsp.LocalSkimmer.Snapshot lastSkimmerSnapshot;
    /** Click-to-identify classifier — buffers ~16 s of audio, classifies on demand. */
    private final com.hamradio.modem.dsp.SignalClassifier signalClassifier =
            new com.hamradio.modem.dsp.SignalClassifier(AudioEngine.SAMPLE_RATE, AudioEngine.FRAME_SIZE);
    private final ModeManager modeManager = new ModeManager();
    private final ModemStatus status = new ModemStatus();
    private final Deque<String> decodeHistory = new ArrayDeque<>();

    private final DigitalTransmitter rttyTransmitter   = new RttyTransmitter();
    private final DigitalTransmitter psk31Transmitter  = new Psk31Transmitter();
    // CW transmitter is built per-transmit from the cw.wpm pref —
    // see buildCwTransmitterFromPrefs() below.
    private final DigitalTransmitter oliviaTransmitter = new OliviaTransmitter();
    private final DigitalTransmitter mfsk16Transmitter   = new Mfsk16Transmitter();
    private final DigitalTransmitter dominoExTransmitter = new DominoExTransmitter();

    private Consumer<SignalSnapshot> spectrumListener     = s -> {};
    private Consumer<String>        decodeListener       = s -> {};
    private Consumer<ModemStatus>   statusListener       = s -> {};

    private String lastPublishedDecodeLine = "";
    private long lastPublishedDecodeAtMs = 0L;

    public ModemService() {
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

        audioTxEngine.setRigControl(buildConfiguredRigControl());

        audioEngine.addListener(this::handleAudioFrame);
        // The classifier runs alongside the active mode decoder — it never
        // demodulates, just buffers samples for on-demand mode identification
        // when the operator clicks a signal in the waterfall.
        audioEngine.addListener(signalClassifier);

        // Honor the saved local-skimmer toggle so the operator's choice
        // survives restarts.
        localSkimmer.setEnabled(PREFS.getBoolean(PREF_LOCAL_SKIMMER, false));
        localSkimmer.setListener(snap -> {
            lastSkimmerSnapshot = snap;         // freshest snapshot, every frame
        });
    }

    /** Click-to-identify entry point: invoked by the UI when the operator
     *  clicks a signal in the waterfall. Returns the best-guess mode +
     *  confidence + bandwidth. Cheap; safe to call from the FX thread. */
    public com.hamradio.modem.dsp.SignalClassifier.Result classifySignal(double centerHz) {
        return signalClassifier.classify(centerHz);
    }

    /** Start audio capture; a failure is reported to the decode log rather than thrown. */
    public void startupEmbedded() {
        try {
            startAudio();
        } catch (Exception e) {
            postDecodeLine("Audio start failed: " + e.getMessage());
            log.warn("Embedded startup failed: {}", e.getMessage());
        }
    }

    public void shutdown() {
        cancelTransmit();
        stopAudio();
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

    /**
     * Tell the modem where the rig is tuned. The host calls this whenever its rig state changes; listeners are only
     * notified when the frequency or mode actually changed, so it is cheap to call on every poll.
     *
     * @param frequencyHz dial frequency in Hz, or 0 when the rig is not connected
     * @param mode        the rig's mode name (e.g. {@code USB}), or null
     */
    public void setRigState(long frequencyHz, String mode) {
        long hz = Math.max(0L, frequencyHz);
        String m = mode == null ? "" : mode;
        if (hz == status.getRigFrequencyHz() && m.equals(status.getRigMode())) return;
        status.setRigFrequencyHz(hz);
        status.setRigMode(m);
        status.setBandplanCaption(captionFor(hz));
        fireStatus();
    }

    /** Convert current rig frequency to ham band string (e.g. "20m"). Empty if unrecognised. */
    public String bandFromRigHz() {
        String b = com.jlog.bandplan.BandplanLoader.getInstance().bandName(status.getRigFrequencyHz());   // the shared band plan
        return b == null ? "" : b;
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

    /**
     * Set how CW is sent, from the host application's settings; saved and used from the next CW transmission.
     *
     * @param keyer {@code AUDIO} (the modem makes the tone and keys the rig with the PTT method) or {@code HAMLIB}
     *              (the text goes to the rig's own keyer over rigctld); blank keeps the saved choice
     * @param wpm   words per minute for both paths, clamped to 5–60; 0 or less keeps the saved speed
     */
    public void applyCwSettings(String keyer, int wpm) {
        if (keyer != null && !keyer.isBlank()) {
            String k = keyer.trim().toUpperCase();
            PREFS.put(PREF_CW_KEYER, k.equals("HAMLIB") ? "HAMLIB" : "AUDIO");
        }
        if (wpm > 0) PREFS.putInt(PREF_CW_WPM, Math.max(5, Math.min(60, wpm)));
        flushPrefsQuietly("CW settings");
    }

    /** The saved CW keyer, {@code AUDIO} or {@code HAMLIB}. */
    public String getCwKeyer() { return PREFS.get(PREF_CW_KEYER, "AUDIO").toUpperCase(); }

    /** The saved CW speed in words per minute. */
    public int getCwWpm() { return PREFS.getInt(PREF_CW_WPM, HamlibCwKeyer.DEFAULT_WPM); }

    /** The saved PTT method, {@code VOX} or {@code HAMLIB}. */
    public String getPttMethod() { return PREFS.get(PREF_PTT_METHOD, "VOX").toUpperCase(); }

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
            // AX.25 is the only mode that lands here today and is permanently
            // decode-only in j-digi by design (packet TX needs a real TNC or
            // direwolf — not on this app's roadmap). The message is worded
            // accordingly so users don't think it's still "coming".
            postDecodeLine(txMode == ModeType.AX25
                    ? "AX.25 is decode-only in j-digi"
                    : "Transmit not available for mode: " + txMode);
            return;
        }

        status.setTxMode(txMode);
        status.setTxTextPreview(buildTxPreview(text));
        status.setTxState(TxState.STARTING);
        status.setTxStatusText("Starting " + txMode + " TX");
        status.setTransmitting(true);
        fireStatus();

        Runnable             onStarted   = () -> Platform.runLater(() -> {
            status.setTxState(TxState.TRANSMITTING);
            status.setTxStatusText(txMode + " TX active");
            fireStatus();
            postDecodeLine("TX started: " + txMode);
        });
        Runnable             onCompleted = () -> Platform.runLater(() -> {
            status.setTxState(TxState.COMPLETE);
            status.setTxStatusText("TX complete");
            status.setTransmitting(false);
            fireStatus();
            postDecodeLine("TX complete: " + txMode);
        });
        Runnable             onCancelled = () -> Platform.runLater(() -> {
            status.setTxState(TxState.CANCELLED);
            status.setTxStatusText("TX cancelled");
            status.setTransmitting(false);
            fireStatus();
            postDecodeLine("TX cancelled");
        });
        java.util.function.Consumer<String> onError = err -> Platform.runLater(() -> {
            status.setTxState(TxState.ERROR);
            status.setTxStatusText("TX error: " + err);
            status.setTransmitting(false);
            fireStatus();
            postDecodeLine("TX error: " + err);
        });

        try {
            if (txMode == ModeType.CW && "HAMLIB".equalsIgnoreCase(PREFS.get(PREF_CW_KEYER, "AUDIO"))) {
                // CW via the rig's built-in keyer (CAT). No audio path used.
                HamlibCwKeyer keyer = new HamlibCwKeyer(
                        PREFS.get(PREF_PTT_HAMLIB_HOST, HamlibRigControl.DEFAULT_HOST),
                        PREFS.getInt(PREF_PTT_HAMLIB_PORT, HamlibRigControl.DEFAULT_PORT),
                        PREFS.getInt(PREF_CW_WPM, HamlibCwKeyer.DEFAULT_WPM));
                keyer.transmit(text, onStarted, onCompleted, onCancelled, onError);
            } else {
                audioTxEngine.transmit(
                        transmitter.createSampleSource(text),
                        onStarted, onCompleted, onCancelled, onError);
            }
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
            // See transmitText() — AX.25 is permanently decode-only in j-digi.
            throw new UnsupportedOperationException(txMode == ModeType.AX25
                    ? "AX.25 is decode-only in j-digi (WAV export not available)"
                    : "WAV export not available for mode: " + txMode);
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

    private DigitalTransmitter transmitterForMode(ModeType mode) {
        if (mode == null) {
            return null;
        }

        return switch (mode) {
            case RTTY   -> rttyTransmitter;
            case PSK31  -> psk31Transmitter;
            // CW rebuilds per-transmit so the cw.wpm pref applies without
            // a restart. Other modes don't have a comparable speed knob.
            case CW     -> buildCwTransmitterFromPrefs();
            case OLIVIA -> oliviaTransmitter;
            case MFSK16   -> mfsk16Transmitter;
            case DOMINOEX -> dominoExTransmitter;
            // AX.25 is permanently decode-only in j-digi. Real packet TX
            // requires a TNC (or a software emulator like direwolf) sitting
            // between the soundcard and the radio's KISS/serial interface,
            // which is out of scope for this app. Don't propose building
            // an Ax25Transmitter — keep using a hardware TNC for packet TX.
            case AX25     -> null;
        };
    }

    /** "20m / CW — CW (US)" style caption for a rig frequency, from the shared band plan's active region. Empty when
     *  the rig is on a non-amateur frequency or hasn't reported one yet. */
    private static String captionFor(long hz) {
        if (hz <= 0) return "";
        BandplanLoader.Description d = BandplanLoader.getInstance().describe(hz);
        return d == null ? "" : d.toString();
    }

    private DigitalTransmitter buildCwTransmitterFromPrefs() {
        int wpm = PREFS.getInt(PREF_CW_WPM, HamlibCwKeyer.DEFAULT_WPM);
        return new CwTransmitter(
                com.hamradio.modem.audio.AudioEngine.SAMPLE_RATE,
                wpm,
                /*carrierHz=*/700.0,
                /*amplitude=*/0.50);
    }

    /**
     * Set the PTT method and rigctld endpoint from the host application's rig settings, save them, and apply them.
     * The same host and port are used by the Hamlib CW keyer. A blank method keeps the saved one; a blank host or an
     * out-of-range port is ignored. If a transmission is under way the new settings are saved but only take effect
     * on the next call, because the PTT implementation cannot be swapped mid-flight.
     *
     * @param method {@code VOX} or {@code HAMLIB}
     */
    public void applyPttSettings(String method, String host, int port) {
        if (method != null && !method.isBlank()) PREFS.put(PREF_PTT_METHOD, method.trim().toUpperCase());
        if (host != null && !host.isBlank()) PREFS.put(PREF_PTT_HAMLIB_HOST, host.trim());
        if (port > 0 && port < 65536) PREFS.putInt(PREF_PTT_HAMLIB_PORT, port);
        flushPrefsQuietly("PTT settings");
        try {
            audioTxEngine.setRigControl(buildConfiguredRigControl());
        } catch (IllegalStateException transmitting) {
            log.info("PTT settings saved; they apply after the current transmission");
        }
    }

    /**
     * Construct the {@link RigControl} the operator chose in Preferences.
     *
     * <p>Two modes ship:
     * <ul>
     *   <li>{@code VOX} — audio-sensing keying. j-digi outputs audio with
     *       no PTT command; whatever sits between j-digi and the antenna
     *       (the rig's built-in VOX, or an interface like SignaLink /
     *       DigiRig that does audio-sense PTT on its own board) does the
     *       keying. This is the default — it's the simplest setup and
     *       works without any CAT cable.</li>
     *   <li>{@code HAMLIB} — remote keying via Hamlib's {@code rigctld}
     *       (default {@code localhost:4532}). Universal — works with
     *       every rig Hamlib supports including Icom (via its {@code icom}
     *       backend). The operator configures rigctld's PTT method
     *       separately: CAT, DTR, RTS, etc.</li>
     * </ul>
     *
     * <p>The Icom-direct {@code CivRigControl} class exists too but isn't
     * wired here — it would need j-digi to spin up its own CivEngine,
     * and Hamlib's {@code icom} backend covers the same case without a
     * second serial-port allocation.
     *
     * <p>Backward compat: the value {@code NONE} from older builds is
     * accepted as a synonym for {@code VOX}.
     */
    private RigControl buildConfiguredRigControl() {
        String method = PREFS.get(PREF_PTT_METHOD, "VOX").toUpperCase();
        switch (method) {
            case "HAMLIB":
                // rigctld endpoint from the saved ptt.hamlib.host / .port prefs.
                String host = PREFS.get(PREF_PTT_HAMLIB_HOST, HamlibRigControl.DEFAULT_HOST);
                int port = PREFS.getInt(PREF_PTT_HAMLIB_PORT, HamlibRigControl.DEFAULT_PORT);
                log.info("PTT method = HAMLIB ({}:{})", host, port);
                return new HamlibRigControl(host, port);
            case "VOX":
            case "NONE":   // legacy alias
            default:
                log.info("PTT method = VOX (audio-sensing — rig/interface keys itself)");
                return new NoOpRigControl();
        }
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

    private void handleAudioFrame(float[] samples) {
        if (status.isTransmitting()) {
            return;
        }

        try {
            FftAnalyzer.SpectrumResult result = fftAnalyzer.analyze(samples);

            // Hand the spectrum to the local skimmer (no-op when disabled)
            localSkimmer.process(result.magnitudes());

            // Phase A multi-channel CW decode: feed every active channel
            // this audio frame. Cheap (one biquad per channel × 256 samples
            // per frame) and a no-op if the skimmer is disabled or there
            // aren't any detected peaks yet.
            if (localSkimmer.isEnabled()) {
                multiCarrierDecoder.processFrame(samples, lastSkimmerSnapshot,
                                                 status.getRigFrequencyHz());
            }

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
}
