# DXCC prefix table generator

`gen_dxcc_prefixes.py` rebuilds `j-log-engine/src/main/resources/com/jlog/scoring/dxcc-prefixes.json` (the table
`DxccResolver` uses) from the contest country files, `cty.dat`. Run it whenever `cty.dat` in
`ars-fx/src/main/resources/com/ars/fx/cty/` is replaced (a new country, a new prefix block):

```bash
tools/dxcc-prefixes/gen_dxcc_prefixes.py                 # regenerate from the bundled cty.dat
tools/dxcc-prefixes/gen_dxcc_prefixes.py --check         # exit 1 if the table on disk is out of date
tools/dxcc-prefixes/gen_dxcc_prefixes.py --refresh-adif  # also re-download the ADIF entity list first
tools/dxcc-prefixes/gen_dxcc_prefixes.py --cty FILE      # use another cty.dat
```

then `mvn install` in `j-log-engine` and `mvn test` in `ars-fx`: `CtyDataTest.engineTableAgreesWithCountryFiles` fails if a
cty.dat prefix is unresolved, an entity ends up with two numbers, or two entities share one.

Files here:
- `base-prefixes.json` - the hand-kept table, one row per prefix. Edit it to correct something cty.dat does not mention.
- `adif-entities.json` - the ADIF DXCC entity codes and names (deleted codes flagged), from adif.org. DXCC numbers in the
  table are these codes.
- `gen_dxcc_prefixes.py` - the generator. `RENUMBER`, `RENAME`, `DROP_PREFIXES` fix base-table errors; `OVERRIDES` maps the
  cty.dat entity names that do not match an ADIF name.

The app also refreshes `cty.dat` itself weekly (`CtyUpdater`, into `~/.j-hub/cty/`). That refresh feeds zones and the
country-files fallback; the engine's table only changes when this script is run and a release is built.
