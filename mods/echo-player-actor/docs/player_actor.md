# Echo Player Actor: справочник для Story

Документ для авторов Story/Dialogues, которые хотят управлять настоящим телом игрока через Echo Player Actor.

Фокус только на Echo Player Actor: поворот, взгляд, движение, остановка, взаимодействие с блоком, вставание, прекращение использования предмета, предикаты и Java API, который исполняет эти действия над `ServerPlayer`.

## Что делает Echo Player Actor

Echo Player Actor управляет текущим настоящим игроком на сервере как телом actor-а.

Он не делает этого:

- не создаёт поддельного игрока;
- не переводит игрока в режим наблюдателя;
- не симулирует клавиши;
- не кликает мышью через клиентский ввод;
- не делает `ServerPlayer` мобом для API навигации.

Он делает это:

- ищет настоящего `ServerPlayer` по UUID;
- берёт lease контроля на тело игрока;
- выполняет команду на серверном тике;
- возвращает `PlayerActorResult`;
- даёт снимок тела игрока: позиция, yaw, pitch, сидит ли игрок, двигается ли, использует ли предмет.

## Главная схема для Story

Story Engine не должен напрямую знать механику тела игрока. Практический путь такой:

```text
StoryResource .es
  -> Action("...", type: "echo_dialogue:play", dialogueId: "...", audienceId: "...")
  -> Echo Dialogue открывает DialogueResource
  -> node диалога содержит Player.*
  -> внешнее ожидание Dialogue отправляет echo_player_actor:*
  -> сервис Echo Player Actor двигает/поворачивает/использует настоящий ServerPlayer
  -> Dialogue продолжается
  -> Story получает исход диалога
```

В текущем исходном коде `Player.*` распознаётся Dialogue EchoScript compiler-ом и компилируется во внешние типы команд `echo_player_actor:*`.

Если в сборке нет runtime-исполнителя внешних ожиданий Dialogue для `echo_player_actor:*`, dialogue будет корректно скомпилирован, но на исполнении остановится на внешнем ожидании. Для прямого вызова из другого мода используйте Java API из раздела `Java API`.

## Где лежат файлы контент-пака

Story:

```text
data/<namespace>/echo_story/stories/**/*.es
```

Dialogue:

```text
data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es
```

Пример структуры:

```text
data/jdh/echo_story/stories/chapter_1/wake_up.es
data/jdh/echo_dialogue/dialogues/chapter_1/wake_up_player.dialogue.es
data/jdh/echo_dialogue/dialogues/chapter_1/door_training.dialogue.es
data/jdh/echo_dialogue/dialogues/chapter_1/corridor_walk.dialogue.es
```

## Минимальный Story, Который Запускает Диалог Player Actor

Файл:

```text
data/jdh/echo_story/stories/chapter_1/wake_up.es
```

Полный код:

```csharp
namespace JDH.Chapter1;

class WakeUpStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/wake_up", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "play_wake_up_dialogue");

        Action(
            "play_wake_up_dialogue",
            type: "echo_dialogue:play",
            dialogueId: "jdh:chapter_1/wake_up_player",
            audienceId: "00000000-0000-0000-0000-000000000000",
            output: "dialogue.outcome",
            next: "after_dialogue");

        Require(
            "after_dialogue",
            variable: "dialogue.outcome",
            value: "jdh:chapter_1/wake_up_done",
            next: "finish");

        Finish("finish");
    }
}
```

Что здесь важно:

- `type: "echo_dialogue:play"` запускает Echo Dialogue через Story-Dialogue Adapter.
- `dialogueId` должен совпадать с `Id(...)` в `.dialogue.es`.
- `audienceId` это UUID игрока как строка. В статичном примере стоит заглушка; в реальном сценарии его должен подставить слой, который стартует story для конкретного игрока.
- `output: "dialogue.outcome"` кладёт терминальный outcome диалога в переменную story `dialogue.outcome`.
- Ветка Story не знает, какие `Player.*` команды были внутри dialogue; она видит только итог.

Контракт `echo_dialogue:play` принимает ровно эти ключи payload:

```text
dialogueId
audienceId
```

Служебные Story-ключи `type`, `output`, `next` принадлежат Story Engine и не попадают в payload обработчика action.

## Минимальный Dialogue с Player Actor

Файл:

```text
data/jdh/echo_dialogue/dialogues/chapter_1/wake_up_player.dialogue.es
```

Полный код:

```csharp
namespace JDH.Chapter1.Dialogues;

class WakeUpPlayerDialogue : DialogueResource
{
    private void Init()
    {
        Id("jdh:chapter_1/wake_up_player");
        Start(WakeUp);

        Alias("player", "jdh:player", display: "Игрок");
    }

    private void WakeUp()
    {
        Player.StopUsingItem();
        Line("player", "...");

        Player.StandUp();
        Line("player", "Я встал.");

        Player.LookAt(124, 65, 190, durationTicks: 12, interpolation: SMOOTH, mode: BODY_AND_HEAD);
        Line("player", "Что это у двери?");

        Player.Move(126, 64, 188, dimension: "minecraft:overworld", timeoutTicks: 160);
        Line("player", "Подойду ближе.");

        Player.LookAt(127, 65, 188, durationTicks: 8, interpolation: SMOOTH);
        Line("player", "Ручка.");

        Player.Interact(127, 64, 188, face: NORTH, hand: MAIN_HAND, maxDistance: 4);
        Line("player", "Дверь открыта.");

        Complete("jdh:chapter_1/wake_up_done");
    }
}
```

Что происходит:

- `Player.StopUsingItem()` вызывает серверный `ServerPlayer.stopUsingItem()`.
- `Player.StandUp()` вызывает `stopRiding()` у настоящего игрока.
- `Player.LookAt(...)` вычисляет yaw/pitch от позиции глаз игрока до точки.
- `Player.Move(...)` запускает движение тела игрока к координате по серверным тикам.
- `Player.Interact(...)` выполняет серверное взаимодействие с блоком через `ServerPlayerGameMode.useItemOn(...)`.
- `Complete(...)` завершает dialogue и отдаёт исход в Story.

## Правило Позиции Player.* В Node Диалога

`Player.*` команда должна стоять до строки, перед которой она должна выполниться.

Правильно:

```csharp
private void Door()
{
    Line("player", "Вижу дверь.");

    Player.LookAt(127, 65, 188, durationTicks: 8);
    Line("player", "Смотрю на ручку.");

    Player.Interact(127, 64, 188, face: NORTH);
    Line("player", "Открыл.");
}
```

Тоже правильно: команда перед первой строкой node.

```csharp
private void Start()
{
    Player.Rotate(180, 0, durationTicks: 10);
    Line("player", "Я обернулся.");
}
```

Неправильно: команда после последней строки.

```csharp
private void Broken()
{
    Line("player", "Я пойду.");
    Player.Move(130, 64, 190, timeoutTicks: 120);
}
```

Почему неправильно: внешняя команда должна иметь строку-продолжение в том же node. Иначе Dialogue compiler выдаст диагностику `EDLG-SEM-045`.

Неправильно: команда после `Choice`.

```csharp
private void BrokenChoice()
{
    Line("player", "Куда идти?");
    Choice("door", "К двери.", Door);
    Player.Move(130, 64, 190, timeoutTicks: 120);
}
```

Почему неправильно: после `Choice`, `Complete`, `Dialogue.GoTo` или `Dialogue.Load` нельзя добавлять `Player.*`. Compiler выдаст диагностику `EDLG-SEM-048`.

Правильно для choice: команды кладутся в целевой node.

```csharp
private void Choose()
{
    Line("player", "Куда идти?");
    Choice("door", "К двери.", Door);
    Choice("window", "К окну.", Window);
}

private void Door()
{
    Player.Move(130, 64, 190, timeoutTicks: 120);
    Line("player", "Я подошёл к двери.");
    Complete("jdh:door");
}

private void Window()
{
    Player.Move(118, 64, 185, timeoutTicks: 120);
    Line("player", "Я подошёл к окну.");
    Complete("jdh:window");
}
```

## Справочник Команд

### Player.Rotate

Синтаксис:

```csharp
Player.Rotate(yaw, pitch);
Player.Rotate(yaw, pitch, durationTicks: 12);
Player.Rotate(yaw: 90, pitch: 0, durationTicks: 12, interpolation: SMOOTH);
```

Обязательные аргументы:

```text
yaw
pitch
```

Дополнительные аргументы:

```text
durationTicks
interpolation
```

Рекомендуемые значения `interpolation`:

```text
LINEAR
SMOOTH
CUBIC
```

Что значит:

- `yaw` это абсолютный горизонтальный угол тела/головы игрока Minecraft.
- `pitch` это абсолютный вертикальный угол.
- `durationTicks: 0` или отсутствие duration должно означать мгновенный поворот в runtime-адаптере.
- `durationTicks: 12` означает плавный поворот примерно за 0.6 секунды на 20 TPS.
- `interpolation: SMOOTH` обычно лучше для катсцен и коротких диалоговых beat-ов.

Пример:

```csharp
private void HearNoise()
{
    Line("player", "Что это было?");

    Player.Rotate(270, 0, durationTicks: 16, interpolation: SMOOTH);
    Line("player", "Сзади кто-то есть.");
}
```

Тип внешнего ожидания:

```text
echo_player_actor:rotate
```

Карта payload:

```text
yaw=270
pitch=0
durationTicks=16
interpolation=SMOOTH
```

### Player.LookAt

Синтаксис:

```csharp
Player.LookAt(x, y, z);
Player.LookAt(x, y, z, durationTicks: 10);
Player.LookAt(x: 124, y: 65, z: 190, durationTicks: 10, interpolation: SMOOTH, mode: BODY_AND_HEAD);
```

Обязательные аргументы:

```text
x
y
z
```

Дополнительные аргументы:

```text
durationTicks
interpolation
mode
```

Рекомендуемые значения `mode`:

```text
BODY_AND_HEAD
HEAD_ONLY
```

PA1 runtime применяет поворот тела/головы вместе. `HEAD_ONLY` зарезервирован контрактом как режим взгляда, но текущий Forge runtime поворачивает тело/голову вместе.

Пример осмотра объекта:

```csharp
private void InspectSwitch()
{
    Player.LookAt(124, 65, 190, durationTicks: 8, interpolation: SMOOTH);
    Line("player", "Выключатель.");

    Player.LookAt(124, 64, 190, durationTicks: 6, interpolation: SMOOTH);
    Line("player", "Под ним свежие царапины.");
}
```

Тип внешнего ожидания:

```text
echo_player_actor:look_at
```

Карта payload:

```text
x=124
y=65
z=190
durationTicks=8
interpolation=SMOOTH
mode=BODY_AND_HEAD
```

### Player.Move

Синтаксис:

```csharp
Player.Move(x, y, z, timeoutTicks: 120);
Player.Move(x, y, z, dimension: "minecraft:overworld", timeoutTicks: 120);
Player.Move(x: 130, y: 64, z: 190, dimension: "minecraft:overworld", timeoutTicks: 160);
```

Обязательные аргументы:

```text
x
y
z
timeoutTicks
```

Дополнительные аргументы:

```text
dimension
```

Dialogue binder добавляет `dimension: "minecraft:overworld"`, если вы его не указали. Для реального контент-пака лучше писать dimension явно.

Что делает runtime:

- проверяет, что игрок online;
- проверяет control lease;
- отклоняет движение, если игрок сидит;
- отклоняет движение между измерениями;
- двигает настоящего `ServerPlayer` через `Entity.move(MoverType.SELF, step)` на серверном тике;
- завершает движение при достижении цели, timeout или stuck-состоянии.

Пример:

```csharp
private void WalkToDoor()
{
    Player.StandUp();
    Line("player", "Я встал.");

    Player.Move(130, 64, 190, dimension: "minecraft:overworld", timeoutTicks: 160);
    Line("player", "Я у двери.");
}
```

Тип внешнего ожидания:

```text
echo_player_actor:move
```

Карта payload:

```text
x=130
y=64
z=190
dimension=minecraft:overworld
timeoutTicks=160
```

### Player.Stop

Синтаксис:

```csharp
Player.Stop();
```

Что делает:

- отменяет активное `Player.Move`;
- не меняет GameMode;
- не телепортирует игрока;
- не трогает камеру.

Пример:

```csharp
private void AbortWalk()
{
    Player.Stop();
    Line("player", "Стой. Я передумал.");
}
```

Тип внешнего ожидания:

```text
echo_player_actor:stop
```

### Player.Interact

Синтаксис:

```csharp
Player.Interact(x, y, z);
Player.Interact(x, y, z, face: NORTH);
Player.Interact(x, y, z, face: NORTH, hand: MAIN_HAND, maxDistance: 4);
```

Обязательные аргументы:

```text
x
y
z
```

Дополнительные аргументы:

```text
face
hand
maxDistance
```

Значения по умолчанию в Dialogue binder:

```text
hand=MAIN_HAND
maxDistance=4.5
dimension=minecraft:overworld
```

`face` принимает имя направления:

```text
NORTH
SOUTH
EAST
WEST
UP
DOWN
```

`hand`:

```text
MAIN_HAND
OFF_HAND
```

Что делает runtime:

- берёт позицию глаз игрока;
- проверяет расстояние до центра блока;
- проверяет прямую видимость до блока;
- проверяет, что блок существует и не air;
- строит `BlockHitResult`;
- вызывает `player.gameMode.useItemOn(...)`;
- делает серверный swing.

Пример двери:

```csharp
private void OpenDoor()
{
    Player.LookAt(127, 65, 188, durationTicks: 8, interpolation: SMOOTH);
    Line("player", "Сейчас.");

    Player.Interact(127, 64, 188, face: NORTH, hand: MAIN_HAND, maxDistance: 4);
    Line("player", "Открыл.");
}
```

Пример рычага:

```csharp
private void PullLever()
{
    Player.Move(124, 64, 190, dimension: "minecraft:overworld", timeoutTicks: 120);
    Line("player", "Я рядом.");

    Player.LookAt(124, 65, 190, durationTicks: 6);
    Line("player", "Рычаг.");

    Player.Interact(124, 65, 190, face: WEST, hand: MAIN_HAND, maxDistance: 3);
    Line("player", "Готово.");
}
```

Тип внешнего ожидания:

```text
echo_player_actor:interact
```

Карта payload:

```text
x=127
y=64
z=188
face=NORTH
hand=MAIN_HAND
maxDistance=4
```

### Player.StandUp

Синтаксис:

```csharp
Player.StandUp();
```

Что делает:

- если игрок сидит в vehicle/seat, вызывает `stopRiding()`;
- если после этого игрок всё ещё passenger, возвращает отказ `PLAYER_ACTOR_STAND_UP_REJECTED`;
- если игрок уже стоит, команда успешна.

Пример:

```csharp
private void LeaveChair()
{
    Player.StandUp();
    Line("player", "Хватит сидеть.");

    Player.Move(120, 64, 180, timeoutTicks: 120);
    Line("player", "Я отошёл.");
}
```

Тип внешнего ожидания:

```text
echo_player_actor:stand_up
```

### Player.StopUsingItem

Синтаксис:

```csharp
Player.StopUsingItem();
```

Что делает:

- вызывает серверный `ServerPlayer.stopUsingItem()`;
- полезно перед катсценой, если игрок держит shield/bow/food/use animation;
- не забирает предмет;
- не меняет inventory.

Пример:

```csharp
private void Interrupt()
{
    Player.StopUsingItem();
    Line("player", "Ладно, потом.");
}
```

Тип внешнего ожидания:

```text
echo_player_actor:stop_using_item
```

### Предикаты

Имена:

```csharp
Player.IsMoving();
Player.IsSitting();
Player.IsUsingItem();
```

Типы:

```text
echo_player_actor:is_moving
echo_player_actor:is_sitting
echo_player_actor:is_using_item
```

Что они значат в API:

- `isMoving` читает active `PlayerActorMotor`;
- `isSitting` читает `ServerPlayer.isPassenger()`;
- `isUsingItem` читает `ServerPlayer.isUsingItem()`.

Для Java-кода лучше использовать `PlayerActorQueryService` напрямую:

```java
PlayerActorId actorId = PlayerActorId.of(player.getUUID());

var lookup = EchoPlayerActorServiceBridge.lookup(server);
if (lookup.status() == EchoPlayerActorServiceBridge.Status.RESOLVED) {
    boolean moving = lookup.queryService().orElseThrow().isMoving(actorId);
    boolean sitting = lookup.queryService().orElseThrow().isSitting(actorId);
    boolean usingItem = lookup.queryService().orElseThrow().isUsingItem(actorId);
}
```

Для авторинга Dialogue предикаты сейчас полезнее как зарезервированные имена внешних команд. Сам граф Dialogue не имеет ветвления по boolean-expression на эти предикаты.

## Большой пример: дверь, выбор и исходы

Файл:

```text
data/jdh/echo_dialogue/dialogues/chapter_1/door_training.dialogue.es
```

Полный код:

```csharp
namespace JDH.Chapter1.Dialogues;

class DoorTrainingDialogue : DialogueResource
{
    private void Init()
    {
        Id("jdh:chapter_1/door_training");
        Start(Intro);

        Alias("player", "jdh:player", display: "Игрок");
    }

    private void Intro()
    {
        Player.StopUsingItem();
        Line("player", "Надо сосредоточиться.");

        Player.StandUp();
        Line("player", "Я на ногах.");

        Player.LookAt(127, 65, 188, durationTicks: 12, interpolation: SMOOTH);
        Line("player", "Передо мной дверь.");

        Choice("open", "Открыть дверь.", OpenDoor);
        Choice("inspect", "Осмотреть дверь.", InspectDoor);
        Choice("leave", "Отойти.", LeaveDoor);
    }

    private void OpenDoor()
    {
        Player.Move(126, 64, 188, dimension: "minecraft:overworld", timeoutTicks: 160);
        Line("player", "Подхожу.");

        Player.LookAt(127, 65, 188, durationTicks: 8, interpolation: SMOOTH);
        Line("player", "Рука на ручке.");

        Player.Interact(127, 64, 188, face: NORTH, hand: MAIN_HAND, maxDistance: 4);
        Line("player", "Открыто.");

        Complete("jdh:chapter_1/door_opened");
    }

    private void InspectDoor()
    {
        Player.Move(126, 64, 188, dimension: "minecraft:overworld", timeoutTicks: 160);
        Line("player", "Надо посмотреть ближе.");

        Player.LookAt(127, 66, 188, durationTicks: 8, interpolation: SMOOTH);
        Line("player", "На дереве свежие следы.");

        Player.LookAt(127, 64, 188, durationTicks: 8, interpolation: SMOOTH);
        Line("player", "А под дверью грязь.");

        Choice("open_after_inspect", "Теперь открыть.", OpenDoor);
        Choice("step_back", "Отойти.", LeaveDoor);
    }

    private void LeaveDoor()
    {
        Player.Stop();
        Line("player", "Нет. Не сейчас.");

        Player.Move(122, 64, 184, dimension: "minecraft:overworld", timeoutTicks: 140);
        Line("player", "Я отошёл.");

        Complete("jdh:chapter_1/door_left_closed");
    }
}
```

Story, который запускает этот dialogue и ветвится по outcome:

```csharp
namespace JDH.Chapter1;

class DoorTrainingStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/door_training", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "door_dialogue");

        Action(
            "door_dialogue",
            type: "echo_dialogue:play",
            dialogueId: "jdh:chapter_1/door_training",
            audienceId: "00000000-0000-0000-0000-000000000000",
            output: "door.outcome",
            next: "opened");

        Require(
            "opened",
            variable: "door.outcome",
            value: "jdh:chapter_1/door_opened",
            next: "finish_opened");

        Finish("finish_opened");
    }
}
```

Если профиль Story Engine поддерживает только ветвление по равенству через `Require`, отдельную ветку для `door_left_closed` оформляйте отдельным story resource или внешним orchestration-слоем. Dialogue outcome всё равно будет доступен как `door.outcome`.

## Большой пример: коридор с несколькими остановками

Файл:

```text
data/jdh/echo_dialogue/dialogues/chapter_1/corridor_walk.dialogue.es
```

Полный код:

```csharp
namespace JDH.Chapter1.Dialogues;

class CorridorWalkDialogue : DialogueResource
{
    private void Init()
    {
        Id("jdh:chapter_1/corridor_walk");
        Start(Begin);

        Alias("player", "jdh:player", display: "Игрок");
    }

    private void Begin()
    {
        Player.StandUp();
        Line("player", "Пора идти.");

        Player.Move(110, 64, 200, dimension: "minecraft:overworld", timeoutTicks: 180);
        Line("player", "Первый поворот.");

        Player.LookAt(115, 65, 205, durationTicks: 10, interpolation: SMOOTH);
        Line("player", "Свет в конце коридора.");

        Player.Move(115, 64, 205, dimension: "minecraft:overworld", timeoutTicks: 180);
        Line("player", "Здесь тише.");

        Player.Rotate(90, 0, durationTicks: 14, interpolation: SMOOTH);
        Line("player", "Нужно свернуть.");

        Player.Move(122, 64, 205, dimension: "minecraft:overworld", timeoutTicks: 200);
        Line("player", "Я на месте.");

        Complete("jdh:chapter_1/corridor_reached");
    }
}
```

Почему здесь после каждого `Player.*` стоит `Line(...)`:

- Dialogue external wait продолжает node на следующей line position.
- После reload/recovery у Dialogue есть понятная точка продолжения.
- Игрок видит короткий текстовый beat после завершения действия.

## Большой пример: взаимодействие с двумя блоками

Файл:

```text
data/jdh/echo_dialogue/dialogues/chapter_1/power_panel.dialogue.es
```

Полный код:

```csharp
namespace JDH.Chapter1.Dialogues;

class PowerPanelDialogue : DialogueResource
{
    private void Init()
    {
        Id("jdh:chapter_1/power_panel");
        Start(StartPanel);

        Alias("player", "jdh:player", display: "Игрок");
    }

    private void StartPanel()
    {
        Player.Move(140, 64, 220, dimension: "minecraft:overworld", timeoutTicks: 200);
        Line("player", "Панель передо мной.");

        Player.LookAt(141, 65, 220, durationTicks: 8, interpolation: SMOOTH);
        Line("player", "Первый рычаг.");

        Player.Interact(141, 65, 220, face: WEST, hand: MAIN_HAND, maxDistance: 3);
        Line("player", "Щёлк.");

        Player.LookAt(141, 66, 221, durationTicks: 8, interpolation: SMOOTH);
        Line("player", "Второй переключатель.");

        Player.Interact(141, 66, 221, face: WEST, hand: MAIN_HAND, maxDistance: 3);
        Line("player", "Генератор ожил.");

        Complete("jdh:chapter_1/power_enabled");
    }
}
```

## Большой пример: сцена пробуждения без NPC

Файл:

```text
data/jdh/echo_dialogue/dialogues/chapter_1/player_only_wakeup.dialogue.es
```

Полный код:

```csharp
namespace JDH.Chapter1.Dialogues;

class PlayerOnlyWakeupDialogue : DialogueResource
{
    private void Init()
    {
        Id("jdh:chapter_1/player_only_wakeup");
        Start(EyesOpen);

        Alias("player", "jdh:player", display: "Игрок");
    }

    private void EyesOpen()
    {
        Player.StopUsingItem();
        Line("player", "Голова тяжёлая.");

        Player.Rotate(0, 10, durationTicks: 20, interpolation: SMOOTH);
        Line("player", "Потолок.");

        Player.Rotate(45, 0, durationTicks: 16, interpolation: SMOOTH);
        Line("player", "Окно.");

        Player.Rotate(120, 0, durationTicks: 16, interpolation: SMOOTH);
        Line("player", "Дверь.");

        Player.StandUp();
        Line("player", "Нужно подняться.");

        Player.Move(102, 64, 75, dimension: "minecraft:overworld", timeoutTicks: 180);
        Line("player", "Я сделал несколько шагов.");

        Complete("jdh:chapter_1/player_awake");
    }
}
```

## Примеры Payload Для Диспетчеров

Dialogue compiler сохраняет `Player.*` как `CompiledDialogueCommand`. Runtime позже видит это как `DialogueExternalWait`.

Авторский код:

```csharp
Player.Interact(127, 64, 188, face: NORTH, hand: MAIN_HAND, maxDistance: 4);
Line("player", "Открыл.");
```

Скомпилированная команда:

```text
type: echo_player_actor:interact
position: 0
arguments:
  x=127
  y=64
  z=188
  face=NORTH
  hand=MAIN_HAND
  maxDistance=4
  dimension=minecraft:overworld
```

Payload внешнего ожидания использует `DialogueCommandPayload.encode`: это URL-encoded текст key/value:

```text
dimension=minecraft%3Aoverworld&face=NORTH&hand=MAIN_HAND&maxDistance=4&x=127&y=64&z=188
```

Диспетчер декодирует его так:

```java
Map<String, String> arguments = DialogueCommandPayload.decode(wait.payload());
String x = arguments.get("x");
String y = arguments.get("y");
String z = arguments.get("z");
String hand = arguments.getOrDefault("hand", "MAIN_HAND");
```

## Java API: Поиск Сервисов

Используйте сервисный мост Forge, а не создавайте runtime-классы вручную.

```java
import io.github.kxyr0.echoplayeractor.forge.api.service.EchoPlayerActorServiceBridge;
import io.github.kxyr0.echoplayeractor.forge.api.service.PlayerActorQueryService;
import io.github.kxyr0.echoplayeractor.forge.api.service.PlayerActorService;
import io.github.kxyr0.echoplayeractor.forge.api.service.PlayerControlLeaseService;
import net.minecraft.server.MinecraftServer;

public final class PlayerActorServices {
    public static Services require(MinecraftServer server) {
        var lookup = EchoPlayerActorServiceBridge.lookup(server);
        if (lookup.status() != EchoPlayerActorServiceBridge.Status.RESOLVED) {
            throw new IllegalStateException("Сервис Echo Player Actor недоступен.");
        }
        return new Services(
            lookup.service().orElseThrow(),
            lookup.queryService().orElseThrow(),
            lookup.leaseService().orElseThrow());
    }

    public record Services(
        PlayerActorService actor,
        PlayerActorQueryService query,
        PlayerControlLeaseService leases) {
    }
}
```

## Java API: Получить И Освободить Управление

Любая команда тела должна идти от владельца, которому разрешён активный lease.

```java
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlLease;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlMode;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlLeaseId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlScopeId;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerActorLeaseExample {
    public static PlayerActorOwnerId acquireForScene(
        PlayerActorServices.Services services,
        ServerPlayer player,
        long serverTick
    ) {
        PlayerActorId actorId = PlayerActorId.of(player.getUUID());
        PlayerActorOwnerId owner = PlayerActorOwnerId.system("chapter_1_wake_up");

        PlayerControlLease lease = new PlayerControlLease(
            PlayerControlLeaseId.create(),
            actorId,
            PlayerControlScopeId.CINEMATIC_CONTROL,
            PlayerControlMode.CINEMATIC_CONTROL,
            owner,
            List.of(owner),
            serverTick,
            0L,
            PlayerControlStatus.ACTIVE,
            0L);

        var result = services.leases().acquire(lease);
        if (result.failure().isPresent()) {
            throw new IllegalStateException(result.message());
        }
        return owner;
    }

    public static void release(
        PlayerActorServices.Services services,
        ServerPlayer player,
        PlayerActorOwnerId owner
    ) {
        services.leases().release(PlayerActorId.of(player.getUUID()), owner);
    }
}
```

Что значат поля lease:

```text
leaseId            уникальный id попытки владения телом
actorId            UUID настоящего ServerPlayer
scopeId            область контроля: human/cinematic
mode               режим контроля: HUMAN_CONTROL или CINEMATIC_CONTROL
rootOwner          главный владелец
authorisedOwners   дополнительные владельцы, которым можно выполнять команды
createdTick        server tick создания
generation         поколение lease, полезно для recovery
status             ACTIVE/RELEASED/CORRUPT
revision           версия записи lease
```

## Java API: Поворот

```java
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInterpolation;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorRotationRequest;
import net.minecraft.server.level.ServerPlayer;

public final class RotatePlayerExample {
    public static void rotate(
        PlayerActorServices.Services services,
        ServerPlayer player,
        PlayerActorOwnerId owner
    ) {
        PlayerActorId actorId = PlayerActorId.of(player.getUUID());

        var result = services.actor().rotate(new PlayerActorRotationRequest(
            PlayerActorCommandId.create(),
            actorId,
            owner,
            180.0D,
            0.0D,
            12,
            PlayerActorInterpolation.SMOOTH));

        if (result.failure().isPresent()) {
            throw new IllegalStateException(result.failure().orElseThrow() + ": " + result.message());
        }
    }
}
```

## Java API: Взгляд На Точку

```java
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInterpolation;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookMode;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookRequest;
import net.minecraft.server.level.ServerPlayer;

public final class LookAtExample {
    public static void lookAtDoorHandle(
        PlayerActorServices.Services services,
        ServerPlayer player,
        PlayerActorOwnerId owner
    ) {
        var result = services.actor().lookAt(new PlayerActorLookRequest(
            PlayerActorCommandId.create(),
            PlayerActorId.of(player.getUUID()),
            owner,
            new Vector3(127.0D, 65.0D, 188.0D),
            10,
            PlayerActorInterpolation.SMOOTH,
            PlayerActorLookMode.BODY_AND_HEAD));

        if (result.failure().isPresent()) {
            throw new IllegalStateException(result.message());
        }
    }
}
```

## Java API: Движение

```java
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import net.minecraft.server.level.ServerPlayer;

public final class MoveExample {
    public static void moveToDoor(
        PlayerActorServices.Services services,
        ServerPlayer player,
        PlayerActorOwnerId owner
    ) {
        var result = services.actor().move(new PlayerActorMoveRequest(
            PlayerActorCommandId.create(),
            PlayerActorId.of(player.getUUID()),
            owner,
            new Vector3(126.0D, 64.0D, 188.0D),
            "minecraft:overworld",
            160));

        if (result.status() == PlayerActorStatus.WAITING) {
            return;
        }
        if (result.failure().isPresent()) {
            throw new IllegalStateException(result.message());
        }
    }
}
```

Как ждать завершения движения в своём коде:

```java
PlayerActorId actorId = PlayerActorId.of(player.getUUID());

if (!services.query().isMoving(actorId)) {
    // Движение завершилось или было остановлено.
}
```

## Java API: Остановка

```java
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import net.minecraft.server.level.ServerPlayer;

public final class StopMoveExample {
    public static void stop(
        PlayerActorServices.Services services,
        ServerPlayer player,
        PlayerActorOwnerId owner
    ) {
        var result = services.actor().stop(PlayerActorId.of(player.getUUID()), owner);
        if (result.failure().isPresent()) {
            throw new IllegalStateException(result.message());
        }
    }
}
```

## Java API: Взаимодействие С Блоком

```java
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInteractionRequest;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class InteractExample {
    public static void openDoor(
        PlayerActorServices.Services services,
        ServerPlayer player,
        PlayerActorOwnerId owner
    ) {
        var result = services.actor().interact(new PlayerActorInteractionRequest(
            PlayerActorCommandId.create(),
            PlayerActorId.of(player.getUUID()),
            owner,
            127,
            64,
            188,
            Optional.of("NORTH"),
            "MAIN_HAND",
            4.0D));

        if (result.failure().isPresent()) {
            throw new IllegalStateException(result.failure().orElseThrow() + ": " + result.message());
        }
    }
}
```

## Java API: Встать И Прекратить Использование Предмета

```java
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import net.minecraft.server.level.ServerPlayer;

public final class BodyCleanupExample {
    public static void cleanup(
        PlayerActorServices.Services services,
        ServerPlayer player,
        PlayerActorOwnerId owner
    ) {
        PlayerActorId actorId = PlayerActorId.of(player.getUUID());

        var stopUsing = services.actor().stopUsingItem(actorId, owner);
        if (stopUsing.failure().isPresent()) {
            throw new IllegalStateException(stopUsing.message());
        }

        var standUp = services.actor().standUp(actorId, owner);
        if (standUp.failure().isPresent()) {
            throw new IllegalStateException(standUp.message());
        }
    }
}
```

## Java-Эскиз: Исполнитель Внешних Ожиданий Dialogue

Этот раздел показывает, что должен делать runtime-адаптер для `echo_player_actor:*`. Это не авторский синтаксис, а мост между ожиданиями Echo Dialogue и сервисом Echo Player Actor.

```java
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInterpolation;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookMode;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorRotationRequest;
import io.github.kxyr0.echoplayeractor.forge.api.service.EchoPlayerActorServiceBridge;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class DialoguePlayerActorWaits {
    private final Map<PlayerActorCommandId, Pending> pendingMoves = new LinkedHashMap<>();

    public void dispatch(
        MinecraftServer server,
        DialogueService dialogueService,
        DialogueSessionView session,
        DialogueExternalWait wait,
        ServerPlayer player
    ) {
        Map<String, String> args = DialogueCommandPayload.decode(wait.payload());
        PlayerActorId actorId = PlayerActorId.of(player.getUUID());
        PlayerActorOwnerId owner = PlayerActorOwnerId.dialogue(session.sessionId().value());
        PlayerActorCommandId commandId = PlayerActorCommandId.create();

        var lookup = EchoPlayerActorServiceBridge.lookup(server);
        if (lookup.status() != EchoPlayerActorServiceBridge.Status.RESOLVED) {
            dialogueService.cancel(session.sessionId());
            return;
        }

        var actor = lookup.service().orElseThrow();
        var result = switch (wait.waitType()) {
            case "echo_player_actor:rotate" -> actor.rotate(new PlayerActorRotationRequest(
                commandId,
                actorId,
                owner,
                number(args, "yaw"),
                number(args, "pitch"),
                integer(args, "durationTicks", 0),
                interpolation(args)));

            case "echo_player_actor:look_at" -> actor.lookAt(new PlayerActorLookRequest(
                commandId,
                actorId,
                owner,
                vector(args),
                integer(args, "durationTicks", 0),
                interpolation(args),
                lookMode(args)));

            case "echo_player_actor:move" -> actor.move(new PlayerActorMoveRequest(
                commandId,
                actorId,
                owner,
                vector(args),
                args.getOrDefault("dimension", "minecraft:overworld"),
                integer(args, "timeoutTicks", 120)));

            case "echo_player_actor:stop" -> actor.stop(actorId, owner);
            case "echo_player_actor:stand_up" -> actor.standUp(actorId, owner);
            case "echo_player_actor:stop_using_item" -> actor.stopUsingItem(actorId, owner);

            default -> throw new IllegalArgumentException("Unsupported player actor wait: " + wait.waitType());
        };

        if (result.status() == PlayerActorStatus.WAITING) {
            pendingMoves.put(commandId, new Pending(dialogueService, session.sessionId(), session.revision(), wait.waitKey(), actorId));
            return;
        }
        if (result.failure().isPresent()) {
            dialogueService.cancel(session.sessionId());
            return;
        }
        resume(dialogueService, session.sessionId(), session.revision(), wait.waitKey());
    }

    public void tick(MinecraftServer server) {
        var lookup = EchoPlayerActorServiceBridge.lookup(server);
        if (lookup.status() != EchoPlayerActorServiceBridge.Status.RESOLVED) {
            return;
        }
        var query = lookup.queryService().orElseThrow();

        for (var entry : pendingMoves.entrySet().stream().toList()) {
            Pending pending = entry.getValue();
            if (!query.isMoving(pending.actorId())) {
                resume(pending.service(), pending.sessionId(), pending.expectedRevision(), pending.waitKey());
                pendingMoves.remove(entry.getKey());
            }
        }
    }

    private void resume(DialogueService service, DialogueSessionId sessionId, int expectedRevision, String waitKey) {
        service.resumeExternal(new DialogueExternalResumeRequest(sessionId, expectedRevision, waitKey));
    }

    private Vector3 vector(Map<String, String> args) {
        return new Vector3(number(args, "x"), number(args, "y"), number(args, "z"));
    }

    private double number(Map<String, String> args, String key) {
        return Double.parseDouble(args.get(key));
    }

    private int integer(Map<String, String> args, String key, int fallback) {
        return args.containsKey(key) ? Integer.parseInt(args.get(key)) : fallback;
    }

    private PlayerActorInterpolation interpolation(Map<String, String> args) {
        return PlayerActorInterpolation.valueOf(args.getOrDefault("interpolation", "LINEAR"));
    }

    private PlayerActorLookMode lookMode(Map<String, String> args) {
        return PlayerActorLookMode.valueOf(args.getOrDefault("mode", "BODY_AND_HEAD"));
    }

    private record Pending(
        DialogueService service,
        DialogueSessionId sessionId,
        int expectedRevision,
        String waitKey,
        PlayerActorId actorId) {
    }
}
```

Что в этом эскизе важно:

- `DialogueCommandPayload.decode(wait.payload())` превращает payload обратно в map.
- `PlayerActorId.of(player.getUUID())` привязывает actor к настоящему `ServerPlayer`.
- `PlayerActorOwnerId.dialogue(session.sessionId().value())` делает session Dialogue владельцем команд.
- Если `PlayerActorResult.status() == WAITING`, adapter должен дождаться завершения движения/поворота и только потом вызвать `resumeExternal(...)`.
- Если команда получила отказ или ошибку, adapter должен отменить dialogue или перевести его в контролируемый failure flow.
- Перед реальным использованием нужен lease: bridge Story/Cutscene/Dialogue должен получить и освободить lease контроля для того же владельца.

## Таблица Payload Команд

| Авторский вызов | Внешний тип | Обязательный payload | Частый дополнительный payload |
| --- | --- | --- | --- |
| `Player.Rotate(yaw, pitch)` | `echo_player_actor:rotate` | `yaw`, `pitch` | `durationTicks`, `interpolation` |
| `Player.LookAt(x, y, z)` | `echo_player_actor:look_at` | `x`, `y`, `z` | `durationTicks`, `interpolation`, `mode` |
| `Player.Move(x, y, z)` | `echo_player_actor:move` | `x`, `y`, `z` | `dimension`, `timeoutTicks` |
| `Player.Stop()` | `echo_player_actor:stop` | нет | нет |
| `Player.Interact(x, y, z)` | `echo_player_actor:interact` | `x`, `y`, `z` | `face`, `hand`, `maxDistance` |
| `Player.StandUp()` | `echo_player_actor:stand_up` | нет | нет |
| `Player.StopUsingItem()` | `echo_player_actor:stop_using_item` | нет | нет |
| `Player.IsMoving()` | `echo_player_actor:is_moving` | нет | нет |
| `Player.IsSitting()` | `echo_player_actor:is_sitting` | нет | нет |
| `Player.IsUsingItem()` | `echo_player_actor:is_using_item` | нет | нет |

## Значения Результатов И Ошибок

Статусы:

| Статус | Что значит |
| --- | --- |
| `SUCCEEDED` | Команда завершилась сразу. |
| `WAITING` | Команда принята и продолжится на серверных тиках. |
| `DUPLICATE` | Повтор того же command id уже известен. |
| `REJECTED` | Команда не принята из-за условий preflight/control/runtime. |
| `FAILED` | Runtime-ошибка при исполнении. |
| `UNAVAILABLE` | Нужный backend или сервис недоступен. |

Частые failures:

| Failure | Что значит | Что делать автору |
| --- | --- | --- |
| `PLAYER_ACTOR_OFFLINE` | Игрок вышел или UUID не найден. | Не продолжать сцену; отменить её или перезапустить позже. |
| `PLAYER_ACTOR_CONTROL_OWNER_MISMATCH` | Команда пришла не от владельца active lease. | Исправить owner lineage или получить lease. |
| `PLAYER_ACTOR_STILL_SEATED` | `Player.Move` вызван, пока игрок сидит. | Перед move вызвать `Player.StandUp()`. |
| `PLAYER_ACTOR_MOVE_DESTINATION_INVALID` | Другое измерение или плохая цель. | Указать правильный `dimension` и координаты. |
| `PLAYER_ACTOR_INTERACTION_TOO_FAR` | Блок дальше `maxDistance`. | Подвести игрока через `Player.Move`. |
| `PLAYER_ACTOR_INTERACTION_NO_LINE_OF_SIGHT` | Между позицией глаз и блоком препятствие. | Повернуть/подвести игрока или выбрать другой face/позицию. |
| `PLAYER_ACTOR_INTERACTION_TARGET_INVALID` | В точке нет блока. | Проверить координаты и timing сцены. |
| `PLAYER_ACTOR_INTERACTION_FACE_UNRESOLVED` | Неверный `face`. | Использовать `NORTH/SOUTH/EAST/WEST/UP/DOWN`. |
| `PLAYER_ACTOR_NAVIGATION_BACKEND_UNSUPPORTED` | Backend навигации не может вести `ServerPlayer` как `Mob`. | Использовать резервный путь через `PlayerActorMotor` или отдельный bridge для игрока. |

## Авторские Паттерны

### Очистка Перед Сценой

```csharp
private void SceneStart()
{
    Player.StopUsingItem();
    Line("player", "Я опустил руку.");

    Player.StandUp();
    Line("player", "Я поднялся.");
}
```

### Посмотреть И Взаимодействовать

```csharp
private void PressButton()
{
    Player.LookAt(150, 66, 210, durationTicks: 6, interpolation: SMOOTH);
    Line("player", "Кнопка.");

    Player.Interact(150, 66, 210, face: WEST, hand: MAIN_HAND, maxDistance: 3);
    Line("player", "Нажал.");
}
```

### Дойти И Повернуться

```csharp
private void EnterRoom()
{
    Player.Move(160, 64, 220, dimension: "minecraft:overworld", timeoutTicks: 200);
    Line("player", "Я вошёл.");

    Player.Rotate(180, 0, durationTicks: 12, interpolation: SMOOTH);
    Line("player", "Дверь закрылась за спиной.");
}
```

### Остановиться На Story Beat

```csharp
private void Interrupted()
{
    Player.Stop();
    Line("player", "Я остановился.");

    Player.LookAt(118, 65, 205, durationTicks: 8, interpolation: SMOOTH);
    Line("player", "Там кто-то есть.");
}
```

### Избежать Ошибки Interact

Плохо:

```csharp
private void TooFar()
{
    Player.Interact(200, 64, 200, face: NORTH);
    Line("player", "Это не сработает, если я далеко.");
}
```

Лучше:

```csharp
private void CloseEnough()
{
    Player.Move(198, 64, 200, dimension: "minecraft:overworld", timeoutTicks: 240);
    Line("player", "Теперь достану.");

    Player.LookAt(200, 65, 200, durationTicks: 8, interpolation: SMOOTH);
    Line("player", "Вот цель.");

    Player.Interact(200, 64, 200, face: NORTH, maxDistance: 4);
    Line("player", "Сработало.");
}
```

## Чеклист Для Автора Story/Dialogues

Перед `/reload`:

- файл story лежит в `data/<namespace>/echo_story/stories/**/*.es`;
- файл dialogue лежит в `data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es`;
- `Story(... id ...)` уникален;
- `Dialogue Id(...)` уникален;
- `Action(... type: "echo_dialogue:play" ...)` ссылается на существующий `dialogueId`;
- `audienceId` это конкретный UUID игрока;
- каждая `Player.*` команда в dialogue стоит перед `Line(...)`;
- после `Choice`, `Complete`, `Dialogue.GoTo`, `Dialogue.Load` нет `Player.*`;
- перед `Player.Move` есть `Player.StandUp`, если игрок может сидеть;
- перед `Player.Interact` есть `Player.Move` или позиция уже гарантированно близко;
- `face` написан как `NORTH/SOUTH/EAST/WEST/UP/DOWN`;
- `hand` написан как `MAIN_HAND` или `OFF_HAND`;
- `dimension` указан явно для движения;
- `timeoutTicks` указан явно для движения;
- runtime-сборка содержит executor для waits `echo_player_actor:*` или ваш мод вызывает `PlayerActorService` напрямую.

## Ручная Smoke-Проверка

Минимальный ручной прогон:

```text
1. Положить StoryResource и DialogueResource в datapack.
2. Запустить dedicated/server world.
3. Убедиться, что загружены echo_story_engine, echo_story_dialogue_adapter, echo_dialogue, echo_player_actor.
4. Если dialogue запускается через external wait, убедиться, что установлен executor для `echo_player_actor:*`.
5. Выполнить /reload.
6. Проверить /echo_story stories.
7. Проверить /echo_dialogue dialogues.
8. Запустить story или dialogue на конкретного игрока.
9. Проверить, что тело игрока реально поворачивается/движется/взаимодействует.
10. Проверить, что GameMode не стал режимом наблюдателя.
11. Проверить, что не появился поддельный игрок.
```

## Шаблоны Для Копирования

### Шаблон: Story Запускает Player Actor Dialogue

```csharp
namespace MyPack.Stories;

class PlayerActorStoryTemplate : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("mypack:player_actor_story", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "play");

        Action(
            "play",
            type: "echo_dialogue:play",
            dialogueId: "mypack:player_actor_dialogue",
            audienceId: "00000000-0000-0000-0000-000000000000",
            output: "dialogue.outcome",
            next: "finish");

        Finish("finish");
    }
}
```

### Шаблон: Dialogue Управляет Телом Игрока

```csharp
namespace MyPack.Dialogues;

class PlayerActorDialogueTemplate : DialogueResource
{
    private void Init()
    {
        Id("mypack:player_actor_dialogue");
        Start(StartScene);

        Alias("player", "mypack:player", display: "Игрок");
    }

    private void StartScene()
    {
        Player.StopUsingItem();
        Line("player", "Начинаем.");

        Player.StandUp();
        Line("player", "Я готов.");

        Player.LookAt(0, 64, 0, durationTicks: 8, interpolation: SMOOTH);
        Line("player", "Смотрю.");

        Player.Move(0, 64, 2, dimension: "minecraft:overworld", timeoutTicks: 120);
        Line("player", "Я пришёл.");

        Complete("mypack:done");
    }
}
```

### Шаблон: Прямая Java-Команда Для Катсцены

```java
var services = PlayerActorServices.require(server);
var owner = PlayerActorLeaseExample.acquireForScene(services, player, server.getTickCount());

try {
    BodyCleanupExample.cleanup(services, player, owner);
    LookAtExample.lookAtDoorHandle(services, player, owner);
    MoveExample.moveToDoor(services, player, owner);
} finally {
    PlayerActorLeaseExample.release(services, player, owner);
}
```

## Текущие Границы PA1

Границы PA1, которые важны для документации и контента:

- `Player.Move` между измерениями отклоняется.
- `Player.Move` это консервативное движение тела по серверным тикам, а не полноценная навигация `Mob`.
- `echo-player-navigation-adapter` не может зарегистрировать `ServerPlayer` как `Mob`; он делает предварительную проверку там, где это возможно, и делегирует движение тела в `PlayerActorMotor`.
- Публичный lifecycle Cutscene сейчас раскрывает playback id, но не полную цепочку владельцев Dialogue/Story.
- Компилятор Dialogue принимает `Player.*`, но исполнение всё ещё зависит от наличия исполнителя внешних ожиданий Dialogue для `echo_player_actor:*` или от прямого вызова Java API.
- Восстановление через SavedData для состояния активной команды Player Actor не полностью реализовано в PA1.
