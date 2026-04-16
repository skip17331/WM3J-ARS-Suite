package com.hamradio.modem.tx;

import com.hamradio.modem.audio.AudioEngine;

public class Psk31Transmitter implements DigitalTransmitter {

    private final double sampleRate;
    private final double symbolRate;
    private final double carrierHz;
    private final double amplitude;
    private final int preambleZeroSymbols;
    private final int postambleOneSymbols;

    public Psk31Transmitter() {
        this(AudioEngine.SAMPLE_RATE, 31.25, 1000.0, 0.50, 32, 32);
    }

    public Psk31Transmitter(double sampleRate,
                            double symbolRate,
                            double carrierHz,
                            double amplitude,
                            int preambleZeroSymbols,
                            int postambleOneSymbols) {
        this.sampleRate = sampleRate;
        this.symbolRate = symbolRate;
        this.carrierHz = carrierHz;
        this.amplitude = amplitude;
        this.preambleZeroSymbols = Math.max(0, preambleZeroSymbols);
        this.postambleOneSymbols = Math.max(0, postambleOneSymbols);
    }

    @Override
    public String getName() {
        return "PSK31";
    }

    @Override
    public SampleSource createSampleSource(String text) {
        return new Psk31SampleSource(
                sampleRate,
                symbolRate,
                carrierHz,
                amplitude,
                preambleZeroSymbols,
                postambleOneSymbols,
                text
        );
    }

    private static final class Psk31SampleSource implements SampleSource {

        private final double sampleRate;
        private final double carrierHz;
        private final double amplitude;
        private final int samplesPerSymbol;
        private final char[] symbols;

        private boolean finished;
        private boolean closed;

        private int symbolIndex;
        private int sampleInSymbol;

        private double phase;
        private final double phaseStep;

        private int stateSign = 1;
        private int symbolStartSign = 1;
        private int symbolEndSign = 1;
        private boolean symbolActive;

        private Psk31SampleSource(double sampleRate,
                                  double symbolRate,
                                  double carrierHz,
                                  double amplitude,
                                  int preambleZeroSymbols,
                                  int postambleOneSymbols,
                                  String text) {
            this.sampleRate = sampleRate;
            this.carrierHz = carrierHz;
            this.amplitude = amplitude;
            this.samplesPerSymbol = Math.max(1, (int) Math.round(sampleRate / symbolRate));
            this.phaseStep = (2.0 * Math.PI * carrierHz) / sampleRate;
            this.symbols = buildSymbolStream(text, preambleZeroSymbols, postambleOneSymbols).toCharArray();
        }

        private static String buildSymbolStream(String text,
                                                int preambleZeroSymbols,
                                                int postambleOneSymbols) {
            StringBuilder bits = new StringBuilder();

            for (int i = 0; i < preambleZeroSymbols; i++) {
                bits.append('0');
            }

            bits.append(Psk31Varicode.encodeToBitStream(text == null ? "" : text));

            for (int i = 0; i < postambleOneSymbols; i++) {
                bits.append('1');
            }

            return bits.toString();
        }

        @Override
        public int read(float[] buffer, int offset, int length) {
            if (closed || finished || buffer == null || length <= 0) {
                return -1;
            }

            int written = 0;

            while (written < length) {
                if (!prepareCurrentSymbol()) {
                    finished = true;
                    break;
                }

                double u = samplesPerSymbol <= 1
                        ? 1.0
                        : (double) sampleInSymbol / (double) samplesPerSymbol;

                double baseband = shapedPhaseState(symbolStartSign, symbolEndSign, u);
                double sample = Math.sin(phase) * amplitude * baseband;

                buffer[offset + written] = (float) sample;
                written++;

                phase += phaseStep;
                if (phase >= (2.0 * Math.PI)) {
                    phase -= (2.0 * Math.PI);
                }

                sampleInSymbol++;
                if (sampleInSymbol >= samplesPerSymbol) {
                    symbolActive = false;
                    sampleInSymbol = 0;
                    symbolIndex++;
                }
            }

            return written > 0 ? written : -1;
        }

        private boolean prepareCurrentSymbol() {
            if (symbolActive) {
                return true;
            }

            if (symbolIndex >= symbols.length) {
                return false;
            }

            char bit = symbols[symbolIndex];
            symbolStartSign = stateSign;

            if (bit == '0') {
                symbolEndSign = -stateSign;
            } else {
                symbolEndSign = stateSign;
            }

            stateSign = symbolEndSign;
            symbolActive = true;
            sampleInSymbol = 0;
            return true;
        }

        private double shapedPhaseState(int startSign, int endSign, double u) {
            if (startSign == endSign) {
                return endSign;
            }

            double mu = Math.max(0.0, Math.min(1.0, u));
            return startSign + (endSign - startSign) * 0.5 * (1.0 - Math.cos(Math.PI * mu));
        }

        @Override
        public boolean isFinished() {
            return finished || closed;
        }

        @Override
        public void close() {
            closed = true;
            finished = true;
        }
    }
}
