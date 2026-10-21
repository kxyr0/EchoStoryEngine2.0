# Sleep Context Inheritance

Context inheritance:

```text
Sleep event
-> Trigger execution context
-> Story execution context
-> implicit current SleepSession
```

Adapter устанавливает context через `PlayerSleepStoryContextBridge` перед запуском Story. Story `Sleep.*` actions
мерджат explicit payload поверх inherited context, поэтому явные аргументы сохраняют приоритет.

Запрещено выбирать глобально любую active session. Если context отсутствует, команда должна диагностировать
`PLAYER_SLEEP_CONTEXT_MISSING`.
