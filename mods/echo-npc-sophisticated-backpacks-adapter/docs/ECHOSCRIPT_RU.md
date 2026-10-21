# EchoScript API

Канонические имена разбираются frontend-ами EchoScript и исполняются через один facade.
Script-adapter регистрирует Story action handlers, Dialogue external waits и Trigger callbacks:

```echoscript
Npc.EquipBackpack(npcId, backpackBlockPosition);
Npc.UnequipBackpack(npcId, destinationBlockPosition);
Npc.IsWearingBackpack(npcId);
Npc.HasBackpackTransition(npcId);
Npc.BackpackIsAt(position);
```

Файл `docs/examples/molly-backpack-story.es` проходит настоящий EchoScript
parser и `NpcBackpackScriptBinder` в unit test; отдельный тест проверяет arity
всех пяти публичных вызовов.

`EquipBackpack` и `UnequipBackpack` — blocking по смыслу сценария: submission
возвращает stable command ID, а host удерживает действие, пока
`NpcBackpackService.command(id)` не вернёт terminal outcome. Нельзя блокировать
Minecraft server thread циклом ожидания; host должен polling делать на своих
обычных story ticks.

Пример авторинга:

```echoscript
namespace JDH.Chapter1.Home;

class MollyGetsReady : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:molly_gets_ready", start: "start");
    }

    private void OnStart()
    {
        Npc.EquipBackpack("jdh:molly", Position(37, -3, 100));
        Npc.Move("jdh:molly", 50, -3, 112);
        Npc.UnequipBackpack("jdh:molly", Position(39, -3, 100));
    }
}
```

В `DialogueResource` те же две команды создают external wait: следующая `Line` появится только после terminal
success. В callback `TriggerResource` команда durable и idempotent, но у callback-хоста нет последовательного
continuation, которое нужно удерживать.

Предикаты facade (`IsWearingBackpack`, `HasBackpackTransition`, `BackpackIsAt`) доступны Java-интеграциям.
Текущие Story/Dialogue/Trigger frontend-контракты не предоставляют общий runtime predicate hook, поэтому DSL-ветвления
по этим трём именам в версии `0.2.0` не публикуются.

Position получает dimension из execution context. Для retry host обязан
повторить тот же command ID; новая сюжетная команда получает новый ID.
`Npc.SetBackpackNbt`, `Npc.RenderBackpack` и generic map payload отсутствуют.
