# Solo J-Map / J-Sat

J-Map and J-Sat can run **on their own** — on a second monitor, a laptop, or a
shack Raspberry Pi — driven by a small JSON launch file. (J-Log, J-Learn,
J-Vault and J-Digi stay in the dock.)

## Run (dev)

```bash
mvn -q javafx:run -Dars.config=launch/j-map-remote.json
```

## Build a runnable jar

```bash
mvn -q package          # → target/ars-fx-linux.jar   desktop x86_64, JavaFX bundled (plain JDK)
mvn -q -Ppi package     # → target/ars-fx-pi.jar       Raspberry Pi, JavaFX NOT bundled
./build-pi.sh           # convenience wrapper for the Pi build + next-steps
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
