------------------------- MODULE MainActivityCalls -------------------------
\* Broken variant for `check --self-test`: onCreate releases right after asking.
OnCreateCalls == <<"initialize", "requestDoor", "release">>
OnResultCalls == <<>>
OnDestroyCalls == <<"release">>
=============================================================================
