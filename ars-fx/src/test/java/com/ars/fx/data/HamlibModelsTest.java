package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class HamlibModelsTest {
    @Test void parsesRigctlTable() {
        String out = " Rig #  Mfg                    Model                   Version         Status      Macro\n"
                   + "     1  Hamlib                 Dummy                   20240101.0      Stable      RIG_MODEL_DUMMY\n"
                   + "  3078  Icom                   IC-7610                 20240101.0      Stable      RIG_MODEL_IC7610\n"
                   + "  1035  Yaesu                  FT-991                  20240101.0      Stable      RIG_MODEL_FT991\n";
        var l = HamlibModels.parse(out);
        assertEquals(3, l.size());
        assertEquals("Hamlib", l.get(0).mfg());
        assertEquals("IC-7610", l.get(1).name());
        assertEquals(1035, l.get(2).number());
    }
}
