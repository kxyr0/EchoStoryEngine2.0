# ADR 0009: Offline Reattach And Terminal Retention

Status: accepted for D2.

Player logout detaches an active durable session instead of cancelling it. Player login reattaches by sending the current presentation without changing session revision. Terminal records remain queryable until explicit acknowledgement or bounded cleanup.

Reason:

- network disconnect is not a dialogue choice;
- recovery after restart may happen before the player is online;
- a future adapter must be able to observe terminal result after the crash window between completion and external consumption.

Consequences:

- active recovery does not require a live `ServerPlayer`;
- reattach is a UI resend, not a domain transition;
- terminal cleanup never removes ACTIVE or blocked snapshots;
- duplicate ACTIVE snapshots for one audience are blocked deterministically and reported through diagnostics.
