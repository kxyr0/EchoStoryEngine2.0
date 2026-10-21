# NPC на EchoScript 2

Файлы: `data/<namespace>/echo_npc/npcs/**/*.npc.es`.

```csharp
class ChrisNpc : NpcResource
{
    Id("jdh:chris");
    Display("Крис");
    Skin("jdh:chris");
    NameColor("#8e87f8ff");
}
```

`Id` можно опустить: имя `chris.npc.es` в namespace `jdh` даёт `jdh:chris`.
`Display` обязателен. `Skin`, `NameColor` и `DefaultForm` необязательны.
Например: `DefaultForm("jdh:sasha/child");`. Каждый вызов принимает одну строку.
`namespace` необязателен. Старые файлы с `Build()` продолжают загружаться.

Skin `jdh:chris` ищется в `assets/jdh/textures/echo_npc/skins/chris.png`.
ActorId приходит из spawn action или команды и отличается от ID definition.
Для одного ID должен существовать ровно один активный файл NPC.
