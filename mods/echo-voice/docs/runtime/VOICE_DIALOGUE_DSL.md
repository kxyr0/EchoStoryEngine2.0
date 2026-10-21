# Echo Dialogue voice DSL

## Озвученная Line

```csharp
Line(
    "john",
    "Текст.",
    voice: Voice("john_001")
);
```

## Line без озвучки

```csharp
Line(
    "sasha",
    "Текст без озвучки."
);
```

Такая Line не запускает и не ищет voice автоматически.

## Volume

```csharp
Line(
    "john",
    "Тихая реплика.",
    voice: Voice(
        "john_002",
        volume: 0.8
    )
);
```

Допустимый диапазон `volume`: `0.0 .. 1.0`; значение по умолчанию — `1.0`.

## Расположение файла

Для DialogueId `jdh:first_morning` и `Voice("john_001")` точная папка:

```text
<gameDir>\story\content-pack\jdh\echo_voice\dialogue\first_morning\
```

Файл может быть `john_001.wav`, `john_001.mp3` или `john_001.ogg`. Одновременно разрешён только один из них.

## Formats

- WAV
- MP3
- OGG

## Extension replacement

`john_001.mp3` → `john_001.wav` не меняет `Voice("john_001")`. После успешного hot reload ключ остаётся прежним, формат меняется, а revision увеличивается.

## Lifecycle

Line enter → voice start. Typewriter reveal → voice continues. Real line advance → voice stops. Next Line → next voice starts. Dialogue end → voice stops.
