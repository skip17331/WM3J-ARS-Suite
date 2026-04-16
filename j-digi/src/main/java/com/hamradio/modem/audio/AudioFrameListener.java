package com.hamradio.modem.audio;

@FunctionalInterface
public interface AudioFrameListener {
    void onFrame(float[] samples);
}
