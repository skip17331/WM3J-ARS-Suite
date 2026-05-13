# WM3J ARS Suite — Roadmap

## Status: feature freeze

**No new features are being considered at this time.** The 2026 push
delivered the ten-module surface (J-Hub, J-Log, J-Map, J-Digi,
J-Bridge, J-Sat, J-Vault, J-Learn, Morse Trainer, J-Log Engine), the
six-language i18n rollout, the cross-platform installer, and the
embedded CW skimmer end-to-end. The next phase is **stabilization** —
shaking out bugs in everything that already exists and polishing the
edges before we expand the surface area again.

What that means concretely:

- New feature proposals will be politely declined for now, with a
  pointer to this file.
- Bug reports, crash dumps, and "this looks wrong" UX feedback are
  the most valuable thing you can send.
- Translations, accessibility fixes, documentation cleanups, and
  small UI polish PRs are all welcome — they fall under the
  stabilization work, not new features.
- The freeze isn't permanent. When the bug backlog is empty and a
  release cycle has gone by without a "this should be obvious but
  isn't" UX complaint, we'll revisit.

---

## What we're actively doing

- **Per-release UI polish.** Each release picks one module and does
  a dedicated pass: keyboard nav, focus order, error-message
  wording, empty-state copy.
- **Issue triage.** Floor: close 3 issues per week. Diagnostics
  bundles attached via the in-app **Report an Issue** button feed
  this directly.
- **Quarterly accessibility audit.** One module per quarter gets a
  screen-reader pass and a keyboard-only test.
- **Translation review.** English and Spanish ship embedded; German,
  French, Italian, and Portuguese ride as drop-in
  `i18n-packs/<module>/` files that started life machine-translated.
  Native-speaker corrections only need editing one `.properties`
  file and opening a PR.
- **Annual beta-tester standup.** The release after the standup is
  named after their top-voted fix.

---

## Intentionally omitted / not doing

These capabilities are deliberately not on the roadmap. The
decisions are settled, but the reasoning is recorded so future
proposers can read it before opening a feature request.

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

- **Outbound RBN telnet from the local skimmer.** The in-suite
  skimmer chain emits scored callsigns as broker `SPOT`s tagged
  `source:"LOCAL_SKIMMER"`, enriched by `SpotEnricher` the same
  way cluster spots are — enough for the operator's own station.
  A separate telnet server that re-publishes those spots in the
  standard RBN wire format would let contest software and the
  public RBN aggregators subscribe, but contesters in that
  workflow are already on N1MM+ / WriteLog / Win-Test driving
  real CW Skimmer Server installations — same deferral logic as
  the SO2R cockpit.

- **MAP65 / polarisation EME.** Niche within a niche; operators
  who need polarisation tracking are already running WSJT-X +
  MAP65 and have invested in the workflow. The EME-lite work that
  shipped in 2026 covers Doppler correction, libration prediction,
  and common-window planning — enough for the ~80% of EME-curious
  operators who want to know when the moon is up and what
  frequency offset to dial in, but who aren't building a
  polarisation tracker.

- **Voice-control listener (`j-voice`).** An offline Vosk model
  parsing phrases like *"tune to twenty meters"* or *"call CQ"*
  into existing broker messages would be a compelling
  accessibility feature, but it's a new module with a non-trivial
  testing and maintenance burden. Off the table during the
  stabilization phase; if it comes back, it'll be as a community
  contribution rather than a planned addition.

---

*Last revised 2026-05-13. This document tracks the current
posture; the `MEMORY.md` index in the Claude memory directory
tracks day-to-day status of in-flight bug/polish work.*
