package com.hamradio.jlearn;

/**
 * JLearnModule — entry point for the J-Learn library module.
 *
 * <h2>Role</h2>
 * J-Learn is the in-app learning and reference library for the WM3J
 * ARS Suite. It bundles ~200 markdown chapters covering propagation,
 * antennas, RF safety, troubleshooting, formulas, band plans, and
 * exam prep — accessible from any other module in the suite.
 *
 * <h2>Surface area</h2>
 * <ul>
 *   <li>Static content under {@code /content/} on the classpath
 *       (chapters as markdown files, indexed by {@code manifest.md}).</li>
 *   <li>A {@link com.hamradio.jlearn.content.ContentManifest} parser that
 *       turns the manifest into a navigable tree.</li>
 *   <li>A {@link com.hamradio.jlearn.content.ContentLoader} that reads
 *       individual chapters from the classpath on demand.</li>
 *   <li>{@link com.hamradio.jlearn.state.ReadingStateStore} for "resume
 *       where you left off" persistence in {@code ~/.j-learn/state.properties}.</li>
 * </ul>
 *
 * <h2>Integration with other modules</h2>
 * Other suite modules (j-log, j-map, j-digi, j-bridge, j-sat) call into
 * J-Learn through this entry point — typically by launching a J-Learn
 * window pointed at a specific section id. The exact handoff depends on
 * the host: a JavaFX module wraps the content in a {@code WebView}; a
 * web-based host (J-Hub config UI) exposes the content via a REST
 * endpoint that serves the markdown directly.
 *
 * <p>This class is intentionally a thin facade — UI rendering and
 * launch glue belong in the host module, not here, so J-Learn can stay
 * dependency-light (JDK-only) and re-usable.
 *
 * <h2>Status</h2>
 * Skeleton only. All methods are unimplemented.
 */
public final class JLearnModule {

    /** Library version string — embedded in any UI footer or "About" pane. */
    public static final String VERSION = "1.0.0";

    private JLearnModule() {
        // Static facade — no instances.
    }

    /**
     * Returns the singleton {@link com.hamradio.jlearn.content.ContentManifest}
     * for this process, loading it lazily from the classpath the first time it's
     * called. Safe to call from multiple threads.
     */
    // TODO: implement
    public static com.hamradio.jlearn.content.ContentManifest manifest() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Returns the singleton {@link com.hamradio.jlearn.state.ReadingStateStore}
     * backed by {@code ~/.j-learn/state.properties}. Used by the host UI to
     * record and recall the user's last-opened section.
     */
    // TODO: implement
    public static com.hamradio.jlearn.state.ReadingStateStore stateStore() {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
