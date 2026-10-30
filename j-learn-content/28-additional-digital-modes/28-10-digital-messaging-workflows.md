---
id: 28-10
title: Digital Messaging Workflows
chapter: 28
section: 10
level: mixed
status: published
---

# Digital Messaging Workflows — How Emcomm Picks Modes in Practice

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

This section is the operational glue for the rest of the chapter. The previous sections introduced individual modes (Winlink, VARA HF/FM, Pactor, Robust Packet, etc.) one at a time. In a real emcomm deployment — a hurricane response, Field Day, an ARES SET (Simulated Emergency Test) — operators choose **which mode for which traffic** based on distance, traffic type, urgency, and what infrastructure is still standing.

This is the playbook that experienced emcomm operators carry in their heads.

## The tactical mode hierarchy

Pick the **lowest-effort mode that still reaches the destination**. In order of preference:

| Distance | First choice | Second choice | Last resort |
|----------|-------------|---------------|-------------|
| Same room | Hand a paper form | — | — |
| Same building / EOC | Voice on VHF/UHF FM repeater | VARA FM to local Winlink gateway | — |
| Same county | VHF/UHF voice, VARA FM, or Packet | Robust Packet HF (40m NVIS) | HF voice |
| Adjacent counties / state | VARA FM via mountaintop repeater | VARA HF (40m / 80m NVIS) | Pactor / HF voice |
| Cross-state / regional | VARA HF (40m / 20m) | Pactor III/IV | HF voice net (relay) |
| Cross-country / long-haul | VARA HF (20m / 30m / 15m) | Pactor IV | HF voice net (multi-relay) |
| International | Winlink via VARA HF or Pactor | HF voice relay | — |

Notice that **voice** is rarely the first choice in modern emcomm. Voice traffic is slow, error-prone, and consumes the operator's full attention. Digital messaging:

- **Self-documents** (the message has a hard copy).
- **Sends ICS forms** with structured fields.
- **Survives operator fatigue** (the radio operator only has to manage the link, not transcribe).
- **Composes faster** (the originating agency types into Winlink Express, no voice-relay intermediary).

## A walk through a typical SET incident

**Scenario:** simulated hurricane. County EOC needs to:

1. Send an ICS-213 status form to the state EOC (~200 km away).
2. Coordinate shelter intake with three local shelters (~5–30 km away).
3. Pass a Red Cross request for blankets to the regional warehouse (~500 km away).

**Step 1 — Set up local-area links (VARA FM):**

Each shelter has an HT or mobile rig and a laptop with Winlink Express + VARA FM modem. The county EOC's Winlink RMS is on a local repeater at 145.050. Shelters check in via VARA FM Winlink session, sending an ICS-213 with "shelter ready, 50 cots, no medical issues." Takes 20 seconds per shelter.

**Step 2 — Send to state EOC (VARA HF, 40m NVIS):**

County EOC opens VARA HF Winlink session on 7.105 MHz USB. RMS at state EOC's regional gateway is 180 km away — NVIS path on 40m is solid. ICS-213 transfers in 40 seconds. State EOC's Winlink Express receives the form, prints it, hands it to the duty officer.

**Step 3 — Cross-country to regional warehouse (Pactor IV or VARA HF, 20m):**

500 km to the regional Red Cross warehouse. 20m band is open mid-day. County EOC opens a VARA HF session at 14.108 MHz to a Winlink gateway near the warehouse. Pactor IV would also work — but the county doesn't own an SCS modem, so VARA HF it is. The blanket request ICS-213-RR transfers in 60 seconds.

**Total elapsed time, all traffic delivered:** about 10 minutes including setup. Doing the same by voice nets would take 30–60 minutes and require many trained net-control operators.

## Forms transmission

Winlink Express ships with hundreds of pre-built **ICS forms** as fill-in-the-blank templates. Common ones:

| Form | Use |
|------|-----|
| ICS-213 | General message |
| ICS-213-RR | Resource request |
| ICS-205 | Communications plan |
| ICS-205A | Communications list |
| ICS-309 | Communications log |
| ICS-214 | Activity log |
| HICS forms | Hospital incident command |
| Red Cross forms | Shelter manager reports, damage assessments |
| Salvation Army forms | EDS canteen reports |
| Local forms | Custom county / state templates |

The operator opens the form, fills the fields, hits "Post to Outbox." The form transmits as either:

- A **structured Winlink template** (sender and recipient both use Winlink Express, which renders the form).
- A **PDF attachment** (if the recipient uses regular email, they get a fillable PDF).
- A **plain-text rendering** (fallback for terminal-only clients like Pat command-line).

This is the killer feature for served agencies. They don't have to learn radio — they hand the radio operator a paper form, the operator types it into Winlink, and the form is delivered intact at the other end.

## Encryption rules

**FCC §97.113(a)(4)** prohibits "messages encoded for the purpose of obscuring their meaning" on amateur frequencies. This is absolute for normal traffic. There are narrow exceptions:

- **Telecommand of model craft / satellites** (out of scope here).
- **NTIA-coordinated emergency drills** where served agency uses standard government forms with encrypted attachments — and this is **only on specifically-authorized exercises**, not routine practice.
- **Personal information in ICS forms** — Winlink Express supports a "Sensitive PII" handling that **does not encrypt** but flags the message as restricted-distribution. The fact that PII is on amateur frequencies has been a topic of FCC clarification (2022): if it's truly sensitive, don't put it on the air.

For practical purposes: **do not encrypt amateur traffic.** Winlink, VARA, Pactor — all of these are designed to be FCC-compliant by transmitting in the clear with **compression but not encryption**. (Compression with a published algorithm is not encryption; the FCC has clarified this multiple times.)

> **Advanced —** Pactor's PMC and VARA's LZHUF compression are publicly-documented algorithms; their bytestreams can be decoded by anyone with the standard decompressor. This is why both modes are legal on amateur frequencies despite the data being unrecognizable on a casual ear. The legal test isn't "can a casual listener read it" but "is the algorithm published and reversible without a key." Compression passes; encryption (AES, etc.) fails.

## Field Day digital ops

Field Day's digital component has evolved with the modes:

- **2010s**: RTTY and PSK31 were dominant.
- **2018+**: FT8 / FT4 mostly took over.
- **2020+**: Winlink "Field Day Bonus" — sending a Winlink message qualifies for a 100-point bonus. Many clubs now do a Winlink session as a routine Field Day operation.

A standard Field Day Winlink message is sent **into the Winlink CMS**, picked up by an arbitrary gateway, and the message hits its target. For Field Day this is usually a friendly Winlink user at home who'll confirm receipt. The bonus rewards the practice of moving real radio email in a non-emergency setting.

## Tactical considerations

- **Multiple paths are better than one.** Practiced emcomm operators don't rely on a single mode — they have VARA HF, Pactor (if available), and VHF voice all available, and they switch when one fails.
- **The internet is not gone everywhere at once.** A regional outage may leave the CMS reachable from one side of the country and not the other. Always try Telnet first — if it works, use it; it's the fastest path.
- **Power management.** Long Winlink sessions on HF draw 25–50 W TX continuously for 30–60 seconds. Battery-powered stations need to plan: 25 W × 60 sec = 0.4 Ah just for one big email. Sustained ops need solar or vehicle charging.
- **Practice in advance.** The mode that works smoothly in a calm Sunday SET is the mode that works in a real incident. Don't try a new mode for the first time during a deployment.

## See also

- §21 — Emergency Comms (the home chapter for emcomm tactics)
- §28-01 — Winlink (the workhorse)
- §28-02 — VARA HF (the typical HF transport)
- §28-03 — VARA FM (the typical VHF transport)
- §28-07 — Pactor (when you have it, when you need it)
- §28-09 — Robust Packet (when AX.25 routing matters)
- §22 — Operating Practice (drill discipline, message handling)
