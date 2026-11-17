package com.jlog.support;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The pre-filled GitHub issue link (built only; nothing is opened). */
class IssueReporterTest {
    private static String param(String url, String name) {
        for (String p : url.substring(url.indexOf('?') + 1).split("&"))
            if (p.startsWith(name + "=")) return URLDecoder.decode(p.substring(name.length() + 1), StandardCharsets.UTF_8);
        return null;
    }

    @Test
    void linkOpensANewBugWithModuleVersionAndLogFile() {
        String url = IssueReporter.buildIssueUrl("J-Log", "1.13.16", "/home/op/.j-log/j-log.log");
        assertTrue(url.startsWith(IssueReporter.REPO_URL + "/issues/new?labels=bug&title="));
        assertEquals("[J-Log v1.13.16] <one-line summary>", param(url, "title"));
        String body = param(url, "body");
        assertTrue(body.contains("- Name: J-Log"));
        assertTrue(body.contains("- Log file (please attach): `/home/op/.j-log/j-log.log`"));
        assertTrue(body.contains("- Java: " + System.getProperty("java.version")));
        assertFalse(url.contains(" "), "everything is URL-encoded");
    }

    @Test
    void noLogFileLineWithoutAPathAndNullsAreSafe() {
        String url = IssueReporter.buildIssueUrl(null, null, "  ");
        assertEquals("[ v] <one-line summary>", param(url, "title"));
        assertFalse(param(url, "body").contains("Log file"));
    }
}
