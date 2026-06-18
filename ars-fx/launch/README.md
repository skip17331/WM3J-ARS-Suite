# Loose (un-docked) mode + solo J-Map / J-Sat

The ARS Suite runs two ways from the **same jar**:

- **Docked** — one window, all modules in a left dock (the default; see `run.sh`).
- **Loose** — each module in its own window, all sharing a **background hub**.

## Loose mode (any module, un-docked)

```bash
./run-loose.sh log      # open J-Log in its own window
./run-loose.sh map      # log | logc | map | sat | digi | vault | learn | bridge
./run-loose.sh hub      # start ONLY the background hub (tray), open nothing
```

A small headless **hub** (`com.ars.fx.HubServer`) owns the Hamlib daemons and the
live data feeds (DX cluster, RBN, rig, rotor) and publishes them on
`ws://127.0.0.1:8090`; each loose window attaches to it. The hub shows a
**system-tray icon** (Open ▸ each module, Quit) but never opens a window of its
own — opening a module just opens that app. The first module you launch brings
the hub up automatically (`HubServer.ensureRunning()`); closing the last window
leaves the hub in the tray until you quit it there. On desktops with no usable
tray (e.g. GNOME/Wayland) the hub runs headless — stop it with Ctrl-C / `kill`.

Loose windows reach **J-Hub / Station settings** via the ⚙ gear in their top bar
(there is no dock); the settings pages offer a "← Back to <module>" link.

## Solo J-Map / J-Sat (JSON-driven, incl. remote over the LAN)

J-Map and J-Sat can also run from a small JSON launch file — handy on a second
monitor, a laptop, or a shack Raspberry Pi, and the only way to point a window at
a **remote** station's hub over the LAN (`remote: ws://192.168.1.50:8090`).

## Run (dev)

```bash
mvn -q javafx:run -Dars.config=launch/j-map-remote.json
```

## Build a runnable jar

```bash
mvn -q package               # → target/ars-fx-linux.jar        desktop Linux x86_64 (JavaFX bundled)
mvn -q -Pwin package         # → target/ars-fx-windows.jar      Windows x86_64
mvn -q -Pmac package         # → target/ars-fx-mac.jar          macOS Intel
mvn -q -Pmac-aarch64 package # → target/ars-fx-mac-aarch64.jar  macOS Apple Silicon
mvn -q -Ppi package          # → target/ars-fx-pi.jar           Raspberry Pi, JavaFX NOT bundled
./build-pi.sh                # convenience wrapper for the Pi build + next-steps
# all desktop+pi jars at once, classified into dist/:  ../build-release.sh --ars-fx
```

```bash
java -jar target/ars-fx-linux.jar --config launch/j-map-remote.json
java -jar ars-fx.jar --write-sample map j-map-solo.json   # generate a starter file
```

## On the Raspberry Pi

OpenJFX publishes no ARM-Linux jar to Maven, so `ars-fx-pi.jar` does **not**
bundle JavaFX — install a **Liberica Full JDK 21** (BellSoft's JDK with JavaFX
built in; the standard way to run JavaFX on a Pi):

```bash
# BellSoft tarball: bell-sw.com/pages/downloads → JDK 21 / Full / ARM 64 / Linux
#   or via apt:     sudo apt install bellsoft-java21-full

# then copy ars-fx-pi.jar + a launch JSON to the Pi and run:
java -jar ars-fx-pi.jar --config j-map-remote.json
#   or use the wrapper:  ./run-solo.sh j-map-remote.json
```

## Launch file

| key       | meaning |
|-----------|---------|
| `module`  | `"map"` or `"sat"` |
| `title`   | window title (optional) |
| `window`  | `{ width, height, maximized }` |
| `dock`    | keep the module dock? default `false` (clean single-module window) |
| `remote`  | station J-Hub WebSocket, e.g. `ws://192.168.1.50:8090`. Omit for a fully local run. |
| `station` | `{ call, grid, lat, lon }` — QTH/call the module uses |

## Remote (talk to the station over the LAN)

When `remote` is set, the solo window connects to the station computer running
the **J-Hub dock app**, which shares its live state over a WebSocket:

- **station → solo:** DX-cluster spots, "heard by" RBN spots, rig freq/mode,
  rotor az/el, station QTH/call.
- **solo → station:** tune-rig and rotate-antenna commands. Clicking a spot in a
  remote J-Map tunes the rig and (on confirm) turns the rotor *at the shack*;
  J-Sat's Doppler tracking and auto-rotor drive the real hardware.

The station serves automatically on port **8090**. To change or disable it, set
in `~/.j-hub/config.json` on the station:

```
remote.port           8090       # listen port
remote.serverEnabled  true        # set false to turn sharing off
```

Without `remote`, the solo window behaves exactly like the in-dock build and
talks to local Hamlib daemons on that machine.
