# Writing DialogueResource

Фактический loader path:

`data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es`

Для этого pack: `src/main/resources/data/jdh/echo_dialogue/dialogues/`.

Новый стандартный профиль:

```csharp
class ExampleDialogue : DialogueResource
{
    Node FirstMeeting()
    {
        Line("john", "Первая строка.");
    }
}
```

Файл `example_dialogue.dialogue.es` получает ID `jdh:example_dialogue`. Class и EchoScript namespace можно свободно
переименовывать; identity меняется при переименовании файла либо через явный `Id(...)`.

Для графа из нескольких nodes:

```csharp
class ExampleMultiDialogue : DialogueResource
{
    start Node Greeting()
    {
        Line("john", "Первая строка.");
        GoTo(Done);
    }

    Node Done()
    {
        Line("john", "Диалог завершен.");
    }
}
```

`Line("john", ...)` связывается с `jdh:john`; актуальные имя, skin, portrait и NPC form приходят из runtime provider.
`Speaker(...)` нужен только для sparse override. `Formerly("old_node", CurrentNode)` явно мигрирует старый saved cursor.

Используйте `authoring/templates/dialogue.dialogue.es.template` и
`authoring/templates/dialogue-multi.dialogue.es.template`. Legacy `Build/Init/Alias/Nodes/NodeDef` остается readable,
но новые файлы и шаблоны не должны его генерировать.
