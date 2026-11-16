package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Label content for a reprint from the log browser and for a contest QSO. */
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
    void contestQsoBecomesALabelWithTheSameTimeFormat() {
        QsoRecord r = new QsoRecord();
        r.setCallsign("k1abc");
        r.setDateTimeUtc(LocalDateTime.of(2026, 11, 14, 3, 5, 9));
        r.setBand("40m"); r.setMode("CW"); r.setFrequency("7.025");
        r.setRstSent("599"); r.setRstReceived("579");
        r.setOperatorName("Ann"); r.setCountry("Boston MA");
        QslLabel.Qso q = QslLabel.fromContest(r);
        assertEquals("K1ABC", q.call());                       // QsoRecord upper-cases the call
        assertEquals("2026-11-14 03:05:09", q.dateTimeUtc());  // the label cuts this to "yyyy-MM-dd HH:mm" + Z
        assertEquals("40m", q.band());
        assertEquals("CW", q.mode());
        assertEquals("7.025", q.freq());
        assertEquals("599", q.rstSent());
        assertEquals("579", q.rstRcvd());
        assertEquals("Ann", q.name());
        assertEquals("Boston MA", q.qth());
    }

    @Test
    void contestFrequencyIsStoredInKilohertzButLabelledInMegahertz() {
        QsoRecord r = new QsoRecord();
        r.setCallsign("K1ABC");
        r.setFrequency("14025");                               // what the contest page stores (Cabrillo's unit)
        assertEquals("14.025", QslLabel.fromContest(r).freq());
        r.setFrequency("14.074");                              // a QSO that already carries MHz is not divided again
        assertEquals("14.074", QslLabel.fromContest(r).freq());
    }

    @Test
    void contestQsoWithNoTimeOrReportsStillConverts() {
        QsoRecord r = new QsoRecord();
        r.setCallsign("N0CALL");
        QslLabel.Qso q = QslLabel.fromContest(r);               // a QSO with nothing but a call must not throw
        assertEquals("N0CALL", q.call());
        assertEquals("", q.dateTimeUtc());                      // the label then falls back to the current UTC time
    }

    @Test
    void contestSwitchHasItsOwnSettingKey() {
        assertNotEquals(QslLabel.KEY_PRINT_ON_SAVE, QslLabel.KEY_PRINT_ON_CONTEST_SAVE);
        assertEquals("label.printOnSave", QslLabel.KEY_PRINT_ON_SAVE);         // the existing saved setting is unchanged
    }
}
