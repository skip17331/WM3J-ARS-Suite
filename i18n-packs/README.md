# ARS Suite — External Language Packs

English and Spanish ship **embedded** in every module's jar. German,
French, Italian, and Portuguese ship as **external `.properties`
files** in this directory so:

1. The jars stay small (no point bundling six full translations into
   every module when most operators only use one or two).
2. Native-speaker contributors can fix translations without rebuilding
   Java — just edit the file and reload the app.

## Install a pack

From the ARS Suite checkout, run the bundled helper script:

```bash
# Linux / macOS — install one language for every module
./install-lang-pack.sh de

# Windows
install-lang-pack.bat de

# Or just one module
./install-lang-pack.sh fr j-digi
```

Then set **Language: de** (or whichever) in J-Hub → Station →
Regional Settings. Modules pick it up on next launch — or live for
modules that listen for `STATION_CONFIG`.

**English and Spanish are already inside every jar** — no install
step needed for those two. The script will tell you that if you
try `./install-lang-pack.sh en`.

### What the script actually does

Copies the `.properties` file from `i18n-packs/<module>/` into
`~/.j-hub/lang/<module>/`. That's it. Re-running is safe; it
overwrites the previous file so you can pull a `git pull` and run
the script again to refresh.

If you'd rather do it by hand:

```bash
mkdir -p ~/.j-hub/lang/j-digi
cp i18n-packs/j-digi/messages_de.properties ~/.j-hub/lang/j-digi/
```

## Layout

```
i18n-packs/
├── j-digi/          messages_{de,fr,it,pt}.properties
├── j-bridge/        messages_{de,fr,it,pt}.properties
├── j-map/           messages_{de,fr,it,pt}.properties
├── j-sat/           messages_{de,fr,it,pt}.properties
└── morse-trainer/   messages_{de,fr,it,pt}.properties
```

J-Log carries all six bundles inside its own jar
(`j-log-engine/src/main/resources/com/jlog/i18n/`) because its
translation set has been hand-verified the longest.

J-Learn and J-Vault are web UIs — their translations live in
`src/main/resources/web/i18n.js` inside each module's jar.

## Contributing

1. Pick the module + language file you want to improve.
2. Edit the `messages_<lang>.properties` file directly. Keys are
   shared across all bundles for the same module — keep them in
   alphabetical order to make merges painless.
3. Open a PR. Mark which keys you reviewed/changed in the description
   (e.g. "menu.* and button.* for j-digi/de").

All non-English bundles started life machine-translated, so corrections
from operators who actually use the language are very welcome.
