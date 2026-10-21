# Echo Player Sleep DSL

Русский справочник по тому, где какие методы доступны для Player Sleep, где их можно использовать, какие аргументы нужны и какие формы в текущей рабочей копии реально компилируются.

Документ относится только к `echo-player-sleep`, `echo-trigger` и `echo-trigger-player-sleep-adapter`. Другие подсистемы здесь не описываются.

## Главное

В текущей рабочей копии Player Sleep доступен автору так:

```text
TriggerResource
  Init()
    Subscribe(PlayerSleep.Confirmed(...))
    Subscribe(PlayerSleep.Confirmed(..., sleepCount: 5))
    Subscribe(PlayerSleep.Confirmed(..., sleepCountAtLeast: 5))
    StartStory(...)

StoryResource
  OnStart()
    Sleep.Blink(...)
    Sleep.CloseEyes(...)
    Sleep.WaitUntilClosed()
    Sleep.EnterDream(...)
    Sleep.ReturnToBed()
    Sleep.CompleteNight()
    Sleep.OpenEyes(...)
    Sleep.Wait(seconds)
    Sleep.Complete()

  Nodes()
    Sleep.CloseEyes("node_id", ..., next: "next_node")
    Action(..., type: "echo_player_sleep:*") // низкоуровневый fallback
```

Высокоуровневые имена вида `Sleep.Arm(...)`, `Sleep.CloseEyes(...)`, `Sleep.EnterDream(...)` теперь являются рабочими intrinsic-вызовами Story EchoScript compiler. Они компилируются в те же external action id `echo_player_sleep:*`, но автору больше не нужно руками писать `Action(..., type: ...)`.

Линейная форма для `StoryResource.OnStart()`:

```echoscript
Sleep.CloseEyes(duration: Ticks(18));
Sleep.WaitUntilClosed();
Sleep.Wait(0.25);
Sleep.EnterDream("minecraft:overworld", 0.5, 64, 0.5, yaw: 180, pitch: 0);
Sleep.OpenEyes(duration: Ticks(16), sleepyBlink: true);
Sleep.WaitUntilOpen();
Sleep.ReturnToBed();
Sleep.CompleteNight();
Sleep.OpenEyes(duration: Ticks(16), sleepyBlink: true);
Sleep.WaitUntilOpen();
Sleep.Complete();
```

Если сон должен переносить не на фиксированные координаты, а в площадку `echo-blackspace`, сначала создай Blackspace и передай сам объект `blackspace`:

```echoscript
var blackspace = await Blackspace.Create("jdh:first_contact");

Sleep.CloseEyes(duration: Ticks(18));
Sleep.WaitUntilClosed();
Sleep.Wait(0.25);
Sleep.EnterDream(blackspace);
Sleep.OpenEyes(duration: Ticks(16), sleepyBlink: true);
Sleep.WaitUntilOpen();
```

`Sleep.Wait(0.25)` и `Sleep.WaitSeconds(0.25)` ждут секунды. Для тиков можно использовать `Sleep.Wait(duration: Ticks(5))` или `Sleep.Wait(ticks: 5)`.

`Sleep.EnterDream("minecraft:overworld", 0.5, 64, 0.5, yaw: 180, pitch: 0)` - это прямой телепорт на координаты.
`Sleep.EnterDream(blackspace)` - это lifecycle-aware вход в Blackspace: sleep action ждёт готовность shader pipeline, телепорт и ACTIVE-сессию.
`Sleep.EnterDream(blackspace.Spawn)` остаётся низкоуровневым target и не включает Blackspace lifecycle; используй его только для ручных координатных сценариев.
`Sleep.ReturnToBed()` при активной Blackspace-сессии ждёт полный выход из Blackspace и восстановление предыдущего shaderpack.

Короткая форма для повторяющегося моргания перед входом в сон:

```echoscript
Sleep.Blink(
    count: 4,
    duration: Ticks(8),
    closedWait: 0.12,
    openWait: 0.18,
    sleepyBlink: true,
    showHud: false);
```

`Sleep.Blink(...)` доступен только в линейном `StoryResource.OnStart()`. Компилятор разворачивает один вызов в такую последовательность для каждого повтора:

```echoscript
Sleep.CloseEyes(duration: Ticks(8));
Sleep.WaitUntilClosed();
Sleep.Wait(0.12);
Sleep.OpenEyes(duration: Ticks(8), sleepyBlink: true, showHud: false);
Sleep.WaitUntilOpen();
Sleep.Wait(0.18);
```

Аргументы `Sleep.Blink(...)`:

| Аргумент | По умолчанию | Что делает |
|---|---:|---|
| `count` | обязателен | Сколько раз повторить закрытие-открытие глаз |
| `duration` | `Ticks(8)` | Длительность закрытия и открытия в тиках |
| `closedWait` | `0.12` | Сколько секунд держать глаза закрытыми внутри одного морга |
| `openWait` | `0.18` | Сколько секунд ждать после открытия глаз перед следующим моргом |
| `sleepyBlink` | `true` | Передаётся в `Sleep.OpenEyes(...)` |
| `showHud` | `false` | Передаётся в `Sleep.OpenEyes(...)`; для сна в blackspace обычно оставлять `false` |

Графовая форма для `StoryResource.Nodes()`:

```echoscript
Sleep.CloseEyes("close_eyes", duration: Ticks(18), next: "wait_until_closed");
Sleep.WaitUntilClosed("wait_until_closed", next: "enter_dream");
Sleep.EnterDream(
    "enter_dream",
    "minecraft:overworld",
    0.5,
    64,
    0.5,
    yaw: 180,
    pitch: 0,
    next: "open_eyes");
```

Низкоуровневая форма `Action(..., type: "echo_player_sleep:*")` остаётся fallback для редких случаев, но для обычного сна используй `Sleep.*`.

## Где Что Использовать

| Место | Что доступно | Для Player Sleep |
|---|---|---|
| `StoryResource.Build()` | Только `return Story("id", start: "node")` | Объявляет story id сна |
| `StoryResource.Nodes()` | `Start`, `Set`, `Require`, `Sleep.*`, `Action`, `Finish` | Графовая форма, где у каждой ноды есть id и `next` |
| `StoryResource.OnStart()` | `Story.Set`, `Story.Load`, `Sleep.*`, другие линейные intrinsic-методы | Основная удобная форма для последовательного сна без ручных node id |
| `TriggerResource` поля | Строковые alias-поля, `Area`, shape-поля | Можно хранить `PlayerSleepScenarioId` и `StoryId` |
| `TriggerResource.Init()` | `Id`, `Subscribe`, `StartStory`, политики | Здесь объявляется маршрутизация сна |
| метод последовательности `TriggerResource` | `Wait(...)` и обычные trigger expressions | Для sleep story subscription обычно не нужен |
| обратные вызовы `TriggerResource` | `OnTriggerEnter`, `OnTriggerExit`, `OnTriggerStay` | Не использовать для маршрутизации Player Sleep |
| `Subscribe(...)` | Только в `TriggerResource.Init()` | `Subscribe(PlayerSleep.Confirmed(...))` |
| `PlayerSleep.*(...)` | Только как selector внутри `Subscribe(...)` | Не вызывать из Story |
| `Action(..., type: "echo_player_sleep:*")` | Только в `StoryResource.Nodes()` | Низкоуровневый fallback, обычно заменяется на `Sleep.*` |

## Быстрая Схема

Обычный сон без arm:

```text
Игрок кликает кровать
-> vanilla sleep
-> sleepCount игрока увеличивается на 1
-> PlayerSleepSession не создаётся
-> PlayerSleep.Confirmed не публикуется
-> Trigger не стартует
-> Story не стартует
```

Сюжетный сон:

```text
Trigger adapter на PlayerSleepInBedEvent смотрит следующий sleepCount
-> если есть подходящий Subscribe(PlayerSleep.Confirmed(...)), adapter делает arm для scenario
-> игрок ложится в обычную кровать
-> sleepCount игрока увеличивается на 1
-> создаётся PlayerSleepSession
-> публикуется PlayerSleep.Confirmed(scenario)
-> Trigger subscription находит точный selector
-> adapter запускает Story через StartStory(...)
-> в Story наследуется sleep context
-> Story выполняет Sleep.* вызовы
```

Для обычного сна без подходящего trigger-подписчика счётчик всё ещё увеличивается только после реального vanilla sleeping state.
Для сюжетного сна с подходящим trigger-подписчиком начало запускается сразу на входе в кровать, чтобы Story могла закрывать глаза без задержки.

`echo_player_sleep:arm` не укладывает игрока сам. Он только помечает следующий vanilla bed sleep как потенциально сюжетный.

## StoryResource

### Доступные Методы Класса

В `StoryResource` binder принимает только эти методы:

```echoscript
public override StoryResourceDef Build()
private void Nodes()
private void OnStart()
```

Практически для Player Sleep нужен такой шаблон:

```echoscript
namespace JDH.Chapter1.Sleep;

class FirstNightDreamStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/first_night_dream", start: "start");
    }

    private void OnStart()
    {
        Story.Set("chapter_1.first_night_dream", true);
        Sleep.CloseEyes(duration: Ticks(18));
        Sleep.WaitUntilClosed();
        Sleep.EnterDream("minecraft:overworld", 0.5, 64, 0.5, yaw: 180, pitch: 0);
        Sleep.OpenEyes(duration: Ticks(16), sleepyBlink: true);
        Sleep.WaitUntilOpen();
        Sleep.ReturnToBed();
        Sleep.CompleteNight();
        Sleep.OpenEyes(duration: Ticks(16), sleepyBlink: true);
        Sleep.WaitUntilOpen();
        Sleep.Complete();
    }
}
```

### `Build()`

Доступно только:

```echoscript
return Story("namespace:path", start: "node_id");
```

Пример:

```echoscript
public override StoryResourceDef Build()
{
    return Story("jdh:chapter_1/first_night_dream", start: "start");
}
```

Нельзя объявлять здесь sleep action. `Build()` только задаёт id истории и стартовую ноду.

### `Nodes()`

В `Nodes()` доступны core-ноды:

```text
Start(...)
Set(...)
Require(...)
Sleep.*(...)
Action(...)
Finish(...)
```

Для Player Sleep обычно используется `Sleep.*`:

```echoscript
Sleep.CloseEyes("close_eyes", duration: Ticks(18), next: "wait_until_closed");
Sleep.WaitUntilClosed("wait_until_closed", next: "enter_dream");
Sleep.EnterDream(
    "enter_dream",
    "minecraft:overworld",
    0.5,
    64,
    0.5,
    yaw: 180,
    pitch: 0,
    next: "finish");
```

У графовой формы `Sleep.*` первый positional аргумент — это id ноды, а `next` — следующая нода. Поле `type` писать не нужно: compiler сам подставляет нужный `echo_player_sleep:*`.

Низкоуровневый `Action(...)` остаётся доступен как fallback:

```echoscript
Action(
    "close_eyes",
    type: "echo_player_sleep:close_eyes",
    duration: 18,
    next: "wait_until_closed");
```

### `Start(...)`

Доступно в `StoryResource.Nodes()`.

```echoscript
Start("start", next: "close_eyes");
```

Назначение:

```text
создаёт стартовую ноду
указывает первую рабочую ноду
```

### `Set(...)`

Доступно в `StoryResource.Nodes()`.

```echoscript
Set(
    "mark_seen",
    variable: "jdh.chapter_1.first_night_seen",
    value: true,
    next: "finish");
```

Назначение:

```text
поставить сюжетную переменную состояния
```

### `Require(...)`

Доступно в `StoryResource.Nodes()`.

```echoscript
Require(
    "need_first_night",
    variable: "jdh.chapter_1.can_sleep",
    value: true,
    next: "arm_sleep");
```

Назначение:

```text
проверить переменную перед продолжением story
```

### `Action(...)`

Доступно в `StoryResource.Nodes()`.

Это низкоуровневый вход в Player Sleep runtime. Для обычного authored sleep story предпочитай `Sleep.*`.

```echoscript
Action(
    "sleep_action",
    type: "echo_player_sleep:complete",
    output: "sleep.complete",
    next: "finish");
```

Все поля кроме `type`, `output`, `next` уходят в payload:

```echoscript
Action(
    "enter_dream",
    type: "echo_player_sleep:enter_dream",
    dimension: "minecraft:overworld",
    x: "240.5",
    y: "72",
    z: "-18.5",
    yaw: "180",
    pitch: "0",
    output: "sleep.dream",
    next: "open_eyes");
```

### `Finish(...)`

Доступно в `StoryResource.Nodes()`.

```echoscript
Finish("finish");
```

Назначение:

```text
завершить StoryResource
```

### `OnStart()`

`OnStart()` существует в Story compiler как линейная форма. Для последовательного сна это самый короткий вариант: compiler сам создаёт ноды и связывает их через `next`.

Пример:

```echoscript
private void OnStart()
{
    Story.Set("chapter_1.first_night_dream", true);
    Sleep.CloseEyes(duration: Ticks(18));
    Sleep.WaitUntilClosed();
    Sleep.EnterDream("minecraft:overworld", 0.5, 64, 0.5, yaw: 180, pitch: 0);
    Sleep.OpenEyes(duration: Ticks(16), sleepyBlink: true);
    Sleep.WaitUntilOpen();
    Sleep.Complete();
}
```

Если нужен ручной граф, используй `Nodes()` и форму `Sleep.CloseEyes("node_id", ..., next: "next_node")`.

## TriggerResource

### Доступные Методы Класса

Для sleep subscription нужен минимум:

```echoscript
class FirstNightSleepSubscription : TriggerResource
{
    private void Init()
    {
        Id("jdh:first_night_sleep");
        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"));
        StartStory("jdh:chapter_1/first_night_dream");
        FireOnlyOnce();
    }
}
```

`TriggerResource` обязан иметь `Init()`.

Варианты исполняемости trigger:

```text
1. Start(method) + sequence Wait(...)
2. реактивные обратные вызовы OnTriggerEnter/OnTriggerExit/OnTriggerStay
3. Subscribe(...) + StartStory(...)
```

Для маршрутизации Player Sleep нужен третий вариант.

### Поля В `TriggerResource`

Можно объявлять строковые alias-поля:

```echoscript
private PlayerSleepScenarioId FirstNight = "jdh:chapter_1/first_night";
private StoryId FirstNightDream = "jdh:chapter_1/first_night_dream";
```

Эти поля можно передавать в `Subscribe(...)` и `StartStory(...)`:

```echoscript
Subscribe(PlayerSleep.Confirmed(FirstNight));
StartStory(FirstNightDream);
```

Можно писать inline:

```echoscript
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"));
StartStory("jdh:chapter_1/first_night_dream");
```

### `Init()`

В `TriggerResource.Init()` доступны:

```text
Id(...)
Scope(...)
Mode(...)
Start(...)
Subscribe(...)
StartStory(...)
AllowManualFire()
Cooldown(...)
Dimension(...)
Bind(...)
FireOnlyOnce()
EnterOnlyOnce()
ExitOnlyOnce()
StayOnlyOnce()
FireCount(...)
StayInterval(...)
StayPolicy(...)
```

Для Player Sleep обычно используются:

```text
Id(...)
Subscribe(PlayerSleep.Confirmed(...))
StartStory(...)
FireOnlyOnce()
Cooldown(...)
```

### `Id(...)`

Доступно только в `TriggerResource.Init()`.

```echoscript
Id("jdh:first_night_sleep");
```

Обязательно для любого trigger.

### `Subscribe(...)`

Доступно только в `TriggerResource.Init()`.

Для Player Sleep:

```echoscript
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"));
```

Разрешённые selector-методы:

```echoscript
PlayerSleep.Confirmed(...)
PlayerSleep.EnteredDream(...)
PlayerSleep.Returned(...)
PlayerSleep.Completed(...)
PlayerSleep.Failed(...)
```

Важно:

```text
PlayerSleep.* можно использовать только внутри Subscribe(...)
PlayerSleep.* нельзя использовать в StoryResource
PlayerSleep.* нельзя использовать как обычный method call в Trigger sequence
```

### `StartStory(...)`

Доступно только в `TriggerResource.Init()`.

```echoscript
StartStory("jdh:chapter_1/first_night_dream");
```

Или через alias:

```echoscript
private StoryId FirstNightDream = "jdh:chapter_1/first_night_dream";

private void Init()
{
    StartStory(FirstNightDream);
}
```

Для sleep subscription `Subscribe(...)` и `StartStory(...)` должны идти вместе.

Плохо:

```echoscript
private void Init()
{
    Id("jdh:broken_sleep");
    Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"));
}
```

Плохо:

```echoscript
private void Init()
{
    Id("jdh:broken_sleep");
    StartStory("jdh:chapter_1/first_night_dream");
}
```

Хорошо:

```echoscript
private void Init()
{
    Id("jdh:first_night_sleep");
    Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"));
    StartStory("jdh:chapter_1/first_night_dream");
    FireOnlyOnce();
}
```

### `FireOnlyOnce()`

Доступно в `TriggerResource.Init()`.

```echoscript
FireOnlyOnce();
```

Для первой сюжетной ночи почти всегда нужно:

```text
один scenario
одна успешная подписка
одна Story execution
```

Нельзя смешивать с `FireCount(...)`.

### `Cooldown(...)`

Доступно в `TriggerResource.Init()`.

```echoscript
Cooldown(Ticks(1200));
```

Можно писать ticks напрямую:

```echoscript
Cooldown(1200);
```

Для повторяемых кошмаров:

```echoscript
class RepeatableNightmareSleep : TriggerResource
{
    private void Init()
    {
        Id("jdh:repeatable_nightmare_sleep");
        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/repeatable_nightmare"));
        StartStory("jdh:chapter_1/repeatable_nightmare");
        Cooldown(Ticks(1200));
    }
}
```

### `Scope(...)`

Доступно в `TriggerResource.Init()`.

```echoscript
Scope(PLAYER);
```

Разрешённые значения:

```text
GLOBAL
SERVER
PLAYER
DIALOGUE_SESSION
```

Для Player Sleep owner строится от игрока, поэтому практически используется `PLAYER` или дефолт.

### `Mode(...)`

Доступно в `TriggerResource.Init()`.

```echoscript
Mode(ONCE);
Mode(REPEAT);
```

Для sleep subscription чаще достаточно `FireOnlyOnce()` или `Cooldown(...)`.

### `Start(...)`

Доступно в `TriggerResource.Init()`, но это не основной путь маршрутизации Player Sleep story.

```echoscript
private void Init()
{
    Id("jdh:some_sequence");
    Start(SomeSequence);
}

private void SomeSequence()
{
    Wait(PlayerEnter(SomeArea));
}
```

Для Player Sleep вместо `Start(...)` нужно:

```echoscript
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"));
StartStory("jdh:chapter_1/first_night_dream");
```

### `Wait(...)`

Доступно только в методе последовательности, на который указывает `Start(...)`.

```echoscript
private void SomeSequence()
{
    Wait(PlayerEnter(SomeArea));
    Wait(HasItem("minecraft:stick"));
}
```

Для sleep subscription не нужен.

### Reactive Callbacks

Доступные callback-имена:

```echoscript
private void OnTriggerEnter()
private void OnTriggerExit()
private void OnTriggerStay()
```

Разрешённые сигнатуры:

```echoscript
private void OnTriggerEnter()
private void OnTriggerEnter(PlayerRef player)
private void OnTriggerEnter(TriggerEvent event)
```

Для Player Sleep запрещено:

```echoscript
private void OnPlayerSleepConfirmed(PlayerSleepEvent event)
{
    Story.Start("jdh:chapter_1/first_night_dream");
}
```

Binder специально отклоняет ручные обратные вызовы сна:

```text
Маршрутизация сюжетного сна должна использовать Subscribe(PlayerSleep.*), а не callback-методы.
```

## PlayerSleep Selector Methods

Эти методы доступны только внутри `Subscribe(...)` в `TriggerResource.Init()`.

| Метод | Event kind | Где использовать | Что запускает |
|---|---|---|---|
| `PlayerSleep.Confirmed(scenario)` | `CONFIRMED` | `Subscribe(...)` | Основной маршрут захвата для dream Story |
| `PlayerSleep.EnteredDream(scenario)` | `ENTERED_DREAM` | `Subscribe(...)` | Селектор события входа в сон |
| `PlayerSleep.Returned(scenario)` | `RETURNED` | `Subscribe(...)` | Селектор события возврата |
| `PlayerSleep.Completed(scenario)` | `COMPLETED` | `Subscribe(...)` | Селектор завершения сна |
| `PlayerSleep.Failed(scenario)` | `FAILED` | `Subscribe(...)` | Селектор ошибки сна |

Текущий adapter запускает Story claim flow по `CONFIRMED`:

```java
event.sleepEvent()
        .filter(sleepEvent -> sleepEvent.kind() == PlayerSleepEventKind.CONFIRMED)
        .ifPresent(sleepEvent -> route(sleepEvent, server, true));
```

Все selector-методы компилируются в exact key:

```text
provider = echo_player_sleep:events
eventKind = CONFIRMED / ENTERED_DREAM / RETURNED / COMPLETED / FAILED
scenarioId = namespace:path
```

Пример exact key:

```text
echo_player_sleep:events|CONFIRMED|jdh:chapter_1/first_night
```

### Фильтр По Номеру Сна

Каждый подтверждённый vanilla bed sleep игрока увеличивает его `sleepCount` на 1. Счётчик ведётся по игроку, а не по scenario: первый успешный сон игрока даёт `sleepCount = 1`, пятый успешный сон даёт `sleepCount = 5`.

В Forge runtime счётчик сохраняется в world saved data `echo_player_sleep_counts`, поэтому не сбрасывается при рестарте сервера или перезаходе игрока.

Обычный selector без ограничения по счётчику остаётся wildcard по номеру сна:

```echoscript
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night"));
```

Такой selector сработает на любом подтверждённом сюжетном сне этого scenario.

Ровно пятый сон игрока:

```echoscript
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCount: 5));
```

Пятый и все последующие сны:

```echoscript
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCountAtLeast: 5));
```

Ограничения можно использовать с любым `PlayerSleep.*` selector:

```echoscript
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCount: 5));
Subscribe(PlayerSleep.EnteredDream("jdh:chapter_1/night", sleepCountAtLeast: 5));
Subscribe(PlayerSleep.Returned("jdh:chapter_1/night", sleepCountAtLeast: 5));
Subscribe(PlayerSleep.Completed("jdh:chapter_1/night", sleepCount: 5));
Subscribe(PlayerSleep.Failed("jdh:chapter_1/night", sleepCountAtLeast: 5));
```

Нельзя указывать оба ограничения одновременно:

```echoscript
Subscribe(PlayerSleep.Confirmed(
    "jdh:chapter_1/night",
    sleepCount: 5,
    sleepCountAtLeast: 5));
```

Такой ресурс отклоняется диагностикой `TRIGGER_SLEEP_SELECTOR_INVALID`.

Ключи selector теперь включают ограничение:

```text
echo_player_sleep:events|CONFIRMED|jdh:chapter_1/night
echo_player_sleep:events|CONFIRMED|jdh:chapter_1/night|sleepCount=5
echo_player_sleep:events|CONFIRMED|jdh:chapter_1/night|sleepCountAtLeast=5
```

Если одновременно подходят несколько selector для одного `scenario`, adapter выбирает самый конкретный:

```text
sleepCount       сильнее sleepCountAtLeast
sleepCountAtLeast сильнее selector без ограничения
больший sleepCountAtLeast сильнее меньшего sleepCountAtLeast
```

Это позволяет держать общий маршрут и отдельный маршрут для пятого сна рядом:

```echoscript
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night"));
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCount: 5));
```

На пятом сне будет выбран selector с `sleepCount: 5`. На остальных снах будет выбран selector без ограничения. Конфликт остаётся только если после применения приоритета всё равно осталось несколько одинаково сильных подписок, например две разные подписки `sleepCount: 5` на один и тот же `scenario`.

## Действия Player Sleep

Все sleep actions доступны через:

```echoscript
Action("node_id", type: "echo_player_sleep:<action>", ..., next: "next_node");
```

Только в:

```text
StoryResource.Nodes()
```

Не доступны в:

```text
TriggerResource.Init()
метод последовательности TriggerResource
обратные вызовы TriggerResource
StoryResource.Build()
```

## Таблица Идентификаторов Действий

| Авторское имя | Реальный `type` | Где доступно | Нужен sleep context |
|---|---|---|---|
| `Sleep.Arm` | `echo_player_sleep:arm` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Нет, но нужен `player` и `scenario` |
| `Sleep.Disarm` | `echo_player_sleep:disarm` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Нет, но нужен `player` |
| `Sleep.Blink` | компиляторный shortcut | Только `StoryResource.OnStart()` | Да, как у развёрнутых `Sleep.CloseEyes` / `Sleep.OpenEyes` |
| `Sleep.CloseEyes` | `echo_player_sleep:close_eyes` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Да, если не передан `player` |
| `Sleep.WaitUntilClosed` | `echo_player_sleep:wait_until_closed` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Да |
| `Sleep.EnterDream` | `echo_player_sleep:enter_dream` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Да |
| `Sleep.OpenEyes` | `echo_player_sleep:open_eyes` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Да |
| `Sleep.WaitUntilOpen` | `echo_player_sleep:wait_until_open` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Да |
| `Sleep.Wait` / `Sleep.WaitSeconds` | `echo_player_sleep:wait` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Нет |
| `Sleep.ReturnToBed` | `echo_player_sleep:return_to_bed` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Да |
| `Sleep.CompleteNight` | `echo_player_sleep:complete_night` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Да |
| `Sleep.Complete` | `echo_player_sleep:complete` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Да |
| `Sleep.Cancel` | `echo_player_sleep:cancel` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Да |
| `Sleep.IsActive` | `echo_player_sleep:is_active` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Нужен `player` или context |
| `Sleep.IsArmed` | `echo_player_sleep:is_armed` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Нужен `player` или context |
| `Sleep.IsEyesClosed` | `echo_player_sleep:is_eyes_closed` | `StoryResource.OnStart()` или `StoryResource.Nodes()` | Нужен `player` или context |

## Контекст И Полезная Нагрузка

Action payload читает строки из named arguments:

```text
player
scenario
owner
command
duration
sleepyBlink
dimension
x
y
z
yaw
pitch
```

Если Story запущена через `StartStory(...)` от `PlayerSleep.Confirmed(...)`, adapter добавляет унаследованный контекст:

```text
player
sleep.player
scenario
sleep.scenario
sleep.session
sleep.count
sleep.originDimension
sleep.bedPosition
sleep.bedBlock
sleep.bedProvider
sleep.revision
trigger.subscription
trigger.execution
story.execution
```

Trigger event payload для sleep event также содержит:

```text
sleepCount
sleep.count
```

Оба значения являются строковым представлением одного и того же per-player счётчика сна.

Правило:

```text
Я внутри Story, запущенной sleep subscription:
  можно не передавать player/scenario в session actions

Я запускаю arm Story вручную:
  нужно явно передать player и scenario

Я запускаю close/open/return/complete без sleep claim:
  будет PLAYER_SLEEP_CONTEXT_MISSING или PLAYER_SLEEP_STORY_CLAIM_REQUIRED
```

## `echo_player_sleep:arm`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.Arm(...)
StoryResource.Nodes() -> Sleep.Arm("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Что делает:

```text
помечает игрока для следующего успешного vanilla bed sleep
не кладёт игрока в кровать
не закрывает глаза
не телепортирует
не запускает Story сразу
```

Аргументы:

| Аргумент | Обязателен | Откуда берётся |
|---|---:|---|
| `player` | да | payload или `sleep.player` |
| `scenario` | да | payload или `sleep.scenario` |
| `owner` | нет | payload или fallback command |
| `command` | нет | payload или Story action execution id |

Низкоуровневая форма:

```echoscript
Action(
    "arm_first_night",
    type: "echo_player_sleep:arm",
    player: "00000000-0000-0000-0000-000000000000",
    scenario: "jdh:chapter_1/first_night",
    owner: "story:jdh:chapter_1/arm_first_night",
    command: "arm:first_night:00000000-0000-0000-0000-000000000000",
    output: "sleep.arm",
    next: "finish");
```

Короткая форма:

```echoscript
Sleep.Arm("jdh:chapter_1/first_night");
```

## `echo_player_sleep:disarm`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Аргументы:

| Аргумент | Обязателен |
|---|---:|
| `player` | да |

Пример:

```echoscript
Action(
    "disarm_sleep",
    type: "echo_player_sleep:disarm",
    player: "00000000-0000-0000-0000-000000000000",
    output: "sleep.disarm",
    next: "finish");
```

## `echo_player_sleep:close_eyes`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Обычно вызывается внутри Story, запущенной по `PlayerSleep.Confirmed(...)`.

Аргументы:

| Аргумент | Обязателен | Значение по умолчанию |
|---|---:|---|
| `player` | через context | `sleep.player` |
| `command` | нет | Story action execution id |
| `duration` | нет | `18` ticks |

Пример:

```echoscript
Action(
    "close_eyes",
    type: "echo_player_sleep:close_eyes",
    duration: "18",
    output: "sleep.close",
    next: "wait_until_closed");
```

Состояния:

```text
STORY_CLAIMED -> CLOSING_EYES -> EYES_CLOSED
DREAMING -> CLOSING_FOR_RETURN -> EYES_CLOSED
```

## `echo_player_sleep:wait_until_closed`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Аргументы:

| Аргумент | Обязателен |
|---|---:|
| `player` | через context |
| `command` | нет |

Пример:

```echoscript
Action(
    "wait_until_closed",
    type: "echo_player_sleep:wait_until_closed",
    output: "sleep.closed",
    next: "enter_dream");
```

Используй после `close_eyes`, если следующая нода требует полностью закрытый экран.

## `echo_player_sleep:enter_dream`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Аргументы:

| Аргумент | Обязателен | Описание |
|---|---:|---|
| `player` | через context | игрок |
| `dimension` | да | dimension id |
| `x` | да | координата |
| `y` | да | координата |
| `z` | да | координата |
| `yaw` | да | поворот |
| `pitch` | да | наклон |
| `command` | нет | durable command id |

Пример:

```echoscript
Action(
    "enter_dream",
    type: "echo_player_sleep:enter_dream",
    dimension: "minecraft:overworld",
    x: "240.5",
    y: "72",
    z: "-18.5",
    yaw: "180",
    pitch: "0",
    output: "sleep.dream",
    next: "open_eyes_in_dream");
```

Состояния:

```text
EYES_CLOSED -> ENTERING_DREAM -> DREAMING
```

Частые ошибки:

```text
PLAYER_SLEEP_EYES_NOT_CLOSED
PLAYER_SLEEP_DREAM_DESTINATION_INVALID
PLAYER_SLEEP_DREAM_DIMENSION_INVALID
PLAYER_SLEEP_DREAM_ENTRY_FAILED
```

## `echo_player_sleep:open_eyes`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Аргументы:

| Аргумент | Обязателен | Значение по умолчанию |
|---|---:|---|
| `player` | через context | `sleep.player` |
| `duration` | нет | `16` ticks |
| `sleepyBlink` | нет | `false` |
| `showHud` | нет | `true` |
| `command` | нет | Story action execution id |

Пример:

```echoscript
Action(
    "open_eyes",
    type: "echo_player_sleep:open_eyes",
    duration: "16",
    sleepyBlink: "true",
    showHud: "false",
    output: "sleep.open",
    next: "wait_until_open");
```

`showHud: false` оставляет vanilla HUD и `echo-player-hud` скрытыми после открытия глаз. Для открытия в blackspace используй `showHud: false`, а для финального открытия возле кровати используй `showHud: true`.

## `echo_player_sleep:wait_until_open`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Пример:

```echoscript
Action(
    "wait_until_open",
    type: "echo_player_sleep:wait_until_open",
    output: "sleep.opened",
    next: "next_scene");
```

Используй, если следующая сцена должна начаться после завершения visual transition.

## `echo_player_sleep:wait`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.Wait(...) или Sleep.WaitSeconds(...)
StoryResource.Nodes() -> Sleep.Wait("node_id", ..., next: "next")
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Аргументы:

| Аргумент | Обязателен | Значение по умолчанию |
|---|---:|---|
| `seconds` | нет | если нет `seconds`, используется `duration` или `ticks` |
| `duration` | нет | `20` ticks |
| `ticks` | нет | `duration` |

Примеры:

```echoscript
Sleep.Wait(0.25);
Sleep.WaitSeconds(1.5);
Sleep.Wait(duration: Ticks(10));

Action(
    "wait_half_second",
    type: "echo_player_sleep:wait",
    seconds: "0.5",
    next: "next_scene");
```

## `echo_player_sleep:return_to_bed`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Пример:

```echoscript
Action(
    "return_to_bed",
    type: "echo_player_sleep:return_to_bed",
    output: "sleep.return",
    next: "open_eyes_in_bed");
```

Состояния:

```text
EYES_CLOSED -> RETURNING -> RETURNED_SLEEPING или RETURNED_AWAKE
```

Возврат использует исходный snapshot кровати, сохранённый при vanilla sleep confirmation.

Частые ошибки:

```text
PLAYER_SLEEP_RETURN_ORIGIN_INVALID
PLAYER_SLEEP_SAFE_WAKE_POSITION_NOT_FOUND
PLAYER_SLEEP_RETURN_FAILED
PLAYER_SLEEP_WAKE_FAILED
```

## `echo_player_sleep:complete_night`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Пример:

```echoscript
Action(
    "complete_night",
    type: "echo_player_sleep:complete_night",
    output: "sleep.night",
    next: "complete_sleep");
```

Используй, когда Story явно владеет моментом завершения ночи.

## `echo_player_sleep:complete`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Пример:

```echoscript
Action(
    "complete_sleep",
    type: "echo_player_sleep:complete",
    output: "sleep.complete",
    next: "finish");
```

Финальное состояние:

```text
COMPLETED
```

## `echo_player_sleep:cancel`

Где доступно:

```text
StoryResource.OnStart() -> Sleep.*(...)
StoryResource.Nodes() -> Sleep.*("node_id", ...)
StoryResource.Nodes() -> Action(...) как низкоуровневый fallback
```

Пример:

```echoscript
Action(
    "cancel_sleep",
    type: "echo_player_sleep:cancel",
    output: "sleep.cancel",
    next: "finish");
```

Финальное состояние:

```text
CANCELLED
```

## Действия-Запросы

Действия-запросы доступны через `Action(...)` в `StoryResource.Nodes()`.

### `echo_player_sleep:is_active`

```echoscript
Action(
    "sleep_is_active",
    type: "echo_player_sleep:is_active",
    output: "sleep.active",
    next: "finish");
```

### `echo_player_sleep:is_armed`

```echoscript
Action(
    "sleep_is_armed",
    type: "echo_player_sleep:is_armed",
    player: "00000000-0000-0000-0000-000000000000",
    output: "sleep.armed",
    next: "finish");
```

### `echo_player_sleep:is_eyes_closed`

```echoscript
Action(
    "sleep_is_eyes_closed",
    type: "echo_player_sleep:is_eyes_closed",
    output: "sleep.eyes_closed",
    next: "finish");
```

Результат:

```text
COMPLETED = условие истинно
SKIPPED = условие ложно
```

## Полный Рабочий Комплект

Нужно два ресурса:

```text
TriggerResource: подписывает PlayerSleep.Confirmed(scenario) на Story
StoryResource: выполняет close/open/dream/return/complete
```

### TriggerResource

```echoscript
namespace JDH.Chapter1.Sleep;

class FirstNightSleepSubscription : TriggerResource
{
    private PlayerSleepScenarioId FirstNight = "jdh:chapter_1/first_night";
    private StoryId FirstNightDream = "jdh:chapter_1/first_night_dream";

    private void Init()
    {
        Id("jdh:first_night_sleep");
        Subscribe(PlayerSleep.Confirmed(FirstNight));
        StartStory(FirstNightDream);
        FireOnlyOnce();
    }
}
```

### StoryResource

```echoscript
namespace JDH.Chapter1.Sleep;

class FirstNightDreamStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/first_night_dream", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "close_eyes");

        Action(
            "close_eyes",
            type: "echo_player_sleep:close_eyes",
            duration: "18",
            output: "sleep.close",
            next: "wait_until_closed");

        Action(
            "wait_until_closed",
            type: "echo_player_sleep:wait_until_closed",
            output: "sleep.closed",
            next: "enter_dream");

        Action(
            "enter_dream",
            type: "echo_player_sleep:enter_dream",
            dimension: "minecraft:overworld",
            x: "240.5",
            y: "72",
            z: "-18.5",
            yaw: "180",
            pitch: "0",
            output: "sleep.dream",
            next: "open_eyes_in_dream");

        Action(
            "open_eyes_in_dream",
            type: "echo_player_sleep:open_eyes",
            duration: "16",
            sleepyBlink: "true",
            output: "sleep.open_dream",
            next: "close_eyes_for_return");

        Action(
            "close_eyes_for_return",
            type: "echo_player_sleep:close_eyes",
            duration: "14",
            output: "sleep.close_return",
            next: "wait_until_closed_for_return");

        Action(
            "wait_until_closed_for_return",
            type: "echo_player_sleep:wait_until_closed",
            output: "sleep.closed_return",
            next: "return_to_bed");

        Action(
            "return_to_bed",
            type: "echo_player_sleep:return_to_bed",
            output: "sleep.return",
            next: "open_eyes_in_bed");

        Action(
            "open_eyes_in_bed",
            type: "echo_player_sleep:open_eyes",
            duration: "16",
            sleepyBlink: "true",
            output: "sleep.open_bed",
            next: "complete_night");

        Action(
            "complete_night",
            type: "echo_player_sleep:complete_night",
            output: "sleep.night",
            next: "complete_sleep");

        Action(
            "complete_sleep",
            type: "echo_player_sleep:complete",
            output: "sleep.complete",
            next: "finish");

        Finish("finish");
    }
}
```

## Минимальная Story Для Захваченного Сна

Если не нужен dream teleport:

```echoscript
namespace JDH.Chapter1.Sleep;

class MinimalClaimedSleepStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/minimal_claimed_sleep", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "complete_night");
        Action("complete_night", type: "echo_player_sleep:complete_night", next: "complete");
        Action("complete", type: "echo_player_sleep:complete", next: "finish");
        Finish("finish");
    }
}
```

## Arm Story Вручную

Если Story запускается не через sleep subscription, контекста ещё нет. Поэтому `arm` должен получить `player` и `scenario` явно.

```echoscript
namespace JDH.Chapter1.Sleep;

class ArmFirstNightSleepStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/arm_first_night_sleep", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "arm_sleep");
        Action(
            "arm_sleep",
            type: "echo_player_sleep:arm",
            player: "00000000-0000-0000-0000-000000000000",
            scenario: "jdh:chapter_1/first_night",
            output: "sleep.arm",
            next: "finish");
        Finish("finish");
    }
}
```

## Что Нельзя

Нельзя использовать `PlayerSleep.*` в `StoryResource`:

```echoscript
class BadStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:bad", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "bad");
        PlayerSleep.Confirmed("jdh:chapter_1/first_night");
        Finish("finish");
    }
}
```

Нельзя использовать `Action(...)` в `TriggerResource.Init()`:

```echoscript
class BadTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:bad_trigger");
        Action("close", type: "echo_player_sleep:close_eyes", next: "finish");
    }
}
```

Нельзя писать ручной sleep callback:

```echoscript
class BadManualSleepRoute : TriggerResource
{
    private void Init()
    {
        Id("jdh:bad_manual_sleep");
    }

    private void OnPlayerSleepConfirmed(PlayerSleepEvent event)
    {
        Story.Start("jdh:chapter_1/first_night_dream");
    }
}
```

Нельзя иметь две подписки на один точный селектор:

```echoscript
class FirstNightA : TriggerResource
{
    private void Init()
    {
        Id("jdh:first_night_a");
        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"));
        StartStory("jdh:chapter_1/dream_a");
    }
}
```

```echoscript
class FirstNightB : TriggerResource
{
    private void Init()
    {
        Id("jdh:first_night_b");
        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"));
        StartStory("jdh:chapter_1/dream_b");
    }
}
```

Результат:

```text
PLAYER_SLEEP_STORY_SUBSCRIPTION_CONFLICT
```

## Состояния Сна

Все текущие статусы:

```text
CREATED
WAITING_FOR_VANILLA_SLEEP
VANILLA_SLEEP_CONFIRMED
WAITING_FOR_STORY_CLAIM
STORY_CLAIMED
CLOSING_EYES
EYES_CLOSED
ENTERING_DREAM
DREAMING
CLOSING_FOR_RETURN
RETURNING
RETURNED_SLEEPING
RETURNED_AWAKE
OPENING_EYES
COMPLETED
CANCELLED
FAILED
RECOVERING
```

Нормальный Успешный Путь:

```text
WAITING_FOR_VANILLA_SLEEP
-> VANILLA_SLEEP_CONFIRMED
-> WAITING_FOR_STORY_CLAIM
-> STORY_CLAIMED
-> CLOSING_EYES
-> EYES_CLOSED
-> ENTERING_DREAM
-> DREAMING
-> CLOSING_FOR_RETURN
-> EYES_CLOSED
-> RETURNING
-> RETURNED_SLEEPING или RETURNED_AWAKE
-> OPENING_EYES
-> COMPLETED
```

## Диагностика И Ошибки

Частые ошибки:

| Код | Что означает |
|---|---|
| `PLAYER_SLEEP_SCENARIO_NOT_FOUND` | arm ссылается на незагруженный scenario |
| `PLAYER_SLEEP_ALREADY_ARMED` | у игрока уже есть активный arm |
| `PLAYER_SLEEP_CONTEXT_MISSING` | action запущен без `player/scenario` и без унаследованного контекста |
| `PLAYER_SLEEP_STORY_CLAIM_REQUIRED` | session action вызван до story claim |
| `PLAYER_SLEEP_STORY_SUBSCRIPTION_CONFLICT` | несколько TriggerResource совпали с одним selector |
| `PLAYER_SLEEP_STORY_CLAIM_TIMEOUT` | adapter не смог claim-нуть session за recovery window |
| `PLAYER_SLEEP_EYES_NOT_CLOSED` | `enter_dream` или `return_to_bed` вызван до закрытия глаз |
| `PLAYER_SLEEP_DREAM_DIMENSION_INVALID` | dream dimension невалиден |
| `PLAYER_SLEEP_RETURN_FAILED` | возврат к кровати не выполнен |

Полный список failure-кодов из контракта:

```text
PLAYER_SLEEP_SCENARIO_NOT_FOUND
PLAYER_SLEEP_SCENARIO_ID_CONFLICT
PLAYER_SLEEP_SCENARIO_INVALID
PLAYER_SLEEP_ALREADY_ARMED
PLAYER_SLEEP_NOT_ARMED
PLAYER_SLEEP_ARM_OWNER_MISMATCH
PLAYER_SLEEP_ARM_IDEMPOTENCY_CONFLICT
PLAYER_SLEEP_BED_NOT_SUPPORTED
PLAYER_SLEEP_BED_MISMATCH
PLAYER_SLEEP_BED_REMOVED
PLAYER_SLEEP_BED_OCCUPIED
PLAYER_SLEEP_BED_PROVIDER_NOT_FOUND
PLAYER_SLEEP_BED_PROVIDER_CONFLICT
PLAYER_SLEEP_VANILLA_INTERACTION_FAILED
PLAYER_SLEEP_VANILLA_CONFIRMATION_TIMEOUT
PLAYER_SLEEP_VANILLA_STATE_LOST
PLAYER_SLEEP_NIGHT_SKIP_INTERCEPTION_FAILED
PLAYER_SLEEP_CONTEXT_MISSING
PLAYER_SLEEP_STORY_CLAIM_WAITING
PLAYER_SLEEP_STORY_CLAIM_REQUIRED
PLAYER_SLEEP_STORY_CLAIM_STALE
PLAYER_SLEEP_STORY_CLAIM_CONFLICT
PLAYER_SLEEP_STORY_CLAIM_OWNER_MISMATCH
PLAYER_SLEEP_STORY_CLAIM_FAILED
PLAYER_SLEEP_STORY_CLAIM_TIMEOUT
PLAYER_SLEEP_STORY_CLAIM_RECOVERY_FAILED
PLAYER_SLEEP_STORY_SUBSCRIPTION_NOT_FOUND
PLAYER_SLEEP_STORY_SUBSCRIPTION_CONFLICT
PLAYER_SLEEP_STORY_START_FAILED
PLAYER_SLEEP_TRANSITION_UNAVAILABLE
PLAYER_SLEEP_TRANSITION_REVISION_STALE
PLAYER_SLEEP_CLOSE_ACK_TIMEOUT
PLAYER_SLEEP_OPEN_ACK_TIMEOUT
PLAYER_SLEEP_EYES_NOT_CLOSED
PLAYER_SLEEP_DREAM_DESTINATION_INVALID
PLAYER_SLEEP_DREAM_DIMENSION_INVALID
PLAYER_SLEEP_DREAM_ENTRY_FAILED
PLAYER_SLEEP_RETURN_ORIGIN_INVALID
PLAYER_SLEEP_SAFE_WAKE_POSITION_NOT_FOUND
PLAYER_SLEEP_RETURN_FAILED
PLAYER_SLEEP_WAKE_FAILED
PLAYER_SLEEP_COMMAND_IDEMPOTENCY_CONFLICT
PLAYER_SLEEP_CONTROL_UNAVAILABLE
PLAYER_SLEEP_OWNER_REMOVED
PLAYER_SLEEP_PLAYER_OFFLINE
PLAYER_SLEEP_RECOVERY_FAILED
PLAYER_SLEEP_PERSISTENCE_FAILED
PLAYER_SLEEP_INPUT_RESTORE_FAILED
PLAYER_SLEEP_STATE_TRANSITION_INVALID
TRIGGER_SLEEP_PROVIDER_UNAVAILABLE
TRIGGER_SLEEP_SCENARIO_INVALID
TRIGGER_SLEEP_SELECTOR_STALE
TRIGGER_SLEEP_EVENT_STALE
TRIGGER_SLEEP_EVENT_DUPLICATE
TRIGGER_SLEEP_CONTEXT_INHERITANCE_FAILED
TRIGGER_SLEEP_SUBSCRIPTION_IDEMPOTENCY_CONFLICT
TRIGGER_SLEEP_RESTART_RECOVERY_FAILED
```

## Команды

Текущий command root Player Sleep в Java:

```text
/playersleep arm <scenario>
/playersleep disarm
/playersleep active
/playersleep count
/playersleep count reset
/playersleep diagnostics
```

`/playersleep count` показывает текущий `sleepCount` для игрока, который выполнил команду.
`/playersleep count reset` сбрасывает `sleepCount` игрока, который выполнил команду, и печатает старое значение.

Диагностические команды Echo Sleep:

```text
/echo_sleep arm status <player>
/echo_sleep session inspect <player>
/echo_sleep event inspect <event_id>
/echo_sleep claim inspect <player>
/echo_sleep count <player>
/echo_sleep diagnostics
```

`/echo_sleep count <player>` показывает `sleepCount` выбранного игрока.

Команды Trigger subscription:

```text
/echo_trigger subscriptions sleep
/echo_trigger subscriptions inspect <subscription_id>
/echo_trigger subscriptions conflicts
/echo_trigger diagnostics
```

Ожидаемый вывод active:

```text
session=<session_id> scenario=<scenario_id> status=<status>
```

Ожидаемый вывод count:

```text
Echo Player Sleep count: 5
player=<name> uuid=<uuid> sleepCount=5
```

Ожидаемый вывод sleep subscriptions:

```text
subscription=<subscription_id> trigger=<trigger_id> selector=<provider|event|scenario> story=<story_id> fireOnlyOnce=<true|false>
```

Ожидаемый конфликт:

```text
selector=echo_player_sleep:events|CONFIRMED|jdh:chapter_1/first_night subscriptions=jdh:first_night_a,jdh:first_night_b
```

## Проверочный Сценарий

```text
1. Создать TriggerResource:
   Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"))
   StartStory("jdh:chapter_1/first_night_dream")

2. Создать StoryResource:
   Story("jdh:chapter_1/first_night_dream", start: "start")
   Sleep.Blink(...)
   Sleep.CloseEyes(...)
   Sleep.WaitUntilClosed()
   Sleep.EnterDream(...)
   Sleep.ReturnToBed()
   Sleep.CompleteNight()
   Sleep.OpenEyes(...)
   Sleep.Complete()

3. Проверить:
   /echo_trigger subscriptions sleep
   /echo_trigger subscriptions conflicts

4. Заармить:
   /playersleep arm jdh:chapter_1/first_night

5. Игрок кликает vanilla bed.

6. Проверить:
   /playersleep active
   /playersleep diagnostics
   /echo_trigger diagnostics
```

## Короткая Карта Доступности

```text
StoryResource.Build()
  можно:
    Story("story_id", start: "node")
  нельзя:
    Action(...)
    Subscribe(...)
    PlayerSleep.*

StoryResource.Nodes()
  можно:
    Start(...)
    Set(...)
    Require(...)
    Sleep.CloseEyes("node_id", ..., next: "next_node")
    Sleep.WaitUntilClosed("node_id", next: "next_node")
    Sleep.EnterDream("node_id", ..., next: "next_node")
    Sleep.OpenEyes("node_id", ..., next: "next_node")
    Sleep.ReturnToBed("node_id", next: "next_node")
    Sleep.CompleteNight("node_id", next: "next_node")
    Sleep.Complete("node_id", next: "finish")
    Action(..., type: "echo_player_sleep:*") как fallback
    Finish(...)
  нельзя:
    Subscribe(...)
    StartStory(...)
    PlayerSleep.Confirmed(...)

StoryResource.OnStart()
  можно:
    Story.Set(...)
    Story.Load(...)
    Sleep.Blink(...)
    Sleep.CloseEyes(...)
    Sleep.WaitUntilClosed()
    Sleep.EnterDream(...)
    Sleep.OpenEyes(...)
    Sleep.ReturnToBed()
    Sleep.CompleteNight()
    Sleep.Complete()
  нельзя:
    Subscribe(...)
    StartStory(...)
    PlayerSleep.Confirmed(...)

TriggerResource.Init()
  можно:
    Id(...)
    Subscribe(PlayerSleep.*(...))
    StartStory(...)
    FireOnlyOnce()
    Cooldown(...)
    Scope(...)
    Mode(...)
  нельзя:
    Action(..., type: "echo_player_sleep:*")
    Sleep.CloseEyes(...)

метод последовательности TriggerResource
  можно:
    Wait(...)
  нельзя:
    PlayerSleep.Confirmed(...)
    Action(..., type: "echo_player_sleep:*")

обратные вызовы TriggerResource
  можно:
    OnTriggerEnter/Exit/Stay
  нельзя:
    OnPlayerSleepConfirmed(PlayerSleepEvent event)
```

## Минимальный Набор Для Одного Player Sleep

```echoscript
class FirstNightSleepSubscription : TriggerResource
{
    private void Init()
    {
        Id("jdh:first_night_sleep");
        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/first_night"));
        StartStory("jdh:chapter_1/first_night_dream");
        FireOnlyOnce();
    }
}
```

```echoscript
class FirstNightDreamStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/first_night_dream", start: "start");
    }

    private void OnStart()
    {
        Sleep.CloseEyes(duration: Ticks(18));
        Sleep.WaitUntilClosed();
        Sleep.EnterDream("minecraft:overworld", 240.5, 72, -18.5, yaw: 180, pitch: 0);
        Sleep.OpenEyes(duration: Ticks(16), sleepyBlink: true);
        Sleep.WaitUntilOpen();
        Sleep.CloseEyes(duration: Ticks(14));
        Sleep.WaitUntilClosed();
        Sleep.ReturnToBed();
        Sleep.CompleteNight();
        Sleep.OpenEyes(duration: Ticks(16), sleepyBlink: true);
        Sleep.WaitUntilOpen();
        Sleep.Complete();
    }
}
```
