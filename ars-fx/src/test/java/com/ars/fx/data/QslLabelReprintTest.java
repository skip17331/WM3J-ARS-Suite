package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Label content for a reprint from the log browser. */
class QslLabelReprintTest {

    @Test
    void normalLogRowKeepsEveryLabelField() {
        JLogDb.QsoFull f = new JLogDb.QsoFull(7, "2026-11-10 14:30:00", "W1AW", "14.074", "20m", "FT8", "-10", "-12",
                "Hiram", "Newington CT", "FN31", "CT", "", 100, "", JLogDb.Refs.NONE);
        QslLabel.Qso q = QslLabel.fromFull(f);
        assertEquals("W1AW", q.call());
        assertEquals("2026-11-10 14:30:00", q.dateTimeUtc());
        assertEquals("14.074", q.freq());
        assertEquals("20m", q.band());
        assertEquals("FT8", q.mode());
        assertEquals("-10", q.rstSent());
        assertEquals("-12", q.rstRcvd());
        assertEquals("Hiram", q.name());
        assertEquals("Newington CT", q.qth());
    }

    @Test
    void printOnSaveKeepsItsSettingKey() {
        assertEquals("label.printOnSave", QslLabel.KEY_PRINT_ON_SAVE);         // the existing saved setting is unchanged
    }
}
