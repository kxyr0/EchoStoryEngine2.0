# Echo Time DSL — полный русский справочник

Документ описывает фактически работающую систему `echo-time` в текущей сборке JDHSeason: главный сюжетный таймер, DSL-команды для Story и Dialogue, временные TriggerResource, HUD, консольные команды, сохранение состояния и взаимодействие с ванильным временем Minecraft.

Актуально для сборок `echo-time 0.2.0` и `echo-trigger-time-adapter 0.2.0`.

> Самый короткий ответ на главный вопрос: при `SyncSun(true)` Echo Time **сам выключает** ванильный `doDaylightCycle`, а затем каждый серверный тик напрямую выставляет положение солнца из сюжетного времени. Поэтому заранее установленный `doDaylightCycle false` Echo Time не останавливает. Наоборот, именно такое состояние необходимо, чтобы ванильный цикл не соревновался с Echo Time. `doWeatherCycle` Echo Time не меняет: погода и сюжетные часы независимы.

## Содержание

1. [Что такое Echo Time](#1-что-такое-echo-time)
2. [Быстрый старт](#2-быстрый-старт)
3. [Где лежат файлы](#3-где-лежат-файлы)
4. [Базовые правила DSL](#4-базовые-правила-dsl)
5. [Конфигурация главных часов](#5-конфигурация-главных-часов)
6. [Как часы идут на самом деле](#6-как-часы-идут-на-самом-деле)
7. [Полный API `Time.*`](#7-полный-api-time)
8. [Политика пересечённых событий](#8-политика-пересечённых-событий)
9. [Использование в DialogueResource](#9-использование-в-dialogueresource)
10. [Использование в StoryResource](#10-использование-в-storyresource)
11. [Временные триггеры `Time.At`](#11-временные-триггеры-timeat)
12. [Паузы, ожидания и проверки времени](#12-паузы-ожидания-и-проверки-времени)
13. [HUD времени](#13-hud-времени)
14. [Консольные команды](#14-консольные-команды)
15. [Солнце, луна, gamerule и погода](#15-солнце-луна-gamerule-и-погода)
16. [Сохранение, перезапуск и hot reload](#16-сохранение-перезапуск-и-hot-reload)
17. [Готовые рецепты](#17-готовые-рецепты)
18. [Ошибки и диагностика](#18-ошибки-и-диагностика)
19. [Частые вопросы](#19-частые-вопросы)
20. [Краткая шпаргалка](#20-краткая-шпаргалка)
21. [Текущие ограничения](#21-текущие-ограничения)

---

## 1. Что такое Echo Time

Echo Time — это отдельные серверные сюжетные часы. Они не читают ванильный `dayTime` как источник истины. Источник истины выглядит так:

```text
Echo Day + HH:mm:ss.ffffff
```

Например:

```text
Day 1 08:30:00
Day 1 09:50:00
Day 2 00:05:00
```

Главные свойства:

- часы едины для всего сервера;
- день начинается с `Day 1`, а не с нулевого дня;
- время хранится с микросекундной точностью;
- DSL принимает время суток в строгом формате `HH:mm`;
- скорость задаётся отношением игровых минут к реальным секундам;
- прогресс считается по серверным simulation tick, а не по системным часам компьютера;
- состояние сохраняется в мире;
- Story, Dialogue, Trigger и HUD читают одну и ту же авторитетную временную шкалу;
- ванильное солнце является проекцией Echo Time, если включён `SyncSun(true)`;
- триггеры работают по пересечению временной границы, поэтому высокая скорость не должна пропускать `09:50`;
- повторное восстановление одной и той же Story/Dialogue-команды не применяет мутацию второй раз.

### Глобальность часов

Echo Time не создаёт отдельное время:

- для каждого игрока;
- для каждого измерения;
- для каждого NPC;
- для каждого чанка;
- для каждой Story или Dialogue.

Все используют один серверный `EchoInstant`.

### Что является источником истины

```text
Echo Time state
  ├─ Story Time.*
  ├─ Dialogue Time.*
  ├─ Time.At triggers
  ├─ HUD snapshot
  └─ vanilla dayTime projection
```

Из этого следует важное правило:

> При `SyncSun(true)` не используй `/time set` для сюжетного времени. Echo Time перезапишет ванильный `dayTime` на следующей синхронизации. Используй `Time.Set(...)` или `/echo_time set ...`.

---

## 2. Быстрый старт

### 2.1. Основная конфигурация

Файл:

```text
<gameDir>/story/configs/jdh/echo_time/time-speed.es
```

Рабочий пример:

```csharp
namespace JDH.Config.Time;

class MainTimeSpeed : TimeSpeedConfigResource
{
    private void Init()
    {
        Id("jdh:main_time");
        InitialDay(1);
        InitialTime("08:30");

        // Одна игровая минута за одну реальную секунду.
        Speed(gameMinutes: 1.0, realSeconds: 1.0);

        Running(true);
        SyncSun(true);
        SyncMoon(true);
        SunDimension("minecraft:overworld");
    }
}
```

### 2.2. Запустить диалог ежедневно в 09:50

Файл с расширением `.trigger.es`:

```csharp
namespace JDH.Triggers.Time;

class UniversityRememberTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:university_remember_trigger");
        Subscribe(Time.At("09:50"));
        StartDialogue("jdh:university_remember");
    }
}
```

### 2.3. Установить 22:30 из диалога или Story

```csharp
Time.Set("22:30");
```

Да, это правильный DSL-код. Он устанавливает `22:30` на **текущем сюжетном дне**.

Чтобы установить конкретный день:

```csharp
Time.Set(Day(2), "22:30");
```

### 2.4. Добавить время

```csharp
Time.Add(Minutes(30));
Time.Add(Hours(2));
```

### 2.5. Подождать до следующего 10:00

```csharp
Time.WaitUntil("10:00");
```

### 2.6. Сбросить часы к `InitialDay` + `InitialTime`

В Minecraft-команде кавычки не нужны:

```text
/echo_time reset
```

Текущая конфигурация выше сбросит часы на:

```text
Day 1 08:30
```

---

## 3. Где лежат файлы

### 3.1. Главная конфигурация Echo Time

Относительно папки Minecraft:

```text
story/configs/jdh/echo_time/time-speed.es
```

В текущем PrismLauncher-инстансе:

```text
C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/story/configs/jdh/echo_time/time-speed.es
```

### 3.2. Конфигурация HUD

```text
story/configs/jdh/echo_time_hud/hud.es
```

### 3.3. Временные триггеры

Каноническая внешняя папка:

```text
story/triggers/**/*.trigger.es
```

Например:

```text
story/triggers/university/university_remember.trigger.es
```

Также discovery принимает `.trigger.es`, находящиеся внутри сегмента:

```text
echo_trigger/triggers
```

в content pack. Каноническая структура:

```text
story/content-pack/jdh/data/jdh/echo_trigger/triggers/chapter_1/example.trigger.es
```

Для новых внешних триггеров проще и нагляднее использовать `story/triggers`.

### 3.4. Диалоги

Обычная структура content pack:

```text
story/content-pack/jdh/data/jdh/echo_dialogue/dialogues/**/*.dialogue.es
```

Например:

```text
story/content-pack/jdh/data/jdh/echo_dialogue/dialogues/chapter_1/university/university_remember.dialogue.es
```

### 3.5. Story

Обычная структура content pack:

```text
story/content-pack/jdh/data/jdh/echo_story/stories/**/*.story.es
```

### 3.6. Сохранённое состояние мира

Главные часы сохраняются в серверном `SavedData` мира под именем:

```text
echo_time_clock.dat
```

Временные trigger-delivery сохраняются отдельно:

```text
echo_trigger_time_deliveries.dat
```

Это внутренние файлы состояния. Не редактируй их вручную при запущенном сервере.

---

## 4. Базовые правила DSL

### 4.1. Формат времени

Во всех DSL-вызовах используется строгая 24-часовая строка:

```text
HH:mm
```

Правильно:

```csharp
"00:00"
"08:30"
"09:50"
"22:30"
"23:59"
```

Неправильно:

```csharp
"0:00"    // нужен ведущий ноль
"8:30"    // нужен ведущий ноль
"24:00"   // максимальный час — 23
"10:60"   // максимальная минута — 59
"09.50"   // разделитель должен быть двоеточием
"09:50:00" // DSL принимает только HH:mm
```

### 4.2. DSL и slash-команда — не одно и то же

В DSL строка обязательно пишется в кавычках:

```csharp
Time.Set("08:30");
```

В чате Minecraft кавычки не пишутся:

```text
/echo_time set 08:30
```

Не надо так:

```text
/echo_time set "08:30"
```

### 4.3. Формат дня

День передаётся специальным выражением `Day(n)`:

```csharp
Day(1)
Day(2)
Day(100)
```

День должен быть положительным. `Day(0)` и отрицательные дни недопустимы.

### 4.4. Формат длительности

DSL поддерживает:

```csharp
Minutes(10)
Hours(2)
```

Длительность не может быть отрицательной.

Правильно:

```csharp
Time.Add(Minutes(0));
Time.Add(Minutes(15));
Time.Add(Hours(8));
```

Неправильно:

```csharp
Time.Add(Minutes(-5));
Time.Add(Seconds(30)); // Seconds сейчас не является поддерживаемой DSL-единицей
Time.Add(Days(1));     // Days сейчас не является поддерживаемой DSL-единицей
```

Сутки можно выразить часами:

```csharp
Time.Add(Hours(24));
```

### 4.5. Идентификаторы ресурсов

Используй namespaced id:

```text
namespace:path
```

Примеры:

```csharp
Id("jdh:main_time");
StartDialogue("jdh:university_remember");
StartStory("jdh:chapter_1/morning_story");
```

### 4.6. Символы без кавычек

Некоторые значения DSL являются символами, а не строками:

```csharp
crossed: FIRE_CROSSED
crossed: SKIP_CROSSED
Anchor(TOP_RIGHT)
ScreenPolicy(SHOW_DURING_GAME_SCREENS)
```

Не заключай такие значения в кавычки.

---

## 5. Конфигурация главных часов

### 5.1. Обязательная форма файла

Конфигурационный binder требует:

- namespace строго `JDH.Config.Time`;
- базовый класс строго `TimeSpeedConfigResource`;
- ровно один член класса;
- этим членом должен быть `private void Init()` без параметров;
- внутри `Init()` разрешены только конфигурационные вызовы.

Канонический шаблон:

```csharp
namespace JDH.Config.Time;

class MainTimeSpeed : TimeSpeedConfigResource
{
    private void Init()
    {
        Id("jdh:main_time");
        InitialDay(1);
        InitialTime("08:30");
        Speed(gameMinutes: 1.0, realSeconds: 1.0);
        Running(true);
        SyncSun(true);
        SyncMoon(true);
        SunDimension("minecraft:overworld");
    }
}
```

Нельзя добавлять в этот класс `Build()`, `Nodes()`, поля, вспомогательные методы или произвольные выражения.

### 5.2. `Id(...)`

```csharp
Id("jdh:main_time");
```

Обязательный параметр. Строка не может быть пустой.

### 5.3. `InitialDay(...)`

```csharp
InitialDay(1);
```

Обязательный параметр. Минимальное значение — `1`.

Это начальный день **нового** состояния мира и целевой день команды `/echo_time reset`.

Изменение `InitialDay(...)` не переписывает текущий сохранённый день автоматически.

### 5.4. `InitialTime(...)`

```csharp
InitialTime("08:30");
```

Обязательный параметр в строгом формате `HH:mm`.

Это начальное время **нового** состояния мира и целевое время `/echo_time reset`.

Изменение `InitialTime(...)` не является автоматическим reset существующего мира.

### 5.5. `Speed(...)`

```csharp
Speed(gameMinutes: 1.0, realSeconds: 1.0);
```

Оба именованных аргумента обязательны:

| Аргумент | Ограничение | Значение |
|---|---:|---|
| `gameMinutes` | `>= 0` | сколько сюжетных минут должно пройти |
| `realSeconds` | `> 0` | за сколько реальных секунд simulation time |

Примеры:

```csharp
// 1 игровая минута за 1 реальную секунду.
Speed(gameMinutes: 1.0, realSeconds: 1.0);

// 1 игровая минута за 5 реальных секунд.
Speed(gameMinutes: 1.0, realSeconds: 5.0);

// 10 игровых минут за 1 реальную секунду.
Speed(gameMinutes: 10.0, realSeconds: 1.0);

// 30 игровых минут за 60 реальных секунд.
Speed(gameMinutes: 30.0, realSeconds: 60.0);

// Автоматический ход часов равен нулю.
Speed(gameMinutes: 0.0, realSeconds: 1.0);
```

Расчёт для автора:

```text
реальных секунд на игровые сутки
= 1440 / gameMinutes * realSeconds
```

Для `1 игровая минута / 1 реальная секунда`:

```text
1440 реальных секунд = 24 реальные минуты на одни игровые сутки
```

Для `1 игровая минута / 5 реальных секунд`:

```text
7200 реальных секунд = 120 реальных минут на одни игровые сутки
```

Слишком большая скорость, превосходящая один игровой день за один серверный тик, отклоняется с ошибкой:

```text
ECHO_TIME_RATE_TOO_HIGH
```

### 5.6. `Running(...)`

```csharp
Running(true);
```

Значения:

```csharp
Running(true);  // автоматический ход разрешён
Running(false); // автоматический ход выключен
```

Если вызов пропущен, значение по умолчанию — `true`.

`Running(false)` не запрещает явные `Time.Set(...)` и `Time.Add(...)`. Оно останавливает автоматическое продвижение по серверным тикам.

### 5.7. `SyncSun(...)`

```csharp
SyncSun(true);
```

При `true`:

- Echo Time захватывает управление ванильным циклом дня;
- сохраняет предыдущее значение `doDaylightCycle`;
- выставляет `doDaylightCycle false`;
- каждый серверный тик проецирует Echo Time в `dayTime` указанных измерений;
- если кто-то снова включит `doDaylightCycle true`, Echo Time обнаружит конфликт и выключит его обратно.

При `false` Echo Time не выставляет ванильный `dayTime`.

Значение по умолчанию — `true`.

### 5.8. `SyncMoon(...)`

```csharp
SyncMoon(true);
```

Параметр сохраняется в конфигурации. В текущей реализации отдельного независимого контроллера луны нет: при абсолютной проекции `dayTime` через `SyncSun(true)` фаза луны естественно следует за последовательностью ванильных дней.

Практические следствия:

- `SyncSun(true)` двигает и дневную фазу, и ванильную лунную фазу через единый абсолютный `dayTime`;
- `SyncMoon(false)` сейчас не создаёт отдельно замороженную луну;
- `SyncSun(false)` отключает всю проекцию `dayTime`, независимо от `SyncMoon`.

Значение по умолчанию — `true`.

### 5.9. `SunDimension(...)`

```csharp
SunDimension("minecraft:overworld");
```

Указывает измерение, куда Echo Time должен проецировать ванильный `dayTime`.

По умолчанию список содержит:

```text
minecraft:overworld
```

Первый явный вызов очищает значение по умолчанию. Последующие вызовы добавляют измерения:

```csharp
SunDimension("minecraft:overworld");
SunDimension("jdh:memory_world");
```

Если измерение не существует или не загружено сервером, оно пропускается.

### 5.10. Минимальная валидная конфигурация

Поскольку `Running`, `SyncSun`, `SyncMoon` и Overworld имеют значения по умолчанию, минимальный файл выглядит так:

```csharp
namespace JDH.Config.Time;

class MainTimeSpeed : TimeSpeedConfigResource
{
    private void Init()
    {
        Id("jdh:main_time");
        InitialDay(1);
        InitialTime("08:30");
        Speed(gameMinutes: 1.0, realSeconds: 1.0);
    }
}
```

Для читаемости production-конфига рекомендуется всё равно писать режимы явно.

### 5.11. Пример медленных часов

```csharp
namespace JDH.Config.Time;

class SlowStoryClock : TimeSpeedConfigResource
{
    private void Init()
    {
        Id("jdh:slow_story_clock");
        InitialDay(1);
        InitialTime("07:00");
        Speed(gameMinutes: 1.0, realSeconds: 10.0);
        Running(true);
        SyncSun(true);
        SyncMoon(true);
        SunDimension("minecraft:overworld");
    }
}
```

### 5.12. Пример полностью управляемых сценарием часов

```csharp
namespace JDH.Config.Time;

class ScriptDrivenClock : TimeSpeedConfigResource
{
    private void Init()
    {
        Id("jdh:script_driven_clock");
        InitialDay(1);
        InitialTime("08:30");
        Speed(gameMinutes: 1.0, realSeconds: 1.0);
        Running(false);
        SyncSun(true);
        SyncMoon(true);
        SunDimension("minecraft:overworld");
    }
}
```

Здесь часы сами не идут, но Story/Dialogue всё ещё могут выполнять `Time.Set` и `Time.Add`, а солнце будет немедленно следовать результату.

---

## 6. Как часы идут на самом деле

### 6.1. Simulation tick, а не wall clock

Echo Time обновляется от тиков Minecraft-сервера.

Это означает:

- при нормальных 20 TPS скорость соответствует конфигу;
- при лаге сервера сюжетные часы замедляются вместе с симуляцией;
- при остановленном сервере время не идёт;
- после запуска нет догоняющего offline-прогресса;
- при паузе integrated server в одиночной игре сюжетные часы тоже не догоняют прошедшее реальное время.

### 6.2. Почему нет накопления ошибки

Скорость компилируется в точное рациональное отношение и считается fixed-point аккумулятором. Дробная часть не теряется каждый тик, а сохраняется в remainder.

Это позволяет задавать, например:

```csharp
Speed(gameMinutes: 1.0, realSeconds: 3.0);
```

без постоянного округления до грубого целого числа игровых секунд на тик.

### 6.3. Переход через полночь

Clock хранит абсолютный instant. Поэтому:

```text
Day 1 23:50 + 20 минут = Day 2 00:10
```

DSL:

```csharp
Time.Set(Day(1), "23:50");
Time.Add(Minutes(20));
```

### 6.4. Игроки и чанки

Главные часы не привязаны к игроку или чанку.

- Выгрузка чанка не останавливает часы.
- Отсутствие игроков на dedicated server не останавливает часы, если сам сервер продолжает тикать.
- Echo Time не force-load-ит чанки.
- NPC или действия, требующие загруженного мира, должны сами учитывать доступность своих объектов.

### 6.5. Серверная авторитетность

Мутации выполняются на сервере, сохраняются, а затем клиентам рассылаются snapshots.

HUD не ведёт отдельные часы. Он показывает последний серверный snapshot.

Текущая периодическая рассылка выполняется каждые `20` серверных тиков, то есть примерно раз в одну секунду при 20 TPS. Дополнительные snapshots отправляются при важных изменениях и по `/echo_time resync`.

---

## 7. Полный API `Time.*`

### 7.1. Таблица методов

| Метод | Dialogue | Story linear | Story graph | Назначение |
|---|:---:|:---:|:---:|---|
| `Time.Set("HH:mm")` | да | да | да | установить время текущего дня |
| `Time.Set(Day(n), "HH:mm")` | да | да | да | установить абсолютный день и время |
| `Time.Add(Minutes(n))` | да | да | да | добавить минуты |
| `Time.Add(Hours(n))` | да | да | да | добавить часы |
| `Time.Pause()` | да | да | да | взять lease паузы |
| `Time.Resume()` | да | да | да | освободить lease текущего владельца |
| `Time.WaitUntil("HH:mm")` | да | да | да | неблокирующе ждать сегодня или завтра |
| `Time.Is("HH:mm")` | да, но результат не ветвит диалог | да | да | проверить текущую минуту |
| `Time.IsBefore("HH:mm")` | да, но результат не ветвит диалог | да | да | строго раньше указанного instant |
| `Time.IsAfter("HH:mm")` | да, но результат не ветвит диалог | да | да | строго позже указанного instant |
| `Time.IsBetween("HH:mm", "HH:mm")` | да, но результат не ветвит диалог | да | да | попасть в интервал, включая ночной |

### 7.2. `Time.Set("HH:mm")`

```csharp
Time.Set("22:30");
```

Устанавливает время на текущем сюжетном дне.

Если сейчас:

```text
Day 4 10:00
```

результат:

```text
Day 4 22:30
```

Если сейчас `Day 4 23:00`, тот же вызов перемотает часы назад в пределах `Day 4`:

```text
Day 4 22:30
```

Политика пересечений по умолчанию:

```text
SKIP_CROSSED
```

### 7.3. `Time.Set(Day(n), "HH:mm")`

```csharp
Time.Set(Day(2), "08:30");
```

Устанавливает абсолютный сюжетный instant:

```text
Day 2 08:30
```

Можно прыгать вперёд и назад. По умолчанию пересечённые временные триггеры не запускаются.

### 7.4. `Time.Set(..., crossed: FIRE_CROSSED)`

```csharp
Time.Set("18:00", crossed: FIRE_CROSSED);
Time.Set(Day(3), "08:00", crossed: FIRE_CROSSED);
```

При прыжке вперёд запускает подходящие временные события между старым и новым instant.

При прыжке назад события не запускаются: у обратного интервала нет прямых пересечений.

### 7.5. `Time.Set(..., crossed: SKIP_CROSSED)`

```csharp
Time.Set("18:00", crossed: SKIP_CROSSED);
```

Явная запись поведения по умолчанию: часы меняются, пропущенные временные точки не воспроизводятся.

### 7.6. `Time.Add(Minutes(n))`

```csharp
Time.Add(Minutes(10));
```

Добавляет длительность к абсолютному instant.

Политика по умолчанию:

```text
FIRE_CROSSED
```

То есть если между началом и концом есть `Time.At(...)`, эти события обрабатываются.

### 7.7. `Time.Add(Hours(n))`

```csharp
Time.Add(Hours(2));
```

Пример:

```text
Day 1 23:30 + 2 часа = Day 2 01:30
```

### 7.8. `Time.Add(..., crossed: SKIP_CROSSED)`

```csharp
Time.Add(Hours(8), crossed: SKIP_CROSSED);
```

Полезно для сна или монтажного пропуска, когда автор не хочет запускать все события ночного расписания.

### 7.9. `Time.Add(..., crossed: FIRE_CROSSED)`

```csharp
Time.Add(Hours(2), crossed: FIRE_CROSSED);
```

Это явная запись поведения `Time.Add` по умолчанию.

### 7.10. `Time.Pause()`

```csharp
Time.Pause();
```

Останавливает автоматический ход Echo Time для lease-владельца текущего выполнения.

Не останавливает:

- серверные тики;
- Minecraft как игру;
- диалоги;
- Story;
- NPC-анимации;
- явные `Time.Set` и `Time.Add`.

### 7.11. `Time.Resume()`

```csharp
Time.Resume();
```

Освобождает паузу, принадлежащую текущему выполнению. Это не команда «стереть вообще все паузы на сервере».

### 7.12. `Time.WaitUntil("HH:mm")`

```csharp
Time.WaitUntil("10:00");
```

Правило выбора цели:

- если сейчас раньше `10:00`, ждать `10:00` сегодня;
- если сейчас ровно `10:00:00`, завершиться немедленно;
- если сейчас позже `10:00`, ждать `10:00` следующего сюжетного дня.

Ожидание durable и не блокирует серверный поток.

### 7.13. `Time.Is("HH:mm")`

```csharp
Time.Is("09:50");
```

Возвращает `true` в течение всей канонической минуты `09:50`, то есть от `09:50:00` включительно до `09:51:00` исключительно.

### 7.14. `Time.IsBefore("HH:mm")`

```csharp
Time.IsBefore("10:00");
```

Сравнивает точный внутренний instant времени суток со значением `10:00:00`.

- `09:59:59` → `true`;
- `10:00:00` → `false`;
- `10:00:30` → `false`.

### 7.15. `Time.IsAfter("HH:mm")`

```csharp
Time.IsAfter("10:00");
```

- `09:59:59` → `false`;
- `10:00:00` → `false`;
- `10:00:30` → `true`.

Из-за этого внутри минуты `10:00` одновременно могут быть истинны `Time.Is("10:00")` и `Time.IsAfter("10:00")`, если внутренние секунды уже больше нуля.

### 7.16. `Time.IsBetween(start, end)`

Обычный дневной интервал:

```csharp
Time.IsBetween("09:00", "17:00");
```

Ночной интервал через полночь:

```csharp
Time.IsBetween("23:00", "01:00");
```

Для ночного интервала истинны:

```text
23:00 и позже
ИЛИ
01:00 и раньше
```

Границы включены как точные instant. Так как строка задаёт начало минуты, `01:00:30` уже позже конечной границы `01:00:00`.

---

## 8. Политика пересечённых событий

### 8.1. Зачем она существует

Пусть есть триггеры:

```text
09:50 — напоминание
10:00 — начало пары
10:30 — звонок
```

Если сценарий мгновенно меняет время с `09:00` на `11:00`, система должна знать, следует ли запускать события, через которые произошёл прыжок.

Для этого существуют два символа:

```csharp
FIRE_CROSSED
SKIP_CROSSED
```

### 8.2. `FIRE_CROSSED`

```csharp
Time.Add(Hours(2), crossed: FIRE_CROSSED);
```

Запускает допустимые временные события в интервале:

```text
(старый instant, новый instant]
```

То есть начало исключено, конец включён.

### 8.3. `SKIP_CROSSED`

```csharp
Time.Set("11:00", crossed: SKIP_CROSSED);
```

Меняет часы, но не публикует пересечённые occurrence.

### 8.4. Значения по умолчанию

| Команда | По умолчанию | Почему |
|---|---|---|
| `Time.Set(...)` | `SKIP_CROSSED` | Set обычно является режиссёрской установкой состояния |
| `Time.Add(...)` | `FIRE_CROSSED` | Add обычно изображает реально прошедшее сюжетное время |

### 8.5. Естественный ход времени

Обычное автоматическое продвижение всегда обрабатывает пересечения. Триггер не зависит от того, совпал ли тик ровно с `09:50:00`.

Например, если один тик перевёл часы:

```text
09:49:58 -> 09:50:01
```

граница `09:50` считается пересечённой.

### 8.6. Прыжок назад

```csharp
Time.Set("08:00", crossed: FIRE_CROSSED);
```

Если до вызова было `10:00`, обратный переход не запускает события `08:00..10:00`. Crossing применяется только к движению вперёд.

### 8.7. Защита от слишком большого количества событий

Текущий предел одной мутации — `1024` пересечённых события.

Если один `Set` или `Add` должен породить больше, мутация отклоняется атомарно с ошибкой:

```text
ECHO_TIME_CROSSED_EVENT_LIMIT
```

Часы не переводятся частично и не запускают произвольный обрезанный набор событий.

### 8.8. Как выбрать политику

Используй `FIRE_CROSSED`, если:

- персонаж реально прожил указанное время;
- должны сработать звонки, расписания и дедлайны;
- короткое ускорение должно быть эквивалентно естественному ходу.

Используй `SKIP_CROSSED`, если:

- это монтажный переход;
- загружается глава в заранее заданном состоянии;
- сон не должен проигрывать все ночные диалоги;
- отладочный переход не должен менять сюжетные доставки.

---

## 9. Использование в DialogueResource

Команды `Time.*` можно размещать в последовательности команд узла диалога между `Line`, `Choice`, `Complete` и другими поддерживаемыми командами.

### 9.1. Полный диалог, устанавливающий 22:30

Ниже форма `DialogueResource`, используемая в текущем content pack:

```csharp
namespace JDH.Chapter1.Home;

class EveningDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ Evening() ];

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:evening_dialogue");
    }

    private void Init()
    {
        Id("jdh:evening_dialogue");
        Start("evening");

        Alias(
            "john",
            "jdh:john",
            display: "Лололошка",
            nameColor: nameColor("#FC9004"),
            visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef Evening()
    {
        var node = new Node("evening");

        Line("john", "Уже поздно...", thought: true);
        Time.Set("22:30");
        Line("john", "Нужно возвращаться домой.", thought: true);

        Complete("jdh:evening_dialogue");
        return node;
    }
}
```

После выполнения `Time.Set("22:30")`:

- серверные сюжетные часы станут `22:30` текущего дня;
- состояние сохранится;
- при `SyncSun(true)` ванильное небо перейдёт к соответствующей фазе;
- HUD получит обновлённый snapshot;
- пересечённые события по умолчанию будут пропущены.

### 9.2. Диалог, добавляющий время

```csharp
private NodeDef Travel()
{
    var node = new Node("travel");

    Line("john", "Дорога заняла около двух часов.", thought: true);
    Time.Add(Hours(2));
    Line("john", "Наконец-то я пришёл.", thought: true);

    Complete("jdh:travel_done");
    return node;
}
```

Здесь пересечённые `Time.At` по умолчанию запускаются.

### 9.3. Монтажный пропуск без событий

```csharp
Time.Add(Hours(6), crossed: SKIP_CROSSED);
```

Полный узел:

```csharp
private NodeDef SleepMontage()
{
    var node = new Node("sleep_montage");

    Line("john", "Нужно хотя бы немного поспать...", thought: true);
    Time.Add(Hours(6), crossed: SKIP_CROSSED);
    Line("john", "Уже утро.", thought: true);

    Complete("jdh:sleep_montage_done");
    return node;
}
```

### 9.4. Пауза времени на время разговора

```csharp
private NodeDef ImportantConversation()
{
    var node = new Node("important_conversation");

    Time.Pause();

    Line("john", "Подожди. Мне нужно всё понять.", thought: true);
    Line("john", "Расскажи с самого начала.");
    Line("john", "Теперь ясно.", thought: true);

    Time.Resume();

    Complete("jdh:important_conversation_done");
    return node;
}
```

Важно:

- это пауза только автоматического сюжетного clock progression;
- сам диалог продолжает работать;
- если другой владелец тоже держит pause lease, одного `Resume()` недостаточно для полного запуска часов;
- при штатном терминальном завершении диалога его pause lease очищается автоматически;
- всё равно рекомендуется явно писать `Time.Resume()` там, где это логически ожидается.

### 9.5. Диалог, ожидающий время

```csharp
private NodeDef WaitForTen()
{
    var node = new Node("wait_for_ten");

    Line("john", "Остаётся дождаться десяти.", thought: true);
    Time.WaitUntil("10:00");
    Line("john", "Пора.", thought: true);

    Complete("jdh:wait_for_ten_done");
    return node;
}
```

Этот вызов приостанавливает последовательность диалога, а не серверный поток. Остальная игра продолжает тикать.

### 9.6. Проверки времени в Dialogue

Компилятор принимает:

```csharp
Time.Is("09:50");
Time.IsBefore("10:00");
Time.IsAfter("08:00");
Time.IsBetween("23:00", "01:00");
```

Но в текущем Dialogue DSL результат внешней команды не является готовой конструкцией `if/else`. Один вызов проверки сам по себе не пропускает и не выбирает `Line`.

Для ветвления диалога используй поддерживаемые самим DialogueResource условия/переменные или вынеси проверку в Story, где результат `value` можно связать с переменной выполнения.

### 9.7. Альтернативная компактная форма Dialogue

Текущий компилятор также тестирует компактную форму метода-узла:

```csharp
namespace JDH.Chapter1.Home;

class LateDialogue : DialogueResource
{
    private void Init()
    {
        Id("jdh:late_dialogue");
        Start(Greeting);
        Alias("john", "jdh:john");
    }

    private void Greeting()
    {
        Line("john", "Опоздали.");
        Time.Set(Day(2), "08:30");
        Time.Add(Minutes(5));
        Time.Pause();
        Time.Resume();
        Time.WaitUntil("10:00");
        Line("john", "Теперь можно продолжать.");
        Complete("jdh:late_dialogue_done");
    }
}
```

В одном проекте придерживайся одной принятой формы. Для существующих JDH-диалогов обычно удобнее продолжать использовать форму с `NodeDef[]`.

### 9.8. Частая ошибка в списке узлов

Имя метода в массиве должно совпадать с реально объявленным методом:

Неправильно:

```csharp
private NodeDef[] dialogueNodes = [ FirstTimeOutside() ];

private NodeDef FirstTimeUniversity()
{
    // ...
}
```

Правильно:

```csharp
private NodeDef[] dialogueNodes = [ FirstTimeUniversity() ];

private NodeDef FirstTimeUniversity()
{
    // ...
}
```

Такая ошибка не относится к самому Echo Time, но невалидный диалог не загрузится, и временный триггер не сможет его начать.

---

## 10. Использование в StoryResource

Time DSL доступен в линейном `OnStart()` и в графовой форме `Nodes()`.

### 10.1. Линейная Story — полный канонический набор

Этот пример соответствует реально поддерживаемому Story binder:

```csharp
namespace JDH.Chapter1.Home;

class WaitForMeeting : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:wait_for_meeting", start: "start");
    }

    private void OnStart()
    {
        Time.Set("10:00");
        Time.Set(Day(2), "08:30");
        Time.Add(Minutes(10));
        Time.Add(Hours(2), crossed: SKIP_CROSSED);
        Time.Pause();
        Time.Resume();
        Time.WaitUntil("10:00");
        Time.Is("09:50");
        Time.IsBefore("10:00");
        Time.IsAfter("08:00");
        Time.IsBetween("23:00", "01:00");
    }
}
```

В реальном сценарии не надо бессмысленно вызывать всё подряд. Этот блок — справочник синтаксиса.

### 10.2. Практическая линейная Story

```csharp
namespace JDH.Chapter1.University;

class BeforeLectureStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:before_lecture", start: "start");
    }

    private void OnStart()
    {
        Time.Set(Day(1), "09:40");
        Time.WaitUntil("09:50");
        Dialogue.Load(
            "jdh:university_remember",
            audienceId: "jdh:local_author",
            output: "dialogue");
    }
}
```

### 10.3. Графовая форма Time action

В `Nodes()` первый позиционный аргумент является id Story-ноды, а `next` обязателен:

```csharp
Time.Set(
    "set_morning",
    Day(2),
    "08:30",
    crossed: SKIP_CROSSED,
    next: "wait_for_lecture");

Time.WaitUntil(
    "wait_for_lecture",
    "10:00",
    next: "finish");
```

Полный пример:

```csharp
namespace JDH.Chapter1.University;

class LectureScheduleStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:lecture_schedule", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "set_morning");

        Time.Set(
            "set_morning",
            Day(2),
            "08:30",
            crossed: SKIP_CROSSED,
            next: "wait_for_lecture");

        Time.WaitUntil(
            "wait_for_lecture",
            "10:00",
            next: "finish");

        Finish("finish");
    }
}
```

### 10.4. Результаты external action в Story

Time action возвращают следующие output keys:

| Действие | Доступные ключи |
|---|---|
| `Time.Set` | `day`, `time`, `revision` |
| `Time.Add` | `day`, `time`, `revision` |
| `Time.Is*` | `value` |
| `Time.Pause` | нет output |
| `Time.Resume` | нет output |
| `Time.WaitUntil` | нет output |

В графовой Story параметр `output` должен совпадать с реально возвращаемым ключом.

Например, получить boolean-проверку:

```csharp
Time.IsBetween(
    "check_night",
    "23:00",
    "01:00",
    output: "value",
    next: "require_night");

Require(
    "require_night",
    variable: "value",
    value: true,
    next: "finish");
```

Полный пример:

```csharp
namespace JDH.Chapter1.Night;

class NightOnlyStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:night_only", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "check_night");

        Time.IsBetween(
            "check_night",
            "23:00",
            "01:00",
            output: "value",
            next: "require_night");

        Require(
            "require_night",
            variable: "value",
            value: true,
            next: "finish");

        Finish("finish");
    }
}
```

Не надо писать произвольное имя вроде `output: "isNight"` для текущего Time predicate runtime: action возвращает ключ именно `value`. Если output key не найден, Story завершит action диагностикой `ESE-ACTION-006`.

Для мутации можно запросить один из реальных ключей:

```csharp
Time.Add(
    "add_two_hours",
    Hours(2),
    output: "time",
    next: "finish");
```

В переменную `time` попадёт строка нового времени `HH:mm`.

### 10.5. Низкоуровневые action type

Высокоуровневый DSL компилируется в следующие action type:

| DSL | Runtime action type |
|---|---|
| `Time.Set` | `echo_time:set` |
| `Time.Add` | `echo_time:add` |
| `Time.Pause` | `echo_time:pause` |
| `Time.Resume` | `echo_time:resume` |
| `Time.WaitUntil` | `echo_time:wait_until` |
| `Time.Is` | `echo_time:is` |
| `Time.IsBefore` | `echo_time:is_before` |
| `Time.IsAfter` | `echo_time:is_after` |
| `Time.IsBetween` | `echo_time:is_between` |

Для обычного authoring используй `Time.*`. Низкоуровневый `Action(..., type: "echo_time:*")` оставляй только как fallback для диагностики или интеграции, которой ещё нет в высокоуровневом DSL.

### 10.6. Идемпотентность Story

Каждая durable Story action получает стабильный mutation id из execution/action identity.

Если сервер перезапустился после применения `Time.Add(Hours(2))`, но до окончательной фиксации продолжения Story, recovery повторит тот же запрос с тем же id. Echo Time вернёт `DUPLICATE`, а не добавит ещё два часа.

Если один и тот же mutation id внезапно приходит с другой сигнатурой команды, это idempotency conflict, а не новая мутация.

---

## 11. Временные триггеры `Time.At`

### 11.1. Ежедневный триггер

```csharp
namespace JDH.Triggers.Time;

class UniversityRememberTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:university_remember_trigger");
        Subscribe(Time.At("09:50"));
        StartDialogue("jdh:university_remember");
    }
}
```

Этот selector создаёт occurrence каждого сюжетного дня в `09:50`.

### 11.2. Триггер конкретного дня

```csharp
namespace JDH.Triggers.Time;

class FirstLectureTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:first_lecture_trigger");
        Subscribe(Time.At(Day(2), "10:00"));
        StartStory("jdh:first_lecture_story");
    }
}
```

Такой selector относится только к:

```text
Day 2 10:00
```

### 11.3. Запуск Dialogue

```csharp
Subscribe(Time.At("09:50"));
StartDialogue("jdh:university_remember");
```

На временной подписке `StartDialogue(...)` поддерживается напрямую.

### 11.4. Запуск Story

```csharp
Subscribe(Time.At("10:00"));
StartStory("jdh:lecture_start");
```

Story получает trigger context и может выполнять глобальные действия даже без игрока, если её собственные actions не требуют audience/player.

### 11.5. Alias-поля id

Можно передавать строку напрямую:

```csharp
StartDialogue("jdh:warning");
```

Или использовать поддерживаемое typed alias-поле:

```csharp
namespace JDH.Triggers.Time;

class WarningTrigger : TriggerResource
{
    private DialogueId Warning = "jdh:warning";

    private void Init()
    {
        Id("jdh:warning_trigger");
        Subscribe(Time.At("21:00"));
        StartDialogue(Warning);
    }
}
```

Для Story аналогично используется `StoryId`.

### 11.6. Только один раз

```csharp
namespace JDH.Triggers.Time;

class FirstMorningOnlyTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:first_morning_only");
        Subscribe(Time.At("08:00"));
        StartDialogue("jdh:first_morning_dialogue");
        FireOnlyOnce();
    }
}
```

Рекомендуемая запись — `FireOnlyOnce()`.

Binder также понимает исторические aliases:

```csharp
FireOnlyOncePerPlayer();
FireOnlyOncePerArm();
```

Для Time trigger они сводятся к тому же флагу. Для ясности не смешивай эти названия в новой документации проекта.

Поведение аудитории:

- для `StartDialogue` once-доставка учитывается для конкретной audience;
- для глобальной `StartStory` once-доставка глобальна.

### 11.7. Ограничения структуры TriggerResource

Для временной подписки:

- нужен `Id(...)`;
- разрешён только один `Subscribe(...)` selector на ресурс;
- нужен ровно один target: `StartDialogue(...)` или `StartStory(...)`;
- `Subscribe` и target должны присутствовать вместе;
- время строго `HH:mm`;
- в `Day(n)` день должен быть положительным.

Неправильно:

```csharp
private void Init()
{
    Id("jdh:broken");
    Subscribe(Time.At("9:50"));
    Subscribe(Time.At("10:00"));
    StartDialogue("jdh:a");
    StartStory("jdh:b");
}
```

Правильно — разделить на два TriggerResource:

```csharp
class ReminderTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:reminder");
        Subscribe(Time.At("09:50"));
        StartDialogue("jdh:a");
    }
}

class LectureTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:lecture");
        Subscribe(Time.At("10:00"));
        StartStory("jdh:b");
    }
}
```

### 11.8. Почему триггер не пропускается на высокой скорости

Selector schedule регистрирует точку времени. Clock publisher проверяет не равенство строки HUD, а пересечение интервала:

```text
previousInstant < occurrence <= currentInstant
```

Поэтому такой переход должен запустить `09:50`:

```text
09:49:50 -> 09:50:10
```

### 11.9. Триггер, добавленный после нужного времени

Временные подписки не воспроизводят прошлое только потому, что файл появился через hot reload.

Если сейчас `Day 1 11:00`, а ты добавил:

```csharp
Subscribe(Time.At("09:50"));
```

первое естественное ежедневное occurrence будет на `Day 2 09:50`.

Для конкретного уже прошедшего дня:

```csharp
Subscribe(Time.At(Day(1), "09:50"));
```

автоматического ретроактивного запуска не будет.

### 11.10. Ровно текущее время при загрузке

Если мир загрузился ровно в `09:50`, scheduler не обязан создавать событие только из факта текущего равенства. Occurrence рождается при пересечении границы. Это защищает от повторного запуска при каждом рестарте в одной минуте.

### 11.11. Перемотка назад и повтор дня

Delivery получает стабильный occurrence identity, включающий сюжетный день. Поэтому повторный проход через ту же точку того же дня после обычного rewind через `Time.Set(...)`, `/echo_time set` или `/echo_time set-day` не должен повторно доставлять уже завершённое occurrence.

Явная команда:

```text
/echo_time reset
```

начиная с `echo-time 0.2.0` + `echo-trigger-time-adapter 0.2.0` открывает новую persistent replay generation. После reset ежедневный trigger без `FireOnlyOnce()` может снова сработать при новом пересечении того же `HH:mm` даже на том же Echo Day. Старые delivery при этом не удаляются: новая генерация получает новый occurrence id, delivery id и Dialogue session id.

На следующем сюжетном дне ежедневный selector получает новый occurrence id и может сработать снова, если не установлен `FireOnlyOnce()`.

### 11.12. Несколько триггеров на одно время

При одинаковом absolute time события упорядочиваются детерминированно:

1. по времени;
2. по приоритету schedule entry, больший приоритет раньше;
3. по selector id лексикографически.

У обычных authored Time trigger текущий adapter публикует стандартный приоритет.

### 11.13. Что происходит без игроков онлайн

Это различается для Story и Dialogue.

`StartStory(...)`:

- имеет глобальную доставку;
- может стартовать без игроков;
- конкретные player-dependent действия внутри Story всё равно могут потребовать игрока.

`StartDialogue(...)`:

- аудитория строится из игроков, находящихся онлайн в момент occurrence;
- если игроков нет, dialogue delivery для будущего вошедшего игрока автоматически не создаётся;
- вход игрока после пересечения не воспроизводит пропущенный диалог.

Если delivery уже была создана для audience, но target service временно недоступен или диалог ещё не опубликован, она остаётся `PENDING` и повторяется примерно раз в секунду до успешного запуска или дальнейшего разрешения состояния.

Если критически важно не потерять событие при пустом сервере, запускай глобальную Story и внутри неё проектируй отдельную устойчивую логику выдачи игроку.

### 11.14. Trigger context для Story

Time trigger передаёт в Story контекст с данными occurrence:

```text
time.occurrenceId
time.subscriptionId
time.triggerId
time.day
time.time
time.intervalStart
time.intervalEnd
time.crossingReason
```

Story execution id строится из delivery id в пространстве `echo_time:trigger/...`, поэтому recovery остаётся детерминированным.

### 11.15. Два события: предупреждение и начало

```csharp
namespace JDH.Triggers.Time;

class LectureReminderTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:lecture_reminder_trigger");
        Subscribe(Time.At("09:50"));
        StartDialogue("jdh:lecture_reminder");
    }
}

class LectureStartTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:lecture_start_trigger");
        Subscribe(Time.At("10:00"));
        StartStory("jdh:lecture_start");
    }
}
```

---

## 12. Паузы, ожидания и проверки времени

### 12.1. Pause lease, а не один глобальный boolean

Пауза хранится как набор владельцев:

```text
story:execution-A
dialogue:session-B
command:Server
```

Clock автоматически идёт, только если:

```text
Running(true)
И
нет активных pause lease
И
Speed не равен нулю
```

### 12.2. Несколько пауз

Пусть одновременно паузу взяли Story и Dialogue:

```text
lease 1: story:chapter_intro
lease 2: dialogue:important_talk
```

Если Dialogue выполнит `Time.Resume()`, Story lease останется, и clock останется на паузе.

Это правильное поведение: чужой сценарий не должен случайно снять чужую паузу.

### 12.3. Консольная пауза

```text
/echo_time pause
```

Владелец формируется из имени command source. Снятие:

```text
/echo_time resume
```

Команда снимает lease этого command owner, а не все Story/Dialogue lease.

Просмотр всех пауз:

```text
/echo_time pauses
```

### 12.4. Что можно делать на паузе

```csharp
Time.Pause();
Time.Set("12:00");
Time.Add(Minutes(30));
Time.Resume();
```

Явные мутации не запрещены паузой. Пауза останавливает только автоматический tick progression.

### 12.5. `WaitUntil` и пауза — возможный deadlock автора

Так писать опасно:

```csharp
Time.Pause();
Time.WaitUntil("10:00");
Time.Resume();
```

Если никто другой не двигает время через `Set/Add` и нет другого механизма снятия паузы, `WaitUntil` никогда не достигнет цели, потому что `Resume` стоит после ожидания.

Правильные варианты:

```csharp
Time.Pause();
// Диалог или сцена.
Time.Resume();
Time.WaitUntil("10:00");
```

или явный переход:

```csharp
Time.Pause();
Time.Set("10:00");
Time.Resume();
```

### 12.6. Durable wait

`Time.WaitUntil` сохраняет:

- стабильный wait id;
- запрошенное `HH:mm`;
- вычисленную абсолютную цель.

После рестарта ожидание не пересчитывается произвольно с нового момента. Оно восстанавливает исходную цель.

### 12.7. Идемпотентность wait

Повторная регистрация того же wait id с тем же временем возвращает существующее ожидание.

Тот же wait id с другим временем вызывает:

```text
Time wait idempotency conflict
```

Это защищает сохранённые выполнения от скрытого изменения смысла команды во время recovery.

### 12.8. Таблица predicates

Пусть точное текущее время `10:00:30`.

| Проверка | Результат | Причина |
|---|:---:|---|
| `Time.Is("10:00")` | `true` | совпадает номер минуты |
| `Time.IsBefore("10:00")` | `false` | уже позже `10:00:00` |
| `Time.IsAfter("10:00")` | `true` | на 30 секунд позже границы |
| `Time.IsBetween("09:00", "11:00")` | `true` | находится внутри интервала |
| `Time.IsBetween("23:00", "01:00")` | `false` | не ночной диапазон |

### 12.9. Проверка ночного диапазона

```csharp
Time.IsBetween("22:00", "06:00");
```

Логика:

```text
current >= 22:00:00 OR current <= 06:00:00
```

### 12.10. Проверки не учитывают номер дня

`Time.Is`, `IsBefore`, `IsAfter`, `IsBetween` работают со временем суток. Они не сравнивают `Echo Day`.

Для события конкретного дня используй:

```csharp
Subscribe(Time.At(Day(2), "10:00"));
```

или Story-переменные/trigger context, если нужна сложная логика дня.

---

## 13. HUD времени

HUD является отдельным клиентским модулем `echo-time-hud`, который отображает server-authoritative snapshot.

### 13.1. Полная конфигурация HUD

Файл:

```text
<gameDir>/story/configs/jdh/echo_time_hud/hud.es
```

Канонический код:

```csharp
namespace JDH.Config.TimeHud;

class MainTimeHud : TimeHudConfigResource
{
    private void Init()
    {
        Id("jdh:main_time_hud");
        Enabled(true);
        Anchor(TOP_RIGHT);
        Offset(x: -24, y: 24);
        ReferenceCanvas(width: 1920, height: 1080);
        SafeMargin(12);
        TextScale(1.0);
        Format("HH:mm");
        ShowDay(false);
        Shadow(true);
        Background(false);
        Opacity(1.0);
        RespectHideHud(true);
        ClampToScreen(true);
        ScreenPolicy(SHOW_DURING_GAME_SCREENS);
    }
}
```

### 13.2. Обязательная форма HUD-конфига

Binder требует:

- namespace строго `JDH.Config.TimeHud`;
- базовый класс `TimeHudConfigResource`;
- ровно один `private void Init()` без параметров;
- внутри только поддерживаемые config calls.

### 13.3. Все HUD-параметры

| Вызов | Допустимые значения | По умолчанию | Назначение |
|---|---|---|---|
| `Id("...")` | непустая строка | обязателен | id конфигурации |
| `Enabled(bool)` | `true/false` | `true` | включить HUD |
| `Anchor(SYMBOL)` | 9 anchors | `TOP_RIGHT` | опорная точка |
| `Offset(x:, y:)` | конечные числа | `-24`, `24` | смещение на reference canvas |
| `ReferenceCanvas(width:, height:)` | положительные int | `1920`, `1080` | базовый дизайн-размер |
| `SafeMargin(n)` | конечное `>= 0` | `12` | безопасный отступ |
| `TextScale(n)` | конечное `> 0` | `1.0` | масштаб текста |
| `Format("HH:mm")` | только `HH:mm` | обязателен | формат времени |
| `ShowDay(bool)` | `true/false` | `false` | показывать сюжетный день |
| `Shadow(bool)` | `true/false` | `true` | тень текста |
| `Background(bool)` | `true/false` | `false` | подложка |
| `Opacity(n)` | `0..1` | `1.0` | прозрачность |
| `RespectHideHud(bool)` | `true/false` | `true` | учитывать скрытие GUI |
| `ClampToScreen(bool)` | `true/false` | `true` | не выпускать HUD за экран |
| `ScreenPolicy(SYMBOL)` | 2 политики | `SHOW_DURING_GAME_SCREENS` | показ поверх игровых экранов |

### 13.4. Anchor

Поддерживаются ровно девять символов:

```csharp
Anchor(TOP_LEFT);
Anchor(TOP_CENTER);
Anchor(TOP_RIGHT);
Anchor(CENTER_LEFT);
Anchor(CENTER);
Anchor(CENTER_RIGHT);
Anchor(BOTTOM_LEFT);
Anchor(BOTTOM_CENTER);
Anchor(BOTTOM_RIGHT);
```

Для правого верхнего угла обычно:

```csharp
Anchor(TOP_RIGHT);
Offset(x: -24, y: 24);
```

Отрицательный `x` сдвигает элемент влево от правого края, положительный `y` — вниз от верхнего края.

### 13.5. ReferenceCanvas

```csharp
ReferenceCanvas(width: 1920, height: 1080);
```

Позиция проектируется от reference canvas на фактический framebuffer. Это делает размещение стабильным при разных разрешениях и GUI scale.

### 13.6. Format

В текущей реализации поддерживается только:

```csharp
Format("HH:mm");
```

Такие варианты будут отклонены:

```csharp
Format("H:mm");
Format("HH:mm:ss");
Format("%s");
```

Ошибка:

```text
HUD_CONFIG_FORMAT_INVALID
```

При ошибке сохраняется last-known-good конфигурация.

### 13.7. ShowDay

```csharp
ShowDay(false);
```

Результат:

```text
09:50
```

```csharp
ShowDay(true);
```

Результат текущего formatter:

```text
Day 2 · 09:50
```

Слово `Day` сейчас жёстко задано formatter и не локализуется через DSL.

### 13.8. ScreenPolicy

```csharp
ScreenPolicy(SHOW_DURING_GAME_SCREENS);
```

HUD может оставаться видимым во время поддерживаемых игровых экранов.

```csharp
ScreenPolicy(WORLD_ONLY);
```

HUD показывается только в обычном world view по политике renderer.

### 13.9. HUD только с временем, справа сверху

```csharp
namespace JDH.Config.TimeHud;

class MinimalTimeHud : TimeHudConfigResource
{
    private void Init()
    {
        Id("jdh:minimal_time_hud");
        Enabled(true);
        Anchor(TOP_RIGHT);
        Offset(x: -24, y: 24);
        ReferenceCanvas(width: 1920, height: 1080);
        SafeMargin(12);
        TextScale(1.0);
        Format("HH:mm");
        ShowDay(false);
        Shadow(true);
        Background(false);
        Opacity(1.0);
        RespectHideHud(true);
        ClampToScreen(true);
        ScreenPolicy(SHOW_DURING_GAME_SCREENS);
    }
}
```

### 13.10. HUD с днём и фоном, слева сверху

```csharp
namespace JDH.Config.TimeHud;

class DetailedTimeHud : TimeHudConfigResource
{
    private void Init()
    {
        Id("jdh:detailed_time_hud");
        Enabled(true);
        Anchor(TOP_LEFT);
        Offset(x: 24, y: 24);
        ReferenceCanvas(width: 1920, height: 1080);
        SafeMargin(12);
        TextScale(1.2);
        Format("HH:mm");
        ShowDay(true);
        Shadow(true);
        Background(true);
        Opacity(0.85);
        RespectHideHud(true);
        ClampToScreen(true);
        ScreenPolicy(WORLD_ONLY);
    }
}
```

### 13.11. Почему HUD меняется раз в секунду

Сервер периодически рассылает time snapshot каждые `20` серверных тиков. При нормальных 20 TPS это одна секунда.

HUD отображает последний snapshot без собственного выдуманного clock progression. Поэтому это ожидаемо:

```text
08:30
через ~1 секунду
08:31
через ~1 секунду
08:32
```

для скорости:

```csharp
Speed(gameMinutes: 1.0, realSeconds: 1.0);
```

Если HUD обновляется каждые пять секунд, проверь актуальность установленного jar и `/echo_time_hud status`. В текущей установленной сборке период исправлен до одной секунды.

### 13.12. Команды HUD

Статус:

```text
/echo_time_hud status
```

Ручная перезагрузка:

```text
/echo_time_hud reload
```

Показать debug bounds:

```text
/echo_time_hud debug bounds on
```

Скрыть bounds:

```text
/echo_time_hud debug bounds off
```

Вывести текущий debug report:

```text
/echo_time_hud debug bounds
```

---

## 14. Консольные команды

Корневая команда:

```text
/echo_time
```

Требуется permission level `2`.

### 14.1. Полный список

| Команда | Назначение |
|---|---|
| `/echo_time status` | текущее время, rate, running, leases, revision, dayTime, dimensions |
| `/echo_time set 08:30` | установить время текущего дня, пропустив пересечения |
| `/echo_time set-day 2 08:30` | установить конкретный день и время |
| `/echo_time reset` | установить активные `InitialDay` + `InitialTime` |
| `/echo_time add 10m` | добавить 10 минут с `FIRE_CROSSED` |
| `/echo_time add 2h` | добавить 2 часа с `FIRE_CROSSED` |
| `/echo_time pause` | взять command pause lease |
| `/echo_time resume` | освободить command pause lease этого source |
| `/echo_time pauses` | показать активные pause lease |
| `/echo_time speed` | показать скомпилированную скорость и configured running |
| `/echo_time reload` | вручную перезагрузить `time-speed.es` |
| `/echo_time resync` | немедленно отправить snapshot клиентам |
| `/echo_time next` | показать до 16 ближайших schedule entries |
| `/echo_time diagnostics` | показать активные config diagnostics |

### 14.2. Установить время

```text
/echo_time set 22:30
```

Кавычки не нужны. Команда использует `SKIP_CROSSED`.

### 14.3. Установить день и время

```text
/echo_time set-day 3 07:15
```

Результат:

```text
Day 3 07:15
```

### 14.4. Reset

```text
/echo_time reset
```

Команда читает **активную принятую конфигурацию** и устанавливает:

```text
InitialDay(...) + InitialTime(...)
```

Если config содержит:

```csharp
InitialDay(1);
InitialTime("08:30");
```

reset даст:

```text
Day 1 08:30
```

Reset использует `SKIP_CROSSED`.

Начиная с `echo-time 0.2.0` + `echo-trigger-time-adapter 0.2.0`, успешный reset также открывает новую persistent generation для `Time.At`. Это делает reset полноценным повторным прогоном временной шкалы: после нового пересечения `09:50` обычный ежедневный trigger может запустить новый Dialogue, даже если `Day 1 09:50` уже доставлялся до reset.

### 14.5. Что reset переоткрывает и чего не очищает

`/echo_time reset` сбрасывает instant часов и увеличивает replay generation временных occurrence. Он не означает «удалить всю историю мода».

Reset позволяет заново доставить последующее occurrence обычного `Time.At` без `FireOnlyOnce()`, потому что оно получит новую identity. При этом он не удаляет автоматически:

- уже завершённые trigger delivery;
- `FireOnlyOnce`-историю;
- Story execution;
- Dialogue session;
- чужие pause lease;
- authored переменные других систем;
- config-файлы.

`FireOnlyOnce()` намеренно не сбрасывается: его delivery id не зависит от occurrence generation. Обычная перемотка через `set` также не открывает новую generation. Для полного тестового прогона используй именно `/echo_time reset`, затем заново пересекай целевое время естественным ходом или `/echo_time add`.

### 14.6. Добавить длительность

```text
/echo_time add 10m
/echo_time add 2h
/echo_time add 24h
```

Поддерживаются только суффиксы нижнего регистра:

```text
m
h
```

Нельзя:

```text
/echo_time add 10min
/echo_time add 2H
/echo_time add -5m
```

Консольный `add` использует `FIRE_CROSSED`. Отдельного параметра `SKIP_CROSSED` у slash-команды сейчас нет.

### 14.7. Проверить следующий schedule

```text
/echo_time next
```

Команда показывает до 16 ближайших зарегистрированных entries, например:

```text
09:50 echo_time:daily/09_50
Day 2 10:00 echo_time:day/2/10_00
```

Если список пуст, временные selector могли не загрузиться или adapter не опубликовал каталог.

### 14.8. Status

```text
/echo_time status
```

Выводит:

```text
Echo Day
Time с внутренними секундами
Absolute instant
Running
Rate
Pause leases
Clock revision
Minecraft derived dayTime
Sun dimensions
```

### 14.9. Resync

```text
/echo_time resync
```

Не меняет часы. Только немедленно отправляет текущий snapshot подключённым клиентам.

### 14.10. Reload

```text
/echo_time reload
```

Запрашивает перезагрузку `time-speed.es`.

Если новый файл невалиден, last-known-good остаётся активным. Смотри:

```text
/echo_time diagnostics
```

---

## 15. Солнце, луна, gamerule и погода

Это ключевой раздел для понимания `doDaylightCycle`.

### 15.1. Что делает `SyncSun(true)`

При захвате daylight ownership Echo Time выполняет логически следующее:

```text
1. Прочитать текущее gamerule doDaylightCycle.
2. Запомнить его для последующего восстановления.
3. Установить doDaylightCycle false.
4. Каждый серверный тик вычислять vanilla dayTime из Echo Time.
5. Записывать вычисленный dayTime в SunDimension.
6. Если кто-то включил doDaylightCycle true — выключить обратно.
```

### 15.2. Если сервер уже имеет `doDaylightCycle false`

Ничего плохого не происходит.

Echo Time не ждёт, пока ваниль сама двинет солнце. Он напрямую пишет `level.setDayTime(expected)` из сюжетного instant.

То есть:

```text
gamerule doDaylightCycle false
+ SyncSun(true)
= Echo Time идёт, солнце идёт по Echo Time
```

### 15.3. Если сервер имеет `doDaylightCycle true`

При `SyncSun(true)` Echo Time меняет его на `false`.

Это необходимо. Если оставить `true`, возникнут два независимых писателя:

```text
vanilla tick прибавляет dayTime
Echo Time выставляет свой dayTime
```

Результатом были бы конфликт, дрожание или лишнее продвижение. Поэтому включать ванильный cycle поверх Echo Time нельзя.

### 15.4. Если вручную выполнить gamerule true

```text
/gamerule doDaylightCycle true
```

при активном:

```csharp
SyncSun(true);
```

Echo Time на следующей синхронизации обнаружит конфликт и вернёт:

```text
doDaylightCycle false
```

Это ожидаемая защита владения.

### 15.5. Нужно ли «отменять doDaylightCycle false»

Нет. `false` отключает только ванильное автономное прибавление `dayTime`. Оно **не запрещает** серверному моду напрямую выставлять `dayTime`.

Желаемое поведение уже реализовано:

```text
ванильный cycle выключен
Echo Time является единственным владельцем положения солнца
```

Включение gamerule было бы не исправлением, а созданием двойного clock source.

### 15.6. Что делает `SyncSun(false)`

```csharp
SyncSun(false);
```

Echo Time продолжает вести сюжетные часы, Story, Dialogue, Trigger и HUD. Но ванильный `dayTime` больше не проецируется из Echo Time.

После штатного освобождения ownership восстанавливается значение `doDaylightCycle`, которое было до захвата.

Практически:

```text
Echo Time: Day 2 14:00
Minecraft sky: живёт по собственному текущему dayTime/gamerule
```

Это режим намеренно рассинхронизированных сюжетных часов и неба.

### 15.7. Восстановление gamerule

Echo Time запоминает прежнее значение.

Пример A:

```text
до захвата: doDaylightCycle true
во время SyncSun(true): false
после штатного release: true
```

Пример B:

```text
до захвата: doDaylightCycle false
во время SyncSun(true): false
после штатного release: false
```

Под «штатным» имеется в виду корректный lifecycle release/reload. При аварийном завершении процесса полагайся на сохранённое gamerule мира и повторный захват на следующем старте.

### 15.8. Почему `/time set` сразу откатывается

При `SyncSun(true)` команда:

```text
/time set day
```

меняет ванильный `dayTime`, но не меняет `EchoInstant`. На следующем server tick Echo Time видит внешнее отличие и возвращает ожидаемое значение.

Используй:

```text
/echo_time set 06:00
```

или DSL:

```csharp
Time.Set("06:00");
```

### 15.9. Маппинг Echo Time в vanilla dayTime

Echo Time использует абсолютную монотонную проекцию с midnight offset `18000` vanilla ticks.

| Echo Time | Derived vanilla dayTime | Фаза внутри vanilla day |
|---|---:|---:|
| `Day 1 00:00` | `18000` | полночь |
| `Day 1 06:00` | `24000` | `0`, утро |
| `Day 1 12:00` | `30000` | `6000`, полдень |
| `Day 1 18:00` | `36000` | `12000`, вечер |
| `Day 2 00:00` | `42000` | `18000`, следующая полночь |

Формула:

```text
dayTime
= (EchoDay - 1) * 24000
  + 18000
  + floor(timeOfDayFraction * 24000)
```

Абсолютное значение не сбрасывается к `0..23999`, чтобы ванильные фазы дней и луны последовательно продолжались.

### 15.10. Луна

В Minecraft фаза луны зависит от абсолютного дня `dayTime`. Поскольку Echo Time проецирует абсолютный растущий dayTime, лунная фаза следует Echo Day.

Отдельной DSL-команды:

```text
Time.SetMoonPhase(...)
```

сейчас нет.

`SyncMoon(false)` пока не создаёт отдельный независимый moon clock.

### 15.11. Погода и `doWeatherCycle`

Echo Time не управляет погодой и не меняет:

```text
doWeatherCycle
```

Поэтому:

```text
gamerule doWeatherCycle false
```

замораживает ванильную смену погоды, но:

- Echo Time продолжает идти;
- временные триггеры продолжают работать;
- HUD продолжает обновляться;
- солнце продолжает следовать Echo Time при `SyncSun(true)`.

И наоборот, `doWeatherCycle true` меняет погоду по ванильным правилам, не меняя скорость Echo Time.

### 15.12. Матрица режимов

| `SyncSun` | `doDaylightCycle` до захвата | Что происходит с небом | Что происходит с Echo Time |
|:---:|:---:|---|---|
| `true` | `true` | rule ставится `false`, небо ведёт Echo Time | идёт по config/leases |
| `true` | `false` | остаётся `false`, небо ведёт Echo Time | идёт по config/leases |
| `false` | `true` | ванильный цикл может идти | Echo Time независим |
| `false` | `false` | ванильное небо заморожено | Echo Time независим |

Погода во всех четырёх строках определяется отдельно через `doWeatherCycle` и погодные команды/моды.

### 15.13. Рекомендуемый production-режим

```csharp
Running(true);
SyncSun(true);
SyncMoon(true);
SunDimension("minecraft:overworld");
```

И не пытаться вручную удерживать:

```text
doDaylightCycle true
```

Echo Time сам выставит корректное значение владения.

---

## 16. Сохранение, перезапуск и hot reload

### 16.1. Что сохраняется

В clock state сохраняются, в частности:

- текущий абсолютный Echo instant;
- day + time;
- revision;
- compiled rate;
- fixed-point remainder;
- configured running state;
- pause lease;
- mutation receipts для идемпотентности;
- durable waits.

В trigger delivery state сохраняются:

- pending delivery;
- completed delivery;
- occurrence/delivery identity;
- once-дедупликация.

### 16.2. Рестарт сервера

После рестарта:

- часы продолжаются с сохранённого instant;
- offline-время не прибавляется;
- pending wait восстанавливается;
- applied durable mutation не применяется повторно;
- completed trigger delivery не повторяется только из-за рестарта;
- HUD после подключения получает серверный snapshot.

### 16.3. Первый запуск нового мира

Если сохранённого clock state нет, используются:

```csharp
InitialDay(...);
InitialTime("...");
```

### 16.4. Изменение InitialTime существующего мира

Было:

```csharp
InitialTime("08:30");
```

Текущие сохранённые часы уже `Day 3 14:20`.

Ты меняешь config на:

```csharp
InitialTime("07:00");
```

После reload текущий instant останется `Day 3 14:20`. Чтобы применить bootstrap-значение как явный reset:

```text
/echo_time reset
```

### 16.5. Hot reload главного конфига

Файл наблюдается watcher-ом. Изменение проходит стадии:

```text
прочитать candidate
-> распарсить EchoScript
-> связать config calls
-> проверить ограничения
-> атомарно опубликовать новую revision
```

Если candidate невалиден:

```text
last-known-good остаётся активным
```

Clock instant при успешном reload не сбрасывается.

### 16.6. Что меняется hot reload-ом

Успешная новая конфигурация может изменить:

- rate;
- `Running`;
- `SyncSun` ownership;
- список `SunDimension`;
- `SyncMoon` config field;
- значения, которые позже использует `/echo_time reset`.

Не изменяется автоматически:

- текущий saved instant;
- завершённая trigger history;
- Story/Dialogue state.

### 16.7. Удалённый или сломанный config

Если `time-speed.es` удалён или не разбирается, активный last-known-good не заменяется пустотой.

Проверка:

```text
/echo_time diagnostics
```

### 16.8. Hot reload временных триггеров

Trigger catalog следит за revision и обновляет schedule. Новые будущие occurrence будут учитываться.

Прошлые временные точки не воспроизводятся автоматически.

### 16.9. Hot reload HUD

HUD config имеет отдельный last-known-good. Ошибка одного HUD-файла не должна ломать server clock.

Проверка:

```text
/echo_time_hud status
```

Ручной reload:

```text
/echo_time_hud reload
```

---

## 17. Готовые рецепты

### 17.1. Напоминание в 09:50 каждый день

Trigger:

```csharp
namespace JDH.Triggers.Time;

class UniversityRememberTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:university_remember_trigger");
        Subscribe(Time.At("09:50"));
        StartDialogue("jdh:university_remember");
    }
}
```

Dialogue:

```csharp
namespace JDH.Chapter1.University;

class UniversityRememberDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ FirstTimeUniversity() ];

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:university_remember");
    }

    private void Init()
    {
        Id("jdh:university_remember");
        Start("university_remember");
        Alias(
            "john",
            "jdh:john",
            display: "Лололошка",
            nameColor: nameColor("#FC9004"),
            visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef FirstTimeUniversity()
    {
        var node = new Node("university_remember");

        Line("john", "Кажется, пора идти на пары...", thought: true);

        Complete("jdh:university_remember");
        return node;
    }
}
```

### 17.2. Один раз на втором дне в 10:00

```csharp
namespace JDH.Triggers.Time;

class DayTwoLectureTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:day_two_lecture_trigger");
        Subscribe(Time.At(Day(2), "10:00"));
        StartStory("jdh:day_two_lecture");
        FireOnlyOnce();
    }
}
```

Для specific-day selector `FireOnlyOnce()` часто избыточен, потому что абсолютное occurrence и так одно. Но он явно фиксирует авторское намерение и защищает от будущей смены selector.

### 17.3. Перевести время после кат-сцены

```csharp
private void OnStart()
{
    Time.Pause();

    // Cutscene.*, Dialogue.Load(...) и другие действия сцены.

    Time.Set("22:30", crossed: SKIP_CROSSED);
    Time.Resume();
}
```

### 17.4. Путешествие, во время которого расписание живёт

```csharp
private void OnStart()
{
    Time.Add(Hours(3), crossed: FIRE_CROSSED);
}
```

Все eligible `Time.At` внутри пройденного интервала получают occurrence.

### 17.5. Сон без ночных напоминаний

```csharp
private void OnStart()
{
    Time.Add(Hours(8), crossed: SKIP_CROSSED);
}
```

Если нужно всегда проснуться в конкретное время текущего/следующего дня, `Set` текущего дня может случайно перемотать назад. Для точного сна лучше заранее определить сценарную цель или использовать Add.

### 17.6. Сон с реальными утренними событиями

```csharp
private void OnStart()
{
    Time.Add(Hours(8), crossed: FIRE_CROSSED);
}
```

Используй только если все пересечённые расписания действительно должны быть доставлены во время мгновенного skip.

### 17.7. Остановить автоматическое время на всю главу

Config:

```csharp
Running(false);
```

Story двигает время явно:

```csharp
Time.Set(Day(1), "08:30");
Time.Set("09:50");
Time.Set("10:00");
```

Для переходов, которые должны воспроизводить расписание:

```csharp
Time.Set("10:00", crossed: FIRE_CROSSED);
```

### 17.8. Ускоренный отладочный прогон

Временно в config:

```csharp
Speed(gameMinutes: 10.0, realSeconds: 1.0);
```

Затем:

```text
/echo_time reload
/echo_time reset
/echo_time next
```

После теста верни production rate. Не забывай, что natural progression на высокой скорости всё равно создаёт crossings.

### 17.9. Проверить триггер 09:50 без ожидания

Если сейчас раньше 09:50:

```text
/echo_time set 09:49
```

Дождись естественного пересечения.

Или:

```text
/echo_time set 09:49
/echo_time add 2m
```

`add` использует `FIRE_CROSSED`, поэтому occurrence `09:50` должно быть обработано.

Не тестируй так:

```text
/echo_time set 09:51
```

Потому что console `set` использует `SKIP_CROSSED`.

### 17.10. Проверить конкретный день

```text
/echo_time set-day 2 09:59
/echo_time add 2m
```

Это пересечёт:

```text
Day 2 10:00
```

### 17.11. Утреннее и вечернее состояние NPC

Два Story trigger:

```csharp
namespace JDH.Triggers.Time;

class NpcMorningTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:npc_morning_trigger");
        Subscribe(Time.At("07:00"));
        StartStory("jdh:npc_morning_schedule");
    }
}

class NpcEveningTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:npc_evening_trigger");
        Subscribe(Time.At("22:00"));
        StartStory("jdh:npc_evening_schedule");
    }
}
```

Почему Story, а не Dialogue: расписание NPC является глобальным действием и должно иметь шанс выполниться без audience.

### 17.12. Время идёт, погода заморожена

Minecraft:

```text
/gamerule doWeatherCycle false
```

Echo Time config:

```csharp
Running(true);
SyncSun(true);
```

Результат:

```text
сюжетные часы идут
солнце движется по Echo Time
текущая погода не меняется ванильным weather cycle
```

### 17.13. Сюжетное время без управления небом

```csharp
Running(true);
SyncSun(false);
```

Подходит для абстрактного измерения, интерфейсного таймера или сцены, где sky контролирует другой мод.

### 17.14. Несколько измерений с одним небом-временем

```csharp
SyncSun(true);
SunDimension("minecraft:overworld");
SunDimension("jdh:memory_world");
```

Оба загруженных `ServerLevel` получают один derived `dayTime`.

### 17.15. Диалог в 09:50 только один раз для audience

```csharp
namespace JDH.Triggers.Time;

class OnceReminderTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:once_reminder_trigger");
        Subscribe(Time.At("09:50"));
        StartDialogue("jdh:once_reminder");
        FireOnlyOnce();
    }
}
```

### 17.16. Story ждёт ближайшее 18:00

```csharp
private void OnStart()
{
    Time.WaitUntil("18:00");
    Dialogue.Load(
        "jdh:evening_bell",
        audienceId: "jdh:local_author",
        output: "dialogue");
}
```

Если Story стартовала после `18:00`, она ждёт следующий день.

### 17.17. Установить время и намеренно запустить напоминание

```csharp
private void OnStart()
{
    Time.Set("09:49", crossed: SKIP_CROSSED);
    Time.Set("09:51", crossed: FIRE_CROSSED);
}
```

Occurrence `09:50` пересекается вторым вызовом.

### 17.18. Установить время без запуска расписания

```csharp
private void OnStart()
{
    Time.Set(Day(5), "12:00", crossed: SKIP_CROSSED);
}
```

Подходит для загрузки главы в заранее подготовленном состоянии.

---

## 18. Ошибки и диагностика

### 18.1. Триггер не сработал — порядок проверки

1. Проверить точное время:

```text
/echo_time status
```

2. Проверить schedule:

```text
/echo_time next
```

3. Проверить формат selector:

```csharp
Subscribe(Time.At("09:50"));
```

4. Проверить, что trigger-файл имеет расширение:

```text
.trigger.es
```

5. Проверить, что он лежит в `story/triggers` или внутри `echo_trigger/triggers`.

6. Проверить уникальность `Id(...)`.

7. Проверить существование target id.

8. Для `StartDialogue` проверить, был ли игрок онлайн в момент occurrence.

9. Проверить `latest.log` и `debug.log` по id ресурса и словам `echo_time`, `echo_trigger`, `dialogue`.

10. Проверить, не тестировалось ли пересечение через `/echo_time set`, которое использует `SKIP_CROSSED`.

### 18.2. Правильный тест пересечения

```text
/echo_time set 09:49
/echo_time add 2m
```

### 18.3. Команда `/echo_time set 08:30` подсвечивается ошибкой

В актуальной сборке time-аргумент принимает двоеточие без кавычек:

```text
/echo_time set 08:30
```

Если клиент всё ещё считает `:` разделяющим аргументом или обновление HUD идёт раз в пять секунд, вероятна старая версия jar. Проверь папку `mods`, перезапусти инстанс и выполни status-команды.

### 18.4. Изменил `InitialTime`, но часы не сбросились

Это ожидаемо. `InitialTime` — bootstrap нового состояния.

Выполни:

```text
/echo_time reload
/echo_time reset
```

### 18.5. `/time set` не держится

Причина:

```csharp
SyncSun(true);
```

Исправление авторского действия:

```text
/echo_time set 12:00
```

или:

```csharp
Time.Set("12:00");
```

### 18.6. Clock не идёт

Проверь:

```text
/echo_time status
/echo_time speed
/echo_time pauses
```

Возможные причины:

- `Running(false)`;
- `Speed(gameMinutes: 0, ...)`;
- активный Story pause lease;
- активный Dialogue pause lease;
- command pause lease;
- сервер фактически не тикает;
- новый config отклонён, а LKG имеет старые значения.

### 18.7. HUD не виден

Проверь:

```text
/echo_time_hud status
```

Возможные причины:

- `Enabled(false)`;
- `RespectHideHud(true)` и GUI скрыт;
- `ScreenPolicy(WORLD_ONLY)` и открыт экран;
- snapshot отсутствует;
- opacity равна `0`;
- config отклонён;
- HUD оказался у края, используй `ClampToScreen(true)` и debug bounds.

### 18.8. HUD показывает старое время

```text
/echo_time resync
/echo_time_hud status
```

Сверь:

- server clock revision;
- HUD snapshot revision;
- подключение клиента;
- TPS сервера.

### 18.9. Ошибка strict time

Сообщение:

```text
Time must use strict 24-hour HH:mm format.
```

Используй:

```csharp
"09:50"
```

а не:

```csharp
"9:50"
```

### 18.10. Ошибка конфигурации rate

Возможные причины:

```csharp
Speed(gameMinutes: -1.0, realSeconds: 1.0); // нельзя
Speed(gameMinutes: 1.0, realSeconds: 0.0);  // нельзя
```

Или rate выше safety cap:

```text
ECHO_TIME_RATE_TOO_HIGH
```

### 18.11. Слишком много событий при Add/Set

Ошибка:

```text
ECHO_TIME_CROSSED_EVENT_LIMIT
```

Решения:

- использовать `SKIP_CROSSED`, если события не нужны;
- разбить переход на осмысленные этапы;
- уменьшить число schedule entries;
- не использовать огромный `FIRE_CROSSED` через множество дней без необходимости.

### 18.12. Trigger виден, но Dialogue не стартует

Проверь:

- `StartDialogue` id точно совпадает с `Dialogue(...)`/`Id(...)`;
- диалог успешно скомпилирован;
- стартовый node id существует;
- массив `dialogueNodes` вызывает правильный метод;
- audience была онлайн в occurrence;
- нет незавершённой конфликтующей dialogue session;
- в логе нет rejected catalog revision.

### 18.13. После rewind триггер не повторился

Для обычного `Time.Set(...)`, `/echo_time set` и `/echo_time set-day` это ожидаемо: уже завершённая delivery того же occurrence/day подавляется.

Используй:

```text
/echo_time reset
```

В версиях `echo-time 0.2.0` + `echo-trigger-time-adapter 0.2.0` reset увеличивает persistent replay generation. Затем новое пересечение создаёт новый Dialogue session:

```text
/echo_time reset
/echo_time set 09:49
/echo_time add 2m
```

Если на trigger установлен `FireOnlyOnce()`, reset намеренно не разрешит его повтор.

### 18.14. Код `ESL-439439234342`

```text
ESL-439439234342
```

Значение:

```text
Повтор Time.At occurrence подавлен, потому что delivery с той же identity уже имеет статус DELIVERED.
```

Типичный сценарий до исправления:

```text
Day 1 09:50 уже запускал диалог
-> время вернули назад на Day 1
-> снова пересекли 09:50
-> occurrence id и delivery id совпали со старыми
-> повтор был подавлен
```

Это не означает ошибку в строке:

```csharp
Subscribe(Time.At("09:50"));
```

И не означает ошибку target id, если в сохранении одновременно присутствуют:

```text
targetId = jdh:university_remember
status = DELIVERED
Dialogue session = time-trigger/<deliveryId>
```

Начиная с adapter `0.2.0`, при подавлении дубликата в лог выводится предупреждение с `ESL-439439234342`, trigger id, target id, днём, временем и delivery id. Правильное действие зависит от намерения:

- обычный сюжетный rewind — ничего не делать, защита от дубля работает;
- полный повторный прогон — выполнить `/echo_time reset` и заново пересечь время;
- trigger с `FireOnlyOnce()` — reset не должен повторять его;
- если reset уже выполнен на версиях `0.2.0`, но код повторяется, проверить, что установлены оба новых jar и сервер был полностью перезапущен.

Для отдельного нового authored события без reset:

- используй следующий сюжетный день;
- создай новый trigger id/selector только если это действительно новое authored событие;
- не пытайся лечить это удалением SavedData на живом мире.

### 18.15. Диагностика config

```text
/echo_time diagnostics
```

Типичные категории:

```text
ECHO_TIME_CONFIG_PARSE_FAILED
ECHO_TIME_CONFIG_BIND_FAILED
ECHO_TIME_CONFIG_INVALID
ECHO_TIME_CONFIG_MISSING
ECHO_TIME_RATE_TOO_HIGH
```

### 18.16. Что искать в логах

Пример PowerShell-поиска после закрытия/остановки клиента или в read-only режиме:

```powershell
rg -n "echo_time|echo_trigger_time|university_remember|09:50|PENDING|REJECT" `
  "C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\logs\latest.log"
```

И отдельно:

```powershell
rg -n "echo_time|echo_trigger_time|university_remember|09:50|PENDING|REJECT" `
  "C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\logs\debug.log"
```

---

## 19. Частые вопросы

### Можно ли в диалоге написать `Time.Set("22:30");`?

Да. Это устанавливает `22:30` текущего сюжетного дня.

### Как установить 22:30 второго дня?

```csharp
Time.Set(Day(2), "22:30");
```

### Как сбросить время к начальному?

```text
/echo_time reset
```

Reset использует активные `InitialDay` и `InitialTime`.

### Есть ли `time clear` или `time reset`?

Ванильная `/time` не управляет Echo Time. Используй:

```text
/echo_time reset
```

### Почему `InitialTime("08:30")` не применилось после перезахода?

Потому что существующий world state загружается из сохранения. Выполни `/echo_time reset`, если нужен явный сброс.

### Если `doDaylightCycle false`, Echo Time остановится?

Нет. При `SyncSun(true)` Echo Time сам двигает `dayTime` напрямую.

### Echo Time включает `doDaylightCycle true`?

Нет, и не должен. Он выставляет `false`, чтобы ваниль не была вторым владельцем clock.

### Почему gamerule снова стал false после моей команды?

Потому что при `SyncSun(true)` Echo Time защищает daylight ownership.

### Как разрешить ванильному солнцу идти самостоятельно?

Установи:

```csharp
SyncSun(false);
```

и перезагрузи config. Тогда сюжетное время и ванильное небо станут независимыми.

### Управляет ли Echo Time погодой?

Нет. `doWeatherCycle` и weather state независимы.

### Если `doWeatherCycle false`, триггеры времени работают?

Да.

### Как заморозить только погоду, но оставить часы и солнце?

```text
/gamerule doWeatherCycle false
```

и в Echo Time:

```csharp
Running(true);
SyncSun(true);
```

### Как заморозить сюжетные часы?

В сценарии:

```csharp
Time.Pause();
```

В config:

```csharp
Running(false);
```

В консоли:

```text
/echo_time pause
```

Это три разных уровня владения. Для временной сценарной паузы используй lease через `Time.Pause()`.

### Почему `/echo_time resume` не запустил часы?

Возможно, остался lease другого Story/Dialogue owner или `Running(false)`. Проверь:

```text
/echo_time pauses
/echo_time speed
```

### Идут ли часы, когда сервер выключен?

Нет. Offline catch-up отсутствует.

### Идут ли часы без игроков?

На dedicated server — да, пока сервер тикает.

### Идут ли часы в выгруженном чанке?

Часы глобальны и идут. Но Echo Time не force-load-ит чанки для NPC-действий.

### Запустится ли `StartDialogue`, если никого нет онлайн?

Нет audience — нет dialogue delivery для будущего вошедшего игрока. Для критического глобального события используй `StartStory`.

### Запустится ли `StartStory`, если никого нет онлайн?

Глобальная Story может стартовать. Player-dependent actions внутри неё могут иметь собственные требования.

### Сработает ли `Time.At("09:50")`, если скорость перескочила с 09:49 на 09:51?

Да, при естественном ходе или мутации с `FIRE_CROSSED`.

### Сработает ли триггер после `/echo_time set 09:51`?

Нет, console `set` использует `SKIP_CROSSED`. Для теста:

```text
/echo_time set 09:49
/echo_time add 2m
```

### Чем `Set` отличается от `Add`?

- `Set` задаёт целевое время/instant и по умолчанию пропускает crossings.
- `Add` добавляет длительность и по умолчанию публикует crossings.

### Что будет при `Time.Set("08:00")`, если сейчас 10:00?

Перемотка назад в рамках текущего дня. Crossed-события назад не запускаются.

### Как перескочить ночь и не запускать ночные события?

```csharp
Time.Add(Hours(8), crossed: SKIP_CROSSED);
```

### Как перескочить ночь и запустить утренние расписания?

```csharp
Time.Add(Hours(8), crossed: FIRE_CROSSED);
```

### `Time.WaitUntil("10:00")` сегодня или завтра?

Сегодня, если текущий instant не позже сегодняшнего `10:00:00`; иначе завтра.

### Можно ли указать `occurrence: NEXT_DAY`?

Нет. В текущем DSL у `Time.WaitUntil` нет named-параметра `occurrence`. Работает фиксированная семантика today-or-next.

### Можно ли добавить секунды?

Нет через текущий authored DSL. Поддерживаются `Minutes(...)` и `Hours(...)`.

### Можно ли задать время с секундами?

Нет. Author-facing формат — `HH:mm`. Внутри clock секунды и микросекунды существуют.

### Почему HUD показывает только минуты?

Поддерживается только `Format("HH:mm")`.

### Почему `ShowDay(true)` пишет `Day`, а не `День`?

Это текущая строка formatter. DSL-локализация label пока не реализована.

### Можно ли отдельно остановить луну?

Нет. Отдельного moon controller сейчас нет.

### Что делает `SyncMoon(false)`?

Поле принимается config, но при `SyncSun(true)` абсолютный vanilla dayTime всё равно определяет лунную фазу. Отдельная заморозка луны не реализована.

### Можно ли синхронизировать несколько измерений?

Да:

```csharp
SunDimension("minecraft:overworld");
SunDimension("jdh:memory_world");
```

### Если измерение не загружено?

Оно пропускается в текущем sync pass.

### Что будет с триггером после hot reload?

Будущие crossings учитывают новый каталог. Прошлое не воспроизводится ретроактивно.

### Может ли один TriggerResource иметь два `Time.At`?

Нет. Создай два TriggerResource с разными id.

### Можно одновременно `StartDialogue` и `StartStory`?

Нет, один subscription имеет один target. Если нужно оба действия, запускай Story, а Story пусть запускает Dialogue.

### Повторится ли `Time.Add` после server recovery?

Для durable Story/Dialogue-команды с тем же mutation id — нет, вернётся idempotent duplicate.

### Повторится ли вручную введённая slash-команда?

Каждый новый ввод получает новый случайный mutation id, поэтому каждый ввод является новой командой.

### Можно ли reset-ом очистить `FireOnlyOnce`?

Нет. Reset открывает новую occurrence generation для обычных повторяемых `Time.At`, но once-delivery намеренно сохраняет постоянную identity.

### Почему один `Time.Resume()` не снял все паузы?

Потому что паузы lease-based и принадлежат разным выполнениям.

---

## 20. Краткая шпаргалка

### Главный config

```csharp
namespace JDH.Config.Time;

class MainTimeSpeed : TimeSpeedConfigResource
{
    private void Init()
    {
        Id("jdh:main_time");
        InitialDay(1);
        InitialTime("08:30");
        Speed(gameMinutes: 1.0, realSeconds: 1.0);
        Running(true);
        SyncSun(true);
        SyncMoon(true);
        SunDimension("minecraft:overworld");
    }
}
```

### Все Time DSL-вызовы

```csharp
Time.Set("10:00");
Time.Set(Day(2), "08:30");
Time.Set("18:00", crossed: FIRE_CROSSED);
Time.Set("18:00", crossed: SKIP_CROSSED);

Time.Add(Minutes(10));
Time.Add(Hours(2));
Time.Add(Hours(8), crossed: SKIP_CROSSED);
Time.Add(Hours(8), crossed: FIRE_CROSSED);

Time.Pause();
Time.Resume();
Time.WaitUntil("10:00");

Time.Is("09:50");
Time.IsBefore("10:00");
Time.IsAfter("08:00");
Time.IsBetween("23:00", "01:00");
```

### Daily trigger

```csharp
namespace JDH.Triggers.Time;

class DailyTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:daily_trigger");
        Subscribe(Time.At("09:50"));
        StartDialogue("jdh:dialogue_id");
    }
}
```

### Specific-day trigger

```csharp
namespace JDH.Triggers.Time;

class SpecificDayTrigger : TriggerResource
{
    private void Init()
    {
        Id("jdh:specific_day_trigger");
        Subscribe(Time.At(Day(2), "10:00"));
        StartStory("jdh:story_id");
    }
}
```

### HUD config

```csharp
namespace JDH.Config.TimeHud;

class MainTimeHud : TimeHudConfigResource
{
    private void Init()
    {
        Id("jdh:main_time_hud");
        Enabled(true);
        Anchor(TOP_RIGHT);
        Offset(x: -24, y: 24);
        ReferenceCanvas(width: 1920, height: 1080);
        SafeMargin(12);
        TextScale(1.0);
        Format("HH:mm");
        ShowDay(false);
        Shadow(true);
        Background(false);
        Opacity(1.0);
        RespectHideHud(true);
        ClampToScreen(true);
        ScreenPolicy(SHOW_DURING_GAME_SCREENS);
    }
}
```

### Главные slash-команды

```text
/echo_time status
/echo_time set 08:30
/echo_time set-day 2 08:30
/echo_time reset
/echo_time add 10m
/echo_time add 2h
/echo_time pause
/echo_time resume
/echo_time pauses
/echo_time speed
/echo_time reload
/echo_time resync
/echo_time next
/echo_time diagnostics
```

### Главный тест trigger

```text
/echo_time set 09:49
/echo_time add 2m
```

### Главное правило gamerule

```text
SyncSun(true)
=> Echo Time ставит doDaylightCycle false
=> Echo Time сам выставляет dayTime
=> солнце всё равно движется по сюжетным часам
```

---

## 21. Текущие ограничения

Это не планы и не обещания будущего API, а честный список того, чего сейчас нет.

- DSL времени принимает только `HH:mm`, без секунд.
- Для длительностей authored DSL поддерживает только `Minutes(...)` и `Hours(...)`.
- `Time.WaitUntil` имеет только today-or-next semantics; параметра `occurrence` нет.
- В Dialogue результат `Time.Is*` не является готовым условным переходом.
- Отдельного weather DSL в Echo Time нет.
- Echo Time не меняет `doWeatherCycle`.
- `SyncMoon(false)` не создаёт независимую заморозку луны.
- HUD поддерживает только `Format("HH:mm")`.
- HUD formatter при `ShowDay(true)` использует английский label `Day`.
- HUD показывает server snapshot и не интерполирует часы сам.
- `StartDialogue` не создаёт ретроактивную доставку игроку, вошедшему после occurrence.
- Trigger, добавленный после прошедшего времени, не воспроизводит прошлое.
- `/echo_time reset` не удаляет delivery history и Story/Dialogue persistence, но открывает новую persistent occurrence generation для повторяемых `Time.At`.
- Slash-команда `add` не принимает выбор crossing policy; она использует `FIRE_CROSSED`.
- Slash-команды `set`, `set-day`, `reset` используют `SKIP_CROSSED`.
- Echo Time не force-load-ит чанки или измерения.
- При `SyncSun(true)` ванильные `/time set` и `doDaylightCycle true` намеренно переопределяются.

---

## Итог

Для обычного проекта держи одну простую модель:

```text
Echo Time — единственные сюжетные часы.
Time.* — способ менять/ждать/проверять их из Story и Dialogue.
Time.At — способ запускать Story или Dialogue на временной границе.
HUD — отображение серверного snapshot.
Vanilla dayTime — проекция Echo Time, а не источник истины.
Weather — независимая система.
```

Рекомендуемая базовая конфигурация:

```csharp
Running(true);
SyncSun(true);
SyncMoon(true);
SunDimension("minecraft:overworld");
```

При этом `doDaylightCycle false` — не баг. Это режим владения, в котором ваниль не мешает, а Echo Time напрямую и детерминированно двигает солнце.
