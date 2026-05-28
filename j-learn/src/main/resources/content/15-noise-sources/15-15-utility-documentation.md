---
id: 15-15
title: Utility Documentation
chapter: 15
section: 15
level: simple
status: draft
---

# Utility Documentation

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The most important practical chapter on power-line RFI. Filing a complaint that gets acted on requires good documentation, the right contact, and persistent follow-up. This section walks the procedure that actually works.

## Before you call

Have ready:

- **Your name, address, and phone number.**
- **Your callsign** (so they know you're a licensed radio operator, not just a complaining customer).
- **A description of the noise**: how it sounds, what bands it affects, what time of day, what weather conditions.
- **The pole or location you suspect**: pole number, GPS coordinates, photographs.
- **Audio recordings** if you can produce them (cellphone audio of the noise on a portable AM radio works fine).
- **A polite, factual tone.** You're asking for help, not making demands.

## Finding the right contact

This is the single most important step. Different utilities have different processes:

### Step 1: call the customer service number

Tell them you have a power-line interference issue and ask for their **EMI** or **RFI** desk. Wording that helps:

- "Radio frequency interference from your equipment."
- "Power line noise affecting amateur radio reception."
- "I believe one of your insulators is arcing."

If the customer service rep doesn't know what you mean, ask to speak to **engineering** or **the operations supervisor**. Don't accept a "we'll send a billing person" — that won't help.

### Step 2: check the utility's website

Many utilities have an explicit "Radio interference" or "EMI" complaint form on their website. Examples:

- **Pacific Gas & Electric (CA)** — has a "Radio interference" web form.
- **Duke Energy** — has an EMI engineering team accessible via customer service.
- **Con Edison (NY)** — has a dedicated number for RFI complaints.

Search "[utility name] RFI" or "[utility name] amateur radio interference" to find the right channel.

### Step 3: ARRL utility liaison

The ARRL maintains relationships with major utilities. If you can't get through, the ARRL Section EMC Coordinator (find via `arrl.org/utility-rfi`) can help escalate.

## What to send

A complete complaint package:

```
Subject: Power line interference complaint — [pole #]

To: [utility EMI desk]

I am writing to report harmful interference from your distribution
system that is affecting my amateur radio operation.

Operator information:
  - Name: [your name]
  - Callsign: [your call]
  - Address: [your address]
  - Phone: [your number]
  - Email: [your email]

Interference details:
  - Symptoms: Continuous broadband HF noise, particularly strong on
    80m and 40m amateur bands. Peaks during dry weather; reduces
    significantly during heavy rain.
  - Direction-found to: Pole #1234567 at [GPS coordinates]
    (corner of Elm and Main Streets, your city)
  - Visible damage: Cracked porcelain insulator on the [west/north]
    phase, photographs attached.
  - Time pattern: Continuous, has persisted for [N weeks/months].

Documentation attached:
  - Photograph of suspect pole (overall view)
  - Photograph of cracked insulator (close-up)
  - GPS waypoint
  - Audio recording of noise (10 second WAV file)

I have direction-found this source using a portable AM receiver. I
would appreciate an inspection at your earliest convenience and
will be happy to assist your technician with on-site verification.

Thank you for your prompt attention.

[Your name, callsign]
```

This is significantly more detailed than the typical "my radio is buzzing" complaint and signals that you're serious and have done your homework. Utilities respond to this kind of complaint much faster.

## After filing

### Track the complaint

Get a complaint or work-order number. Note it. Reference it in any follow-up communication.

### Follow up at intervals

If you haven't heard back in **2 weeks**, call again. Be polite but persistent.

```
"I'm following up on RFI complaint #ABC123 filed on [date]. Could
you give me an update on its status?"
```

### When the technician arrives

If a tech calls to schedule an inspection, be available to meet them on-site. The single most useful thing you can do: walk to the suspect pole with the tech and have your portable AM radio. Demonstrate the noise.

The tech may not have a sniffer or may have a basic one. Your prepared evidence (recordings, photos) helps them confirm the diagnosis quickly.

### When the repair is done

Verify the noise is gone. Note the date. Email the utility a thank-you with confirmation:

```
"I'm following up on RFI complaint #ABC123. The noise was eliminated
within hours of the work performed on [date]. Thank you for the
prompt resolution."
```

This goodwill helps next time. RFI engineers remember which operators are easy to work with.

## When the utility doesn't respond

If you've waited 2-3 months with no progress:

### Escalate within the utility

- Call again. Ask for a supervisor.
- Email the engineering manager.
- File a formal complaint via the utility's official channel (often a written complaint logged with a public utility commission has more weight than a phone call).

### Contact your state Public Utility Commission

If the utility is regulated, your PUC can usually escalate. Document everything: dates, names, complaint numbers, attempts at resolution. The PUC takes time but moves utilities that ignore individual complaints.

### Contact the FCC

The FCC's Enforcement Bureau can intervene under Part 15 (utilities are subject to it). The complaint process:

- File an informal complaint at `consumercomplaints.fcc.gov`.
- Cite your callsign, the utility's name, the pole location, and the documentation.
- The FCC may forward to the utility for response.

Realistically, FCC enforcement of Part 15 against utilities is slow. Use this as a last resort, but having an FCC complaint on file helps when you escalate within the utility.

### ARRL escalation

The ARRL has lobbying relationships with utility associations. Contact your ARRL Section EMC Coordinator if you've exhausted other paths.

## A reasonable timeline

A typical successful complaint resolution:

| Time | Event |
|------|-------|
| Day 1 | Complaint filed with documentation |
| Week 2 | Utility acknowledges, schedules inspection |
| Week 4 | Tech visits, confirms problem |
| Month 3 | Repair scheduled |
| Month 4-6 | Repair done; problem resolved |

Don't expect Day-2 resolution. Expect month-long cycles. Persist.

## Maintaining the relationship

Once you've worked with the local utility EMI engineer, keep in touch:

- Send the occasional thank-you email.
- Report new problems quickly while you have the relationship.
- Be helpful (if a tech needs to find a specific pole, walk them through it).

A utility EMI engineer who knows you and trusts your reports will respond to your future complaints faster.

## Special cases

### Multiple operators reporting the same source

If several hams in your area hear the same noise, file complaints together (or at least reference each other). The utility takes "5 operators reporting noise from one pole" much more seriously than "one operator complaining."

The local club is the right channel for coordinating this. Many areas have formal RFI committees that aggregate complaints.

### Noise from a different state's utility

If the bad pole is across a state line, both your state's utility and the neighboring state's utility may need to be involved. The pole owner files the work order; coordination can be slow.

## See also

- §15-09 — arcing insulators (most common cause)
- §15-13 — AM radio identification (where the diagnosis comes from)
- §15-14 — SDR identification
- §14 — household RFI (for problems that turn out not to be utility-related)
