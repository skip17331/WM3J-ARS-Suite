package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.plugin.ContestPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Every layout note shipped in a contest definition must say only what that definition itself says. */
class ContestNotesTest {
    static List<ContestPlugin> all() throws Exception {
        Path dir = Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins");
        List<ContestPlugin> out = new ArrayList<>();
        try (var fs = Files.list(dir)) {
            for (Path f : (Iterable<Path>) fs.filter(x -> x.toString().endsWith(".json")).sorted()::iterator)
                out.add(new ObjectMapper().readValue(f.toFile(), ContestPlugin.class));
        }
        return out;
    }

    @Test void noteSaysOnlyWhatTheDefinitionSays() throws Exception {
        int withNotes = 0;
        for (ContestPlugin p : all()) {
            String note = ContestLayout.of(p.getLayout()).note();
            if (note.isEmpty()) continue;
            withNotes++;
            if (p.getContestId().startsWith("ARRL_SS")) continue;             // hand-written
            if (note.contains("once for the whole contest")) assertTrue(p.isContestWideDupe(), p.getContestId());
            if (note.contains("once per band")) assertTrue(p.getMultiplierModel() != null && p.getMultiplierModel().isPerBand(), p.getContestId());
            if (note.contains("once per mode")) assertTrue(p.isPerModeMultipliers(), p.getContestId());
            if (note.contains("band is fixed to")) assertNotNull(p.getLockedBand(), p.getContestId());
            if (note.contains("mode is fixed to")) assertNotNull(p.getLockedMode(), p.getContestId());
        }
        assertTrue(withNotes >= 40, "most contests carry a flag worth a note, got " + withNotes);
    }

    @Test void everyFlagWorthANoteHasOne() throws Exception {
        for (ContestPlugin p : all()) {
            boolean flagged = p.isContestWideDupe() || (p.getMultiplierModel() != null && p.getMultiplierModel().isPerBand())
                    || p.isPerModeMultipliers() || p.getLockedBand() != null || p.getLockedMode() != null;
            if (flagged) assertFalse(ContestLayout.of(p.getLayout()).note().isEmpty(), p.getContestId() + " should carry a note");
        }
    }
}
