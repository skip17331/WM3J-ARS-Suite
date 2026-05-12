package com.hamradio.jlearn;

import com.hamradio.jlearn.content.ContentEntry;
import com.hamradio.jlearn.content.ContentManifest;
import com.hamradio.jlearn.state.FileReadingStateStore;
import com.hamradio.jlearn.state.ReadingState;
import com.hamradio.jlearn.state.ReadingStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Façade test: walks a typical host integration end-to-end —
 * list chapters → pick a section → save where we left off →
 * reopen and resume.
 */
class JLearnModuleTest {

    @AfterEach
    void restoreDefaultStateStore() {
        JLearnModule.setStateStore(null);
    }

    @Test
    void manifestReturnsACachedSingleton() {
        ContentManifest a = JLearnModule.manifest();
        ContentManifest b = JLearnModule.manifest();
        assertSame(a, b);
        assertTrue(a.size() > 0);
    }

    @Test
    void hostIntegrationFlow(@TempDir Path tmp) {
        // Inject a temp-path store so we don't touch ~/.j-learn.
        JLearnModule.setStateStore(new FileReadingStateStore(tmp.resolve("state.properties")));

        ContentManifest m = JLearnModule.manifest();
        ContentEntry first = m.all().get(0);

        ReadingStateStore store = JLearnModule.stateStore();
        assertTrue(store.load().isEmpty(), "fresh store starts empty");

        store.save(ReadingState.newSnapshot(first.id(), 0.25));
        Optional<ReadingState> back = store.load();
        assertTrue(back.isPresent());
        assertEquals(first.id(), back.get().lastSectionId());
        assertEquals(0.25, back.get().scrollFraction(), 1e-9);

        // "Resume": look the entry up by the saved id.
        ContentEntry resumed = m.byId(back.get().lastSectionId());
        assertEquals(first, resumed);
    }
}
