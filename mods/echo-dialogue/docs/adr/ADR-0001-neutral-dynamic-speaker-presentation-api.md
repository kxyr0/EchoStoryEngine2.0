# ADR-0001: Neutral Dynamic Speaker Presentation API

## Status

Accepted for D6.

## Context

Future adapters need to resolve current presentation for a `SpeakerId` without making Echo Dialogue depend on Echo NPC, Story Engine, Entity, GameProfile or client rendering types.

## Decision

Create `dialogue-presentation-api` as a plain Java 17 public module depending only on `dialogue-contracts`.

The API version starts at `1.0` and is separate from mod version, save schema and network protocol.

The provider request contains only:

- `DialogueSessionId`;
- `DialogueId`;
- `SpeakerId`;
- `AudienceId`;
- kind `LINE` or `CHOICE`;
- authored fallback display name;
- optional authored fallback `SpeakerVisualRef`.

## Consequences

External provider mods can compile against public artifacts only. Echo Dialogue stays independent from Echo NPC and Story Engine. New request fields require an API version decision instead of silently widening context.
