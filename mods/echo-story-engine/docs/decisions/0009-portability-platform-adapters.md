# 0009 Portability And Platform Adapters

## Status

Accepted.

## Context

The first platform is Forge 1.20.1, but the story runtime should not be tied to that version or loader.

## Decision

Put platform lifecycle, logging, time, and state adapter implementations in `story-forge-1.20.1`. Keep contracts/core
loader-neutral.

## Alternatives

- Store Forge references in runtime state. Rejected because it prevents migration and recovery.
- Create multiple adapters now. Rejected because only Forge 1.20.1 is required.

## Consequences

Future Minecraft version or loader changes should replace adapters first, not core runtime contracts.

## Enforcement

Module split, ArchUnit rules, and `docs/architecture/PORTABILITY.md`.

## Links

- `docs/architecture/PORTABILITY.md`
- `story-forge-1.20.1/src/main/java/io/github/kxyr0/echostoryengine/story/forge/composition/ForgeStoryCompositionRoot.java`
