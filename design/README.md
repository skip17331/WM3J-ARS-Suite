# Design reference

`ars-suite/` is the original design handoff for the ARS Suite UI: HTML/JSX prototypes for J-Hub and each module, the
design tokens (`ars-tokens.css`), and reference screenshots (`handoff-shots/`). The JavaFX app in `../ars-fx` was built
from it.

It is a **historical reference**, not a spec to follow. The app has moved on; where they differ, the app wins. It does
not cover later additions such as the Morse Trainer, the QSO phrase translator or the macro tag palette.

Still useful:

- `ars-tokens.css` and `EDITING-STANDARD.md` — colours, spacing and the card / chiclet conventions.
- `Rig Control Drawer.html` — `ars-fx/.../shell/RigPanel.java` is built 1:1 from it.
- `handoff-shots/` — screenshots of the intended look.

Stale in `HANDOFF.md` and `PROJECT-OVERVIEW.md`: they describe building the screens as a task, and `HANDOFF.md` mentions
an embedded web UI on `localhost:8081`. J-Hub is now part of the one JavaFX app; there is no separate web UI.
