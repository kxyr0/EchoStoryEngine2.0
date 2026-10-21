# Resource Loading

Forge runtime подключает reload listener, который ищет:

```text
data/*/echo_npc/npcs/**/*.npc.es
```

## Reload policy

1. Все candidates читаются как UTF-8.
2. Каждый source компилируется через `EchoScriptNpcCompiler`.
3. Definitions собираются в candidate catalog.
4. Duplicate `NpcDefinitionId` становится error diagnostic.
5. Если есть error diagnostic, active catalog не заменяется.
6. Если ошибок нет, `NpcCatalogState` атомарно публикует новый immutable catalog.

Existing actors остаются привязаны к definition data, с которой были spawned.
Reload не переписывает уже активных actors.
