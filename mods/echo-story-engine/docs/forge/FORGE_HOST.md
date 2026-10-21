# Forge Host

## Назначение

Forge host - единственный production слой, который связывает Minecraft/Forge lifecycle с нейтральным Story Engine
runtime. Он не добавляет gameplay-семантику: host загружает StoryResource, запускает story execution, сохраняет
checkpoints через порт и публикует diagnostics.

## Lifecycle

- `EchoStoryEngineMod` остается тонким `@Mod` entrypoint и регистрирует composition root.
- `ForgeStoryRuntimeCoordinator` подписывается на Forge events:
  - `AddReloadListenerEvent` для `.es` resource reload;
  - `RegisterCommandsEvent` для `/echo_story`;
  - `ServerStartedEvent` для создания `ForgeServerHost`;
  - `ServerStoppingEvent` и `ServerStoppedEvent` для остановки и сброса host;
  - `TickEvent.ServerTickEvent` на фазе `END` для drain persistence callbacks.
- `ForgeServerHost` существует для одного active `MinecraftServer`.
- Host states: `NOT_STARTED`, `STARTING`, `ACTIVE`, `STOPPING`, `STOPPED`, `FAILED`.

## Threading

Story state, catalog reference updates и command dispatch происходят на server thread. Background persistence worker
не вызывает Minecraft APIs и не мутирует Story state напрямую. Worker возвращает immutable completion callbacks;
server tick drains до `32` callbacks за tick.

Persistence queue limits:

- pending operation capacity: `64`;
- per-tick drain budget: `32`;
- shutdown timeout: `10s`.

Если очередь заполнена или закрыта, host emits `ESE-FORGE-PERSISTENCE-001` и не dispatch external commands.

## Persistence root

Forge привязывает durable execution checkpoints к active world:

```text
<world>/data/echo_story_engine/executions
```

Путь выводится из `MinecraftServer.getWorldPath(LevelResource.ROOT)`. Lower modules получают только
`ExecutionStateRepository`; `Path` не проходит в contracts/core APIs.

## External actions

M4 не реализует production action registry или third-party handler discovery. Saved external command без production
handler диагностируется как `ESE-FORGE-ACTION-001`. M5 остается границей для public extension contracts.
