# Cutscene EchoScript reference

Dialogue EchoScript adapter понимает следующие calls внутри node:

```csharp
Cutscene.Start(BedtimeStory);
Cutscene.Start(BedtimeStory, holdLastFrame: true);
Cutscene.Play("jdh:chapter_1/bedtime_story");
Cutscene.Wait(id: "jdh:chapter_1/bedtime_story");
Cutscene.Stop(BedtimeStory);
```

`holdLastFrame: true` keeps the camera on the final frame and keeps the cutscene active until
`Cutscene.Stop(id)` runs. `hold: true` is accepted as a short alias.

`BedtimeStory` может быть полем со строковым initializer:

```csharp
private CutsceneId BedtimeStory = "jdh:chapter_1/bedtime_story";
```

Компиляция превращает calls в external commands:

- `echo_cutscene:start`;
- `echo_cutscene:play`;
- `echo_cutscene:wait`;
- `echo_cutscene:stop`.

Команда должна стоять перед следующей `Line`, потому что Dialogue external wait продолжает node с этой позиции.
