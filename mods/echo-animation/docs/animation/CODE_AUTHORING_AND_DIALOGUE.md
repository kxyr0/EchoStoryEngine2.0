# Animation Code Authoring And Dialogue

Этот документ про кодовый путь Echo Animation, который важен при настройке
лица, портрета и вызова face-анимаций из Echo Dialogue.

## Быстрый Вызов Из Диалога

Для face-анимации в строке диалога используй `.SetAnimationFace(...)`:

```csharp
private FaceAnimationId FatherLookLeft = "jdh:face_test/look_left";

private NodeDef Greeting()
{
    var node = new Node("greeting");
    Line("father", "Ты поздно вернулся.").SetAnimationFace(FatherLookLeft);
    Line("father", "Скажи прямо, где ты был?");
    return node;
}
```

Удалить анимацию так же просто: убери `.SetAnimationFace(...)` с этой строки.
Когда следующая реплика того же портрета приходит без `faceAnimation` cue,
клиентский portrait-state сбрасывает прошлую line-анимацию.

Прямой string тоже читается, но typed field лучше:

```csharp
Line("father", "Смотри на меня.").SetAnimationFace("jdh:face_test/look_left");
```

## Поток Данных

Путь от EchoScript до пикселей портрета сейчас такой:

1. `NodeLineReader` в `dialogue-echoscript-adapter` читает
   `.SetAnimationFace(FaceAnimationId)` и добавляет
   `CompiledDialogueLine.Cue("faceAnimation", value)`.
2. `DialoguePresentationFactory` копирует line cues в
   `DialoguePresentation.Cue`.
3. `DialogueOpenMessage` кодирует cues в network packet protocol `4`.
4. `DialogueVisualFaceLayerBridge` в `echo-animation` слушает
   `ClientboundDialogueOpenEvent` через reflection.
5. `DialoguePortraitAnimationState` сохраняет активный `faceAnimation`
   по `visualId`.
6. `DialogueVisualFaceLayerRenderer` на каждый render кадр берет pose из
   `DialoguePortraitAnimationState`, потом накладывает idle blink через
   `IdleBlinkPose`, затем рисует face layers поверх портрета.

Главное: диалог не знает, как рисовать глаза. Он только передает cue. Рисует
и семплит клип `echo-animation`.

## Условия Для Портретной Анимации

Портретная face-анимация сработает, если совпадают три id:

- `Alias(... visual: Portrait("jdh:john_father"))` или
  `SkinHead("jdh:john_father")` дает visual id;
- `.face-rig.es` имеет `face JohnFather("jdh:john_father")`;
- `.face-animation.es` либо не указывает `rig`, либо указывает
  `rig: "jdh:john_father"`.

Если visual id не равен face rig id, базовая картинка портрета появится, но
animated face layers не наложатся. Это сделано намеренно: renderer ищет rig
через `FaceRigId.parse(visualId)`.

## Content Player Portrait

`echo-content-player` теперь регистрирует optional provider для Echo Dialogue,
когда `echo_dialogue` загружен.

Кодовый путь:

- `EchoContentPlayerMod` проверяет `ModList.get().isLoaded("echo_dialogue")`;
- `PlayerDialoguePresentationRegistration` подписывается на
  `EchoDialogueSpeakerPresentationRegistrationEvent`;
- `PlayerSpeakerPresentationSession` распознает speaker id текущего игрока:
  `jdh:john`, `jdh:player`, `echo_content_player:player`,
  `echo_content_player:self`;
- audience id диалога читается как UUID аккаунта;
- `PlayerProfileQueryService.findByAccountUuid(...)` возвращает профиль;
- `PlayerSpeakerPresentation` отдает display name профиля и
  `SpeakerVisualRef.skinHead(profile.appearance().skinId())`.

Клиентский resolver Echo Dialogue ищет `SKIN_HEAD` теперь в трех местах:

- `assets/<ns>/textures/echo_dialogue/skins/<path>.png`;
- `assets/<ns>/textures/echo_npc/skins/<path>.png`;
- `assets/<ns>/textures/echo_player/skins/<path>.png`.

Поэтому `SkinHead("echo_content_player:player/base")` попадает в
`assets/echo_content_player/textures/echo_player/skins/player/base.png`.

## Idle Blink В Портрете

Idle blink для портрета настраивается в `.portrait-overlay.es`, секция `idle`:

```csharp
idle: [
    Blink "eye_closure"
    {
        Travel(28, -40);
        Gaze(1, 1);
        Idle(true);
        PeriodTicks(30);
        CloseTicks(2);
        HoldTicks(2);
        OpenTicks(2);
    }
]
```

Это не отдельная face-анимация. Это постоянная клиентская поза поверх base pose
или line pose. Код: `PortraitOverlayConfigParser` читает `Blink`, а
`IdleBlinkPose.apply(...)` добавляет `blinkUpperTravel` и `blinkLowerTravel`
к текущему `BonePose`.

`Travel(35, -35)` значит примерно `+0.035` для upper bone и `-0.035` для lower
bone: числа делятся на 1000 так же, как в face rig controls. Если верхние веки
в портрете закрывают глаз слишком низко, обычно правятся два места:

- `Travel(upper, lower)` в `idle` для силы движения;
- `Layer("upper_eyelid_left/right", placeY: "...", scaleY: "...")`
  в overlay для базовой посадки и толщины.

## Face Layers И Overlay

`DialogueVisualFaceLayerRenderer` рисует не bones напрямую, а face layers.
Важно не путать ids:

- animation tracks работают с bone id: `left_pupil`, `right_pupil`,
  `upper_eyelids`, `lower_eyelids`;
- portrait overlay работает с layer id: `pupil_left`, `pupil_right`,
  `upper_eyelid_left`, `upper_eyelid_right`.

Это причина, почему `Layer("upper_eyelids", ...)` не менял размер в портрете:
такого layer id нет. Нужны конкретные left/right layer ids.

Overlay placement специально сохраняет animated delta:

- neutral placement берется из rig;
- overlay `placeX/placeY/scaleX/scaleY` задает портретную базу;
- разница между animated pose и neutral pose добавляется поверх overlay.

Кодовая точка: `DialogueVisualFaceLayerRenderer.overridePlacement(...)`.

## Facial Animation Clips

Старые `.face-animation.es` клипы сейчас нужны именно для
`.SetAnimationFace(...)`:

```csharp
return FacialAnimation("jdh:face_test/look_left", rig: "jdh:john_father",
    durationMillis: 900, end: "HOLD",
    tracks: [
        Track("left_pupil", frames: [
            Frame(0, x: 0),
            Frame(450, x: "-0.025"),
            Frame(900, x: "-0.025")
        ])
    ]);
```

`DialoguePortraitAnimationState` семплит их через `ClipSampler`.

`end` влияет на поведение после `durationMillis`:

- `HOLD` оставляет последний кадр, удобно для взгляда в сторону;
- `LOOP` повторяет клип;
- `RETURN_TO_BASE` возвращает pose к нулю после конца.

## Универсальные Face-Анимации

Не нужно создавать отдельный `.face-animation.es` под каждого персонажа. Если
анимация должна работать на любом лице с теми же bone ids, не указывай `rig`:

```csharp
return FacialAnimation("jdh:face_test/look_left",
    durationMillis: 900, end: "HOLD",
    tracks: [
        Track("left_pupil", frames: [
            Frame(0, x: 0),
            Frame(450, x: "-0.025"),
            Frame(900, x: "-0.025")
        ]),
        Track("right_pupil", frames: [
            Frame(0, x: 0),
            Frame(450, x: "-0.025"),
            Frame(900, x: "-0.025")
        ])
    ]);
```

Такой клип применяется и к `jdh:john_father`, и к
`echo_content_player:player/base`, если в обоих rigs есть bones
`left_pupil` и `right_pupil`. В диалоге вызывается один и тот же id:

```csharp
private FaceAnimationId LookLeft = "jdh:face_test/look_left";

Line("father", "Ты поздно вернулся.").SetAnimationFace(LookLeft);
Line("john", "Я был у старого моста.").SetAnimationFace(LookLeft);
```

`rig: "..."` нужен только когда клип специально сделан под один rig и должен
быть заблокирован для остальных.

Для обычного моргания в строке диалога лучше делать клип с закрытием и
открытием и `RETURN_TO_BASE`. Для постоянного idle blink лучше использовать
`idle: [ Blink ... ]` в portrait overlay.

## Где Править Код

Частые задачи и места:

- добавить новый line cue syntax:
  `dialogue-echoscript-adapter/.../NodeLineReader`;
- пронести cue до клиента:
  `DialoguePresentation`, `DialoguePresentationFactory`,
  `DialogueOpenMessage`;
- поменять, какие speaker ids считаются текущим игроком:
  `PlayerSpeakerPresentationSession.isAudiencePlayerSpeaker`;
- поменять путь поиска player skin:
  `ClientSpeakerVisualResolver.textureLocations`;
- поменять, как line face animation живет в портрете:
  `DialoguePortraitAnimationState`;
- поменять порядок наложения line pose и idle blink:
  `DialogueVisualFaceLayerRenderer.renderRig`;
- править размеры/посадку конкретного портрета без Java:
  `story/configs/<ns>/echo_animation/portrait_overlays/*.portrait-overlay.es`.

## Что Уже Упрощено

- Для диалога больше не нужен отдельный story `Action(...)`, если нужно только
  лицо в портрете: используй `.SetAnimationFace(...)` прямо на `Line`.
- Idle blink включается/выключается в overlay одной секцией `idle`.
- Player portrait для `jdh:john` может прийти из `echo-content-player` через
  provider, без прописывания конкретного имени игрока в dialogue resource.
- Один и тот же `.face-animation.es` клип может работать и для in-world face,
  и для портрета, если tracks используют те же bone ids.

## Ограничения

- `.SetAnimationFace(...)` сейчас визуально применяется к портрету на клиенте.
  Серверный playback subject для NPC этим кодом не создается.
- `.SetAnimation(...)` для body animation тоже переносится как cue, но
  отдельный автоматический body playback bridge еще не добавлен.
- Account skin без authored resource id не превращается в `SkinHead` сам по
  себе. Текущий `echo-content-player` нормализует базовый resource skin, поэтому
  стандартный portrait path работает.
