package com.hamradio.jlearn.state;

import java.time.Instant;
import java.util.Objects;

/**
 * ReadingState — snapshot of where the user left off.
 *
 * <p>Three pieces of information matter when re-opening J-Learn:
 * the last-opened section id, a scroll fraction (0.0 = top, 1.0 = bottom),
 * and the timestamp the snapshot was written.
 *
 * <p>The scroll fraction is stored as a fraction rather than a pixel
 * offset so it survives font-size changes and re-renders.
 */
public final class ReadingState {

    private final String  lastSectionId;
    private final double  scrollFraction;
    private final Instant savedAt;

    public ReadingState(String lastSectionId, double scrollFraction, Instant savedAt) {
        if (lastSectionId == null || lastSectionId.isBlank()) {
            throw new IllegalArgumentException("lastSectionId must be non-blank");
        }
        if (scrollFraction < 0.0 || scrollFraction > 1.0 || Double.isNaN(scrollFraction)) {
            throw new IllegalArgumentException(
                "scrollFraction must be in [0.0, 1.0]: " + scrollFraction);
        }
        if (savedAt == null) {
            throw new IllegalArgumentException("savedAt must not be null");
        }
        this.lastSectionId  = lastSectionId;
        this.scrollFraction = scrollFraction;
        this.savedAt        = savedAt;
    }

    /** Factory that stamps {@code savedAt = Instant.now()}. */
    public static ReadingState newSnapshot(String lastSectionId, double scrollFraction) {
        return new ReadingState(lastSectionId, scrollFraction, Instant.now());
    }

    public String  lastSectionId()  { return lastSectionId;  }
    public double  scrollFraction() { return scrollFraction; }
    public Instant savedAt()        { return savedAt;        }

    @Override public boolean equals(Object o) {
        if (!(o instanceof ReadingState)) return false;
        ReadingState r = (ReadingState) o;
        return lastSectionId.equals(r.lastSectionId)
            && Double.compare(scrollFraction, r.scrollFraction) == 0
            && savedAt.equals(r.savedAt);
    }
    @Override public int hashCode() {
        return Objects.hash(lastSectionId, scrollFraction, savedAt);
    }
    @Override public String toString() {
        return "ReadingState{" + lastSectionId
             + " @" + String.format("%.2f", scrollFraction)
             + " " + savedAt + "}";
    }
}
