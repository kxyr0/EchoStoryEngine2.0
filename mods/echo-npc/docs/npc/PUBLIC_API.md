# Public API

## Generic body attachments

`npc-forge-api-1.20.1` exposes a client-only, renderer-neutral body attachment
boundary. `NpcBodyAttachmentRenderRegistrationEvent` accepts extensions that
render against `TORSO_BACK`, `LEFT_HAND` or `RIGHT_HAND` through
`NpcBodyAttachmentRenderContext`. The API exposes public actor/form metadata and
model anchors, not `EchoNpcEntity` or renderer internals.

## Client render cancellation

`npc-forge-api-1.20.1` exposes the explicit terminal result
`NpcClientRenderResult.CANCELLED`. Echo NPC stops later render extensions and
does not render the NPC model, name, shadow or layers for that frame.

Основной public contract находится в `npc-contracts`.

## Service

`NpcService` предоставляет:

- `health()`;
- `definitions()`;
- `actors()` / `listActors(limit)`;
- `actor(ActorId)` / `query(ActorId)`;
- `spawn(SpawnNpcRequest)`;
- `teleport(TeleportNpcRequest)`;
- `despawn(ActorId)`;
- `despawn(ActorId, ActorMutationId)`;
- `operation(String)`;
- `listOperations(int)`;
- `blockedActors(int)`;
- `retryRecovery(ActorId)`.
- `setForm(NpcFormChangeRequest)`;
- `changeSkin(NpcSkinChangeRequest)`;
- `placeAt(NpcPlaceRequest)`;
- `getForm(ActorId)` / `isForm(ActorId, NpcFormId)`;
- `formCatalog(int)` / `form(NpcFormId)`.

Read-only методы синхронны. Mutating методы в N2 возвращают submission result:
`QUEUED` означает, что durable write принят в bounded persistence queue. Entity
создается, перемещается или удаляется позже, после successful repository result
на server thread.

Для external integrations `SpawnNpcRequest` и `TeleportNpcRequest` могут
содержать optional `ActorMutationId`. `despawn(ActorId, ActorMutationId)`
принимает такой же stable idempotency key отдельным аргументом. Когда он задан,
runtime использует `ActorMutationId.value()` как `operationId`, а повторный
вызов с тем же ID переиспользует уже известную operation и не ставит вторую
persistence mutation.

`ActorMutationId` является namespaced value object длиной до 64 символов. Он не
является `ActorId`, Minecraft Entity UUID или `EntityBindingId` и не становится
authority для actor identity.

## Results

Mutating result содержит `operationId`, когда operation принята:

- spawn: `QUEUED`, `REJECTED`, `ALREADY_ACTIVE`, `ACTOR_ID_CONFLICT`,
  `DEFINITION_NOT_FOUND`, `WORLD_NOT_FOUND`, `PLATFORM_REJECTED`,
  `SERVICE_STOPPED`;
- teleport: `QUEUED`, `REJECTED`, `MOVED`, `ACTOR_NOT_FOUND`,
  `WORLD_NOT_FOUND`, `PLATFORM_REJECTED`, `SERVICE_STOPPED`;
- despawn: `QUEUED`, `REJECTED`, `TOMBSTONED`, `REMOVED`, `ALREADY_ABSENT`,
  `PLATFORM_REJECTED`, `SERVICE_STOPPED`.
- form change: `QUEUED`, `APPLIED`, `ALREADY_ACTIVE`, `ACTOR_NOT_FOUND`,
  `FORM_NOT_FOUND`, `SPACE_BLOCKED`, `REJECTED`, `SERVICE_STOPPED`;
- skin change: `QUEUED`, `COMMITTED`, `ALREADY_ACTIVE`, `NOT_FOUND`,
  `REJECTED`, `SERVICE_STOPPED`, `FAILURE`;
- place: `QUEUED`, `PLACED`, `ACTOR_NOT_FOUND`, `WORLD_NOT_FOUND`,
  `SPACE_BLOCKED`, `REJECTED`, `SERVICE_STOPPED`.

`NpcResults.Operation` stores bounded in-memory terminal status:
`QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` or `REJECTED`.

## Read Model

`NpcActorView` exposes logical identity and recovery state:

- `ActorId`, `NpcDefinitionId`, display name and skin ID from the pinned
  definition;
- desired world, position and rotation;
- desired presence and materialization status;
- definition compatibility;
- optional materialized position;
- optional active form id, revision and fingerprint;
- optional active rest attachment;
- actor and repository revisions;
- optional blocked reason.

## Forms

`NpcFormId` is authored body/appearance identity, not NPC identity. A single
`ActorId`, for example `jdh:sasha`, can switch between `jdh:sasha/child` and
`jdh:sasha/adult` through `Npc.SetForm`.

`NpcFormSnapshot` exposes skin, model profile, server dimensions, body geometry,
anchors, animation graph id, face profile id and portrait resource id. Runtime
persists only active form id/revision/fingerprint in actor save schema v3.

Public API does not expose `Path`, `File`, JSON/Gson classes, Minecraft entity
UUID, `ServerLevel`, `Entity`, renderer classes or repository internals.

## Dynamic skins

`NpcSkinChangeRequest` содержит durable `ActorId`, target `NpcSkinId` и optional
`ActorMutationId`. `NpcService.changeSkin` не меняет actor identity, active form,
body geometry или attachments. Новый skin сохраняется до синхронизации entity;
resource existence проверяет клиентский resolver с bounded fallback policy.

## Forge Bridge

Другие Forge mods получают service через `EchoNpcServiceBridge.lookup()`.
Bridge возвращает `EchoNpcServiceLookupResult` со статусами:

- `RESOLVED`;
- `UNAVAILABLE`;
- `DUPLICATE_PROVIDER`.

Bridge не использует ServiceLoader, reflection или global singleton service.
