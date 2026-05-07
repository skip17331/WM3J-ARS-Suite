package com.morsetrainer.decoder;

import com.morsetrainer.core.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimingDecoderTest {

    private TimingDecoder decoder;
    private List<Character> received;

    @BeforeEach
    void setUp() {
        AppConfig cfg = new AppConfig();
        cfg.wpm = 20;                 // dit = 60ms
        cfg.adaptiveDecoder = false;  // strict timing for predictable tests
        decoder = new TimingDecoder(cfg);
        decoder.setEstimatedDitMs(60);
        received = new ArrayList<>();
        decoder.onCharacter(received::add);
    }

    @Test
    void decodesSingleE() {
        // E = "."
        long t = 0;
        decoder.accept(KeyEvent.down(t));
        decoder.accept(KeyEvent.up(t + 60));
        decoder.tick(t + 60 + 600); // long gap forces flush
        assertEquals(List.of('E'), received);
    }

    @Test
    void decodesA() {
        // A = ".-"
        long t = 0;
        decoder.accept(KeyEvent.down(t));
        decoder.accept(KeyEvent.up(t + 60));
        t += 60 + 60;          // intra gap
        decoder.accept(KeyEvent.down(t));
        decoder.accept(KeyEvent.up(t + 180));
        decoder.tick(t + 180 + 600);
        assertEquals(List.of('A'), received);
    }

    @Test
    void decodesMultipleCharactersWithCharGap() {
        // T = "-", E = "."
        long t = 0;
        decoder.accept(KeyEvent.down(t));
        decoder.accept(KeyEvent.up(t + 180));
        t += 180 + 200;        // > 2.5*dit gap (180ms > 150)
        decoder.accept(KeyEvent.down(t));
        decoder.accept(KeyEvent.up(t + 60));
        decoder.tick(t + 60 + 600);
        assertEquals(List.of('T', 'E'), received);
    }

    @Test
    void emitsQuestionMarkForUnknownPattern() {
        // 6 dits = no valid mapping
        long t = 0;
        for (int i = 0; i < 6; i++) {
            decoder.accept(KeyEvent.down(t));
            decoder.accept(KeyEvent.up(t + 60));
            t += 60 + 60;
        }
        decoder.tick(t + 600);
        assertEquals(1, received.size());
        assertEquals('?', received.get(0));
    }

    @Test
    void wordBoundaryFires() {
        AppConfig cfg = new AppConfig();
        cfg.wpm = 20;
        cfg.adaptiveDecoder = false;
        TimingDecoder d = new TimingDecoder(cfg);
        d.setEstimatedDitMs(60);
        List<Character> chars = new ArrayList<>();
        boolean[] wordFired = { false };
        d.onCharacter(chars::add);
        d.onWordBoundary(() -> wordFired[0] = true);

        long t = 0;
        d.accept(KeyEvent.down(t));
        d.accept(KeyEvent.up(t + 60));
        d.tick(t + 60 + 800);   // gap > 6*dit -> word boundary

        assertEquals(List.of('E'), chars);
        assertTrue(wordFired[0]);
    }
}
