package com.hamradio.jlearn.state;

import java.time.Instant;

/**
 * ReadingState — snapshot of where the user left off.
 *
 * <p>Three pieces of information matter when re-opening J-Learn:
 * <ol>
 *   <li><b>Last-opened section id</b> — the {@code NN-NN} pointer back into
 *       the manifest. Always set (the host should never persist a state
 *       record without one).</li>
 *   <li><b>Scroll fraction</b> — where in the section the user was, expressed
 *       as a fraction between 0.0 (top) and 1.0 (bottom). Stored as a
 *       fraction rather than pixel offset so it survives font-size changes
 *       and re-renders.</li>
 *   <li><b>Timestamp</b> — when the snapshot was written, so the UI can
 *       show "you left off here, 4 hours ago".</li>
 * </ol>
 *
 * <h2>Status</h2>
 * Skeleton only — fields, accessors, builder, and serialization are
 * not yet implemented.
 */
public final class ReadingState {

    // TODO: declare fields (lastSectionId, scrollFraction, savedAt)
    // TODO: constructor + validation (id non-blank, scrollFraction in [0,1])
    // TODO: accessors and equals/hashCode
    // TODO: factory method newSnapshot(String id, double scrollFraction) that
    //       stamps savedAt = Instant.now()

    private ReadingState() {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
