# Echo Observation DSL

## 1. Назначение

`echo_observation` описывает неизменяемые точки наблюдения в EchoScript. Точка не является Entity или BlockEntity: это определение с world-space anchor, условиями фокусировки, отдельными overlay/thought-текстами и линейным блоком действий после завершения.

Прогресс хранится отдельно для UUID каждого игрока. Сервер является источником истины: клиент выбирает и отображает кандидата, но сервер повторно проверяет активность, dimension, дистанцию, направление взгляда и line of sight.

## 2. Где лежат файлы

```text
data/<namespace>/echo_observation/observations/<name>.es
```

Пример для resource `jdh:museum_room`:

```text
data/jdh/echo_observation/observations/museum_room.es
```

Допускается окончание `.observation.es`; ID всё равно выводится из basename файла. Namespace declaration внутри EchoScript не обязателен.

## 3. Минимальный валидный resource

```echoscript
class MuseumRoomObservations : ObservationResource
{
    Observation DoorLock()
    {
        Focus(1.0);
        Text("На замке нет следов взлома.");
    }
}
```

Без `At(...)` declaration валидна и получает диагностический hint `OBSERVATION_UNPLACED`, но не появляется в gameplay. Это нормальный стартовый формат для последующей расстановки через authoring-мод.

Один source-файл обязан содержать ровно один класс, наследующий `ObservationResource`, и хотя бы одну безаргументную declaration с возвращаемым типом `Observation`.

## 4. Resource ID и Observation ID

По умолчанию resource ID выводится из namespace и имени файла:

```text
data/jdh/echo_observation/observations/museum_room.es
→ jdh:museum_room
```

Его можно задать явно один раз:

```echoscript
class MuseumRoomObservations : ObservationResource
{
    Id("jdh:museum_interior");

    Observation BrokenWindow()
    {
        Text("Стекло разбито изнутри.");
        Thought("Окно разбили изнутри. Нужно понять зачем.");
    }
}
```

Имя declaration нормализуется в snake_case:

```text
BrokenWindow → broken_window
HTTPDoor2 → http_door2
```

Полный ID имеет вид:

```text
<resource-id>#<local-id>
jdh:museum_room#broken_window
```

После публикации ID следует считать постоянным. Если declaration переименована, используйте `Formerly(...)` для переноса completion и disabled-state.

## 5. Полный пример

```echoscript
class MuseumRoomObservations : ObservationResource
{
    Observation BrokenWindow()
    {
        At(
            Block(120, 64, -53),
            Offset(0.721, 0.438, 1.000),
            face: SOUTH,
            dimension: "minecraft:overworld"
        );

        Focus(1.2);
        Distance(10.0);
        ScreenRadius(22.0);
        Repeatable(false);
        Formerly("window_clue");

        Text("Стекло разбито изнутри.");
        Thought("Окно разбили изнутри. Нужно понять зачем.");

        OnComplete()
        {
            Dialogue.Load("jdh:broken_window_reaction");
            Story.Start("jdh:window_discovered");
        }
    }
}
```

## 6. Справочник declaration-вызовов

### `At(...)`

```echoscript
At(
    Block(2, -2, 415),
    Offset(0.000, 0.552, 0.360),
    face: WEST,
    dimension: "minecraft:overworld"
);
```

Параметры:

- `Block(x, y, z)` — целочисленные координаты блока.
- `Offset(x, y, z)` — локальные координаты внутри блока, каждая в диапазоне `0.0..1.0` включительно.
- `face` — `DOWN`, `UP`, `NORTH`, `SOUTH`, `WEST` или `EAST` без кавычек.
- `dimension` — namespaced dimension ID строкой.

World position вычисляется как `Block + Offset`. Разрешён только один `At(...)` на declaration. Рекомендуется не писать его вручную, а использовать authoring-мод.

### `Focus(seconds)`

```echoscript
Focus(1.25);
```

Время удержания фокуса. Допустимо конечное число в диапазоне `(0, 60]` секунд. Значение переводится в ticks через округление вверх. Default: `1.0` секунда.

### `Distance(blocks)`

```echoscript
Distance(8.0);
```

Максимальная дистанция клиентского показа и серверной проверки. Допустимый диапазон `(0, 128]`. Default: `6.0`.

Сервер допускает небольшой технический запас `1.25` блока при проверке позиции, но автору нельзя рассчитывать на него как на часть дизайна.

### `ScreenRadius(pixels)`

```echoscript
ScreenRadius(20.0);
```

Радиус кольца выбора в reference-space 1920×1080. Допустимый диапазон `[4, 128]`. Default: `18.0`.

Это не world-space размер. На другом разрешении он масштабируется относительно физической высоты framebuffer и не зависит от GUI Scale.

### `Repeatable(value)`

```echoscript
Repeatable(false);
```

Default: `false`.

- `false`: завершённая точка больше не eligible, пока прогресс не будет сброшен.
- `true`: точку можно фокусировать повторно. Durable completion ID сохраняется; уже подтверждённые action receipts не выполняются второй раз.

### `Text(value)`

```echoscript
Text("На столе остался ещё тёплый чай.");
```

Короткий inspection-текст. После подтверждённого completion:

1. сервер отправляет текст только завершившему точку игроку;
2. клиент показывает его обычным анимированным Dialogue overlay с speaker `Наблюдение`;
3. overlay не создаёт серверную dialogue-сессию и закрывается автоматически;

`Text(...)` не создаёт узел в Thought Space. Для полотна используется отдельный `Thought(...)`.

Если установленного Dialogue-мода нет, Observation использует компактный fallback, чтобы completion не падал. Если одновременно запускается полноценный диалог, полноценная dialogue-сессия имеет приоритет.

### `Thought(value)`

```echoscript
Thought("На столе остался ещё тёплый чай. Кто-то был здесь недавно.");
```

Внутренняя мысль игрока. После подтверждённого completion она становится read-only подписью узла в Thought Space.

- `Thought(...)` ничего не показывает поверх игрового мира.
- `Thought(...)` не создаёт Dialogue-сессию и не запускает transient overlay.
- Точка без `Thought(...)` не создаёт узел на чёрном полотне, даже если у неё есть `Text(...)`.
- `Text(...)` и `Thought(...)` можно указывать вместе либо использовать только один из них.

Пример только для полотна:

```echoscript
Observation PrivateConclusion()
{
    Thought("Я не должен никому рассказывать о найденном письме.");
}
```

### `Formerly(oldLocalId)`

```echoscript
Formerly("window_clue");
```

Переносит per-player completion и disabled-state со старого локального ID на текущий. Указывается именно local ID без resource-префикса. Вызов можно повторить для нескольких исторических имён.

После успешной миграции старые записи удаляются из актуального состояния. Action receipts сохраняют идемпотентность ранее выполненных действий.

### `OnComplete() { ... }`

```echoscript
OnComplete()
{
    Dialogue.Load("jdh:father_leaves_dialogue");
    Story.Start("jdh:after_father_leaves");
}
```

Разрешён один линейный блок без условий, циклов и вложенных секций. Каждый argument должен быть scalar: string, integer, decimal, boolean или имя/enum.

Встроенные actions:

- `Dialogue.Load("namespace:id")` — запускает Dialogue для UUID игрока из completion context. `audienceId` указывать не нужно и нельзя.
- `Story.Start("namespace:id")` — запускает Story с execution ID, содержащим UUID игрока.

Порядок выполнения совпадает с порядком строк. Для каждого action создаётся durable receipt. После перезагрузки уже подтверждённый action не запускается повторно.

## 7. Полноценный диалог после Observation

Inspection-текст:

```echoscript
Text("Дверь была открыта совсем недавно.");
```

Отдельный текст мысли:

```echoscript
Thought("Кто-то вышел через эту дверь перед моим приходом.");
```

Полноценная реакция:

```echoscript
OnComplete()
{
    Dialogue.Load("jdh:door_reaction");
}
```

Dialogue resource хранится отдельно:

```text
data/jdh/echo_dialogue/dialogues/door_reaction.dialogue.es
```

```echoscript
class DoorReaction : DialogueResource
{
    start Node Start()
    {
        Line("john", "Кто-то только что был здесь.", thought: true);
    }
}
```

Не переносите полноценные ветки, choices и speaker visuals в `Text(...)`.

## 8. Активация из Story

Observation resources по умолчанию не активны для игрока. Управление выполняется из Story context:

```echoscript
private void OnStart()
{
    Observation.Activate("jdh:museum_room");
    Observation.Disable("jdh:museum_room#door_lock");
    Observation.Enable("jdh:museum_room#broken_window");

    Observation.IsActive("jdh:museum_room", output: "roomActive");
    Observation.IsCompleted("jdh:museum_room#broken_window", output: "windowSeen");

    Observation.Deactivate("jdh:museum_room");
}
```

Доступные операции:

| Вызов | Target | Результат |
|---|---|---|
| `Observation.Activate(resource)` | Resource ID | Добавляет resource в active set игрока |
| `Observation.Deactivate(resource)` | Resource ID | Удаляет resource из active set |
| `Observation.Enable(point)` | Observation ID | Удаляет point из disabled set |
| `Observation.Disable(point)` | Observation ID | Добавляет point в disabled set |
| `Observation.IsActive(resource, output: "key")` | Resource ID | Записывает `true`/`false` |
| `Observation.IsCompleted(point, output: "key")` | Observation ID | Записывает `true`/`false` |

Опциональный named argument `player` принимает явный player payload. Без него UUID извлекается из текущего Story execution context. Система не выбирает ближайшего, первого или случайного игрока.

## 9. Gameplay flow

Для появления точки должны одновременно выполняться условия:

- resource активен для игрока;
- point не disabled;
- declaration содержит `At(...)`;
- point не завершена либо имеет `Repeatable(true)`;
- dimension совпадает;
- дистанция не превышает `Distance`;
- anchor находится перед active camera и не закрыт блоками.

Игрок включает концентрацию клавишей `Z`, совмещает точку с центром экрана и удерживает клавишу инвентаря `E`. Во время концентрации открытие inventory отменяется, поэтому `E` принадлежит focus-механике.

Клиент отправляет только start/heartbeat/stop с ID и revision. Процент клиентом не отправляется. Сервер закрепляет definition revision и сам отсчитывает ticks.

## 10. Порядок completion

Сервер выполняет операции в следующем порядке:

1. durable mark completion;
2. lifecycle event;
3. отправка `Text(...)` игроку, если он задан;
4. последовательное выполнение `OnComplete` actions;
5. запись action receipts;
6. синхронизация нового snapshot клиенту; completion делает `Thought(...)` доступным Thought Space.

Прогресс включает:

- active resource IDs;
- disabled Observation IDs;
- completion IDs;
- action execution receipts.

## 11. Команды

```text
/echo_observation status
/echo_observation list
/echo_observation active
/echo_observation inspect "jdh:museum_room#broken_window"
/echo_observation reload
/echo_observation reset "jdh:museum_room#broken_window"
```

- `status` — число resources, points, focus sessions и catalog revision.
- `list` — все полные Observation IDs, не выходя из игры.
- `active` — active resources текущего игрока.
- `inspect` — source, line, revision, placement, focus ticks и anchor.
- `reload` — resource reload, требует permission level 2.
- `reset` — удаляет completion и receipts выбранной точки для текущего игрока. Разрешено оператору либо в integrated server при установленном authoring-моде.

## 12. Reload и last-known-good

Resource reload строит полный candidate catalog. Ошибка синтаксиса, неправильный ID, collision, недопустимый диапазон или неизвестный declaration call отклоняет candidate. Последний успешно установленный catalog продолжает работать.

Nерасставленная declaration создаёт hint, а не error. Это позволяет сначала написать логику, затем расставить точки в игре.

## 13. Частые ошибки

### Точка не видна

Проверьте:

1. есть ли `At(...)`;
2. активирован ли resource для этого UUID;
3. не disabled ли point;
4. не завершена ли non-repeatable point;
5. совпадает ли dimension;
6. хватает ли `Distance`;
7. не перекрыт ли anchor другим блоком;
8. включена ли концентрация.

Используйте `/echo_observation active`, `list` и `inspect`.

### `Text(...)` не показывает полноценный диалог

Это ожидаемо: `Text` — одна transient inspection-line. Для полноценной dialogue-сессии используйте `Dialogue.Load(...)`.

### После переименования пропал прогресс

Добавьте `Formerly("старый_local_id")`, выполните reload и не меняйте одновременно resource ID без отдельной миграции.

### `OnComplete` остановился

Проверьте наличие интеграционного мода для target action и корректность ID. Невыполненный action не получает receipt; уже подтверждённые предыдущие actions повторно не выполняются.
