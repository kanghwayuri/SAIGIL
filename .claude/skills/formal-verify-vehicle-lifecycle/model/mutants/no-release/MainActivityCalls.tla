------------------------- MODULE MainActivityCalls -------------------------
\* Broken variant for `check --self-test`: onDestroy does not call release().
OnCreateCalls == <<"initialize", "requestDoor">>
OnResultCalls == <<>>
OnDestroyCalls == <<>>
=============================================================================
