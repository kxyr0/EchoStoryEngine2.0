# Cutscene persistence

Core содержит DTO для будущего durable persistence:

- `CutscenePlaybackCheckpoint`;
- `CutscenePersistenceDto`.

Forge runtime версии `0.1.0` уже отделяет server playback id, owner id и command idempotency. Это позволяет позже
сохранять активные playback-и и восстанавливать их после reload/server restart без изменения public API.

Файловые camera tracks читаются из world directory:

```text
<world>/echo/cutscenes/*.camera.es
```

Reload выполняется командой `/echo_cutscene reload`.
