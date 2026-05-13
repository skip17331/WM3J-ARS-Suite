package com.hamradio.jlearn;

import com.hamradio.jlearn.content.ContentManifest;
import com.hamradio.jlearn.state.FileReadingStateStore;
import com.hamradio.jlearn.state.ReadingStateStore;

/**
 * JLearnModule — entry point for the J-Learn library module.
 *
 * <p>J-Learn is the in-app learning and reference library for the WM3J
 * ARS Suite. It bundles ~200 markdown chapters covering propagation,
 * antennas, RF safety, troubleshooting, formulas, band plans, and
 * exam prep — accessible from any other module in the suite.
 *
 * <h2>Surface area</h2>
 * <ul>
 *   <li>{@link ContentManifest} — navigation tree parsed from
 *       {@code /content/manifest.md}.</li>
 *   <li>{@link com.hamradio.jlearn.content.ContentLoader} — markdown
 *       bodies fetched from the classpath on demand.</li>
 *   <li>{@link ReadingStateStore} — "resume where you left off" state in
 *       {@code ~/.j-learn/state.properties}.</li>
 * </ul>
 *
 * <p>Hosts launch a J-Learn UI of their own choosing (JavaFX WebView,
 * HTTP iframe, native widget); this class is the thin facade that hands
 * them the data structures.
 */
public final class JLearnModule {

    /** Library version string. */
    public static final String VERSION = "1.0.44";

    private static volatile ReadingStateStore stateStore;
    private static final   Object             STATE_LOCK = new Object();

    private JLearnModule() {}

    /** Returns the singleton manifest, loading it lazily on first call. */
    public static ContentManifest manifest() {
        return ContentManifest.load();
    }

    /** Returns the singleton reading-state store backed by
     *  {@link FileReadingStateStore#DEFAULT_PATH}. */
    public static ReadingStateStore stateStore() {
        ReadingStateStore s = stateStore;
        if (s != null) return s;
        synchronized (STATE_LOCK) {
            if (stateStore == null) stateStore = new FileReadingStateStore();
            return stateStore;
        }
    }

    /** Test hook — install a custom store (typically an in-memory or
     *  tmp-path one). Pass {@code null} to revert to the default. */
    public static void setStateStore(ReadingStateStore store) {
        synchronized (STATE_LOCK) { stateStore = store; }
    }
}
