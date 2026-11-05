package com.ars.fx.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class LegalDocsTest {
    @AfterEach
    void reset() { LegalDocs.setDirForTest(null); }

    static Map<String, byte[]> docs(String text) {
        return Map.of("LICENSE", ("license " + text).getBytes(StandardCharsets.UTF_8),
                "CodeOfConduct.md", ("conduct " + text).getBytes(StandardCharsets.UTF_8),
                "LEGAL.md", ("legal " + text).getBytes(StandardCharsets.UTF_8));
    }
    static String local(Path dir, String f) throws IOException { return Files.readString(dir.resolve(f)); }

    @Test
    void shippedDocumentsAreBuiltIntoTheApp() {
        for (LegalDocs.Doc d : LegalDocs.DOCS) {
            byte[] b = LegalDocs.bundled(d.file());
            assertNotNull(b, d.file() + " should be on the classpath (pom copies it from the repo root)");
            assertTrue(b.length > 500, d.file());
        }
        assertTrue(new String(LegalDocs.bundled("LICENSE"), StandardCharsets.UTF_8).contains("GNU GENERAL PUBLIC LICENSE"));
    }

    @Test
    void firstRunPutsAllThreeInPlace(@TempDir Path tmp) throws Exception {
        LegalDocs.setDirForTest(tmp);
        LegalDocs.Result r = LegalDocs.install(docs("v1")::get, "1.0.0");
        assertEquals(3, r.updated().size());
        assertEquals("license v1", local(tmp, "LICENSE"));
        assertEquals("1.0.0", r.version());
        assertTrue(LegalDocs.read(LegalDocs.DOCS.get(0)).startsWith("license v1"), "reads the local copy");
    }

    @Test
    void aNewerReleaseReplacesChangedDocumentsAndLeavesTheRest(@TempDir Path tmp) throws Exception {
        LegalDocs.setDirForTest(tmp);
        LegalDocs.install(docs("v1")::get, "1.0.0");
        Map<String, byte[]> next = new java.util.HashMap<>(docs("v1"));
        next.put("LEGAL.md", "legal v2".getBytes(StandardCharsets.UTF_8));
        LegalDocs.Result r = LegalDocs.install(next::get, "1.1.0");
        assertEquals(java.util.List.of("LEGAL.md"), r.updated(), "only the changed one is rewritten");
        assertEquals("legal v2", local(tmp, "LEGAL.md"));
        assertEquals("license v1", local(tmp, "LICENSE"));
        assertEquals("1.1.0", r.version());
    }

    @Test
    void sameOrOlderReleaseNeverOverwrites(@TempDir Path tmp) throws Exception {
        LegalDocs.setDirForTest(tmp);
        LegalDocs.install(docs("new")::get, "2.0.0");
        assertTrue(LegalDocs.install(docs("old")::get, "1.0.0").updated().isEmpty());
        assertTrue(LegalDocs.install(docs("same-version-different")::get, "2.0.0").updated().isEmpty());
        assertEquals("license new", local(tmp, "LICENSE"));
    }

    @Test
    void aMissingLocalFileIsRestoredEvenFromTheSameVersion(@TempDir Path tmp) throws Exception {
        LegalDocs.setDirForTest(tmp);
        LegalDocs.install(docs("v1")::get, "1.0.0");
        Files.delete(tmp.resolve("LICENSE"));
        LegalDocs.Result r = LegalDocs.install(docs("v1")::get, "1.0.0");
        assertEquals(java.util.List.of("LICENSE"), r.updated());
    }

    @Test
    void aDownloadedUpdateJarIsCheckedAndTheLocalCopiesUpdated(@TempDir Path tmp) throws Exception {
        LegalDocs.setDirForTest(tmp.resolve("legal"));
        LegalDocs.install(docs("v1")::get, "1.0.0");

        Path jar = tmp.resolve("ars-fx-1.2.0.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            put(out, "com/ars/fx/version.properties", "version=1.2.0\n".getBytes(StandardCharsets.UTF_8));
            for (var e : docs("v2").entrySet()) put(out, "com/ars/fx/legal/" + e.getKey(), e.getValue());
        }
        LegalDocs.Result r = LegalDocs.installFromJar(jar);
        assertEquals(3, r.updated().size());
        assertEquals("license v2", local(tmp.resolve("legal"), "LICENSE"));
        assertEquals("1.2.0", r.version());

        Path older = tmp.resolve("ars-fx-1.0.5.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(older))) {
            put(out, "com/ars/fx/version.properties", "version=1.0.5\n".getBytes(StandardCharsets.UTF_8));
            for (var e : docs("v0").entrySet()) put(out, "com/ars/fx/legal/" + e.getKey(), e.getValue());
        }
        assertTrue(LegalDocs.installFromJar(older).updated().isEmpty(), "an older jar does not roll the documents back");
        assertEquals("license v2", local(tmp.resolve("legal"), "LICENSE"));
    }

    private static void put(JarOutputStream out, String name, byte[] data) throws IOException {
        out.putNextEntry(new JarEntry(name)); out.write(data); out.closeEntry();
    }
}
