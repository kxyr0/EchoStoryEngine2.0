# ADR 0007: Save Before Presentation

Status: accepted for D2.

Dialogue host must durably save the next checkpoint before sending any client `open`, `update` or `close` presentation.

Reason:

- crash after presentation but before save would show a state that recovery cannot reproduce;
- terminal close must not hide a result before it is retained for future consumers;
- stale repository writers must not advance client state.

Consequences:

- start/advance/cancel/complete use preview transitions first;
- state is installed only after successful repository save;
- save failure, queue saturation and repository revision conflict leave previous authoritative state in place;
- client may receive a resync to the previous durable line.
