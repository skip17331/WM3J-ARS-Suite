package com.hamradio.modem.mode;

import com.hamradio.modem.dsp.Goertzel;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;

import java.util.Optional;

public class Ax25Mode implements DigitalMode {
    private static final double MARK = 1200.0;
    private static final double SPACE = 2200.0;

    @Override
    public String getName() {
        return "AX25";
    }

    @Override
    public Optional<DecodeMessage> process(SignalSnapshot snapshot, long rigFrequencyHz) {
        double mark = Goertzel.power(snapshot.getSamples(), (float) snapshot.getSampleRate(), MARK);
        double space = Goertzel.power(snapshot.getSamples(), (float) snapshot.getSampleRate(), SPACE);
        double total = Math.max(mark + space, 1e-9);
        double dominance = Math.max(mark, space) / total;
        if (snapshot.getRms() > 0.02 && dominance > 0.66) {
            String tone = mark > space ? "1200 Hz" : "2200 Hz";
            double snr = 10.0 * Math.log10((Math.max(mark, space) + 1e-9) / (Math.min(mark, space) + 1e-9));
            return Optional.of(new DecodeMessage(
                    ModeType.AX25,
                    "AX.25 AFSK tone energy detected (" + tone + ")",
                    rigFrequencyHz,
                    snapshot.getPeakFrequencyHz(),
                    snr,
                    dominance));
        }
        return Optional.empty();
    }
}
