package com.hamradio.jhub;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
        String[] common = WINDOWS
            ? new String[] {
                "C:\\Program Files\\hamlib-w64\\bin\\rigctl.exe",
                "C:\\Program Files (x86)\\hamlib-w32\\bin\\rigctl.exe",
              }
            : MAC
                ? new String[] { "/usr/local/bin/rigctl", "/opt/homebrew/bin/rigctl" }
                : new String[] { "/usr/bin/rigctl", "/usr/local/bin/rigctl" };

        Detected rig = detect("rigctl", common);
        Detected rot = detect("rotctl", mapRotSuffix(common));

        JsonObject o = new JsonObject();
        boolean installed = rig.installed || rot.installed;
        o.addProperty("installed",  installed);
        o.addProperty("rigctlPath", rig.path);
        o.addProperty("rigctlVersion", rig.version);
        o.addProperty("rotctlPath", rot.path);
        o.addProperty("rotctlVersion", rot.version);
        o.addProperty("installHint", hamlibInstallHint());
        return o;
    }

    // Map rigctl paths to their rotctl siblings
    private static String[] mapRotSuffix(String[] rigPaths) {
        String[] out = new String[rigPaths.length];
        for (int i = 0; i < rigPaths.length; i++) {
            out[i] = rigPaths[i].replace("rigctl", "rotctl");
        }
        return out;
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
        if (path == null) {
            for (String p : fallbackPaths) {
                if (new File(p).isFile() && new File(p).canExecute()) { path = p; break; }
            }
        }
        if (path == null) return d;
        d.installed = true;
        d.path      = path;
        d.version   = probeVersion(path);
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
