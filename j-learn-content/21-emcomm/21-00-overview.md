---
id: 21-00
title: Emergency & Public Service Communications — Overview
chapter: 21
section: 00
level: simple
status: published
---

# Emergency & Public Service Communications — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

When the cell network is down, the internet is out, the power grid is dark, and 911 is overloaded — amateur radio still works. A 100-watt HF rig running off a car battery can reach a coordinator a thousand miles away over a propagated path. A 5-watt HT can reach a regional repeater and pass health-and-welfare traffic out of a damaged town. A linked digital network can move formal messages across a state in minutes when no other channel is functioning.

This is what amateur radio's public-service tradition is for: stepping in when the normal infrastructure fails. The chapter covers the **organizations** that coordinate amateur emergency response (ARES, RACES, MARS), the **operating frameworks** they use (NTS, ICS), the **frequencies and nets** where this activity happens (Hurricane Watch Net, MMSN, SATERN, regional emergency nets), and the **procedures** that make it work in practice.

## How the chapter is organized

| § | Topic | What it covers |
|---|-------|----------------|
| 21-01 | NTS | National Traffic System — formal radiogram message handling; how nets work |
| 21-02 | ICS basics | FEMA's Incident Command System — the structure that emcomm operates within |
| 21-03 | Emergency frequencies | Major emcomm nets — HWN, SATERN, MMSN, regional ARES nets, simplex frequencies |
| 21-04 | Message forms | Radiograms (ARRL form), ICS-213, formal traffic structure |
| 21-05 | Operating procedures | Net control, tactical communications, organizational activation |

## The three main organizations

There are three formal amateur emergency organizations operating in the United States, each with a different sponsor and different scope:

### ARES — Amateur Radio Emergency Service

- **Sponsor**: ARRL (American Radio Relay League).
- **Scope**: Voluntary served-agency communications (Red Cross, Salvation Army, hospitals, county emergency management, etc.).
- **Membership**: Open to any licensed amateur willing to volunteer; register with your local Section Manager / Emergency Coordinator.
- **Activation**: When a served agency requests support; coordinated through the local ARES leadership chain (EC → DEC → SEC → SM).
- **Authority**: None — ARES operates as volunteer auxiliary; serves at the request of the served agency.
- **Equipment**: Operator-owned amateur stations; operators bring their own gear.
- **Training**: ARRL-recommended training (FEMA ICS-100, ICS-200, IS-700, etc.); local groups may have additional drills and certifications.

### RACES — Radio Amateur Civil Emergency Service

- **Sponsor**: FCC under 47 CFR §97.407, run by state/local civil emergency management agencies.
- **Scope**: Civil-defense and emergency communications during officially-declared emergencies; part of state/local government emergency response.
- **Membership**: Amateur radio operators registered with the responsible civil-defense agency; required by federal law.
- **Activation**: When a state/local government declares an emergency requiring RACES.
- **Authority**: Federal — the President, FEMA, or other authorized agencies can activate RACES; state/local activation is also valid.
- **Equipment**: Often operator-owned, sometimes agency-owned; varies by jurisdiction.
- **Training**: ICS coursework typically required (FEMA IS-100, IS-700, IS-800); RACES-specific procedures vary by state.

> **Advanced —** RACES historically had unique operating privileges in declared emergencies that ARES did not — e.g., authorization to relay messages on behalf of government agencies. The 1990s-era Section 97.407 has been substantially relaxed but RACES retains its formal civil-defense designation. Many ARES groups in practice also have RACES designations through their county emergency management agencies; a single operator may participate in both with different authorities depending on the activation type.

### MARS — Military Auxiliary Radio System

- **Sponsor**: U.S. Department of Defense (Army MARS, Navy-Marine Corps MARS, Air Force MARS — three separate but coordinated programs).
- **Scope**: Auxiliary military communications support; backup voice/data circuits for DoD; community-relations functions.
- **Membership**: U.S. citizens with valid amateur radio license; require DoD security check and training.
- **Activation**: Continuous (members participate in regular nets); special activations for military training exercises and contingency support.
- **Authority**: DoD-sanctioned; MARS operators work outside amateur frequencies on dedicated MARS allocations (4.0, 5.0, 7.5, 14.5 MHz, etc., adjacent to amateur bands).
- **Equipment**: Member-owned; specific equipment standards required.
- **Training**: DoD-sponsored, including network operations, message handling, COMSEC awareness.

MARS members have an additional "MARS callsign" appended to their amateur callsign that's used in MARS operations.

## Why three organizations?

Each fills a different role in the emergency-response landscape:

- **ARES** = volunteer support to non-government agencies (Red Cross, hospitals).
- **RACES** = formal support to government emergency management (county OEM, state OES, FEMA).
- **MARS** = backup support to military and federal networks.

Many active emcomm operators belong to two or all three, with different responsibilities under each.

## What the public-service tradition looks like in practice

A typical amateur radio emergency activation:

1. **Event triggers activation** (hurricane warning, earthquake, severe ice storm, large-scale power outage, etc.).
2. **Local ARES Emergency Coordinator** is contacted by the Red Cross, county EM, or other served agency.
3. **Net control station** is established, typically on a designated frequency or repeater.
4. **Volunteers report in** to the net; positions are assigned.
5. **Field operators deploy** to shelters, EOCs, hospitals — wherever communications are needed.
6. **Traffic is passed** between field stations and the net control / served agency.
7. **Operations continue** for hours to days as the situation requires.
8. **Demobilization** when the served agency releases the volunteers.

This is the core pattern. Variations exist for specific event types (hurricane preparation, search and rescue, wildfire, planned events like marathons and parades).

## Public service vs. emergency

Two related but distinct functions:

- **Public service**: amateur radio support for planned events — marathons, parades, bicycle races, charity walks. Operators provide communications between event personnel, finish lines, water stops, ambulance crews. Useful, valuable, a step toward emergency capability through practice.
- **Emergency**: response to unplanned, often severe, events — natural disasters, large-scale infrastructure failures, missing person searches. Higher pressure, higher stakes.

The skills overlap heavily: net discipline, message handling, equipment readiness, ICS familiarity. Many operators do public service regularly to stay practiced for emergency activations.

## What this chapter doesn't cover

- **Specific local ARES/RACES procedures** — vary by section, state, county. Get involved with your local group.
- **MARS operating frequencies and procedures** — restricted to MARS members; published only in MARS-internal documents.
- **Training certifications** — FEMA IS-100, IS-200, IS-700, IS-800 are taken via the FEMA Emergency Management Institute (free online). Not detailed here; sign up at training.fema.gov.
- **Tower climbing, generator operation, vehicle communications setup** — operational details specific to deployment scenarios.
- **Mental health and stress response in disaster work** — important, but a separate topic from radio operations.

## Where the suite helps

J-Hub doesn't include a built-in emcomm-specific module. The general modules support emcomm work:

- **J-Sat** for satellite contacts including ISS amateur passes and NOAA weather satellites.
- **J-Hub Cluster** for spotting open frequencies during contests/events.
- **J-Log** for QSO logging during operations.
- **J-Bridge** for digital mode operation.

For dedicated emcomm software, look at WinLink Express (radio email), NBEMS (Fldigi-based message handling), or dedicated tactical packet systems.

## How to get involved

For ARES:

1. Identify your **ARRL Section** (arrl.org/sections; tied to your state).
2. Contact the **Section Emergency Coordinator (SEC)** or **District Emergency Coordinator (DEC)** for your area.
3. Attend a local meeting or net.
4. Complete the **FEMA ICS-100 training** (free online).
5. Volunteer for drills.

For RACES:

1. Contact your **county emergency management** office.
2. Express interest in RACES communications support.
3. Complete required ICS training.
4. Participate in scheduled exercises.

For MARS:

1. Visit the relevant service's MARS website (Army MARS, Navy-Marine MARS, Air Force MARS).
2. Apply for membership; pass a basic security review.
3. Complete training program.
4. Participate in regular nets.

## See also

- §21-01 — NTS (formal traffic handling)
- §21-02 — ICS basics
- §21-03 — Emergency frequencies (the major nets)
- §21-04 — Message forms
- §21-05 — Operating procedures
- §04 — Repeaters & bandplans (where many local emcomm nets operate)
- §19 — Q-codes & prosigns (formal traffic vocabulary)
