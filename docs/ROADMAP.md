# WM3J ARS Suite — Roadmap

Where the suite is going. The 2026 push (i18n, modern installer, the
ten-module surface area) is in the bag and shipping — this file now
covers what's actively parked waiting for a contributor, what we've
deliberately chosen not to build, and the polish that runs forever.

> **How to read this file**
>
> - **Parked.** Items where the design and scope are clear but the
>   work hasn't been picked up. Documented so a future contributor
>   can pick one up without re-litigating whether it belongs.
> - **Intentionally omitted.** Capabilities the project has
>   consciously chosen *not* to build. Documented respectfully so
>   it's clear why, and so they don't keep reappearing as drive-by
>   proposals.
> - **Continuous.** Polish, triage, accessibility — runs in parallel
>   to everything else, never claims a "done" date.

---

## Parked

These have shipped detector-tier groundwork or partial scope and are
clearly defined; they're waiting for a contributor with time.

- **Embedded skimmer outbound RBN telnet.** The decode chain
  shipped Phase A → C over 2026-05-13: `LocalSkimmer` detects up
  to 16 CW carriers per audio frame; `MultiCarrierDecoder`
  spawns one AFC-locked `CwMode` per detected carrier and reaps
  idle channels after 5 seconds (`CwMode.setLockedCarrier`
  added); `CallsignScorer` extracts ITU callsign patterns with
  repetition / context / EOT scoring; `SkimmerSpotPublisher`
  promotes scored callsigns clearing 0.65 confidence into
  broker-format `SPOT` JSON with `source:"LOCAL_SKIMMER"` and a
  5-minute per-(callsign, kHz) dedup window. j-hub's
  `MessageRouter.handleInboundSpot` now runs unenriched app
  SPOTs through `SpotEnricher` so DXCC, bearing, distance, and
  local-time fields land the same way they do for cluster
  spots. 68 tests in j-digi (11 scorer + 9 decoder + 10
  publisher + the existing 38). **Remaining (Phase D):** a tiny
  TCP server on a configurable port that publishes the same
  spot stream to telnet clients in the standard RBN wire
  format, so other apps + the public RBN networks can subscribe
  to this station's skimmer. Useful for contesters who want to
  contribute spots; revisit when an operator wants to drive the
  work.

- **Voice-control listener (`j-voice`).** Offline Vosk model to
  parse phrases like *"tune to twenty meters"*, *"call CQ"*, or
  *"log this QSO"* into `RIG_CONTROL` / `MODEM_TX` / `QSO_SAVE`
  WebSocket messages. The hooks already exist on the broker side —
  this is purely a new module that emits the same messages a
  keyboard shortcut would. Compelling for accessibility and mobile
  operating, but a niche audience; revisit if a contributor wants
  to own it.

---

## Intentionally omitted / not doing

These are not on the roadmap and won't be. The decisions are settled,
but the reasoning is recorded so future proposers can read it before
opening a feature request.

- **Full internet-remote operation (WAN audio).** Real internet-
  remote needs sub-100 ms audio over WAN, push-to-talk over WebRTC,
  and a security model against rogue control of someone else's
  station. That's its own product category, and there are good
  options already operating in that space — RemoteHams, RCForb,
  FlexRadio Maestro. ARS Suite stays a station-side suite that
  pairs cleanly with whichever WAN-remote tool you prefer.

- **Full SO2R contest cockpit.** Serious SO2R contesters want the
  feature depth and decades of polish in N1MM+, WriteLog, and
  Win-Test. ARS Suite isn't going to outcompete those tools, and
  trying would only dilute the modules we already do well. The
  better integration story: keep ARS Suite logging, awards, and
  maps; let the contest software run the cockpit; bridge them via
  the existing UDP broadcasts — the same model we already use to
  defer to WSJT-X for FT8.

- **MAP65 / polarisation EME.** Niche within a niche; operators
  who need polarisation tracking are already running WSJT-X +
  MAP65 and have invested in the workflow. The EME-lite work that
  shipped in 2026 covers Doppler correction, libration prediction,
  and common-window planning — enough for the ~80% of EME-curious
  operators who want to know when the moon is up and what
  frequency offset to dial in, but who aren't building a
  polarisation tracker.

---

## Continuous (no phase, runs forever)

- **Per-release UI polish.** Each release picks one module and does
  a dedicated pass: keyboard nav, focus order, error-message
  wording, empty-state copy.
- **Issue triage.** Floor: close 3 issues per week. Diagnostics
  bundles attached via the in-app **Report an Issue** button feed
  this directly.
- **Quarterly accessibility audit.** One module per quarter gets a
  screen-reader pass and a keyboard-only test.
- **Annual beta-tester standup.** Ship a release named after the
  top-voted fix.
- **Translation review.** EN + ES ship embedded; DE / FR / IT / PT
  ride as drop-in `i18n-packs/<module>/` files and started life
  machine-translated. Native-speaker corrections welcome any time —
  it's just editing one `.properties` file and opening a PR.

---

*Last revised 2026-05-13. This document tracks intent; the
`MEMORY.md` index in the Claude memory directory tracks day-to-day
status of in-flight work.*
