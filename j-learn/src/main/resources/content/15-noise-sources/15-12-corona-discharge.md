---
id: 15-12
title: Corona Discharge
chapter: 15
section: 12
level: mixed
status: draft
---

# Corona Discharge

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Corona is a partial electrical discharge in the air around a high-voltage conductor. Unlike an arc (which jumps a gap to a definite endpoint), corona is a continuous low-energy ionization of the air at sharp points or under high field strength. It happens on essentially all high-voltage transmission lines to some degree; it becomes a problem when conditions amplify it.

## How it sounds

Distinct from arcing:

- **Continuous "whoosh" or hiss**, like AC noise floor but with structure.
- Often **modulated by 60 Hz** — listen for a faint pulse synchronized with line frequency.
- **Worse in foggy, drizzly, or wet conditions** — water droplets on the conductor concentrate the electric field at sharp points.
- **Worse in fog than in heavy rain** — heavy rain washes contamination off, reducing local field concentration.
- Usually less intense than insulator arcs but more constant.

## Where it happens

- **Transmission lines** (115 kV and up): corona is normal at full design voltage; intensity scales with voltage.
- **Damaged conductors**: a strand of an aluminum conductor that breaks creates a sharp point, dramatically increasing local corona.
- **Contaminated insulators**: see §15-09; the insulator itself can corona before it fully arcs.
- **Sharp hardware**: clamps with sharp edges, broken hardware, etc.

Most residential distribution lines (4–34 kV) have minimal corona under normal conditions because the design voltages are below the corona threshold for the conductor diameter.

## How to find a corona source

If your noise pattern matches corona (whoosh, weather-modulated, persistent in wet weather):

1. **Walk along the route of any transmission lines** in your area (115 kV+ lines are visually obvious — large multi-conductor structures, often on wide easements).
2. **Listen with a portable receiver** (§15-13).
3. Note where the noise peaks — often a specific span of conductor or a specific tower.
4. Check for visible damage — a "spaghetti" of broken aluminum strands, ice damage from a storm, or hardware that's clearly been hit.

For distribution lines, corona is less common but not unheard-of after storm damage.

## What the utility can do

For transmission lines:

- **Conductor repair** for damaged strands.
- **Hardware replacement** for damaged or sharp clamps.
- **Voltage adjustment** rarely (corona scales with voltage; the line is at design voltage already).

For distribution lines: usually fix the underlying cause (replace the damaged conductor or hardware).

## A note on weather sensitivity

Corona is the easiest power-line noise to identify by weather correlation. If your noise:

- Is gone or minimal in dry weather.
- Appears in fog or light drizzle.
- Increases in heavy rain (initially) then decreases as the rain washes the contamination off.
- Returns as the conductor dries.

You're almost certainly hearing corona, not insulator arcs (which usually behave the opposite way).

> **Advanced —** Corona threshold for a conductor depends on diameter, surface condition, and air pressure/humidity. The Peek formula gives the gradient at which corona begins:
>
> `E_c (kV/cm) = 21.1 · m · δ · (1 + 0.301 / sqrt(r·δ))`
>
> where `m` is the surface roughness factor (1.0 for smooth, 0.7-0.9 for stranded), `δ` is the relative air density (1.0 at sea level standard), and `r` is conductor radius in cm. For a typical 1-inch (2.5 cm radius) ACSR conductor, the threshold is around 25 kV/cm gradient. A 230 kV line operates near this threshold; a 500 kV line operates well above and produces obvious corona under all conditions. Higher humidity and lower air pressure (high altitude) lower the corona threshold and increase emissions.

## How utilities mitigate corona

Modern transmission line design includes:

- **Bundled conductors**: multiple sub-conductors per phase, increases effective diameter, lowers gradient, suppresses corona.
- **Smooth surfaces**: cleaner conductor manufacturing reduces the surface-roughness factor.
- **Larger diameter**: thicker conductors have lower surface gradient.
- **Treated insulators**: hydrophobic coatings prevent water film formation.

Older transmission lines designed in the 1960s sometimes weren't optimized for corona. Modern rebuilds are quieter.

## Reporting corona

Same procedure as other power-line noise (§15-15). Mention that you suspect corona specifically; this helps the utility direct the right kind of inspector (transmission engineer vs. distribution lineman).

## See also

- §15-09 — arcing insulators (different mechanism)
- §15-13 — AM radio identification
- §15-15 — utility documentation
