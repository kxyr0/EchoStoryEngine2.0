# Sleep Story Context

`StartStory(...)` наследует sleep context автоматически через adapter.

В Story actions доступны ключи:

- `player` / `sleep.player`
- `scenario` / `sleep.scenario`
- `sleep.session`
- `sleep.count`
- `sleep.originDimension`
- `sleep.bedPosition`
- `sleep.bedBlock`
- `sleep.bedProvider`
- `sleep.revision`
- `trigger.subscription`
- `trigger.execution`
- `story.execution`

`Sleep.CloseEyes`, `Sleep.EnterDream`, `Sleep.ReturnToBed`, `Sleep.CompleteNight`, `Sleep.OpenEyes` и `Sleep.Complete`
используют этот context как fallback. Если context отсутствует и payload не содержит player/session данных,
возвращается `PLAYER_SLEEP_CONTEXT_MISSING`.
