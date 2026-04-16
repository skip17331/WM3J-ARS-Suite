package com.hamradio.modem.mode;

import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class ModeManager {

    private final Map<ModeType, DigitalMode> modes = new EnumMap<>(ModeType.class);
    private final RttyMode    rttyMode    = new RttyMode();
    private final DigitalMode psk31Mode   = new Psk31Mode();
    private final DigitalMode cwMode      = new CwMode();
    private final DigitalMode oliviaMode  = new OliviaMode();
    private final DigitalMode mfsk16Mode   = new Mfsk16Mode();
    private final DigitalMode dominoExMode = new DominoExMode();
    private final DigitalMode ax25Mode     = new Ax25Mode();

    public ModeManager() {
        modes.put(ModeType.RTTY,   rttyMode);
        modes.put(ModeType.PSK31,  psk31Mode);
        modes.put(ModeType.CW,     cwMode);
        modes.put(ModeType.OLIVIA, oliviaMode);
        modes.put(ModeType.MFSK16,   mfsk16Mode);
        modes.put(ModeType.DOMINOEX, dominoExMode);
        modes.put(ModeType.AX25,     ax25Mode);
    }

    public Optional<DecodeMessage> process(ModeType mode, SignalSnapshot snapshot, long rigFrequencyHz) {
        DigitalMode decoder = modes.get(mode);
        if (decoder == null) {
            return Optional.empty();
        }
        return decoder.process(snapshot, rigFrequencyHz);
    }

    public RttyMode getRttyMode() {
        return rttyMode;
    }

    public DigitalMode getMode(ModeType mode) {
        return modes.get(mode);
    }
}
