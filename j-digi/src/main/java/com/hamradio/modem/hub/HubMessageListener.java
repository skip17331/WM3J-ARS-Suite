package com.hamradio.modem.hub;

import com.google.gson.JsonObject;

public interface HubMessageListener {
    void onConnected();
    void onDisconnected();
    void onMessage(JsonObject message);
    void onError(String errorMessage);
}
