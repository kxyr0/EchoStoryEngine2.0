# NPC Navigation Adapter Map

Repository: `D:\JDHSeason\mods\echo-npc-navigation-adapter`

| Package | Responsibility |
| --- | --- |
| `navigation` | `ActorId` to `NavigationSubjectId`, agent handle and agent resolver. |
| `script` | Shared argument parsing, Java facade for move/stop/isMoving and deterministic navigation IDs. |
| `dialogue` | Dialogue external wait listener, non-blocking move start and LineAfterMove arrival waits. |
| `story` | Story extension factory, session and action handlers. |
| `trigger` | Trigger callback listener for fire-and-forget NPC movement actions. |
| `runtime` | Server-scoped adapter sessions ticked from Forge server tick. |

Event flow:

```text
Story Npc.Move(...)
    -> NpcMoveStoryAction
    -> NpcNavigationScriptFacade
    -> EchoNavigationServiceBridge
    -> NavigationService.move
    -> NpcNavigationAgentResolver
    -> EchoNpcNavigationBodyBridge
    -> Echo NPC materialized Mob
```

Dialogue flow:

```text
Dialogue Npc.Move(...)
    -> CompiledDialogueCommand echo_navigation:npc_move
    -> durable DialogueExternalWait
    -> DialogueExternalWaitDispatchEvent
    -> NpcDialogueNavigationListener
    -> NavigationService.move
    -> remember move by dialogue session and NPC
    -> DialogueService.resumeExternal

Dialogue LineAfterMove(...)
    -> CompiledDialogueCommand echo_navigation:npc_move_arrival
    -> durable DialogueExternalWait
    -> NpcDialogueNavigationListener
    -> poll remembered move until ARRIVED
    -> DialogueService.resumeExternal
```

Trigger flow:

```text
Trigger Npc.Move(...)
    -> TriggerCallbackAction Npc.Move
    -> TriggerCallbackInvocationEvent
    -> NpcTriggerNavigationListener
    -> NavigationService.move
```

Dependency rule:

- Adapter imports public APIs only.
- Echo Navigation never imports Echo NPC.
- Echo NPC never imports Echo Navigation implementation.

Navigation 0.2.0:

- `Npc.Move(...)` still sends a normal `NavigationRequest`.
- Backend selection is owned by Echo Navigation runtime config.
- The adapter exposes the materialized `Mob` through `NavigationAgentHandle`; Echo Navigation reads actual width and
  height from that handle for A* agent profiling.
