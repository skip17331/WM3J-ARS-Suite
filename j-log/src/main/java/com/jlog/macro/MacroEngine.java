package com.jlog.macro;

import com.jlog.civ.CivEngine;
import com.jlog.cluster.HubEngine;
import com.jlog.db.MacroDao;
import com.jlog.model.Macro;
import com.jlog.model.Macro.MacroAction;
import com.jlog.util.AppConfig;
import com.jlog.util.MacroVariableEngine;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
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
                Platform.runLater(() -> {
                    try {
                        AudioClip clip = new AudioClip(
                            Paths.get(path).toUri().toString());
                        clip.play();
                    } catch (Exception ex) {
                        log.error("Voice playback failed: {}", path, ex);
                    }
                });
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
}
