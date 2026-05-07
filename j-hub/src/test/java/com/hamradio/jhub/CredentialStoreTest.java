package com.hamradio.jhub;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CredentialStore integration tests. Each test redirects the on-disk path
 * via the {@code j-hub.credentials.path} system property and clears the
 * singleton's cache so the four tests are independent.
 */
class CredentialStoreTest {

    private Path tmpStore;

    @BeforeEach
    void setUp() throws Exception {
        tmpStore = Files.createTempFile("creds-test", ".enc");
        Files.deleteIfExists(tmpStore);
        System.setProperty("j-hub.credentials.path", tmpStore.toString());
        resetSingleton();
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(tmpStore);
        System.clearProperty("j-hub.credentials.path");
        resetSingleton();
    }

    private void resetSingleton() throws Exception {
        Field cache = CredentialStore.class.getDeclaredField("cache");
        cache.setAccessible(true);
        ((java.util.Map<?, ?>) cache.get(CredentialStore.getInstance())).clear();
        Field loaded = CredentialStore.class.getDeclaredField("loaded");
        loaded.setAccessible(true);
        loaded.setBoolean(CredentialStore.getInstance(), false);
    }

    @Test
    void roundTripPreservesCredentials() {
        JsonObject creds = new JsonObject();
        creds.addProperty("user", "WM3J");
        creds.addProperty("pass", "hunter2");
        CredentialStore.getInstance().put("eqsl", creds);

        // Force a reload from disk by clearing the cache.
        try { resetSingleton(); } catch (Exception e) { fail(e); }
        JsonObject readBack = CredentialStore.getInstance().get("eqsl");
        assertEquals("WM3J",    readBack.get("user").getAsString());
        assertEquals("hunter2", readBack.get("pass").getAsString());
    }

    @Test
    void emptyServiceReturnsEmptyObject() {
        JsonObject o = CredentialStore.getInstance().get("nonexistent");
        assertNotNull(o);
        assertTrue(o.entrySet().isEmpty());
    }

    @Test
    void hasReturnsFalseUntilCredentialsStored() {
        assertFalse(CredentialStore.getInstance().has("eqsl"));
        JsonObject creds = new JsonObject();
        creds.addProperty("token", "abc");
        CredentialStore.getInstance().put("eqsl", creds);
        assertTrue(CredentialStore.getInstance().has("eqsl"));
        CredentialStore.getInstance().remove("eqsl");
        assertFalse(CredentialStore.getInstance().has("eqsl"));
    }

    @Test
    void onDiskFileIsOpaque() throws Exception {
        String secret = "do-not-leak-this-token-" + System.nanoTime();
        JsonObject creds = new JsonObject();
        creds.addProperty("token", secret);
        CredentialStore.getInstance().put("clublog", creds);

        assertTrue(Files.exists(tmpStore), "Store file should exist after put()");
        byte[] bytes = Files.readAllBytes(tmpStore);
        String asAscii = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertFalse(asAscii.contains(secret),
            "Plaintext credential leaked into on-disk file");
        assertFalse(asAscii.contains("token"),
            "JSON key 'token' leaked into on-disk file");
        assertFalse(asAscii.contains("clublog"),
            "Service name 'clublog' leaked into on-disk file");
    }
}
