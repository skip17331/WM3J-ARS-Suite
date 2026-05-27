package com.hamradio.jhub;

import com.google.gson.JsonObject;
import com.hamradio.jhub.model.JHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Probes the host system for the external binary dependencies that the ARS
 * Suite can optionally use: Hamlib ({@code rigctl}/{@code rotctl}) and WSJT-X.
 *
 * Design:
 *   • No installation. Detection only.
 *   • Two-step probe: (1) check {@code $PATH} via {@code which}/{@code where};
 *     (2) fall back to platform-specific common install paths.
 *   • Every detected binary is invoked with {@code --version} (2 s timeout)
 *     so we can surface the version in the dashboard card.
 *
 * Returns a small {@link JsonObject} intended for direct serialization by
 * the {@code /api/deps} servlet.
 */
public final class DependencyChecker {

    private static final Logger log = LoggerFactory.getLogger(DependencyChecker.class);
    private static final boolean WINDOWS = Platform.isWindows();
    private static final boolean MAC     = Platform.isMac();

    private DependencyChecker() {}

    /** Snapshot of all external dependencies. */
    public static JsonObject checkAll() {
        JsonObject out = new JsonObject();
        out.add("hamlib", checkHamlib());
        out.add("wsjtx",  checkWsjtx());
        out.addProperty("os", System.getProperty("os.name", ""));
        return out;
    }

    // -----------------------------------------------------------------
    // Hamlib — rigctl is the CAT probe; rotctl is the rotor probe.
    // We consider Hamlib "installed" if either is present.
    // -----------------------------------------------------------------

    public static JsonObject checkHamlib() {
        Detected rig  = detect("rigctl",  hamlibCandidates("rigctl"));
        Detected rigd = detect("rigctld", hamlibCandidates("rigctld"));
        Detected rot  = detect("rotctl",  hamlibCandidates("rotctl"));

        JsonObject o = new JsonObject();
        boolean installed = rig.installed || rot.installed || rigd.installed;
        o.addProperty("installed",  installed);
        o.addProperty("rigctlPath", rig.path);
        o.addProperty("rigctlVersion", rig.version);
        o.addProperty("rigctldPath", rigd.path);
        o.addProperty("rigctldVersion", rigd.version);
        o.addProperty("rotctlPath", rot.path);
        o.addProperty("rotctlVersion", rot.version);
        o.addProperty("installHint", hamlibInstallHint());
        return o;
    }

    /** Find rigctld on this system, or null if not installed. Used by RigctldManager. */
    public static String findRigctld() { return findDaemon("rigctld"); }

    /** Find rotctld on this system, or null if not installed. Used by RotctldManager. */
    public static String findRotctld() { return findDaemon("rotctld"); }

    /** Find ampctld on this system, or null if not installed. Used by AmpctldManager. */
    public static String findAmpctld() { return findDaemon("ampctld"); }

    /** Shared probe for the *ctld daemon family. Honors JHubConfig.hamlibBinDir
     *  override, then PATH, then platform-specific common install locations. */
    private static String findDaemon(String name) {
        Detected d = detect(name, hamlibCandidates(name));
        return d.installed ? d.path : null;
    }

    /**
     * Build the ordered list of candidate paths for a Hamlib tool ({@code rigctl},
     * {@code rigctld}, {@code rotctl}, {@code rotctld}, {@code ampctl}, {@code ampctld}).
     *
     * <p>Order:
     * <ol>
     *   <li>User override from {@code JHubConfig.hamlibBinDir} (accepts either
     *       the {@code bin/} folder itself or its parent — auto-appends
     *       {@code bin/} if needed).</li>
     *   <li>Platform-specific common install locations. On Windows this
     *       includes a directory scan for {@code hamlib-w64*} / {@code hamlib-w32*}
     *       subfolders under each known parent, since the official ZIP
     *       extracts to a versioned folder name (e.g.
     *       {@code C:\hamlib-w64-4.5.5\}).</li>
     * </ol>
     *
     * PATH lookup is handled separately by {@link #findOnPath(String)} and
     * runs first in {@link #detect(String, String[])}.
     */
    private static String[] hamlibCandidates(String tool) {
        String binary = WINDOWS ? tool + ".exe" : tool;
        List<String> out = new ArrayList<>();

        // 1) User override from j-hub.json
        try {
            String override = ConfigManager.getInstance().getConfig().hamlibBinDir;
            if (override != null && !override.isBlank()) {
                Path p = Paths.get(override.trim());
                // Accept either the bin/ folder itself, or an install root that
                // contains a bin/ subfolder.
                if (Files.isDirectory(p.resolve("bin"))) p = p.resolve("bin");
                out.add(p.resolve(binary).toString());
            }
        } catch (Exception e) {
            log.trace("hamlibBinDir override lookup failed: {}", e.getMessage());
        }

        // 2) Platform-specific common locations
        if (WINDOWS) {
            List<String> parents = new ArrayList<>();
            parents.add("C:\\Program Files");
            parents.add("C:\\Program Files (x86)");
            parents.add("C:\\");
            parents.add("C:\\Tools");
            String home = System.getProperty("user.home", "");
            if (!home.isEmpty()) {
                parents.add(home);
                parents.add(home + "\\Downloads");
            }
            for (String parent : parents) {
                Path pp = Paths.get(parent);
                // Bare names (in case the user manually renamed). Use Path
                // joining to normalize separators on Windows.
                out.add(pp.resolve("hamlib-w64").resolve("bin").resolve(binary).toString());
                out.add(pp.resolve("hamlib-w32").resolve("bin").resolve(binary).toString());
                // Versioned folder scan
                if (!Files.isDirectory(pp)) continue;
                try (Stream<Path> children = Files.list(pp)) {
                    children
                        .filter(Files::isDirectory)
                        .filter(d -> {
                            String n = d.getFileName().toString().toLowerCase();
                            return n.startsWith("hamlib-w64") || n.startsWith("hamlib-w32");
                        })
                        .forEach(d -> out.add(d.resolve("bin").resolve(binary).toString()));
                } catch (Exception e) {
                    log.trace("hamlib scan {} failed: {}", parent, e.getMessage());
                }
            }
        } else if (MAC) {
            out.add("/usr/local/bin/" + binary);
            out.add("/opt/homebrew/bin/" + binary);
            out.add("/opt/local/bin/" + binary);
        } else {
            out.add("/usr/bin/" + binary);
            out.add("/usr/local/bin/" + binary);
            out.add("/opt/hamlib/bin/" + binary);
        }

        return out.toArray(new String[0]);
    }

    private static String hamlibInstallHint() {
        if (WINDOWS) return "Download from https://hamlib.github.io/";
        if (MAC)     return "brew install hamlib";
        return "sudo apt install libhamlib-utils    (or dnf/pacman equivalent)";
    }

    // -----------------------------------------------------------------
    // WSJT-X
    // -----------------------------------------------------------------

    public static JsonObject checkWsjtx() {
        String[] common = WINDOWS
            ? new String[] {
                "C:\\WSJT\\wsjtx\\bin\\wsjtx.exe",
                "C:\\Program Files\\WSJT\\wsjtx\\bin\\wsjtx.exe",
              }
            : MAC
                ? new String[] { "/Applications/wsjtx.app/Contents/MacOS/wsjtx" }
                : new String[] { "/usr/bin/wsjtx", "/usr/local/bin/wsjtx" };

        Detected w = detect("wsjtx", common);

        JsonObject o = new JsonObject();
        o.addProperty("installed", w.installed);
        o.addProperty("path",      w.path);
        o.addProperty("version",   w.version);
        o.addProperty("installHint", wsjtxInstallHint());
        return o;
    }

    private static String wsjtxInstallHint() {
        if (WINDOWS) return "Download from https://wsjt.sourceforge.io/wsjtx.html";
        if (MAC)     return "Download .dmg from https://wsjt.sourceforge.io/wsjtx.html  (or brew install --cask wsjtx)";
        return "sudo apt install wsjtx    (or download .deb/.rpm from wsjt.sourceforge.io)";
    }

    // -----------------------------------------------------------------
    // Shared probe: try PATH, then common paths; then attempt --version.
    // -----------------------------------------------------------------

    private static final class Detected {
        boolean installed = false;
        String  path      = "";
        String  version   = "";
    }

    private static Detected detect(String binaryName, String[] fallbackPaths) {
        Detected d = new Detected();
        String path = findOnPath(binaryName);
        String source = "PATH";
        if (path == null) {
            for (String p : fallbackPaths) {
                File f = new File(p);
                if (f.isFile() && f.canExecute()) { path = p; source = "fallback"; break; }
            }
        }
        if (path == null) {
            // Log what we tried so the user can debug missing-Hamlib reports
            // from j-hub.log without needing to read the source.
            if ("rigctl".equals(binaryName) || "rigctld".equals(binaryName)) {
                log.info("Hamlib probe: {} not found on PATH or in any of {} candidate(s): {}",
                    binaryName, fallbackPaths.length, String.join(", ", fallbackPaths));
            } else {
                log.debug("Probe: {} not found on PATH or in {} candidate path(s)",
                    binaryName, fallbackPaths.length);
            }
            return d;
        }
        d.installed = true;
        d.path      = path;
        d.version   = probeVersion(path);
        log.debug("Probe: {} found via {} at {}", binaryName, source, path);
        return d;
    }

    private static String findOnPath(String binary) {
        String finder  = WINDOWS ? "where" : "which";
        String target  = WINDOWS ? binary + ".exe" : binary;
        try {
            Process p = new ProcessBuilder(finder, target)
                .redirectErrorStream(true)
                .start();
            p.waitFor(2, TimeUnit.SECONDS);
            String out = new String(p.getInputStream().readAllBytes()).trim();
            if (p.exitValue() == 0 && !out.isEmpty()) {
                // On Windows `where` may return multiple lines; take the first.
                int nl = out.indexOf('\n');
                return nl > 0 ? out.substring(0, nl).trim() : out;
            }
        } catch (Exception e) {
            log.trace("findOnPath({}) failed: {}", binary, e.getMessage());
        }
        return null;
    }

    private static String probeVersion(String binaryPath) {
        try {
            Process p = new ProcessBuilder(binaryPath, "--version")
                .redirectErrorStream(true)
                .start();
            if (!p.waitFor(2, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "";
            }
            String out = new String(p.getInputStream().readAllBytes()).trim();
            // Grab the first line — version output format varies wildly.
            int nl = out.indexOf('\n');
            String first = nl > 0 ? out.substring(0, nl) : out;
            return first.length() > 120 ? first.substring(0, 120) : first;
        } catch (Exception e) {
            log.trace("probeVersion({}) failed: {}", binaryPath, e.getMessage());
            return "";
        }
    }
}
