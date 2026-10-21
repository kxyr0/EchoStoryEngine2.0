# Cutscenes from Dialogue

End-frame hold:

```csharp
Cutscene.Start(BedtimeStory, holdLastFrame: true);
Cutscene.Stop(BedtimeStory);
```

`holdLastFrame: true` keeps the camera on the final frame until the later stop command.
`hold: true` is accepted as a short alias.

Добавь adapter:

```text
mods/echo-dialogue-cutscene-adapter
```

Пример node:

```csharp
private CutsceneId BedtimeStory = "jdh:chapter_1/bedtime_story";

private void Bedtime()
{
    Line("father", "Listen.");
    Cutscene.Play(BedtimeStory);
    Line("father", "Now sleep.");
}
```

`Cutscene.Play` остановит диалог на durable external wait, запустит camera playback, а после
`CutsceneLifecycleEvent` completed/stopped вызовет `DialogueService.resumeExternal(...)`.

Если диалог отменили раньше, adapter снимает pending wait и отправляет `CutsceneService.stop(...)`.
