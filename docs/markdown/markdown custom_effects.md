# Echo Effects DSL

Подробный справочник по DSL пользовательских эффектов Echo Effects для Minecraft Forge 1.20.1.

Документ описывает EF1: декларативные эффекты, которые пишутся в `.es`, проходят настоящий EchoScript parser, затем `EffectResource` binder, validator, curve compiler и попадают в runtime как immutable `EffectDefinition`.

---

## 1. Главная идея

Echo Effects не требует отдельный Java-класс под каждый сюжетный эффект.

Правильная цепочка:

```text
EffectResource .es
-> EchoScript parser
-> EffectResource binder
-> EffectDefinitionValidator
-> immutable EffectDefinition
-> EffectRuntime
-> ActiveEffectInstance
-> channel contributions
-> Forge server/client runtime
-> cleanup
```

Эффект является контентом:

```text
<gameDir>/story/content-pack/<namespace>/echo_effects/effects/<effect>.es
```

Пример:

```text
<gameDir>/story/content-pack/jdh/echo_effects/effects/first_morning_daze.es
```

---

## 2. Что запрещено в DSL эффектов

Нельзя:

- создавать Java-класс на каждый эффект;
- делать `switch` по `EffectId` в Java runtime;
- писать hardcoded `jdh:` effects внутри Java;
- использовать `Action(...)`;
- использовать `ExecuteAction`;
- передавать `Map<String, Object>` как основу semantics;
- вызывать произвольный Java-код из `.es`;
- делать пользовательский `OnTick` callback;
- парсить `.es` каждый tick;
- создавать второй EchoScript parser.

Правильно:

```csharp
class FirstMorningDazeEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:first_morning/daze");
    }

    private void Init()
    {
        Duration(Seconds(6));
        Stacking(RESTART);
        Lifetime(INDEPENDENT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0), 1.0),
            Key(Seconds(0.4), 0.82, SMOOTH),
            Key(Seconds(6), 1.0, SMOOTH)
        ));
    }
}
```

Неправильно:

```csharp
class FirstMorningDazeEffect : EffectResource
{
    private void Init()
    {
        Action("call_java_tick_callback");
        ExecuteAction("jdh.firstMorningDaze");
        OnTick(UpdateCameraByScriptCode);
    }
}
```

---

## 3. Минимальный EffectResource

Самый маленький рабочий эффект:

```csharp
namespace JDH.Effects;

class MinimalEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:minimal");
    }

    private void Init()
    {
        Duration(Seconds(1));
        Stacking(RESTART);
        Lifetime(INDEPENDENT);
    }
}
```

Что обязательно:

- класс наследуется от `EffectResource`;
- есть `Build()`;
- `Build()` возвращает `Effect("namespace:path")`;
- `Init()` содержит декларативные настройки и каналы.

---

## 4. Полный пример: FirstMorningDaze

```csharp
namespace JDH.Effects;

class FirstMorningDazeEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:first_morning/daze");
    }

    private void Init()
    {
        Duration(Seconds(6));

        Stacking(RESTART);
        Lifetime(INDEPENDENT);

        EndOn(
            PLAYER_DEATH,
            WORLD_EXIT
        );

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0.0), 1.0),
            Key(Seconds(0.4), 0.82, SMOOTH),
            Key(Seconds(4.8), 0.82),
            Key(Seconds(6.0), 1.0, SMOOTH)
        ));

        Camera.FovOffset(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(0.5), -8.0, SMOOTH),
            Key(Seconds(4.5), -8.0),
            Key(Seconds(6.0), 0.0, SMOOTH)
        ));

        Camera.Shake(
            amplitude: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.3), 0.32, SMOOTH),
                Key(Seconds(4.8), 0.20),
                Key(Seconds(6.0), 0.0, SMOOTH)
            ),
            frequency: 7.0,
            seed: STABLE
        );

        Screen.Tint(
            color: Color("#6A7180"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.5), 0.24, SMOOTH),
                Key(Seconds(4.8), 0.24),
                Key(Seconds(6.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Screen.Vignette(
            color: Color("#11131A"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.5), 0.30, SMOOTH),
                Key(Seconds(4.8), 0.30),
                Key(Seconds(6.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Sound.PlayAt(
            time: Seconds(0.0),
            sound: "minecraft:block.beacon.ambient",
            volume: 0.25,
            pitch: 0.65
        );
    }
}
```

---

## 5. EffectId

`EffectId` описывает тип эффекта, а не конкретный запуск.

Формат:

```text
namespace:path
```

Примеры:

```text
jdh:first_morning/daze
jdh:dream/soft_blur
jdh:fear/panic_breath
echo_test:camera/fov_pulse
```

В `Build()`:

```csharp
public override EffectResourceDef Build()
{
    return Effect("jdh:fear/panic_breath");
}
```

Не смешивать:

```text
EffectId          = jdh:first_morning/daze
EffectInstanceId = конкретный запуск этого эффекта на конкретном игроке
```

Один и тот же `EffectId` может иметь несколько active instances, если policy `ALLOW_MULTIPLE`.

---

## 6. Duration

Рабочие формы:

```csharp
Duration(Ticks(120));
Duration(Seconds(6));
Duration(InfiniteDuration());
```

Finite duration:

- больше нуля;
- измеряется server ticks;
- не использует wall-clock time;
- при lag длится заданное количество simulation ticks.

Примеры:

```csharp
private void Init()
{
    Duration(Ticks(40)); // 2 секунды при 20 TPS
}
```

```csharp
private void Init()
{
    Duration(Seconds(10));
}
```

```csharp
private void Init()
{
    Duration(InfiniteDuration());
    Lifetime(UNTIL_REMOVED);
}
```

Плохие варианты:

```csharp
Duration(Ticks(0));       // invalid
Duration(Seconds(-2));    // invalid
Duration(Seconds(0));     // invalid
```

---

## 7. Time helpers

Используются внутри duration, curves и timeline events:

```csharp
Ticks(1)
Ticks(20)
Seconds(0.25)
Seconds(1)
Seconds(6.5)
```

Пример:

```csharp
Sound.PlayAt(
    time: Seconds(1.25),
    sound: "minecraft:block.amethyst_block.chime",
    volume: 0.4,
    pitch: 1.2
);
```

---

## 8. Curves

Curve описывает значение канала во времени.

Форма:

```csharp
Curve(
    Key(Seconds(0), 1.0),
    Key(Seconds(1), 0.8, SMOOTH),
    Key(Seconds(5), 0.8),
    Key(Seconds(6), 1.0, SMOOTH)
)
```

Ключ:

```csharp
Key(time, value)
Key(time, value, interpolation)
```

Поддерживаемые interpolation:

```text
STEP
LINEAR
SMOOTH
CUBIC
```

Правила:

- ключей минимум один;
- время ключей строго возрастает;
- duplicate key time запрещен;
- значения должны быть finite;
- curve компилируется один раз, не каждый tick.

---

## 9. Curve: STEP

`STEP` держит левое значение до следующего ключа.

```csharp
Camera.FovOffset(Curve(
    Key(Seconds(0), 0.0),
    Key(Seconds(1), -10.0, STEP),
    Key(Seconds(3), -10.0, STEP),
    Key(Seconds(3.1), 0.0, STEP)
));
```

Подходит для резких переходов:

```csharp
Screen.Tint(
    color: Color("#FFFFFF"),
    opacity: Curve(
        Key(Seconds(0), 0.0),
        Key(Seconds(0.05), 1.0, STEP),
        Key(Seconds(0.20), 0.0, STEP)
    ),
    layer: ABOVE_UI
);
```

---

## 10. Curve: LINEAR

`LINEAR` делает равномерный переход.

```csharp
Movement.SpeedMultiplier(Curve(
    Key(Seconds(0), 1.0),
    Key(Seconds(1), 0.6, LINEAR),
    Key(Seconds(4), 0.6),
    Key(Seconds(5), 1.0, LINEAR)
));
```

---

## 11. Curve: SMOOTH

`SMOOTH` делает мягкий ease-in/ease-out.

```csharp
Camera.FovOffset(Curve(
    Key(Seconds(0), 0.0),
    Key(Seconds(0.8), -12.0, SMOOTH),
    Key(Seconds(4), -12.0),
    Key(Seconds(5), 0.0, SMOOTH)
));
```

---

## 12. Curve: CUBIC

`CUBIC` зарезервирован для более выразительной интерполяции. В EF1 он проходит typed curve compiler и sample-ится как smooth cubic-like transition.

```csharp
Screen.Vignette(
    color: Color("#000000"),
    opacity: Curve(
        Key(Seconds(0), 0.0),
        Key(Seconds(2), 0.55, CUBIC),
        Key(Seconds(8), 0.55),
        Key(Seconds(10), 0.0, CUBIC)
    ),
    layer: BELOW_UI
);
```

---

## 13. Stacking

Форма:

```csharp
Stacking(RESTART);
```

Поддерживаемые policies:

```text
IGNORE_NEW
REFRESH_DURATION
RESTART
STACK_INTENSITY
ALLOW_MULTIPLE
REPLACE_OLD
```

### IGNORE_NEW

Новый Apply возвращает существующий active instance.

```csharp
Stacking(IGNORE_NEW);
```

Пример:

```csharp
class NoSpamDazeEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:daze/no_spam");
    }

    private void Init()
    {
        Duration(Seconds(5));
        Stacking(IGNORE_NEW);

        Camera.Shake(
            amplitude: Curve(Key(Seconds(0), 0.2), Key(Seconds(5), 0.0)),
            frequency: 5.0,
            seed: STABLE
        );
    }
}
```

### REFRESH_DURATION

Новый Apply сбрасывает remaining duration без повторного start-event.

```csharp
Stacking(REFRESH_DURATION);
```

Пример:

```csharp
class PoisonLikeBlurEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:screen/refresh_blur");
    }

    private void Init()
    {
        Duration(Seconds(3));
        Stacking(REFRESH_DURATION);

        Screen.Vignette(
            color: Color("#102014"),
            opacity: Curve(Key(Seconds(0), 0.25), Key(Seconds(3), 0.0)),
            layer: BELOW_UI
        );
    }
}
```

### RESTART

Старая generation завершается, новая начинается заново.

```csharp
Stacking(RESTART);
```

### STACK_INTENSITY

Новый Apply повышает intensity до лимита.

```csharp
Stacking(STACK_INTENSITY);
```

Пример:

```csharp
class PanicStackEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:fear/panic_stack");
    }

    private void Init()
    {
        Duration(Seconds(8));
        Stacking(STACK_INTENSITY);

        Camera.Shake(
            amplitude: Curve(
                Key(Seconds(0), 0.0),
                Key(Seconds(0.4), 0.15, SMOOTH),
                Key(Seconds(8), 0.0, SMOOTH)
            ),
            frequency: 9.0,
            seed: STABLE
        );
    }
}
```

### ALLOW_MULTIPLE

Каждый Apply создает отдельный instance.

```csharp
Stacking(ALLOW_MULTIPLE);
```

### REPLACE_OLD

Старый active instance удаляется, новый становится единственным.

```csharp
Stacking(REPLACE_OLD);
```

---

## 14. Lifetime

Форма:

```csharp
Lifetime(INDEPENDENT);
```

Поддерживаемые policies:

```text
INDEPENDENT
OWNER_BOUND
SESSION_BOUND
UNTIL_REMOVED
```

### INDEPENDENT

Эффект продолжает жить после завершения Dialogue/Story/Cutscene, если duration еще не закончился.

```csharp
Lifetime(INDEPENDENT);
```

### OWNER_BOUND

Эффект завершается вместе с owner execution.

```csharp
Lifetime(OWNER_BOUND);
```

### SESSION_BOUND

Эффект живет в рамках session host-а.

```csharp
Lifetime(SESSION_BOUND);
```

### UNTIL_REMOVED

Подходит для infinite effects.

```csharp
Duration(InfiniteDuration());
Lifetime(UNTIL_REMOVED);
```

---

## 15. EndOn

Форма:

```csharp
EndOn(PLAYER_DEATH, WORLD_EXIT);
```

Поддерживаемые conditions:

```text
PLAYER_DEATH
WORLD_EXIT
DISCONNECT
DIMENSION_CHANGE
OWNER_CANCELLED
OWNER_COMPLETED
```

Пример:

```csharp
private void Init()
{
    Duration(Seconds(30));
    Lifetime(INDEPENDENT);

    EndOn(
        PLAYER_DEATH,
        DISCONNECT,
        DIMENSION_CHANGE
    );
}
```

---

## 16. Movement.SpeedMultiplier

Меняет множитель скорости игрока.

Форма:

```csharp
Movement.SpeedMultiplier(Curve(...));
```

Пример мягкого замедления:

```csharp
Movement.SpeedMultiplier(Curve(
    Key(Seconds(0.0), 1.0),
    Key(Seconds(0.5), 0.75, SMOOTH),
    Key(Seconds(4.0), 0.75),
    Key(Seconds(5.0), 1.0, SMOOTH)
));
```

Пример рывка:

```csharp
Movement.SpeedMultiplier(Curve(
    Key(Seconds(0.0), 1.0),
    Key(Seconds(0.1), 1.35, SMOOTH),
    Key(Seconds(0.8), 1.35),
    Key(Seconds(1.2), 1.0, SMOOTH)
));
```

Правила runtime:

- итоговые movement contributions перемножаются;
- чужие modifiers не перезаписываются;
- после terminal state contribution должен быть удален.

---

## 17. Camera.FovOffset

Добавляет offset к FOV.

Форма:

```csharp
Camera.FovOffset(Curve(...));
```

Сужение FOV:

```csharp
Camera.FovOffset(Curve(
    Key(Seconds(0), 0.0),
    Key(Seconds(0.7), -10.0, SMOOTH),
    Key(Seconds(5), -10.0),
    Key(Seconds(6), 0.0, SMOOTH)
));
```

Короткий импульс:

```csharp
Camera.FovOffset(Curve(
    Key(Seconds(0.0), 0.0),
    Key(Seconds(0.15), 8.0, SMOOTH),
    Key(Seconds(0.35), -4.0, SMOOTH),
    Key(Seconds(0.80), 0.0, SMOOTH)
));
```

---

## 18. Camera.Roll

Наклон камеры.

Форма:

```csharp
Camera.Roll(Curve(...));
```

Пример:

```csharp
Camera.Roll(Curve(
    Key(Seconds(0), 0.0),
    Key(Seconds(0.5), 4.0, SMOOTH),
    Key(Seconds(2.0), -3.0, SMOOTH),
    Key(Seconds(3.0), 0.0, SMOOTH)
));
```

Для cleanup после terminal path:

```text
camera residual offset = 0
camera residual roll = 0
```

---

## 19. Camera.Shake

Тряска камеры.

Форма:

```csharp
Camera.Shake(
    amplitude: Curve(...),
    frequency: 7.0,
    seed: STABLE
);
```

Пример:

```csharp
Camera.Shake(
    amplitude: Curve(
        Key(Seconds(0.0), 0.0),
        Key(Seconds(0.2), 0.5, SMOOTH),
        Key(Seconds(1.2), 0.25),
        Key(Seconds(2.0), 0.0, SMOOTH)
    ),
    frequency: 12.0,
    seed: STABLE
);
```

Мягкая тревога:

```csharp
Camera.Shake(
    amplitude: Curve(
        Key(Seconds(0.0), 0.0),
        Key(Seconds(1.0), 0.08, SMOOTH),
        Key(Seconds(10.0), 0.08),
        Key(Seconds(12.0), 0.0, SMOOTH)
    ),
    frequency: 4.0,
    seed: STABLE
);
```

Важно:

- camera shake не является server-authoritative движением игрока;
- это visual channel;
- он должен очищаться после завершения эффекта.

---

## 20. Screen.Tint

Цветовая заливка экрана.

Форма:

```csharp
Screen.Tint(
    color: Color("#6A7180"),
    opacity: Curve(...),
    layer: BELOW_UI
);
```

Пример:

```csharp
Screen.Tint(
    color: Color("#6A7180"),
    opacity: Curve(
        Key(Seconds(0.0), 0.0),
        Key(Seconds(0.5), 0.24, SMOOTH),
        Key(Seconds(4.8), 0.24),
        Key(Seconds(6.0), 0.0, SMOOTH)
    ),
    layer: BELOW_UI
);
```

Белая вспышка:

```csharp
Screen.Tint(
    color: Color("#FFFFFF"),
    opacity: Curve(
        Key(Seconds(0.00), 0.0),
        Key(Seconds(0.05), 0.85, STEP),
        Key(Seconds(0.30), 0.0, SMOOTH)
    ),
    layer: ABOVE_UI
);
```

Правила:

- цвет в формате `#RRGGBB`;
- opacity обычно `0.0` - `1.0`;
- screen effect должен покрывать весь framebuffer;
- размер не зависит от GUI Scale.

---

## 21. Screen.Vignette

Затемнение краев экрана.

Форма:

```csharp
Screen.Vignette(
    color: Color("#11131A"),
    opacity: Curve(...),
    layer: BELOW_UI
);
```

Пример:

```csharp
Screen.Vignette(
    color: Color("#11131A"),
    opacity: Curve(
        Key(Seconds(0.0), 0.0),
        Key(Seconds(0.5), 0.30, SMOOTH),
        Key(Seconds(4.8), 0.30),
        Key(Seconds(6.0), 0.0, SMOOTH)
    ),
    layer: BELOW_UI
);
```

Темный сонный эффект:

```csharp
Screen.Vignette(
    color: Color("#050610"),
    opacity: Curve(
        Key(Seconds(0.0), 0.0),
        Key(Seconds(2.0), 0.65, SMOOTH),
        Key(Seconds(12.0), 0.65),
        Key(Seconds(14.0), 0.0, SMOOTH)
    ),
    layer: BELOW_UI
);
```

---

## 22. Screen layers

Поддерживаемые layer values:

```text
BELOW_UI
ABOVE_UI
```

`BELOW_UI`:

```csharp
Screen.Tint(
    color: Color("#102030"),
    opacity: Curve(Key(Seconds(0), 0.2), Key(Seconds(2), 0.0)),
    layer: BELOW_UI
);
```

`ABOVE_UI`:

```csharp
Screen.Tint(
    color: Color("#FFFFFF"),
    opacity: Curve(Key(Seconds(0), 0.7), Key(Seconds(0.2), 0.0)),
    layer: ABOVE_UI
);
```

---

## 23. Sound.PlayAt

Одноразовый звук на timeline.

Форма:

```csharp
Sound.PlayAt(
    time: Seconds(0.0),
    sound: "minecraft:block.beacon.ambient",
    volume: 0.25,
    pitch: 0.65
);
```

Примеры:

```csharp
Sound.PlayAt(
    time: Seconds(0.0),
    sound: "minecraft:block.amethyst_block.chime",
    volume: 0.4,
    pitch: 1.3
);
```

```csharp
Sound.PlayAt(
    time: Seconds(1.2),
    sound: "minecraft:entity.player.breath",
    volume: 0.6,
    pitch: 0.8
);
```

```csharp
Sound.PlayAt(
    time: Ticks(30),
    sound: "minecraft:block.portal.ambient",
    volume: 0.2,
    pitch: 0.6
);
```

Timeline events должны иметь fired ledger, чтобы при recovery/reconnect/restart не повторять one-shot sound.

---

## 23.1. VanillaEffect.Apply

`VanillaEffect.Apply(...)` добавляет обычный ванильный `MobEffect` Minecraft на игрока, на котором активен текущий Echo Effects эффект.

Это нужно, когда кастомный эффект должен одновременно иметь:

- свой `EffectId`;
- свое название, описание и иконку через `Presentation(...)`;
- кастомные каналы Echo Effects: camera, screen, movement, sound;
- настоящий vanilla status effect: тошнота, слепота, медлительность, слабость и так далее.

Форма:

```csharp
VanillaEffect.Apply(
    time: Seconds(0.0),
    effect: "minecraft:nausea",
    duration: Seconds(7.0),
    amplifier: 0,
    ambient: false,
    visible: true,
    showIcon: true
);
```

Аргументы:

- `time` - когда применить ванильный эффект относительно старта Echo Effects эффекта; если не указан, используется `Seconds(0)`;
- `effect` - id ванильного эффекта из registry Minecraft, например `minecraft:nausea`;
- `duration` - длительность ванильного эффекта через `Seconds(...)` или `Ticks(...)`;
- `amplifier` - уровень эффекта, где `0` это обычный уровень I, `1` это уровень II;
- `ambient` - vanilla ambient flag;
- `visible` - показывать particles/визуальный след эффекта;
- `showIcon` - показывать иконку эффекта в vanilla HUD.

Пример с тошнотой:

```csharp
VanillaEffect.Apply(
    effect: "minecraft:nausea",
    duration: Seconds(7),
    amplifier: 0
);
```

Пример со слепотой на 2 секунды после задержки:

```csharp
VanillaEffect.Apply(
    time: Seconds(0.5),
    effect: "minecraft:blindness",
    duration: Seconds(2),
    amplifier: 0,
    visible: false,
    showIcon: false
);
```

Пример с медлительностью:

```csharp
VanillaEffect.Apply(
    time: Seconds(0),
    effect: "minecraft:slowness",
    duration: Seconds(5),
    amplifier: 1
);
```

Важно:

- `VanillaEffect.Apply` является one-shot timeline event и срабатывает один раз на активный instance эффекта;
- при `REFRESH_DURATION` ledger сбрасывается, поэтому событие может сработать снова;
- `minecraft:nausea` поддерживается как авторское имя для тошноты; Forge 1.20.1 также учитывает старый внутренний alias `minecraft:confusion`;
- это не заменяет `Presentation(...)`: кастомный Echo Effects эффект все равно остается отдельным эффектом со своим id, названием, описанием и иконкой.

---

## 24. Sound.Loop

`Sound.Loop` зарезервирован для EF1+ runtime cleanup.

Ожидаемая форма:

```csharp
Sound.Loop(
    sound: "minecraft:block.portal.ambient",
    volume: Curve(Key(Seconds(0), 0.0), Key(Seconds(2), 0.4), Key(Seconds(10), 0.0)),
    pitch: 0.7
);
```

Обязательное требование для loop sound:

```text
terminal state -> stop loop sound
remove/fadeOut -> stop loop sound after fade
disconnect -> stop loop sound
restart recovery -> no duplicate loop
```

---

## 25. Particles

Particles зарезервированы как typed channel.

Ожидаемый стиль DSL:

```csharp
Particles.EmitAtPlayer(
    time: Seconds(0.2),
    particle: "minecraft:ash",
    count: 12,
    radius: 0.5
);
```

Ожидаемый loop/curve вариант:

```csharp
Particles.Aura(
    particle: "minecraft:dust",
    countPerSecond: Curve(
        Key(Seconds(0), 0.0),
        Key(Seconds(1), 8.0, SMOOTH),
        Key(Seconds(8), 8.0),
        Key(Seconds(10), 0.0, SMOOTH)
    ),
    radius: 0.8
);
```

Нельзя:

```csharp
Particles.ExecuteJava("spawnCustomParticlesEveryTick");
```

---

## 26. Effect.ApplyToPlayer

Основная команда применения эффекта к текущему игроку execution context.

Рабочая форма:

```csharp
Effect.ApplyToPlayer("jdh:first_morning/daze");
```

С intensity:

```csharp
Effect.ApplyToPlayer(
    "jdh:first_morning/daze",
    intensity: 0.75
);
```

Семантика:

- player берется из текущего EchoScript execution context;
- автор `.es` не получает UUID вручную;
- команда создает deterministic `EffectCommandId`;
- server-authoritative runtime создает `EffectInstanceId`;
- после подтвержденного запуска script продолжает выполнение;
- Apply не ждет окончания эффекта;
- server thread не блокируется.

Нельзя выбирать:

- первого игрока на сервере;
- ближайшего игрока;
- случайного игрока;
- последнего подключившегося игрока.

Ошибка при отсутствии player context:

```text
EFFECT_PLAYER_CONTEXT_MISSING
```

---

## 27. Dialogue example: Apply

```csharp
namespace JDH.Dialogues;

class FirstMorningDialogue : DialogueResource
{
    private void Start()
    {
        Line("Ты открываешь глаза.");

        Effect.ApplyToPlayer("jdh:first_morning/daze");

        Line("Комната медленно собирается в резкость.");
    }
}
```

Важно: в этом примере `Line("Комната...")` выполняется сразу после подтверждения запуска, не после окончания эффекта.

---

## 28. Effect.RemoveFromPlayer

Удаляет эффект с текущего игрока.

Форма:

```csharp
Effect.RemoveFromPlayer("jdh:first_morning/daze");
```

С fade-out:

```csharp
Effect.RemoveFromPlayer(
    "jdh:first_morning/daze",
    fadeOut: Ticks(10)
);
```

Семантика:

- неблокирующая после подтвержденного removal/fade-out;
- не создает новый instance;
- target берется из execution context.

Пример:

```csharp
private void SkipDaze()
{
    Effect.RemoveFromPlayer(
        "jdh:first_morning/daze",
        fadeOut: Ticks(8)
    );

    Line("Ты резко приходишь в себя.");
}
```

---

## 29. Effect.WaitOnPlayer

Ждет terminal state эффекта.

Форма:

```csharp
Effect.WaitOnPlayer("jdh:first_morning/daze");
```

Семантика:

- durable blocking command;
- server thread не блокируется;
- host сохраняет suspension;
- resume происходит после terminal result конкретного effect instance lineage.

Пример:

```csharp
private void Start()
{
    Line("Ты пытаешься подняться.");

    Effect.ApplyToPlayer("jdh:first_morning/daze");
    Effect.WaitOnPlayer("jdh:first_morning/daze");

    Line("Головокружение отпускает.");
}
```

---

## 30. Predicates

Проверки не меняют runtime.

Формы:

```csharp
Effect.IsActiveOnPlayer("jdh:first_morning/daze");
Effect.HasCompletedOnPlayer("jdh:first_morning/daze");
```

Пример:

```csharp
private void MaybeApply()
{
    if (!Effect.IsActiveOnPlayer("jdh:first_morning/daze")) {
        Effect.ApplyToPlayer("jdh:first_morning/daze");
    }
}
```

Пример с completed:

```csharp
private void AfterWakeUp()
{
    if (Effect.HasCompletedOnPlayer("jdh:first_morning/daze")) {
        Line("Теперь ты можешь идти.");
    }
}
```

---

## 31. Story usage

Пример сюжетного действия:

```csharp
namespace JDH.Story;

class MorningStory : StoryResource
{
    private void Start()
    {
        Dialogue.Play("jdh:first_morning/dialogue");

        Effect.ApplyToPlayer("jdh:first_morning/daze");

        Story.SetFlag("jdh:first_morning_started", true);
    }
}
```

С ожиданием:

```csharp
private void Start()
{
    Effect.ApplyToPlayer("jdh:dream/fade_in");
    Effect.WaitOnPlayer("jdh:dream/fade_in");

    Scene.Load("jdh:home/bedroom");
}
```

---

## 32. Cutscene usage

Пример:

```csharp
namespace JDH.Cutscenes;

class IntroCutscene : CutsceneResource
{
    private void Start()
    {
        Effect.ApplyToPlayer("jdh:camera/intro_breath");

        Camera.PlayTrack("jdh:intro/bedroom_pan");

        Effect.WaitOnPlayer("jdh:camera/intro_breath");

        Dialogue.Play("jdh:intro/after_camera");
    }
}
```

---

## 33. Рецепт: короткая белая вспышка

```csharp
namespace JDH.Effects;

class FlashWhiteEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:screen/flash_white");
    }

    private void Init()
    {
        Duration(Seconds(0.35));
        Stacking(RESTART);
        Lifetime(INDEPENDENT);

        Screen.Tint(
            color: Color("#FFFFFF"),
            opacity: Curve(
                Key(Seconds(0.00), 0.0),
                Key(Seconds(0.04), 0.95, STEP),
                Key(Seconds(0.35), 0.0, SMOOTH)
            ),
            layer: ABOVE_UI
        );

        Sound.PlayAt(
            time: Seconds(0.0),
            sound: "minecraft:block.amethyst_block.hit",
            volume: 0.35,
            pitch: 1.6
        );
    }
}
```

Применение:

```csharp
Effect.ApplyToPlayer("jdh:screen/flash_white");
```

---

## 34. Рецепт: сильная усталость

```csharp
namespace JDH.Effects;

class HeavyFatigueEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:vitals/heavy_fatigue");
    }

    private void Init()
    {
        Duration(Seconds(12));
        Stacking(REFRESH_DURATION);
        Lifetime(INDEPENDENT);
        EndOn(PLAYER_DEATH, WORLD_EXIT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0.0), 1.0),
            Key(Seconds(1.0), 0.62, SMOOTH),
            Key(Seconds(10.0), 0.62),
            Key(Seconds(12.0), 1.0, SMOOTH)
        ));

        Camera.FovOffset(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(1.0), -6.0, SMOOTH),
            Key(Seconds(10.0), -6.0),
            Key(Seconds(12.0), 0.0, SMOOTH)
        ));

        Screen.Vignette(
            color: Color("#08090D"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(1.0), 0.45, SMOOTH),
                Key(Seconds(10.0), 0.45),
                Key(Seconds(12.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );
    }
}
```

---

## 35. Рецепт: паника

```csharp
namespace JDH.Effects;

class PanicEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:fear/panic");
    }

    private void Init()
    {
        Duration(Seconds(8));
        Stacking(STACK_INTENSITY);
        Lifetime(INDEPENDENT);
        EndOn(PLAYER_DEATH, DISCONNECT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0.0), 1.0),
            Key(Seconds(0.2), 1.12, SMOOTH),
            Key(Seconds(2.0), 1.12),
            Key(Seconds(8.0), 1.0, SMOOTH)
        ));

        Camera.Shake(
            amplitude: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.25), 0.42, SMOOTH),
                Key(Seconds(6.0), 0.26),
                Key(Seconds(8.0), 0.0, SMOOTH)
            ),
            frequency: 14.0,
            seed: STABLE
        );

        Camera.FovOffset(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(0.3), 5.0, SMOOTH),
            Key(Seconds(6.0), 5.0),
            Key(Seconds(8.0), 0.0, SMOOTH)
        ));

        Screen.Tint(
            color: Color("#301414"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.5), 0.18, SMOOTH),
                Key(Seconds(7.0), 0.18),
                Key(Seconds(8.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Sound.PlayAt(
            time: Seconds(0.0),
            sound: "minecraft:entity.warden.heartbeat",
            volume: 0.3,
            pitch: 1.4
        );
    }
}
```

---

## 36. Рецепт: сонный провал

```csharp
namespace JDH.Effects;

class DreamFallEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:dream/fall");
    }

    private void Init()
    {
        Duration(Seconds(14));
        Stacking(REPLACE_OLD);
        Lifetime(OWNER_BOUND);
        EndOn(WORLD_EXIT, OWNER_CANCELLED);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0.0), 1.0),
            Key(Seconds(2.0), 0.45, SMOOTH),
            Key(Seconds(12.0), 0.45),
            Key(Seconds(14.0), 1.0, SMOOTH)
        ));

        Camera.FovOffset(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(3.0), -18.0, SMOOTH),
            Key(Seconds(12.0), -18.0),
            Key(Seconds(14.0), 0.0, SMOOTH)
        ));

        Camera.Roll(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(3.0), 8.0, SMOOTH),
            Key(Seconds(8.0), -5.0, SMOOTH),
            Key(Seconds(14.0), 0.0, SMOOTH)
        ));

        Screen.Tint(
            color: Color("#2A3048"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(3.0), 0.32, SMOOTH),
                Key(Seconds(12.0), 0.32),
                Key(Seconds(14.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Screen.Vignette(
            color: Color("#02030A"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(3.0), 0.70, SMOOTH),
                Key(Seconds(12.0), 0.70),
                Key(Seconds(14.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );
    }
}
```

---

## 37. Рецепт: бесконечный эффект до ручного удаления

```csharp
namespace JDH.Effects;

class LowOxygenEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:body/low_oxygen");
    }

    private void Init()
    {
        Duration(InfiniteDuration());
        Stacking(IGNORE_NEW);
        Lifetime(UNTIL_REMOVED);
        EndOn(PLAYER_DEATH, WORLD_EXIT, DISCONNECT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0), 0.72)
        ));

        Camera.Shake(
            amplitude: Curve(
                Key(Seconds(0), 0.08)
            ),
            frequency: 3.0,
            seed: STABLE
        );

        Screen.Vignette(
            color: Color("#001018"),
            opacity: Curve(
                Key(Seconds(0), 0.40)
            ),
            layer: BELOW_UI
        );
    }
}
```

Применение:

```csharp
Effect.ApplyToPlayer("jdh:body/low_oxygen");
```

Удаление:

```csharp
Effect.RemoveFromPlayer(
    "jdh:body/low_oxygen",
    fadeOut: Ticks(20)
);
```

---

## 38. Рецепт: cinematic heartbeat

```csharp
namespace JDH.Effects;

class HeartbeatCinematicEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:cinematic/heartbeat");
    }

    private void Init()
    {
        Duration(Seconds(5));
        Stacking(RESTART);
        Lifetime(OWNER_BOUND);

        Camera.FovOffset(Curve(
            Key(Seconds(0.00), 0.0),
            Key(Seconds(0.15), -4.0, SMOOTH),
            Key(Seconds(0.35), 0.0, SMOOTH),
            Key(Seconds(0.70), -4.0, SMOOTH),
            Key(Seconds(0.95), 0.0, SMOOTH),
            Key(Seconds(5.00), 0.0)
        ));

        Screen.Tint(
            color: Color("#260A0A"),
            opacity: Curve(
                Key(Seconds(0.00), 0.0),
                Key(Seconds(0.15), 0.18, SMOOTH),
                Key(Seconds(0.35), 0.0, SMOOTH),
                Key(Seconds(0.70), 0.18, SMOOTH),
                Key(Seconds(0.95), 0.0, SMOOTH),
                Key(Seconds(5.00), 0.0)
            ),
            layer: BELOW_UI
        );

        Sound.PlayAt(
            time: Seconds(0.0),
            sound: "minecraft:entity.warden.heartbeat",
            volume: 0.25,
            pitch: 0.9
        );

        Sound.PlayAt(
            time: Seconds(0.7),
            sound: "minecraft:entity.warden.heartbeat",
            volume: 0.25,
            pitch: 0.9
        );
    }
}
```

---

## 39. Рецепт: алкогольное головокружение

```csharp
namespace JDH.Effects;

class DrunkDizzinessEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:body/drunk_dizziness");
    }

    private void Init()
    {
        Duration(Seconds(20));
        Stacking(REFRESH_DURATION);
        Lifetime(INDEPENDENT);
        EndOn(PLAYER_DEATH, WORLD_EXIT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0), 1.0),
            Key(Seconds(2), 0.88, SMOOTH),
            Key(Seconds(18), 0.88),
            Key(Seconds(20), 1.0, SMOOTH)
        ));

        Camera.Roll(Curve(
            Key(Seconds(0), 0.0),
            Key(Seconds(2), 5.0, SMOOTH),
            Key(Seconds(5), -4.0, SMOOTH),
            Key(Seconds(8), 4.0, SMOOTH),
            Key(Seconds(12), -5.0, SMOOTH),
            Key(Seconds(16), 3.0, SMOOTH),
            Key(Seconds(20), 0.0, SMOOTH)
        ));

        Camera.Shake(
            amplitude: Curve(
                Key(Seconds(0), 0.0),
                Key(Seconds(2), 0.12, SMOOTH),
                Key(Seconds(18), 0.12),
                Key(Seconds(20), 0.0, SMOOTH)
            ),
            frequency: 2.0,
            seed: STABLE
        );

        Screen.Tint(
            color: Color("#6F6458"),
            opacity: Curve(
                Key(Seconds(0), 0.0),
                Key(Seconds(2), 0.16, SMOOTH),
                Key(Seconds(18), 0.16),
                Key(Seconds(20), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );
    }
}
```

---

## 40. Рецепт: холод

```csharp
namespace JDH.Effects;

class ColdEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:environment/cold");
    }

    private void Init()
    {
        Duration(Seconds(10));
        Stacking(REFRESH_DURATION);
        Lifetime(INDEPENDENT);
        EndOn(PLAYER_DEATH, WORLD_EXIT, DISCONNECT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0), 1.0),
            Key(Seconds(1), 0.9, SMOOTH),
            Key(Seconds(9), 0.9),
            Key(Seconds(10), 1.0, SMOOTH)
        ));

        Camera.Shake(
            amplitude: Curve(
                Key(Seconds(0), 0.0),
                Key(Seconds(1), 0.10, SMOOTH),
                Key(Seconds(9), 0.10),
                Key(Seconds(10), 0.0, SMOOTH)
            ),
            frequency: 18.0,
            seed: STABLE
        );

        Screen.Tint(
            color: Color("#AFC8FF"),
            opacity: Curve(
                Key(Seconds(0), 0.0),
                Key(Seconds(1), 0.18, SMOOTH),
                Key(Seconds(9), 0.18),
                Key(Seconds(10), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );
    }
}
```

---

## 41. Рецепт: резкое пробуждение

```csharp
namespace JDH.Effects;

class WakeSnapEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:first_morning/wake_snap");
    }

    private void Init()
    {
        Duration(Seconds(1.2));
        Stacking(RESTART);
        Lifetime(INDEPENDENT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0), 0.65),
            Key(Seconds(0.4), 0.65),
            Key(Seconds(1.2), 1.0, SMOOTH)
        ));

        Camera.FovOffset(Curve(
            Key(Seconds(0), -16.0),
            Key(Seconds(0.2), 10.0, SMOOTH),
            Key(Seconds(1.2), 0.0, SMOOTH)
        ));

        Screen.Tint(
            color: Color("#FFFFFF"),
            opacity: Curve(
                Key(Seconds(0.0), 0.8),
                Key(Seconds(0.15), 0.3, SMOOTH),
                Key(Seconds(1.2), 0.0, SMOOTH)
            ),
            layer: ABOVE_UI
        );

        Sound.PlayAt(
            time: Seconds(0),
            sound: "minecraft:block.glass.break",
            volume: 0.18,
            pitch: 0.7
        );
    }
}
```

---

## 42. Комбинирование эффектов

Можно применять несколько эффектов:

```csharp
Effect.ApplyToPlayer("jdh:vitals/heavy_fatigue");
Effect.ApplyToPlayer("jdh:fear/panic");
Effect.ApplyToPlayer("jdh:screen/flash_white");
```

Runtime должен композить contributions:

```text
movement speed = multiply
fov offset     = sum
camera roll    = sum
shake          = max amplitude
tint opacity   = sum/clamp on renderer
vignette        = sum/clamp on renderer
```

Пример:

```csharp
private void SceneStart()
{
    Effect.ApplyToPlayer("jdh:vitals/heavy_fatigue");
    Effect.ApplyToPlayer("jdh:fear/panic");

    Line("Ты слышишь шаги за дверью.");
}
```

---

## 43. Instance lifecycle

Типичный путь:

```text
CREATED
-> APPLYING
-> ACTIVE
-> FADING_OUT
-> COMPLETED
-> cleanup
```

Удаление:

```text
ACTIVE
-> REMOVED
-> cleanup
```

Ошибка:

```text
APPLYING
-> FAILED
-> cleanup
```

Status не равен outcome.

Statuses:

```text
CREATED
VALIDATING
APPLYING
ACTIVE
FADING_OUT
SUSPENDED
COMPLETED
REMOVED
CANCELLED
FAILED
RECOVERING
```

Outcomes:

```text
COMPLETED
REMOVED
CANCELLED
TARGET_MISSING
TARGET_DISCONNECTED
DEFINITION_MISSING
DEFINITION_STALE
CHANNEL_UNAVAILABLE
CHANNEL_CONFLICT
INVALID_DURATION
INVALID_CURVE
CLIENT_SYNC_FAILED
PERSISTENCE_FAILED
INTERNAL_FAILURE
```

---

## 44. Diagnostics

Основные diagnostic codes:

```text
EFFECT_RESOURCE_PARSE_FAILED
EFFECT_RESOURCE_BIND_FAILED
EFFECT_PLAYER_CONTEXT_MISSING
EFFECT_DEFINITION_MISSING
EFFECT_DUPLICATE_ID
EFFECT_INVALID_DURATION
EFFECT_INVALID_CURVE
EFFECT_UNKNOWN_CHANNEL
EFFECT_INVALID_COLOR
EFFECT_SOUND_EVENT_INVALID
EFFECT_LAST_KNOWN_GOOD_RETAINED
```

Пример ошибки invalid curve:

```csharp
Movement.SpeedMultiplier(Curve(
    Key(Seconds(2), 0.7),
    Key(Seconds(1), 1.0)
));
```

Ожидаемая диагностика:

```text
EFFECT_INVALID_CURVE: Curve keys must be strictly sorted by effect tick.
```

Пример duplicate EffectId:

```csharp
// file A
return Effect("jdh:first_morning/daze");

// file B
return Effect("jdh:first_morning/daze");
```

Ожидаемая диагностика:

```text
EFFECT_DUPLICATE_ID
```

---

## 45. Hot reload

При reload:

```text
scan content-pack
-> parse .es
-> bind EffectResource
-> validate definitions
-> compile curves
-> publish catalog
```

Если candidate invalid:

```text
old accepted catalog stays active
diagnostics are published
last-known-good retained
```

Команда Forge runtime:

```text
/echo_effects reload
```

Проверить definitions:

```text
/echo_effects definitions
```

Проверить diagnostics:

```text
/echo_effects diagnostics
```

Применить вручную:

```text
/echo_effects apply <player> jdh:first_morning/daze
```

Удалить вручную:

```text
/echo_effects remove <player> jdh:first_morning/daze
```

Посмотреть active:

```text
/echo_effects active <player>
```

---

## 46. Production file layout

Пример content pack:

```text
story/
  content-pack/
    jdh/
      echo_effects/
        effects/
          first_morning_daze.es
          panic.es
          dream_fall.es
```

Пример testmod resource:

```text
effects-testmod-1.20.1/
  src/main/resources/
    data/echo_effects_test/echo_effects/effects/first_morning_daze.es
```

---

## 47. Style guide для авторов эффектов

Хорошо:

```csharp
Duration(Seconds(6));
Stacking(RESTART);
Lifetime(INDEPENDENT);
EndOn(PLAYER_DEATH, WORLD_EXIT);
```

Хорошо:

```csharp
Movement.SpeedMultiplier(Curve(
    Key(Seconds(0), 1.0),
    Key(Seconds(0.5), 0.8, SMOOTH),
    Key(Seconds(6), 1.0, SMOOTH)
));
```

Плохо:

```csharp
Movement.SpeedMultiplier(RandomCurve());
```

Плохо:

```csharp
Camera.Shake(
    amplitude: ExecuteAction("get_amplitude_from_java"),
    frequency: 7.0,
    seed: STABLE
);
```

Плохо:

```csharp
Duration(Seconds(999999999));
```

---

## 48. Checklist перед добавлением эффекта

Проверить:

- `EffectId` уникален;
- duration положительный или infinite;
- stacking policy выбран осознанно;
- lifetime policy соответствует сцене;
- `Presentation(...)` задан для сюжетных эффектов, которые должны иметь имя, описание и иконку;
- end conditions заданы;
- curve keys строго отсортированы;
- opacity возвращается к `0.0`;
- movement speed возвращается к `1.0`;
- FOV/roll/shake возвращаются к `0.0`;
- one-shot sounds не повторяются при recovery;
- vanilla effects через `VanillaEffect.Apply` имеют ограниченную duration;
- эффект проходит parser/binder/validator.

---

## 48.1. Собственные название, описание и иконка

Эффект Echo Effects может быть полностью кастомным: без ванильного `MobEffect`, но со своим id, ключом локализации, описанием и иконкой.

За это отвечает декларативный блок `Presentation(...)` внутри `Init()`.

```csharp
Presentation(
    nameKey: "effect.jdh.body.head_spinning",
    descriptionKey: "effect.jdh.body.head_spinning.description",
    icon: "jdh:textures/echo_effects/icons/head_spinning.png"
);
```

Это значит:

- `nameKey` - ключ локализации названия эффекта;
- `descriptionKey` - ключ локализации описания;
- `icon` - путь к PNG-иконке эффекта в ресурсах мода или контент-пака.

Пример локализации:

```json
{
  "effect.jdh.body.head_spinning": "Голова кружится",
  "effect.jdh.body.head_spinning.description": "Комната плывет, шаги становятся тяжелее, а камера мягко уходит в сторону."
}
```

Файл локализации:

```text
assets/jdh/lang/ru_ru.json
```

Файл иконки:

```text
assets/jdh/textures/echo_effects/icons/head_spinning.png
```

Важный момент: `Presentation(...)` не делает эффект ванильным эффектом Minecraft. Он остается ресурсом Echo Effects:

```csharp
public override EffectResourceDef Build()
{
    return Effect("jdh:body/head_spinning");
}
```

Такой эффект применяется обычной DSL-командой:

```csharp
Effect.ApplyToPlayer("jdh:body/head_spinning");
```

Проверяется в игре через отладочный вывод:

```text
/echo_effects definitions
```

В выводе должны быть видны:

```text
jdh:body/head_spinning
nameKey=effect.jdh.body.head_spinning
descriptionKey=effect.jdh.body.head_spinning.description
icon=jdh:textures/echo_effects/icons/head_spinning.png
```

Полный пример эффекта “Голова кружится”:

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

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0.0), 1.0),
            Key(Seconds(0.4), 0.78, SMOOTH),
            Key(Seconds(5.8), 0.78),
            Key(Seconds(7.0), 1.0, SMOOTH)
        ));

        Camera.FovOffset(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(0.5), -7.0, SMOOTH),
            Key(Seconds(5.8), -7.0),
            Key(Seconds(7.0), 0.0, SMOOTH)
        ));

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

        Screen.Tint(
            color: Color("#6F748A"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.6), 0.20, SMOOTH),
                Key(Seconds(5.8), 0.20),
                Key(Seconds(7.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
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

        Sound.PlayAt(
            time: Seconds(0.0),
            sound: "minecraft:block.amethyst_block.chime",
            volume: 0.22,
            pitch: 0.70
        );

        VanillaEffect.Apply(
            time: Seconds(0.0),
            effect: "minecraft:nausea",
            duration: Seconds(7.0),
            amplifier: 0,
            ambient: false,
            visible: true,
            showIcon: true
        );
    }
}
```

---

## 49. Большой пример: сцена первого утра

Effect:

```csharp
namespace JDH.Effects;

class FirstMorningFullEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:first_morning/full");
    }

    private void Init()
    {
        Duration(Seconds(10));
        Stacking(RESTART);
        Lifetime(INDEPENDENT);
        EndOn(PLAYER_DEATH, WORLD_EXIT, DISCONNECT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0.0), 1.0),
            Key(Seconds(0.5), 0.70, SMOOTH),
            Key(Seconds(4.0), 0.70),
            Key(Seconds(8.0), 0.86, SMOOTH),
            Key(Seconds(10.0), 1.0, SMOOTH)
        ));

        Camera.FovOffset(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(0.8), -14.0, SMOOTH),
            Key(Seconds(4.0), -14.0),
            Key(Seconds(7.0), -6.0, SMOOTH),
            Key(Seconds(10.0), 0.0, SMOOTH)
        ));

        Camera.Roll(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(1.2), 5.0, SMOOTH),
            Key(Seconds(3.0), -4.0, SMOOTH),
            Key(Seconds(5.0), 3.0, SMOOTH),
            Key(Seconds(10.0), 0.0, SMOOTH)
        ));

        Camera.Shake(
            amplitude: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.4), 0.30, SMOOTH),
                Key(Seconds(5.0), 0.18),
                Key(Seconds(10.0), 0.0, SMOOTH)
            ),
            frequency: 6.0,
            seed: STABLE
        );

        Screen.Tint(
            color: Color("#6A7180"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.5), 0.28, SMOOTH),
                Key(Seconds(5.0), 0.28),
                Key(Seconds(10.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Screen.Vignette(
            color: Color("#11131A"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.5), 0.36, SMOOTH),
                Key(Seconds(5.0), 0.36),
                Key(Seconds(10.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Sound.PlayAt(
            time: Seconds(0.0),
            sound: "minecraft:block.beacon.ambient",
            volume: 0.25,
            pitch: 0.65
        );

        Sound.PlayAt(
            time: Seconds(3.2),
            sound: "minecraft:block.amethyst_block.chime",
            volume: 0.20,
            pitch: 1.1
        );
    }
}
```

Dialogue:

```csharp
namespace JDH.Dialogues;

class FirstMorningDialogue : DialogueResource
{
    private void Start()
    {
        Line("Сначала был только свет.");

        Effect.ApplyToPlayer("jdh:first_morning/full");

        Line("Потом появилась тяжесть в руках.");
        Line("Комната медленно собиралась из серых пятен.");

        Effect.WaitOnPlayer("jdh:first_morning/full");

        Line("Ты наконец смог сфокусироваться.");
    }
}
```

---

## 50. Короткий итог

Писать эффект нужно так:

```csharp
namespace MyPack.Effects;

class MyEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("mypack:category/my_effect");
    }

    private void Init()
    {
        Duration(Seconds(5));
        Presentation(
            nameKey: "effect.mypack.category.my_effect",
            descriptionKey: "effect.mypack.category.my_effect.description",
            icon: "mypack:textures/echo_effects/icons/my_effect.png"
        );
        Stacking(RESTART);
        Lifetime(INDEPENDENT);
        EndOn(PLAYER_DEATH, WORLD_EXIT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0), 1.0),
            Key(Seconds(1), 0.8, SMOOTH),
            Key(Seconds(5), 1.0, SMOOTH)
        ));

        Camera.FovOffset(Curve(
            Key(Seconds(0), 0.0),
            Key(Seconds(1), -8.0, SMOOTH),
            Key(Seconds(5), 0.0, SMOOTH)
        ));

        Screen.Tint(
            color: Color("#6A7180"),
            opacity: Curve(
                Key(Seconds(0), 0.0),
                Key(Seconds(1), 0.2, SMOOTH),
                Key(Seconds(5), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );
    }
}
```

Применять из host-а:

```csharp
Effect.ApplyToPlayer("mypack:category/my_effect");
```

Ждать:

```csharp
Effect.WaitOnPlayer("mypack:category/my_effect");
```

Удалять:

```csharp
Effect.RemoveFromPlayer(
    "mypack:category/my_effect",
    fadeOut: Ticks(10)
);
```
