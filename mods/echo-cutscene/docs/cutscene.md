# Echo Cutscene: справочник для Dialogue и Story

Документ для авторов катсцен, диалогов и story-сцен. Фокус только на Echo Cutscene: камера, detached playback, блокировка gameplay input, запуск из Dialogue и прямой вызов из Java.

Echo Cutscene не двигает настоящее тело игрока как актера. За тело отвечает Echo Player Actor. Echo Cutscene отвечает за то, что игрок видит камерой во время сцены, когда его обычный ввод должен быть заблокирован, а Dialogue при этом может продолжать показывать реплики и выборы.

## Что делает Echo Cutscene

Echo Cutscene делает это:

- загружает `.camera.es` треки из мира;
- создает `CutsceneDefinition` для каждого трека;
- запускает detached camera playback для конкретного `ServerPlayer`;
- блокирует обычный gameplay input на клиенте;
- держит настоящее тело игрока на сервере в исходном состоянии через body lock;
- отправляет lifecycle-события Forge;
- дает Dialogue-командам `Cutscene.Start`, `Cutscene.Play`, `Cutscene.Wait`, `Cutscene.Stop` управлять playback.

Echo Cutscene не делает это:

- не создает отдельного fake player;
- не переводит игрока в spectator;
- не пишет полноценную режиссуру NPC;
- не запускает Dialogue сам по себе;
- не управляет лицом, одеждой, portrait-ами или анимациями;
- не двигает тело игрока по маршруту;
- не загружает отдельные `.cutscene.es` definition-файлы.

## Главная схема

Практический путь для story такой:

```text
StoryResource .es
  -> Action(... type: "echo_dialogue:play" ...)
  -> Echo Dialogue открывает DialogueResource
  -> Dialogue node вызывает Cutscene.*
  -> EchoScript compiler превращает Cutscene.* в echo_cutscene:* external wait
  -> echo-dialogue-cutscene-adapter получает external wait
  -> CutsceneService.start/stop/isPlaying
  -> Echo Cutscene запускает detached camera
  -> клиент блокирует gameplay input, но Dialogue screen остается рабочим
  -> CutsceneLifecycleEvent возвращает ожидания Dialogue
  -> Dialogue продолжается
  -> Story получает outcome диалога
```

Для автора контента это обычно выглядит так:

```text
1. Написать или записать camera track:
   <world>/echo/cutscenes/chapter_1/home/arrival.camera.es

2. Перезагрузить треки:
   /echo_cutscene reload

3. Проверить playback:
   /echo_cutscene play jdh:chapter_1/home/arrival

4. Использовать тот же id в Dialogue:
   Cutscene.Play("jdh:chapter_1/home/arrival");

5. Если Dialogue запускается из Story:
   Action(... type: "echo_dialogue:play", dialogueId: "...", audienceId: "...");
```

## Модули, которые участвуют

Минимальный набор для катсцен:

```text
mods/echo-cutscene
```

Для катсцен из диалогов:

```text
mods/echo-dialogue
mods/echo-dialogue-cutscene-adapter
mods/echo-cutscene
```

Для запуска диалога из story:

```text
mods/echo-story-engine
mods/echo-story-dialogue-adapter
mods/echo-dialogue
mods/echo-dialogue-cutscene-adapter
mods/echo-cutscene
```

Если сцена еще и двигает тело игрока:

```text
mods/echo-player-actor
mods/echo-cutscene-player-actor-adapter
```

## Где лежат файлы

Camera tracks сейчас читаются из папки мира, а не из `data/<namespace>`:

```text
<world>/echo/cutscenes/**/*.camera.es
```

Примеры:

```text
<world>/echo/cutscenes/intro.camera.es
<world>/echo/cutscenes/chapter_1/wake_up.camera.es
<world>/echo/cutscenes/chapter_1/home/arrival.camera.es
```

Текущий loader превращает относительный путь файла в `CutsceneId` с namespace `jdh`.

```text
<world>/echo/cutscenes/intro.camera.es
  -> jdh:intro

<world>/echo/cutscenes/chapter_1/wake_up.camera.es
  -> jdh:chapter_1/wake_up

<world>/echo/cutscenes/chapter_1/home/arrival.camera.es
  -> jdh:chapter_1/home/arrival
```

`CameraTrackId` создается автоматически:

```text
jdh:chapter_1/home/arrival
  -> jdh:chapter_1/home/arrival_camera
```

Dialogue resources лежат в content pack:

```text
data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es
```

Story resources лежат в content pack:

```text
data/<namespace>/echo_story/stories/**/*.es
```

## Минимальный camera track

Файл:

```text
<world>/echo/cutscenes/chapter_1/home/arrival.camera.es
```

Полный код:

```text
# Минимальный legacy-track.
# Формат: key <time_ms> <x> <y> <z> <yaw> <pitch> <fov>

key 0 0 65 0 0 0 70
key 3000 3 65 2 90 0 70
key 6000 0 66 4 180 0 70
```

Что значит каждая строка:

- `key` создает keyframe камеры.
- `0`, `3000`, `6000` это время в миллисекундах от начала катсцены.
- `x y z` это позиция камеры в мире.
- `yaw` это поворот по горизонтали.
- `pitch` это наклон вверх/вниз.
- `fov` это field of view в градусах.
- Если в файле только `key`, трек считается schema 1 и работает как `FREE`.

Минимальные требования:

- нужно минимум два `key`;
- значения должны быть конечными числами;
- итоговые `key` после сортировки должны идти по строго возрастающему времени;
- `CutsceneId` должен быть namespaced id вида `namespace:path`, но файловый loader сейчас сам делает `jdh:<path>`.

Проверка в игре:

```text
/echo_cutscene reload
/echo_cutscene play jdh:chapter_1/home/arrival
/echo_cutscene status
/echo_cutscene stop
```

## Camera track schema 2

Schema 2 добавляет смысловые строки: режим камеры, цель, framing.

Файл:

```text
<world>/echo/cutscenes/chapter_1/home/father_close.camera.es
```

Полный код:

```text
# Камера едет слева направо и всегда смотрит в глаза игрока.

schema 2
mode 0 LOOK_AT_WITH_OFFSET
target 0 PLAYER EYES LIVE
framing 0 0.15 -0.1

key 0 10 65 10 0 0 60
key 1200 11.5 65.4 10.7 0 0 58
key 2400 13 65.6 11.5 0 0 55
```

Что здесь важно:

- `schema 2` явно включает современный формат.
- `mode 0 LOOK_AT_WITH_OFFSET` говорит камере вычислять yaw/pitch по target-у.
- `target 0 PLAYER EYES LIVE` значит, что камера смотрит на живую позицию глаз игрока.
- `framing 0 0.15 -0.1` смещает цель в кадре: чуть вправо и чуть ниже.
- В `LOOK_AT` и `LOOK_AT_WITH_OFFSET` yaw/pitch в строках `key` уже не главная режиссура, потому что направление пересчитывается на target.

## Быстрый smoke test

Сначала сделайте файл:

```text
<world>/echo/cutscenes/test/three_points.camera.es
```

Код:

```text
schema 2
mode 0 FREE

key 0 0 66 0 0 0 70
key 1000 2 66 0 45 0 70
key 2000 2 67 2 90 -5 65
```

Потом в игре:

```text
/echo_cutscene reload
/echo_cutscene play jdh:test/three_points
/echo_cutscene status
```

Ожидаемый результат:

- камера отрывается от тела;
- тело игрока остается в мире;
- обычные WASD/use/attack/hotbar не управляют gameplay;
- после последнего keyframe playback завершается автоматически;
- `/echo_cutscene status` во время playback показывает id и elapsed.

## Команды Echo Cutscene

Обычный playback:

```text
/echo_cutscene play <id>
/echo_cutscene play <id> hold
/echo_cutscene stop
/echo_cutscene stop <id>
/echo_cutscene status
/echo_cutscene reload
```

Примеры:

```text
/echo_cutscene play jdh:chapter_1/bedtime_story
/echo_cutscene play jdh:chapter_1/home/father_close hold
/echo_cutscene stop jdh:chapter_1/home/father_close
/echo_cutscene stop
/echo_cutscene status
/echo_cutscene reload
```

Что делает `hold`:

- playback доходит до последнего frame;
- камера остается на последнем frame;
- катсцена считается активной;
- ее нужно остановить через `/echo_cutscene stop` или `Cutscene.Stop(...)`.

Authoring:

```text
/echo_cutscene enter <id>
/echo_cutscene enter <id> <speed>
/echo_cutscene mode <FREE|LOOK_AT|LOOK_AT_WITH_OFFSET>
/echo_cutscene target clear
/echo_cutscene target player
/echo_cutscene target player <FEET|CHEST|HEAD|EYES|CUSTOM>
/echo_cutscene target fixed <x> <y> <z>
/echo_cutscene exit
/echo_cutscene author status
```

Примеры:

```text
/echo_cutscene enter jdh:chapter_1/home/arrival 0.45
/echo_cutscene mode LOOK_AT_WITH_OFFSET
/echo_cutscene target player EYES
/echo_cutscene target fixed 12 65 8
/echo_cutscene target clear
/echo_cutscene exit
```

Echo Debug также прокидывает похожие команды:

```text
/echo_debug cutscene enter jdh:story_telling_sister_1 0.45
/echo_debug cutscene mode LOOK_AT
/echo_debug cutscene mode LOOK_AT_WITH_OFFSET
/echo_debug cutscene target player EYES
/echo_debug cutscene target player HEAD
/echo_debug cutscene target fixed <x> <y> <z>
/echo_debug cutscene target clear
```

Клавиши authoring:

```text
F4 - start/stop recording
F6 - pitch-flight toggle
F7 - поставить FIXED_POINT target из raycast detached camera
key.echo_debug.cutscene_camera_mode - смена режима камеры, по умолчанию не назначена
```

## Reference: строки .camera.es

Общие правила:

- пустые строки игнорируются;
- строки с `#` в начале игнорируются;
- все значения разделяются пробелами;
- неизвестная команда строки вызывает ошибку загрузки;
- parser сортирует `key`, `mode`, `target`, `framing` по времени;
- после binding keyframe timestamps должны быть strictly increasing.

### schema

Формат:

```text
schema <version>
```

Пример:

```text
schema 2
```

Что значит:

- `schema 1` это legacy key-only track;
- `schema 2` это track с semantic camera mode, target и framing;
- если есть `mode`, `target` или `framing`, parser сам считает track schema 2 даже без строки `schema 2`.

### key

Формат:

```text
key <time_ms> <x> <y> <z> <yaw> <pitch> <fov>
```

Пример:

```text
key 1500 12.5 65.25 -4.75 90 -8 60
```

Что значит:

- `time_ms` время от старта playback;
- `x y z` позиция detached camera;
- `yaw pitch` направление камеры в `FREE` mode;
- `fov` угол обзора;
- interpolation сейчас создается как `CameraInterpolation.SMOOTH`.

Полезные FOV ориентиры:

```text
key 0 0 65 0 0 0 70    # обычный обзор
key 0 0 65 0 0 0 55    # ближе, портретнее
key 0 0 65 0 0 0 85    # шире, больше пространства
```

### mode

Формат:

```text
mode <time_ms> <mode>
```

Доступные режимы:

```text
FREE
LOOK_AT
LOOK_AT_WITH_OFFSET
```

Примеры:

```text
mode 0 FREE
mode 1200 LOOK_AT
mode 3200 LOOK_AT_WITH_OFFSET
```

Что значит:

- `FREE`: камера использует authored yaw/pitch из `key`.
- `LOOK_AT`: позиция камеры authored, но yaw/pitch считаются по target.
- `LOOK_AT_WITH_OFFSET`: камера смотрит на target и применяет screen-space framing.

### target

Формат `NONE`:

```text
target <time_ms> NONE
```

Формат `FIXED_POINT`:

```text
target <time_ms> FIXED_POINT <x> <y> <z>
```

Формат `PLAYER`:

```text
target <time_ms> PLAYER <anchor> <binding>
```

Формат `NPC`:

```text
target <time_ms> NPC <id> <anchor> <binding>
```

Формат `BETWEEN`:

```text
target <time_ms> BETWEEN <first> <second> <weight>
```

Примеры:

```text
target 0 NONE
target 0 FIXED_POINT 18 65 22
target 0 PLAYER EYES LIVE
target 0 PLAYER HEAD LIVE
target 0 NPC jdh:john_father EYES LIVE
target 0 NPC jdh:john_father CHEST RECORDED
target 0 BETWEEN jdh:john jdh:john_father 0.5
```

`anchor`:

```text
FEET
CHEST
HEAD
EYES
CUSTOM
```

`binding`:

```text
LIVE
RECORDED
```

Практический смысл:

- `PLAYER EYES LIVE` хорошо для shot-а, который держит взгляд на игроке.
- `NPC <id> EYES LIVE` хорошо для реплики NPC.
- `NPC <id> CHEST LIVE` спокойнее для medium shot.
- `FIXED_POINT` хорошо для предмета, двери, окна, следа, алтаря.
- `BETWEEN first second 0.5` хорошо для кадра на двух участников разговора.

### framing

Формат:

```text
framing <time_ms> <horizontal> <vertical>
```

Примеры:

```text
framing 0 0 0
framing 0 0.15 -0.1
framing 1200 -0.2 0.05
```

Практический смысл:

- `horizontal > 0` двигает цель в одну сторону кадра;
- `horizontal < 0` двигает цель в другую сторону;
- `vertical > 0` поднимает framing;
- `vertical < 0` опускает framing;
- framing особенно полезен в `LOOK_AT_WITH_OFFSET`.

## Полный camera track: диалог у двери

Файл:

```text
<world>/echo/cutscenes/chapter_1/home/door_talk.camera.es
```

Полный код:

```text
schema 2

# Начинаем как общий establishing shot.
mode 0 LOOK_AT_WITH_OFFSET
target 0 BETWEEN jdh:john jdh:john_father 0.5
framing 0 0 -0.05
key 0 7 65 12 0 0 70
key 1200 7.4 65.2 11.4 0 0 68

# Переходим на отца.
target 1800 NPC jdh:john_father EYES LIVE
framing 1800 -0.18 -0.04
key 2400 8.5 65.3 10.3 0 0 60
key 3600 8.8 65.3 9.9 0 0 58

# Возвращаемся к игроку.
target 4200 PLAYER EYES LIVE
framing 4200 0.16 -0.06
key 4800 6.2 65.25 10.1 0 0 60
key 6000 5.8 65.25 9.8 0 0 58
```

Проверка:

```text
/echo_cutscene reload
/echo_cutscene play jdh:chapter_1/home/door_talk
```

Почему это удобно:

- target можно менять в середине track-а;
- `key` отвечает за траекторию камеры;
- `target` отвечает за то, куда камера смотрит;
- `framing` отвечает за композицию лица/объекта в кадре.

## Dialogue: минимальный запуск катсцены

Файл:

```text
data/jdh/echo_dialogue/dialogues/chapter_1/home/door_talk.dialogue.es
```

Полный код:

```csharp
namespace JDH.Chapter1.Home;

class DoorTalkDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ StartScene() ];
    private CutsceneId DoorTalk = "jdh:chapter_1/home/door_talk";

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:chapter_1/home/door_talk");
    }

    private void Init()
    {
        Id("jdh:chapter_1/home/door_talk");
        Start("start_scene");
        Alias("father", "jdh:john_father", display: "Отец", visual: Portrait("jdh:john_father"));
        Alias("john", "jdh:john", display: "Джон", visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef StartScene()
    {
        var node = new Node("start_scene");

        Cutscene.Play(DoorTalk);
        Line("father", "Стой. Сначала скажи, где ты был.");

        Line("john", "У старого моста.");
        Line("father", "Один?");
        Line("john", "Да.");

        Complete("jdh:door_talk_done");
        return node;
    }
}
```

Что происходит:

- `Cutscene.Play(DoorTalk)` запускает катсцену и ставит Dialogue на ожидание.
- Dialogue продолжит выполнение только после завершения playback.
- Следующая `Line(...)` обязательна, потому что external command должен иметь строку после себя в том же node.
- `DoorTalk` это поле со строкой id. Compiler подставит `"jdh:chapter_1/home/door_talk"`.

Во что это компилируется на уровне external wait:

```text
Cutscene.Play(DoorTalk)
  -> type: echo_cutscene:play
  -> payload: cutscene=jdh:chapter_1/home/door_talk
```

## Dialogue: Start, потом Wait

`Cutscene.Start` запускает катсцену и сразу продолжает Dialogue. Это нужно, когда реплики должны идти поверх движения камеры.

Полный код:

```csharp
namespace JDH.Chapter1.Home;

class ArrivalDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ Arrival() ];
    private CutsceneId ArrivalCamera = "jdh:chapter_1/home/arrival";

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:chapter_1/home/arrival");
    }

    private void Init()
    {
        Id("jdh:chapter_1/home/arrival");
        Start("arrival");
        Alias("father", "jdh:john_father", display: "Отец", visual: Portrait("jdh:john_father"));
        Alias("john", "jdh:john", display: "Джон", visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef Arrival()
    {
        var node = new Node("arrival");

        Cutscene.Start(ArrivalCamera);
        Line("father", "Ты поздно вернулся.");

        Line("john", "Я знаю.");
        Line("father", "Я ждал почти весь вечер.");

        Cutscene.Wait(ArrivalCamera);
        Line("father", "Теперь говори спокойно.");

        Complete("jdh:arrival_done");
        return node;
    }
}
```

Что происходит:

- `Start` начинает playback и сразу делает `resumeExternal`.
- Первые реплики идут, пока камера еще движется.
- `Wait` проверяет, идет ли эта катсцена.
- Если идет, Dialogue ждет lifecycle завершения.
- Если уже закончилась, Dialogue продолжает сразу.

Такой стиль удобен для длинных establishing shots.

## Dialogue: hold last frame и выбор

Для сцены, где камера должна замереть на последнем кадре, используйте `Cutscene.Start(..., holdLastFrame: true)`, а потом `Cutscene.Stop(...)` в ветках.

Файл camera:

```text
<world>/echo/cutscenes/chapter_1/home/father_question.camera.es
```

Код camera:

```text
schema 2
mode 0 LOOK_AT_WITH_OFFSET
target 0 NPC jdh:john_father EYES LIVE
framing 0 -0.12 -0.05

key 0 4 65 8 0 0 65
key 1600 4.4 65.2 7.5 0 0 58
key 2600 4.6 65.2 7.3 0 0 55
```

Файл Dialogue:

```text
data/jdh/echo_dialogue/dialogues/chapter_1/home/father_question.dialogue.es
```

Полный код:

```csharp
namespace JDH.Chapter1.Home;

class FatherQuestionDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ Question(), Truth(), Lie() ];
    private CutsceneId FatherQuestion = "jdh:chapter_1/home/father_question";

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:chapter_1/home/father_question");
    }

    private void Init()
    {
        Id("jdh:chapter_1/home/father_question");
        Start("question");
        Alias("father", "jdh:john_father", display: "Отец", visual: Portrait("jdh:john_father"));
        Alias("john", "jdh:john", display: "Джон", visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef Question()
    {
        var node = new Node("question");

        Cutscene.Start(FatherQuestion, holdLastFrame: true);
        Line("father", "Скажи прямо.");

        Line("father", "Где ты был?");
        Choice("truth", "Сказать правду.", next: "truth");
        Choice("lie", "Солгать.", next: "lie");
        return node;
    }

    private NodeDef Truth()
    {
        var node = new Node("truth");

        Cutscene.Stop(FatherQuestion);
        Line("john", "Я был у старого моста.");

        Line("father", "Один?");
        Line("john", "Да.");
        Complete("jdh:told_truth");
        return node;
    }

    private NodeDef Lie()
    {
        var node = new Node("lie");

        Cutscene.Stop(FatherQuestion);
        Line("john", "Я просто задержался во дворе.");

        Line("father", "Не во дворе.");
        Line("john", "Понял.");
        Complete("jdh:lied");
        return node;
    }
}
```

Почему здесь `Start`, а не `Play`:

- `Cutscene.Play(..., holdLastFrame: true)` стартует и ждет окончания.
- Но `holdLastFrame` оставляет катсцену активной на последнем кадре.
- Значит Dialogue не дойдет до следующих строк, пока кто-то внешний не вызовет stop.
- Если stop должен быть внутри этого же Dialogue, используйте `Start(..., holdLastFrame: true)`.

Короткая форма тоже работает:

```csharp
Cutscene.Start(FatherQuestion, hold: true);
Line("father", "Где ты был?");
```

## Dialogue: Stop active scene from another node

Можно держать одну камеру между несколькими nodes и останавливать ее в нужной ветке.

```csharp
private NodeDef Intro()
{
    var node = new Node("intro");

    Cutscene.Start("jdh:chapter_1/home/slow_push", holdLastFrame: true);
    Line("father", "Подойди ближе.");

    Choice("come", "Подойти.", next: "ComeCloser");
    Choice("stay", "Остаться на месте.", next: "StayThere");
    return node;
}

private NodeDef ComeCloser()
{
    var node = new Node("ComeCloser");

    Cutscene.Stop(id: "jdh:chapter_1/home/slow_push");
    Line("john", "Хорошо.");

    Complete("jdh:come_closer");
    return node;
}

private NodeDef StayThere()
{
    var node = new Node("StayThere");

    Cutscene.Stop(cutsceneId: "jdh:chapter_1/home/slow_push");
    Line("john", "Нет.");

    Complete("jdh:stay_there");
    return node;
}
```

`id:` и `cutsceneId:` оба превращаются в payload key `cutscene`.

## Dialogue command reference

### Cutscene.Start

Формы:

```csharp
Cutscene.Start("jdh:chapter_1/home/arrival");
Cutscene.Start(id: "jdh:chapter_1/home/arrival");
Cutscene.Start(cutsceneId: "jdh:chapter_1/home/arrival");
Cutscene.Start(ArrivalCamera);
Cutscene.Start(ArrivalCamera, holdLastFrame: true);
Cutscene.Start(ArrivalCamera, hold: true);
```

Компиляция:

```text
echo_cutscene:start
payload: cutscene=<id>, holdLastFrame=<bool optional>, hold=<bool optional>
```

Поведение runtime adapter:

- читает `cutscene` из payload;
- вызывает `CutsceneService.start(player, id, commandId, endBehavior)`;
- сразу вызывает `DialogueService.resumeExternal(...)`;
- не ждет завершения playback.

Когда использовать:

- реплики должны идти поверх движения камеры;
- нужна заморозка на последнем кадре и дальнейший выбор;
- следующий node сам решит, когда вызвать `Cutscene.Stop`.

### Cutscene.Play

Формы:

```csharp
Cutscene.Play("jdh:chapter_1/home/arrival");
Cutscene.Play(id: "jdh:chapter_1/home/arrival");
Cutscene.Play(cutsceneId: "jdh:chapter_1/home/arrival");
Cutscene.Play(ArrivalCamera);
Cutscene.Play(ArrivalCamera, holdLastFrame: true);
```

Компиляция:

```text
echo_cutscene:play
payload: cutscene=<id>, holdLastFrame=<bool optional>, hold=<bool optional>
```

Поведение runtime adapter:

- запускает катсцену;
- затем делает то же, что `Cutscene.Wait`;
- Dialogue продолжится, когда `CutsceneService.isPlaying(player, id)` станет false.

Когда использовать:

- нужно показать законченный camera shot перед следующей репликой;
- игрок не должен читать реплики во время движения камеры;
- не нужен stop из следующих строк этого же node.

Осторожно:

```csharp
Cutscene.Play(ArrivalCamera, holdLastFrame: true);
Line("father", "Эта строка не появится, пока кто-то внешний не остановит катсцену.");
```

### Cutscene.Wait

Формы:

```csharp
Cutscene.Wait("jdh:chapter_1/home/arrival");
Cutscene.Wait(id: "jdh:chapter_1/home/arrival");
Cutscene.Wait(cutsceneId: "jdh:chapter_1/home/arrival");
Cutscene.Wait(ArrivalCamera);
```

Компиляция:

```text
echo_cutscene:wait
payload: cutscene=<id>
```

Поведение runtime adapter:

- если катсцена не идет, Dialogue продолжается сразу;
- если идет, Dialogue ставится на pending wait;
- `CutsceneLifecycleEvent` проверяет pending waits и продолжает готовые Dialogue sessions.

Когда использовать:

- `Start` уже был выше;
- реплики могли идти поверх камеры;
- теперь нужно синхронизироваться перед выбором, transition или финалом.

### Cutscene.Stop

Формы:

```csharp
Cutscene.Stop("jdh:chapter_1/home/arrival");
Cutscene.Stop(id: "jdh:chapter_1/home/arrival");
Cutscene.Stop(cutsceneId: "jdh:chapter_1/home/arrival");
Cutscene.Stop(ArrivalCamera);
```

Компиляция:

```text
echo_cutscene:stop
payload: cutscene=<id>
```

Поведение runtime adapter:

- вызывает `CutsceneService.stop(player, id, commandId)`;
- удаляет pending wait для этой команды;
- сразу продолжает Dialogue.

Когда использовать:

- завершить `holdLastFrame`;
- остановить длинный shot при выборе игрока;
- подчистить камеру перед `Complete`.

## Правила размещения Cutscene.* в Dialogue

External command должен быть перед `Line(...)`, которая идет после него в том же node.

Правильно:

```csharp
private NodeDef Scene()
{
    var node = new Node("scene");

    Cutscene.Play("jdh:chapter_1/home/arrival");
    Line("father", "Теперь камера закончилась, и эта строка появилась.");

    Complete("jdh:done");
    return node;
}
```

Неправильно:

```csharp
private NodeDef Scene()
{
    var node = new Node("scene");

    Line("father", "Эта строка последняя.");
    Cutscene.Play("jdh:chapter_1/home/arrival");

    Complete("jdh:done");
    return node;
}
```

Почему неправильно:

- compiler создаст external command после последней `Line`;
- Dialogue runtime не будет иметь следующей строки, к которой можно вернуться;
- diagnostic: `EDLG-SEM-045 External command must be followed by a Line in the same node.`

Еще неправильно:

```csharp
private NodeDef Scene()
{
    var node = new Node("scene");

    Line("father", "Выбирай.");
    Choice("yes", "Да.", next: "yes");
    Cutscene.Stop("jdh:chapter_1/home/arrival");

    return node;
}
```

Почему неправильно:

- command стоит после `Choice`;
- внешняя команда не должна объявляться после `Choice`, `Complete` или transition;
- diagnostic: `EDLG-SEM-047`.

Правильный вариант:

```csharp
private NodeDef Scene()
{
    var node = new Node("scene");

    Cutscene.Stop("jdh:chapter_1/home/arrival");
    Line("father", "Выбирай.");

    Choice("yes", "Да.", next: "yes");
    Choice("no", "Нет.", next: "no");
    return node;
}
```

## Аргументы Cutscene.* в Dialogue

Compiler принимает простые значения:

```csharp
Cutscene.Play("jdh:chapter_1/home/arrival");
Cutscene.Play(id: "jdh:chapter_1/home/arrival");
Cutscene.Play(cutsceneId: "jdh:chapter_1/home/arrival");
Cutscene.Play(ArrivalCamera);
Cutscene.Start(ArrivalCamera, holdLastFrame: true);
Cutscene.Start(ArrivalCamera, hold: true);
```

Field reference:

```csharp
private CutsceneId ArrivalCamera = "jdh:chapter_1/home/arrival";

private NodeDef Scene()
{
    var node = new Node("scene");

    Cutscene.Play(ArrivalCamera);
    Line("father", "Камера использовала id из поля.");

    Complete("jdh:done");
    return node;
}
```

Что делает compiler:

```text
ArrivalCamera
  -> fieldResolver
  -> "jdh:chapter_1/home/arrival"
  -> payload key cutscene
```

Не используйте сложные выражения в аргументах `Cutscene.*`:

```csharp
Cutscene.Play(GetSceneId());              // не simple value
Cutscene.Play("jdh:" + "arrival");        // не simple value
Cutscene.Play(SceneIds.Arrival);          // не plain field name
```

Сейчас reader принимает:

- string literal;
- bool literal;
- integer literal;
- name expression;
- named arguments.

Если id не найден:

```csharp
Cutscene.Play();
```

Compiler даст diagnostic:

```text
EDLG-SEM-046 Cutscene command requires cutscene, id or first positional argument.
```

## Story запускает Dialogue с катсценой

Story-side action остается обычным `echo_dialogue:play`. Story не знает, какие `Cutscene.*` команды внутри Dialogue.

Файл Story:

```text
data/jdh/echo_story/stories/chapter_1/home/arrival_story.es
```

Полный код:

```csharp
namespace JDH.Chapter1.Home;

class ArrivalStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/home/arrival_story", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "play_dialogue");

        Action(
            "play_dialogue",
            type: "echo_dialogue:play",
            dialogueId: "jdh:chapter_1/home/arrival_dialogue",
            audienceId: "jdh:local_author",
            output: "dialogue.outcome",
            next: "check_dialogue");

        Require(
            "check_dialogue",
            variable: "dialogue.outcome",
            value: "jdh:arrival_done",
            next: "finish");

        Finish("finish");
    }
}
```

Файл Dialogue:

```text
data/jdh/echo_dialogue/dialogues/chapter_1/home/arrival_dialogue.dialogue.es
```

Полный код:

```csharp
namespace JDH.Chapter1.Home;

class ArrivalDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ Intro() ];
    private CutsceneId Camera = "jdh:chapter_1/home/arrival";

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:chapter_1/home/arrival_dialogue");
    }

    private void Init()
    {
        Id("jdh:chapter_1/home/arrival_dialogue");
        Start("intro");
        Alias("father", "jdh:john_father", display: "Отец", visual: Portrait("jdh:john_father"));
        Alias("john", "jdh:john", display: "Джон", visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef Intro()
    {
        var node = new Node("intro");

        Cutscene.Start(Camera);
        Line("father", "Ты пришел поздно.");

        Line("john", "Я знаю.");

        Cutscene.Wait(Camera);
        Line("father", "Хорошо. Теперь слушай.");

        Complete("jdh:arrival_done");
        return node;
    }
}
```

Файл camera:

```text
<world>/echo/cutscenes/chapter_1/home/arrival.camera.es
```

Полный код:

```text
schema 2
mode 0 LOOK_AT_WITH_OFFSET
target 0 BETWEEN jdh:john jdh:john_father 0.5
framing 0 0 -0.04

key 0 0 65 0 0 0 70
key 1500 1.4 65.2 1.2 0 0 66
key 3000 2.8 65.4 2.4 0 0 62
```

Что важно:

- `type: "echo_dialogue:play"` принимает только `dialogueId` и `audienceId`;
- `output: "dialogue.outcome"` забирает terminal outcome диалога;
- `Cutscene.*` находится внутри Dialogue, а не внутри Story;
- для Minecraft-игрока `audienceId` обычно должен быть UUID игрока как строка.

## Большой Dialogue-шаблон для катсцены

Этот шаблон показывает несколько паттернов сразу: intro shot, реплики поверх камеры, hold, выбор, stop.

```csharp
namespace JDH.Chapter1.Template;

class CutsceneDialogueTemplate : DialogueResource
{
    private NodeDef[] dialogueNodes = [ Intro(), Question(), Accept(), Refuse(), Cleanup() ];

    private CutsceneId IntroCamera = "jdh:chapter_1/template/intro";
    private CutsceneId QuestionCamera = "jdh:chapter_1/template/question";

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:chapter_1/template/cutscene_dialogue");
    }

    private void Init()
    {
        Id("jdh:chapter_1/template/cutscene_dialogue");
        Start("intro");
        Alias("father", "jdh:john_father", display: "Отец", visual: Portrait("jdh:john_father"));
        Alias("john", "jdh:john", display: "Джон", visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef Intro()
    {
        var node = new Node("intro");

        Cutscene.Play(IntroCamera);
        Line("father", "Сначала общий план.");

        Line("john", "Я вижу дом.");
        return node.Next("question");
    }

    private NodeDef Question()
    {
        var node = new Node("question");

        Cutscene.Start(QuestionCamera, holdLastFrame: true);
        Line("father", "Теперь крупный план.");

        Line("father", "Ты готов отвечать?");
        Choice("accept", "Да.", next: "accept");
        Choice("refuse", "Нет.", next: "refuse");
        return node;
    }

    private NodeDef Accept()
    {
        var node = new Node("accept");

        Cutscene.Stop(QuestionCamera);
        Line("john", "Да. Спрашивай.");

        return node.Next("cleanup");
    }

    private NodeDef Refuse()
    {
        var node = new Node("refuse");

        Cutscene.Stop(QuestionCamera);
        Line("john", "Нет. Я не хочу говорить.");

        return node.Next("cleanup");
    }

    private NodeDef Cleanup()
    {
        var node = new Node("cleanup");

        Cutscene.Stop(QuestionCamera);
        Line("father", "Ладно.");

        Complete("jdh:template_done");
        return node;
    }
}
```

Примечание по `Cleanup`: если `QuestionCamera` уже остановлена в ветке, повторный `Stop` может вернуть failure на стороне cutscene, но Dialogue adapter все равно продолжит session. Такой cleanup удобен только если вы хотите принудительно убрать удержанную камеру; в строгом production-flow лучше не делать лишний `Stop`.

## Runtime bridge для Dialogue

`echo-dialogue-cutscene-adapter` слушает `DialogueExternalWaitDispatchEvent`.

Типы wait:

```text
echo_cutscene:start
echo_cutscene:play
echo_cutscene:wait
echo_cutscene:stop
```

Логика в виде псевдокода:

```java
switch (waitType) {
    case "echo_cutscene:start" -> {
        cutscene.start(player, cutsceneId, commandId, endBehavior);
        dialogue.resumeExternal(sessionId, revision, waitKey);
    }
    case "echo_cutscene:play" -> {
        cutscene.start(player, cutsceneId, commandId, endBehavior);
        if (cutscene.isPlaying(player, cutsceneId)) {
            pending.put(waitKey);
        } else {
            dialogue.resumeExternal(sessionId, revision, waitKey);
        }
    }
    case "echo_cutscene:wait" -> {
        if (cutscene.isPlaying(player, cutsceneId)) {
            pending.put(waitKey);
        } else {
            dialogue.resumeExternal(sessionId, revision, waitKey);
        }
    }
    case "echo_cutscene:stop" -> {
        cutscene.stop(player, cutsceneId, commandId);
        pending.remove(waitKey);
        dialogue.resumeExternal(sessionId, revision, waitKey);
    }
}
```

`endBehavior` читается так:

```java
boolean hold = Boolean.parseBoolean(payload.getOrDefault(
        "holdLastFrame",
        payload.getOrDefault("hold", "false")));
CutsceneEndBehavior behavior = hold
        ? CutsceneEndBehavior.HOLD_LAST_FRAME
        : CutsceneEndBehavior.AUTO_STOP;
```

Если Dialogue завершается терминально, пока есть pending cutscene wait, adapter останавливает катсцену:

```text
commandId = dialogue-terminal:<waitKey>
cutscene.stop(player, cutsceneId, commandId)
```

Практический вывод:

- `Play` безопасен для обычных законченных катсцен;
- `Start + Wait` лучше для реплик поверх камеры;
- `Start(... holdLastFrame: true) + Stop` лучше для выбора на последнем кадре;
- если `cutscene` отсутствует в payload, adapter логирует warning и продолжает Dialogue.

## Java API: lookup и start

Public service:

```java
package io.github.kxyr0.example;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.forge.api.event.CutsceneServiceLookupEvent;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class CutsceneApiExample {
    private CutsceneApiExample() {
    }

    public static Optional<CutsceneService> lookup(MinecraftServer server) {
        CutsceneServiceLookupEvent event = new CutsceneServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }

    public static CutsceneResult playArrival(MinecraftServer server, ServerPlayer player) {
        CutsceneService service = lookup(server).orElseThrow();
        return service.start(
                player,
                CutsceneId.parse("jdh:chapter_1/home/arrival"),
                CutsceneCommandId.create());
    }
}
```

Что делает код:

- создает `CutsceneServiceLookupEvent`;
- публикует его в Forge event bus;
- Echo Cutscene добавляет provider через `event.provide(service)`;
- вызывается `start`.

## Java API: hold last frame

```java
package io.github.kxyr0.example;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import net.minecraft.server.level.ServerPlayer;

public final class HoldCutsceneExample {
    private HoldCutsceneExample() {
    }

    public static CutsceneResult startHeld(CutsceneService service, ServerPlayer player) {
        return service.start(
                player,
                CutsceneId.parse("jdh:chapter_1/home/father_question"),
                CutsceneCommandId.create(),
                CutsceneEndBehavior.HOLD_LAST_FRAME);
    }

    public static CutsceneResult stopHeld(CutsceneService service, ServerPlayer player) {
        return service.stop(
                player,
                CutsceneId.parse("jdh:chapter_1/home/father_question"),
                CutsceneCommandId.create());
    }
}
```

Что важно:

- default method `start(..., endBehavior)` в API есть для совместимости;
- Forge implementation переопределяет его и реально передает `HOLD_LAST_FRAME`;
- held playback остается активным, пока не будет `stop`.

## Java API: lifecycle event

```java
package io.github.kxyr0.example;

import io.github.kxyr0.echocutscene.api.runtime.CutsceneOutcome;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.forge.api.event.CutsceneLifecycleEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CutsceneLifecycleListener {
    @SubscribeEvent
    public void onCutsceneLifecycle(CutsceneLifecycleEvent event) {
        CutsceneStatus status = event.status();
        CutsceneOutcome outcome = event.outcome();

        if (outcome == CutsceneOutcome.ACCEPTED) {
            onStarted(event);
        }

        if (status == CutsceneStatus.COMPLETED || status == CutsceneStatus.STOPPED) {
            onFinished(event);
        }
    }

    private void onStarted(CutsceneLifecycleEvent event) {
        // event.player() - ServerPlayer
        // event.playbackId() - id конкретного playback
    }

    private void onFinished(CutsceneLifecycleEvent event) {
        // Освободить свои ожидания, если мод держал состояние по playbackId.
    }
}
```

Lifecycle status:

```text
STARTING
PLAYING
HOLDING_LAST_FRAME
WAITING_FOR_CLIENT
COMPLETED
STOPPED
FAILED
```

Lifecycle outcome:

```text
ACCEPTED
ALREADY_PLAYING
COMPLETED
STOPPED
NOT_FOUND
REJECTED
FAILED
```

Текущий server host публикует event при accepted start и при finish/stop. Переход в `HOLDING_LAST_FRAME` делает playback активным, но сам по себе не является окончанием.

## Java API: query active playback

`CutsceneQueryService` имеет такой контракт:

```java
package io.github.kxyr0.echocutscene.forge.api.service;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public interface CutsceneQueryService {
    Optional<CutscenePlaybackView> activePlayback(ServerPlayer player);
}
```

`CutscenePlaybackView` содержит:

```java
public record CutscenePlaybackView(
        CutscenePlaybackId playbackId,
        CutsceneId cutsceneId,
        CameraTrackId cameraTrackId,
        UUID playerId,
        CutsceneStatus status,
        long elapsedMillis,
        double speed
) {
}
```

Использование внутри кода, где service уже получен через вашу composition root:

```java
query.activePlayback(player).ifPresent(view -> {
    CutsceneId cutscene = view.cutsceneId();
    long elapsed = view.elapsedMillis();
    double speed = view.speed();
});
```

В текущем public API есть lookup event для `CutsceneService` и `CutsceneAuthoringService`. Отдельного lookup event для `CutsceneQueryService` в просмотренном коде нет, поэтому не рассчитывайте на него как на discoverable service без собственной интеграции.

## Java API: authoring service

Lookup:

```java
package io.github.kxyr0.example;

import io.github.kxyr0.echocutscene.forge.api.event.CutsceneAuthoringServiceLookupEvent;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneAuthoringService;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class CutsceneAuthoringLookup {
    private CutsceneAuthoringLookup() {
    }

    public static Optional<CutsceneAuthoringService> lookup(MinecraftServer server) {
        CutsceneAuthoringServiceLookupEvent event = new CutsceneAuthoringServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }
}
```

Вход в authoring:

```java
authoring.enter(
        player,
        CutsceneId.parse("jdh:chapter_1/home/arrival"),
        CutsceneCommandId.create(),
        0.45D);
```

Смена режима:

```java
authoring.setMode(player, CameraControlMode.LOOK_AT_WITH_OFFSET, CutsceneCommandId.create());
```

Target на игрока:

```java
authoring.setTarget(
        player,
        CameraTarget.player(CameraTargetAnchor.EYES, CameraTargetBinding.LIVE),
        CutsceneCommandId.create());
```

Fixed target:

```java
authoring.setTarget(
        player,
        CameraTarget.fixed(new Vector3(12.0D, 65.0D, 8.0D)),
        CutsceneCommandId.create());
```

Очистка target:

```java
authoring.clearTarget(player, CutsceneCommandId.create());
```

Выход:

```java
authoring.exit(
        player,
        CutsceneId.parse("jdh:chapter_1/home/arrival"),
        CutsceneCommandId.create());
```

## Runtime contracts

`CutsceneDefinition`:

```java
public record CutsceneDefinition(
        CutsceneId cutsceneId,
        CameraTrackId cameraTrackId,
        CameraTrackRevision pinnedRevision,
        double speed,
        CutsceneInputPolicy inputPolicy,
        CutsceneHudPolicy hudPolicy,
        CutsceneEndBehavior endBehavior
) {
}
```

Default playback policies:

```java
CutsceneInputPolicy.defaultPlayback()
// blockGameplay = true
// allowDialogue = true
// allowSkip = true
// allowPauseMenu = true

CutsceneHudPolicy.defaultPlayback()
// hideGameplayHud = true
// keepDialogueVisible = true
// showDebugHud = false
```

`CutsceneResult`:

```java
public record CutsceneResult(
        CutsceneOutcome outcome,
        Optional<CutscenePlaybackId> playbackId,
        List<CutsceneDiagnostic> diagnostics
) {
}
```

Типичный разбор:

```java
CutsceneResult result = service.start(player, id, CutsceneCommandId.create());

switch (result.outcome()) {
    case ACCEPTED -> {
        CutscenePlaybackId playbackId = result.playbackId().orElseThrow();
        // playback запущен
    }
    case ALREADY_PLAYING -> {
        // у игрока уже есть активная катсцена
    }
    case FAILED -> {
        result.diagnostics().forEach(diagnostic -> {
            // diagnostic.code()
            // diagnostic.message()
        });
    }
    default -> {
        // STOPPED, COMPLETED, NOT_FOUND, REJECTED
    }
}
```

## Input lock

Во время playback или authoring клиент блокирует обычный gameplay input, если нет открытого screen.

Блокируются:

```text
movement: forward, back, left, right
jump
sneak
sprint
inventory
swap offhand
drop
use
attack
save/load toolbar
hotbar slots
mouse button
mouse scroll
interaction key mapping
```

Почему Dialogue остается рабочим:

- suppressor блокирует gameplay input только когда `minecraft.screen == null`;
- если открыт Dialogue screen, UI может принимать выборы и продолжать session;
- gameplay clicks/use/attack не должны уходить в мир.

Server-side body lock:

```text
start playback
  -> BodyLockStore.lock(player, playbackId)
  -> PlayerBodySnapshot.capture(player)
  -> active playback owns lock

finish/stop/logout
  -> BodyLockStore.unlock(player)
```

Snapshot содержит:

```text
playerId
x y z
yaw pitch
pose
vehicleId
selectedSlot
mainHand
offHand
gameType
```

Практический вывод:

- камера может летать отдельно;
- тело игрока остается настоящим `ServerPlayer`;
- если нужен актерский проход тела, используйте Echo Player Actor вместе с cutscene.

## Один активный playback на игрока

Сейчас server host хранит active playback по UUID игрока.

Если вызвать start, пока уже идет катсцена:

```text
CutsceneOutcome.ALREADY_PLAYING
playbackId = id уже активного playback
```

Пример плохого Dialogue:

```csharp
Cutscene.Start("jdh:scene/a");
Line("father", "Первая камера.");

Cutscene.Start("jdh:scene/b");
Line("father", "Вторая камера может не стартовать, потому что первая еще активна.");
```

Правильно:

```csharp
Cutscene.Start("jdh:scene/a");
Line("father", "Первая камера.");

Cutscene.Wait("jdh:scene/a");
Line("father", "Первая закончилась.");

Cutscene.Play("jdh:scene/b");
Line("father", "Вторая закончилась.");
```

Или:

```csharp
Cutscene.Start("jdh:scene/a", holdLastFrame: true);
Line("father", "Первая камера удерживается.");

Cutscene.Stop("jdh:scene/a");
Line("father", "Теперь можно запускать вторую.");

Cutscene.Play("jdh:scene/b");
Line("father", "Вторая закончилась.");
```

## Диагностика и типичные проблемы

### Track не найден

Симптом:

```text
cutscene outcome=FAILED playback=none
```

Возможная причина:

```text
CUTSCENE_TRACK_NOT_FOUND
```

Проверьте:

```text
1. Файл лежит в <world>/echo/cutscenes.
2. Расширение ровно .camera.es.
3. После создания файла выполнен /echo_cutscene reload.
4. Id соответствует пути.
```

Пример:

```text
Файл:
<world>/echo/cutscenes/chapter_1/home/arrival.camera.es

Id:
jdh:chapter_1/home/arrival
```

### В файле один key

Неправильно:

```text
key 0 0 65 0 0 0 70
```

Ошибка:

```text
Camera track needs at least two keyframes
```

Правильно:

```text
key 0 0 65 0 0 0 70
key 1000 0 65 2 0 0 70
```

### Дублируется time_ms

Неправильно:

```text
key 0 0 65 0 0 0 70
key 0 1 65 1 90 0 70
```

Ошибка:

```text
CUTSCENE_TRACK_INVALID
Camera keyframe timestamps must be strictly increasing.
```

Правильно:

```text
key 0 0 65 0 0 0 70
key 1000 1 65 1 90 0 70
```

### Неверный target

Неправильно:

```text
target 0 PLAYER EYES
```

Причина:

```text
PLAYER target требует anchor и binding.
```

Правильно:

```text
target 0 PLAYER EYES LIVE
```

### Команда Dialogue стоит после последней Line

Неправильно:

```csharp
Line("father", "Последняя строка.");
Cutscene.Wait("jdh:chapter_1/home/arrival");
Complete("jdh:done");
```

Правильно:

```csharp
Cutscene.Wait("jdh:chapter_1/home/arrival");
Line("father", "Строка после ожидания.");
Complete("jdh:done");
```

### Play hold зависает

Неправильно, если Stop должен быть ниже в этом же Dialogue:

```csharp
Cutscene.Play("jdh:chapter_1/home/question", holdLastFrame: true);
Line("father", "До этой строки Dialogue может не дойти.");
Cutscene.Stop("jdh:chapter_1/home/question");
```

Правильно:

```csharp
Cutscene.Start("jdh:chapter_1/home/question", holdLastFrame: true);
Line("father", "Эта строка появится, камера удержится на последнем кадре.");
Cutscene.Stop("jdh:chapter_1/home/question");
Line("father", "Камера остановлена.");
```

### Уже идет другая катсцена

Симптом:

```text
CutsceneOutcome.ALREADY_PLAYING
```

Проверьте:

```text
/echo_cutscene status
/echo_cutscene stop
```

В Dialogue добавьте `Wait` или `Stop` перед следующим `Start`.

## Практические шаблоны camera

### Static close-up

```text
schema 2
mode 0 LOOK_AT_WITH_OFFSET
target 0 NPC jdh:john_father EYES LIVE
framing 0 -0.12 -0.05

key 0 4 65 8 0 0 55
key 2000 4 65 8 0 0 55
```

Для чего:

- статичный крупный план говорящего;
- удобно для важной реплики;
- минимальный риск motion sickness.

### Slow push-in

```text
schema 2
mode 0 LOOK_AT_WITH_OFFSET
target 0 PLAYER EYES LIVE
framing 0 0.12 -0.08

key 0 8 65 8 0 0 70
key 2200 7 65.2 7 0 0 62
key 4400 6.2 65.3 6.2 0 0 55
```

Для чего:

- усилить напряжение;
- держать лицо в кадре через `LOOK_AT_WITH_OFFSET`;
- уменьшать FOV к концу.

### Reveal object

```text
schema 2
mode 0 LOOK_AT_WITH_OFFSET
target 0 PLAYER EYES LIVE
framing 0 0.18 -0.06
key 0 2 65 2 0 0 65
key 1500 3 65.2 3 0 0 62

target 1800 FIXED_POINT 14 65 9
framing 1800 0 -0.02
key 3000 5 65.5 5 0 0 60
key 4500 7 65.7 6 0 0 58
```

Для чего:

- сначала показать реакцию игрока;
- затем перевести внимание на объект;
- target меняется без ручного расчета yaw/pitch.

### Two-shot between actors

```text
schema 2
mode 0 LOOK_AT_WITH_OFFSET
target 0 BETWEEN jdh:john jdh:john_father 0.5
framing 0 0 -0.06

key 0 6 65 10 0 0 70
key 3000 6.4 65.3 9.4 0 0 66
```

Для чего:

- кадр на двух участников;
- хорошо перед выбором;
- `weight 0.5` держит середину между actor-ами.

## Практические шаблоны Dialogue

### Камера до реплики

```csharp
Cutscene.Play("jdh:chapter_1/home/reveal");
Line("father", "Вот почему я позвал тебя сюда.");
```

Используйте, когда реплика должна прозвучать после визуального reveal.

### Камера поверх реплики

```csharp
Cutscene.Start("jdh:chapter_1/home/arrival");
Line("father", "Ты поздно.");
Line("john", "Я знаю.");
Cutscene.Wait("jdh:chapter_1/home/arrival");
Line("father", "Теперь говори.");
```

Используйте, когда камера идет одновременно с диалогом.

### Hold до выбора

```csharp
Cutscene.Start("jdh:chapter_1/home/question", holdLastFrame: true);
Line("father", "Где ты был?");
Choice("truth", "Сказать правду.", next: "truth");
Choice("lie", "Солгать.", next: "lie");
```

В ветках:

```csharp
Cutscene.Stop("jdh:chapter_1/home/question");
Line("john", "Я был у моста.");
```

Используйте, когда хотите сохранить композицию камеры во время выбора.

### Stop перед Complete

```csharp
Cutscene.Stop("jdh:chapter_1/home/question");
Line("father", "Хорошо.");
Complete("jdh:done");
```

Используйте, когда есть шанс, что камера удерживается.

## Чеклист перед коммитом контента

Проверьте camera:

```text
[ ] Файл лежит в <world>/echo/cutscenes.
[ ] Расширение .camera.es.
[ ] Есть минимум два key.
[ ] Все key time_ms возрастают.
[ ] Id в Dialogue соответствует пути файла.
[ ] После правки выполнен /echo_cutscene reload.
[ ] /echo_cutscene play <id> работает.
[ ] /echo_cutscene stop снимает held camera.
```

Проверьте Dialogue:

```text
[ ] Каждая Cutscene.* команда стоит перед следующей Line в том же node.
[ ] Нет Cutscene.* после Choice, Complete или transition.
[ ] Для hold используется Start + Stop, если Stop должен быть в этом же Dialogue.
[ ] Play используется только для законченного playback.
[ ] Wait стоит там, где реально нужна синхронизация.
[ ] echo-dialogue-cutscene-adapter есть в runtime-сборке.
```

Проверьте Story:

```text
[ ] Story вызывает type: "echo_dialogue:play".
[ ] payload содержит только dialogueId и audienceId.
[ ] dialogueId совпадает с Id(...) в Dialogue.
[ ] output записывает outcome, если story нужна ветка результата.
```

## Короткая шпаргалка

Файл:

```text
<world>/echo/cutscenes/chapter_1/home/arrival.camera.es
```

Id:

```text
jdh:chapter_1/home/arrival
```

Playback:

```text
/echo_cutscene reload
/echo_cutscene play jdh:chapter_1/home/arrival
/echo_cutscene play jdh:chapter_1/home/arrival hold
/echo_cutscene stop
```

Dialogue:

```csharp
Cutscene.Play("jdh:chapter_1/home/arrival");
Line("father", "После катсцены.");

Cutscene.Start("jdh:chapter_1/home/arrival");
Line("father", "Поверх катсцены.");
Cutscene.Wait("jdh:chapter_1/home/arrival");
Line("father", "После завершения.");

Cutscene.Start("jdh:chapter_1/home/question", holdLastFrame: true);
Line("father", "Вопрос на удержанном кадре.");
Cutscene.Stop("jdh:chapter_1/home/question");
Line("father", "Камера снята.");
```

Camera schema 2:

```text
schema 2
mode 0 LOOK_AT_WITH_OFFSET
target 0 PLAYER EYES LIVE
framing 0 0.15 -0.1
key 0 10 65 10 0 0 70
key 1000 12 66 11 0 0 70
```

Java:

```java
CutsceneServiceLookupEvent event = new CutsceneServiceLookupEvent(server);
MinecraftForge.EVENT_BUS.post(event);
CutsceneService service = event.providers().stream().findFirst().orElseThrow();

service.start(
        player,
        CutsceneId.parse("jdh:chapter_1/home/arrival"),
        CutsceneCommandId.create());
```

## Границы текущей реализации

- `.camera.es` загружаются из `<world>/echo/cutscenes`, а не из datapack `data/<namespace>`.
- Namespace для файлового loader-а сейчас фиксирован как `jdh`.
- Отдельного authoring-файла `.cutscene.es` сейчас нет.
- `CutsceneDefinition` создается автоматически: speed `1.0`, default input policy, default HUD policy.
- Один игрок имеет один active playback.
- Held playback остается active до `Stop`.
- Dialogue adapter продолжает session даже если `start` вернул failure; failure остается на стороне cutscene diagnostics/logs.
- Для query service в просмотренном public API нет отдельного lookup event.
- Для актерского тела игрока нужна связка с Echo Player Actor.
