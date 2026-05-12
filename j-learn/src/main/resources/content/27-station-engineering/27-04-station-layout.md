---
id: 27-04
title: Station Layout
chapter: 27
section: 04
level: simple
status: draft
---

# Station Layout

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A station's physical layout — where the radio sits, where the paddle is, where the cables run, where the operator's chair is — determines how easy it is to *use* the station and how long the operator can use it without fatigue or injury. Good layout is invisible. Bad layout, you fight against every QSO.

This section is the layout view; §27-10 covers ergonomics in depth. Read both together if planning a new shack.

## The basic shape

The classic amateur shack has the operator facing a desk, with the major gear arranged at arm's reach:

```
        ┌────────────────────────────────────────────┐
        │                                            │
        │   ┌──────────────────────────────────┐     │
        │   │           Monitor                │     │
        │   │                                  │     │
        │   └──────────────────────────────────┘     │
        │   ┌──────┐  ┌──────┐  ┌──────┐  ┌─────┐    │
        │   │ Tuner│  │ Rig  │  │ Amp  │  │ PSU │    │
        │   └──────┘  └──────┘  └──────┘  └─────┘    │
        │                                            │
        │   ┌──Paddle──┐         ┌──Mouse/KB──┐      │
        │   │   ●●     │ Logbook │            │      │
        │   └──────────┘         └────────────┘      │
        │                                            │
        │                ▲                           │
        │           OPERATOR                         │
        └────────────────────────────────────────────┘
```

Key principles, applied in order of importance:

1. **Radio's front panel at eye level when seated** — or just below. Reaching up to a high shelf is uncomfortable. Looking down at a recessed unit is fine but not as natural.
2. **Paddle directly in front of dominant hand** — not behind the keyboard, not on the far edge of the desk. Wrist relaxed, elbow at 90°.
3. **Mic on a boom** — adjustable, swings out of the way for CW or digital, comes to mouth distance for phone.
4. **Keyboard and mouse on the side opposite the paddle** — so the operator doesn't have to switch hands.
5. **Logbook (paper or digital) within easy reach** — directly in front, between paddle and keyboard if both are used.
6. **PSU at the back** — heat rises, fans are noisy, you don't need to see the PSU often.

## Cable management

Cables are the silent killer of station ergonomics. They:

- Pull radios off the back of the desk when you move them.
- Catch on chairs and arms.
- Tangle into a knot that requires unplugging everything to fix.
- Make finding a broken cable a 20-minute archaeology project.

The fix is **discipline at install time**:

- **Each cable labeled at both ends.** Cheap label-maker tape (Brother P-Touch) or just masking tape and a Sharpie. "Coax → 80m dipole" on both ends.
- **Cables bundled by destination.** All the coax in one bundle; all the DC power in another; all the USB in a third. Velcro straps, not zip ties (Velcro lets you add or remove cables later; zip ties don't).
- **Service loop at each end.** Leave 6–12" of slack at each connection. Moving a rig 4" doesn't require unplugging anything.
- **Vertical cable drops** behind the desk into a tray or hook system — cables hang down, not lie in a tangled mess on the floor.
- **No coax on the floor.** Foot traffic on coax cracks it eventually. Run coax up the wall, across, and down to each rig.

A finished install looks deceptively bare from the front of the desk — most cables hidden behind, with only the in-use signal and power leads visible.

## The "trapped operator" anti-pattern

A common mistake: the operator's chair is pushed against the back wall, and the desk is in front, with all four walls or a corner blocking exit on every other side. To leave the operating position, the operator has to roll the chair *backward into a wall* and slide sideways out.

```
        ┌─────────────────────────────────────┐
        │                                     │
        │      ┌────────────────────────┐     │
        │      │      Desk + gear       │     │
        │      └────────────────────────┘     │
        │                ●                    │  ← chair
        │ ████████████████████████████████████│  ← wall right against chair
        └─────────────────────────────────────┘

         Operator cannot exit normally; must climb over desk or
         crawl sideways out of a corner. Catastrophic in a fire.
```

Why this is bad:

- **In a fire or emergency**, the operator can't exit quickly. *This has killed people.*
- **Long sessions become a trap** — the operator stays because getting up is awkward.
- **Visitors and family can't approach** comfortably.
- **Cleaning is impossible** — vacuum can't get behind the chair.

The fix: leave at least **3 feet of clear floor behind the chair**, in a direction that leads to a doorway. The operator can stand up and walk away without moving any furniture. This is also the OSHA / building-code expectation for occupied workstations and applies in spirit to home shacks.

If the room is small (closet shacks, attic stations), reorient so the desk is along the *side* wall and the chair faces *into* the room, with the doorway behind or to one side.

## Light placement

Light source position is one of those things nobody thinks about until they have a screen-glare problem on a long contest night.

**Wrong:** Overhead light directly behind the operator. Light bounces off the back of the operator's head and reflects off the monitor as a glare blob. The operator unconsciously tilts the head to avoid it and ends up with a sore neck.

**Wrong:** Bright window behind the operator during the day. Same problem, much worse.

**Wrong:** Single point-source LED bulb directly above the desk. The reflection on the radio's display LCD washes out the readings.

**Right:** Diffuse ambient light from above and slightly *in front of* the operator, plus a focused task light (gooseneck LED, $20) for the logbook and any paper materials. The monitor is at low brightness; the keyboard is dimly lit; the radio's LCDs are readable without competing with reflections.

The general rule: **light should not come from directly behind the operator's head, nor from directly above the operator's screen.** Side-and-front, or diffuse-from-front, is comfortable for hours.

For contesting, a single dim red LED or amber bulb (preserves dark adaptation, doesn't shock the eyes when you look up from the screen) is the night-shift standard. Many SO2R operators use this and report measurably less fatigue during 36+ hour contests.

## Heat and airflow

Even a modest HF station produces heat:

- 100 W rig: 50–80 W dissipated as heat (depends on duty cycle).
- 500 W amp: 200–400 W dissipated under transmit.
- PSU: 50–100 W.
- Computer: 50–200 W.

In a small closed shack room, this can raise temperature 5–15 °F over ambient during a long session. The radio doesn't mind; the operator does, and the gear's MTBF improves with cooler operation.

Layout considerations:

- **Don't stack gear without airflow gaps.** 1–2" between vertically stacked units; never block the rear or top of an amplifier.
- **Place hot equipment at the back/top.** Heat rises; the rig and amp at the top of the rack stay cooler if the lower shelves stay cooler.
- **Provide an exhaust path.** A small ventilation fan in the upper corner of the room, or a window vent during summer.
- **The operator's chair should not be in the path of hot-air exhaust** from a rig fan.

## A worked layout: a 6×8 ft spare bedroom

A typical small-shack arrangement, using one wall of a small bedroom:

```
        ┌─────────────────────── 8 ft ────────────────────────┐
        │ Door                                                │
        │  ┃                                                  │
        │  ┃                          Window                  │
        │  ┃         ╔══════════════════════════════════╗     │
        │  ┃         ║ Top shelf: amp, PSU, antenna     ║     │
        │  ┃         ║          switch                  ║     │
        │  ┃         ╠══════════════════════════════════╣     │
        │  ┃         ║  Monitor                         ║     │
        │  ┃         ║  Rig            Tuner            ║     │
        │  ┃         ╠══════════════════════════════════╣     │
        │  ┃         ║  Paddle    Notebook    KB+Mouse  ║     │
        │  ┃         ╚══════════════════════════════════╝     │
        6 ft                            ●                     │
        │                          chair                      │
        │                                                     │
        │   ←─── 3+ ft clearance ───→                         │
        │                                                     │
        │  Bookshelf                                          │
        └─────────────────────────────────────────────────────┘
```

The desk runs along the long wall. The operator faces the desk, with the door visible and accessible to one side. The window provides natural light from the side (not behind). Behind the chair is open floor leading to the door.

Total cost of the layout itself (excluding equipment): a $200 IKEA-or-similar desk, a $300 task chair (the most important furniture purchase — see §27-10), a $40 monitor arm, a $30 LED task light, a $20 Velcro-strap cable kit. Under $600 in furniture for a setup that will support comfortable operation for years.

## Related sections

- [§27-10 — Shack Ergonomics](27-10-shack-ergonomics.md) — the human-factors view of the same problem
- [§27-06 — Power Distribution](27-06-power-distribution.md) — DC wiring choices that simplify the layout
- [§27-05 — Ferrite Deployment](27-05-ferrite-deployment-strategy.md) — where chokes go in the layout
