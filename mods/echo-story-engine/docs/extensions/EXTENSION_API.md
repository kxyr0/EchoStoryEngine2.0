# Story Extension API

M5 public API состоит из двух артефактов:

- `story-extension-api`: loader-neutral Java contracts.
- `story-extension-forge-1.20.1`: Forge 1.20.1 registration bridge.

Extension descriptor объявляет extension id, display name, implementation version, required API major, minimum API
minor и список action descriptors. Engine поддерживает Story Extension API `1.0`: major должен совпасть, minor engine
должен быть не меньше minimum minor.

Handler получает `ActionStartRequest`, `ActionRecoveryRequest` или `ActionCancelRequest` на server thread. Завершение
возвращается через `ActionCompletionSink`, который можно вызвать с любого thread. Engine применяет completion только
через bounded server-thread queue.

`ActionExecutionId` является idempotency key для start/recover/completion. Handler не должен хранить Minecraft или
Forge objects в Story state, completion outputs или public descriptor.

`ActionCompletion.outputs()` содержит только bounded string values. Если StoryResource action объявляет generic
binding `output: "name"`, Engine берёт `outputs().get("name")`, сохраняет значение в Story variable `name` перед
переходом к `next` и после этого использует обычные `Require` nodes для ветвления. Если key отсутствует, execution
переходит в `FAILED` с `ESE-ACTION-006`; без `output` старое поведение completion не меняется.

StoryResource action arguments задаются плоскими дополнительными named literal
arguments у `Action(...)`. Служебные имена `type`, `output` и `next`
зарезервированы Story Engine, остальные named arguments попадают в
`ActionStartRequest.arguments()` / `ActionRecoveryRequest.arguments()` как
`Map<String, String>`.

Пример:

```echoscript
Action(
    "spawn_father",
    type: "echo_npc:spawn",
    actorId: "echo_story_npc_test:john_father",
    worldId: "minecraft:overworld",
    x: "1.5",
    y: 64,
    z: "0",
    yaw: "0",
    pitch: "0",
    next: "finish");
```

EchoScript V1 поддерживает `string`, `integer` и `boolean` literals; дробные
числа нужно author-ить строкой. Вложенные calls/arrays/objects в action payload
не принимаются.

`story-extension-forge-1.20.1` также публикует `EchoStoryExecutionBridge`. Он
нужен для test/acceptance Forge mods, которым требуется запустить реальное
Story execution через public surface. `lookup(server)` возвращает только
server-scoped starter с методом `start(StoryId, ExecutionId)`. Bridge не
экспортирует `ForgeServerHost`, repository, runtime implementation или mutable
Story state.
