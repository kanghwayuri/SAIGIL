---
name: verify-saigil
description: Drive the SAIGIL Android app (com.gomsoo.saigil) on the Pleos Connect car emulator the way a user does and capture proof. Use to prove a change to the app works on the real screen, such as saving a profile or getting a recommendation, before calling it done.
---

# Verify SAIGIL

SAIGIL is a conversation companion app for the Pleos Connect car platform. It saves driver and passenger profiles and topics, recommends how to open a conversation, speaks suggestions while driving, and answers Gleo AI. This skill builds the app, runs it on the car emulator, changes the vehicle state the way the emulator's own Virtual Console does, and saves proof.

Everything goes through one helper, run from the repo root:

```
.claude/skills/verify-saigil/control-saigil <command>
```

The feature recipes are in [`features/`](features/README.md). Read that index before driving.

## Hard rules

- SAIGIL runs only on the Pleos car emulator (an AVD named `Pleos_Connect_*`). Never install or run it on a phone emulator, even when one is connected. The helper refuses any device that lacks the Pleos Virtual Console.
- There is one car emulator and one install of the app on it, so two runs cannot go side by side. `launch` refuses while another run is open. Do not work around the refusal. Run `cleanup` if the open run is yours, otherwise wait.
- If the Virtual Console is open in a browser, someone else is driving the vehicle state. The `door` commands refuse, and that refusal is correct.

## Launch

```
.claude/skills/verify-saigil/control-saigil launch
```

It opens a run, records the driver door state so cleanup can restore it, runs the repo's own `just install <serial>` (build the debug APK, install it, start the app from its launcher entry), and waits until the screen is usable.

Ready means SAIGIL is in the foreground and shows its screen title, `사이길` on a fresh start. `launch` prints that line. It fails after 30 seconds if SAIGIL shows no text, and that failure is a finding about the app, not about the harness.

Requirements: `adb`, `just`, `curl`, and `python3` on `PATH`, and the Pleos car emulator already booted. The helper finds the emulator by itself. When more than one is connected, set `SAIGIL_SERIAL=<serial>`.

## Doctor

```
.claude/skills/verify-saigil/control-saigil doctor
```

Read-only. Run it first whenever anything looks off. It reports `OK`, `WARN`, or `FAIL` for each of these and exits non-zero on any `FAIL`:

- the Pleos car emulator and its AVD name
- whether the installed APK is byte-identical to `app/build/outputs/apk/debug/app-debug.apk`, so you are not driving a stale build
- whether `pleos.car.permission.STT_SERVICE` is granted
- whether `MainActivity` is in the foreground, and the text it shows
- whether the Virtual Console answers, the driver door state, and whether another console client is active
- whether a run is open, and where its evidence goes

## Drive

| Command | What it does |
| --- | --- |
| `control-saigil text` | Prints the text of every SAIGIL view on screen, one per line. Prints nothing when SAIGIL is not in the foreground. |
| `control-saigil relaunch` | Stops the app, starts it again from its launcher entry, waits for ready, and prints the result line. |
| `control-saigil door open` | Opens the front-left (driver) door through the Virtual Console and confirms the console reports it open. |
| `control-saigil door close` | Closes it the same way. |
| `control-saigil door state` | Prints the console's view of the driver door. Read-only. |

Stable handles on the screen, all plain text because the screen has no resource IDs:

- the app bar title `SAIGIL`
- the screen title, the first line `control-saigil text` prints

The app reads the door once, when the activity is created. After a `door` command the screen keeps its old value until `relaunch`.

The Virtual Console is the emulator's own control surface, an HTTP server on device port 8088 that the helper reaches through a temporary `adb forward`. Door writes are `PUT /vehicle/body?door_fl=true|false`. They are rejected with `emulator_inactive` unless a console session heartbeat is live, and the helper holds one only for the length of the write.

## Evidence

```
.claude/skills/verify-saigil/control-saigil evidence <label>
```

It writes five files to `verify-artifacts/<run-id>/`, each named `<label>.<kind>`:

- `png`: a screenshot of the whole car display. It shows the app and the platform's own 3D car, which draws the open door.
- `ui.xml`: the UI hierarchy
- `text.txt`: the SAIGIL text on screen
- `logcat.txt`: every `SAIGIL`, `SaigilDrive` (spoken suggestions) and `SaigilGleo` (Gleo requests and responses) log line since `launch`
- `vehicle.json`: the Virtual Console's door and door-lock state at that moment

`verify-artifacts/` ignores itself in git. `launch` also saves `install.log` there.

Proof standards:

- Drive the real path: the installed debug APK, started from its launcher entry, on the car emulator. Never call the SDK from a test or set the text directly.
- Capture the action and the result. Take evidence before the change, after the change, and after the relaunch that shows it.
- Check the side that the app does not own. `vehicle.json` and the 3D car in the screenshot must agree with the number the app shows.
- Read the screenshot. A text match alone does not show that the screen is the app's.

## Cleanup

```
.claude/skills/verify-saigil/control-saigil cleanup
```

It puts the driver door back to the state recorded at `launch`, stops the SAIGIL instance the run started, and closes the run. It leaves the app installed and never touches `verify-artifacts/`. Run it after every run, failed ones included. After it finishes, confirm the evidence files are still there.

## Helpers

`control-saigil` is the only helper. It is executable, and every command is shown above. Commands that change vehicle state (`door open`, `door close`) need an open run so that cleanup can undo them.
