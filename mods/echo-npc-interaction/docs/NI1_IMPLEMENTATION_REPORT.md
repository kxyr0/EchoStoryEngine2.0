# NI1 Implementation Report

## Versions

- Echo NPC Interaction: `0.1.0`.
- Echo NPC Interaction Navigation Adapter: `0.1.0`.
- Existing public APIs `echo-npc`, `echo-navigation`, `echo-script`: не менялись.
- New public query API: `NpcInteractionQueryService.isInteracting(npcId)`.
- Existing binders обновлены: Story EchoScript adapter и Dialogue EchoScript adapter.

## DSL evidence

Dialogue parser/binder PASS покрывает:

```csharp
Npc.Interact(Sasha, 100, 64, 100);
Npc.Interact(Sasha, TargetBlock);
Npc.StopInteraction(Sasha);
```

Story parser/binder PASS покрывает:

```csharp
Npc.Interact("sample:father", 100, 64, 100);
```

В Story текущий binder не имеет общей таблицы NPC aliases, поэтому для Story
используется namespaced id literal.

`Npc.IsInteracting(Sasha)` как host-level DSL predicate в этом проходе не
зарегистрирован; доступен public query API.

## Provider evidence

Compile-level provider:

```text
provider = echo_npc_interaction:vanilla_block_use
specificity = 10
execution policy = server-side Minecraft block use pipeline
verification policy = BLOCK_STATE_CHANGED
```

Runtime world evidence по конкретным BlockId требует manual Minecraft acceptance
или GameTest world и в этом проходе не выполнялся.

## Navigation evidence

Adapter evidence:

```text
execution port = NpcInteractionNavigationPort
backend = active Echo Navigation backend from NavigationQueryService
owned move = deterministic NavigationMoveId
checks = collision, headroom, candidate line of sight
pathfinding = delegated to Echo Navigation
```

## Interaction evidence

Vanilla provider evidence:

```text
before fingerprint captured
FakePlayer compatibility context positioned at NPC
server gameMode.useItemOn called
proxy menu/riding cleanup checked
after fingerprint captured
receipt built on verified change
```

## Chair evidence

NI1 generic provider explicitly rejects proxy seating:

```text
NPC seated = not inferred from proxy
proxy seated = typed failure
diagnostic = NPC_INTERACTION_REQUIRES_SEAT_PROVIDER
```

Real chair support still requires a specialized provider/adapter for the audited
chair mechanics.

## Restart evidence

Core contains receipt replay and deterministic ids. Full disk persistence,
crash/restart replay and idempotency across a real server restart are documented
and partially modeled, but not proven by manual Minecraft acceptance in this pass.

## Compatibility evidence

Compile/runtime boundaries support:

- vanilla world-state object through normal block use;
- compatible mod world-state object through normal block use;
- player-bound GUI typed failure;
- chair/mod furniture through future provider.

## Source shape

New repos pass:

```text
Java files > 180 logical LOC = 0
EchoScript files > 180 logical LOC = 0
leaf Java packages > 7 files = 0
leaf content folders > 7 files = 0
```

## Честность

- `InteractionResult.SUCCESS` не считается доказательством полезного результата.
- Player proxy не считается настоящим телом NPC.
- Proxy riding state не считается сидением NPC.
- Найденный approach не считается proof of arrival.
- Arrival не считается successful use.
- Поддержка любого мода без provider не обещается.
