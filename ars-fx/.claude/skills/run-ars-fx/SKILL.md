---
name: run-ars-fx
description: Run, start, build, screenshot and drive the ARS Suite desktop app (ars-fx, JavaFX: J-Hub, J-Map, J-Sat, J-Log ...) on Linux. Use to see a UI change working in the real window, click through it, take a screenshot, rebuild the packaged jar, or fix "the hub / cluster won't connect".
---

# Run ars-fx

`ars-fx` is one JavaFX 21 / Java 21 app: a docked window (J-Hub, J-Map, J-Sat, J-Log, ...) that is a *client* of a background **hub** process (WebSocket, port 8090) which owns the rig, rotor and DX-cluster connections. Run it from source, drive it with `app.sh` in this folder, and **look at the screenshots**.

All paths below are relative to `ars-fx/` (this unit). The helper is `.claude/skills/run-ars-fx/app.sh`.

Works on any Linux desktop, X11 or Wayland (verified on Fedora 45 KDE / Wayland, JDK 25, Maven; earlier on Linux Mint Cinnamon / X11). The app takes its own screenshots and receives its own input through `DevHook` (`com.ars.fx.shell.DevHook`), which `app.sh start` switches on with `-Dars.devhook=<folder>`; the packaged app never has it on. No `wmctrl`, `xdotool`, `python-xlib` or screen grabber is needed, and a screenshot can only ever show the app's window, never another one on the desktop.

## Prerequisites

`java` 21+, `mvn`, and `python3` with `Pillow` (only for cropping pictures). The four embedded libraries must be installed in `~/.m2` at the app's version (see Build).

## Run (agent path)

```bash
S=.claude/skills/run-ars-fx
$S/app.sh start sat              # hub (default) | map | sat | log | digi | bridge | vault | learn; takes ~40 s
$S/app.sh shot /tmp/ars1.png     # the app window only, in scene pixels (2560x1336 on a 2560x1440 screen)
$S/app.sh click 2416 155         # J-Sat "Globe" button; prints the control it hit, e.g. ok LabeledText.text
$S/app.sh drag 1300 700 1100 760
$S/app.sh wheel 1300 700 3       # 3 notches up = zoom in
$S/app.sh dbl 1300 700           # double-click
$S/app.sh nav hub                # switch surface (hub, map, sat, log, ..., setup:<id>)
$S/app.sh click 530 620; $S/app.sh type "W1AW"; $S/app.sh key BACK_SPACE 4
$S/app.sh shot /tmp/ars2.png
$S/app.sh stop                   # stops this dev run only, never the background hub
```

Coordinates are **pixels of a `shot` picture**. If you read a position off a shrunk copy, scale it back up. Crop or shrink before looking:

```bash
python3 -c "from PIL import Image; im=Image.open('/tmp/ars1.png'); im.thumbnail((1600,1600)); im.save('/tmp/ars1s.png')"   # then x,y * 1.6
```

Input goes to the JavaFX node under the point as JavaFX events (and keys to the focused control), not through the system pointer: Wayland lets no app move the real pointer, and this way a test never takes over the operator's mouse or keyboard. Keys are JavaFX `KeyCode` names (`ENTER`, `BACK_SPACE`, `TAB`, `ESCAPE`, `F1`...). Every command prints `ok ...` or `error ...` and exits non-zero on error. The window does not need to be in front.

## Build

The desktop shortcuts and `ars-fx.sh` run the **packaged jar** `target/ars-fx-linux.jar`, which goes stale after a pull or merge. The nightly job bumps the version in every `pom.xml`, and `ars-fx` will not build until the four libraries exist in `~/.m2` at that version:

```bash
cd .. && for m in j-log-common j-log-engine j-digi j-bridge; do (cd $m && mvn -o -q -DskipTests install); done
cd ars-fx && mvn -o -q -DskipTests package       # writes target/ars-fx-linux.jar
```

Tests: `mvn -o test` (about 620; `DaemonManagerTest.rtsWithoutASerialPortIsLeftAtTheDefault` fails while a USB rig is plugged in). One class: `mvn -o -q test -Dtest=GlobeProjectionTest`.

## Run (human / installed path)

`bash ars-fx.sh` opens the docked app from the packaged jar; `bash ars-fx.sh --module map` opens one module; `bash ars-fx.sh --hub` starts the background hub alone. Useless for agents except to start the hub.

## Gotchas

- **A stale hub blocks everything.** A hub started by an earlier `./run.sh` (it shows `-Dars.hub.auto=true` and `-cp .../target/classes`) can outlive its windows, keep `~/.j-hub/hub.lock`, and stop listening on 8090. New windows then cannot attach and a new hub refuses to start ("another hub process is starting or running"). Check: `ss -ltn | grep :8090` and `pgrep -af HubServer`. Fix: `kill <pid>` of the stale hub, then `nohup bash ./ars-fx.sh --hub > /tmp/hub.log 2>&1 &` and wait for the `listening on 0.0.0.0:8090` line. Do not stop a hub you did not start without asking the user.
- **Never rebuild the jar or recompile under a running hub.** The hub runs from `target/ars-fx-linux.jar` (or `target/classes`). Replacing it under the JVM is the likely way hubs die. Stop the hub, rebuild, start it again.
- **The cluster is opt-in.** The DX cluster only connects after you press **Connect** on the J-Hub DX Cluster panel (or turn on Setup > Data > DX Cluster > Auto-connect on start). A dev window attaches to the running hub and mirrors its spots. Without a connected hub J-Map shows `0 spots`.
- **`app.sh stop` only matches `-Dsurface=` and `javafx:run`.** Never `pkill -f javafx:run` by hand: it matches your own shell's command line.
- **Never screenshot the desktop** (spectacle, gnome-screenshot, grim): on Wayland the app may be behind the user's other windows, and the picture shows their private screen. Use `app.sh shot`.
- **Clicks change settings.** Toggles such as J-Sat's Polar / Blue marble / Globe are saved in `~/.j-hub/config.json` (`sat.view`); put back what you change.
- **Do not click the sun icon at the bottom left of the dock.** It is the theme button and rewrites `theme.scheme` in `~/.j-hub/config.json`. Check with `grep -o '"theme.scheme":"[a-z]*"' ~/.j-hub/config.json` after a GUI session. Clicking a spot also writes `prop.target` there; remove it if you did not mean to leave it.
- **J-Sat views depend on live passes.** The Polar plot is empty while every satellite is below the horizon (the note under it says so). The "station is inside the footprint" styling and the line to your station only show while a satellite is above your horizon; the top bar shows the next AOS.
- **Positions move.** Satellite and spot markers move every second, so take a screenshot, read the position, then click within a few seconds.
- The log may show repeated `NullPointerException ... RTTexture.createGraphics()` from a JavaFX canvas; the views still rendered normally in every run.

## Troubleshooting

- `Could not resolve dependencies ... j-log-engine:jar:<version>` -> install the four libraries first (Build).
- `app did not come up` from `app.sh start` -> read `/tmp/ars-fx-run.log`; first run after a pull compiles for a while, retry.
- `error: no answer in 20 s` -> the dev run is not up or was started without the hook (`./run.sh` does not set it); `pgrep -af "[D]surface="`, then `app.sh start`.
- Menu launchers show old behaviour -> the packaged jar is stale: stop the hub, rebuild (Build), start the hub, reopen the window.
