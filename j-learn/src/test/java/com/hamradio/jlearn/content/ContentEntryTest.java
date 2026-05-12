package com.hamradio.jlearn.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentEntryTest {

    @Test
    void splitsChapterAndSectionFromId() {
        ContentEntry e = new ContentEntry("01-04", "Ionospheric Layers",
            "01-propagation/01-04-ionospheric-layers.md",
            ContentEntry.Level.MIXED, ContentEntry.Status.DRAFT);
        assertEquals("01", e.chapter());
        assertEquals("04", e.section());
    }

    @Test
    void rejectsMalformedId() {
        assertThrows(IllegalArgumentException.class,
            () -> new ContentEntry("1-4", "x", "p.md",
                ContentEntry.Level.SIMPLE, ContentEntry.Status.DRAFT));
        assertThrows(IllegalArgumentException.class,
            () -> new ContentEntry(null, "x", "p.md",
                ContentEntry.Level.SIMPLE, ContentEntry.Status.DRAFT));
    }

    @Test
    void rejectsBlankTitleOrPath() {
        assertThrows(IllegalArgumentException.class,
            () -> new ContentEntry("01-01", "", "p.md",
                ContentEntry.Level.SIMPLE, ContentEntry.Status.DRAFT));
        assertThrows(IllegalArgumentException.class,
            () -> new ContentEntry("01-01", "title", "",
                ContentEntry.Level.SIMPLE, ContentEntry.Status.DRAFT));
    }

    @Test
    void equalsKeyedOnIdOnly() {
        ContentEntry a = new ContentEntry("01-01", "Title A", "a.md",
            ContentEntry.Level.SIMPLE, ContentEntry.Status.STUB);
        ContentEntry b = new ContentEntry("01-01", "Title B different", "b.md",
            ContentEntry.Level.ADVANCED, ContentEntry.Status.PUBLISHED);
        assertEquals(a, b, "same id => equal even if other fields differ");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void levelAndStatusParseUnknownAsSensibleDefault() {
        assertEquals(ContentEntry.Level.MIXED,    ContentEntry.Level.parse("???"));
        assertEquals(ContentEntry.Level.MIXED,    ContentEntry.Level.parse(null));
        assertEquals(ContentEntry.Level.SIMPLE,   ContentEntry.Level.parse(" SIMPLE "));
        assertEquals(ContentEntry.Status.DRAFT,   ContentEntry.Status.parse("???"));
        assertEquals(ContentEntry.Status.DRAFT,   ContentEntry.Status.parse(null));
        assertEquals(ContentEntry.Status.PUBLISHED, ContentEntry.Status.parse(" published "));
    }
}
