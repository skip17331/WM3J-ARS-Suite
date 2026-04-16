package com.hamradio.modem.tx;

public interface SampleSource {
    int read(float[] buffer, int offset, int length);
    boolean isFinished();
    void close();
}
