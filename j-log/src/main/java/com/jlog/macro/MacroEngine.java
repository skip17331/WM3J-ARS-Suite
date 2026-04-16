package com.jlog.macro;

import com.jlog.civ.CivEngine;
import com.jlog.db.MacroDao;
import com.jlog.model.Macro;
import com.jlog.model.Macro.MacroAction;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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
                String text = action.getData();
                log.debug("Macro: CW text = {}", text);
                CivEngine.getInstance().sendCw(text);
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
}
