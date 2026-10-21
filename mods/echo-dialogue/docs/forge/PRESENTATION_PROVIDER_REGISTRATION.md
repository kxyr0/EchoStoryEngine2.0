# Presentation Provider Registration D6

External Forge mods register dynamic speaker presentation providers through:

`EchoDialogueSpeakerPresentationRegistrationEvent`

Package:

`io.github.kxyr0.echodialogue.forge.api`

## Lifecycle

1. Echo Dialogue host starts after `MinecraftServer` exists.
2. Echo Dialogue posts one registration event on the Forge event bus.
3. External mods call `event.register(SpeakerPresentationProviderFactory)`.
4. Echo Dialogue closes registration.
5. Descriptors are validated.
6. Compatible provider sessions are created.
7. The active provider catalog is frozen and sorted by provider ID.
8. Provider sessions are closed on server stop.

Factories are registered, not provider sessions. One session is created per server host. Integrated-server world switch creates fresh sessions.

## Factory Contract

`SpeakerPresentationProviderFactory` exposes:

- `descriptor()`;
- `create(SpeakerPresentationProviderCreationContext context)`.

`SpeakerPresentationProviderCreationContext` contains only the current `MinecraftServer`. The provider session itself exposes neutral `resolve(SpeakerPresentationRequest)`.

No Dialogue host implementation, catalog mutation, provider object list, networking object or client renderer is exposed.

## Commands

Read-only operator commands:

```text
/echo_dialogue providers
/echo_dialogue provider <providerId>
```

They show descriptor, API requirement, status, rejection reason and bounded counters/diagnostics. They do not expose external provider state or speaker request history.
