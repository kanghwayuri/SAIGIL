------------------------- MODULE MainActivityCalls -------------------------
\* Broken variant for `check --self-test`: onCreate asks before initialize().
OnCreateCalls == <<"requestDoor", "initialize">>
OnResultCalls == <<>>
OnDestroyCalls == <<"release">>
=============================================================================
