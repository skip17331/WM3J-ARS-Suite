package com.ars.fx.morse.trainer;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.morse.decoder.KeyEvent;
import com.ars.fx.morse.decoder.TimingDecoder;
import com.ars.fx.morse.trainer.sendpractice.SendPracticeSession;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** The practice session is driven from three places at once now (key events, a timing tick on its own thread, the screen reading metrics). */
class SendPracticeSessionThreadingTest {
    private static void dit(SendPracticeSession s, long t) {
        s.accept(new KeyEvent(KeyEvent.Type.DOWN, t));
        s.accept(new KeyEvent(KeyEvent.Type.UP, t + 60));
    }

    @Test void aTickAfterTheKeyHasBeenUpLongEnoughClosesTheCharacter() {
        SendPracticeSession s = new SendPracticeSession(SendPracticeSession.Mode.FREE, "", new TimingDecoder());
        long t = 1000;
        dit(s, t); dit(s, t + 120); dit(s, t + 240);          // S: three dits, one dit apart
        assertEquals("", s.decoded(), "not closed yet: more could follow");
        s.tick(t + 240 + 60 + 600);                           // the key has been up for ten dits
        assertEquals("S", s.decoded());
    }

    @Test void keyEventsTicksAndMetricReadsFromThreeThreadsDoNotCollide() throws Exception {
        SendPracticeSession s = new SendPracticeSession(SendPracticeSession.Mode.FREE, "", new TimingDecoder());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        long end = System.currentTimeMillis() + 1500;
        Thread keys = new Thread(() -> {
            try { long t = 0; while (System.currentTimeMillis() < end) { dit(s, t); t += 130; dit(s, t); t += 700; } }
            catch (Throwable e) { failure.set(e); }
        });
        Thread tick = new Thread(() -> {
            try { long t = 0; while (System.currentTimeMillis() < end) { s.tick(t += 25); } }
            catch (Throwable e) { failure.set(e); }
        });
        Thread screen = new Thread(() -> {
            try { while (System.currentTimeMillis() < end) { s.diagnostics(); s.troubleReport(); s.decoded(); s.accuracy(); } }
            catch (Throwable e) { failure.set(e); }
        });
        keys.start(); tick.start(); screen.start();
        keys.join(); tick.join(); screen.join();
        assertNull(failure.get(), String.valueOf(failure.get()));
    }
}
