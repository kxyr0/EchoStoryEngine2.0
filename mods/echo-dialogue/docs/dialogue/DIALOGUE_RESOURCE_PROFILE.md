# Профиль DialogueResource ES2

Канонический путь: `data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es`.

Каноническая форма:

```csharp
class SampleGreeting : DialogueResource
{
    start Node Greeting()
    {
        Line("father", "Где ты был?");
        GoTo(Done);
    }

    Node Done()
    {
        Line("john", "Гулял.", thought: true);
    }
}
```

ID ресурса выводится из data namespace и basename файла; node ID — из имени `Node`. `Id`, `NodeId`, `Speaker` и
`Formerly` доступны как явные bounded overrides. Все детали, диагностики и правила совместимости описаны в
[high-level profile](../architecture/ECHOSCRIPT_HIGH_LEVEL_DIALOGUE.md).
