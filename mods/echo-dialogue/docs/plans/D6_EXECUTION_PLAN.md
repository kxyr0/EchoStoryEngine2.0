# D6 Execution Plan: Dynamic Speaker Presentation Provider Boundary

## Статус входа

D5 подтверждён пользователем 2026-07-20: static portrait, skin-head, fallback, reload, LINE restart и CHOICE restart считаются рабочим baseline. D6 можно начинать.

Echo Dialogue остаётся самостоятельным Forge 1.20.1 модом. D6 не добавляет Echo NPC, Story Engine, ActorId, NpcService, Entity lookup, GameProfile, HTTP, дистанции, lease-логику, animation, voice или client-side provider resolution.

## Audit

Authored presentation сейчас строится в `DialogueSessionEngine.presentation(...)`:

- LINE берёт текущую `CompiledDialogueLine` и отдаёт `speakerDisplayName`, optional `SpeakerVisualRef`, `text`;
- CHOICE берёт prompt из текущей line того же node, если он есть, и ordered `VisibleChoice`;
- CHOICE без prompt отправляется без speaker context;
- `ForgeDialogueSaveFlow.deliver(...)`, `resync(...)` и `ForgeDialogueRecovery.reattach(...)` отправляют `DialogueNetwork.sendOpen(...)` только после authoritative state/save;
- snapshots сохраняют только stable session cursor/state/fingerprint, а не transfer packet;
- `DialogueOpenMessage` уже переносит display name и optional visual, поэтому packet shape менять не нужно.

Нужно добавить общий server-side resolution path до `DialogueNetwork.sendOpen(...)`, чтобы initial start, advance, choice, reconnect и resync использовали одну политику.

## Module Decision

Добавляется новый plain-Java модуль `dialogue-presentation-api`, потому что D6 вводит публичную extension boundary, provider descriptor, API versioning и session contract. Размещение этих типов в `dialogue-contracts` смешало бы базовые Dialogue service contracts с отдельным provider lifecycle.

Итоговая зависимость:

```text
dialogue-contracts
        ↑
dialogue-presentation-api
        ↑
dialogue-forge-api-1.20.1
        ↑
dialogue-forge-1.20.1
```

D5 visual model будет продвинут из `dialogue-core` в `dialogue-contracts` как neutral public model, чтобы внешний provider мог вернуть `SpeakerVisualRef` без зависимости от `dialogue-core`. Это перенос, не дублирование.

## Public Neutral API

Package: `io.github.kxyr0.echodialogue.presentation.api`.

Типы:

- `SpeakerPresentationProviderId`: bounded namespaced provider ID.
- `SpeakerPresentationApiVersion`: current API `1.0`, exact major match, engine minor >= provider minimum minor.
- `SpeakerPresentationProviderDescriptor`: provider ID, display name, implementation version, required API major, minimum API minor.
- `SpeakerPresentationRequest`: `DialogueSessionId`, `DialogueId`, `SpeakerId`, `AudienceId`, `Kind.LINE|CHOICE`, authored fallback display name, optional authored fallback `SpeakerVisualRef`.
- `ResolvedSpeakerPresentation`: full dynamic display name and optional neutral `SpeakerVisualRef`.
- `SpeakerPresentationResolution`: explicit non-null variants `NOT_HANDLED`, `RESOLVED`, `TEMPORARILY_UNAVAILABLE`, `FAILURE`.
- `SpeakerPresentationProviderSession`: server-scoped neutral resolver session with idempotent close contract.

No Forge, Minecraft, EchoScript, persistence DTO, network packet, renderer, ActorId, Entity, ResourceLocation, Path, URL, GameProfile, line text, choice list, outcome or arbitrary context map.

## Forge Registration API

`dialogue-forge-api-1.20.1` adds:

- `EchoDialogueSpeakerPresentationRegistrationEvent`;
- `SpeakerPresentationProviderFactory`;
- `SpeakerPresentationProviderCreationContext`.

Echo Dialogue posts the event once per host after `MinecraftServer` exists. External mods register factories. After posting, registration closes. The event exposes snapshots only and rejects late registration.

## Provider Lifecycle

Host start:

1. Post registration event on server thread.
2. Validate descriptors and API versions.
3. Reject duplicate provider IDs deterministically.
4. Create one session per accepted factory.
5. Freeze catalog sorted by provider ID.

Host stop:

1. Stop accepting resolution.
2. Close each provider session once.
3. Isolate close exceptions.
4. Discard catalog with the host, so a fresh integrated-server world receives fresh sessions.

No global mutable registry, no ServiceLoader, no reflection, no classpath scanning, no IMC, no hot registration and no priorities.

## Resolution Policy

For each LINE or speakerful CHOICE presentation:

1. Build `SpeakerPresentationRequest` from authored speaker context.
2. Query all active compatible provider sessions in sorted provider ID order.
3. `NOT_HANDLED` does not claim the speaker.
4. `RESOLVED`, `TEMPORARILY_UNAVAILABLE` and `FAILURE` claim the speaker.
5. No claims means authored fallback.
6. Exactly one `RESOLVED` means use its full display name and optional visual.
7. Exactly one unavailable/failure means authored fallback plus bounded diagnostic.
8. Two or more claims means authored fallback plus deterministic ambiguity diagnostic containing sorted provider IDs.
9. Provider exception is adapted to `FAILURE`, bounded diagnostic, and unrelated providers are still queried.

Registration order never decides the winner. Numeric priorities are not supported.

## Validation And Bounds

Dynamic display name must be non-blank, must not contain control characters except ordinary whitespace policy, and must fit the existing speaker-name bound. Visual must be existing D5 `SpeakerVisualRef` with supported kind and bounded namespaced ID.

Invalid result never reaches the client. Echo Dialogue copies the provider result into its own immutable `DialoguePresentation`.

## LINE And CHOICE Integration

`DialoguePresentation` is extended with server-neutral IDs needed for D6 request construction:

- `DialogueId`;
- `AudienceId`;
- optional `SpeakerId`.

Network encoding continues to send only the existing D5 transfer fields. CHOICE without speaker context does not invoke providers and does not reuse a stale visual.

## Persistence, Fingerprint, Network

No Save Schema V3. No dynamic display name, dynamic visual, provider ID, provider status, provider diagnostics or provider session state is persisted.

Compiled fingerprint remains authored-only: authored display name and authored visual still affect compatibility; provider output never affects fingerprint.

Network protocol remains `3` unless packet shape changes. D6 should not change packet shape.

Reconnect, recovery reattach and save-failure resync rebuild the current presentation from compiled authored state and the active runtime provider catalog without incrementing dialogue revision. D6 does not implement automatic same-line refresh when external state changes.

## Commands

Add read-only operator commands:

- `/echo_dialogue providers`;
- `/echo_dialogue provider <providerId>`.

They show bounded Russian diagnostics: provider ID, display name, implementation version, API requirement, active/rejected/stopped status, rejection reason and small resolution counters. They do not expose provider objects, MinecraftServer, factory objects or external speaker state.

## Test Provider

Create separate Gradle build:

`integration-tests/echo-dialogue-presentation-test-mod`

It is not a root subproject and builds a separate Forge JAR against `build/integration-maven` public artifacts only.

Identity:

- mod ID: `echo_dialogue_presentation_test`;
- display name: `Echo Dialogue Presentation Test`;
- package: `io.github.kxyr0.echodialogue.presentationtest`.

It registers through the public Forge event and supports deterministic speakers for resolved visual, resolved text-only, not-handled, unavailable, failure, malformed and ambiguity scenarios. It must not bundle Echo Dialogue API classes.

## Build And Verification Strategy

Root build adds orchestration:

- publish public artifacts to `build/integration-maven`;
- build the separate provider mod against that repo;
- verify provider JAR has no API copies;
- stage final Echo Dialogue JAR and provider JAR for isolated tests.

Plain-JVM tests cover API IDs/versioning, model immutability/bounds, registration freeze, duplicate IDs, deterministic provider ordering, fallback, unavailable, exception, malformed result, ambiguity, no session mutation and fingerprint exclusion.

Forge/GameTests cover real event registration, command visibility, LINE dynamic name/visual, CHOICE dynamic speaker context, fallback cases, ambiguity diagnostics, revision stability, restart/recovery behavior and provider shutdown.

Manual visual acceptance remains user-observed for final client rendering.

## Production Type Plan

`dialogue-contracts`:

- move D5 visual types to `io.github.kxyr0.echodialogue.api.visual`.

`dialogue-presentation-api`:

- seven public neutral API files in one leaf package.

`dialogue-forge-api-1.20.1`:

- one registration event;
- one provider factory interface;
- one creation context record.

`dialogue-forge-1.20.1`:

- server-side provider registration;
- active catalog;
- resolution policy;
- bounded provider diagnostics/status;
- integration into save/recovery send path;
- command read model.

Each leaf package stays at seven production Java files or fewer.

## Risks

- Moving visual types changes package imports and packaging verifier paths.
- Adding IDs to `DialoguePresentation` touches many tests.
- Real provider GameTests need the separate provider mod on Forge runtime classpath; this must be automated without Maven Local.
- Command diagnostics must stay bounded and not become a metrics framework.

## Rollback

Remove `dialogue-presentation-api`, Forge registration API, provider catalog/resolver, command additions, integration-test provider build and docs. Move visual imports back only if no downstream public API depends on them. Persistence snapshots remain compatible because D6 does not change save schema.

## Explicit Non-Goals

No Echo NPC integration, no ActorId/NpcService/NpcSkinId, no Entity UUID, no distance/lease system, no provider priorities, no partial field merge, no client-side provider resolution, no remote skin downloads, no GameProfile lookup, no animation changes and no Story-specific speaker model.
