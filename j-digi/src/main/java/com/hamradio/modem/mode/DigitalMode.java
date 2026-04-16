package com.hamradio.modem.mode;

import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.SignalSnapshot;

import java.util.Optional;

public interface DigitalMode {
    String getName();
    Optional<DecodeMessage> process(SignalSnapshot snapshot, long rigFrequencyHz);
}
