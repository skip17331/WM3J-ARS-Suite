package com.hamradio.ars.installer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * ARS Suite installer — Linux, macOS, and Windows.
 *
 * Manifest-driven: reads {@code manifest.json} from inside the installer jar,
 * walks each module, and for every one that has a built jar it wires up
 * platform-appropriate desktop integration:
 *
 * <ul>
 *   <li><b>Linux</b> — writes {@code .desktop} entries to
 *       {@code ~/.local/share/applications/}, copies icons to
 *       {@code ~/.local/share/icons/}, chmods the launch scripts.</li>
 *   <li><b>macOS</b> — generates a {@code .app} bundle for each module
 *       under {@code ~/Applications/}. The bundle's {@code Contents/MacOS/}
 *       wrapper script execs the module's existing {@code .sh} launcher,
 *       so source-tree updates propagate without re-running the installer.</li>
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
    private static final String OS_NAME =
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean WINDOWS = OS_NAME.contains("win");
    private static final boolean MAC     = OS_NAME.contains("mac") || OS_NAME.contains("darwin");

    private Installer() {}

    public static void main(String[] args) throws Exception {
        Path sourceRoot = resolveSourceRoot(args);
        Path home       = Paths.get(System.getProperty("user.home"));

        banner();
        info("Platform:    " + (WINDOWS ? "Windows" : MAC ? "macOS" : "Linux"));
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
        List<String> pruned    = new ArrayList<>();

        if      (WINDOWS) installWindows(sourceRoot, modules, installed, skipped, pruned);
        else if (MAC)     installMac    (sourceRoot, home, modules, installed, skipped, pruned);
        else              installLinux  (sourceRoot, home, modules, installed, skipped, pruned);

        System.out.println();
        info("Installed shortcuts:");
        for (String s : installed) System.out.println("  + " + s);
        if (!pruned.isEmpty()) {
            System.out.println();
            info("Removed stale shortcuts from earlier installs:");
            for (String s : pruned) System.out.println("  x " + s);
        }
        if (!skipped.isEmpty()) {
            System.out.println();
            warn("Skipped (build the module first, then re-run installer):");
            for (String s : skipped) System.out.println("  - " + s);
        }

        System.out.println();
        depCheck();

        System.out.println();
        info("Done. Launch via your system menu, or directly:");
        if (WINDOWS) {
            System.out.println("    " + sourceRoot.resolve("ars-fx\\ars-fx.bat") + "              (docked)");
            System.out.println("    " + sourceRoot.resolve("ars-fx\\ars-fx.bat") + " --module log  (loose)");
        } else if (MAC) {
            System.out.println("    open ~/Applications/ARS\\ Suite.app");
            System.out.println("    " + sourceRoot.resolve("ars-fx/ars-fx.sh") + "              (docked)");
            System.out.println("    " + sourceRoot.resolve("ars-fx/ars-fx.sh") + " --module log  (loose)");
        } else {
            System.out.println("    " + sourceRoot.resolve("ars-fx/ars-fx.sh") + "              (docked)");
            System.out.println("    " + sourceRoot.resolve("ars-fx/ars-fx.sh") + " --module log  (loose)");
        }
    }

    // -----------------------------------------------------------------
    // Linux installer path
    // -----------------------------------------------------------------

    private static void installLinux(Path sourceRoot, Path home,
                                     JsonArray modules,
                                     List<String> installed, List<String> skipped,
                                     List<String> pruned) throws Exception {
        Path appsDir = home.resolve(".local/share/applications");
        Path iconDir = home.resolve(".local/share/icons");
        Files.createDirectories(appsDir);
        Files.createDirectories(iconDir);

        // Track what we write this run so we can prune suite shortcuts left by
        // earlier installs (e.g. the old per-program ars-j-*.desktop entries).
        Set<String> keptDesktops = new HashSet<>();
        Set<String> keptIcons    = new HashSet<>();

        for (var el : modules) {
            JsonObject m = el.getAsJsonObject();
            String name   = m.get("name").getAsString();
            String title  = m.get("title").getAsString();
            String launch = m.get("launch").getAsString();
            String jar    = m.get("jar").getAsString();
            String iconR  = m.has("icon") ? m.get("icon").getAsString() : null;
            String cats   = m.has("categories") ? m.get("categories").getAsString() : "Utility;";
            String cmt    = m.has("comment") ? m.get("comment").getAsString() : title;
            String args   = m.has("args") ? m.get("args").getAsString() : null;

            Path jarPath    = resolveJar(sourceRoot, jar);
            Path launchPath = sourceRoot.resolve(launch);
            if (jarPath == null)           { skipped.add(name + " (missing build: " + jar + ")");    continue; }
            if (!Files.exists(launchPath)) { skipped.add(name + " (missing launch: " + launch + ")"); continue; }

            try { launchPath.toFile().setExecutable(true, false); } catch (Exception ignored) {}

            String iconField = null;
            if (iconR != null) {
                Path src = sourceRoot.resolve(iconR);
                if (Files.exists(src)) {
                    Path dst = iconDir.resolve("ars-" + name + ".png");
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    iconField = dst.toString();
                    keptIcons.add(dst.getFileName().toString());
                }
            }

            Path desktop = appsDir.resolve("ars-" + name + ".desktop");
            writeDesktopEntry(desktop, title, cmt, launchPath, args, iconField, cats);
            keptDesktops.add(desktop.getFileName().toString());
            installed.add(name + " → " + desktop);
        }

        // Prune stale suite shortcuts/icons, but ONLY ones we own: the old per-program
        // entries from the pre-ars-fx installer (LEGACY_NAMES), plus any ars-fx* file
        // this run did not write. Never the broad ars-* glob — that would clobber
        // unrelated third-party apps (e.g. the standalone ARS Plugin Builder).
        for (String n : LEGACY_NAMES) {
            removeIfPresent(appsDir.resolve("ars-" + n + ".desktop"), pruned);
            removeIfPresent(iconDir.resolve("ars-" + n + ".png"),     pruned);
        }
        pruneStale(appsDir, "ars-fx*.desktop", keptDesktops, pruned);
        pruneStale(iconDir, "ars-fx*.png",     keptIcons,    pruned);
    }

    /** Shortcut/icon basenames written by the old per-program installer (pre-ars-fx), so the new
     *  unified app can clear them. Deliberately explicit — NOT a glob — so unrelated ars-* apps survive. */
    private static final Set<String> LEGACY_NAMES = Set.of(
        "j-hub", "j-log", "j-map", "j-digi", "j-bridge", "j-sat", "j-vault", "j-learn", "morse-trainer");

    private static void removeIfPresent(Path p, List<String> pruned) {
        try { if (Files.deleteIfExists(p)) pruned.add(p.toString()); } catch (Exception ignored) {}
    }

    /** Delete files matching {@code glob} in {@code dir} whose names are not in {@code keep}. Used to
     *  clear our own (ars-fx*) shortcuts that a later run no longer writes. */
    private static void pruneStale(Path dir, String glob, Set<String> keep, List<String> pruned) {
        if (!Files.isDirectory(dir)) return;
        try (var ds = Files.newDirectoryStream(dir, glob)) {
            for (Path p : ds) {
                if (keep.contains(p.getFileName().toString())) continue;
                try { Files.deleteIfExists(p); pruned.add(p.toString()); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static void writeDesktopEntry(Path path, String title, String comment,
                                          Path launchScript, String args, String iconPath, String categories)
            throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[Desktop Entry]\n");
        sb.append("Type=Application\n");
        sb.append("Version=1.0\n");
        sb.append("Name=").append(title).append('\n');
        sb.append("Comment=").append(comment).append('\n');
        sb.append("Exec=bash \"").append(launchScript).append('"');
        if (args != null && !args.isBlank()) sb.append(' ').append(args);
        sb.append('\n');
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
    // macOS installer path
    // -----------------------------------------------------------------
    //
    // We plant a .app bundle per module in ~/Applications/.  Each bundle's
    // Contents/MacOS/<name> is a thin shell wrapper that execs the
    // module's existing .sh launcher in the source checkout, so updates
    // (git pull, mvn rebuild) propagate automatically — no re-install.

    private static void installMac(Path sourceRoot, Path home,
                                   JsonArray modules,
                                   List<String> installed, List<String> skipped,
                                   List<String> pruned) throws Exception {
        Path appsDir = home.resolve("Applications");
        Files.createDirectories(appsDir);

        for (var el : modules) {
            JsonObject m = el.getAsJsonObject();
            String name   = m.get("name").getAsString();
            String title  = m.get("title").getAsString();
            String launch = m.get("launch").getAsString();
            String jar    = m.get("jar").getAsString();
            String iconR  = m.has("icon")    ? m.get("icon").getAsString()    : null;
            String cmt    = m.has("comment") ? m.get("comment").getAsString() : title;
            String args   = m.has("args")    ? m.get("args").getAsString()    : null;

            Path jarPath    = resolveJar(sourceRoot, jar);
            Path launchPath = sourceRoot.resolve(launch);
            if (jarPath == null)           { skipped.add(name + " (missing build: " + jar + ")");    continue; }
            if (!Files.exists(launchPath)) { skipped.add(name + " (missing launch: " + launch + ")"); continue; }

            try { launchPath.toFile().setExecutable(true, false); } catch (Exception ignored) {}

            // Bundle layout:
            //   ~/Applications/<Title>.app/
            //       Contents/
            //           Info.plist
            //           MacOS/<name>            (executable shell wrapper)
            //           Resources/icon.png      (PNG icon if available)
            Path appBundle = appsDir.resolve(title + ".app");
            Path contents  = appBundle.resolve("Contents");
            Path macosDir  = contents.resolve("MacOS");
            Path resDir    = contents.resolve("Resources");
            Files.createDirectories(macosDir);
            Files.createDirectories(resDir);

            // Copy icon if present (macOS Finder displays PNG icons via
            // CFBundleIconFile since 10.7).  Real .icns generation needs
            // iconutil + macOS-only tooling; PNG is good enough for now.
            String iconFile = null;
            if (iconR != null) {
                Path src = sourceRoot.resolve(iconR);
                if (Files.exists(src)) {
                    Path dst = resDir.resolve("icon.png");
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    iconFile = "icon.png";
                }
            }

            // Info.plist — minimal but valid LSUIElement=false bundle.
            String bundleId = "com.hamradio.ars." + name.replace("-", "");
            writePlist(contents.resolve("Info.plist"),
                       name, title, cmt, bundleId, iconFile);

            // Launcher script under Contents/MacOS/.  Must be executable
            // and match CFBundleExecutable in Info.plist.
            Path launcher = macosDir.resolve(name);
            writeMacLauncher(launcher, launchPath, args);

            installed.add(name + " → " + appBundle);
        }

        // Prune legacy per-program bundles from earlier installs (old "WM3J …" naming).
        if (Files.isDirectory(appsDir)) {
            try (var ds = Files.newDirectoryStream(appsDir, "WM3J *.app")) {
                for (Path p : ds) {
                    try { deleteRecursive(p); pruned.add(p.toString()); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }

    /** Recursively delete a directory tree (used to remove a stale .app bundle). */
    private static void deleteRecursive(Path root) throws java.io.IOException {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            });
        }
    }

    private static void writePlist(Path plistPath,
                                   String executableName, String displayName,
                                   String comment, String bundleId,
                                   String iconFile) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" ")
          .append("\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        sb.append("  <key>CFBundleName</key><string>").append(escapeXml(displayName)).append("</string>\n");
        sb.append("  <key>CFBundleDisplayName</key><string>").append(escapeXml(displayName)).append("</string>\n");
        sb.append("  <key>CFBundleIdentifier</key><string>").append(escapeXml(bundleId)).append("</string>\n");
        sb.append("  <key>CFBundleVersion</key><string>1.0</string>\n");
        sb.append("  <key>CFBundleShortVersionString</key><string>1.0</string>\n");
        sb.append("  <key>CFBundleExecutable</key><string>").append(escapeXml(executableName)).append("</string>\n");
        sb.append("  <key>CFBundlePackageType</key><string>APPL</string>\n");
        sb.append("  <key>CFBundleInfoDictionaryVersion</key><string>6.0</string>\n");
        sb.append("  <key>NSHighResolutionCapable</key><true/>\n");
        sb.append("  <key>LSMinimumSystemVersion</key><string>10.15</string>\n");
        if (iconFile != null) {
            sb.append("  <key>CFBundleIconFile</key><string>")
              .append(escapeXml(iconFile)).append("</string>\n");
        }
        if (comment != null && !comment.isBlank()) {
            sb.append("  <key>NSHumanReadableCopyright</key><string>")
              .append(escapeXml(comment)).append("</string>\n");
        }
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        Files.writeString(plistPath, sb.toString(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeMacLauncher(Path launcherPath, Path moduleLaunchScript, String args)
            throws java.io.IOException {
        // Launchd / Finder set CWD to /, so we always cd into the module
        // directory before delegating.  Logs go to ~/Library/Logs/ARS-Suite/
        // because Finder swallows stdout/stderr from .app bundles.  Finder passes
        // no args to a .app, so any per-shortcut args are baked in here.
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("# Auto-generated by ARS Suite installer. Do not edit — re-run install.sh.\n");
        sb.append("LOG_DIR=\"$HOME/Library/Logs/ARS-Suite\"\n");
        sb.append("mkdir -p \"$LOG_DIR\"\n");
        sb.append("LOG_FILE=\"$LOG_DIR/").append(launcherPath.getFileName()).append(".log\"\n");
        sb.append("cd \"").append(moduleLaunchScript.getParent()).append("\" || exit 1\n");
        sb.append("exec bash \"").append(moduleLaunchScript).append("\" ");
        if (args != null && !args.isBlank()) sb.append(args).append(' ');
        sb.append("\"$@\" >>\"$LOG_FILE\" 2>&1\n");
        Files.writeString(launcherPath, sb.toString(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try { launcherPath.toFile().setExecutable(true, false); } catch (Exception ignored) {}
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // -----------------------------------------------------------------
    // Windows installer path
    // -----------------------------------------------------------------

    private static void installWindows(Path sourceRoot, JsonArray modules,
                                       List<String> installed, List<String> skipped,
                                       List<String> pruned) throws Exception {
        // Start Menu folder: %APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            warn("APPDATA is not set — falling back to user.home\\AppData\\Roaming");
            appData = System.getProperty("user.home") + "\\AppData\\Roaming";
        }
        Path startMenu = Paths.get(appData,
            "Microsoft", "Windows", "Start Menu", "Programs", "ARS Suite");
        Files.createDirectories(startMenu);

        // The whole "ARS Suite" Start-Menu folder is ours, so any .lnk this run
        // does not write is a leftover from an earlier install — prune it below.
        Set<String> keptLnks = new HashSet<>();

        for (var el : modules) {
            JsonObject m = el.getAsJsonObject();
            String name   = m.get("name").getAsString();
            String title  = m.get("title").getAsString();
            String launch = m.get("launch").getAsString();      // unix path in manifest
            String jar    = m.get("jar").getAsString();
            String args   = m.has("args") ? m.get("args").getAsString() : null;

            Path jarPath = resolveJar(sourceRoot, jar);
            if (jarPath == null) {
                skipped.add(name + " (missing build: " + jar + ")");
                continue;
            }

            // We generate a sibling .bat for each .sh. The .bat mirrors the
            // shell script's behavior: set module dir as cwd, launch the jar
            // with the javafx module path in lib/javafx.
            Path shPath  = sourceRoot.resolve(launch);
            Path batPath = siblingBat(sourceRoot, shPath, jarPath);
            if (!Files.exists(batPath)) writeBatLauncher(batPath);

            // Generate a Windows .ico from the module's PNG icon (the repo
            // only ships PNGs; .lnk needs .ico). Best-effort: a bad/absent
            // image just yields a shortcut with the default icon.
            Path iconIco = null;
            if (m.has("icon") && !m.get("icon").isJsonNull()) {
                Path png = sourceRoot.resolve(m.get("icon").getAsString());
                if (Files.exists(png)) {
                    try {
                        iconIco = batPath.resolveSibling(name + ".ico");
                        pngToIco(png, iconIco);
                    } catch (Exception ex) {
                        warn("icon for " + name + " skipped: " + ex.getMessage());
                        iconIco = null;
                    }
                }
            }

            Path shortcut = startMenu.resolve(title + ".lnk");
            createWindowsShortcut(shortcut, batPath, batPath.getParent(), title, args, iconIco);
            keptLnks.add(shortcut.getFileName().toString());
            installed.add(name + " → " + shortcut);
        }

        // Prune stale Start-Menu shortcuts from an earlier install.
        pruneStale(startMenu, "*.lnk", keptLnks, pruned);
    }

    /** Resolve a module's built jar from its manifest hint in a
     *  version-agnostic way. The manifest pins a concrete filename (e.g.
     *  {@code j-hub/target/j-hub-1.5.0.jar}) but a version bump must never
     *  strand the installer, so if that exact file is absent we glob the
     *  module's {@code target/} dir for {@code <artifact>-*.jar}, preferring
     *  the runnable shaded/fat uber jar and skipping the thin
     *  {@code original-}/{@code -sources}/{@code -javadoc} artifacts. Returns
     *  the resolved jar, or {@code null} if no runnable jar is present. */
    private static Path resolveJar(Path sourceRoot, String jarHint) {
        Path hint = sourceRoot.resolve(jarHint);
        if (Files.exists(hint)) return hint;                 // exact manifest match

        Path targetDir = hint.getParent();
        if (targetDir == null || !Files.isDirectory(targetDir)) return null;

        // Artifact prefix = filename up to the version, e.g.
        // "j-hub-1.5.0.jar" -> "j-hub", "morse-trainer-1.0.0.jar" -> "morse-trainer".
        String fileName = hint.getFileName().toString();
        String prefix   = fileName.replaceFirst("-\\d.*$", "");

        List<Path> candidates = new ArrayList<>();
        try (var ds = Files.newDirectoryStream(targetDir, prefix + "-*.jar")) {
            for (Path p : ds) {
                String n = p.getFileName().toString();
                if (n.startsWith("original-") || n.endsWith("-sources.jar")
                        || n.endsWith("-javadoc.jar")) continue;
                candidates.add(p);
            }
        } catch (java.io.IOException e) {
            return null;
        }
        if (candidates.isEmpty()) return null;

        // Prefer the runnable uber jar (shade adds a -shaded/-fat classifier).
        for (Path p : candidates) {
            String n = p.getFileName().toString();
            if (n.endsWith("-shaded.jar") || n.endsWith("-fat.jar")
                    || n.endsWith("-jar-with-dependencies.jar")) return p;
        }
        // Otherwise the highest-versioned plain jar.
        candidates.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return candidates.get(candidates.size() - 1);
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

    private static void writeBatLauncher(Path batPath) throws java.io.IOException {
        // Artifact id == module dir name for every suite module, so the jar
        // is target\<mod>-<version>.jar.
        String mod = batPath.getParent().getFileName().toString();
        StringBuilder sb = new StringBuilder();
        sb.append("@echo off\r\n");
        sb.append("setlocal\r\n");
        sb.append("set \"SCRIPT_DIR=%~dp0\"\r\n");
        // JavaFX is bundled into every module's fat jar (OS-classifier Maven
        // deps + a non-Application Launcher Main-Class), so a plain
        // `java -jar` is all that is needed — no --module-path / lib/javafx.
        // Prefer the runnable fat/uber jar (shade adds a -shaded/-fat
        // classifier), else the in-place shaded main jar (skip thin
        // original-/sources/javadoc) so a version bump never breaks this.
        sb.append("set \"JAR=\"\r\n");
        sb.append("for /f \"delims=\" %%F in ('dir /b /a-d /o-d \"%SCRIPT_DIR%target\\")
          .append(mod).append("-*-shaded.jar\" \"%SCRIPT_DIR%target\\")
          .append(mod).append("-*-fat.jar\" \"%SCRIPT_DIR%target\\")
          .append(mod)
          .append("-*-jar-with-dependencies.jar\" 2^>nul') do if not defined JAR set \"JAR=%SCRIPT_DIR%target\\%%F\"\r\n");
        sb.append("if not defined JAR for /f \"delims=\" %%F in ('dir /b /a-d /o-d \"%SCRIPT_DIR%target\\")
          .append(mod)
          .append("-*.jar\" 2^>nul ^| findstr /v /i \"original- sources javadoc\"') do if not defined JAR set \"JAR=%SCRIPT_DIR%target\\%%F\"\r\n");
        sb.append("if not defined JAR (\r\n");
        sb.append("  echo Error: ").append(mod)
          .append(" jar not found in \"%SCRIPT_DIR%target\" - build it:  mvn -DskipTests -f \"%SCRIPT_DIR%pom.xml\" package\r\n");
        sb.append("  exit /b 1\r\n");
        sb.append(")\r\n");
        // cd into the module dir so logback's logs/ and per-module config
        // files resolve relative to <mod>/, not whatever cwd the caller had.
        sb.append("cd /d \"%SCRIPT_DIR%\"\r\n");
        sb.append("java -Dfile.encoding=UTF-8 -jar \"%JAR%\" %*\r\n");
        sb.append("endlocal\r\n");
        Files.writeString(batPath, sb.toString(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** Create a Windows .lnk file via a PowerShell WScript.Shell snippet. */
    private static void createWindowsShortcut(Path lnk, Path target, Path workDir,
                                              String title, String args, Path iconFile)
            throws Exception {
        // Build a single-liner PowerShell command that creates the shortcut.
        // Backtick-quote is PS-style; we pass via -Command so no script file
        // needed.
        String escLnk     = lnk.toString().replace("\"", "`\"");
        String escTarget  = target.toString().replace("\"", "`\"");
        String escWorkDir = workDir.toString().replace("\"", "`\"");
        String escTitle   = title.replace("\"", "`\"");
        String argLine    = "";
        if (args != null && !args.isBlank()) {
            String escArgs = args.replace("\"", "`\"");
            argLine = "$s.Arguments=\"" + escArgs + "\";";
        }
        String iconLine   = "";
        if (iconFile != null && Files.exists(iconFile)) {
            String escIcon = iconFile.toString().replace("\"", "`\"");
            iconLine = "$s.IconLocation=\"" + escIcon + ",0\";";
        }

        String ps =
            "$s=(New-Object -ComObject WScript.Shell).CreateShortcut(\"" + escLnk + "\");" +
            "$s.TargetPath=\"" + escTarget + "\";" +
            "$s.WorkingDirectory=\"" + escWorkDir + "\";" +
            "$s.Description=\"" + escTitle + "\";" +
            argLine +
            iconLine +
            "$s.Save();";

        // Pass via -EncodedCommand (base64 UTF-16LE). Java's ProcessBuilder
        // mangles embedded double quotes when it builds the Windows command
        // line, so a -Command string arrives at PowerShell with the path
        // quotes stripped and spaces (Start Menu, ARS Suite) break the parser.
        // Encoding sidesteps all command-line quoting entirely.
        String encoded = Base64.getEncoder()
            .encodeToString(ps.getBytes(StandardCharsets.UTF_16LE));

        ProcessBuilder pb = new ProcessBuilder(
            "powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded);
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

    /**
     * Convert a PNG to a single-image Windows {@code .ico}. The repo ships
     * 512px+ PNGs; Explorer only reliably renders PNG-compressed icon entries
     * up to 256x256, so we downscale to 256. Vista+ accepts a raw PNG payload
     * inside the ICO container, so no BMP/DIB encoding is needed.
     */
    private static void pngToIco(Path png, Path ico) throws java.io.IOException {
        BufferedImage src = ImageIO.read(png.toFile());
        if (src == null) throw new java.io.IOException("unreadable image: " + png);

        final int size = 256;
        BufferedImage dst = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();

        ByteArrayOutputStream pngBuf = new ByteArrayOutputStream();
        ImageIO.write(dst, "png", pngBuf);
        byte[] payload = pngBuf.toByteArray();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // ICONDIR (6 bytes)
        writeLE16(out, 0);                 // reserved
        writeLE16(out, 1);                 // type = icon
        writeLE16(out, 1);                 // image count
        // ICONDIRENTRY (16 bytes) — width/height byte 0 means 256
        out.write(0);                      // bWidth
        out.write(0);                      // bHeight
        out.write(0);                      // bColorCount (no palette)
        out.write(0);                      // bReserved
        writeLE16(out, 1);                 // wPlanes
        writeLE16(out, 32);                // wBitCount
        writeLE32(out, payload.length);    // dwBytesInRes
        writeLE32(out, 22);                // dwImageOffset (6 + 16)
        out.write(payload, 0, payload.length);

        Files.write(ico, out.toByteArray(),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeLE16(OutputStream o, int v) throws java.io.IOException {
        o.write(v & 0xFF);
        o.write((v >>> 8) & 0xFF);
    }

    private static void writeLE32(OutputStream o, int v) throws java.io.IOException {
        o.write(v & 0xFF);
        o.write((v >>> 8) & 0xFF);
        o.write((v >>> 16) & 0xFF);
        o.write((v >>> 24) & 0xFF);
    }

    // -----------------------------------------------------------------
    // Common bits
    // -----------------------------------------------------------------

    /**
     * Strip wrapping/stray double quotes from a path argument. On Windows a
     * trailing backslash before the closing quote — e.g. {@code --root
     * "C:\dir\"} (and %~dp0 always ends in '\') — escapes the quote, so the
     * JVM receives a literal trailing {@code "} and Paths.get throws
     * InvalidPathException. Quotes are never valid in a Windows path, so it's
     * safe to drop them.
     */
    private static String cleanPathArg(String s) {
        if (s == null) return "";
        s = s.trim();
        while (s.startsWith("\"")) s = s.substring(1);
        while (s.endsWith("\""))   s = s.substring(0, s.length() - 1);
        return s.trim();
    }

    private static Path resolveSourceRoot(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--root".equals(args[i]) || "-r".equals(args[i])) {
                return Paths.get(cleanPathArg(args[i + 1])).toAbsolutePath().normalize();
            }
        }
        String env = System.getenv("ARS_SUITE_HOME");
        if (env != null && !env.isBlank()) {
            return Paths.get(cleanPathArg(env)).toAbsolutePath().normalize();
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
        boolean hasWsjtx  = which(WINDOWS ? "wsjtx.exe"  : MAC ? "wsjtx_cmd" : "wsjtx");
        String hamlibHint = MAC     ? "brew install hamlib"
                           : WINDOWS ? "download from hamlib.sourceforge.io"
                           :           "install Hamlib";
        String wsjtxHint  = MAC     ? "brew install --cask wsjtx"
                           : WINDOWS ? "install from wsjt.sourceforge.io"
                           :           "install from wsjt.sourceforge.io";
        System.out.println("  " + (hasRigctl ? "[OK] " : "[-- ] ") + "Hamlib / rigctl"
            + (hasRigctl ? "" : "   — " + hamlibHint));
        System.out.println("  " + (hasRotctl ? "[OK] " : "[-- ] ") + "Hamlib / rotctl"
            + (hasRotctl ? "" : "   — included with Hamlib"));
        System.out.println("  " + (hasWsjtx  ? "[OK] " : "[-- ] ") + "WSJT-X"
            + (hasWsjtx ? "" : "   — " + wsjtxHint));
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
