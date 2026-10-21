# ADR-0002: Server-Scoped Forge Provider Registration

## Status

Accepted for D6.

## Context

Provider sessions may need current server-scoped services from their own mod, but Echo Dialogue must not use global registries, ServiceLoader, reflection or hot registration.

## Decision

Echo Dialogue posts one `EchoDialogueSpeakerPresentationRegistrationEvent` per host after `MinecraftServer` exists. External mods register factories. Echo Dialogue closes the event, validates descriptors/API versions/duplicate IDs, creates one provider session per accepted factory and freezes the catalog sorted by provider ID.

Provider sessions are closed on host stop and recreated for the next integrated-server world or dedicated-server process.

## Consequences

Registration order does not determine resolution. Old provider sessions cannot be retained as Echo Dialogue state. The Forge bridge exposes only event/factory/context contracts, not host implementation.
