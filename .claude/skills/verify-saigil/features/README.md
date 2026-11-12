# SAIGIL verification map

This directory is the maintained source for verifying the user-facing behavior of SAIGIL. Read the index before driving the app, then use the matching feature file as the recipe.

## Baseline preconditions

- The Pleos car emulator (an AVD named `Pleos_Connect_*`) is booted. SAIGIL is never run on a phone emulator.
- Run every command from the repo root. `control-saigil` below means `.claude/skills/verify-saigil/control-saigil`.
- Start with `control-saigil launch`. It builds and installs the debug APK, starts the app, and opens a run.
- Run `control-saigil doctor` and require no `FAIL` line, an installed APK that matches the local build, and an open run.
- The front-left door starts closed unless a recipe says otherwise. Check with `control-saigil door state`.
- Never drive while the Virtual Console is open in a browser. `doctor` warns about it.

## Driving conventions

- Start every recipe from the baseline state unless its preconditions say otherwise.
- Assert on the exact screen text printed by `control-saigil text`. The screen has no resource IDs.
- Treat every command as literal.
- The app reads the door once per launch. Run `control-saigil relaunch` after any door change before asserting the new value.
- End every run with `control-saigil cleanup`, which restores the door. It keeps the proof artifacts.

## Proof and skip reporting

- Capture the user action and the resulting state, not only the final screen.
- Proof is one `control-saigil evidence <label>` per step. It saves a screenshot, the UI hierarchy, the screen text, the `SAIGIL` log lines, and the Virtual Console's door state.
- The number on screen must agree with `vehicle.json` and with the 3D car in the screenshot.
- Record the feature ID and entry point used with every artifact by using the sub-feature ID as the evidence label.
- Report an unreachable path with the attempted command and the unmet precondition.
- Do not report a skipped entry point as verified through a different path.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the user-visible behavior. It then uses exactly four H2 sections in this order.

1. `Sub-features` lists short IDs with one line for each behavior.
2. `How to get to it (user POV)` lists every user entry point.
3. `Driving it with control-saigil` starts with `Preconditions:` and uses labeled bullets that pair each user action with an exact command and observable result.
4. `Gotchas` lists traps that can waste or invalidate a verification run.

Keep implementation details out of the map. Name only user paths, stable handles, required state, commands, and observable proof.

## Features

- [Open the app](./open-app.md) covers starting SAIGIL from its launcher entry and reaching a usable screen.
- [Driver door position](./door-position.md) covers the closed and open readouts, the once-per-launch read, and the failure message.
