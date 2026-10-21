# Написание диалогов

Новый канонический стиль — high-level `DialogueResource`:

```csharp
class SampleQuestion : DialogueResource
{
    Node Question()
    {
        Line("father", "Ты поможешь мне?");
    }
}
```

Файл `data/sample/echo_dialogue/dialogues/question.dialogue.es` компилируется как `sample:question`. `namespace`,
`Build`, `Init`, `Start`, `Alias`, `Nodes`, `NodeDef`, `new Node` и `return node` не нужны. Один `Node` становится
entry автоматически; для нескольких нужен ровно один `start Node`. Переходы пишутся как `GoTo(TargetNode)`.

Полный авторский контракт: [ECHOSCRIPT_HIGH_LEVEL_DIALOGUE](../architecture/ECHOSCRIPT_HIGH_LEVEL_DIALOGUE.md).
Runtime-вызовы: [DIALOGUE_DSL](../runtime/DIALOGUE_DSL.md). Архитектура lowering:
[ECHOSCRIPT_DIALOGUE_LOWERING](../architecture/ECHOSCRIPT_DIALOGUE_LOWERING.md).

Legacy D7/Build/Init/Nodes синтаксис остается совместимым, но не должен использоваться в новых шаблонах.
