---
id: 13-04
title: Ethernet Over Power
chapter: 13
section: 04
level: simple
status: draft
---

# Ethernet Over Power

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

**Powerline networking** (PLN, also called "Ethernet over power", PLC) sends data through your house's AC wiring using high-frequency carriers across exactly the spectrum you want to use for HF amateur radio. By design. There's no way to filter your way out of this; it's the radio equivalent of having a competing transmitter in your house.

## What it is

A pair of adapters: one plugs into a wall outlet near your router and connects via Ethernet cable; the other plugs into an outlet near a device (TV, gaming console, computer in another room). The two adapters use the AC wiring as a transmission line for data, modulating signals between roughly 2 MHz and 80 MHz.

Brands and standards:

- **HomePlug AV** — original standard, 2 MHz to 28 MHz.
- **HomePlug AV2** — extended to 80 MHz, including some "MIMO" modes that use both hot and neutral.
- **G.hn** — competing standard from ITU-T, similar idea, similar effects.

## Why it's terrible for amateur radio

- The carrier frequencies overlap **all the major HF amateur bands** — 80 m, 40 m, 30 m, 20 m, 17 m, 15 m, 12 m, 10 m.
- The signals **conduct on house wiring** and radiate from every wire in your house — the AC mains becomes a giant antenna for the powerline modem's spectrum.
- The signals **also propagate to your neighbors' houses** if you share a transformer (which is normal for residential wiring).

A pair of HomePlug adapters in active use can raise the noise floor on 20 m by 20 dB or more.

## What it sounds like

Distinctive: not the smooth hash of a switching supply, but **bursty data activity** with characteristic patterns:

- Roughly continuous when data is flowing (web browsing, streaming).
- Bursty during idle (the adapters do periodic "keep-alive" pings).
- Goes silent when one adapter is unplugged.

On a waterfall, you can see specific channelized usage — not a smooth haze but distinct frequency bands lit up.

## How to identify

The simplest test:

1. Note the noise floor on 20 m.
2. Unplug both powerline adapters.
3. Note the new noise floor.

If the difference is significant (10+ dB), powerline networking is the source.

You can also identify by the **bursty/intermittent character** of the noise — it varies with network activity, unlike a switching supply that produces continuous hash.

## What you can do

**Stop using powerline networking.** That's the only real fix.

Realistic alternatives:

### Run real Ethernet

Cat5e or Cat6 cable from the router to wherever you need wired Ethernet. Costs $30 in cable plus 1–2 hours of running it through walls or along baseboards. Permanent solution.

### Use Wi-Fi instead

If the device is portable or doesn't need wired speed, Wi-Fi is fine. Modern Wi-Fi 6 / Wi-Fi 6E performs better than HomePlug AV2 in most homes.

### Mesh Wi-Fi

For weak Wi-Fi coverage in distant rooms, a mesh Wi-Fi system (Eero, Google Wi-Fi, Asus ZenWifi) gives wired-Ethernet-like performance to all rooms without the powerline pollution.

### MoCA over coax

If your house has cable TV coax in every room, MoCA adapters use that coax (already shielded) for Ethernet. Quieter than HomePlug; comparable speed.

### Conduit-shielded Ethernet

For installations where running Cat6 is hard, run it through metal conduit. Slightly more work, but the conduit shields the cable from external RFI and prevents the cable from radiating.

## When you can't stop using it

If powerline is truly the only option (renting and can't run cable, or you don't control the network):

- **Choose adapters that minimize HF impact.** Some modern PLN adapters have improved filtering and notch the amateur bands. Check reviews.
- **Use the lowest performance tier you need.** A 200 Mbps adapter is less broadband than a gigabit one.
- **Use only when needed.** Unplug when not in use.
- **Plug into outlets far from your antenna feedline.** Doesn't eliminate the problem but reduces local pickup.
- **Operate during quiet times** — most network activity is evening; mornings may be quieter.

None of these fully solve the problem. They just reduce the impact.

## Educational note for non-radio people

Powerline networking is sometimes oversold. It's marketed as "easy" and "wireless-quality without Wi-Fi", but the practical reality includes:

- Variable performance based on house wiring (poor neutrals, multiple breakers, surge suppressors all degrade it).
- Some adapter brands work better with same-brand than mixed-brand.
- Performance often less than rated speed — the marketing claims of "1 Gbps" are best-case numbers.

If you're explaining to a household member why the new powerline adapters are coming back to the store: "The signal these use is exactly the frequencies my radio listens to. There's no way to filter it. Wired Ethernet is faster, more reliable, and doesn't interfere with anything." This usually gets buy-in.

## Regulatory situation

In the US, the FCC has formally limited PLN under Part 15, but enforcement is inconsistent. Some PLN models clearly exceed the limits when measured; complaints to the FCC about specific noisy units have occasionally resulted in product recalls.

Other countries have varied stances:
- **UK and EU** have stricter limits, leading to lower-power PLN products in those markets.
- **Japan** essentially banned residential PLN due to ham radio interference.

## See also

- §12 — RFI overview
- §12-08 — SDR waterfall (PLN has a distinctive waterfall signature)
- §13-01 — switching supplies (different noise type)
