package com.hamradio.jbridge;

/**
 * UpstreamAppConnectionListener — fired when the upstream digital-mode app
 * (WSJT-X or JTDX) connects, disconnects, or sends a heartbeat that reveals a
 * new {@code sourceApp}/{@code version}.
 *
 * <p>Replaces the original {@code BiConsumer&lt;Boolean,String&gt;} callback so the
 * listener can pass the source app name (WSJT-X / JTDX / Unknown) alongside
 * the version string without conflating them in display output.
 */
@FunctionalInterface
public interface UpstreamAppConnectionListener {
    /**
     * @param connected true on heartbeat / false on Close or socket loss
     * @param sourceApp WSJT-X / JTDX / Unknown — see {@link WsjtxProtocolDecoder#detectSourceApp(String)}
     * @param version   version string from the heartbeat (e.g. "2.7.0")
     */
    void onChange(boolean connected, String sourceApp, String version);
}
