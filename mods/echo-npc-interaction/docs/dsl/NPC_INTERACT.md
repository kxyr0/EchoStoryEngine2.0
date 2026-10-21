# Npc.Interact

`Npc.Interact` запускает один durable interaction sequence для NPC и точного
block target.

```csharp
Npc.Interact(Sasha, 100, 64, 100);
Npc.Interact(Sasha, new BlockPosition(100, 64, 100));
Npc.Interact(Sasha, TargetBlock, face: AUTO, hand: MAIN_HAND);
```

Runtime выполняет: target inspect, provider selection, approach resolution,
Echo Navigation move, alignment, server-side use, verification, receipt и script
resume.

Команда не требует `BlockId`, provider id, mod id, item id или backend id.
# NI2 update

Координаты всегда указывают на сам интерактивный block target. Для Handcrafted chair указывается блок стула, а не место перед ним.

Runtime сначала завершает approach. Только после arrival и alignment выполняется generic proxy use. Если proxy начал сидеть на entity, результат переносится на настоящего NPC через direct attach или `EchoNpcSeatAnchorEntity`.

`InteractionResult.SUCCESS` не считается достаточным доказательством. Успех требует полезного consequence: изменение мира, подтверждённый ride handoff или другое typed evidence в receipt v2.
