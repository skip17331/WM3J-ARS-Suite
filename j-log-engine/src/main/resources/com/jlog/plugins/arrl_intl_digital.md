# ARRL International Digital Contest — J-Log Contest Module

Plugin file: `arrl_intl_digital.json`
Engine support: `pointsByBandClass` (HF=1, VHF=2), `FieldDef.validator = "maidenhead"`, per-band multiplier accounting (`multiplierModel.perBand = true`), `grid_map` pane type, `Maidenhead` utility.

---

## J-Digi Contest-Mode Requirements (Generic)

Authoritative contract for J-Digi whenever any J-Log contest module (including this one) is active. J-Digi is a **contest-aware digital front end**; J-Log remains the **authoritative contest engine**. No code, APIs, or implementation details in this block — only the behavioural contract.

### 1. Dynamic Contest-Aware QSO Form
- J-Digi MUST build its QSO entry panel from the exchange fields defined in the active J-Log contest module.
- J-Log supplies the list of required exchange fields.
- J-Digi displays those fields and validates that each required field is populated before a QSO is accepted.
- No hardcoded contest-specific fields.

### 2. Contest-Aware Macro Variables
J-Digi MUST expose macro placeholders corresponding to each contest exchange field. Examples:
`<CALL>`, `<GRID>`, `<RST>`, `<SERIAL>`, `<POWER>`.
- Placeholders in outgoing messages expand to current-QSO values.
- Received values captured from the waterfall populate the matching placeholders in the entry form.

### 3. Contest-Aware Dupe Indication
- J-Digi MUST be able to determine whether the worked callsign is a dupe for the active contest.
- A dupe warning MUST be displayed before the operator commits the QSO.
- Accidental logging of a dupe SHOULD require an explicit override.
- Dupe scope (contest-wide vs per-band vs per-mode) MUST match the active contest's declared rule.

### 4. Contest-Aware Multiplier Indication
- J-Digi MUST be able to determine whether the received exchange represents a new multiplier under the active contest's rules.
- New-multiplier QSOs MUST be visually distinguished in the entry form.
- Multiplier scope (per-band, per-mode, per-contest) MUST match the active contest.

### 5. Contest-Aware Band/Mode Restrictions
- J-Digi MUST enforce the band and mode restrictions declared by the active contest module.
- If the radio's current band or mode is outside the allowed set, J-Digi MUST warn the operator and block QSO logging on invalid combinations.
- The UI updates immediately when the radio's band or mode changes.

### 6. Contest-Aware Auto-Logging
When the operator commits a QSO, J-Digi MUST:
1. Gather every declared exchange field.
2. Gather band, mode, frequency.
3. Package the QSO.
4. Submit it to J-Log for scoring.

J-Digi does NOT calculate points, multipliers, or dupes itself — it surfaces those states from J-Log and/or the shared contest database.

### 7. Contest-Aware Waterfall Integration
- Clicking a decoded callsign in the waterfall populates the contest QSO form.
- Exchange elements detected in decoded text (grid, serial, state) auto-populate the matching fields.
- J-Digi does NOT interpret contest rules — it only transfers parsed tokens into the form.

### 8. Contest-Mode Activation
- J-Digi enters contest mode when J-Log broadcasts that a contest module is active (and supplies the exchange schema).
- J-Digi leaves contest mode when J-Log broadcasts that no contest is active.
- In non-contest mode, J-Digi reverts to its normal digital-QSO form.
