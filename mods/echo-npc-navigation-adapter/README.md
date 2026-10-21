# Echo NPC Navigation Adapter

This adapter connects Echo NPC to Echo Navigation without implementation imports between those engines.

It provides:

- `ActorId` to `NavigationSubjectId` mapping;
- a `NavigationAgentResolver` that reads `NpcNavigationBody` from Echo NPC public API;
- Story external actions `echo_navigation:npc_move` and `echo_navigation:npc_stop`;
- Story EchoScript sugar `Npc.Move(...)`, `Npc.Sprint(...)` and `Npc.Stop(...)`;
- Dialogue EchoScript commands `Npc.Move(...)`, `LineAfterMove(...)` and `Npc.Stop(...)` through durable external waits;
- Trigger callback actions `Npc.Move(...)`, `Npc.Sprint(...)` and `Npc.Stop(...)`;
- Java facade methods for `move`, `stop` and `isMoving`.

The adapter depends on public artifacts from:

- `echo-navigation`;
- `echo-npc`;
- `echo-story-engine`.
- `echo-dialogue`;
- `echo-trigger`.

Examples live in `examples/`:

- `father_leaves.dialogue.es`: a dialogue where `Npc.Move(...)` starts walking, normal `Line(...)` can speak during movement, and `LineAfterMove(...)` waits for arrival.
- `father_gate.trigger.es`: a trigger callback that starts an NPC move.
- `father_walk.story.es`: a Story graph node using `Npc.Move(...)`. Story movement resumes after durable start by
  default; use `wait: true` or `await: "ARRIVED"` when the next node must wait for arrival.

Locomotion authoring uses block coordinates or `Vector3(...)`:

```csharp
Npc.Move("jdh:father", Vector3(200, 64, 180));
Npc.Move("jdh:father", Vector3(200, 64, 180), speed: 2.5);
Npc.Sprint("jdh:father", Vector3(200, 64, 180));
Npc.Sprint("jdh:father", Vector3(200, 64, 180), speed: 5.5, syncAnimation: false);
```

Without `speed`, walk uses the authored `1.35` blocks/second and sprint uses `4.3` blocks/second. With `speed`, the
animation adapter follows measured movement speed. Set `syncAnimation: false` to keep native clip playback speed.
