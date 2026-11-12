------------------------- MODULE MainActivityCalls -------------------------
(* Mirror of app/src/main/java/com/gomsoo/saigil/MainActivity.kt.

   Each definition lists the Vehicle SDK calls that one MainActivity method
   makes, in source order. The call names are "initialize" (vehicle.initialize),
   "requestDoor" (getCurrentDoorPosition) and "release" (vehicle.release).

   This is the only file to edit when MainActivity changes which SDK calls it
   makes, their order, or the method they are made from. *)

\* onCreate: vehicle.initialize(), then requestDriverDoorPosition().
OnCreateCalls == <<"initialize", "requestDoor">>

\* The onComplete and onFailed lambdas of getCurrentDoorPosition.
\* They log and set the text. They make no SDK call.
OnResultCalls == <<>>

\* onDestroy: vehicle.release().
OnDestroyCalls == <<"release">>
=============================================================================
