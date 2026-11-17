package com.ars.fx.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PrivateFilesTest {

    @Test
    void ownerOnlyOnPosixFileSystems(@TempDir Path dir) throws Exception {
        Path f = Files.writeString(dir.resolve("upload.json"), "{\"lotw\":{\"password\":\"x\"}}");
        assumeTrue(Files.getFileAttributeView(f, PosixFileAttributeView.class) != null, "no POSIX permissions here (Windows)");
        Files.setPosixFilePermissions(f, PosixFilePermissions.fromString("rw-r--r--"));
        assertTrue(PrivateFiles.restrict(f));
        assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), Files.getPosixFilePermissions(f));
        assertEquals("{\"lotw\":{\"password\":\"x\"}}", Files.readString(f));    // the content is untouched
    }

    @Test
    void neverThrows(@TempDir Path dir) throws Exception {
        assertFalse(PrivateFiles.restrict(null));
        assertFalse(PrivateFiles.restrict(dir.resolve("missing.json")));
        Path f = Files.writeString(dir.resolve("config.json"), "{}");
        boolean posix = Files.getFileAttributeView(f, PosixFileAttributeView.class) != null;
        assertEquals(posix, PrivateFiles.restrict(f));                          // applied on Linux / macOS, left alone on Windows
        assertEquals("{}", Files.readString(f));
    }
}
