# Driver door position

SAIGIL shows the position of the front-left (driver) door as a line of text in the middle of the screen. The value is `0` while the door is closed and `1` while it is open on the Pleos car emulator.

## Sub-features

- `door-closed` shows `ROW_1_LEFT door position: 0` while the driver door is closed.
- `door-open` shows `ROW_1_LEFT door position: 1` while the driver door is open.
- `door-read-once` keeps the value from launch on screen when the door changes while the app is open.
- `door-failed` shows `Failed: <message>` when the vehicle does not answer the request.

## How to get to it (user POV)

- Open SAIGIL. The readout is the only content of its only screen.

## Driving it with control-saigil

Preconditions:

- `control-saigil launch` succeeded and `control-saigil doctor` shows no `FAIL`.
- `control-saigil door state` prints `front-left door open=false`.

- **Closed readout.** Look at the screen. Run `control-saigil text`. The output has the line `ROW_1_LEFT door position: 0`. Run `control-saigil evidence door-closed`.
- **Open the door.** Open the driver door. Run `control-saigil door open`. It prints `front-left door open=true`.
- **Read once.** Keep the app open and look again. Run `control-saigil text`. The line still reads `ROW_1_LEFT door position: 0`. Run `control-saigil evidence door-read-once`. In the screenshot the 3D car on the left shows the driver door open while the app still shows `0`.
- **Open readout.** Close the app and open it again. Run `control-saigil relaunch`. It prints `ROW_1_LEFT door position: 1`. Run `control-saigil evidence door-open`.
- **Close the door.** Close the driver door and reopen the app. Run `control-saigil door close` and then `control-saigil relaunch`. It prints `ROW_1_LEFT door position: 0`.
- **Proof.** In `verify-artifacts/<run-id>/`, `door-open.png` shows the open door on the 3D car next to `ROW_1_LEFT door position: 1`, `door-open.vehicle.json` has `"front_left": true`, and `door-open.logcat.txt` has `Door position (ROW_1_LEFT): 0` followed by `Door position (ROW_1_LEFT): 1`.

## Gotchas

- The value on screen is the value at launch. Asserting the new value without `control-saigil relaunch` fails, and that is the `door-read-once` behavior, not a harness fault.
- `door-failed` has no reachable entry point on the car emulator. The vehicle answers the request even while the Virtual Console has no session. Report it as not driven. Do not run the app on a phone emulator to force the failure.
- `Loading door position...` is replaced soon after launch, before `control-saigil text` can usually catch it. Do not assert on it.
- Door writes are refused while the Virtual Console is open in a browser. Close that tab or wait.
- The app reads only the front-left door. Opening another door does not change the readout.
- `control-saigil cleanup` restores the door state recorded at `launch`. A run that skips cleanup leaves the door open for the next run.
