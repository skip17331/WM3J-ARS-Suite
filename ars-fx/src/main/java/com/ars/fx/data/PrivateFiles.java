package com.ars.fx.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

/**
 * Keeps the settings files that hold passwords and keys readable by their owner only: {@code ~/.j-log/upload.json} (log upload
 * logins), {@code ~/.j-log/lookup.json} (callsign lookup logins) and {@code ~/.j-hub/config.json} (the contest network key, among
 * all the other settings).
 *
 * <p>On Linux and macOS the file is set to {@code rw-------}. Windows has no POSIX permissions; there, files under the user's own
 * profile folder are already limited to that user (plus SYSTEM and Administrators) by the folder's inherited access list, so nothing is
 * changed. The secrets are still stored as plain text: this only stops other accounts on the same computer from reading them.
 */
public final class PrivateFiles {
    private PrivateFiles() {}

    private static final EnumSet<PosixFilePermission> OWNER_ONLY = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    /** Limit {@code file} to its owner where the file system supports POSIX permissions. Never throws; returns whether it was applied. */
    public static boolean restrict(Path file) {
        try {
            if (file == null || !Files.exists(file)) return false;
            PosixFileAttributeView v = Files.getFileAttributeView(file, PosixFileAttributeView.class);
            if (v == null) return false;                                   // Windows: the profile folder's access list already covers it
            if (!v.readAttributes().permissions().equals(OWNER_ONLY)) v.setPermissions(OWNER_ONLY);
            return true;
        } catch (Exception e) {
            return false;                                                  // best effort: a settings save must never fail on this
        }
    }
}
