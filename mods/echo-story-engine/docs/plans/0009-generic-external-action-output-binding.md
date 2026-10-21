# 0009 Generic External Action Output Binding

## Цель

Добавить в Story Engine минимальный generic механизм, который позволяет внешнему action вернуть именованный output,
сохранить его как Story variable и использовать существующий `Require` для ветвления.

## Scope Firewall

- Нужно для определения, исполнения, сохранения и диагностики Story state.
- Не является самостоятельной gameplay-системой.
- Не содержит Dialogue, Choice, Outcome, Minecraft entity, UI или third-party mod semantics.

## Синтаксис

Старый `Action("node", type: "...", next: "...")` остаётся валидным.

Новый необязательный generic binding:

```csharp
Action("ask", type: "sample:external", output: "outcome", next: "check");
```

`output` означает: взять `ActionCompletion.outputs().get("outcome")` и записать это значение в Story variable
`outcome` перед переходом к `next`.

## Изменения

- `story-contracts`: `StoryRuntimeInput.externalActionCompleted(...)` получает overload с `outputs`.
- `story-core`: `StoryNode` хранит optional output binding; `RuntimeTransitionPolicy` применяет output только для
  successful completion и только если текущий external action node объявил `output`.
- `story-definition`: neutral `SourceNode` и валидаторы разрешают optional `output` у `external_action`.
- `story-echoscript-adapter`: intrinsic `Action` принимает named argument `output`.
- `story-forge-1.20.1`: `ForgeServerHost` передаёт `ActionCompletion.outputs()` в runtime input.

## Поведение

- Если `output` не объявлен, старое поведение не меняется.
- Если `output` объявлен, но completion не содержит такой key, execution переходит в `FAILED` с generic diagnostic.
- Duplicate completion не меняет variables повторно.
- Persistence не требует новой schema: output value хранится в уже существующем `StoryExecutionState.variables`.

## Проверки

- Plain JVM tests для runtime binding, missing output, duplicate completion и non-dialogue action output.
- Source/EchoScript tests для `Action(..., output: "...")`.
- Forge host test, что queued `ActionCompletion.outputs()` не теряются.
- `test`, `architectureTest`, `check`, `build`, `sourceShapeCheck`, `checkWhitespace`.

## Non-goals

- Нет Dialogue-specific Story типов.
- Нет выбора branch внутри external action node.
- Нет множественного binding в одном action.
- Нет expression language или generic UI.
