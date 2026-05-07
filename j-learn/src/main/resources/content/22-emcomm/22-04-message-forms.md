---
id: 22-04
title: Message Forms
chapter: 22
section: 04
level: simple
status: draft
---

# Message Forms

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section covers the two formal message structures amateur emergency operators most often handle: the **radiogram** (NTS format) and the **ICS-213 General Message** (FEMA standard for emergency operations). Each has a defined structure, specific procedural conventions, and a workflow for transmission.

For the network that carries these messages, see §22-01 (NTS) and §22-02 (ICS). For the procedures used while transmitting them, see §22-05.

## The radiogram (NTS format)

The radiogram is the standardized message structure used by the National Traffic System. It has been essentially unchanged since 1949 and is the most-portable amateur radio message form — it can move via voice, CW, or digital with the same structure.

### Structure

A radiogram has four parts:

1. **Preamble** — administrative metadata about the message itself.
2. **Address** — who the message is for.
3. **Text** — the message content.
4. **Signature** — who originated the message (a person, not the radio operator).

### The preamble

A typical preamble:

```
NUMBER 47 ROUTINE WM3J 16 STAFFORD VA 1430Z NOV 15
```

Decoded:

- **Number 47** — sequential message number assigned by the originating station; resets at year-end or as the operator chooses.
- **Routine** — precedence (Routine, Welfare, Priority, or Emergency).
- **WM3J** — station of origin (the callsign of the operator who first put the message on the radio).
- **16** — check (count of words in the text).
- **Stafford VA** — place of origin (city, state of the originator, not the radio operator).
- **1430Z** — filing time (UTC time when the message entered the radio system).
- **Nov 15** — date.

Optional: handling instructions (HX codes) appended after precedence:

- **HXA** = collect call (don't accept charges); rare today.
- **HXB** = cancel after a certain time if not delivered.
- **HXC** = report delivery to originator.
- **HXD** = report failure to deliver.
- **HXE** = deliver only by phone, not in person.
- **HXF** = hold delivery until a specified date.
- **HXG** = delivery instruction supersedes phone delivery.

### The address

```
JOHN SMITH
123 MAIN STREET
ANYTOWN VA 12345
PHONE 555-1234
```

Each line transmitted with "BREAK" or pause between. Phone is optional but typical — the receiving station usually delivers by phone, so the number matters.

### The text

Up to 25 words is the customary maximum (longer is acceptable but conventionally avoided). The check (preamble) tells the receiver how many words to expect.

A typical text:

```
ARRIVED SAFELY HOTEL X CALL ME WHEN YOU CAN X LOVE STEVE
```

The "X" indicates a sentence break (period). Other procedural words:

- **QUERY** = a question mark.
- **STOP** = end of sentence (alternative to X, less common).

The check counts every word in the text — including "X" if used; including punctuation indicators.

### The signature

A single name (or two initials + name, etc.) — the person on whose behalf the message is being sent, not the radio operator. "Mary Smith" or "Steve" or "Dr. Johnson."

### Putting it together

```
NUMBER 47 ROUTINE WM3J 16 STAFFORD VA 1430Z NOV 15
JOHN SMITH
123 MAIN STREET
ANYTOWN VA 12345
PHONE 555-1234
BREAK
ARRIVED SAFELY HOTEL X CALL ME WHEN YOU CAN X LOVE STEVE
BREAK
END NO MORE
```

The "BREAK" separates the structure into sections; the "END NO MORE" closes the message and indicates no further messages from this station.

## The ICS-213 General Message form

The ICS-213 is FEMA's general-purpose form for messages within an active incident response. It's used by all ICS-trained agencies; amateur operators use it when supporting a served agency that's running ICS.

### Structure

The form has eight fields:

| Field | Content |
|-------|---------|
| 1. Incident Name | Which incident this message belongs to |
| 2. To (Name/Position) | Who the message is for, including their position in ICS |
| 3. From (Name/Position) | Who's sending the message |
| 4. Subject | Brief topic |
| 5. Date | Date the message was originated |
| 6. Time | Time of origination |
| 7. Message | The actual content |
| 8. Approved by | Signature of the person authorized to send |
| 9. Reply | Reserved for response from the addressee |

### Sample

```
INCIDENT NAME: Hurricane Maple Response
TO: Operations Section Chief
FROM: Communications Unit Leader
SUBJECT: Repeater Coverage Update
DATE: November 15, 2026
TIME: 1430

MESSAGE:
The 146.940 MHz repeater is now operational on backup
generator power. Coverage from Stafford to Manassas
restored. All ARES volunteers in that area can now
operate normally. Recommend updating ICS-205.

APPROVED BY: J. Smith, COML

REPLY:
[Empty - to be filled in by recipient]
```

### How ICS-213 differs from radiograms

| Aspect | Radiogram (NTS) | ICS-213 |
|--------|------------------|---------|
| Audience | General public ("send a message to grandma") | Within incident response only |
| Format | Strict structure, fixed preamble | Form-based; less rigid |
| Routing | Through NTS hierarchy | Within the ICS organization |
| Delivery | Via phone or mail to addressee | Hand-delivered or read aloud at meetings |
| Persistence | Saved by recipient | Filed in incident records |
| Volume | One message at a time | Many simultaneous |

NTS radiograms are for **outside-incident** communication (family notifications, business messages). ICS-213 is for **inside-incident** coordination.

## Prowords used in formal traffic

Prowords ("procedure words") are short conventional phrases that mean specific things during voice traffic. Using them consistently keeps copy clean.

| Proword | Meaning |
|---------|---------|
| **OVER** | I have finished transmitting; expecting a reply |
| **OUT** | This conversation is finished; no reply expected (never use OVER OUT — pick one) |
| **ROGER** | I received and understood your last transmission |
| **WILCO** | I will comply (implies ROGER) |
| **AFFIRMATIVE** | Yes |
| **NEGATIVE** | No |
| **BREAK** | Separator between segments of one message |
| **BREAK BREAK BREAK** | Interrupting an ongoing exchange (urgent) |
| **CORRECTION** | I made a mistake; the correct word is what follows |
| **DISREGARD** | Cancel my last transmission |
| **I SPELL** | The next group is spelled phonetically |
| **FIGURES** | The next group is numerals |
| **MIXED GROUP** | The next group has mixed letters and numerals |
| **INITIALS** | The next group is single letters |
| **READ BACK** | Repeat my entire transmission verbatim |
| **I SAY AGAIN** | I am repeating my own transmission |
| **SAY AGAIN** | Please repeat your transmission |
| **SAY AGAIN ALL AFTER X** | Please repeat from the word X to the end |
| **SAY AGAIN ALL BEFORE X** | Please repeat from the start to the word X |
| **SAY AGAIN WORD AFTER X** | Just the one word that came after X |
| **SAY AGAIN WORD BEFORE X** | Just the one word that came before X |
| **THIS IS** | The next callsign or designator is the transmitting station |
| **WAIT** | Stand by; I am pausing |
| **WAIT OUT** | Pausing for an extended time; I'll call you back |

These same prowords appear in military, maritime, and aviation radio practice — they're the lingua franca of formal voice radio.

### When to use I SPELL

Any time clarity matters: proper names, addresses, unusual words, addresses, callsigns. Pattern:

```
"The name is Smith. I spell. Sierra Mike India Tango Hotel. Smith. Over."
```

The proword **I SPELL** announces the phonetic spelling. The word is repeated after the spelling so the receiver can verify.

### When to use FIGURES

Numerals always:

```
"Phone is figures five five five one two three four. Over."
```

Prevents confusion between "five-oh" and "fifty," "one" and "won," etc.

## A fully-phoneticized worked example

Sending the radiogram from earlier on voice. The proper-procedure transmission:

```
WM3J:  "W1ABC this is WM3J. I have one routine for you. Over."
W1ABC: "WM3J this is W1ABC. Roger, ready to copy. Over."
WM3J:  "Number figures four seven, routine, WM3J, check figures
        one six, Stafford Virginia, time figures one four three
        zero zulu, November figures one five. Break.
        John Smith. I spell. Juliet Oscar Hotel November
        Sierra Mike India Tango Hotel. Smith. Break.
        Figures one two three Main Street. Break.
        Anytown Virginia. I spell. Alpha November Yankee
        Tango Oscar Whiskey November. Anytown. Virginia.
        Zip figures one two three four five. Break.
        Phone figures five five five one two three four. Break.
        Arrived safely hotel x-ray. I spell hotel. Hotel Oscar
        Tango Echo Lima. Hotel. Call me when you can x-ray.
        Love Steve. I spell Steve. Sierra Tango Echo Victor
        Echo. Steve. Break.
        End no more. Over."
W1ABC: "WM3J this is W1ABC. Roger, copied number four seven
        from WM3J. QSL the message. Over."
WM3J:  "W1ABC this is WM3J. Roger, QRU. WM3J out."
```

Notes on what's happening:

- **FIGURES** prefixes every numeric group so they aren't confused with letters.
- **I SPELL** prefixes phonetic spelling of any name or unfamiliar word, repeating the word afterward so the receiver can confirm what was being spelled.
- **BREAK** separates the preamble, address, text, and signature segments so the receiver can chunk their copy.
- **X-RAY** is spoken in place of "X" in the text so the receiver knows whether it's punctuation or a literal letter.
- **OVER** at the end of each transmission tells the other station "your turn."
- **OUT** at the very end signals "no more from me."

If W1ABC missed a word, the exchange becomes:

```
W1ABC: "WM3J this is W1ABC. Say again all after Hotel in the address. Over."
WM3J:  "W1ABC this is WM3J. I say again all after Hotel.
        Anytown Virginia. I spell. Alpha November Yankee
        Tango Oscar Whiskey November. Anytown. Virginia.
        Zip figures one two three four five. Break. Over."
```

This pattern (SAY AGAIN ALL AFTER) is faster than retransmitting the whole thing.

## Transmitting message forms

Three modes for moving formal messages over amateur radio:

### Voice

The traditional method. The sender reads each field aloud; the receiver writes it down. Phonetics for names, addresses, and unusual words.

**Pace**: approximately 12-15 words per minute for voice reception. Slower than CW but no special equipment needed.

**Etiquette**: announce the format ("I have an ICS-213; ready to copy?"); wait for "ready"; transmit field by field; pause between fields for the receiver to verify. If a fill is needed, sender re-transmits the missed words.

### CW

Faster than voice (20-25 WPM common); requires both operators to copy CW. Same basic structure as voice, but using prosigns (BREAK = `<BT>`, end of message = `<AR>`, end of contact = `<SK>`).

### Digital (Fldigi / NBEMS)

A relatively new approach: the message is filled out in a software form (Fldigi-FLMSG, etc.) that generates a structured digital transmission. The form is decoded at the receiving end and reconstructed identically.

Advantages: no copy errors, faster, supports complex forms with many fields, automatic logging.

**NBEMS** (Narrow-Band Emergency Messaging System) using Fldigi has become a standard for digital emcomm; supports radiogram, ICS-213, ICS-214, and other forms.

## Health and welfare traffic

A specific category of traffic that comes up during disasters: **health and welfare** messages, typically generated en masse from shelters. The pattern:

1. Shelter operator generates a list of shelter occupants who want to notify family they're safe.
2. List is converted to NTS radiograms (typically routine or welfare precedence) — often dozens or hundreds of messages.
3. NTS network moves them to the relevant areas.
4. Local NTS operators deliver via phone.

The check-of-format and integrity matter — getting names spelled wrong defeats the purpose. Phonetic spelling for every word in the text is normal procedure.

## Common message-form mistakes

- **Skipping the check.** The check is the receiver's integrity verification. Always count and announce.
- **Long text.** Beyond 25 words, the message becomes hard to copy reliably. Break long content into multiple messages.
- **Mixing radiogram and ICS-213 structures.** They serve different purposes; pick the right one for the situation.
- **No phonetic spelling for names.** "John Smith" needs "Juliet Oscar Hotel November Sierra Mike India Tango Hotel" for accuracy.
- **Forgetting to identify station of origin.** The radiogram preamble's "WM3J" identifies who first put it on radio; without it, debug-tracing message routing later is difficult.
- **Wrong precedence.** Using "Emergency" for non-emergency increases the noise during real emergencies. Reserve EMERGENCY for actual life-safety situations.
- **Not signing the ICS-213.** The "Approved by" field is required for the message to be valid; an unsigned ICS-213 is just a draft.

## Storage and records

After transmission, messages enter records:

- **NTS radiograms** typically have a logbook entry (sender's perspective and receiver's perspective). Some operators retain a delivery confirmation; ARRL provides a "service message" mechanism for status reports.
- **ICS-213 messages** are part of the official incident record. After action review may use them. Don't discard.

For digital-mode messages (NBEMS), the software typically logs everything automatically; no extra effort needed.

> ⚙️ **Advanced —** The radiogram preamble dates from telegraph operating practices of the 1880s; the structure was adopted essentially unchanged for amateur use in the 1949 NTS plan. The check ("count the words") is borrowed from commercial telegraphy where charges were per word; in amateur use it became an integrity check. The "X" for period was a Western Union convention. The HX codes (HXA, HXB, etc.) date from the 1960s as the radiogram was being formalized for amateur use; many are now obsolete (HXA "collect call" is meaningless in modern context) but the structure preserves them.

## See also

- §22-00 — Chapter overview
- §22-01 — NTS (the network for radiogram delivery)
- §22-02 — ICS basics (the framework for ICS-213)
- §22-05 — Operating procedures
- §19 — Q-codes & prosigns (vocabulary)
- §21 — Digital modes (NBEMS, Fldigi)
