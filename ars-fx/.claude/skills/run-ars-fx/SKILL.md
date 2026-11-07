---
name: run-ars-fx
description: Run, start, build, screenshot and drive the ARS Suite desktop app (ars-fx, JavaFX: J-Hub, J-Map, J-Sat, J-Log ...) on Linux. Use to see a UI change working in the real window, click through it, take a screenshot, rebuild the packaged jar, or fix "the hub / cluster won't connect".
---

# Run ars-fx

`ars-fx` is one JavaFX 21 / Java 21 app: a docked window (J-Hub, J-Map, J-Sat, J-Log, ...) that is a *client* of a background **hub** process (WebSocket, port 8090) which owns the rig, rotor and DX-cluster connections. Run it from source, drive it with the two helpers in this folder, and **look at the screenshots**.

All paths below are relative to `ars-fx/` (this unit). The helpers are in `.claude/skills/run-ars-fx/`.

Verified on Linux Mint (Cinnamon) with a **real X display** (`DISPLAY=:0`), JDK 21, Maven 3.8. It was not tried under `xvfb` (not installed here).

## Prerequisites

Already present on this machine: `java` 21, `mvn`, `python3` with `python-xlib` and `Pillow`, `wmctrl`, `gnome-screenshot`. There is no `xdotool`; `driver.py` sends input with python-xlib instead. The four embedded libraries must be installed in `~/.m2` at the app's version (see Build).

## Run (agent path)

```bash
S=.claude/skills/run-ars-fx
$S/app.sh start sat              # hub (default) | map | sat | log | digi | bridge | vault | learn; takes ~40 s
$S/app.sh shot /tmp/ars1.png     # focuses the window, saves a 3840x2160 screenshot
python3 $S/driver.py click 3607 237          # J-Sat "Globe" button (this window size)
python3 $S/driver.py drag 2200 1000 1900 1100
python3 $S/driver.py wheel 1930 1070 3       # 3 notches up = zoom in
python3 $S/driver.py dbl 3400 1700           # double-click
wmctrl -a "ARS Suite"; python3 $S/driver.py click 530 620   # J-Hub: callsign field
python3 $S/driver.py type "W1AW"; python3 $S/driver.py key BackSpace 4
$S/app.sh shot /tmp/ars2.png
$S/app.sh stop                   # stops this dev run only, never the background hub
```

`driver.py` coordinates are **screen pixels, as in the screenshot file**. If you read a position off the picture the Read tool shows (2576 wide), multiply by 1.49 (3840 / 2576). Crop big screenshots before looking:

```bash
python3 -c "from PIL import Image; Image.open('/tmp/ars1.png').crop((200,150,3700,1950)).resize((1400,720)).save('/tmp/ars1c.png')"
```

`driver.py` commands: `click X Y`, `dbl X Y`, `drag X1 Y1 X2 Y2`, `wheel X Y N`, `type TEXT` (letters, digits, space, `-`, `/`), `key NAME [N]`. Run `python3 .claude/skills/run-ars-fx/driver.py` for the list.

## Build

The desktop shortcuts and `ars-fx.sh` run the **packaged jar** `target/ars-fx-linux.jar`, which goes stale after a pull or merge. The nightly job bumps the version in every `pom.xml`, and `ars-fx` will not build until the four libraries exist in `~/.m2` at that version:

```bash
cd .. && for m in j-log-common j-log-engine j-digi j-bridge; do (cd $m && mvn -o -q -DskipTests install); done
cd ars-fx && mvn -o -q -DskipTests package       # writes target/ars-fx-linux.jar
```

Tests: `mvn -o test` (about 470, all pass). One class: `mvn -o -q test -Dtest=GlobeProjectionTest`.

## Run (human / installed path)

`bash ars-fx.sh` opens the docked app from the packaged jar; `bash ars-fx.sh --module map` opens one module; `bash ars-fx.sh --hub` starts the background hub alone. Useless for agents except to start the hub.

## Gotchas

- **A stale hub blocks everything.** A hub started by an earlier `./run.sh` (it shows `-Dars.hub.auto=true` and `-cp .../target/classes`) can outlive its windows, keep `~/.j-hub/hub.lock`, and stop listening on 8090. New windows then cannot attach and a new hub refuses to start ("another hub process is starting or running"). Check: `ss -ltn | grep :8090` and `pgrep -af HubServer`. Fix: `kill <pid>` of the stale hub, then `nohup bash ./ars-fx.sh --hub > /tmp/hub.log 2>&1 &` and wait for the `listening on 0.0.0.0:8090` line. Do not stop a hub you did not start without asking the user.
- **Never rebuild the jar or recompile under a running hub.** The hub runs from `target/ars-fx-linux.jar` (or `target/classes`). Replacing it under the JVM is the likely way hubs die. Stop the hub, rebuild, start it again.
- **The cluster is opt-in.** The DX cluster only connects after you press **Connect** on the J-Hub DX Cluster panel (or turn on Setup > Data > DX Cluster > Auto-connect on start). A dev window attaches to the running hub and mirrors its spots. Without a connected hub J-Map shows `0 spots`.
- **`app.sh stop` only matches `-Dsurface=` and `javafx:run`.** Never `pkill -f javafx:run` by hand: it matches your own shell's command line.
- **Focus.** Run `wmctrl -a "ARS Suite"` in the same command as a `driver.py` call (`app.sh shot` already does); the terminal steals focus between calls. On a shared desktop the user may click in the same window while you test.
- **Do not click the sun icon at the bottom left of the dock.** It is the theme button and rewrites `theme.scheme` in `~/.j-hub/config.json`. Check with `grep -o '"theme.scheme":"[a-z]*"' ~/.j-hub/config.json` after a GUI session. Clicking a spot also writes `prop.target` there; remove it if you did not mean to leave it.
- **J-Sat views depend on live passes.** The Polar plot is empty while every satellite is below the horizon (the note under it says so). The "station is inside the footprint" styling and the line to your station only show while a satellite is above your horizon; the top bar shows the next AOS.
- **Positions move.** Satellite and spot markers move every second, so take a screenshot, read the position, then click within a few seconds.
- The log may show repeated `NullPointerException ... RTTexture.createGraphics()` from a JavaFX canvas; the views still rendered normally in every run.

## Troubleshooting

- `Could not resolve dependencies ... j-log-engine:jar:1.12.5` -> install the four libraries first (Build).
- `app did not come up` from `app.sh start` -> read `/tmp/ars-fx-run.log`; first run after a pull compiles for a while, retry.
- Screenshot shows the terminal, not the app -> the app is not running or lost focus; `pgrep -af "[D]surface="` and `wmctrl -l`.
- Menu launchers show old behaviour -> the packaged jar is stale: stop the hub, rebuild (Build), start the hub, reopen the window.
