package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.jlog.db.ContestQsoDao;
import com.jlog.db.DatabaseManager;
import com.jlog.model.QsoRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** The contest page's 3-second timer rescores the whole log only when the log (or the facts the score depends on) changed. */
class ContestRecomputeIfChangedTest {
    @Test void rescoresOnlyWhenSomethingChanged() throws Exception {
        String oldHome = System.getProperty("user.home");
        Path home = Files.createTempDirectory("ars-recompute");
        System.setProperty("user.home", home.toString());
        try {
            HubConfig.reload();
            HubConfig.set("station.call", "W1AW"); HubConfig.set("station.grid", "FN31pr");
            DatabaseManager.getInstance().initAll();
            com.jlog.plugin.PluginLoader.getInstance().init();
            var plugin = com.jlog.plugin.PluginLoader.getInstance().getById("CQ_WW_CW");
            assertNotNull(plugin);
            var f = ContestState.class.getDeclaredField("plugin"); f.setAccessible(true); f.set(null, plugin);
            ContestQsoDao dao = ContestQsoDao.getInstance(); dao.setStationId("T");

            AtomicInteger fired = new AtomicInteger();
            ContestState.clearListeners();
            ContestState.addListener(fired::incrementAndGet);

            ContestState.recomputeIfChanged();
            assertEquals(1, fired.get(), "the first look computes");
            for (int i = 0; i < 20; i++) ContestState.recomputeIfChanged();
            assertEquals(1, fired.get(), "twenty ticks with nothing changed do no work");

            QsoRecord q = new QsoRecord(); q.setUid(java.util.UUID.randomUUID().toString()); q.setContestId("CQ_WW_CW"); q.setCallsign("DL1ABC");
            q.setBand("20m"); q.setMode("CW"); q.setDateTimeUtc(LocalDateTime.of(2026, 11, 28, 1, 0)); q.setOrigin("T"); q.setUpdatedMs(System.nanoTime());
            dao.insert(q);
            ContestState.recomputeIfChanged();
            assertEquals(2, fired.get(), "a new QSO is rescored");
            assertEquals(1, ContestState.score().qsoCount());
            ContestState.recomputeIfChanged();
            assertEquals(2, fired.get(), "and then it is quiet again");

            HubConfig.set("station.grid", "JN58");                 // a fact the score depends on
            ContestState.recomputeIfChanged();
            assertEquals(3, fired.get(), "a changed station fact is rescored");

            q.setNotes("edited"); q.setUpdatedMs(System.nanoTime()); dao.update(q);
            ContestState.recomputeIfChanged();
            assertEquals(4, fired.get(), "an edit is rescored");
        } finally {
            ContestState.clearListeners();
            System.setProperty("user.home", oldHome); HubConfig.reload();
        }
    }
}
