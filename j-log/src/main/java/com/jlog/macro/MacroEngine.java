package com.jlog.macro;

import com.jlog.civ.CivEngine;
import com.jlog.cluster.HubEngine;
import com.jlog.db.MacroDao;
import com.jlog.model.Macro;
import com.jlog.model.Macro.MacroAction;
import com.jlog.util.AppConfig;
import com.jlog.util.MacroVariableEngine;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Macro engine — loads macros from the database and executes them.
 *
 * Supported action types:
 *   CIV_COMMAND      — send raw CI-V hex string
 *   PTT_ON / PTT_OFF — key/unkey transmitter via CI-V
 *   VOICE_PLAY       — play audio file (path in data)
 *   CW_TEXT          — send CW via CI-V
 *   INSERT_EXCHANGE  — call registered exchange-insert handler
 *   AUTOFILL_FIELDS  — call registered autofill handler
 *   DELAY_MS         — sleep N milliseconds
 */
public class MacroEngine {

    private static final Logger log = LoggerFactory.getLogger(MacroEngine.class);
    private static final MacroEngine INSTANCE = new MacroEngine();
    public static MacroEngine getInstance() { return INSTANCE; }
    private MacroEngine() {}

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "macro-executor");
        t.setDaemon(true);
        return t;
    });

    // Hooks registered by active controller
    private Consumer<String> exchangeInsertHandler;
    private Runnable         autofillHandler;
    // Supplies the live QSO context (callsign / RST / exchange / serial) for variable
    // expansion in CW_TEXT macros. Populated by NormalLogController on init.
    private Supplier<MacroVariableEngine.Context> variableContextSupplier;

    // Tracks whether a CW transmission is currently in flight on the Hamlib path,
    // so a subsequent CW_TEXT macro can abort-and-replace instead of overlapping.
    // Reset by a scheduled timer based on the estimated send duration.
    private final AtomicBoolean cwInFlight = new AtomicBoolean(false);
    // Standard CW "I goofed, disregard" prosign — eight dits sent at the current speed.
    private static final String CW_ABORT_PROSIGN = "EEEEEEEE";

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /** Execute the macro bound to a function key (1-12). No-op if none bound. */
    public void triggerFKey(int fkey) {
        Macro m = MacroDao.getInstance().getByFKey(fkey);
        if (m != null) {
            log.debug("Triggering F{} → macro '{}'", fkey, m.getName());
            execute(m);
        }
    }

    /** Execute a macro by id. */
    public void triggerById(long id) {
        MacroDao.getInstance().fetchAll().stream()
            .filter(m -> m.getId() == id)
            .findFirst()
            .ifPresent(this::execute);
    }

    /** Execute a macro object directly. Runs on background thread. */
    public void execute(Macro macro) {
        executor.submit(() -> runActions(macro.getActions()));
    }

    /** Register the handler that inserts exchange text into the active field. */
    public void setExchangeInsertHandler(Consumer<String> handler) {
        this.exchangeInsertHandler = handler;
    }

    /** Register the autofill handler. */
    public void setAutofillHandler(Runnable handler) {
        this.autofillHandler = handler;
    }

    /** Register the supplier that produces the live QSO context for macro variable
     *  expansion (callsign field, RST fields, exchange, serial number, rig state). */
    public void setVariableContextSupplier(Supplier<MacroVariableEngine.Context> supplier) {
        this.variableContextSupplier = supplier;
    }

    /** Abort any in-flight CW transmission via Hamlib stop_morse and clear the
     *  in-flight flag. Called by the ESC key handler in NormalLogController. */
    public void abortCw() {
        if (cwInFlight.getAndSet(false)) {
            HubEngine.getInstance().stopCw();
            log.debug("CW abort (ESC)");
        }
    }

    /** Abort any in-flight voice macro (stops WAV playback and releases PTT).
     *  Bound to ESC by NormalLogController. Idempotent. */
    public void abortVoice() {
        if (VoiceKeyer.getInstance().isPlaying()) {
            VoiceKeyer.getInstance().stop();
            HubEngine.getInstance().sendPtt(false);
            log.debug("Voice macro abort (ESC)");
        }
    }

    /** Return all macros from the database. */
    public List<Macro> getAllMacros() {
        return MacroDao.getInstance().fetchAll();
    }

    // ---------------------------------------------------------------
    // Private execution
    // ---------------------------------------------------------------

    private void runActions(List<MacroAction> actions) {
        for (MacroAction action : actions) {
            try {
                executeAction(action);
            } catch (Exception ex) {
                log.error("Macro action error: type={}", action.getType(), ex);
            }
        }
    }

    private void executeAction(MacroAction action) throws InterruptedException {
        switch (action.getType()) {

            case CIV_COMMAND -> {
                log.debug("Macro: CI-V raw = {}", action.getData());
                CivEngine.getInstance().sendRawHex(action.getData());
            }

            case PTT_ON -> {
                log.debug("Macro: PTT ON");
                CivEngine.getInstance().setPtt(true);
            }

            case PTT_OFF -> {
                log.debug("Macro: PTT OFF");
                CivEngine.getInstance().setPtt(false);
            }

            case VOICE_PLAY -> {
                String path = action.getData();
                log.debug("Macro: voice play = {}", path);
                playVoiceMacro(path);
            }

            case CW_TEXT -> {
                String raw = action.getData();
                String text = expandVariables(raw);
                if (text == null || text.isEmpty()) break;
                int wpm = AppConfig.getInstance().getCwWpm();
                log.debug("Macro: CW text [{} WPM] = {}", wpm, text);

                HubEngine hub = HubEngine.getInstance();
                if (hub.isConnected() && hub.isHamlibCwAvailable()) {
                    sendCwViaHamlib(hub, text, wpm);
                } else {
                    // Fallback path — Icom CI-V direct keying. Variable expansion
                    // still applied so both transports stay in sync.
                    CivEngine.getInstance().sendCw(text);
                }
            }

            case INSERT_EXCHANGE -> {
                String text = action.getData();
                log.debug("Macro: insert exchange = {}", text);
                if (exchangeInsertHandler != null) {
                    Platform.runLater(() -> exchangeInsertHandler.accept(text));
                }
            }

            case AUTOFILL_FIELDS -> {
                log.debug("Macro: autofill fields");
                if (autofillHandler != null) {
                    Platform.runLater(autofillHandler);
                }
            }

            case DELAY_MS -> {
                int ms = action.getIntData();
                log.debug("Macro: delay {}ms", ms);
                Thread.sleep(ms);
            }

            default -> log.warn("Unknown macro action type: {}", action.getType());
        }
    }

    // ---------------------------------------------------------------
    // CW helpers
    // ---------------------------------------------------------------

    private String expandVariables(String template) {
        if (template == null || template.isEmpty()) return template;
        if (variableContextSupplier == null) return template;
        MacroVariableEngine.Context ctx = variableContextSupplier.get();
        return MacroVariableEngine.substitute(template, ctx);
    }

    /** Send {@code text} as CW via the Hamlib path, applying abort-and-replace
     *  semantics if a previous CW transmission is still in flight. */
    private void sendCwViaHamlib(HubEngine hub, String text, int wpm) {
        if (cwInFlight.getAndSet(true)) {
            // Replace: stop the running transmission, send the abort prosign so
            // the receiving op disregards the partial, then start the new text.
            hub.stopCw();
            hub.sendCw(CW_ABORT_PROSIGN, wpm);
        }
        hub.sendCw(text, wpm);
        scheduleInFlightClear(text.length() + CW_ABORT_PROSIGN.length(), wpm);
    }

    /** Estimate when the morse will finish and clear the in-flight flag. PARIS
     *  standard: a word averages 50 dot-units, with ~5 chars per word, so a single
     *  character takes 60 / (5 * wpm) seconds. A small safety margin avoids racing
     *  the next macro press against a still-keying rig. */
    private void scheduleInFlightClear(int charCount, int wpm) {
        if (wpm <= 0) wpm = 25;
        long ms = Math.round((charCount * 60_000.0) / (5.0 * wpm)) + 250L;
        executor.submit(() -> {
            try { Thread.sleep(ms); } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            cwInFlight.set(false);
        });
    }

    // ---------------------------------------------------------------
    // Voice keyer — PTT-bracketed WAV playback
    // ---------------------------------------------------------------

    /**
     * Play a voice keyer WAV file with Hamlib PTT bracketing.
     *
     * <p>Sequence: PTT-on → wait pre-roll → playback → drain → wait post-roll →
     * PTT-off. All blocking happens on the macro-executor thread (this method),
     * which is safe because the executor is single-threaded and the controller
     * thread isn't waiting on it.
     *
     * <p>Concurrent-macro behavior is read from {@link AppConfig#getVoiceConcurrentMode()}:
     * <ul>
     *   <li>REPLACE — abort any current playback, release PTT, then start fresh
     *   <li>QUEUE   — wait for the current playback to finish (executor serializes)
     *   <li>IGNORE  — drop this macro silently if one is already playing
     * </ul>
     */
    private void playVoiceMacro(String path) {
        if (path == null || path.isBlank()) return;
        File wav = new File(path);
        if (!wav.isFile()) {
            log.warn("Voice macro WAV not found: {}", path);
            return;
        }

        AppConfig cfg = AppConfig.getInstance();
        VoiceKeyer keyer = VoiceKeyer.getInstance();
        HubEngine  hub   = HubEngine.getInstance();

        if (keyer.isPlaying()) {
            String mode = cfg.getVoiceConcurrentMode();
            switch (mode) {
                case "IGNORE" -> {
                    log.debug("Voice macro ignored — already playing (mode=IGNORE)");
                    return;
                }
                case "QUEUE" -> {
                    // Single-threaded executor: waiting here means the previous
                    // playback's onDone has already run and PTT-off has fired
                    // before we proceed.
                    while (keyer.isPlaying()) {
                        try { Thread.sleep(50); }
                        catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt(); return;
                        }
                    }
                }
                default -> {
                    // REPLACE
                    keyer.stop();
                    hub.sendPtt(false);
                    try { Thread.sleep(50); } // brief settle so the rig sees the PTT-off edge
                    catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt(); return;
                    }
                }
            }
        }

        int preRoll  = cfg.getVoicePreRollMs();
        int postRoll = cfg.getVoicePostRollMs();

        hub.sendPtt(true);
        try { Thread.sleep(preRoll); }
        catch (InterruptedException ignored) {
            hub.sendPtt(false);
            Thread.currentThread().interrupt();
            return;
        }

        // onDone runs on the playback thread; the post-roll + PTT-off happen
        // there so the macro executor is free to serve the next macro press.
        keyer.play(wav,
                () -> {
                    try { Thread.sleep(postRoll); }
                    catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    hub.sendPtt(false);
                },
                err -> {
                    // Playback failed — release PTT immediately so the rig isn't stuck keyed.
                    hub.sendPtt(false);
                });
    }
}
