---
id: 14-02
title: Household Sources
chapter: 14
section: 02
level: simple
status: published
---

# Household Sources

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Modern houses are full of low-level RF emitters. Most are accidents of design — a switching converter that should have a snubber but doesn't, a USB cable that should be shielded but is bare. Together they raise your noise floor, sometimes dramatically. This section catalogs the offenders most likely to be in your house and what to do about each.

## The big-impact sources

### Switching power supplies

Almost every modern electronic device uses one — phone chargers, laptop bricks, LED drivers, CCTV cameras, smart speakers, the wall warts that came with everything you own. They generate RF hash from the switching frequency (20 kHz to 1 MHz) and its harmonics (often into the high-MHz range).

**Sound:** steady, continuous hash. Worse on lower bands.

**Quick test:** unplug suspect supplies one at a time; watch your S-meter.

**Fix:** replace cheap supplies with quality ones that have proper RF filtering. Apple, Anker, and brand-name laptop OEMs generally make quiet supplies. Random eBay supplies are often noisy. Cost: $20–40 per replacement.

Detail in §15-01.

### LED light bulbs and dimmers

The cheap LED bulbs sold at hardware stores have a tiny switching driver inside the bulb, and most have minimal filtering. A house with 30 cheap LED bulbs creates a measurable noise floor on HF.

**Sound:** harsh hash that varies with the dimmer setting. Worse when dimmed than at full brightness.

**Quick test:** turn off all the lights in the house. If the noise drops, LEDs are the problem.

**Fix:** replace cheap LEDs with quiet ones (Cree, GE Lighting, Philips Soft White). For dimmers, switch from "leading edge" to "trailing edge" types if your dimmer technology supports it. Cost: $5–15 per bulb.

Detail in §15-02.

### Plasma and old LCD TVs

Plasma TVs are notorious for HF noise; they're mostly out of production but plenty are still in use. Old LCD TVs with backlight inverters can also be noisy.

**Sound:** hash that varies with what's on the screen. Often worse on bright scenes.

**Quick test:** turn the TV off. Noise drops? It's the TV.

**Fix:** modern OLED or full LED-backlit TVs are typically quiet. If you can't replace, add ferrite chokes to the TV's power cord and HDMI cables. Cost: $0–20 in chokes; $400+ for a new TV.

### Ethernet over power (Powerline Networking)

Devices that send Ethernet over the AC mains using high-frequency carriers in the HF spectrum. They're directly designed to use frequencies you want to receive.

**Sound:** distinctive bursty noise at specific frequencies. Comes and goes with network activity.

**Quick test:** unplug the powerline adapter. Noise stops? It's that.

**Fix:** stop using powerline networking. Run real Ethernet cable, or use Wi-Fi. There is no satisfactory filter for powerline interference; it shares the spectrum with your hobby. Cost: time and cable runs.

Detail in §15-04.

### Solar PV inverters

Roof solar systems use inverters that convert DC to AC. Most have inadequate RF filtering and inject hash into the house wiring.

**Sound:** continuous broadband hash, only present in daylight.

**Quick test:** noise during the day, gone at night = solar inverter very likely.

**Fix:** request EMC-compliant inverter from your installer. Some brands (SMA, SolarEdge, Enphase) are quieter than others. Add common-mode filtering at the inverter's AC and DC connections. Cost: $0–500 depending on what works.

Detail in §15-03.

### Smart-home devices

Smart speakers (Echo, Google Home), smart bulbs, doorbell cameras, baby monitors, cordless phones, Wi-Fi mesh nodes. Each one is a small contributor; together they add up.

**Sound:** varies. Often a combination of switching-supply hash and Wi-Fi/Bluetooth bursts.

**Quick test:** unplug the network gateway. Notice anything?

**Fix:** prefer wired connections where possible; pick known-good brands; add ferrite chokes to power and Ethernet cables on offending devices.

### Doorbell transformers

Old mechanical doorbells use a 24 VAC transformer that, when degraded, can buzz audibly and emit RF.

**Sound:** 60 Hz buzz with subtle modulation.

**Quick test:** disable the doorbell at the breaker. Noise stops?

**Fix:** replace the transformer with a modern quiet unit. Cost: $20–40.

### Furnace blowers, well pumps, refrigerators

Motor-driven appliances generate brush noise (commutator-and-brush motors) or PWM hash (electronically commutated motors).

**Sound:** comes and goes with motor cycling. Worse when the motor starts.

**Quick test:** wait for the motor to stop. Noise vanishes?

**Fix:** add a snap-on ferrite at the motor's power supply. For commutator motors, replace worn brushes (a hum that's been getting worse over time often indicates this).

## The medium-impact sources

### Smart thermostats

Some smart thermostats (Nest, Ecobee) include radios for Wi-Fi/Bluetooth/Zigbee that emit during periodic syncs.

**Sound:** brief bursts at intervals.

**Fix:** typically not addressable; magnitude is small.

### Network gear

Routers, switches, Wi-Fi access points. Modern enterprise-grade gear is generally quiet; consumer gear is often noisier.

**Sound:** continuous low-level hash.

**Fix:** add ferrite chokes on power leads. Use shielded Ethernet cables (Cat6a STP) for runs that matter.

### Computer monitors

LCD monitors with cheap backlight drivers can radiate at specific frequencies. CRT monitors (still in some labs) are major emitters.

**Sound:** narrow tonal whine at one or two specific frequencies.

**Fix:** higher-quality monitor; add chokes to monitor power and signal cables.

### Battery chargers

Power tool chargers, laptop chargers, e-bike chargers. Same issues as switching supplies.

**Sound:** hash, only when charging.

**Fix:** unplug when not in use; add ferrites; replace with quality chargers.

Detail in §15-06.

### Inverters in vehicles

Especially relevant for hybrids and EVs in your garage, or RV inverters. The DC-to-AC conversion is a big switching source.

**Sound:** hash on lower bands, sometimes specific tones.

**Fix:** ensure vehicle ground bonding is good; add common-mode chokes on DC and AC sides; for inverters, look for "pure sine wave" units that are typically quieter than "modified sine wave."

## A practical inventory

Walk through your house and count: phone chargers in outlets, USB hubs, smart-home devices, LED bulbs, network gear, TVs and monitors, motorized appliances. A typical modern house has 50+ items in these categories. Most contribute a small amount; collectively they raise the noise floor by 5–15 dB on lower bands.

You can't fix all of them. Pick the worst offenders (test by switching things off). Replace or filter those. Accept the rest.

## A simple test process

1. **Set the radio to a quiet frequency** (no signals, just noise floor).
2. **Note the S-meter reading.**
3. **Turn off all breakers in your house except the radio.**
4. **Note the new S-meter reading.** Difference = your house's contribution.
5. **Turn breakers back on one at a time.** Note which breaker causes the biggest jump.
6. **For the worst breaker, turn off devices on that circuit one at a time.** Find the worst single device.
7. **Replace or filter that device.** Re-test.
8. **Repeat.**

Realistic expectation: you can typically reduce in-house noise by 6–10 dB through three or four targeted device replacements. Beyond that, returns diminish quickly.

## See also

- §14-05 — isolation workflow (how to systematically test)
- §14-06 — step-by-step elimination (the practical procedure)
- §15 — specific noise source categories with detail
- §15 — power line noise specifically (different beast — utility issue)
