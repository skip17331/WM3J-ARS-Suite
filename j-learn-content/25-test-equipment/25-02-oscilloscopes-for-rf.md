---
id: 25-02
title: Oscilloscopes for RF Work
chapter: 25
section: 02
level: mixed
status: published
---

# Oscilloscopes for RF Work

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A scope shows **voltage versus time**. That sounds basic, but it's exactly the wrong tool for most RF work — and exactly the right tool for the *adjacent* work that happens in every ham station: power supplies, audio, modulation envelopes, switching transients, CW keying shapes. This section explains when a scope helps in a radio shack and when you reach for a spectrum analyzer instead.

## When a scope helps in RF work

A scope shines on four problems:

### 1. Looking at modulation envelopes

The waveform of a transmitter feeding a dummy load is, viewed at slow timebase, the **modulation envelope** — the shape of how the carrier amplitude varies with the audio. A clean SSB signal looks like the speech waveform on a higher-frequency carrier; a clean AM signal looks like a sinusoid riding on the carrier with peaks at carrier + audio.

What you can see:
- **Overmodulation** — the envelope flat-tops at peak audio (carrier driven into compression). Tells you mic gain or compressor are too aggressive.
- **Speech processor effect** — the envelope's peak-to-average ratio gets squashed.
- **Asymmetry** — if positive and negative envelope peaks aren't symmetric, the PA may be unbalanced or the audio has DC offset.

You don't see the **RF carrier itself** clearly unless your scope is fast enough — that's the next section. You see its *envelope* by slowing the timebase down to milliseconds per division.

### 2. Switching-supply ripple

Modern HF rigs run on 13.8 V at 20+ amps. The supply that produces this is almost always a switching converter running at 50–200 kHz with a filtered output. Failed filter capacitors let the switching frequency leak through.

Procedure: probe across the rig's DC input with the scope on **AC coupling**. You should see <50 mV ripple on a good supply. If you see hundreds of mV of fast spikes, the filter caps are aging — common cause of "rig sounds noisy" complaints that aren't actually about the antenna.

```
    voltage (AC-coupled, 50 mV/div)
     │
   0 │ ──╲─╱╲─╱╲─╱╲─╱╲─╱╲─╱╲─╱╲─╱── healthy: 30 mV sinusoid at 120 Hz
     │
     │ ─╱╲─╱╲▕━┛▕━┛▕━┛▕━┛▕━┛▕━ failed: 300 mV switching spikes at 100 kHz
     │
     └─────────────────────────► time
```

### 3. Low-frequency audio-chain debugging

The audio path in a rig — mic → preamp → modulator → demodulator → speaker — is well below 10 kHz. A 100 MHz scope is total overkill, and at the same time the *right* tool. Look at:

- Mic preamp output waveform (should be a clean replica of speech)
- AGC behavior (slow envelope changes that should be smooth, not stepped)
- Audio filter ringing (should not "ring" after a sharp transition)
- Speaker driver waveform (clean reproduction of received audio)

Most rig service manuals give expected waveforms at named test points. A scope plus the service manual finds 90% of audio-chain faults in 10 minutes.

### 4. CW keying waveform / sidetone

CW key-up and key-down should be **shaped** — not abrupt — to avoid generating wide bandwidth ("key clicks"). The keying envelope on a scope should show 5–10 ms rise and fall times. A square-edged keying envelope generates clicks heard 10+ kHz away.

Procedure: TX a steady CW dit-dah-dit into a dummy load with a small RF sniffer probe (a 10 dB attenuator + diode + scope) and watch the envelope.

```
    envelope amplitude
     │
     │   ╱──────╲           ╱──╲                  ╱──────╲
     │  ╱        ╲         ╱    ╲                ╱        ╲
     │ ╱          ╲       ╱      ╲              ╱          ╲
     │╱            ╲_____╱        ╲____________╱            ╲___
     │
     │ ←dah→ ←space→ ←dit→ ←space→         ←dah→
     │   ↑                              ↑
     │  ~10 ms rise (good)             ~0 ms rise (key clicks!)
     └────────────────────────────────────────────────► time
```

Modern rigs do this shaping internally; older transmitters often don't and a 1980s HW-101 modified for QRP can be a click factory until shaping is added.

## When NOT to use a scope

Three things scopes are bad at, where you reach for a spectrum analyzer instead:

- **Harmonic content.** A scope shows the time-domain waveform; harmonics appear as waveform distortion, but you can't quantify them in dBc. SA shows them as separate spectral lines you can measure.
- **Receiver noise figure / sensitivity.** A scope can't display anything quieter than its own noise floor, which is typically -40 dBm on a good 1 GHz scope. Receivers care about -120 dBm signals.
- **Identifying a single mystery signal.** A scope time-domain trace of a complex RF environment is unreadable. SA shows you each carrier separately.

## Sampling rate, bandwidth, and the "look at a 30 MHz signal" problem

A scope's two key specs are **bandwidth** (analog front-end -3 dB cutoff) and **sample rate** (how often the ADC samples). They're not the same:

- **100 MHz bandwidth, 1 GS/s sample rate** is a typical entry-level digital scope (Rigol DS1054Z).
- **A 30 MHz signal viewed on a 100 MHz scope** looks distorted — even though 30 MHz is "below the bandwidth," the scope's response is already rolling off (typically -1 to -2 dB at 30 MHz on a 100 MHz scope), and harmonic distortion of a not-quite-pure 30 MHz signal will land near or above the -3 dB point.

**Rule of thumb: scope bandwidth should be ≥ 5× the signal frequency** to faithfully reproduce waveform shape (i.e. capture the first few harmonics). To see a 30 MHz signal cleanly you want at least 150 MHz of scope bandwidth, ideally 200 MHz+.

For HF amateur work this means:
- A 100 MHz scope is fine for **envelope** work up through 6 m (carriers up to ~50 MHz; envelopes are kHz–MHz).
- A 100 MHz scope is **not** fine for looking at the carrier waveform directly above 20 MHz.
- For 2 m / 70 cm carrier-waveform work you need a 500+ MHz scope, which gets expensive fast.

Sample rate has a related but separate rule: **Nyquist** requires ≥ 2× the highest frequency, but practical "faithful reproduction" wants ≥ 5× the highest frequency. A 1 GS/s scope is fine up to ~200 MHz of signal; a 500 MS/s scope only up to ~100 MHz.

> **Advanced —** Modern digital scopes have "DSO" architecture: the analog front-end has a fixed bandwidth (the -3 dB point), the ADC has a fixed sample rate, and the displayed waveform is reconstructed using sin(x)/x interpolation between samples. The interpolation works fine up to a few times below sample rate, but the reconstruction quality degrades as you push past Nyquist/2. Some older analog scopes (Tek 7000-series, HP 1700-series) had much higher *real* bandwidth than their digital descendants at the same price point — at the cost of no storage, no math, no decoding.

## The 10× probe rule

Scope probes come in two flavors:

- **1× probes** — direct connection. Show the actual voltage but heavily load the circuit (capacitive loading 50–100 pF).
- **10× probes** — built-in 9 MΩ resistor + 9 pF compensating capacitor + cable shielding. Divides voltage by 10 (scope must compensate) but presents only ~10 pF capacitive load — much less perturbation of fast circuits.

**Rule: always use 10× probes for RF work.** A 1× probe on a 14 MHz signal can pull the circuit's tuning off-frequency or kill the Q outright.

Quick check: compensate the probe before use. Probe the scope's built-in **probe-cal output** (a 1 kHz square wave). The displayed square should have flat tops. If the tops droop or ring, adjust the probe's trimmer cap until the square wave is square. A miscompensated probe distorts what you're seeing in a way that's easy to misdiagnose.

## Setting up to look at RF

For envelope work into a dummy load:

1. **Use a sampling probe / pickup loop** — a small loop of wire near the dummy load picks up enough RF for the scope. A 10 dB attenuator between loop and scope protects the input.
2. **Set timebase** to the modulation rate — milliseconds per division for SSB / voice, microseconds for CW keying envelopes.
3. **Set vertical** to ~200 mV/div initially; adjust based on signal strength.
4. **Trigger on the envelope** — auto trigger or normal-mode at a level near the envelope peak.

Never plug the scope's BNC directly into the rig's antenna jack at full power — a 100 W carrier delivers way more than the scope's input rating (typically 400 V max, but 100 W into 50 Ω is 70 V RMS continuous, with peaks that may exceed input ratings on overdrive).

## Common mistakes

- **Probing high-impedance points with a 1× probe.** Loads the circuit and shifts tuning. Always 10× for RF-adjacent work.
- **Forgetting probe compensation.** Distorts displayed waveform. Compensate before each session if probes are shared / swapped.
- **Probing the antenna output directly.** Damages the scope. Use a pickup loop or directional coupler with attenuator.
- **Trying to see a carrier when you should be seeing the envelope.** Slow the timebase. The carrier is at MHz; the envelope is at kHz / Hz.
- **Not zeroing the trace.** Ground-clip the probe and verify "0 V" reads as 0. DC offset in the probe or scope distorts envelope readings.

> **Advanced —** A high-end scope with FFT math turns into a poor-man's spectrum analyzer. The Rigol DS1054Z and Siglent SDS1104X-E have built-in FFT — limited dynamic range (~40 dB) and resolution bandwidth (one bin = sample rate / record length), but enough to spot a strong harmonic without a separate instrument. Not a substitute for a real SA when measuring spurious emissions to FCC limits, but useful for quick checks.

## See also

- §25-03 — Spectrum Analyzers (the right tool for frequency-domain work)
- §25-06 — Power & SWR Meters
- §13-03 — Distorted Audio
- §13-06 — Power Supply (using a scope to find ripple)
- §17 — Formulas (waveform math)
