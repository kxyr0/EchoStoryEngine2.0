# Thought Space Config DSL

`echo_thought_space 0.6.0+` использует настоящий high-level frontend EchoScript 0.3 ES2. Старые `Build()`, `override`, `*Def` и ручная сборка объектов не нужны.

Файлы располагаются в:

```text
story/configs/<namespace>/echo_thought_space/**/*.thought-space.es
```

Основной JDH-файл:

```text
story/configs/jdh/echo_thought_space/thought_space.thought-space.es
```

## Полный пример ES2

```echoscript
class Thoughts : ThoughtSpaceResource
{
    Style(
        zoomInitial: 1.0,
        zoomMin: 0.35,
        zoomMax: 3.0,
        zoomStep: 0.15,
        circleRadius: 30.0,
        nodeFontSize: 1.0,
        titleText: "ФОРМА МЫСЛИ",
        titleSize: 1.5,
        titleX: 0.5,
        titleY: 0.08
    );

    Thought BrokenGlass()
    {
        Text("На полу лежат осколки стекла.");
    }

    Thought MudInside()
    {
        Text("Под окном остались грязные следы.");
    }

    Combination WindowBrokenInside()
    {
        From(
            "jdh:thought_space#broken_glass",
            "jdh:thought_space#mud_inside"
        );

        Thought("Окно разбили изнутри.");
    }
}
```

`namespace` не требуется. Имя resource-класса не влияет на gameplay identity. В одном файле объявляется ровно один `ThoughtSpaceResource`; внутри разрешены один `Style(...)` и любое число `Thought Name()` и `Combination Name()`.

## Style

Все аргументы `Style(...)` именованные и опциональные. Пропущенное поле наследует встроенное значение либо значение предыдущего файла в лексикографическом порядке путей.

| Поле | По умолчанию | Диапазон | Значение |
|---|---:|---:|---|
| `zoomInitial` | `1.0` | `0.1..10.0` | Масштаб при открытии полотна |
| `zoomMin` | `0.35` | `0.1..10.0` | Нижний предел zoom |
| `zoomMax` | `3.0` | `0.1..10.0` | Верхний предел zoom |
| `zoomStep` | `0.15` | `0.01..2.0` | Изменение масштаба за notch колеса |
| `circleRadius` | `30.0` | `4.0..400.0` | Радиус узла при zoom `1.0` |
| `nodeFontSize` | `1.0` | `0.25..8.0` | Множитель размера подписи узла |
| `titleText` | `""` | строка | Заголовок; пустая строка отключает его |
| `titleSize` | `1.5` | `0.25..8.0` | Множитель размера заголовка |
| `titleX` | `0.5` | `0.0..1.0` | Позиция центра заголовка по горизонтали |
| `titleY` | `0.10` | `0.0..1.0` | Позиция центра заголовка по вертикали |

Должно выполняться `zoomMin <= zoomInitial <= zoomMax`.

## Thought

Стартовую мысль, которая сразу появляется на полотне, можно объявить без Observation:

```echoscript
Thought BrokenGlass()
{
    Text("На полу лежат осколки стекла.");
}
```

Для файла `jdh/echo_thought_space/thought_space.thought-space.es` её ID будет выведен автоматически:

```text
jdh:thought_space#broken_glass
```

Имя `BrokenGlass` превращается в `broken_glass`. Необязательный `Id("namespace:path#name")` нужен только тогда, когда ID нельзя менять при переименовании declaration или файла.

## Combination

Сценарист задаёт только символическое имя вывода, два входа и текст новой мысли:

```echoscript
Combination WindowBrokenInside()
{
    From("jdh:thought_space#broken_glass", "jdh:thought_space#mud_inside");
    Thought("Окно разбили изнутри.");
}
```

- `From(...)` принимает ровно два ID существующих мыслей.
- `Thought(...)` принимает одну подпись результата.
- Порядок входов не важен.
- Имя `WindowBrokenInside` нормализуется в `window_broken_inside`.
- ID результата выводится автоматически из пути файла.

Для файла `jdh/echo_thought_space/thought_space.thought-space.es` результат примера имеет ID:

```text
jdh:thought_space#window_broken_inside
```

Этот ID можно использовать во вложенном рецепте:

```echoscript
Combination SuspectKnewTheHouse()
{
    From(
        "jdh:thought_space#window_broken_inside",
        "jdh:house#missing_spare_key"
    );

    Thought("Тот, кто вошёл, знал, где лежит запасной ключ.");
}
```

Если ID должен пережить переименование файла или самой комбинации, добавьте необязательный `Id(...)`:

```echoscript
Id("jdh:deductions#stable_window_conclusion");
```

В обычных рецептах `Id(...)` писать не нужно. Точные ID доступных узлов показывает `/echo_thought_space list`.

## Hot reload и ошибки

Пока полотно открыто, метаданные файлов проверяются раз в `350` мс. EchoScript parser и typed binder запускаются только после изменения списка файлов, timestamp или размера; в render loop разбора DSL нет.

Синтаксическая ошибка EchoScript, неизвестный member/call, неправильный тип аргумента, дубликат пары либо некорректный диапазон отклоняют весь новый набор. Активными остаются последние корректные style и catalog рецептов.

```text
/echo_thought_space status
/echo_thought_space list
/echo_thought_space debug on
```

`status` показывает revision, число файлов, текущий zoom, размеры, число комбинаций и открытых результатов. `list` показывает ID всех текущих узлов, а ниже — все объявленные комбинации в форме `input + input -> result`, включая ещё не открытые результаты.
