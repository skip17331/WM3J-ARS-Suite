package com.hamradio.ars.installer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ARS Suite installer — Linux and Windows.
 *
 * Manifest-driven: reads {@code manifest.json} from inside the installer jar,
 * walks each module, and for every one that has a built jar it wires up
 * platform-appropriate desktop integration:
 *
 * <ul>
 *   <li><b>Linux</b> — writes {@code .desktop} entries to
 *       {@code ~/.local/share/applications/}, copies icons to
 *       {@code ~/.local/share/icons/}, chmods the launch scripts.</li>
 *   <li><b>Windows</b> — generates {@code .bat} launchers if missing,
 *       creates Start-Menu shortcuts under
 *       {@code %APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite\}
 *       via a PowerShell {@code WScript.Shell} snippet.</li>
 * </ul>
 *
 * Zero-state guarantee: never touches {@code j-hub.json}, databases, or logs.
 * Safe to run repeatedly as an upgrade.
 */
public final class Installer {

    private static final String MANIFEST_RESOURCE = "/manifest.json";
    private static final boolean WINDOWS =
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    private Installer() {}

    public static void main(String[] args) throws Exception {
        Path sourceRoot = resolveSourceRoot(args);
        Path home       = Paths.get(System.getProperty("user.home"));

        banner();
        info("Platform:    " + (WINDOWS ? "Windows" : "Linux / macOS"));
        info("Source root: " + sourceRoot);
        info("User home:   " + home);
        System.out.println();

        JsonObject manifest;
        try (var in = Installer.class.getResourceAsStream(MANIFEST_RESOURCE)) {
            if (in == null) throw new IllegalStateException("manifest.json missing from installer jar");
            manifest = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                                 .getAsJsonObject();
        }

        JsonArray modules = manifest.getAsJsonArray("modules");
        List<String> installed = new ArrayList<>();
        List<String> skipped   = new ArrayList<>();

        if (WINDOWS) installWindows(sourceRoot, modules, installed, skipped);
        else         installLinux  (sourceRoot, home, modules, installed, skipped);

        System.out.println();
        info("Installed shortcuts:");
        for (String s : installed) System.out.println("  + " + s);
        if (!skipped.isEmpty()) {
            System.out.println();
            warn("Skipped (build the module first, then re-run installer):");
            for (String s : skipped) System.out.println("  - " + s);
        }

        System.out.println();
        depCheck();

        System.out.println();
        info("Done. Launch via your system menu, or directly:");
        System.out.println("    " + sourceRoot.resolve(WINDOWS ? "j-hub\\start.bat"
                                                                : "j-hub/start.sh"));
    }

    // -----------------------------------------------------------------
    // Linux installer path
    // -----------------------------------------------------------------

    private static void installLinux(Path sourceRoot, Path home,
                                     JsonArray modules,
                                     List<String> installed, List<String> skipped) throws Exception {
        Path appsDir = home.resolve(".local/share/applications");
        Path iconDir = home.resolve(".local/share/icons");
        Files.createDirectories(appsDir);
        Files.createDirectories(iconDir);

        for (var el : modules) {
            JsonObject m = el.getAsJsonObject();
            String name   = m.get("name").getAsString();
            String title  = m.get("title").getAsString();
            String launch = m.get("launch").getAsString();
            String jar    = m.get("jar").getAsString();
            String iconR  = m.has("icon") ? m.get("icon").getAsString() : null;
            String cats   = m.has("categories") ? m.get("categories").getAsString() : "Utility;";
            String cmt    = m.has("comment") ? m.get("comment").getAsString() : title;

            Path jarPath    = sourceRoot.resolve(jar);
            Path launchPath = sourceRoot.resolve(launch);
            if (!Files.exists(jarPath))    { skipped.add(name + " (missing build: " + jar + ")");    continue; }
            if (!Files.exists(launchPath)) { skipped.add(name + " (missing launch: " + launch + ")"); continue; }

            try { launchPath.toFile().setExecutable(true, false); } catch (Exception ignored) {}

            String iconField = null;
            if (iconR != null) {
                Path src = sourceRoot.resolve(iconR);
                if (Files.exists(src)) {
                    Path dst = iconDir.resolve("ars-" + name + ".png");
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    iconField = dst.toString();
                }
            }

            Path desktop = appsDir.resolve("ars-" + name + ".desktop");
            writeDesktopEntry(desktop, title, cmt, launchPath, iconField, cats);
            installed.add(name + " → " + desktop);
        }
    }

    private static void writeDesktopEntry(Path path, String title, String comment,
                                          Path launchScript, String iconPath, String categories)
            throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[Desktop Entry]\n");
        sb.append("Type=Application\n");
        sb.append("Version=1.0\n");
        sb.append("Name=").append(title).append('\n');
        sb.append("Comment=").append(comment).append('\n');
        sb.append("Exec=bash \"").append(launchScript).append("\"\n");
        sb.append("Path=").append(launchScript.getParent()).append('\n');
        sb.append("Terminal=false\n");
        if (iconPath != null) sb.append("Icon=").append(iconPath).append('\n');
        sb.append("Categories=").append(categories).append('\n');
        sb.append("StartupNotify=true\n");
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try { path.toFile().setExecutable(true, false); } catch (Exception ignored) {}
    }

    // -----------------------------------------------------------------
    // Windows installer path
    // -----------------------------------------------------------------

    private static void installWindows(Path sourceRoot, JsonArray modules,
                                       List<String> installed, List<String> skipped) throws Exception {
        // Start Menu folder: %APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            warn("APPDATA is not set — falling back to user.home\\AppData\\Roaming");
            appData = System.getProperty("user.home") + "\\AppData\\Roaming";
        }
        Path startMenu = Paths.get(appData,
            "Microsoft", "Windows", "Start Menu", "Programs", "ARS Suite");
        Files.createDirectories(startMenu);

        for (var el : modules) {
            JsonObject m = el.getAsJsonObject();
            String name   = m.get("name").getAsString();
            String title  = m.get("title").getAsString();
            String launch = m.get("launch").getAsString();      // unix path in manifest
            String jar    = m.get("jar").getAsString();

            Path jarPath = sourceRoot.resolve(jar);
            if (!Files.exists(jarPath)) {
                skipped.add(name + " (missing build: " + jar + ")");
                continue;
            }

            // We generate a sibling .bat for each .sh. The .bat mirrors the
            // shell script's behavior: set module dir as cwd, launch the jar
            // with the javafx module path in lib/javafx.
            Path shPath  = sourceRoot.resolve(launch);
            Path batPath = siblingBat(sourceRoot, shPath, jarPath);
            if (!Files.exists(batPath)) writeBatLauncher(batPath, jarPath);

            Path shortcut = startMenu.resolve(title + ".lnk");
            createWindowsShortcut(shortcut, batPath, batPath.getParent(), title);
            installed.add(name + " → " + shortcut);
        }
    }

    /** Convert a unix-style launch path (e.g. {@code j-hub/start.sh}) to the
     *  module's native {@code .bat} path. Accepts either a sibling of the
     *  .sh or the module root. */
    private static Path siblingBat(Path sourceRoot, Path shPath, Path jarPath) {
        String filename = shPath.getFileName().toString();
        String bat = filename.endsWith(".sh")
            ? filename.substring(0, filename.length() - 3) + ".bat"
            : filename + ".bat";
        // Place the .bat next to the .sh (module root)
        return shPath.resolveSibling(bat);
    }

    private static void writeBatLauncher(Path batPath, Path jarPath) throws java.io.IOException {
        Path moduleDir = batPath.getParent();
        Path relJar    = moduleDir.relativize(jarPath);
        StringBuilder sb = new StringBuilder();
        sb.append("@echo off\r\n");
        sb.append("setlocal\r\n");
        sb.append("set \"SCRIPT_DIR=%~dp0\"\r\n");
        // JavaFX native libs live in <module>\lib\javafx if a Windows JavaFX
        // distribution was copied there. Use forward slashes in --module-path
        // so java on Windows doesn't choke on backslashes in CLI args.
        sb.append("set \"JAVAFX=%SCRIPT_DIR%lib\\javafx\"\r\n");
        sb.append("java --module-path \"%JAVAFX%\" ");
        sb.append("--add-modules javafx.controls,javafx.fxml ");
        sb.append("-Dfile.encoding=UTF-8 ");
        sb.append("-jar \"%SCRIPT_DIR%").append(relJar.toString().replace('/', '\\')).append("\" %*\r\n");
        sb.append("endlocal\r\n");
        Files.writeString(batPath, sb.toString(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** Create a Windows .lnk file via a PowerShell WScript.Shell snippet. */
    private static void createWindowsShortcut(Path lnk, Path target, Path workDir, String title)
            throws Exception {
        // Build a single-liner PowerShell command that creates the shortcut.
        // Backtick-quote is PS-style; we pass via -Command so no script file
        // needed.
        String escLnk     = lnk.toString().replace("\"", "`\"");
        String escTarget  = target.toString().replace("\"", "`\"");
        String escWorkDir = workDir.toString().replace("\"", "`\"");
        String escTitle   = title.replace("\"", "`\"");

        String ps =
            "$s=(New-Object -ComObject WScript.Shell).CreateShortcut(\"" + escLnk + "\");" +
            "$s.TargetPath=\"" + escTarget + "\";" +
            "$s.WorkingDirectory=\"" + escWorkDir + "\";" +
            "$s.Description=\"" + escTitle + "\";" +
            "$s.Save();";

        ProcessBuilder pb = new ProcessBuilder(
            "powershell", "-NoProfile", "-NonInteractive", "-Command", ps);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        if (!p.waitFor(10, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new java.io.IOException("PowerShell shortcut creation timed out");
        }
        if (p.exitValue() != 0) {
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new java.io.IOException("PowerShell failed (" + p.exitValue() + "): " + out);
        }
    }

    // -----------------------------------------------------------------
    // Common bits
    // -----------------------------------------------------------------

    private static Path resolveSourceRoot(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--root".equals(args[i]) || "-r".equals(args[i])) {
                return Paths.get(args[i + 1]).toAbsolutePath().normalize();
            }
        }
        String env = System.getenv("ARS_SUITE_HOME");
        if (env != null && !env.isBlank()) {
            return Paths.get(env).toAbsolutePath().normalize();
        }
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("j-hub/pom.xml"))) return cwd;
        if (cwd.getParent() != null && Files.exists(cwd.getParent().resolve("j-hub/pom.xml")))
            return cwd.getParent();
        throw new IllegalStateException(
            "Cannot find ARS Suite source root. Pass --root <path> or set ARS_SUITE_HOME.");
    }

    private static void depCheck() {
        info("Checking optional external dependencies…");
        boolean hasRigctl = which(WINDOWS ? "rigctl.exe" : "rigctl");
        boolean hasRotctl = which(WINDOWS ? "rotctl.exe" : "rotctl");
        boolean hasWsjtx  = which(WINDOWS ? "wsjtx.exe"  : "wsjtx");
        System.out.println("  " + (hasRigctl ? "[OK] " : "[-- ] ") + "Hamlib / rigctl"
            + (hasRigctl ? "" : "   — install Hamlib"));
        System.out.println("  " + (hasRotctl ? "[OK] " : "[-- ] ") + "Hamlib / rotctl"
            + (hasRotctl ? "" : "   — included with Hamlib"));
        System.out.println("  " + (hasWsjtx  ? "[OK] " : "[-- ] ") + "WSJT-X"
            + (hasWsjtx ? "" : "   — install from wsjt.sourceforge.io"));
        System.out.println();
        System.out.println("  (optional — the suite runs without these; j-hub's Dashboard");
        System.out.println("   has a re-check button after you install them.)");
    }

    private static boolean which(String cmd) {
        String finder = WINDOWS ? "where" : "which";
        try {
            Process p = new ProcessBuilder(finder, cmd).redirectErrorStream(true).start();
            p.waitFor(2, TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void banner() {
        System.out.println();
        System.out.println("  +==========================================+");
        System.out.println("  |      WM3J ARS Suite Installer            |");
        System.out.println("  +==========================================+");
        System.out.println();
    }
    private static void info(String s) { System.out.println("[info] " + s); }
    private static void warn(String s) { System.out.println("[warn] " + s); }
}
