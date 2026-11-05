package com.ars.fx.data;

import com.ars.fx.AppVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.jar.JarFile;

/**
 * The licence, Code of Conduct and legal notes, kept as local files so they can be read without a web link:
 * {@code ~/.j-hub/legal/}. The app carries a copy of each (built into the jar from the repository root); that copy is put
 * in place on first run, and when an update is downloaded the new release's copies are checked and replace the local ones
 * if that release is newer. The local files record which app version supplied them.
 */
public final class LegalDocs {
    private LegalDocs() {}

    public record Doc(String id, String title, String file) {}
    public record Result(List<String> updated, String version) {}

    public static final List<Doc> DOCS = List.of(
            new Doc("license", "GNU General Public License v3", "LICENSE"),
            new Doc("conduct", "Code of Conduct", "CodeOfConduct.md"),
            new Doc("legal", "Legal and privacy notes", "LEGAL.md"));

    private static final String RES = "com/ars/fx/legal/";
    private static final String META = "installed.properties";
    private static Path dirOverride;                         // tests
    static void setDirForTest(Path p) { dirOverride = p; }
    public static Path dir() { return dirOverride != null ? dirOverride : Paths.get(System.getProperty("user.home"), ".j-hub", "legal"); }

    // ---- reading -------------------------------------------------------------------------------------
    /** The text of a document: the local copy, else the one built into the app. */
    public static String read(Doc d) {
        try {
            Path p = dir().resolve(d.file());
            if (Files.exists(p)) return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
        byte[] b = bundled(d.file());
        return b == null ? "This document is not available." : new String(b, StandardCharsets.UTF_8);
    }
    public static Path localPath(Doc d) { return dir().resolve(d.file()); }

    static byte[] bundled(String file) {
        try (InputStream in = LegalDocs.class.getClassLoader().getResourceAsStream(RES + file)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) { return null; }
    }

    // ---- keeping the local copies current --------------------------------------------------------------
    /** First run, and every start: put the app's own copies in place if the local ones are missing or older. */
    public static Result installBundled() {
        return install(LegalDocs::bundled, AppVersion.current());
    }

    /** A downloaded (and verified) update: take the documents from that jar if its version is newer than what is installed. */
    public static Result installFromJar(Path jar) throws IOException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            String version = AppVersion.UNKNOWN;
            var ve = jf.getEntry("com/ars/fx/version.properties");
            if (ve != null) {
                Properties p = new Properties();
                try (InputStream in = jf.getInputStream(ve)) { p.load(in); }
                version = p.getProperty("version", AppVersion.UNKNOWN).trim();
            }
            return install(file -> {
                var e = jf.getEntry(RES + file);
                if (e == null) return null;
                try (InputStream in = jf.getInputStream(e)) { return in.readAllBytes(); }
                catch (IOException ex) { return null; }
            }, version);
        }
    }

    /**
     * Write each document that is missing, or that differs from the source's copy when the source is a newer version than the
     * one that supplied the local files. A source that is the same or older never overwrites.
     */
    static Result install(Function<String, byte[]> source, String sourceVersion) {
        List<String> updated = new ArrayList<>();
        try {
            Files.createDirectories(dir());
            Properties meta = new Properties();
            Path metaFile = dir().resolve(META);
            if (Files.exists(metaFile)) try (InputStream in = Files.newInputStream(metaFile)) { meta.load(in); }
            String have = meta.getProperty("version", "");
            boolean newer = have.isBlank() || AppVersion.isNewer(sourceVersion, have);
            for (Doc d : DOCS) {
                byte[] bytes = source.apply(d.file());
                if (bytes == null) continue;
                Path local = dir().resolve(d.file());
                boolean missing = !Files.exists(local);
                if (missing || (newer && !sha(Files.readAllBytes(local)).equals(sha(bytes)))) {
                    Path tmp = dir().resolve(d.file() + ".part");
                    Files.write(tmp, bytes);
                    Files.move(tmp, local, StandardCopyOption.REPLACE_EXISTING);
                    updated.add(d.file());
                }
            }
            if (newer) meta.setProperty("version", sourceVersion);
            try (var out = Files.newOutputStream(metaFile)) { meta.store(out, "Legal documents installed by the app; do not edit"); }
            return new Result(updated, meta.getProperty("version", ""));
        } catch (IOException e) {
            return new Result(updated, "");
        }
    }

    static String sha(byte[] b) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
