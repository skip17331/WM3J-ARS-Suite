package com.ars.fx.data;

import com.ars.fx.net.ContestNetwork;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The contest network's shared key lives in the system keyring, like the upload and lookup passwords (punch list 4a-1). */
class NetworkKeyKeyringTest {

    private String oldHome;
    private SecretStoreTest.Fake fake;
    private Path home;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        oldHome = System.getProperty("user.home");
        home = tmp;
        System.setProperty("user.home", tmp.toString());
        HubConfig.reload();
        fake = new SecretStoreTest.Fake();
        SecretStore.useBackend(fake);
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", oldHome);
        SecretStore.useBackend(null);
        HubConfig.reload();
    }

    private String configFile() throws Exception {
        Path p = home.resolve(".j-hub/config.json");
        return Files.exists(p) ? Files.readString(p) : "";
    }

    @Test
    void aKeySetInSetupGoesToTheKeyringNotTheFile() throws Exception {
        ContestNetwork.setSharedKey("field-day#2027");
        assertEquals("field-day#2027", fake.items.get("contest-network-key"));
        assertEquals("field-day#2027", ContestNetwork.sharedKey());
        assertFalse(configFile().contains("field-day"), "the key is not in config.json");
    }

    @Test
    void anOlderVersionsKeyIsMovedOutOfTheFile() throws Exception {
        HubConfig.set(ContestNetwork.KEY_KEY, "old-plain-key");
        assertTrue(configFile().contains("old-plain-key"));
        assertEquals("old-plain-key", ContestNetwork.sharedKey(), "still works while it is moved");
        assertEquals("old-plain-key", fake.items.get("contest-network-key"));
        assertFalse(configFile().contains("old-plain-key"), "gone from config.json");
        assertEquals("old-plain-key", ContestNetwork.sharedKey(), "read from the keyring from now on");
    }

    @Test
    void withoutAKeyringTheKeyStaysInTheFileAsBefore() throws Exception {
        fake.up = false;
        SecretStore.useBackend(fake);
        ContestNetwork.setSharedKey("plain-only");
        assertTrue(configFile().contains("plain-only"));
        assertEquals("plain-only", ContestNetwork.sharedKey());
    }

    @Test
    void clearingTheKeyClearsTheKeyring() {
        ContestNetwork.setSharedKey("temp-key");
        ContestNetwork.setSharedKey("");
        assertEquals("", ContestNetwork.sharedKey());
        assertNull(fake.items.get("contest-network-key"));
    }
}
