---
id: 04-10
title: EchoLink
chapter: 04
section: 10
level: mixed
status: draft
---

# EchoLink

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

EchoLink links **analog FM** stations over the internet. Created by Jonathan Taylor (K1RFD) in 2002, it connects three kinds of endpoints — repeaters, simplex RF links, and plain computer/phone users — into one global network of a few hundred thousand validated hams. Its hallmark is approachability: you can be on the air through a repeater on the other side of the planet from your laptop in an afternoon. (For the open-source, higher-fidelity cousin, see §04-11 AllStar; for the comparison of all the linking systems, §04-05.)

## The three kinds of node

What you connect *to* — and how others see *you* — depends on the endpoint type, shown by the callsign suffix in the node list:

| Appears as | What it is |
|------------|------------|
| `W1ABC` (no suffix) | A **user** running the EchoLink app on a PC or phone — no RF on their end |
| `W1ABC-L` | A **simplex link** — a radio on a simplex frequency tying that local channel into EchoLink |
| `W1ABC-R` | A **repeater** node — a full repeater bridged to EchoLink |
| `*NAME*` | A **conference server** — a multi-user room for nets (e.g. `*ECHOTEST*`, node 9999, the test server) |

Every node also has a unique **node number**; that number is what you dial over RF.

## Validation — the one-time gate

Before your first connection, EchoLink makes you **validate your callsign** — upload a copy of your license (or let the system verify it against the FCC database). This proves you're licensed and is what keeps the network amateurs-only. Do it once at echolink.org; it can take a day or two to clear.

## Connecting

**Over RF (through an EchoLink repeater/link):**
- DTMF-dial the destination **node number** — e.g. key up and send `9 9 9 9` to reach the `*ECHOTEST*` server (the standard "is my audio getting through?" check).
- Disconnect with the node's defined sequence (often `#`).
- The local node IDs and announces connects/disconnects with voice prompts.

**From the app (PC / Android / iOS):**
- Browse the live list of online nodes, tap to connect, and PTT with the spacebar or the on-screen button.
- Handy while traveling — talk into your home repeater from a hotel. Some operators consider app-to-repeater "a bit like cheating," since your end has no RF; etiquette, not a rule.

## Operating etiquette — leave a gap

The one habit that marks an EchoLink newcomer is **not pausing**. There's internet latency plus, often, several link hops, and a courtesy-tone/squelch-tail delay at each repeater. So:

- **Wait 1–2 seconds** after someone unkeys before you transmit — both to avoid doubling and to let stations or nodes "break."
- **Wait** after you key up before you start talking, so the link path opens and your first syllable isn't clipped.
- **ID** per normal rules; the system isn't an excuse to skip it.
- Listen first — on a busy conference, transmissions arrive only after they decode/relay.

> **Advanced —** EchoLink audio is **compressed** (GSM 6.10 historically, ~13 kbit/s, with Speex as an option), which is why it sounds a notch below plain FM and well below AllStar's near-telephone-quality audio. Running a node yourself ("Sysop mode") means installing the EchoLink software with a radio interface and **forwarding ports** on your router — UDP 5198–5199 and TCP 5200 — so inbound connections reach your node. NAT/firewall issues are the usual reason a home node won't accept connections.

## When EchoLink shines

- No local repeater for the system you want, or you're traveling.
- Checking into a **net** carried on a conference server.
- Tying a small simplex area into the wider world with a `-L` node.

## When it doesn't help

- You want the best possible audio or an open, scriptable platform → AllStar (§04-11).
- The internet is down — like all linked systems, internet-routed contacts die instantly; only local RF survives.

## Common mistakes

- **Not pausing** for latency and link delay — doubling and clipped first words.
- **Skipping validation** then wondering why you can't connect.
- **Forgetting to ID** — the linked path doesn't exempt you.
- **Sysop port-forwarding** missed — node runs but never accepts inbound connections.
- **Camping on a busy conference** with an idle node — ties up the room.

## See also

- §04-05 — Linked systems (the overview/comparison of all of them)
- §04-11 — AllStar Link (the open-source, higher-fidelity analog linker; bridges to EchoLink)
- §04-01 — What a repeater is (linked systems are repeaters plus software)
- §24-11 — Cross-mode linking (bridging analog and digital networks)
