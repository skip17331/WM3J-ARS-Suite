package com.jlog.support;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * IssueReporter — opens a pre-filled GitHub Issues page in the operator's
 * default browser so beta testers can file structured bug reports with one
 * click. Wired into each JavaFX module's <em>Help → Report an Issue…</em>
 * menu item.
 *
 * The pre-filled body includes the module name, version, OS, JVM, and
 * (when supplied) a hint at where the local log file lives so the
 * operator can attach it manually. For the cross-module diagnostic zip
 * containing every module's logs + config snapshot, the body points to
 * <em>J-Hub → Logging &amp; Data → Export Diagnostics</em>.
 *
 * <p>GitHub's "new issue" endpoint doesn't accept file uploads via URL
 * parameters; the operator drags-and-drops the diag bundle into the issue
 * once the page loads.
 */
public final class IssueReporter {

    /** Public GitHub repo where issues are filed. */
    public static final String REPO_URL = "https://github.com/skip17331/WM3J-ARS-Suite";

    private IssueReporter() {}

    /**
     * Build the pre-filled issue URL without opening anything. Useful when
     * the caller wants to show the URL in a dialog first (e.g. so a
     * headless operator can copy-paste it elsewhere).
     */
    public static String buildIssueUrl(String moduleName, String version, String logFilePath) {
        String title = "[" + safe(moduleName) + " v" + safe(version) + "] <one-line summary>";
        StringBuilder body = new StringBuilder(1024);
        body.append("### Module\n");
        body.append("- Name: ").append(safe(moduleName)).append('\n');
        body.append("- Version: ").append(safe(version)).append('\n');
        if (logFilePath != null && !logFilePath.isBlank()) {
            body.append("- Log file (please attach): `").append(logFilePath).append("`\n");
        }
        body.append('\n');
        body.append("### Environment\n");
        body.append("- OS: ").append(safe(System.getProperty("os.name", "")))
            .append(' ').append(safe(System.getProperty("os.version", ""))).append('\n');
        body.append("- Arch: ").append(safe(System.getProperty("os.arch", ""))).append('\n');
        body.append("- Java: ").append(safe(System.getProperty("java.version", "")))
            .append(" (").append(safe(System.getProperty("java.vendor", ""))).append(")\n");
        body.append('\n');
        body.append("### What I expected to happen\n\n\n");
        body.append("### What actually happened\n\n\n");
        body.append("### Steps to reproduce\n");
        body.append("1. \n2. \n3. \n\n");
        body.append("### Diagnostics bundle\n");
        body.append("If the issue spans more than one module, please attach the full\n");
        body.append("ARS Suite diagnostics zip:\n");
        body.append("J-Hub → **Logging & Data** → **Configuration Backup & Export** → **Export Diagnostics**\n");

        return REPO_URL + "/issues/new"
            + "?labels=bug"
            + "&title=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
            + "&body="  + URLEncoder.encode(body.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Generate the pre-filled URL and ask the OS to open it in the user's
     * default browser. Falls back silently if `java.awt.Desktop` isn't
     * available (headless environments, broken X11) — the caller is
     * expected to wrap this in something that can also show the URL in a
     * dialog if needed.
     *
     * Runs the actual browser launch on a daemon thread so we don't
     * stall the JavaFX Application Thread.
     */
    public static void openGitHubIssue(String moduleName, String version, String logFilePath) {
        String url = buildIssueUrl(moduleName, version, logFilePath);
        Thread t = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported()
                        && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    return;
                }
            } catch (Exception ignored) {
                // Fall through to the platform-specific fallbacks below.
            }
            // Linux often refuses Desktop.browse() under PipeWire/Wayland —
            // shell out to xdg-open / open / explorer as a last resort.
            String os = System.getProperty("os.name", "").toLowerCase();
            String[] cmd;
            if (os.contains("mac")) {
                cmd = new String[]{ "open", url };
            } else if (os.contains("win")) {
                cmd = new String[]{ "rundll32", "url.dll,FileProtocolHandler", url };
            } else {
                cmd = new String[]{ "xdg-open", url };
            }
            try { new ProcessBuilder(cmd).start(); }
            catch (Exception ignored) { /* best effort — caller can show the URL */ }
        }, "issue-reporter-browser");
        t.setDaemon(true);
        t.start();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
