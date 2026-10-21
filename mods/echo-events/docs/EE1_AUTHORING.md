# Событийные реакции EchoScript

## Блок When рядом с действиями

Подписку можно разместить прямо внутри `DialogueResource`, `StoryResource` или
`TriggerResource`. Когда событие произойдёт, выполняются команды в фигурных
скобках этого When. Отдельный Then для такого блока не нужен.

Например, файл `data/jdh/echo_dialogue/dialogues/mother.dialogue.es`:

```csharp
class Mother : DialogueResource
{
    Node Intro() { Line("john", "Поговорим о случившемся."); }

    When(Dialogue.Completed("jdh:father"), id: "after_father")
    {
        Dialogue.Play("jdh:mother");
    }
}
```

Файл `data/jdh/echo_story/stories/chapter.es`:

```csharp
class Chapter : StoryResource
{
    OnStart() { Story.Set("chapter_started", true); }

    When(Dialogue.Choice("jdh:father", "tell_truth"), id: "truth", once: true)
    {
        Scene.Play("jdh:father_angry_exit");
        Observation.Activate("jdh:house");
    }
}
```

Можно добавить несколько When в один класс. Обязательный `id` — локальное
стабильное имя подписки: маленькие латинские буквы, цифры, `_`, `/`, `-`.
Перестановка блоков не меняет их ID. Переименование id или владельца создаёт
другую подписку. `once: true` ограничивает реакцию одним принятием для каждого
игрока. По умолчанию новый occurrence снова вызывает обработчик.

Эти подписки активны на сервере, пока ресурс загружен, независимо от текущего
проигрывания диалога или истории. Обычные Node/OnStart остаются самостоятельным
графом; загрузка ресурса не исполняет тело When. Inline-обработчики принимают
строковые ID и те же пять команд действий, что Trigger-подписки. Произвольные
Story-команды, GoTo и переход к Node внутри When пока не поддерживаются.

Загрузка использует активные SERVER_DATA ресурсы Dialogue/Story, включая
подключённые datapacks и внешний content-pack. Trigger-файлы поддерживают как
новые блоки, так и прежний синтаксис. После изменения выполнить Doctor,
обычный reload данных для ресурсов и `/echo_trigger reload` для подписок.
Ошибка компиляции любого блока сохраняет предыдущий каталог подписок.

## Совместимость: When в Init и Then

Прежний синтаксис `TriggerResource` продолжает работать: `When(...)` в Init
задаёт условие, отдельный метод Then — действия. Для новых подписок удобнее
блоки выше. Пример сохраняется в
`data/jdh/echo_trigger/triggers/father_finished.trigger.es`:

```csharp
class FatherFinished : TriggerResource
{
    private void Init()
    {
        Id("jdh:father_finished");
        When(Dialogue.Completed("jdh:father"));
    }

    private void Then()
    {
        Observation.Activate("jdh:house");
        Scene.Play("jdh:father_leaves");
    }
}
```

Then исполняет действия по порядку. Доступны Scene.Play, Story.Start,
Dialogue.Play, Observation.Activate и Observation.Enable. Все эти действия
требуют player context. World event NPC.Died не предоставляет current player;
Doctor диагностирует такую несовместимую реакцию.

## Предикаты

| Предикат | Аргументы |
|---|---|
| Dialogue.Started / Completed / Cancelled / Failed | dialogue ID |
| Dialogue.Choice | dialogue ID, choice ID |
| Scene.Started / Completed / Cancelled / Failed | scene ID |
| Cutscene.Started / Completed / Cancelled | cutscene ID |
| Observation.Completed | `resource#point` |
| Npc.Arrived | actor ID, destination location ID |
| Npc.Died | actor ID |
| Animation.Marker | subject ID, animation ID, marker ID |
| Animation.Completed | subject ID, animation ID |

```csharp
class TruthChosen : TriggerResource
{
    private void Init()
    {
        Id("jdh:truth_chosen");
        When(Dialogue.Choice("jdh:father", "tell_truth"));
        Once();
    }
    private void Then()
    {
        Scene.Play("jdh:father_angry_exit");
    }
}
```

Одна When на ресурс. Стабильный subscription ID выводится из Id ресурса и
именованной точки When; его не задают вручную строкой Event.Subscribe.
Once/FireOnlyOnce/FireOnlyOncePerPlayer записывают потребление отдельно для
каждого игрока после принятия реакции. Удаление или переименование Trigger Id
создаёт другую подписку; это нужно учитывать при изменении контента.

Выбор должен существовать в опубликованном диалоге. Отменённый выбор,
отклонённый пакет или ручная гидрация состояния не означают Dialogue.Choice.
Cancelled и Failed не означают Completed. Animation.Marker наблюдает уже
принятый runtime marker и не заменяет точный callback для предметов/attachments.

В Then используются строковые ResourceId или известные строковые поля Init.
Произвольные Java callbacks, generic Event.Subscribe, произвольный код в Then,
неподдерживаемые cooldown и несколько When отклоняются компилятором.

После изменения выполнить Doctor и reload Trigger. Неуспешный reload оставляет
предыдущие подписки, а pending delivery использует сохранённую revision действий.
Статус фактических доставок доступен через `/echo_events inspect`.
