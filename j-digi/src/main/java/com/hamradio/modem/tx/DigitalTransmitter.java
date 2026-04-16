package com.hamradio.modem.tx;

public interface DigitalTransmitter {
    String getName();
    SampleSource createSampleSource(String text);
}
