# NPC Stop Authoring

Target canonical syntax:

```csharp
Npc.Stop(Father);
```

Implemented Story graph syntax:

```csharp
Npc.Stop("father_stop", "jdh:john_father", next: "after_stop");
```

Dialogue and Trigger can use `Npc.Stop(father)` / `Npc.Stop("jdh:john_father")` in the same places as `Npc.Move`.

Runtime behavior:

- cancels the active movement for the NPC subject;
- stops the backend path;
- records `CANCELLED`;
- does not teleport the NPC;
- returns success to Story after the cancel request is accepted.

`Npc.IsMoving(npcId)` exists as a Java facade query in the adapter. Direct EchoScript predicate binding is still not
implemented.
