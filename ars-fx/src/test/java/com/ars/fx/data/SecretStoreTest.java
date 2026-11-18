package com.ars.fx.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Passwords go to the system keyring when there is one, and stay in the file (as before) when there is not. */
class SecretStoreTest {
    /** A keyring in memory. */
    static final class Fake implements SecretStore.Backend {
        final Map<String, String> items = new HashMap<>();
        boolean up = true; int gets;
        @Override public boolean available() { return up; }
        @Override public String get(String a) { gets++; return items.get(a); }
        @Override public boolean put(String a, String v) { items.put(a, v); return true; }
        @Override public boolean delete(String a) { items.remove(a); return true; }
    }

    private String oldHome;
    private Fake fake;
    private Path home;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        oldHome = System.getProperty("user.home");
        home = tmp;
        System.setProperty("user.home", tmp.toString());
        fake = new Fake();
        SecretStore.useBackend(fake);
        UploadConfig.resetForTest(); LookupConfig.resetForTest();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", oldHome);
        SecretStore.useBackend(null);
        UploadConfig.resetForTest(); LookupConfig.resetForTest();
    }

    private JsonObject uploadFile() throws Exception {
        return JsonParser.parseString(Files.readString(home.resolve(".j-log/upload.json"))).getAsJsonObject();
    }

    @Test
    void aSavedPasswordGoesToTheKeyringAndNotTheFile() throws Exception {
        UploadConfig.setField("eqsl", "user", "W1AW");
        UploadConfig.setField("eqsl", "password", "s3cret#1");
        assertEquals("s3cret#1", UploadConfig.field("eqsl", "password"));
        assertEquals("s3cret#1", fake.items.get("upload.eqsl.password"));
        assertEquals("W1AW", uploadFile().getAsJsonObject("eqsl").get("user").getAsString(), "a username is not a secret");
        assertFalse(uploadFile().getAsJsonObject("eqsl").has("password"), "the password is not in the file");
        assertFalse(Files.readString(home.resolve(".j-log/upload.json")).contains("s3cret"));
    }

    @Test
    void theClubLogTokenAndApiKeysAreSecretsToo() throws Exception {
        UploadConfig.setField("clublog", "token", "cl_abc");
        UploadConfig.setField("clublog", "apikey", "key1");
        UploadConfig.setField("qrz_logbook", "apikey", "qrzkey");
        assertEquals(Map.of("upload.clublog.token", "cl_abc", "upload.clublog.apikey", "key1", "upload.qrz_logbook.apikey", "qrzkey"), fake.items);
        assertFalse(Files.readString(home.resolve(".j-log/upload.json")).contains("cl_abc"), "no secret in the file");
    }

    @Test
    void aPasswordAlreadyInTheFileMovesToTheKeyringWhenTheFileIsRead() throws Exception {
        Files.createDirectories(home.resolve(".j-log"));
        Files.writeString(home.resolve(".j-log/upload.json"), "{\"eqsl\":{\"enabled\":true,\"user\":\"W1AW\",\"password\":\"old pw\"}}");
        assertEquals("old pw", UploadConfig.field("eqsl", "password"));
        assertEquals("old pw", fake.items.get("upload.eqsl.password"));
        JsonObject f = uploadFile().getAsJsonObject("eqsl");
        assertFalse(f.has("password"), "removed from the file once the keyring has it");
        assertTrue(f.get("enabled").getAsBoolean());
        assertEquals("W1AW", f.get("user").getAsString());
    }

    @Test
    void withoutAKeyringTheFileKeepsThePasswordAsBefore() throws Exception {
        fake.up = false; SecretStore.useBackend(fake); UploadConfig.resetForTest();
        UploadConfig.setField("eqsl", "password", "plain");
        assertEquals("plain", UploadConfig.field("eqsl", "password"));
        assertEquals("plain", uploadFile().getAsJsonObject("eqsl").get("password").getAsString());
        assertTrue(fake.items.isEmpty());
    }

    @Test
    void clearingAPasswordRemovesItFromTheKeyring() throws Exception {
        UploadConfig.setField("eqsl", "password", "x");
        UploadConfig.setField("eqsl", "password", "");
        assertEquals("", UploadConfig.field("eqsl", "password"));
        assertFalse(fake.items.containsKey("upload.eqsl.password"));
    }

    @Test
    void lookupPasswordsFollowTheSameRules() throws Exception {
        LookupConfig.set("qrz", true, "W1AW", "qrzpass", "");
        assertEquals("qrzpass", LookupConfig.get("qrz").secret());
        assertEquals("qrzpass", fake.items.get("lookup.qrz.secret"));
        String file = Files.readString(home.resolve(".j-log/lookup.json"));
        assertFalse(file.contains("qrzpass"), file);
        assertTrue(file.contains("W1AW"));
        LookupConfig.resetForTest();                                         // a restart: read back from the keyring
        assertEquals("qrzpass", LookupConfig.get("qrz").secret());
        assertTrue(LookupConfig.get("qrz").enabled());
    }

    @Test
    void lookupPasswordInTheFileMovesToTheKeyring() throws Exception {
        Files.createDirectories(home.resolve(".j-log"));
        Files.writeString(home.resolve(".j-log/lookup.json"), "{\"hamqth\":{\"enabled\":true,\"user\":\"u\",\"secret\":\"pw\",\"extra\":\"\"}}");
        assertEquals("pw", LookupConfig.get("hamqth").secret());
        assertEquals("pw", fake.items.get("lookup.hamqth.secret"));
        assertFalse(Files.readString(home.resolve(".j-log/lookup.json")).contains("\"pw\""));
    }

    @Test
    void theKeyringIsAskedOnceAndRemembered() {
        fake.items.put("a", "v");
        assertEquals("v", SecretStore.get("a"));
        assertEquals("v", SecretStore.get("a"));
        assertEquals(1, fake.gets);
        assertEquals("", SecretStore.get("missing"));
        assertEquals("", SecretStore.get("missing"));
        assertEquals(2, fake.gets, "an absent one is remembered too");
    }

    @Test
    void optOutProperty() {
        System.setProperty("ars.keyring", "false");
        try {
            SecretStore.useBackend(fake);
            assertFalse(SecretStore.available());
            assertFalse(SecretStore.put("a", "b"));
        } finally { System.clearProperty("ars.keyring"); }
    }

    @Test
    void quotingForTheShellsIsSafe() {
        assertEquals("a''b", SecretStore.CliBackend.psq("a'b"));
        assertEquals("\"p\\\"w\\\\x\"", SecretStore.CliBackend.macq("p\"w\\x"));
    }
}
