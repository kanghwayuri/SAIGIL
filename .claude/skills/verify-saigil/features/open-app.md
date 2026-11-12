# Open the app

Opening SAIGIL from the car's app launcher brings up a single screen with the app bar title `SAIGIL` and a readout line that settles within a few seconds.

## Sub-features

- `open-launcher` starts the app from its launcher entry and brings it to the foreground.
- `open-ready` replaces `Loading door position...` with a result line without user input.
- `open-again` reaches the same ready screen when the app is closed and opened again.

## How to get to it (user POV)

- Choose the SAIGIL icon in the app grid of the car's bottom bar.

## Driving it with control-saigil

Preconditions:

- The Pleos car emulator is booted and no run is open.

- **Open from the launcher.** Start the app from its launcher entry. Run `control-saigil launch`. It prints a line that starts with `ROW_1_LEFT door position: ` and the run ID.
- **Foreground and identity.** Look at the screen. Run `control-saigil doctor`. The line for `com.gomsoo.saigil/.MainActivity` says it is in the foreground. Run `control-saigil text`. The output has the lines `SAIGIL` and `ROW_1_LEFT door position: 0`.
- **Open again.** Close the app and open it again. Run `control-saigil relaunch`. It prints the result line again.
- **Proof.** Run `control-saigil evidence open-ready`. `open-ready.png` shows the purple `SAIGIL` app bar on the right side of the car display and `open-ready.text.txt` has both lines.

## Gotchas

- `launch` and `relaunch` start the app through its launcher intent. They do not tap the icon in the app grid, so the icon's presence in the grid is not proven by this recipe.
- The car display also shows the platform's cluster and 3D car on the left. `control-saigil text` filters to SAIGIL's own views. Text from the left panel never appears in it.
- `launch` reinstalls the debug APK. A run that needs the old build must not call it.
