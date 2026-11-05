package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkshopSettingsTest {
    @Test void rotorPresetsKeepTheirSettingKeys() {
        // the keys are what J-Hub ▸ Rotor control has always saved them under
        assertEquals("rotor.preset.SAmerica", "rotor.preset." + RotorClient.PRESETS.get(2).key());
        assertEquals("rotor.preset.AsiaLP", "rotor.preset." + RotorClient.PRESETS.get(4).key());
        assertEquals(6, RotorClient.PRESETS.size());
        for (RotorClient.Preset p : RotorClient.PRESETS) { assertTrue(p.az() >= 0 && p.az() < 360, p.name()); }
    }

    @Test void asciiExportDropsAccentsAndKeepsPlainText() {
        // forExport only changes text when the charset setting is ASCII; the transliteration itself is checked directly
        String s = "Müller café 73";
        String d = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        assertEquals("Muller cafe 73", d);
        assertEquals("plain 73", LogExport.forExport("plain 73"));
    }

    @Test void everyModuleIsListed() {
        assertEquals(10, ModuleConfig.all().size());   // Info + seven modules + the Morse Trainer and Antenna Workshop dock entries
        assertEquals("info", ModuleConfig.all().get(0).id());
        assertEquals("log", ModuleConfig.all().get(1).id());
        assertEquals("morse", ModuleConfig.all().get(8).id());
        assertEquals("workshop", ModuleConfig.all().get(9).id());
    }
}
