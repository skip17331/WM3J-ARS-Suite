# ARS Suite — Editing Standard (cards, chiclets & components)

Use this whenever you (or Claude Code) add or adjust **cards, chiclets, drawers, or any module surface**, so every edit stays on-system. Paste the "Edit prompt" at the bottom into Claude Code with your specific change.

---

## 1. Non-negotiable rules
1. **Tokens only.** Never hard-code a color, radius, or shadow. Use the CSS custom properties from `ars-tokens.css` (`--surface-*`, `--border*`, `--t1..t4`, `--accent*`, status, radius, shadow). New colors → derive with `color-mix(in oklch, …)` or `oklch()` at the existing chroma/lightness. No raw hex in components.
2. **JavaFX-safe.** Solid fills, ≤1px borders, the two defined drop shadows. **No** `backdrop-filter`, blur, or heavy filters on native surfaces (Launcher, J-Log).
3. **Telemetry is monospace.** Any number a radio produces — frequency, azimuth, SWR, dB, serial, score, clock — uses `.ars-mono` (JetBrains Mono, tabular figures). Labels/prose stay IBM Plex Sans.
4. **The `--h` hue convention.** A card/chiclet/drawer tied to a module sets one local variable and derives everything from it:
   ```html
   <div class="card" style="--h: var(--log)">…</div>
   ```
   Tints come from `color-mix(in oklch, var(--h) <pct>%, var(--surface-2))`. Never use a module color directly as a big fill — always mix toward a surface.
5. **Status by more than color.** Running/active = hue tint **+** left accent bar (or dot) **+** a text label. Never color alone (accessibility + light theme).
6. **Both themes.** Anything you add must read in light mode (`.ars-root.ars-light`). Because you used tokens (rule 1), it will — don't special-case unless required.

---

## 2. Card anatomy (the standard pattern)
A module card / chiclet is built from these layers. Match this when editing:
```
container         background: var(--surface-2);  border: 1px solid var(--border);  border-radius: var(--radius);
  └ running       background: color-mix(in oklch, var(--h) 7%, var(--surface-2));
                  border-color: color-mix(in oklch, var(--h) 30%, var(--border));
                  ::before  left accent bar  width:3px  background: var(--h)
  ├ icon tile     38px  radius 9px  bg color-mix(--h 18%, --surface-3)  color var(--h)  border color-mix(--h 28%, transparent)
  ├ title         14–15px  weight 600  color var(--t1)
  ├ tag/sub       11px  color var(--t3)
  ├ stat line     mono  color var(--h) when live, else var(--t3)
  └ footer        state label (uppercase, 11px, var(--ok) when running) + action button
hover             border-color: var(--border-glow);  (tiles may translateY(-2px))
```
Reference implementations to copy from: `dir-a.jsx` `.a-card`, `dir-b.jsx` `.b-tile`, `dir-a-drawer.jsx` `.ad-chic`, `jlearn.css` `.jl2-deck`, `jvault.css` `.jv-*`.

## 3. Chiclet (module tile) rules
- Square-ish, icon + name + status; the icon tile is the hue anchor.
- Collapsed/mini state shows icon only with a corner status pip; expanded adds name + stat.
- Launch/Stop is the primary action; running state uses the card "running" treatment above.

## 4. Drawers — use the shared component, don't re-roll
Any collapsible right-rail panel = `SuiteDrawer` (`suite-shell.jsx`):
```jsx
<SuiteDrawer title="Antenna · Rotor" hue="sat" glyph={glyph} defaultOpen summary="045° NE">
  …body…
</SuiteDrawer>
```
- `hue` = a module/token name (`sat`,`map`,`digi`,`bridge`,`log`,`vault`,`learn`,`accent`).
- `summary` shows in the header when collapsed (a glanceable value).
- Body helpers already styled: `.sx-kv` (label/value row), `.sx-swx` (4-up metric grid), `.sx-bc` (band-condition rows), `.sx-ro-*` (rotor). Reuse these before inventing markup.
- Standard instrument set (rotor/prop/space-wx/weather) = drop in `<SuiteInstruments defaultRotor={50} />`.

## 5. Spacing & layout
- Card padding 14–15px; gaps 11–13px; rail padding 11px, drawer gap 9px.
- Density modes (compact/comfortable/spacious) are conceptual — if you implement them, scale padding/gap/font from the comfortable values above, don't redraw.
- Right rails are ~300–322px; left dock 58px collapsed / ~212px hover.

---

## 6. Edit prompt template (paste into Claude Code)
> **Context:** Editing the ARS Suite. Follow `design/ars-suite/EDITING-STANDARD.md` and use `ars-tokens.css` tokens only.
>
> **Change:** <describe — e.g. "add a 'Temp' card to the J-Log instrument rail" or "give J-Sat chiclets a max-elevation badge">.
>
> **Requirements:**
> - Build it from the card/chiclet anatomy in §2/§3; set `--h: var(--<module>)` and derive tints via `color-mix`.
> - Telemetry values in `.ars-mono`; labels in IBM Plex Sans.
> - Status conveyed by tint + bar/dot + label, not color alone.
> - JavaFX-safe (no blur/filters) if this lands on the Launcher or J-Log.
> - Must read in both dark and `.ars-light`.
> - If it's a collapsible panel, use `SuiteDrawer`; if a standard metric row, use `.sx-kv`.
> - Don't introduce new hex colors, radii, or shadows — reuse tokens.
>
> **Acceptance:** Matches the reference visual weight of existing cards in the same module; no raw hex; works in both themes; (native surfaces) no disallowed CSS.

---

## 7. Quick "is this on-system?" checklist
- [ ] Only token vars / `color-mix` / `oklch` — no stray hex
- [ ] `--h` set once, tints derived from it
- [ ] Mono on every numeric readout
- [ ] Running/active = tint + bar/dot + label
- [ ] Reads in light theme
- [ ] Native target: no blur/filter/backdrop
- [ ] Collapsible? → `SuiteDrawer`. Metric row? → `.sx-kv`
