---
id: 22-05
title: Operating Procedures
chapter: 22
section: 05
level: simple
status: draft
---

# Operating Procedures

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section is the practical operations guide for amateur radio in emergency and public-service contexts. The previous sections covered the organizations (ARES, RACES, MARS), the framework (ICS), the message structure (NTS, ICS-213), and the network frequencies. This section is what you actually do behind the radio, on the air, during an active operation.

## The activation chain

How an amateur emergency response actually starts:

### ARES activation

1. **Triggering event** — a served agency (Red Cross, hospital, county EM) needs communications support.
2. **Served agency contacts** the local ARES Emergency Coordinator (EC) — usually by phone.
3. **EC notifies** the section leadership (DEC, SEC, SM) and assesses scope.
4. **EC alerts** local volunteers — typically by phone tree, repeater announcement, or pager.
5. **Volunteers report** to a designated check-in frequency or location.
6. **Net Control Station** is established on a regional repeater or HF frequency.
7. **Operations begin** — assignments are made, field operators deploy, traffic flows.

Time from trigger to operational: typically 30 minutes to 2 hours depending on scope.

### RACES activation

1. **Government declaration** of emergency — county, state, or federal.
2. **Civil emergency management** activates the registered RACES operators per their roster.
3. **RACES operators report** to their designated locations (often the EOC, county OEM, designated shelter).
4. **Operations integrate** with the state/local emergency management framework — RACES is *part of* the government response, not auxiliary.

RACES activation typically requires the responsible government to formally request it; pre-positioned RACES operators may have specific shifts and assignments documented in advance.

### MARS activation

MARS members participate in regular nets continuously; "activation" is an ongoing pattern of training and exercise rather than a discrete event. During named DoD exercises or contingencies, MARS may receive specific tasking — but day-to-day MARS operating is the steady state.

## Net control procedures

A formal emergency net follows a structured pattern. Net Control Station (NCS) responsibilities:

### Opening the net

```
"This is WM3J, Net Control for the [event] Net.
The net is now formally open at [time].
This is a directed net for [purpose].
Stations needing immediate emergency assistance, please call now.
Otherwise, all stations may check in on a single transmission with
callsign and grid square. Proceed with check-ins."
```

Then NCS waits for check-ins. NCS records each check-in: callsign, location, status, capabilities, traffic if any.

### Managing traffic

Once check-ins slow, NCS asks for traffic:

```
"Stations with formal traffic, please call now."
```

Stations with messages identify themselves and what they have. NCS pairs them with stations that can deliver:

```
"WM3J, please listen up 5 for W1ABC who has a message for you."
```

Both stations move off-net to exchange the message, then return to confirm completion.

### Closing the net

```
"This is WM3J, Net Control. The [event] Net is now formally
closed at [time]. Thank you to all stations. WM3J 73."
```

A formal close transitions the frequency back to general operating.

### Check-in scripts (operator side)

What you say when you check into a directed traffic net depends on what you're bringing. The standard patterns:

**Plain check-in, no traffic:**

```
NCS: "Stations check in now."
You: "WM3J, no traffic."
NCS: "WM3J, copy, no traffic. Stand by."
```

**Check-in with traffic:**

```
You: "WM3J, one routine for Stafford."
NCS: "WM3J, copy one routine for Stafford. Stand by."
```

The format is: callsign + count + precedence + destination. Don't list the addressee on the air; the destination zone (city, region, area net) is enough at this stage.

**Check-in with multiple messages:**

```
You: "WM3J, one routine Stafford, one routine Manassas, one welfare Texas."
```

**Check-in as a relay station** (you can move messages to other nets):

```
You: "WM3J, liaison to the Eastern Area Net, no traffic."
```

**Late check-in** (NCS already past the check-in phase):

```
You: "Break for late check-in."
NCS: "Late check-in, go ahead."
You: "WM3J, no traffic."
```

**Checking out (leaving the net):**

```
You: "WM3J requesting permission to secure."
NCS: "WM3J, permission granted, 73."
You: "WM3J out."
```

Don't just disappear — checking out tells NCS you're no longer available for traffic assignments and stops them from calling you.

### Passing traffic on the side frequency

When NCS pairs you with another station, you both QSY to a side frequency to exchange the message. The protocol:

```
NCS:  "WM3J, please listen up 5 for W1ABC who has one routine for you."
You:  "WM3J copies, going up 5."
[Both stations move to the working frequency]
You:  "W1ABC, this is WM3J on the side frequency."
W1ABC: "WM3J, W1ABC. Are you ready to copy?"
You:  "Ready to copy. Go ahead."
[Message is transmitted using the radiogram protocol from §22-01]
You:  "WM3J copies number 47 from W1ABC. QRU. Over."
W1ABC: "Roger, no more for you. W1ABC clear."
You:  "WM3J clear, returning to the net."
[Both stations return to the net frequency]
You:  "Net Control, WM3J back on frequency, traffic complete."
NCS:  "WM3J, copy. Stand by."
```

Two key rules:

- **Always announce return to the net** so NCS knows you're back and available.
- **Don't editorialize on the side frequency** — exchange the message and clear. Other stations may need that side frequency.

### "Say again" procedures during traffic

Missing a word during a radiogram is normal. The cleanup patterns:

| You missed | You say |
|------------|---------|
| The whole text | "Say again the text." |
| Words after a known word | "Say again all after [word]." |
| Words before a known word | "Say again all before [word]." |
| One word | "Say again word after [word]." or "Say again word before [word]." |
| A line of address | "Say again line three of the address." |
| The check (word count) | "Say again the check." |
| The number | "Say again the number." |

If the receiving station's check doesn't match what they copied, they say:

```
"My count is 14, your check is 16. Say again the text."
```

The sender retransmits the whole text (faster than searching for which two words were missed).

### Net control etiquette

- **Wait your turn.** Don't transmit while NCS is calling.
- **Brief responses.** Single transmission for check-in; don't editorialize.
- **Take direction.** NCS decides the pace; follow.
- **Keep transmissions short.** Long transmissions block other operators.
- **Acknowledge clearly.** "WM3J copies" — let NCS know you got the assignment.
- **Roger your assignments.** When given a task, repeat it back to confirm.

## Tactical communications

Tactical comms is the moment-to-moment radio activity *inside* an event — not formal traffic, but the routine "do this, go there, status update" exchanges between stations supporting an operation.

### Conventions

- **Callsigns**: amateur callsigns are required by FCC at least every 10 minutes, but tactical operations often use **tactical callsigns** within transmission and append the FCC call at sign-off:

```
"Shelter Bravo, this is Net Control"
"Net Control, Shelter Bravo, go ahead"
"Bravo, Net wants 12 supply trucks at your location for tomorrow"
"Roger, 12 trucks tomorrow, Bravo out"
"Net out, KK6XX"   [FCC ID at end]
```

- **Brevity codes**: standard ICS terminology (Operations, Logistics, etc.) instead of agency-specific terms.
- **No casual chatter**: stick to the message.
- **One conversation at a time**: don't crosstalk; wait for the channel to clear.

### Tactical callsigns

A tactical callsign is a position-based identifier independent of the operator. Examples:

- **"Net Control"**: whoever is running the net at the moment.
- **"Shelter Alpha"**: the operator currently at shelter Alpha, regardless of which volunteer.
- **"EOC"**: the operator currently at the Emergency Operations Center.
- **"Mobile 1"**: a roving vehicle.

Tactical callsigns simplify message addressing — you don't have to remember which operator is on shift; you just call the position. The amateur callsign is appended at sign-off for FCC ID compliance.

## Specific scenarios

### Hurricane response

The Hurricane Watch Net (HWN) on 14.325 USB is active when a storm is within 300 miles of land. As an amateur in the storm's path:

1. **Pre-storm**: install storm shutters, charge batteries, secure antennas, fuel generators.
2. **Storm arrival**: check into HWN if you can; provide weather observations.
3. **During eye/peak**: report observed conditions every 30-60 minutes if possible. Wind speed (estimate from leaf/branch behavior, debris), barometric pressure, rain rate, damage observations.
4. **Post-storm**: continue reporting as conditions evolve. Assist with health-and-welfare traffic for evacuees.

Reports go to WX4NHC at the National Hurricane Center; NHC uses them in their forecast products.

### Severe weather (Skywarn)

When the National Weather Service activates a Skywarn net:

1. Tune to the regional Skywarn frequency (varies by NWS office; check local resources).
2. Check in with your callsign, location, and trained-spotter status.
3. Report observed weather: hail size, wind damage, tornado sightings, flooding, lightning, etc.
4. Stay on frequency until the warning expires or net closes.

Skywarn certification is required for formal reports; contact your local NWS office for free training.

### Disaster shelter operation

Deployed to a Red Cross or county shelter:

1. **Check in** with your supervisor (the shelter manager or assigned coordinator).
2. **Set up** your station in a designated location, typically near the shelter's command post.
3. **Establish** contact with the regional net control.
4. **Report** shelter status periodically: number of evacuees, supplies status, medical needs, security concerns.
5. **Pass** any ICS-213 traffic the shelter manager generates.
6. **Document** your activity in an ICS-214 log throughout the shift.
7. **Demobilize** when released — formal sign-out, return equipment, after-action notes.

### Mobile / vehicle communications

Operators deployed in vehicles for damage assessment, supply runs, or roving support:

1. Equipped with a mobile rig, GPS, and a way to receive directions from net control.
2. Identify by tactical callsign ("Mobile 1") plus FCC ID.
3. Report location and status periodically (e.g., every 30 minutes or at every checkpoint).
4. Avoid running the rig at high power for extended periods unless necessary — vehicle electrical systems can struggle.

## Activation drills and after-action reviews

Real emcomm operators **drill regularly**. Formats:

- **Tabletop exercises**: scenario-based discussion; no actual radio traffic.
- **Communications exercises (COMMEX)**: simulated emergency activation with real radio traffic on real frequencies.
- **Full-scale exercises**: integrated with the served agency; field deployment, multi-agency coordination.

After every real activation and many drills, an **After-Action Review (AAR)** captures:

- What worked.
- What didn't.
- What changes are needed for next time.

The AAR is a critical learning tool. Short, written, distributed to participants.

## Practical operator tips

- **Be where you need to be.** Show up to your assigned location promptly. If running late, notify net control.
- **Keep the rig running.** Battery condition matters; charge between shifts.
- **Carry copies of relevant forms.** ICS-213, ICS-214, blank radiogram pads. A clipboard with these prepped is faster than digital fumbling.
- **Don't volunteer outside your role.** Stay in your communications lane; don't try to do search-and-rescue or medical work as a side job.
- **Eat and hydrate.** Long shifts in stressful conditions; manage your physiology so you can do the radio job.
- **Switch operators.** Two people sharing a 12-hour shift is more reliable than one person doing the full 12 hours alone.
- **Know your limits.** When you're tired, hand off. A fatigued operator makes errors.
- **Write everything down.** ICS-214 is the log; use it. Memory is unreliable; written records are not.
- **Respect the chain of command.** Direct instructions come from your supervisor and ultimately the IC. Don't go around the chain.

## Common operating procedure mistakes

- **"I have a great idea"** — bypassing the chain to suggest something to the IC personally. Tell your supervisor; they handle escalation.
- **Talking over net control.** NCS is in charge of the net's flow; let them run it.
- **Long transmissions.** Brevity is critical when others are waiting. Make your point and pass back.
- **Forgetting to ID.** FCC requires identification every 10 minutes minimum during transmissions. Tactical callsigns count for tactical purposes, but FCC ID at sign-off is mandatory.
- **Operating without authorization.** ARES support requires the served agency's request; don't show up unannounced and start broadcasting "ARES net" frequencies.
- **Discussing operational details on the air.** Sensitive information (medical conditions, security issues, individual identities) shouldn't go in the clear. Use ICS-213 forms for sensitive content; don't talk over open repeaters.
- **Ignoring radio discipline.** This isn't a casual ragchew. Attention, brevity, and accuracy aren't optional in emcomm.

## Equipment readiness

What every emcomm operator should have ready:

- **Radio**: HF + VHF/UHF coverage; current firmware.
- **Backup radio**: a second working setup in case the primary fails.
- **Power**: charged battery; AC supply with cable; 12V cable for vehicle use.
- **Antennas**: portable wire antennas for HF; mobile whip for VHF/UHF; coax with appropriate connectors.
- **Cables**: short patches; long runs; extension cords.
- **Tools**: SWR meter, multimeter, soldering iron + solder, basic hand tools.
- **Documentation**: ICS-213/214 forms, radiogram pads, frequency lists, callsigns of key local stations.
- **Personal supplies**: water, food, weather-appropriate clothing for 24+ hours of operation.

A "ready bag" with this kit lets you deploy on short notice.

> ⚙️ **Advanced —** The amateur radio service is "secondary" to commercial and governmental services in disaster response — i.e., we support, we don't lead. The 47 CFR §97 rules permit amateurs to communicate during emergencies even without normal authorization (§97.405), but only when normal communications systems have failed and only when the assistance is necessary. ARRL's PSCM (Public Service Communications Manual) is the operational guide for ARES activations; FEMA's IS-100 and IS-200 cover the ICS framework. The dual identity — voluntary auxiliary with regulatory backing — is the distinctive American amateur-emcomm tradition.

## See also

- §22-00 — Chapter overview
- §22-01 — NTS (formal traffic-handling)
- §22-02 — ICS basics
- §22-03 — Emergency frequencies
- §22-04 — Message forms
- §02 — Repeaters & bandplans
- §19 — Q-codes & prosigns
- §14 — Shack inventory (knowing what gear you have for deployment)
