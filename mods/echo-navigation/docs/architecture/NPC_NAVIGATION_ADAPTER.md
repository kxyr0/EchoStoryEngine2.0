# NPC Navigation Adapter Boundary

Echo Navigation does not import Echo NPC.

The adapter owns the integration:

```text
Echo NPC public API
        |
echo-npc-navigation-adapter
        |
Echo Navigation public API
```

Responsibilities:

- map `ActorId` to `NavigationSubjectId`;
- resolve a materialized NPC body through `EchoNpcNavigationBodyBridge`;
- provide `NavigationAgentHandle` to Echo Navigation;
- register Story external actions for NPC movement;
- expose Java facade methods for `move`, `stop` and `isMoving`.

N5 Echo NPC forms do not change navigation subject identity. The adapter maps
`ActorId` to `NavigationSubjectId`; active form dimensions are owned by Echo NPC
materialization and placement checks before movement starts.

Forbidden dependencies:

- Echo Navigation importing `EchoNpcEntity`;
- Echo NPC importing vanilla navigation backend internals;
- Dialogue, Story or Trigger importing Echo Navigation implementation classes.
