package com.hamradio.ars.installer;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The installer's file-writing helpers: what ends up on the operator's desktop and in their menus. */
class InstallerTest {
    @TempDir Path dir;

    @Test
    void desktopEntryLaunchesTheScriptFromItsFolder() throws Exception {
        Path script = dir.resolve("My Suite/ars-fx/ars-fx.sh");
        Path entry = dir.resolve("ars-fx-map.desktop");
        Installer.writeDesktopEntry(entry, "J-Map", "map", script, "--module map", "/icons/map.png", "HamRadio;");
        List<String> lines = Files.readAllLines(entry);
        assertEquals("[Desktop Entry]", lines.get(0));
        assertTrue(lines.contains("Name=J-Map"));
        assertTrue(lines.contains("Exec=bash \"" + script + "\" --module map"), "a path with a space stays one argument");
        assertTrue(lines.contains("Path=" + script.getParent()));
        assertTrue(lines.contains("Icon=/icons/map.png"));
        assertTrue(lines.contains("Terminal=false"));
    }

    @Test
    void desktopEntryWithoutArgsOrIcon() throws Exception {
        Path entry = dir.resolve("a.desktop");
        Installer.writeDesktopEntry(entry, "ARS Suite", "c", dir.resolve("ars-fx.sh"), " ", null, "X;");
        String text = new String(Files.readAllBytes(entry), StandardCharsets.UTF_8);
        assertTrue(text.contains("Exec=bash \"" + dir.resolve("ars-fx.sh") + "\"\n"), "no trailing argument");
        assertFalse(text.contains("Icon="));
    }

    @Test
    void escapesXmlForTheMacPlist() {
        assertEquals("Q&amp;A &lt;J-Log&gt; &quot;x&quot; it&apos;s", Installer.escapeXml("Q&A <J-Log> \"x\" it's"));
    }

    @Test
    void stripsStrayQuotesFromWindowsPaths() {
        assertEquals("C:\\ARS Suite\\", Installer.cleanPathArg("\"C:\\ARS Suite\\\""));
        assertEquals("C:\\dir\\", Installer.cleanPathArg("C:\\dir\\\""), "%~dp0 escapes the closing quote");
        assertEquals("", Installer.cleanPathArg(null));
    }

    @Test
    void findsTheSuiteFromArgsEnvOrTheFolderItRunsIn() throws Exception {
        Files.createDirectories(dir.resolve("suite/ars-fx"));
        Files.createFile(dir.resolve("suite/ars-fx/pom.xml"));
        Path suite = dir.resolve("suite");
        assertEquals(suite, Installer.resolveSourceRoot(new String[]{"--root", "\"" + suite + "\""}, null, dir));
        assertEquals(suite, Installer.resolveSourceRoot(new String[0], suite.toString(), dir));
        assertEquals(suite, Installer.resolveSourceRoot(new String[0], null, suite), "run from the checkout");
        assertEquals(suite, Installer.resolveSourceRoot(new String[0], null, suite.resolve("installer")), "run from installer/");
        assertThrows(IllegalStateException.class, () -> Installer.resolveSourceRoot(new String[0], " ", dir));
    }

    @Test
    void resolvesTheJarTheManifestNamesOrAPlatformSibling() throws Exception {
        Path target = Files.createDirectories(dir.resolve("ars-fx/target"));
        Files.createFile(target.resolve("original-ars-fx-linux.jar"));
        Files.createFile(target.resolve("ars-fx-windows.jar"));
        assertEquals(target.resolve("ars-fx-windows.jar"), Installer.resolveJar(dir, "ars-fx/target/ars-fx-linux.jar"),
                "missing jar: a built sibling, never original-*");
        Files.createFile(target.resolve("ars-fx-linux.jar"));
        assertEquals(target.resolve("ars-fx-linux.jar"), Installer.resolveJar(dir, "ars-fx/target/ars-fx-linux.jar"));
        assertNull(Installer.resolveJar(dir, "nothing/target/x-1.0.jar"));
    }

    @Test
    void prunesOnlyOurOwnStaleShortcuts() throws Exception {
        for (String n : Arrays.asList("ars-fx.desktop", "ars-fx-old.desktop", "other-app.desktop")) Files.createFile(dir.resolve(n));
        List<String> pruned = new ArrayList<>();
        Installer.pruneStale(dir, "ars-fx*.desktop", new HashSet<>(Arrays.asList("ars-fx.desktop")), pruned);
        assertTrue(Files.exists(dir.resolve("ars-fx.desktop")));
        assertFalse(Files.exists(dir.resolve("ars-fx-old.desktop")));
        assertTrue(Files.exists(dir.resolve("other-app.desktop")), "another program's shortcut is left alone");
        assertEquals(1, pruned.size());
    }

    @Test
    void windowsIconIsA256PixelPngInAnIcoHeader() throws Exception {
        BufferedImage src = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(10, 10, 0xFF336699);
        Path png = dir.resolve("icon.png"), ico = dir.resolve("icon.ico");
        ImageIO.write(src, "png", png.toFile());
        Installer.pngToIco(png, ico);
        byte[] b = Files.readAllBytes(ico);
        ByteBuffer h = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, h.getShort(0));                 // reserved
        assertEquals(1, h.getShort(2));                 // type: icon
        assertEquals(1, h.getShort(4));                 // one image
        assertEquals(0, b[6]);                          // width byte 0 = 256
        assertEquals(b.length - 22, h.getInt(14), "size of the embedded PNG");
        assertEquals(22, h.getInt(18), "the PNG follows the 22-byte header");
        BufferedImage inner = ImageIO.read(new ByteArrayInputStream(Arrays.copyOfRange(b, 22, b.length)));
        assertEquals(256, inner.getWidth());
        assertEquals(256, inner.getHeight());
    }

    @Test
    void menuFolderIsOursAndNeverContests() throws Exception {
        Path menus = dir.resolve(".config/menus/applications-merged");
        Files.createDirectories(menus);
        Path contest = menus.resolve("ars-contest.menu");
        Files.writeString(contest, "contest-menu");
        Files.writeString(menus.resolve("ars-amateur-radio.menu"), "old");              // the Suite's own earlier file
        Path dirs = dir.resolve(".local/share/desktop-directories");
        Files.createDirectories(dirs);
        Files.writeString(dirs.resolve("ars-contest.directory"), "c");
        Files.writeString(dirs.resolve("ars-amateur-radio.directory"), "old");

        Installer.writeAmateurRadioMenu(dir, dir.resolve(".local/share/icons"));

        String xml = Files.readString(menus.resolve("ars-suite.menu"));
        assertTrue(xml.contains("<Name>Amateur Radio</Name>") && xml.contains("<Name>ARS Suite</Name>"));
        assertTrue(xml.contains("X-ARS-Suite") && !xml.contains("X-ARS-Contest"));
        assertTrue(xml.contains("ars-suite-amateur-radio.directory") && xml.contains("ars-suite.directory"));
        for (String m : new String[] {"cinnamon-applications-merged", "plasma-applications-merged", "kf5-applications-merged"})
            assertTrue(Files.exists(dir.resolve(".config/menus").resolve(m).resolve("ars-suite.menu")), m);
        assertEquals("contest-menu", Files.readString(contest), "ARS Contest's menu file is left alone");
        assertTrue(Files.exists(dirs.resolve("ars-contest.directory")));
        assertFalse(Files.exists(menus.resolve("ars-amateur-radio.menu")), "the superseded Suite menu file is removed");
        assertFalse(Files.exists(dirs.resolve("ars-amateur-radio.directory")));
        assertTrue(Files.readString(dirs.resolve("ars-suite.directory")).contains("Name=ARS Suite"));
    }

    @Test
    void oldStartMenuFolderIsClearedOnlyWhenItHoldsJustOurShortcuts() throws Exception {
        Path old = dir.resolve("Programs/ARS Suite");
        Files.createDirectories(old);
        Files.writeString(old.resolve("J-Hub.lnk"), "x");
        Installer.migrateOldStartMenuFolder(old, new ArrayList<>());
        assertFalse(Files.exists(old));

        Files.createDirectories(old);
        Files.writeString(old.resolve("J-Hub.lnk"), "x");
        Files.writeString(old.resolve("notes.txt"), "mine");
        Installer.migrateOldStartMenuFolder(old, new ArrayList<>());
        assertTrue(Files.exists(old.resolve("notes.txt")), "anything that is not a Suite shortcut stays");
    }
}
