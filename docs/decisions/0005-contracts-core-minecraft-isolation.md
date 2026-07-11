# 0005 Contracts/Core Minecraft Isolation

## Status

Accepted.

## Context

Minecraft and Forge types are convenient but would leak platform semantics into public and core contracts.

## Decision

`story-contracts` and `story-core` must not import Minecraft, Forge, Mojang logging, SLF4J, NBT, Codec, GUI,
networking, filesystem, or third-party mod APIs. Use neutral Story Engine value types.

## Alternatives

- Wrap `ResourceLocation` directly. Rejected because contracts/core would depend on Minecraft.
- Accept platform types in core for speed. Rejected because it blocks portability and unit testing.

## Consequences

Adapters translate platform facts into neutral input before calling core.

## Enforcement

ArchUnit rules and module dependencies.

## Links

- `docs/architecture/DEPENDENCY_RULES.md`
- `story-contracts/src/main/java/io/github/kxyr0/echostoryengine/story/api/identity/NamespacedId.java`
