# ADR-0003: Full Result, Fallback And Runtime-Only Output

## Status

Accepted for D6.

## Context

Partial merge rules make failure and ambiguity hard to reason about. Persisting provider output would couple durable sessions to external runtime state.

## Decision

A claiming provider returns a full `ResolvedSpeakerPresentation`: display name, optional name color and optional visual. There is no partial merge.

Resolution policy:

- no claims -> authored fallback;
- one `RESOLVED` -> dynamic presentation;
- one unavailable/failure/malformed/exception -> authored fallback plus bounded diagnostic;
- multiple claims -> authored fallback plus deterministic ambiguity diagnostic.

Dynamic provider output is excluded from persistence and compiled fingerprint.
This ADR originally kept the network schema stable at protocol `3`; line cues
and speaker name color later changed `DialogueOpenMessage`, so the current network protocol is `5`.

## Consequences

Provider removal or outage cannot block recovery. Reconnect/restart resolves again from current provider catalog. D6 does not implement automatic same-line refresh or provider priorities.
