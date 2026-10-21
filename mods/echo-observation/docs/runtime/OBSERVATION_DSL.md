# Observation DSL

Файл лежит в `data/<namespace>/echo_observation/observations/<name>.es`; по умолчанию его ID — `<namespace>:<name>`. Namespace declaration внутри EchoScript не обязателен.

```echoscript
class MuseumRoomObservations : ObservationResource
{
    Observation DoorLock()
    {
        Focus(1.0);

        Text(
            "На замке нет следов взлома."
        );

        Thought(
            "Замок не взламывали. Значит, дверь открыли ключом."
        );
    }
}
```

Имя declaration нормализуется: `DoorLock` → `door_lock`, полный ID — `jdh:museum_room#door_lock`. Без `At` point parser-valid, но имеет hint `OBSERVATION_UNPLACED` и не участвует в gameplay.

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
        Thought("Окно разбили изнутри.");

        OnComplete()
        {
            Dialogue.Load("jdh:broken_window_reaction");
        }
    }
}
```

Defaults: `Focus(1.0)`, `Distance(6.0)`, `ScreenRadius(18.0)`, `Repeatable(false)`. `Text(...)` показывает только transient Dialogue overlay. `Thought(...)` записывает только подпись узла Thought Space после completion. Оба вызова независимы и необязательны. `OnComplete` — generic compiled linear block. `Dialogue.Load("namespace:id")` запускает диалог для игрока, завершившего точку, если установлен Echo Dialogue. `Story.Start("namespace:id")` запускает StoryResource, если установлен Echo Story Engine.

Сам диалог также полностью описывается в `.es` по пути `data/<namespace>/echo_dialogue/dialogues/<name>.dialogue.es`:

```echoscript
class BrokenWindowReaction : DialogueResource
{
    start Node Start()
    {
        Line("john", "Стекло разбито изнутри.", thought: true);
    }
}
```

StoryResource может управлять per-player state durable external actions:

```echoscript
private void OnStart()
{
    Observation.Activate("jdh:museum_room");
    Observation.Disable("jdh:museum_room#door_lock");
    Observation.Enable("jdh:museum_room#door_lock");
    Observation.IsActive("jdh:museum_room", output: "roomActive");
    Observation.IsCompleted("jdh:museum_room#broken_window", output: "windowSeen");
    Observation.Deactivate("jdh:museum_room");
}
```

Команды берут UUID из Story execution context (`/player/<uuid>/`) либо из явно унаследованного `player` payload; они не выбирают nearest/first/random player. До `Observation.Activate` точки resource глобально не видимы.
