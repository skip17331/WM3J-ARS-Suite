---
id: 24-12
title: Digital Voice Etiquette
chapter: 24
section: 12
level: mixed
status: draft
---

# Digital Voice Etiquette

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Digital voice rides on a worldwide internet-linked network, so a single careless habit on your handheld is felt by hundreds of listeners on three continents. The technology also behaves differently from analog FM — audio doesn't appear until a frame *decodes*, and there's latency through every link hop. The etiquette below is what keeps DMR, D-STAR, and Fusion pleasant to share.

## Listen before you transmit — and it's harder here

On analog FM you can hear a QSO in progress instantly. On digital, **audio only appears after the decoder locks**, so a channel that sounds dead may have a conversation a half-second from your speaker. Monitor a talkgroup or reflector for a good while before calling — longer than your analog instinct says.

## Leave generous gaps

Internet latency plus repeater/hotspot **hang time** means the path takes a moment to open and to release:

- **Pause after you key up** before speaking, or your first word is clipped while the link establishes.
- **Pause after the other station unkeys** before you reply — both to avoid doubling and to let other stations (and time-delayed linked nodes) break in. A 1–2 second gap is the norm; on a heavily-linked talkgroup, longer.

## Don't kerchunk

Keying up without talking ties up linked infrastructure worldwide and IDs you anyway on digital. If you need to test, use a **Parrot / echo** service (DMR talkgroup **9990**, or EchoLink `*ECHOTEST*`) — never the live network.

## Talkgroup & reflector discipline

This is where most friction happens:

- **Move long QSOs off the busy calling channels.** Don't ragchew on DMR **Worldwide (91)** or **North America (3100)**, or a continent-wide reflector — make contact, then move to a regional, statewide, or chat talkgroup.
- **Don't yank a link others are using.** A D-STAR repeater (or a DMR time slot) is linked to *one* reflector/talkgroup at a time; jumping it to another **drops everyone else's QSO** without warning. Check what's in progress before you relink, and **return the link to where you found it** when you're done.
- **Mind your hotspot.** A private hotspot keyed up on a worldwide talkgroup is broadcasting *you* globally. Keep casual and test traffic on local/parrot talkgroups.

> **Advanced —** On DMR specifically, remember the two-slot TDMA structure: your repeater's **slot 1 and slot 2 carry independent conversations**, and many talkgroups are slot-assigned by the repeater owner. Transmitting a static talkgroup on the wrong slot, or a long "PTT to link" on a user-activated talkgroup, can hold that slot against everyone else on the machine. Learn your local repeater's slot/talkgroup plan before you start changing TGs.

## The universal courtesies still apply

- **Identify** per your normal rules — the linked network doesn't exempt you.
- **Don't hold PTT** past the timeout timer; long-winded transmissions block the linked path for everyone.
- **Be patient with audio** — new operators often have mic-gain/deviation issues; a kind word fixes more than an annoyed one.

## See also

- §24-02 — DMR talkgroups (the IDs and slot assignments this etiquette protects)
- §24-05 — D-STAR routing (where reflector-yanking causes the most friction)
- §24-11 — Cross-mode linking (etiquette spans bridged networks too)
- §04-05 — Linked systems (the broader landscape)
- §04-10 — EchoLink (the same leave-a-gap latency etiquette on analog)
