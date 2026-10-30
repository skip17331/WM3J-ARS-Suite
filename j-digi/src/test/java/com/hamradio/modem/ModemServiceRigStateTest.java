package com.hamradio.modem;

import com.hamradio.modem.model.ModemStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ModemServiceRigStateTest {
    @Test
    void rigStateFillsStatusAndOnlyNotifiesOnChange() {
        ModemService svc = new ModemService();
        AtomicInteger fired = new AtomicInteger();
        svc.setStatusListener(s -> fired.incrementAndGet());

        svc.setRigState(14_074_000L, "USB");
        ModemStatus st = svc.getStatus();
        assertEquals(14_074_000L, st.getRigFrequencyHz());
        assertEquals("USB", st.getRigMode());
        assertTrue(st.getBandplanCaption().startsWith("20m"), st.getBandplanCaption());
        assertEquals("20m", svc.bandFromRigHz());
        assertEquals(1, fired.get());

        svc.setRigState(14_074_000L, "USB");           // a poll with nothing new
        assertEquals(1, fired.get());

        svc.setRigState(0L, null);                      // rig went away
        assertEquals(0L, st.getRigFrequencyHz());
        assertEquals("", st.getRigMode());
        assertEquals("", st.getBandplanCaption());
        assertEquals("", svc.bandFromRigHz());
        assertEquals(2, fired.get());
    }
}
