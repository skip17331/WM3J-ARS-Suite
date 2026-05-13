package com.hamradio.jhub.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;

/**
 * AudioProbe — enumerate, categorise, and loopback-test audio devices on the
 * host running j-hub. Powers the <em>Audio Setup Wizard</em> in the j-hub
 * web UI so a new operator can plug a SignaLink or DigiRig in and have the
 * suite figure out the correct sound card assignments in seconds rather
 * than the half-hour of trial-and-error that audio routing usually demands.
 *
 * <p>This class uses only the JDK's {@code javax.sound.sampled} API — no
 * native bindings, no JNI. It assumes j-hub is running on the shack PC
 * (the typical deployment); for the remote-Pi-display case it surfaces
 * the Pi's audio devices, which is also correct because that's the
 * machine actually doing the rig audio path.
 */
public final class AudioProbe {

    /** Audio format used for both probing and the loopback test — 48 kHz
     *  16-bit mono, the standard for digital-mode audio in the suite. */
    public static final AudioFormat FORMAT =
        new AudioFormat(48_000f, 16, 1, true, false);

    private AudioProbe() {}

    // ---------------------------------------------------------------
    // Device enumeration
    // ---------------------------------------------------------------

    public static List<DeviceInfo> enumerateInputs() {
        return enumerate(TargetDataLine.class, "input");
    }

    public static List<DeviceInfo> enumerateOutputs() {
        return enumerate(SourceDataLine.class, "output");
    }

    private static List<DeviceInfo> enumerate(Class<?> lineClass, String direction) {
        List<DeviceInfo> devices = new ArrayList<>();
        DataLine.Info info = new DataLine.Info((Class<? extends DataLine>) lineClass, FORMAT);
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (!mixer.isLineSupported(info)) continue;
                DeviceInfo d = new DeviceInfo();
                d.id          = buildId(mixerInfo);
                d.name        = mixerInfo.getName();
                d.description = mixerInfo.getDescription();
                d.vendor      = mixerInfo.getVendor();
                d.version     = mixerInfo.getVersion();
                d.direction   = direction;
                d.category    = categorise(d.name, d.description);
                devices.add(d);
            } catch (Exception ignored) {
                // Some mixers throw on getMixer() — skip them silently
            }
        }
        return devices;
    }

    /**
     * Heuristic categorisation based on device name. Lets the wizard
     * group devices into "your rig", "PC speakers/mic", and "virtual
     * cables" so the operator doesn't have to read every cryptic device
     * name themselves.
     */
    public static String categorise(String name, String description) {
        String s = (name + " " + (description == null ? "" : description)).toLowerCase();
        // Virtual cables — most distinctive, check first
        if (s.contains("vb-audio") || s.contains("vb cable") || s.contains("cable input")
            || s.contains("cable output") || s.contains("voicemeeter")) return "virtual";
        if (s.contains("blackhole") || s.contains("soundflower") || s.contains("loopback audio"))
            return "virtual";
        if (s.contains("jdigi_loop") || s.contains("null sink") || s.contains("monitor of"))
            return "virtual";
        // Rig audio interfaces
        if (s.contains("signalink")) return "rig-signalink";
        if (s.contains("digirig"))   return "rig-digirig";
        if (s.contains("rigblaster")) return "rig-rigblaster";
        if (s.contains("microham"))   return "rig-microham";
        // Built-in rig USB sound cards — IC-7300, IC-705, FT-991A, etc.
        // "USB Audio CODEC" is the generic name nearly all of them use.
        if (s.contains("usb audio codec")) return "rig-builtin";
        // Specific rigs sometimes report a friendlier name
        if (s.contains("ic-7300") || s.contains("ic-705") || s.contains("ic-7610")
            || s.contains("ft-991") || s.contains("ftdx") || s.contains("ts-590")
            || s.contains("ts-890") || s.contains("kx3") || s.contains("k3s")
            || s.contains("k4")) return "rig-builtin";
        // PC built-in
        if (s.contains("built-in") || s.contains("internal")
            || s.contains("hda") || s.contains("realtek") || s.contains("intel")) return "pc";
        return "other";
    }

    private static String buildId(Mixer.Info m) {
        // Combine name + version because some hosts expose two mixers with
        // identical names but different driver layers (e.g. ALSA vs PulseAudio).
        return m.getName() + " | " + (m.getVersion() == null ? "" : m.getVersion());
    }

    // ---------------------------------------------------------------
    // Loopback test
    // ---------------------------------------------------------------

    /**
     * Play a short test tone on the chosen output and capture it on the
     * chosen input simultaneously. Returns peak level + a rough SNR figure
     * so the wizard can render a green/yellow/red verdict.
     */
    public static LoopbackResult loopbackTest(String outputDeviceId, String inputDeviceId,
                                              int durationMs, double freqHz) {
        LoopbackResult r = new LoopbackResult();
        r.requestedFreqHz = freqHz;
        r.sampleRate      = (int) FORMAT.getSampleRate();
        r.durationMs      = durationMs;

        Mixer.Info outMixer = findMixer(outputDeviceId);
        Mixer.Info inMixer  = findMixer(inputDeviceId);
        if (outMixer == null) { r.error = "Output device not found"; return r; }
        if (inMixer  == null) { r.error = "Input device not found";  return r; }

        int samples = (int) (FORMAT.getSampleRate() * durationMs / 1000.0);
        byte[] tone = synthTone(freqHz, samples);
        byte[] captured = new byte[tone.length];

        SourceDataLine out = null;
        TargetDataLine in  = null;
        try {
            out = (SourceDataLine) AudioSystem.getMixer(outMixer)
                .getLine(new DataLine.Info(SourceDataLine.class, FORMAT));
            in  = (TargetDataLine) AudioSystem.getMixer(inMixer)
                .getLine(new DataLine.Info(TargetDataLine.class, FORMAT));

            out.open(FORMAT, tone.length);
            in.open(FORMAT, captured.length);
            in.start();

            // Launch capture on a side thread so it runs concurrently with
            // the playback; main thread blocks on playback completion.
            final TargetDataLine inRef = in;
            final byte[] capRef = captured;
            Thread capt = new Thread(() -> {
                int read = 0;
                while (read < capRef.length) {
                    int n = inRef.read(capRef, read, capRef.length - read);
                    if (n <= 0) break;
                    read += n;
                }
            }, "audio-probe-capture");
            capt.setDaemon(true);
            capt.start();

            out.start();
            out.write(tone, 0, tone.length);
            out.drain();
            capt.join(durationMs + 500L);

            r.peakDb = peakDb(captured);
            r.snrDb  = toneSnrDb(captured, freqHz);
            r.detected = r.snrDb > 6.0; // 6 dB SNR threshold for "detected"
        } catch (Exception e) {
            r.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            try { if (out != null) { out.stop(); out.close(); } } catch (Exception ignored) {}
            try { if (in  != null) { in.stop();  in.close();  } } catch (Exception ignored) {}
        }
        return r;
    }

    private static Mixer.Info findMixer(String id) {
        for (Mixer.Info m : AudioSystem.getMixerInfo()) {
            if (buildId(m).equals(id)) return m;
        }
        return null;
    }

    private static byte[] synthTone(double freqHz, int samples) {
        byte[] out = new byte[samples * 2]; // 16-bit
        double phaseStep = 2 * Math.PI * freqHz / FORMAT.getSampleRate();
        // 50%-amplitude tone with a 10-ms cosine ramp at each end to avoid clicks
        int ramp = Math.min(samples / 4, (int) (FORMAT.getSampleRate() * 0.010));
        for (int i = 0; i < samples; i++) {
            double env = 1.0;
            if (i < ramp)             env = 0.5 - 0.5 * Math.cos(Math.PI * i / ramp);
            else if (i > samples - ramp) env = 0.5 - 0.5 * Math.cos(Math.PI * (samples - i) / ramp);
            short s = (short) (Math.sin(i * phaseStep) * 16384 * env);
            out[2*i]     = (byte)  (s & 0xff);
            out[2*i + 1] = (byte) ((s >> 8) & 0xff);
        }
        return out;
    }

    private static double peakDb(byte[] pcm) {
        int peak = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            short s = (short) ((pcm[i] & 0xff) | (pcm[i+1] << 8));
            int a = Math.abs(s);
            if (a > peak) peak = a;
        }
        if (peak == 0) return -120.0;
        return 20 * Math.log10(peak / 32768.0);
    }

    /**
     * Compute an SNR figure by measuring how much energy sits at the test
     * frequency vs everything else. Uses a single-bin Goertzel filter so
     * we don't pull in a full FFT for this one-shot.
     */
    private static double toneSnrDb(byte[] pcm, double freqHz) {
        int n = pcm.length / 2;
        // Goertzel for the target bin
        double coeff = 2 * Math.cos(2 * Math.PI * freqHz / FORMAT.getSampleRate());
        double q1 = 0, q2 = 0;
        double totalEnergy = 0;
        for (int i = 0; i < n; i++) {
            short s = (short) ((pcm[2*i] & 0xff) | (pcm[2*i+1] << 8));
            double x = s / 32768.0;
            double q0 = coeff * q1 - q2 + x;
            q2 = q1; q1 = q0;
            totalEnergy += x * x;
        }
        double tonePower = q1 * q1 + q2 * q2 - q1 * q2 * coeff;
        if (totalEnergy <= 0) return -120.0;
        double noisePower = Math.max(totalEnergy - tonePower, 1e-12);
        return 10 * Math.log10(tonePower / noisePower);
    }

    // ---------------------------------------------------------------
    // Result records (kept as POJOs so Gson serialises them cleanly)
    // ---------------------------------------------------------------

    public static final class DeviceInfo {
        public String id;
        public String name;
        public String description;
        public String vendor;
        public String version;
        public String direction;   // "input" | "output"
        public String category;    // see categorise() output
    }

    public static final class LoopbackResult {
        public boolean detected;
        public double  peakDb;
        public double  snrDb;
        public double  requestedFreqHz;
        public int     sampleRate;
        public int     durationMs;
        public String  error;
    }
}
