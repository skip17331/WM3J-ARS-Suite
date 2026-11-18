package com.jlog.plugin;

import com.jlog.util.AppConfig;
import com.jlog.db.DatabaseManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Every contest definition says how its entry page is laid out: a layout block (field order, tiles, CQ text; each part optional). */
class PluginLayoutTest {
    @BeforeAll
    static void init(@TempDir Path tmpHome) throws Exception {
        System.setProperty("user.home", tmpHome.toString());
        AppConfig.getInstance().load();
        DatabaseManager.getInstance().initAll();
        PluginLoader.getInstance().init();
    }

    @Test
    void everyContestHasALayout() {
        var all = PluginLoader.getInstance().getAvailablePlugins();
        assertFalse(all.isEmpty());
        for (ContestPlugin p : all) {
            assertNotNull(p.getLayout(), p.getContestId() + " has no layout");
        }
    }
}
