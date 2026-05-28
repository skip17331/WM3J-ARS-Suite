package com.jlog.export;

import com.jlog.model.QsoRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the 2026-05-28 export-charset fix. Before the fix, AdifExporter
 * used the no-arg {@code FileWriter} which defers to the JVM default
 * charset — Cp1252 on pre-JDK-18 Windows JREs, UTF-8 elsewhere. A user
 * notes field with "André" would land as bytes {@code 41 6E 64 72 E9}
 * on Windows and {@code 41 6E 64 72 C3 A9} on Linux, silently producing
 * incompatible files. Now we force UTF-8 unconditionally.
 */
class AdifExporterCharsetTest {

    @Test
    void exportEncodesNonAsciiNotesAsUtf8(@TempDir Path dir) throws Exception {
        QsoRecord q = new QsoRecord();
        q.setCallsign("XE1ABC");
        q.setBand("20m");
        q.setMode("SSB");
        q.setDateTimeUtc(LocalDateTime.of(2026, 1, 1, 12, 0));
        q.setNotes("André");                          // 'é' = cp1252 0xE9 / UTF-8 0xC3 0xA9

        Path out = dir.resolve("utf8-check.adi");
        AdifExporter.exportAdif(out, List.of(q), false);

        byte[] bytes = Files.readAllBytes(out);

        // UTF-8 encoding of 'é' must be present somewhere in the file.
        assertTrue(containsSequence(bytes, new byte[]{(byte) 0xC3, (byte) 0xA9}),
            "exported file must contain UTF-8 bytes for 'é' (0xC3 0xA9)");

        // The cp1252 single-byte encoding of 'é' (0xE9) must NOT appear as
        // a lone byte. (It might appear as the *second* byte of a multibyte
        // sequence — that's fine; we just need to confirm we're not emitting
        // the bare cp1252 form for the 'é' in "André".)
        String asUtf8 = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(asUtf8.contains("André"),
            "exported file must round-trip 'André' through UTF-8 decoding");

        // Negative sanity: decoding as cp1252 would mangle the 0xC3 0xA9
        // pair into "Ã©", so a cp1252 decode must NOT produce "André".
        String asCp1252 = new String(bytes, java.nio.charset.Charset.forName("windows-1252"));
        assertFalse(asCp1252.contains("André"),
            "if cp1252 decode also yields 'André', the file is cp1252 (regression)");
    }

    private static boolean containsSequence(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
