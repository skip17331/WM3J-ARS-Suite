# Solo J-Map / J-Sat

J-Map and J-Sat can run **on their own** — on a second monitor, a laptop, or a
shack Raspberry Pi — driven by a small JSON launch file. (J-Log, J-Learn,
J-Vault and J-Digi stay in the dock.)

## Run

```bash
# from the ars-fx module (dev):
mvn -q javafx:run -Dars.config=launch/j-map-remote.json

# from a built jar:
java -Dars.config=/path/to/j-map-remote.json -jar ars-fx.jar
#   or:  java -jar ars-fx.jar --config /path/to/j-map-remote.json
```

Generate a starter file without hand-editing:

```bash
java -jar ars-fx.jar --write-sample map  j-map-solo.json
java -jar ars-fx.jar --write-sample sat  j-sat-solo.json
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
