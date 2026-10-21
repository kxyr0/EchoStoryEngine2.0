# NPC Move Authoring

Target canonical syntax:

```csharp
Npc.Move(Father, 200, 64, 180);
```

Required semantics for durable movement:

- start one durable movement;
- save the movement before dispatch;
- Story `Npc.Move(...)` resumes after movement start by default, without blocking the server thread;
- Story `Npc.Move(..., wait: true)` or `Npc.Move(..., await: "ARRIVED")` resumes only after `ARRIVED`;
- fail the script on movement failure when arrival was requested;
- do not create duplicates after restart;
- do not teleport on failure.

Implemented Story graph syntax:

```csharp
Npc.Move("father_move_gate", "jdh:john_father", 200, 64, 180, next: "after_father_arrived");
Npc.Move("father_move_gate_wait", "jdh:john_father", 200, 64, 180, wait: true, next: "after_father_arrived");
```

Story still needs a node id and `next` because Story uses an explicit graph. The call lowers to external action type
`echo_navigation:npc_move`. By default it lowers with `await: "STARTED"` so the next Story node can run while the NPC is
walking. Use `wait: true` or `await: "ARRIVED"` for the old arrival-gated behavior.

Dialogue syntax inside a node method:

```csharp
Line("father", "Я проверю ворота.");
Npc.Move(father, 200, 64, 180);
Line("father", "Пока иду, расскажу, что видел у дороги.");
LineAfterMove("father", "Я на месте.");
```

In dialogue, `Npc.Move(...)` starts durable movement and immediately resumes normal `Line(...)` calls, so characters can
walk and talk. `LineAfterMove(...)` is the first line that waits for the matching movement and resumes only after
`ARRIVED`.

Trigger callback syntax:

```csharp
private void OnTriggerEnter(PlayerRef player)
{
    Npc.Move("jdh:john_father", 200, 64, 180);
}
```

Trigger callback movement is fire-and-forget because Trigger callbacks publish actions; they do not own a durable
script wait.

Failure behavior:

- Story `await: "STARTED"` completes with `SUCCESS` after durable movement is accepted and returns `moveId`/`status`.
- Story `await: "ARRIVED"` completes with `SUCCESS` only on `ARRIVED`.
- Any terminal non-arrival status completes an arrival-gated Story action with `FAILURE`.
- Dialogue `LineAfterMove(...)` continues on `ARRIVED` and cancels the dialogue on any terminal non-arrival status.
- The movement ID is deterministic from Story execution ID, NPC ID and destination.
