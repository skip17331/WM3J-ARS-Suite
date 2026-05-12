---
id: 08-07
title: RF Burns
chapter: 08
section: 07
level: simple
status: draft
---

# RF Burns

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

An RF burn is what happens when you touch (or get near) a high-voltage point of a transmitting antenna. Unlike a thermal burn, which heats the skin from the surface, an RF burn deposits energy *into* tissue at high frequency, often producing deep, slow-healing injuries that can be more severe than they look on the surface.

This is **separate from MPE**: MPE rules govern chronic-exposure compliance (averaged over minutes); RF burns are acute injuries from contact with energized antenna components. A station can be 100% MPE-compliant and still produce an RF burn if you grab the wrong piece of metal during transmit.

This section explains how RF burns happen, what makes some installations particularly burn-prone, and how to design your station and habits to avoid them.

## How RF burns happen

The mechanism: a transmitting antenna develops standing-wave **voltages** (V) and **currents** (I) along its length. At certain places — typically the **ends of half-wave wires**, the **gaps of magnetic loops**, and the **outer ends of inverted V legs** — the V is very high (kilovolts at moderate power). A high-Q matching component like a vacuum capacitor in a magloop can also reach kilovolts.

When you touch one of these high-voltage points (or come within sparking distance), current flows through your body to ground. At RF frequencies:

- Skin effect makes the current flow predominantly in the surface layers of tissue.
- The capacitive coupling to your body (via fingertips) gives a low-impedance path at HF.
- Tissue with low water content (skin, fat) heats faster than tissue with high water content (muscle).

Result: a localized burn at the contact point, often deeper than thermal burn appearance suggests, and sometimes accompanied by visible arcing across the gap.

## Voltage levels at antenna components

Approximate peak voltages for antenna components at 100 W and 1500 W:

| Component | At 100 W | At 1500 W |
|-----------|----------|-----------|
| Center of dipole feedpoint (50 Ω) | 100 V | 387 V |
| End of half-wave dipole | ~700 V | ~2700 V |
| End of inverted V leg (low end) | ~700 V | ~2700 V |
| Tip of vertical antenna | ~700 V | ~2700 V |
| End of EFHW wire (far from feedpoint) | ~1500 V | ~5800 V |
| Gap of small magnetic loop tuning capacitor | 3–6 kV | 12–25 kV |
| Ladder line feeding a high-impedance antenna | 500–2000 V | 2000–7000 V |

These are sustained RMS voltages with the carrier on. **Peak voltages can be 2× higher for SSB modulation peaks.**

## What an RF burn feels like

At the moment of contact:

- A sharp, localized "hot pinch" sensation, like touching a hot wire.
- May or may not produce a visible arc (depends on voltage and skin conductivity).
- May produce an audible "snap" or "fizz."
- Pain is immediate and intense at the contact point.

After the burn:

- A small white or charred mark at the contact site, often the size of a fingertip or smaller.
- Deeper than it looks — RF burns can damage tissue 1–3 mm below the visible surface.
- Slow to heal (2–6 weeks for moderate burns).
- May leave a permanent scar.

For severe burns from prolonged contact with high-voltage RF (e.g., grabbing a magloop's capacitor at 1500 W and not letting go), the injury can include:

- Significant tissue necrosis below the contact site.
- Possible nerve damage with permanent numbness or tingling.
- Risk of secondary infection due to deep injury.

These are uncommon but documented.

## Where RF burns commonly occur

| Scenario | Risk level |
|----------|-----------|
| Touching the end of a wire dipole during TX | High — open ends are voltage maxima |
| Touching an inverted V leg's lower end during TX (especially if low to ground) | High |
| Contacting a magnetic loop's capacitor or its support structure during TX | Very high — kilovolts at moderate power |
| Touching ladder line during TX | High (depending on antenna's impedance match) |
| Touching coax shield during TX | Moderate (common-mode current; usually not severe but possible) |
| Touching a Yagi's element during TX | Moderate to high (peak voltages are at element ends) |
| Touching the inside of a feedpoint enclosure during TX | Variable; depends on the enclosure design and impedance |
| Touching the antenna tuner's coil or capacitor during TX (high-power tuner adjustment) | High |

## Specific high-risk scenarios

### Magnetic loop tuning at high power

The capacitor gap of a small mag loop is the highest-voltage point in any common amateur antenna. At 100 W on 80 m, the gap voltage can be 8 kV. At 1500 W, 30 kV. Touching anything near the gap during TX risks a serious burn or arc.

**Best practice**: motorize the tuning capacitor and adjust remotely. Never touch the loop or capacitor during TX, even at QRP. If the loop is at desk level and accessible, position yourself at least 2 ft from the gap during TX.

### Mobile installs with whip antennas

The tip of a mobile whip is a high-voltage point. At 100 W into a 102-inch whip on 40 m, the tip can carry 1 kV. Children (or adults) reaching for the whip get burned. **Mobile whips should have an insulated cap or ball on the tip**, and adults should be educated about not touching them with the rig in TX.

### Tower-mounted Yagi adjustment

Yagi elements have voltage maxima at their tips. A climber adjusting an element near the tip while a partner is keying the rig (a not-uncommon scenario) is taking severe burn risk. **Always lock the rig out of TX before tower work**, and use lockout/tagout procedures (a power-off switch with a physical lock, key in pocket of climber).

### Backyard field day with low antennas

Dipoles strung 8 ft above the ground at field day put high-voltage ends near eye level. **Tie ends off above 8 ft minimum**, and rope off the area around the antenna ends if children or non-radio guests are present.

### Interactive antenna tuning (manual tuner)

Manual tuning of an antenna tuner involves operating the rig in TX while touching tuner controls (capacitors, inductors). The tuner's controls are at the matching network's inside — high voltage, often in the kilovolt range. **Modern manual tuners insulate the knobs from the actual reactive components**, but only as much as the design intends. Be careful adjusting tuners during TX, especially at high power.

## Designing for burn safety

Three categories:

### 1. Component placement

- **Antenna ends above 8 ft** for any horizontal wire antenna in a populated area.
- **Antenna tips out of reach** — typically 12+ ft for an inverted V's low ends, well above any walking surface for mobile whips.
- **Magnetic loops behind a fence or barrier** in any space where children might be.
- **Tower antennas** generally elevated enough that ground-level personnel cannot reach the elements.

### 2. Visible warnings

- "RF — Do Not Touch" signs at antenna anchor points and on any controlling enclosure.
- Bright tape or flagging on antenna ends in low installations.
- Clear demarcation of "transmit area" near operating positions.

### 3. Operational practices

- **TX inhibit when the antenna is being touched.** Most modern rigs allow a remote inhibit input; use it (e.g., during tower work).
- **Brief family members** about not approaching antennas during TX, and how to recognize when the rig is active (audio indicator, transmit lights, panel display).
- **Use lower power for tune-up** procedures involving manual contact with antenna components.
- **Don't operate** the station with the antenna feedpoint exposed and reachable (bare wire connections, missing covers).

## First aid for RF burns

Treatment is similar to thermal burns of equivalent visible severity, with extra attention to depth:

1. **Stop the source**: take the rig off the air immediately. Don't continue transmitting while injured.
2. **Cool the burn**: cool running water for 15-20 minutes to halt continued tissue damage from residual heat.
3. **Cover loosely** with a clean dry cloth. Do not break blisters.
4. **Seek medical attention** for any RF burn, even small ones — the deep-tissue damage is often more than appearance suggests. Tell the medical staff it's an RF burn, not a thermal burn; the treatment is similar but they should look for signs of deeper damage.
5. **Don't rub or apply pressure** — this can spread the injury.

For severe burns (extensive area, deep, signs of nerve involvement, persistent severe pain): emergency room.

## What an RF burn is not

For clarity:

- **RF burns are not radiation burns** in the radiological sense. They are localized tissue heating from RF currents, similar to a thermal burn but from inside-out.
- **Touching coax shield during TX** doesn't usually cause RF burns — common-mode currents are usually not high enough on the shield for significant tissue damage. But it does indicate a balun problem (see §06-12).
- **The "tingling sensation" some operators feel** when touching radio gear during TX is RF "shock" — capacitive coupling of station ground potential through your body. Annoying, not usually injurious, but indicates poor station grounding (see §13-05).
- **MPE non-compliance does not imply burn risk.** Bystanders 30 ft from your antenna might exceed MPE limits without ever being at burn risk; burn risk requires direct contact with the high-voltage components.

> ⚙️ **Advanced —** RF tissue heating at HF and lower-VHF frequencies is dominated by **ohmic heating** in the highest-conductivity tissue layers (typically the dermis and subdermal fat). At higher frequencies (UHF and microwave) the dominant mechanism shifts to **dielectric heating** of water-rich tissue. This is why HF RF burns are often shallow (1–3 mm) but characterized by surface charring, while microwave RF damage is often deeper and characterized by inner-tissue hyperthermia. The thermal time constant of skin is on the order of 1 second; brief contact (< 0.5 s) is much less injurious than sustained contact.

## Common mistakes

- **Treating an RF burn as "just a small burn."** They look small and are often deeper than they appear. Get medical attention.
- **Assuming "low power means safe to touch."** Even 5 W into a half-wave wire produces 100+ V at the ends — enough for a sting and possible burn over multiple seconds of contact.
- **Skipping antenna-end insulators** ("just two pieces of wire to a fence post"). The wire ends produce kilovolts at moderate power; insulators isolate them mechanically and visually.
- **Ignoring the "tingle" from station equipment.** It indicates poor common-mode current management and is a precursor to RF burns at higher power.
- **Operating in close proximity to antennas at unfamiliar power levels.** A 100-W operator who suddenly upgrades to a 1500 W amplifier needs to re-evaluate where antenna ends sit relative to humans — voltages 4× higher than they're used to.

## See also

- §08-01 — FCC rules (MPE rules; not directly burn-related but related context)
- §08-06 — Safe antenna placement (geometry to prevent burn risk)
- §06-12 — Baluns and chokes (common-mode current → tingling/burn potential)
- §06-05 — Magnetic loops (kilovolt capacitor gaps)
- §13-05 — Grounding (poor grounding → tingling sensation; precursor to burns)
- §14 — RFI (related to common-mode currents)
