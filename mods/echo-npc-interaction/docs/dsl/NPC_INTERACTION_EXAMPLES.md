# NPC Interaction Examples

Dialogue:

```csharp
private BlockPosition ChairPosition = new BlockPosition(101, 64, 99);

private void Greeting()
{
    Line("Sasha", "Сейчас попробую.");
    Npc.Interact(Sasha, 100, 64, 100);
    Npc.Interact(Sasha, ChairPosition);
    Npc.StopInteraction(Sasha);
    Line("Sasha", "Готово.");
}
```

Story:

```csharp
private void OnStart()
{
    Npc.Interact("jdh:sasha", 100, 64, 100);
    Dialogue.Load("jdh:after_light_off");
}
```

Node action syntax:

```csharp
Npc.Interact("press_switch", "jdh:sasha", 100, 64, 100, next: "after_switch");
```
# NI2 chair example

```csharp
private BlockPosition ChairPosition =
    new BlockPosition(37, -3, 100);

Npc.Interact(Molly, ChairPosition);
Npc.StandUp(Molly);
```

Сценарий не знает мебельный мод. Если block use создаёт transient seat entity и сажает proxy, runtime переносит сидение на Molly.
