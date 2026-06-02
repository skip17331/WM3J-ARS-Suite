# Scoring Extraction — Refactor Plan (scoped, not yet started)

**Goal:** lift contest scoring / multiplier / dupe logic out of the JavaFX
`ContestLogController` into a **pure, UI-free, unit-testable API in
`j-log-engine`**. Today this logic is entangled with the UI, the DB singletons,
and live entry-field reads, so it can't be tested or reused. Extracting it gives
the suite its **first testable scoring surface** and lets the **plugin builder**
do a faithful contest score/multiplier preview.

> Status: planning. Nothing here is built yet. Numbers/lines were read from the
> source (2026-06-02); re-verify before each stage.

---

## 1. What's being moved (inventory)

`j-log/.../controller/ContestLogController.java` (4,014 LOC) — scoring/mult/dupe ≈ **1,150 LOC**:

| Piece | Lines | Notes |
|---|---|---|
| `computeQsoPoints` + 11 `*Points` + qso_party helpers | ~380 LOC | per-QSO points; 13 point-path branches |
| `updateStats` | 2533–3206 (673 LOC) | multiplier count + total score; 16 branches; **~40% is `Platform.runLater` map-paint** |
| dupe dispatch in `buildRecord` + `isRover`/`fdModeClass` | ~100 LOC | sets `q.setDupe(...)` |

Already **pure and ready to move** — `com.jlog.scoring.*` (9 files, **711 LOC**:
AriDx, AsianEntities, DxccResolver, OceaniaDx, QsoParty, RussianDx, Scandinavian,
Wag, WaeMultiplier) — currently in `j-log`, no JavaFX/DAO/AppConfig deps (only
`CallsignRegion`, already engine-side, + two JSON resources DxccResolver loads).

Already **engine-side & reusable:** `ContestQsoDao` (all dupe + `distinctField*`
+ `fetchByContest` + `totalPointsByContest` + `findByCallsign`), `ContestQtcDao`,
`ContestPlugin` (incl. `pointsForBand/Mode/regionPair`), `QsoRecord`, `AppConfig`,
`CallsignRegion`, `Maidenhead`, `BandPlan`, `MultiplierLists`.

## 2. The blockers (why it isn't already clean)

1. **Scoring reads live UI fields, not `QsoRecord`.** `computeQsoPoints` reads
   rookie `year_rcvd` (1912) and distance grids (1932–33) via `getFieldValue`;
   the dupe block reads `grid_rcvd` (1748,1763) and `state_prov_rcvd` (1801);
   `qpMyQth` reads `state_prov_sent` (2242). A pure scorer must get these from
   the record / a context object.
2. **`updateStats` fuses compute + UI.** Every branch ends in `Platform.runLater`
   mutating ~20 FXML nodes (labels + map panes). Compute is interleaved with paint.
3. **Hard-wired singletons.** `ContestQsoDao/ContestQtcDao/DxccResolver/AppConfig
   .getInstance()` called directly inside scoring → needs a live DB to run.
4. **Mid-computation DB scans.** Per-QSO `qpMyQth` and most aggregate branches
   call `fetchByContest(contestId)` — scoring assumes a populated `contest.db`.
5. **`multColumn` is mutable controller state** (set from `computeMultiplierColumn`);
   must become a derived value of the plugin.
6. **Points vs mults computed differently** — points are the stored `SUM(points)`
   column; mults are recomputed from `fetchByContest`. The scorer should own both.
7. **Zero existing tests** anywhere in j-log / j-log-engine — both the risk and
   the prize.

## 3. Target API (engine, UI-free)

New package `com.jlog.scoring` in `j-log-engine`:

```java
/** Everything scoring needs about the operator's station, gathered once by the caller. */
record StationContext(String ownCallsign, String ownGrid, String sentQth, String sentSection) {}

/** Aggregate result; tokens let the UI paint maps without re-deriving anything. */
record ContestScore(int qsoCount, long points, int mults, long score,
                    Map<String, Set<String>> workedTokens) {}

final class ContestScorer {
    // per-QSO points — pure; q must already carry the rcvd fields (year/grid/QTH)
    static int points(ContestPlugin p, QsoRecord q, StationContext ctx);

    // dupe — pure over the candidate + the prior QSOs for that callsign
    static boolean isDupe(ContestPlugin p, QsoRecord candidate, List<QsoRecord> priorForCall);

    // aggregate — pure over the full (dupe-flagged) QSO set
    static ContestScore score(ContestPlugin p, List<QsoRecord> qsos, StationContext ctx);
}
```

The controller becomes a thin adapter: build `StationContext` from `AppConfig`,
ensure the `QsoRecord` carries its rcvd fields, call `ContestScorer`, then
`Platform.runLater` to paint labels/maps from `ContestScore.workedTokens`. No
scoring math left in the UI.

## 4. Staged plan (each stage builds, ships, and is reversible)

**Stage 0 — relocate the pure helpers.** Move `com.jlog.scoring.*` (9 files) +
`DxccResolver` + its two JSON resources into `j-log-engine`; fix imports. No
behavior change. *Verify:* full suite builds; j-log + j-digi run; the builder can
now see the helpers. *Risk: low* (mechanical). *Payoff: immediate* (builder reuse).

**Stage 1 — feed scoring from the record, not the UI.** Make `computeQsoPoints`
and the dupe block read rookie year / grids / QTH from `q` (promote those values
onto the record in `applyFieldsToRecord` before scoring). Still in the controller.
*Verify:* scores/dupes unchanged on a captured set of real logs (see §5).
*Risk: medium* (the subtle order-dependence at 1827–1850).

**Stage 2 — extract per-QSO points → `ContestScorer.points()` + `StationContext`.**
Controller delegates. Write the **first unit tests**: golden QSO → expected points
per multiplierType. *Verify:* per-row points identical pre/post. *Risk: medium.*

**Stage 3 — extract dupe → `ContestScorer.isDupe()`** (pure over candidate +
prior-for-call list; controller fetches the list via DAO). Fold the simple
band/mode/contest-wide cases into the same pure function. Unit-test all 6 modes.
*Verify:* dupe flags identical. *Risk: low–medium.*

**Stage 4 — split `updateStats`.** Extract the compute half →
`ContestScorer.score(plugin, qsos, ctx)` returning `ContestScore`. The controller's
`updateStats` shrinks to: get the QSO list, call the scorer, `Platform.runLater`
paint from `workedTokens`. Unit-test mult counts + totals for representative
contests (CQ WW, WPX, a QSO party, WAE, Field Day, a section contest).
*Verify:* score/mults/labels identical. *Risk: high* (the 673-LOC, 16-branch
core) — do it one branch family at a time behind the new API.

**Stage 5 — builder integration.** Plugin builder depends on the engine
`ContestScorer`; the Preview tab gains a small sample-QSO input and shows a real
score/mult/dupe breakdown. *Risk: low* (additive, in the builder repo).

## 5. Risk control — characterization tests first

There are **no tests today**, so the refactor's safety net must be built before
moving code: capture **golden outputs** (per-QSO points, dupe flags, final
score/mults) from the *current* behavior over a corpus of representative contest
logs — one per multiplierType family — then assert the extracted engine produces
identical numbers. These characterization tests become the permanent regression
suite the scoring has never had.

## 6. Effort & sequencing

- Stage 0: ~½ day (mechanical move + resource paths).
- Stages 1–3: the bulk of the value at moderate risk; each independently shippable.
- Stage 4: the large one; gate behind golden tests; branch-family at a time.
- Stage 5: small, in the builder.

Stages 0–3 already unblock the builder's per-QSO points + dupe preview; Stage 4
unblocks the full score/mult preview. Ship incrementally — do **not** big-bang it.

## 7. Payoff

- First unit-testable scoring in the suite (currently 0 tests over 1,150 LOC).
- `ContestLogController` shrinks ~1,150 → ~400 LOC (adapter only).
- Plugin builder gets a faithful contest score/mult/dupe preview.
- Scoring becomes reusable by any future consumer (web log, contest robot, etc.).
