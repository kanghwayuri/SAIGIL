---
name: formal-verify-vehicle-lifecycle
description: Model-check how SAIGIL's MainActivity uses the Pleos Vehicle SDK lifecycle (initialize, the door position request, release) against every order of binding, answering, service death and the user leaving. Use when a change touches MainActivity's SDK calls, their order, or the lifecycle method they are made from, when the Vehicle SDK version changes, or when another SDK request is added.
---

# Formally verify the Vehicle SDK lifecycle

MainActivity asks the Pleos Vehicle SDK for the driver door position while the SDK is still binding to the vehicle service on another thread. Whether the answer reaches the screen depends on the order in which binding, the answer, a service crash, and the user leaving happen. Tests reach one or two of those orders. This check reaches all of them inside the bounds below.

The tool is the TLA+ model checker TLC, because the question is about the order of events and not about input values. The model is small, about 30 to 220 states for each configuration, and the whole check takes about 5 seconds.

## What is verified

| Property | In plain words | Harm when it breaks |
| --- | --- | --- |
| `NoStrandedRequest`, `ScreenLeavesLoading` | While the activity is alive, the screen always leaves `Loading door position...` for a position or a `Failed:` message. | The driver looks at `Loading door position...` with no error until they restart the app. |
| `NoSelfInflictedFailure` | The app never shows `Failed:` because it asked before `initialize()` or after `release()`. | The app reports a failure on a healthy car. |
| `ReleaseUnbindsOnceBound` | After `onDestroy` the app holds no binding to the vehicle service. | A destroyed activity stays bound to the vehicle service and is leaked. |

`NoStrandedRequest` is the state form of the first promise and gives the shortest path to a break. `ScreenLeavesLoading` is the same promise as a liveness property and catches the forms the state form does not list, such as never asking at all.

## Run

```
.claude/skills/formal-verify-vehicle-lifecycle/check
```

It needs `java` on `PATH` and TLC 1.7.4 at `~/.cache/tlaplus/tla2tools-1.7.4.jar`. When the jar is missing, `check` prints the download command and stops. It refuses a jar whose SHA-256 is not the pinned one. Set `TLA2TOOLS_JAR` to use another copy.

A clean run prints six lines that all start with `ok` and exits 0:

```
ok       healthy        holds
ok       late           holds
ok       dies           holds
ok       late-stranded  known finding F1 still breaks NoStrandedRequest
ok       late-unbind    known finding F2 still breaks ReleaseUnbinds
ok       dies-stranded  known finding F3 still breaks NoStrandedRequest
```

| Line starts with | Meaning | Exit code |
| --- | --- | --- |
| `ok` | The configuration holds, or a known finding still reproduces. | 0 |
| `BROKEN` | A property that must hold is violated, or TLC could not run the model. The line names the property and the file with the trace. | 1 |
| `CHANGED` | A known finding no longer reproduces. | 2 |
| `STALE` | `MainActivity.kt` or the SDK version differs from what `sync.txt` records. | 3 |

The three environments are `healthy` (the vehicle service is running and stays up), `late` (it may not be running when the app starts, and may never start), and `dies` (it may die once and is restarted). TLC's full output for each configuration is in `build/formal-verify-vehicle-lifecycle/<config>.out`.

Read every result with the pstack `/statechart` skill. It shows each broken rule as its shortest path in plain words, and it draws how the request moves between states when the rules hold.

## Does my change touch the verified code?

It does when it changes any of these. Run `check` before and after.

| Code | Model |
| --- | --- |
| `MainActivity.onCreate`, the order of `vehicle.initialize()` and `requestDriverDoorPosition()` | `OnCreateCalls` in `model/MainActivityCalls.tla` |
| The `onComplete` and `onFailed` lambdas in `MainActivity.requestDriverDoorPosition`, when they start making SDK calls | `OnResultCalls` |
| `MainActivity.onDestroy`, `vehicle.release()` | `OnDestroyCalls` |
| `pleosVehicle` in `gradle/libs.versions.toml` | The SDK actions in `model/VehicleLifecycle.tla` |

The SDK actions describe `ai.pleos.playground:Vehicle` 2.3.2, read from `Vehicle-2.3.2-sources.jar` in the Gradle cache:

| SDK code | Model action |
| --- | --- |
| `Vehicle.initialize` -> `BaseServiceConnector.connectService` | `Initialize`, `BindSucceeds`, `BindFails`, `BindGivesUp` |
| `Door.getCurrentDoorPosition` -> `VehicleServiceManager.getProperty` -> `BaseServiceConnector.request`, `sendApi` | `RequestDoor`, `DoorAnswers` |
| `Vehicle.release` -> `BaseServiceConnector.disconnectService` | `Release` |
| `BaseServiceConnector.onServiceConnected`, `onRequestCapability`, `consumeAllCached` | `OnServiceConnected`, `CapabilityAnswers` |
| `BaseServiceConnector.onServiceDisconnected` | `OnServiceDisconnected` |
| `runOnUiThread { resultView.text = ... }` | `UiRuns` |

A change that only edits text, logging, or layout does not touch it. `STALE` still appears because the file hash changed. Confirm that the SDK calls are the same and resync.

## Update the model for an intended change

1. Edit `model/MainActivityCalls.tla` so each list names the SDK calls its method makes, in source order. The call names are `initialize`, `requestDoor`, and `release`.
2. Run `check`.
3. When the SDK version changes, open the new sources jar, compare the methods in the table above with the model actions, and change `model/VehicleLifecycle.tla` where they differ.
4. When the app starts making a kind of call the model does not have, such as a listener registration or a second request in flight, add an action for it in `model/VehicleLifecycle.tla` and name the SDK method it mirrors in a comment.
5. Run `check --self-test`. It runs three broken variants of `MainActivityCalls.tla` from `model/mutants/` and requires that each is caught by the property that guards it. A model change that lets a broken variant pass has weakened the check.
6. Run `check --resync` only after you have confirmed that the model describes the code at the new commit and `check` prints no `BROKEN` line. It rewrites `sync.txt`. Never resync to make `STALE` go away without that confirmation.

## When `check` prints BROKEN

Open the named `.out` file and read the steps between `State 1` and the last state. Each step is a model action from the tables above. Retell the path in the code's words, then decide:

- It is a real bug when every step is something the code, the SDK, Android, or the user can do in that order. Report it with the path. Do not change the property or the model to make it pass.
- It is a modeling error when a step cannot happen in the code. Two main-thread steps interleaved inside one lifecycle method, or an SDK step that the SDK source does not have, are the usual cases. Fix the model action, say what was wrong, and run `check --self-test`.

A `BROKEN` line with `ERROR` and no property name means TLC could not parse or run the model. The `.out` file has the parser message.

## Known findings

These are properties that the current code breaks. `check` expects each to keep breaking and prints `CHANGED` when one stops. They come from the model of the SDK source. None has been reproduced on a device, because the car emulator gives no way to stop the vehicle service.

**F1. The request is stranded when the vehicle service is not up in time.** Breaks `NoStrandedRequest` in `late`.

1. `onCreate` calls `vehicle.initialize()`, then asks for the door position. The SDK is still connecting, so it puts the request in `cachedCall`.
2. `bindService` returns false on every try, because the vehicle service is not running yet.
3. After the last retry, `connectService` sets the state to `DISCONNECT` and logs. It does not touch `cachedCall`, and it calls neither `onComplete` nor `onFailed`.
4. The screen shows `Loading door position...` for as long as the activity lives. The SDK retries 30 times, 500 ms apart, so this happens when the service is not up within about 15 seconds.

**F2. A binding made after `release()` outlives the activity.** Breaks `ReleaseUnbinds` in `late`.

1. `onCreate` calls `vehicle.initialize()`, which starts the bind coroutine.
2. The user leaves before `bindService` has succeeded. `onDestroy` calls `vehicle.release()`. `disconnectService` unbinds and clears the cache, but it does not cancel the coroutine.
3. The coroutine calls `bindService` again and it succeeds. The destroyed activity now holds a binding that nothing will release.

The window is one thread hop when the service is up, and up to about 15 seconds when it is late. `ReleaseUnbindsOnceBound` is `ReleaseUnbinds` with this case set aside, so the app's own duty to call `release()` stays checked.

**F3. The request is lost when the vehicle service dies before it answers.** Breaks `NoStrandedRequest` in `dies`.

1. The SDK binds, gets the capability config, and sends the cached request to the service.
2. The service process dies before it answers.
3. Android reconnects and the SDK registers its listeners again, but it does not send the request again and calls neither callback.
4. The screen shows `Loading door position...` for as long as the activity lives.

## Scope

Verified, within these bounds:

- One activity instance, created once and destroyed at most once.
- One door request in flight at a time.
- `MaxRetry = 2` failed binds before the SDK gives up. The SDK uses 30. The count only decides how long the retry window is, not which steps can happen.
- The vehicle service dies at most once.
- Each lifecycle method's SDK calls run in order on the main thread, and no other main-thread callback runs between them. The bind coroutine and the service's answers can happen between any two of those calls.
- Steps of the app, the SDK, and Android happen once they can. The user leaving, the service dying, and a late service starting are never forced.

Assumed, not verified:

- `pleos.car.permission.CAR_DOORS` is granted. Without it the SDK calls `onFailed` at once.
- The model matches the SDK. It was written from the 2.3.2 sources and is not derived from them by a tool. One prediction was checked on the car emulator at commit `e943192`: with `vehicle.release()` added after the request in `onCreate`, the model breaks `NoStrandedRequest` in `healthy`, and the real app stayed on `Loading door position...` for the full 30 seconds that `verify-saigil` waits.
- Android delivers `onServiceConnected` after a successful bind while the service is up, and `onServiceDisconnected` after the service dies.

Not covered:

- Whether the position value is right. That is what the `verify-saigil` skill proves on the car emulator.
- Data races inside the SDK, such as `cachedCall` being a plain `LinkedList` that binder threads and the main thread both touch.
- Configuration changes that recreate the activity, and more than one `Vehicle` instance.
- Listener registration, `setDoorPosition`, and every other SDK API.

## Sync record

`sync.txt` holds the SHA-256 of `MainActivity.kt`, the `pleosVehicle` version, and the commit at which the model was last confirmed to describe them. `check` compares the first two on every run.
