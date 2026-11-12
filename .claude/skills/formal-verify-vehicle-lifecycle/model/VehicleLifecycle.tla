-------------------------- MODULE VehicleLifecycle --------------------------
(* How MainActivity's door request moves through the Pleos Vehicle SDK.

   The app side is MainActivity.kt, mirrored in MainActivityCalls.tla.
   The SDK side is ai.pleos.playground:Vehicle 2.3.2, classes
   BaseServiceConnector and VehicleServiceManager, read from its sources jar.

   Threads. Everything named "main thread" below runs to completion before
   the next main-thread step starts, so those steps are enabled only while
   `main` is empty. The bind coroutine runs on Dispatchers.Default and the
   service's answers arrive on binder threads, so those steps can happen
   between any two SDK calls of a main-thread method. *)
EXTENDS Naturals, Sequences, MainActivityCalls

CONSTANTS
    MaxRetry,          \* failed bindService calls that are retried (SDK: MAX_RETRY = 30)
    ServiceMayBeLate,  \* TRUE: the vehicle service may not be running when the app starts
    MaxDeaths          \* times the vehicle service process may die and be restarted

VARIABLES
    activity,      \* MainActivity: "notCreated", "created", "destroyed"
    main,          \* SDK calls the main thread still has to make in its current method
    screen,        \* resultView.text: "Loading", "Position", "Failed"
    misuse,        \* TRUE once the app was shown a failure that its own call order caused
    started,       \* initialize() was called and release() has not been called since
    connectState,  \* BaseServiceConnector.connectionInfo.connectState
    binder,        \* BaseServiceConnector.serviceBinder is not null
    door,          \* the door request: "none", "cached", "sent", "answered", "dropped", "lost"
    bindJob,       \* the coroutine in connectService: "idle", "trying", "done", "gaveUp"
    failedBinds,   \* bindService calls that returned false so far
    bound,         \* Android holds a binding for the SDK's ServiceConnection
    linked,        \* onServiceConnected was delivered and onServiceDisconnected has not followed
    svc,           \* the vehicle service process: "up", "down"
    binderAlive,   \* the binder the SDK holds reaches a live service process
    deaths,        \* times the service process has died
    cap,           \* the getCapabilityConfig call: "none", "inFlight"
    uiPost,        \* runOnUiThread block waiting for the main thread: "none", "Position", "Failed"
    lateBind       \* a bindService call succeeded after release()

vars == <<activity, main, screen, misuse, started, connectState, binder, door,
          bindJob, failedBinds, bound, linked, svc, binderAlive, deaths, cap,
          uiPost, lateBind>>

TypeOK ==
    /\ activity \in {"notCreated", "created", "destroyed"}
    /\ main \in Seq({"initialize", "requestDoor", "release"})
    /\ screen \in {"Loading", "Position", "Failed"}
    /\ misuse \in BOOLEAN
    /\ started \in BOOLEAN
    /\ connectState \in {"DISCONNECT", "CONNECTING", "CONNECT"}
    /\ binder \in BOOLEAN
    /\ door \in {"none", "cached", "sent", "answered", "dropped", "lost"}
    /\ bindJob \in {"idle", "trying", "done", "gaveUp"}
    /\ failedBinds \in 0..MaxRetry
    /\ bound \in BOOLEAN
    /\ linked \in BOOLEAN
    /\ svc \in {"up", "down"}
    /\ binderAlive \in BOOLEAN
    /\ deaths \in 0..MaxDeaths
    /\ cap \in {"none", "inFlight"}
    /\ uiPost \in {"none", "Position", "Failed"}
    /\ lateBind \in BOOLEAN

Init ==
    /\ activity = "notCreated"
    /\ main = <<>>
    /\ screen = "Loading"
    /\ misuse = FALSE
    /\ started = FALSE
    /\ connectState = "DISCONNECT"
    /\ binder = FALSE
    /\ door = "none"
    /\ bindJob = "idle"
    /\ failedBinds = 0
    /\ bound = FALSE
    /\ linked = FALSE
    /\ svc \in (IF ServiceMayBeLate THEN {"up", "down"} ELSE {"up"})
    /\ binderAlive = FALSE
    /\ deaths = 0
    /\ cap = "none"
    /\ uiPost = "none"
    /\ lateBind = FALSE

-----------------------------------------------------------------------------
(* MainActivity, main thread. *)

\* MainActivity.onCreate starts: the screen shows "Loading door position...".
Create ==
    /\ activity = "notCreated"
    /\ main = <<>>
    /\ activity' = "created"
    /\ screen' = "Loading"
    /\ main' = OnCreateCalls
    /\ UNCHANGED <<misuse, started, connectState, binder, door, bindJob,
                   failedBinds, bound, linked, svc, binderAlive, deaths, cap,
                   uiPost, lateBind>>

\* MainActivity.onDestroy starts. The user can leave at any time.
Destroy ==
    /\ activity = "created"
    /\ main = <<>>
    /\ activity' = "destroyed"
    /\ main' = OnDestroyCalls
    /\ UNCHANGED <<screen, misuse, started, connectState, binder, door, bindJob,
                   failedBinds, bound, linked, svc, binderAlive, deaths, cap,
                   uiPost, lateBind>>

\* Vehicle.initialize -> BaseServiceConnector.connectService: the state becomes
\* CONNECTING at once and the bind coroutine is launched.
Initialize ==
    /\ main # <<>>
    /\ Head(main) = "initialize"
    /\ main' = Tail(main)
    /\ started' = TRUE
    /\ connectState' = "CONNECTING"
    /\ bindJob' = "trying"
    /\ failedBinds' = 0
    /\ UNCHANGED <<activity, screen, misuse, binder, door, bound, linked, svc,
                   binderAlive, deaths, cap, uiPost, lateBind>>

\* Door.getCurrentDoorPosition -> VehicleServiceManager.getProperty ->
\* BaseServiceConnector.request and sendApi. A failure reported here runs the
\* app's onFailed on the main thread, so its runOnUiThread block runs inline.
RequestDoor ==
    /\ main # <<>>
    /\ Head(main) = "requestDoor"
    /\ \/ \* request(): the SDK is not connected, so onFailed is called at once.
          /\ connectState = "DISCONNECT"
          /\ screen' = "Failed"
          /\ misuse' = (misuse \/ ~started)
          /\ main' = OnResultCalls \o Tail(main)
          /\ UNCHANGED <<activity, started, connectState, binder, door, bindJob,
                         failedBinds, bound, linked, svc, binderAlive, deaths,
                         cap, uiPost, lateBind>>
       \/ \* sendApi(): no binder yet, so the call goes into cachedCall.
          /\ connectState # "DISCONNECT"
          /\ ~binder
          /\ door' = "cached"
          /\ main' = Tail(main)
          /\ UNCHANGED <<activity, screen, misuse, started, connectState, binder,
                         bindJob, failedBinds, bound, linked, svc, binderAlive,
                         deaths, cap, uiPost, lateBind>>
       \/ \* sendApi(): getPropertyValue goes out to the service.
          /\ connectState # "DISCONNECT"
          /\ binder
          /\ binderAlive
          /\ door' = "sent"
          /\ main' = Tail(main)
          /\ UNCHANGED <<activity, screen, misuse, started, connectState, binder,
                         bindJob, failedBinds, bound, linked, svc, binderAlive,
                         deaths, cap, uiPost, lateBind>>
       \/ \* The binder is dead: getProperty's catching block calls onFailed.
          /\ connectState # "DISCONNECT"
          /\ binder
          /\ ~binderAlive
          /\ screen' = "Failed"
          /\ main' = OnResultCalls \o Tail(main)
          /\ UNCHANGED <<activity, misuse, started, connectState, binder, door,
                         bindJob, failedBinds, bound, linked, svc, binderAlive,
                         deaths, cap, uiPost, lateBind>>

\* Vehicle.release -> BaseServiceConnector.disconnectService: unbind, clear
\* cachedCall, drop the binder. The bind coroutine is not cancelled.
Release ==
    /\ main # <<>>
    /\ Head(main) = "release"
    /\ main' = Tail(main)
    /\ started' = FALSE
    /\ connectState' = "DISCONNECT"
    /\ binder' = FALSE
    /\ binderAlive' = FALSE
    /\ bound' = FALSE
    /\ linked' = FALSE
    /\ door' = IF door = "cached" THEN "dropped" ELSE door
    /\ UNCHANGED <<activity, screen, misuse, bindJob, failedBinds, svc, deaths,
                   cap, uiPost, lateBind>>

\* The runOnUiThread block posted by onComplete or onFailed sets the text.
UiRuns ==
    /\ main = <<>>
    /\ uiPost # "none"
    /\ screen' = uiPost
    /\ uiPost' = "none"
    /\ main' = OnResultCalls
    /\ UNCHANGED <<activity, misuse, started, connectState, binder, door, bindJob,
                   failedBinds, bound, linked, svc, binderAlive, deaths, cap,
                   lateBind>>

-----------------------------------------------------------------------------
(* SDK bind coroutine in BaseServiceConnector.connectService, Dispatchers.Default. *)

\* bindService returns true.
BindSucceeds ==
    /\ bindJob = "trying"
    /\ svc = "up"
    /\ bound' = TRUE
    /\ bindJob' = "done"
    /\ lateBind' = (lateBind \/ ~started)
    /\ UNCHANGED <<activity, main, screen, misuse, started, connectState, binder,
                   door, failedBinds, linked, svc, binderAlive, deaths, cap,
                   uiPost>>

\* bindService returns false and retryWhen waits CONNECT_RETRY_TIME for another try.
BindFails ==
    /\ bindJob = "trying"
    /\ svc = "down"
    /\ failedBinds < MaxRetry
    /\ failedBinds' = failedBinds + 1
    /\ UNCHANGED <<activity, main, screen, misuse, started, connectState, binder,
                   door, bindJob, bound, linked, svc, binderAlive, deaths, cap,
                   uiPost, lateBind>>

\* The last retry fails: the catch block sets DISCONNECT. cachedCall is left as it is.
BindGivesUp ==
    /\ bindJob = "trying"
    /\ svc = "down"
    /\ failedBinds = MaxRetry
    /\ bindJob' = "gaveUp"
    /\ connectState' = "DISCONNECT"
    /\ UNCHANGED <<activity, main, screen, misuse, started, binder, door,
                   failedBinds, bound, linked, svc, binderAlive, deaths, cap,
                   uiPost, lateBind>>

-----------------------------------------------------------------------------
(* Android and the vehicle service. *)

\* BaseServiceConnector.onServiceConnected, main thread: keep the binder and
\* ask for the capability config.
OnServiceConnected ==
    /\ main = <<>>
    /\ bound
    /\ svc = "up"
    /\ ~linked
    /\ linked' = TRUE
    /\ connectState' = "CONNECT"
    /\ binder' = TRUE
    /\ binderAlive' = TRUE
    /\ cap' = "inFlight"
    /\ UNCHANGED <<activity, main, screen, misuse, started, door, bindJob,
                   failedBinds, bound, svc, deaths, uiPost, lateBind>>

\* The capability answer arrives: consumeAllCached sends what cachedCall holds.
CapabilityAnswers ==
    /\ cap = "inFlight"
    /\ cap' = "none"
    /\ door' = IF door = "cached" /\ connectState = "CONNECT" /\ binder
               THEN "sent"
               ELSE door
    /\ UNCHANGED <<activity, main, screen, misuse, started, connectState, binder,
                   bindJob, failedBinds, bound, linked, svc, binderAlive, deaths,
                   uiPost, lateBind>>

\* IGetPropertyResult.onResult or onFailed arrives: the app's callback posts
\* its runOnUiThread block.
DoorAnswers ==
    /\ door = "sent"
    /\ door' = "answered"
    /\ \E result \in {"Position", "Failed"} : uiPost' = result
    /\ UNCHANGED <<activity, main, screen, misuse, started, connectState, binder,
                   bindJob, failedBinds, bound, linked, svc, binderAlive, deaths,
                   cap, lateBind>>

\* The vehicle service process dies. Calls it had not answered are never answered.
ServiceDies ==
    /\ svc = "up"
    /\ deaths < MaxDeaths
    /\ svc' = "down"
    /\ deaths' = deaths + 1
    /\ binderAlive' = FALSE
    /\ cap' = "none"
    /\ door' = IF door = "sent" THEN "lost" ELSE door
    /\ UNCHANGED <<activity, main, screen, misuse, started, connectState, binder,
                   bindJob, failedBinds, bound, linked, uiPost, lateBind>>

\* BaseServiceConnector.onServiceDisconnected, main thread.
OnServiceDisconnected ==
    /\ main = <<>>
    /\ linked
    /\ ~binderAlive
    /\ linked' = FALSE
    /\ connectState' = "CONNECTING"
    /\ binder' = FALSE
    /\ UNCHANGED <<activity, main, screen, misuse, started, door, bindJob,
                   failedBinds, bound, svc, binderAlive, deaths, cap, uiPost,
                   lateBind>>

\* A service that was not running when the app started comes up. It may never do so.
ServiceStartsLate ==
    /\ svc = "down"
    /\ deaths = 0
    /\ svc' = "up"
    /\ UNCHANGED <<activity, main, screen, misuse, started, connectState, binder,
                   door, bindJob, failedBinds, bound, linked, binderAlive, deaths,
                   cap, uiPost, lateBind>>

\* Android restarts a service that died.
ServiceRestarts ==
    /\ svc = "down"
    /\ deaths > 0
    /\ svc' = "up"
    /\ UNCHANGED <<activity, main, screen, misuse, started, connectState, binder,
                   door, bindJob, failedBinds, bound, linked, binderAlive, deaths,
                   cap, uiPost, lateBind>>

\* Nothing more happens after the activity is gone. Keeps end states from
\* counting as deadlocks.
Idle ==
    /\ activity = "destroyed"
    /\ main = <<>>
    /\ UNCHANGED vars

Next ==
    \/ Create \/ Destroy \/ Initialize \/ RequestDoor \/ Release \/ UiRuns
    \/ BindSucceeds \/ BindFails \/ BindGivesUp
    \/ OnServiceConnected \/ CapabilityAnswers \/ DoorAnswers
    \/ ServiceDies \/ OnServiceDisconnected \/ ServiceStartsLate
    \/ ServiceRestarts \/ Idle

\* Every step of the app, the SDK and Android happens once it can. The user
\* leaving, the service dying and a late service starting are never forced.
Fairness ==
    /\ WF_vars(Initialize) /\ WF_vars(RequestDoor) /\ WF_vars(Release)
    /\ WF_vars(UiRuns)
    /\ WF_vars(BindSucceeds) /\ WF_vars(BindFails) /\ WF_vars(BindGivesUp)
    /\ WF_vars(OnServiceConnected) /\ WF_vars(CapabilityAnswers)
    /\ WF_vars(DoorAnswers) /\ WF_vars(OnServiceDisconnected)
    /\ WF_vars(ServiceRestarts)

Spec == Init /\ [][Next]_vars /\ Fairness

-----------------------------------------------------------------------------
(* Properties. *)

\* The app never shows "Failed:" because it asked before initialize() or after release().
NoSelfInflictedFailure == ~misuse

\* The request can no longer be answered: release() dropped it from the cache,
\* the service died holding it, or the SDK stopped trying to bind while it was cached.
RequestStranded ==
    \/ door \in {"dropped", "lost"}
    \/ door = "cached" /\ bindJob = "gaveUp"

\* While the screen shows "Loading door position...", the request can still be answered.
NoStrandedRequest ==
    ~(activity = "created" /\ screen = "Loading" /\ RequestStranded)

\* The screen always leaves "Loading door position..." while the activity lives.
ScreenLeavesLoading ==
    (activity = "created") ~> (screen # "Loading" \/ activity = "destroyed")

\* After onDestroy the app holds no binding to the vehicle service.
ReleaseUnbinds ==
    (activity = "destroyed" /\ main = <<>>) => ~bound

\* ReleaseUnbinds without finding F2: a binding that the SDK's own retry made
\* after release() is not counted against the app.
ReleaseUnbindsOnceBound ==
    (activity = "destroyed" /\ main = <<>> /\ ~lateBind) => ~bound
=============================================================================
