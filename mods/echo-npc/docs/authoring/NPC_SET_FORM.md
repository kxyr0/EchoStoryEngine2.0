# Npc.SetForm

`Npc.SetForm` меняет активную форму существующего NPC.

Контракт:

- вход: `ActorId` NPC и target `NpcFormId`;
- durable identity не меняется;
- если форма уже активна, операция возвращает idempotent результат;
- sleeping NPC не меняет форму до `Npc.WakeUp`;
- если новые габариты не помещаются в текущем месте, операция отклоняется.

Командный аналог:

```text
/echo_npc form set <npc_id> <form_id>
```

Для сценариев с резким скачком времени:

```text
FadeOut
Npc.SetForm(jdh:sasha, jdh:sasha/adult)
Npc.PlaceAt(jdh:sasha, target_world, target_position)
FadeIn
```

Порядок важен: сначала скрыть кадр, затем поменять форму и позицию, затем
открыть сцену.
