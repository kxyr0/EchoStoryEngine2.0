# Creating A Forge Adapter

Forge adapter mod подписывается на `EchoStoryExtensionRegistrationEvent` и регистрирует
`ForgeStoryExtensionFactory`.

Минимальный lifecycle:

1. В `@Mod` constructor подписаться на `MinecraftForge.EVENT_BUS`.
2. На registration event вызвать `event.registrar().register(factory)`.
3. Factory возвращает stable `StoryExtensionDescriptor`.
4. На server host start factory создаёт один `StoryExtensionSession`.
5. Session возвращает handlers для declared action types.
6. Handler завершает action через `ActionCompletionSink`.

Adapter должен зависеть от `echo_story_engine` в `mods.toml`. Для isolated integration build этот репозиторий
публикует public artifacts в `build/integration-maven`, а пример внешнего adapter build лежит в:

```text
integration-tests/echo-story-extension-test-mod
```

Этот build не входит в root `settings.gradle` и не использует project dependencies на engine modules.

Если test-only adapter или acceptance mod должен стартовать Story execution,
используйте `EchoStoryExecutionBridge.lookup(server)`. Этот public Forge bridge
возвращает bounded `StoryExecutionStarter` для `StoryId` + `ExecutionId` и не
даёт доступа к implementation host.
