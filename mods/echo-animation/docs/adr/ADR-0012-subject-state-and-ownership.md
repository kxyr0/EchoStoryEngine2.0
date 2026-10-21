# ADR-0012: Subject State And Ownership Leases

## Decision

Animation runtime state is stored per `AnimationSubjectId`, not per graph and
not per NPC implementation. Channel mutation requires an active lease.

## Consequences

- Two subjects can share one graph without sharing state.
- One channel can change without resetting another channel.
- A second owner receives conflict; last-write-wins is not used.
- A released lease cannot mutate state.
