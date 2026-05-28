---
id: 26-10
title: PCB Layout Basics for RF
chapter: 26
section: 10
level: mixed
status: draft
---

# PCB Layout Basics for RF

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## RF layout is not "digital layout with more care"

At DC and audio frequencies, a wire is a wire and a trace is a trace. Resistance matters; nothing else does. At RF, every wire is an inductor, every gap between traces is a capacitor, and every loop area is a magnetic-field antenna. The layout itself becomes part of the circuit's transfer function.

A schematic that simulates beautifully will refuse to oscillate (or oscillate when it shouldn't) if the layout is wrong. Two layouts of the same RF circuit can have 10 dB different performance. The advice in this section is the difference between "works" and "looks good in the simulator."

## Short traces, always

The single most important RF layout rule: **make every signal trace as short as possible**. The inductance of a PCB trace is roughly 20 nH per inch (for a typical 0.025" trace width on FR4). At 30 MHz, 1 inch of trace is 3.8 Ω of inductive reactance — significant for a 50 Ω circuit.

This means:

- Place components in the order they appear in the schematic. Don't run a signal across the board, do something to it, and run it back.
- Decoupling capacitors go **right at** the IC's power pin — not 2" away.
- Tank-circuit components (L and C) go right next to each other, with the signal node a single via or short trace.
- The output connector ideally is on the same edge of the board as the output stage, so the signal travels directly to the connector.

## Ground plane on at least one side

For RF work, **always use a double-sided PCB** with one entire side dedicated to ground plane. The ground plane:

1. **Provides a low-impedance return path** under every signal trace. The signal current's return flows directly below the trace, in the plane, minimizing the loop area.
2. **Shields** signal traces from each other (cross-coupling) and from external EMI.
3. **Acts as a thermal mass** for through-hole components soldered through to it.
4. **Bonds to the chassis** at many short straps, creating a unified ground reference.

For two-sided boards in KiCad or EAGLE, the convention is **bottom = ground plane, top = signal**. Through-hole component leads come from the top; the lead that's "ground" gets soldered to the plane directly.

For Manhattan-style construction, the **base copper-clad sheet is the ground plane**, and small islands of copper-clad glued on top are signal pads. Every component has one lead going to the plane and one to an island.

> **Advanced —** For four-layer boards (rare in amateur homebrew), the canonical arrangement is: top signal, ground plane, power plane, bottom signal. The two inner planes form a built-in bypass capacitor of significant value, dramatically improving high-frequency power-supply decoupling. Most amateur projects don't justify four-layer cost, but RF-amp-final or microwave projects sometimes do.

## Via stitching

Where the ground plane is on both top and bottom (or in inner layers), connect them with **vias every quarter-wavelength** (or closer) at the highest frequency of interest. This keeps the two planes at the same potential — they act as a single thick plane.

For HF work (up to 30 MHz), λ/4 ≈ 2.5 meters, so stitching is barely necessary. For VHF (144 MHz), λ/4 ≈ 50 cm — still loose. For UHF (440 MHz), λ/4 ≈ 17 cm. For microwave (2.4 GHz), λ/4 ≈ 3 cm — and stitching every 5 mm becomes mandatory.

The rule of thumb: **via every 0.25" for any work up to 1 GHz; every 0.10" for higher**. Place vias along the edges of the board and especially adjacent to any signal trace that runs near the edge.

## Microstrip vs stripline

At higher frequencies, the geometry of a trace **is the circuit** — the trace becomes a transmission line with a defined characteristic impedance, and you design around that.

**Microstrip**: signal trace on top of the PCB, ground plane on the bottom. Easy to lay out, easy to probe with a scope. The impedance depends on trace width, board thickness, and dielectric (FR4 has εr ≈ 4.4).

```
Cross-section:

  signal trace ─── (top copper, e.g. 0.030" wide)
       │
   ── FR4 (e.g. 0.062" thick) ──
       │
  ──────────────  (bottom copper, ground plane)
```

For 50 Ω on standard FR4 (0.062" / 1.6 mm): trace width ~0.110" (2.8 mm). For 75 Ω: ~0.050" (1.3 mm). Calculators are everywhere online; the Saturn PCB Toolkit is the standard amateur reference.

**Stripline**: signal trace sandwiched between two ground planes (inner-layer trace in a multi-layer board). Better shielding, less radiation, but you can't probe it without drilling. Used in commercial RF products; rare in amateur homebrew.

For amateur HF, **don't worry about characteristic impedance** unless the trace is more than λ/10 long (about 3 ft at 30 MHz). For VHF and above, microstrip impedance matters.

## Component placement for thermal management

RF amplifier final stages dissipate real power as heat — even a 5 W class-E PA may dump 1 W of waste heat. Components placed badly can cook nearby parts.

Rules:

- **Heat-generating parts** (PA transistor, voltage regulator, dropping resistor) **at the edge of the board**, ideally on the side nearest a chassis wall (which acts as a heatsink).
- **Heat-sensitive parts** (oscillators, precision resistors) **away** from heat sources. The crystal oscillator's frequency drifts with temperature; keep it cool.
- **Vertical airflow paths** unobstructed. If the chassis vents through the bottom, don't put a tall heatsink right above the vent slots.
- **Thermal vias** under TO-220 / TO-247 tabs — multiple vias from the tab pad through to the ground plane on the other side, conducting heat into the plane and out to the chassis.

For class-AB SSB amplifiers, the bias-stability resistor (often a 1 W resistor) should be **thermally coupled to the final transistors** — they share temperature, so bias drift tracks temperature drift. This is the "Vbe multiplier" or "diode-on-the-heatsink" trick in every push-pull amp design.

## Common KiCad / EAGLE traps

When using free CAD tools, watch for:

- **Auto-router output for RF.** Auto-routers optimize for length, not for impedance, ground integrity, or RF performance. **Manual route every RF trace.** Auto-route is OK for power/control nets.
- **No ground plane on inner layer.** KiCad defaults to a 2-layer board with no ground plane unless you explicitly add one. Add a copper fill polygon on the bottom layer, connected to GND, covering 100 % of the board area.
- **Forgotten ground vias.** A signal trace crossing the board may pull ground current with it, creating a long current loop. Add a ground via every 0.25" along the trace.
- **Decoupling caps far from the IC.** The schematic shows "0.1 µF from VCC to GND" — but the layout has the cap 0.5" away with thin traces. Move the cap directly under the IC.
- **Long traces parallel to each other.** Two parallel signal traces couple capacitively. Cross at right angles or separate by > 3× trace-to-plane spacing.
- **Sharp 90° corners on traces.** Slight reflection point at HF; insignificant below VHF but ugly. Use 45° corners or curves.

## Manhattan-style vs PCB — when each is appropriate

**Manhattan-style construction** uses a piece of copper-clad sheet as the chassis and ground plane, with small pads of copper glued on top for component terminations. The ground plane is uninterrupted; components are mounted "dead bug" style (IC upside-down) or with leads bent flat to the surface.

When Manhattan is fine:

- **Single-stage RF circuits** (single amp, single mixer) where the topology is simple
- **One-off prototypes** where you're tuning component values
- **Audio circuits** with RF on the input
- **QRP transmitters** up to several watts (Pixie, Forty-9er, BITX, etc.)
- **Filter banks** for a few bands

When you need a real PCB:

- **Multi-stage circuits** with > 5 active devices
- **Anything with surface-mount parts** (SMT)
- **Microwave** (above ~500 MHz) where transmission-line geometry matters
- **Anything you plan to build > 1 of** (PCBs become cheaper per unit at quantity)

Both approaches work. The classic *Solid State Design for the Radio Amateur* (W7ZOI and W1FB) shows a generation of Manhattan-style designs that compete with anything PCB-fabbed.

> **Advanced —** "Ugly construction" is even simpler — you don't even cut pads. Components solder lead-to-lead in midair, with one lead going to the ground plane and others to a junction node held up by the component's own rigidity. Wes Hayward W7ZOI made an art form of this; his "ugly weekender" 40 m CW transmitter is a 70-component circuit built without a single cut pad, working perfectly on the first power-up.

## PCB sourcing — modern reality

A 4-layer prototype PCB from JLCPCB or PCBWay (China-based) costs ~$5 for 5 boards in 2026, with 2-week lead time. OSH Park (USA) is ~$15 for 3 boards, 3-week lead time. The era of "homebrew means etching your own PCBs" is gone — even for one-offs, ordering a fab board is cheaper and faster than DIY etching.

For amateur QRP and learning projects, Manhattan-style is still preferred because:

1. The layout *is* the schematic, visible and understandable
2. Component swaps for tuning are trivial
3. No design-tool learning curve
4. No 2-week wait

For amateur projects intended to ship to others (kits, club builds), real PCB is essential.

## See also

- [§26-08 — Enclosures & Shielding](26-08-enclosures-shielding.md) — board-to-chassis bonding
- [§26-09 — Grounding for Homebrew](26-09-grounding-for-homebrew.md) — star ground inside the PCB
- [§26-02 — Low-Pass Filters](26-02-low-pass-filters.md) — typical layout case study
- [§17 — Formulas](../17-formulas/) — transmission-line impedance math
- [§25 — Test Equipment](../25-test-equipment/) — NanoVNA for layout verification
