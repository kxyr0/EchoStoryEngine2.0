# Presentation

`Presentation(...)` описывает то, как кастомный Echo Effects эффект показывается в UI, логах, отладочных командах и ресурсных браузерах.

Это не ванильный `MobEffect`. Эффект остается declarative `EffectResource`, но у него появляется собственное имя, описание и иконка.

```csharp
private void Init()
{
    Duration(Seconds(7));
    Presentation(
        nameKey: "effect.jdh.body.head_spinning",
        descriptionKey: "effect.jdh.body.head_spinning.description",
        icon: "jdh:textures/echo_effects/icons/head_spinning.png"
    );

    Stacking(RESTART);
    Lifetime(INDEPENDENT);
}
```

## Аргументы

| Аргумент | Тип | Назначение |
| --- | --- | --- |
| `nameKey` | string | Ключ локализации названия эффекта. |
| `descriptionKey` | string | Ключ локализации описания эффекта. |
| `icon` | resource location | Путь к PNG-иконке эффекта. |

## Локализация

Файл `assets/<namespace>/lang/ru_ru.json`:

```json
{
  "effect.jdh.body.head_spinning": "Голова кружится",
  "effect.jdh.body.head_spinning.description": "Комната плывет, шаги становятся тяжелее, а камера мягко уходит в сторону."
}
```

## Иконка

Файл иконки кладется в `assets/<namespace>/textures/echo_effects/icons/`.

```text
assets/jdh/textures/echo_effects/icons/head_spinning.png
```

В `Presentation(...)` этот файл указывается как обычный resource location:

```csharp
icon: "jdh:textures/echo_effects/icons/head_spinning.png"
```

## Пример полного эффекта

```csharp
namespace JDH.Effects;

class HeadSpinningEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:body/head_spinning");
    }

    private void Init()
    {
        Duration(Seconds(7));
        Presentation(
            nameKey: "effect.jdh.body.head_spinning",
            descriptionKey: "effect.jdh.body.head_spinning.description",
            icon: "jdh:textures/echo_effects/icons/head_spinning.png"
        );
        Stacking(RESTART);
        Lifetime(INDEPENDENT);
        EndOn(PLAYER_DEATH, WORLD_EXIT, DISCONNECT);

        Camera.Roll(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(0.8), 6.0, SMOOTH),
            Key(Seconds(2.2), -5.0, SMOOTH),
            Key(Seconds(3.8), 4.0, SMOOTH),
            Key(Seconds(5.4), -3.0, SMOOTH),
            Key(Seconds(7.0), 0.0, SMOOTH)
        ));

        Camera.Shake(
            amplitude: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.4), 0.24, SMOOTH),
                Key(Seconds(5.8), 0.16),
                Key(Seconds(7.0), 0.0, SMOOTH)
            ),
            frequency: 5.5,
            seed: STABLE
        );

        Screen.Vignette(
            color: Color("#10121C"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.6), 0.28, SMOOTH),
                Key(Seconds(5.8), 0.28),
                Key(Seconds(7.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );
    }
}
```
