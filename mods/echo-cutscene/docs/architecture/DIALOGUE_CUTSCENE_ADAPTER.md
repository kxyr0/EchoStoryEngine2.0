# Dialogue cutscene adapter

End-frame hold:

- `Cutscene.Start(id, holdLastFrame: true)` holds the final camera frame until `Cutscene.Stop(id)`;
- `hold: true` is accepted as a short alias.

Adapter находится в отдельном проекте:

```text
mods/echo-dialogue-cutscene-adapter
```

Он не импортирует implementation-классы Echo Dialogue или Echo Cutscene. Используются только public API:

- `DialogueExternalWaitDispatchEvent`;
- `EchoDialogueServiceLookupEvent`;
- `CutsceneServiceLookupEvent`;
- `CutsceneLifecycleEvent`;
- `DialogueService`;
- `CutsceneService`.

Семантика:

- `Cutscene.Start(id)` запускает cutscene и сразу резюмирует диалог;
- `Cutscene.Play(id)` запускает cutscene и резюмирует диалог после завершения;
- `Cutscene.Wait(id)` ждет уже активную cutscene;
- `Cutscene.Stop(id)` останавливает cutscene и сразу резюмирует диалог.

При terminal/cancel диалога pending wait снимается, а adapter отправляет `CutsceneService.stop(...)`.
