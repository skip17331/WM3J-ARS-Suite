package com.hamradio.modem.tx;

import com.hamradio.modem.audio.AudioEngine;

import java.util.ArrayDeque;
import java.util.Deque;

public class RttyTransmitter implements DigitalTransmitter {

    private final double sampleRate;
    private final double baudRate;
    private final double markHz;
    private final double shiftHz;
    private final double stopBits;
    private final double amplitude;

    public RttyTransmitter() {
        this(AudioEngine.SAMPLE_RATE, 45.45, 2125.0, 170.0, 1.5, 0.50);
    }

    public RttyTransmitter(double sampleRate,
                           double baudRate,
                           double markHz,
                           double shiftHz,
                           double stopBits,
                           double amplitude) {
        this.sampleRate = sampleRate;
        this.baudRate = baudRate;
        this.markHz = markHz;
        this.shiftHz = shiftHz;
        this.stopBits = stopBits;
        this.amplitude = amplitude;
    }

    @Override
    public String getName() {
        return "RTTY";
    }

    @Override
    public SampleSource createSampleSource(String text) {
        return new RttySampleSource(
                sampleRate,
                baudRate,
                markHz,
                shiftHz,
                stopBits,
                amplitude,
                text
        );
    }

    private static final class RttySampleSource implements SampleSource {

        private final double sampleRate;
        private final double markHz;
        private final double spaceHz;
        private final double samplesPerBit;
        private final double amplitude;

        private final Deque<Symbol> symbols = new ArrayDeque<>();

        private boolean finished;
        private boolean closed;

        private double phase;
        private Symbol currentSymbol;
        private double symbolSamplesRemaining;

        private RttySampleSource(double sampleRate,
                                 double baudRate,
                                 double markHz,
                                 double shiftHz,
                                 double stopBits,
                                 double amplitude,
                                 String text) {
            this.sampleRate = sampleRate;
            this.markHz = markHz;
            this.spaceHz = markHz + shiftHz;
            this.samplesPerBit = sampleRate / baudRate;
            this.amplitude = amplitude;

            buildSymbols(text, stopBits);
        }

        private void buildSymbols(String text, double stopBits) {
            symbols.addLast(new Symbol(true, samplesPerBit * 12.0)); // idle mark lead-in

            RttyBaudot.EncodedText encoded = RttyBaudot.encodeText(text == null ? "" : text);
            for (int code : encoded.getCodes()) {
                appendCharacter(code, stopBits);
            }

            symbols.addLast(new Symbol(true, samplesPerBit * 12.0)); // idle mark tail
        }

        private void appendCharacter(int code, double stopBits) {
            // Start bit = SPACE (0)
            symbols.addLast(new Symbol(false, samplesPerBit));

            // 5 data bits, LSB first. MARK = 1, SPACE = 0.
            for (int bit = 0; bit < 5; bit++) {
                boolean mark = ((code >> bit) & 0x01) != 0;
                symbols.addLast(new Symbol(mark, samplesPerBit));
            }

            // Stop bits = MARK (1)
            symbols.addLast(new Symbol(true, samplesPerBit * stopBits));
        }

        @Override
        public int read(float[] buffer, int offset, int length) {
            if (closed || finished || buffer == null || length <= 0) {
                return -1;
            }

            int written = 0;

            while (written < length) {
                if (currentSymbol == null || symbolSamplesRemaining <= 0.0) {
                    currentSymbol = symbols.pollFirst();
                    if (currentSymbol == null) {
                        finished = true;
                        break;
                    }
                    symbolSamplesRemaining = currentSymbol.durationSamples();
                }

                double freq = currentSymbol.mark() ? markHz : spaceHz;
                double phaseStep = (2.0 * Math.PI * freq) / sampleRate;

                buffer[offset + written] = (float) (Math.sin(phase) * amplitude);
                phase += phaseStep;
                if (phase >= (2.0 * Math.PI)) {
                    phase -= (2.0 * Math.PI);
                }

                symbolSamplesRemaining -= 1.0;
                written++;
            }

            return written > 0 ? written : -1;
        }

        @Override
        public boolean isFinished() {
            return finished || closed;
        }

        @Override
        public void close() {
            closed = true;
            finished = true;
            symbols.clear();
            currentSymbol = null;
        }
    }

    private record Symbol(boolean mark, double durationSamples) {
    }
}
