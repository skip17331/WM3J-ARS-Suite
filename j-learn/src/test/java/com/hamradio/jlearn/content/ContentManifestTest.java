package com.hamradio.jlearn.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test against the actual bundled manifest. The j-learn jar
 * ships ~200 sections — load() should parse all of them, expose them by
 * chapter, and resolve each entry's body via {@link ContentLoader}.
 */
class ContentManifestTest {

    @BeforeEach
    void freshCache() {
        ContentManifest.resetCache();
    }

    @Test
    void loadsBundledManifest() {
        ContentManifest m = ContentManifest.load();
        assertTrue(m.size() > 100,
            "expected >100 sections in the bundled manifest, got " + m.size());
    }

    @Test
    void byIdAndByChapterAgree() {
        ContentManifest m = ContentManifest.load();
        ContentEntry first = m.all().get(0);
        assertSame(first, m.byId(first.id()));
        List<ContentEntry> chapter = m.byChapter(first.chapter());
        assertTrue(chapter.contains(first));
    }

    @Test
    void chaptersAreInManifestOrderAndDistinct() {
        ContentManifest m = ContentManifest.load();
        List<String> chs = m.chapters();
        assertEquals(chs.size(), chs.stream().distinct().count(),
            "chapter list must be distinct");
        // Manifest opens with Part I → chapter 00 must come first.
        assertEquals("00", chs.get(0));
    }

    @Test
    void byIdReturnsNullForUnknownId() {
        ContentManifest m = ContentManifest.load();
        assertNull(m.byId("99-99"));
    }

    @Test
    void everyEntryResolvesToARealMarkdownBody() {
        ContentManifest m = ContentManifest.load();
        for (ContentEntry e : m.all()) {
            String body = ContentLoader.read(e);
            assertNotNull(body, "missing markdown for " + e.id() + " at " + e.path());
            assertTrue(body.startsWith("---"),
                "expected YAML frontmatter at start of " + e.path());
        }
    }

    @Test
    void frontMatterParserHandlesStandardBlock() {
        String body = ""
            + "---\n"
            + "id: 01-04\n"
            + "title: Test\n"
            + "level: mixed\n"
            + "status: draft # comment\n"
            + "---\n"
            + "# Body";
        Map<String, String> fm = ContentLoader.frontMatter(body);
        assertEquals("01-04", fm.get("id"));
        assertEquals("Test",   fm.get("title"));
        assertEquals("mixed",  fm.get("level"));
        assertEquals("draft",  fm.get("status"), "trailing # comment must be stripped");
    }
}
