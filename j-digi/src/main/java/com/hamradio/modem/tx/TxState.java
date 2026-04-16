package com.hamradio.modem.tx;

public enum TxState {
    IDLE,
    STARTING,
    TRANSMITTING,
    STOPPING,
    CANCELLED,
    COMPLETE,
    ERROR
}
