# Echo Magic DSL: заклинания, размещение файлов и использование сейчас

Документ описывает фактически реализованную на 2026-08-03 систему магии для Minecraft Forge 1.20.1:

- generic spell engine `echo-magic` версии `0.7.0`;
- runtime-мод `echo-void-magic` версии `0.2.2`;
- runtime-мод `echo-fire-magic` версии `0.3.1`;
- runtime-мод `echo-water-magic` версии `0.2.3`;
- runtime-мод `echo-air-magic` версии `0.2.3`;
- runtime-мод `echo-blood-magic` версии `0.3.0`;
- movement-мод `echo-spark-magic` версии `0.1.0`;
- профиль EchoScript `SpellResource`;
- заклинания `echo_void_magic:void_daggers`, `echo_fire_magic:fire_bolt`, `echo_water_magic:water_bolt`,
  `echo_air_magic:air_bolt` и `echo_blood_magic:blood_bolt`;
- серверные команды, рисование жестов в воздухе, reload и тестирование;
- границу между тем, что уже работает, и API, которое ещё требует host-адаптера.

Главные исходники:

- `D:\JDHSeason\mods\echo-magic`
- `D:\JDHSeason\mods\echo-void-magic`
- `D:\JDHSeason\mods\echo-fire-magic`
- `D:\JDHSeason\mods\echo-water-magic`
- `D:\JDHSeason\mods\echo-air-magic`
- `D:\JDHSeason\mods\echo-blood-magic`
- `D:\JDHSeason\mods\echo-magic-epicfight-adapter`
- `D:\JDHSeason\mods\echo-spark-magic`
- `D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_magic\spells`
- `C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_magic\spells`

Отдельная актуальная инструкция по `V`, шести символам, замедлению времени и связи жестов со `School(...)`:

- `D:\JDHSeason\docs\markdown\magic-gestures.md`

## Самое важное за минуту

Magic DSL — это не файл с расширением `.dsl`. Файл заклинания обязан иметь расширение `.es`.

```text
Правильно: void_daggers.es
Неправильно: void_daggers.dsl
```

`.dsl` loader не увидит. Название «Magic DSL» означает профиль языка EchoScript внутри `.es`:

```csharp
class SomeSpell : SpellResource
{
    private void Init()
    {
        // Полное декларативное описание заклинания.
    }
}
```

Сейчас реально работают:

1. Загрузка `SpellResource` из datapack resources и внешнего `story/content-pack`.
2. автоматический hot reload внешних `.es` с atomic last-known-good каталогом; `/reload` также поддерживается.
3. Выдача, удаление, выбор и применение заклинаний серверными командами.
4. Рисование через переназначаемое действие управления; default — `V`. Символ школы сначала запоминает школу, а следующий прямой штрих выполняет cast после серверной проверки.
5. Server-authoritative burst, generic projectile, swept collision и data-driven damage.
6. Void Daggers: три снаряда, каждый наносит `2.0` damage.
7. Fire Bolt: один снаряд, `4.0` damage и огненный след, частицы которого поднимаются вверх.
8. Water Bolt: один снаряд, `4.0` damage и сине-бирюзовый след без дополнительного движения.
9. Air Bolt: быстрый снаряд, `3.0` damage и серебристо-голубой след с лёгким подъёмом.
10. Blood Bolt: `5.0` начального damage и кровавая метка с пятью импульсами по `1.0` раз в секунду.
11. Blood Rapier: `POINT` призывает на 12 секунд настоящий временный предмет; невидимая item-модель обходится без спрайта, а частицы рисуют клинок и след. Ближние удары получают `+2.0` damage и обновляют метку.
12. Spark Magic: passive жёлтый след, создаваемый только при фактическом движении включённого игрока.
13. Сохранение known spells, выбранного spell и cooldown в SavedData.
14. Клиентский trail/core particle с runtime tint и настраиваемым вектором движения.

Сейчас не подключены напрямую к Story/Dialogue runtime:

```csharp
Magic.GrantToPlayer("echo_void_magic:void_daggers");
Magic.SelectForPlayer("echo_void_magic:void_daggers");
Magic.CastAsPlayer("echo_void_magic:void_daggers");
```

Binder и executor этих вызовов реализованы в `magic-script-adapter-1.20.1`, но в текущих репозиториях нет Story или
Dialogue bridge, который регистрирует их в конкретном host. Поэтому для игры прямо сейчас используй команды и клавишу
применения. Вставлять `Magic.*` в `StoryResource` или `DialogueResource` и ожидать выполнения пока нельзя.

## Из каких частей состоит магия

| Часть | Где находится | Что делает | Что в ней писать |
| --- | --- | --- | --- |
| Generic engine | `mods/echo-magic` | Каталог, known/selected state, cooldown, cast, projectile, collision, damage, sync | Java engine/API; конкретных школ здесь быть не должно |
| Spell definition | `echo_magic/spells/**/*.es` | Описывает параметры одного заклинания | `class ... : SpellResource` и `Init()` |
| Void Forge content | `mods/echo-void-magic/void-magic-forge-1.20.1` | Регистрирует particle, texture и damage type | Java registration и Minecraft JSON/resources |
| Fire Forge content | `mods/echo-fire-magic/fire-magic-forge-1.20.1` | Регистрирует огненную particle, её подъём, texture и damage type | Java registration и Minecraft JSON/resources |
| Water Forge content | `mods/echo-water-magic/water-magic-forge-1.20.1` | Регистрирует water particle, texture и damage type | Java registration и Minecraft JSON/resources |
| Air Forge content | `mods/echo-air-magic/air-magic-forge-1.20.1` | Регистрирует air particle, её подъём, texture и damage type | Java registration и Minecraft JSON/resources |
| Blood Forge content | `mods/echo-blood-magic/blood-magic-forge-1.20.1` | Регистрирует blood particle, texture и damage type | Java registration и Minecraft JSON/resources |
| Universal Epic Fight adapter | `mods/echo-magic-epicfight-adapter` | Рисует частицами любое временное оружие с `SummonedWeaponData`, прикрепляет его к main-hand tool joint и синхронизирует след с атакой Epic Fight | Общий Java-код без проверки школы или item ID; вид атаки задаётся capability JSON оружия |
| JDH Magic content pack | `mods/echo-story-content-pack/src/main/resources/data/jdh/echo_magic/spells` | Хранит все 14 редактируемых `.es` заклинаний по подпапкам школ | Только `SpellResource`; живые Prism-файлы применяются автоматическим hot reload |
| Spark movement content | `mods/echo-spark-magic/spark-magic-forge-1.20.1` | Создаёт particle у ног только при движении включённого игрока | Java movement event, persistent player state и Minecraft resources; атакующего `.es` нет |
| Внешний контент-пак | `<gameDir>/story/content-pack/<pack>/echo_magic/spells` | Позволяет добавить новое spell без пересборки JAR | Новые `.es` с уникальными SpellId |
| Script command adapter | `magic-script-adapter-1.20.1` | Понимает `Magic.*` и требует player context | Используется host-адаптером; сам по себе Story/Dialogue не подключает |
| Public Java API | `magic-forge-api-1.20.1` | Даёт catalog/known/selected/cast services будущему wheel и другим модам | Java integration через `MagicService` |

Ключевая граница:

```text
SpellResource описывает заклинание.
Echo Magic исполняет его.
JDH content pack предоставляет все `.es` определения заклинаний.
Школьные Forge-моды предоставляют runtime, команды, частицы, damage types и особую механику школ.
Echo Spark Magic предоставляет movement-only след и намеренно не регистрирует projectile spell или damage type.
Story/Dialogue должны иметь отдельный bridge, чтобы исполнять Magic.*.
```

## Как установить и использовать прямо сейчас

### Состояние текущего PrismLauncher instance

Проверенный game directory:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft
```

В этот instance уже установлены совместимые production JAR:

```text
echo-magic-0.1.1-all.jar
echo-void-magic-0.1.1.jar
echo-fire-magic-0.1.1.jar
echo-water-magic-0.1.0.jar
echo-air-magic-0.1.0.jar
echo-blood-magic-0.1.0.jar
echo-spark-magic-0.1.0.jar
```

Старый `echo-magic-0.1.0-all.jar` убран из `mods`, поэтому дублирующего mod ID нет.

### Production JAR

Использовать нужно engine и content-моды нужных школ:

```text
D:\JDHSeason\mods\echo-magic\magic-forge-1.20.1\build\libs\echo-magic-0.1.1-all.jar
D:\JDHSeason\mods\echo-void-magic\void-magic-forge-1.20.1\build\libs\echo-void-magic-0.1.1.jar
D:\JDHSeason\mods\echo-fire-magic\fire-magic-forge-1.20.1\build\libs\echo-fire-magic-0.1.1.jar
D:\JDHSeason\mods\echo-water-magic\water-magic-forge-1.20.1\build\libs\echo-water-magic-0.1.0.jar
D:\JDHSeason\mods\echo-air-magic\air-magic-forge-1.20.1\build\libs\echo-air-magic-0.1.0.jar
D:\JDHSeason\mods\echo-blood-magic\blood-magic-forge-1.20.1\build\libs\echo-blood-magic-0.1.0.jar
D:\JDHSeason\mods\echo-spark-magic\spark-magic-forge-1.20.1\build\libs\echo-spark-magic-0.1.0.jar
```

Важно: для engine выбирай именно `echo-magic-0.1.1-all.jar`, а не маленький `echo-magic-0.1.1.jar`.

Закрой Minecraft перед заменой модов. Пример установки через PowerShell:

```powershell
$JdhGame = 'C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft'
$MagicJar = 'D:\JDHSeason\mods\echo-magic\magic-forge-1.20.1\build\libs\echo-magic-0.1.1-all.jar'
$VoidJar = 'D:\JDHSeason\mods\echo-void-magic\void-magic-forge-1.20.1\build\libs\echo-void-magic-0.1.1.jar'
$FireJar = 'D:\JDHSeason\mods\echo-fire-magic\fire-magic-forge-1.20.1\build\libs\echo-fire-magic-0.1.1.jar'
$WaterJar = 'D:\JDHSeason\mods\echo-water-magic\water-magic-forge-1.20.1\build\libs\echo-water-magic-0.1.0.jar'
$AirJar = 'D:\JDHSeason\mods\echo-air-magic\air-magic-forge-1.20.1\build\libs\echo-air-magic-0.1.0.jar'
$BloodJar = 'D:\JDHSeason\mods\echo-blood-magic\blood-magic-forge-1.20.1\build\libs\echo-blood-magic-0.1.0.jar'
$SparkJar = 'D:\JDHSeason\mods\echo-spark-magic\spark-magic-forge-1.20.1\build\libs\echo-spark-magic-0.1.0.jar'

Copy-Item -LiteralPath $MagicJar -Destination (Join-Path $JdhGame 'mods') -Force
Copy-Item -LiteralPath $VoidJar -Destination (Join-Path $JdhGame 'mods') -Force
Copy-Item -LiteralPath $FireJar -Destination (Join-Path $JdhGame 'mods') -Force
Copy-Item -LiteralPath $WaterJar -Destination (Join-Path $JdhGame 'mods') -Force
Copy-Item -LiteralPath $AirJar -Destination (Join-Path $JdhGame 'mods') -Force
Copy-Item -LiteralPath $BloodJar -Destination (Join-Path $JdhGame 'mods') -Force
Copy-Item -LiteralPath $SparkJar -Destination (Join-Path $JdhGame 'mods') -Force
```

Не держи одновременно несколько версий одного и того же mod ID в `mods`.

### Первый запуск Void Daggers

После установки и полного перезапуска игры:

```text
/echo_magic spells
/echo_void_magic setup @s
```

После успешного `setup` появится сообщение:

```text
Void Daggers выбраны для <player>. Используйте назначенную клавишу «Применить выбранное заклинание».
```

Теперь смотри в нужную сторону и используй назначенное действие `Применить выбранное заклинание`. Intent уходит на
сервер; клиент не выбирает произвольную точку попадания и не наносит damage самостоятельно.

Проверить состояние:

```text
/echo_magic status @s
/echo_magic debug player @s
/echo_magic debug status
```

Все корневые команды требуют permission level `2`. В одиночном мире должны быть разрешены cheats; на сервере нужен OP
или соответствующая выдача прав.

### Первый запуск Fire Bolt

После полного перезапуска игры:

```text
/echo_magic spells
/echo_fire_magic setup @s
```

Команда выдаёт и выбирает `echo_fire_magic:fire_bolt`. После этого используется то же действие
`Применить выбранное заклинание`. Fire Bolt выпускает один projectile с огненным круговым следом. Каждый sprite следа
продолжает двигаться вверх со скоростью `0.04` блока за тик и одновременно исчезает за `10` ticks, поэтому круги не
остаются неподвижной полосой.

### Первый запуск Water Bolt

После полного перезапуска игры:

```text
/echo_magic spells
/echo_water_magic setup @s
```

Команда выдаёт и выбирает `echo_water_magic:water_bolt`. Способность использует тот же круг и glow falloff, что
`void_circle.png`; изменена только палитра на тёмно-синюю с ярким cyan-центром. Дополнительное движение particle для
воды не задано.

### Первый запуск Air Bolt

После полного перезапуска игры:

```text
/echo_magic spells
/echo_air_magic setup @s
```

Команда выдаёт и выбирает `echo_air_magic:air_bolt`. Это быстрый projectile с серебристо-небесным кругом. Sprite
поднимается на `0.025` блока за тик — слабее огненного `0.04`, поэтому воздушный след выглядит лёгким, но не пламенным.

### Первый запуск Blood Bolt

После полного перезапуска игры:

```text
/echo_magic spells
/echo_blood_magic setup @s
```

Команда выдаёт и выбирает `echo_blood_magic:blood_bolt`. Частица сохраняет круг и плавный glow исходного ассета,
но использует ярко-красный центр и тёмно-красные края. Самостоятельное движение sprite не задано.

### Включение movement-only Spark Magic

После полного перезапуска игры:

```text
/echo_spark_magic setup @s
/echo_spark_magic status @s
```

После `setup` жёлтые частицы появляются у ног только при фактическом изменении позиции. В покое, spectator mode и при
езде particle не создаются. Это passive movement trail: он не занимает selected spell, не наносит damage и не
запускается клавишей `V`.

Управление:

```text
/echo_spark_magic enable @s
/echo_spark_magic disable @s
/echo_spark_magic status @s
```

Включённое состояние сохраняется у игрока и копируется после respawn.

### Клавиша

Клавиша зарегистрирована в Minecraft Controls:

```text
Категория: Echo Magic
Действие: Применить выбранное заклинание
Default: V
```

`V` — только default. Переназначение выполняется через стандартный локализованный экран:

```text
Настройки → Управление → Назначение клавиш → Магия Echo → Применить выбранное заклинание
```

Можно назначить другую клавишу клавиатуры или кнопку мыши. Minecraft сохраняет bind в `options.txt`, поэтому отдельной
настройки клавиши в `echo-magic-client.toml` нет и быть не должно.

Клик потребляется только в игре: открытый GUI не должен запускать cast. Spectator и мёртвый игрок cast не принимаются.

## Где должны лежать `.es` заклинания

### Внутри обычного Forge/datapack resource

Resource loader ищет:

```text
data/<namespace>/echo_magic/spells/**/*.es
```

Реальный Void Daggers в исходнике JDH content pack:

```text
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_magic\spells\void\void_daggers.es
```

В школьный Void JAR заклинание больше не упаковывается. Live-копия находится по пути:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_magic\spells\void\void_daggers.es
```

### Во внешнем story content-pack

Echo Magic отдельно сканирует:

```text
<gameDir>/story/content-pack/**/echo_magic/spells/**/*.es
```

Для текущего JDH pack рекомендуемый путь:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_magic\spells\
```

Пример:

```text
...\content-pack\jdh\data\jdh\echo_magic\spells\void\void_bolt.es
```

После изменения внешнего файла достаточно выполнить:

```text
/echo_magic spells
```

### Где не надо размещать spell

Эти варианты loader не считает Magic spell source:

```text
story/content-pack/jdh/magic/void_bolt.es
story/content-pack/jdh/echo_magic/void_bolt.es
story/content-pack/jdh/echo_magic/spells/void_bolt.dsl
assets/jdh/echo_magic/spells/void_bolt.es
```

Путь обязан содержать каталог `echo_magic/spells`, а расширение обязано быть `.es`.

### Как изменить существующий Void Daggers

Bundled-копии больше нет, поэтому дубликат ID не создаётся. Редактируй единственный live-файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_magic\spells\void\void_daggers.es
```

После сохранения живого Prism-файла подожди до `0,75` секунды: hot reload сработает автоматически, пересобирать школьный JAR или выполнять `/reload` не требуется. Для нового заклинания всё равно используй уникальный ID, например `jdh:void_bolt`.

## Полный минимальный шаблон SpellResource

Все секции, кроме `FriendlyFire`, обязательны. Практически лучше всегда писать и `FriendlyFire`, чтобы поведение было
видно прямо в файле.

```csharp
namespace JDH.Magic;

class ExampleBoltSpell : SpellResource
{
    private SpellId Spell = "jdh:example_bolt";
    private MagicSchoolId SchoolId = "echo_void_magic:void";
    private MagicDamageTypeId DamageId = "echo_void_magic:void_magic";
    private MagicParticleStyleId ParticleId = "echo_void_magic:void_circle";

    private int ProjectileCount = 1;
    private double DamageAmount = 2.0;
    private double ProjectileSpeed = 1.2;
    private double ProjectileRange = 20.0;
    private double HitRadius = 0.20;

    private void Init()
    {
        Id(Spell);
        DisplayName("Example Bolt");
        School(SchoolId);

        Cooldown(Ticks(20));

        Cast(
            Origin(PLAYER_EYES),
            Direction(PLAYER_LOOK),
            Hand(MAIN_HAND),
            SwingHand(true)
        );

        Burst(
            count: ProjectileCount,
            interval: Ticks(0)
        );

        Projectile(
            speed: BlocksPerTick(ProjectileSpeed),
            range: Blocks(ProjectileRange),
            radius: HitRadius,
            gravity: 0.0,
            pierce: 0
        );

        Collision(
            blocks: STOP,
            entities: FIRST_VALID_TARGET
        );

        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);

        Trail(
            particle: ParticleId,
            spacing: 0.18,
            size: 0.30,
            lifetime: Ticks(8),
            color: Color("#0F6B32"),
            alphaStart: 0.90,
            alphaEnd: 0.0
        );

        Core(
            particle: ParticleId,
            size: 0.16,
            color: Color("#45FF4A")
        );

        Hit(
            Damage(
                type: DamageId,
                amount: DamageAmount
            )
        );
    }
}
```

Этот пример переиспользует Void particle и Void damage type. Поэтому для него должны быть установлены оба production
JAR: `echo_magic` и `echo_void_magic`.

Запуск примера после сохранения в external content-pack:

```text
/echo_magic spells
/echo_magic grant @s jdh:example_bolt
/echo_magic select @s jdh:example_bolt
/echo_magic cast @s
```

После выбора можно использовать назначенную клавишу вместо `/echo_magic cast @s`.

## Какой EchoScript разрешён внутри SpellResource

Канонический и поддерживаемый профиль очень узкий. Это намеренно declarative DSL, а не общий Java/C# runtime.

Разрешено:

- опциональный `namespace`;
- первый top-level class с base type `SpellResource`;
- поля-константы со string, integer, decimal или boolean literal;
- enum-like name в поле, например `private SpellOrigin OriginId = PLAYER_EYES;`;
- один метод `Init()`;
- прямые expression statements разрешённых intrinsic-вызовов внутри `Init()`;
- positional и named arguments в предусмотренных местах;
- комментарии, которые понимает EchoScript parser;
- переносы длинных аргументов на несколько строк.

Надёжный стиль полей:

```csharp
private SpellId Spell = "jdh:spell";
private int Count = 3;
private double Speed = 1.6;
private bool CanHitAllies = false;
```

Declared type поля используется для читаемости. Binder смотрит на literal initializer, а не исполняет систему типов C#.

Не используй цепочки констант:

```csharp
private SpellId First = "jdh:spell";
private SpellId Second = First; // Не становится значением First; binder увидит имя "First".
```

Не поддерживается SpellResource binder-ом:

- `Build()` вместо `Init()`;
- local variables внутри `Init()`;
- helper-методы, которые должны вернуть секцию;
- `if`, `else`, `switch`;
- `for`, `while`, `foreach`;
- арифметика и вычисляемые выражения;
- присваивания после объявления;
- callbacks `OnCast`, `OnHit`, `OnTick`;
- создание Java/Minecraft объектов;
- `new SomeType(...)`;
- вызовы команд, damage или particle spawn вручную;
- несколько spell definitions, которые binder должен собрать из одного класса.

Неправильно:

```csharp
private void Init()
{
    var damage = 2.0;

    if (damage > 1.0)
    {
        Hit(Damage(type: "jdh:magic", amount: damage));
    }
}
```

Неправильно:

```csharp
public override SpellDefinition Build()
{
    return Spell(...);
}
```

Правило: один `.es` — один `SpellResource`, а все его секции являются прямыми вызовами в `Init()`.

## Полный справочник секций

### Id

```csharp
Id("jdh:example_bolt");
```

Или через field constant:

```csharp
private SpellId Spell = "jdh:example_bolt";
Id(Spell);
```

Формат:

```text
[a-z0-9_.-]+:[a-z0-9_./-]+
```

Правильно:

```text
jdh:void_bolt
jdh:chapter_1/void_bolt
echo_void_magic:void_daggers
```

Неправильно:

```text
JDH:VoidBolt
jdh
jdh:void bolt
```

`SpellId` — постоянный save/API contract. Без необходимости не переименовывай ID уже выданного игрокам spell.

### DisplayName

```csharp
DisplayName("Void Daggers");
```

Строка не должна быть пустой. Сейчас это authored display string, а не translation key.

### School

```csharp
School("echo_void_magic:void");
```

`MagicSchoolId` обязан содержать `:`. Сейчас school — типизированная классификация для каталога и будущего spell wheel.
Generic engine не делает `switch` по school и не добавляет механику автоматически.

### Cooldown

```csharp
Cooldown(Ticks(16));
```

`20 ticks = 1 second` при нормальном TPS. Значение должно быть `>= 0`.

Cooldown:

- начинается сразу после принятого cast;
- хранится как server ready tick;
- повторный intent до ready tick отклоняется;
- сохраняется и синхронизируется клиенту.

### Cast

```csharp
Cast(
    Origin(PLAYER_EYES),
    Direction(PLAYER_LOOK),
    Hand(MAIN_HAND),
    SwingHand(true)
);
```

Поддерживаемые enum values в contracts:

```text
Origin: PLAYER_EYES
Direction: PLAYER_LOOK
Hand: MAIN_HAND, OFF_HAND
SwingHand: true, false
```

Фактический runtime сейчас всегда создаёт projectile из server player eyes, берёт server look и добавляет forward offset
`0.35`. Клиент не может прислать собственный direction.

Текущее ограничение: `OFF_HAND` парсится, но runtime swing пока вызывает `MAIN_HAND`. Для предсказуемого результата сейчас
пиши `Hand(MAIN_HAND)`.

### Burst

```csharp
Burst(count: 3, interval: Ticks(2));
```

| Аргумент | Значение |
| --- | --- |
| `count` | Число generic projectile, минимум `1` |
| `interval` | Интервал между emissions в server ticks, минимум `0` |

При cast на tick `T` пример выше выпускает projectile на `T`, `T+2`, `T+4`.

Server config ограничивает `count` через `maximum_burst_count`, default `32`.

### Projectile

```csharp
Projectile(
    speed: BlocksPerTick(1.6),
    range: Blocks(24.0),
    radius: 0.22,
    gravity: 0.0,
    pierce: 0
);
```

| Аргумент | Единица | Ограничение | Что делает |
| --- | --- | --- | --- |
| `speed` | blocks/tick | `> 0`, не выше server limit | Длина движения за tick |
| `range` | blocks | `> 0`, не выше server limit | Максимальный пройденный путь |
| `radius` | blocks | `>= 0` | Расширяет AABB entity при swept collision |
| `gravity` | blocks/tick² | `>= 0` | Каждый tick добавляется вниз к velocity |
| `pierce` | count | `>= 0` | Зарезервированный contract |

Текущее ограничение: значение `pierce` сохраняется в definition, но generic runtime пока завершает projectile после первого
block/entity hit и не использует pierce counter. Сейчас пиши только `pierce: 0`.

Particle не является projectile. Projectile — серверная entity с ID, caster, pinned definition revision, previous/current
position, range и hit receipt.

### Collision

```csharp
Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
```

Contracts содержат:

```text
blocks: STOP, PASS
entities: FIRST_VALID_TARGET, PASS
```

Но текущий Forge runtime ещё не переносит collision policy в projectile payload: он всегда ищет ближайший block collider и
первую ближайшую valid living entity. Поэтому сейчас поддержанный практический вариант только:

```csharp
Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
```

Не используй `PASS`, пока runtime не начнёт применять этот contract.

Collision server-authoritative и swept: проверяется весь отрезок previous → current, а не только endpoint. Стекло с
collision shape останавливает projectile. Fluid игнорируется через `ClipContext.Fluid.NONE`.

### Targets

```csharp
Targets(LIVING_EXCEPT_CASTER);
```

Сейчас это единственный `SpellTargetPolicy`.

Исключаются:

- caster;
- мёртвые и removed entity;
- spectator;
- invulnerable target;
- marker armor stand;
- allies при `FriendlyFire(false)`.

### FriendlyFire

```csharp
FriendlyFire(false);
```

При `false` engine сначала спрашивает `MagicTargetRelationProvider`, затем проверяет vanilla team relation. При `true`
ally filter не применяется. Caster всё равно исключён политикой `LIVING_EXCEPT_CASTER`.

Если секцию пропустить, draft default равен `false`, но лучше писать её явно.

### Trail

```csharp
Trail(
    particle: "echo_void_magic:void_circle",
    spacing: 0.18,
    size: 0.30,
    lifetime: Ticks(8),
    color: Color("#0F6B32"),
    vanillaColor: Color("#0F6B32"),
    alphaStart: 0.90,
    alphaEnd: 0.0
);
```

| Аргумент | Ограничение |
| --- | --- |
| `particle` | Зарегистрированный `namespace:path` particle style |
| `spacing` | `> 0`, расстояние между trail points в blocks |
| `size` | `> 0` |
| `lifetime` | минимум `1` tick |
| `color` | RGB `#RRGGBB` |
| `vanillaColor` | Необязательный RGB для `minecraft:dust` и общего ванильного режима; default равен `color` |
| `alphaStart` | `0.0..1.0` |
| `alphaEnd` | `0.0..1.0` |

Trail sampling выполняется клиентом по пройденному отрезку и переносит remainder spacing между ticks. Сервер не отправляет
packet на каждую trail point.

ID должен существовать в particle registry. Специальные Echo-частицы требуют установленный content-мод, а ванильные
ID вида `minecraft:dust`, `minecraft:flame`, `minecraft:splash`, `minecraft:cloud` уже доступны без Java-регистрации.

Самый простой вариант с точным цветом — ванильная пыль:

```csharp
private MagicParticleStyleId ParticleId = "minecraft:dust";
```

После этого `Trail` и `Core` используют свой `vanillaColor`. Для `minecraft:dust` работают цвет и размер; lifetime и
alpha-кривая остаются ванильными. Простые ванильные частицы вроде `minecraft:flame` используют собственные цвет и
анимацию, поэтому `vanillaColor` для них не перекрашивает sprite.

### Core

```csharp
Core(
    particle: "echo_void_magic:void_circle",
    size: 0.16,
    color: Color("#45FF4A"),
    vanillaColor: Color("#45FF4A")
);
```

`size` должен быть `> 0`, оба цвета — `#RRGGBB`. `Core.particle` выбирается независимо от `Trail.particle`, поэтому
ядро и след можно сделать разными. Core создаётся с повышенным приоритетом particle budget.

### Hit и Damage

```csharp
Hit(
    Damage(
        type: "echo_void_magic:void_magic",
        amount: 2.0
    )
);
```

`amount` должен быть конечным числом `> 0` и не выше `maximum_damage_per_hit` из server config.

Damage type обязан существовать в Minecraft damage type registry. Echo Void Magic предоставляет:

```text
echo_void_magic:void_magic
```

Engine создаёт `DamageSource` с:

```text
direct entity = generic MagicProjectileEntity
causing entity = caster
target = выбранная LivingEntity
```

Затем сервер вызывает `target.hurt(...)`. Прямое изменение health из DSL отсутствует. Один `SpellProjectileId` получает
не более одной damage receipt.

Если damage type не зарегистрирован, projectile визуально завершится, но `hurt` не будет применён.

## Полный рабочий Void Daggers

Это фактический source, а не приблизительный пример:

```csharp
namespace EchoVoidMagic.Spells;

class VoidDaggersSpell : SpellResource
{
    private SpellId VoidDaggers =
        "echo_void_magic:void_daggers";

    private MagicSchoolId VoidMagic =
        "echo_void_magic:void";

    private MagicDamageTypeId VoidDamage =
        "echo_void_magic:void_magic";

    private MagicParticleStyleId VoidCircle =
        "echo_void_magic:void_circle";

    private int DaggerCount = 3;
    private double DamagePerDagger = 2.0;
    private double Speed = 1.6;
    private double Range = 24.0;
    private double HitRadius = 0.22;

    private void Init()
    {
        Id(VoidDaggers);
        DisplayName("Void Daggers");
        School(VoidMagic);

        Cooldown(Ticks(16));

        Cast(
            Origin(PLAYER_EYES),
            Direction(PLAYER_LOOK),
            Hand(MAIN_HAND),
            SwingHand(true)
        );

        Burst(
            count: DaggerCount,
            interval: Ticks(2)
        );

        Projectile(
            speed: BlocksPerTick(Speed),
            range: Blocks(Range),
            radius: HitRadius,
            gravity: 0.0,
            pierce: 0
        );

        Collision(
            blocks: STOP,
            entities: FIRST_VALID_TARGET
        );

        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);

        Trail(
            particle: VoidCircle,
            spacing: 0.18,
            size: 0.30,
            lifetime: Ticks(8),
            color: Color("#0F6B32"),
            alphaStart: 0.90,
            alphaEnd: 0.0
        );

        Core(
            particle: VoidCircle,
            size: 0.16,
            color: Color("#45FF4A")
        );

        Hit(
            Damage(
                type: VoidDamage,
                amount: DamagePerDagger
            )
        );
    }
}
```

Результат одного принятого cast:

```text
3 projectile
emission ticks: T, T+2, T+4
2.0 damage максимум от одного projectile
6.0 theoretical total при попадании всех трёх
cooldown: 16 ticks
speed: 1.6 blocks/tick
range: 24 blocks
hit radius: 0.22 blocks
```

## Какие параметры можно безопасно менять в Void Daggers

Можно менять в `.es` и пересобирать Void JAR:

```csharp
private int DaggerCount = 3;
private double DamagePerDagger = 2.0;
private double Speed = 1.6;
private double Range = 24.0;
private double HitRadius = 0.22;
```

Также можно менять:

- cooldown;
- burst interval;
- gravity;
- friendly fire;
- trail spacing, size, lifetime, tint и alpha;
- core size и tint;
- display name.

Нельзя получить одной заменой значения в текущем DSL:

- AoE explosion;
- healing;
- status effect;
- teleport;
- homing projectile;
- chain lightning;
- block placement;
- sound;
- animation callback;
- отдельный `OnHit` script;
- реальный pierce;
- collision `PASS`.

Для такой механики нужен новый typed contract и generic runtime capability в `echo-magic`, а не hardcoded
`if (spellId == ...)` в engine.

## Реализованный Fire Bolt

Файл заклинания:

```text
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_magic\spells\fire\fire_bolt.es
```

Основные параметры сейчас:

```text
spell: echo_fire_magic:fire_bolt
school: echo_fire_magic:fire
damage type: echo_fire_magic:fire_magic
particle: echo_fire_magic:fire_flame
projectile count: 1
damage: 4.0
cooldown: 20 ticks
speed: 1.35 blocks/tick
range: 22 blocks
hit radius: 0.26 blocks
trail lifetime: 10 ticks
```

Начиная с Fire Magic `0.1.1`, texture имеет ту же круговую геометрию, что Void/Water/Air/Blood particles. Горячий
жёлто-белый центр плавно переходит в оранжевое свечение и тёмно-красно-оранжевые края. Стабильный registry ID
`echo_fire_magic:fire_flame` сохранён, поэтому существующие `.es` менять не нужно.

В `.es` можно менять damage, cooldown, скорость, дальность, радиус, trail/core и прочие уже описанные generic-параметры.
Но самостоятельный подъём particle — не параметр текущего SpellResource. Он задаётся Forge-регистрацией:

```java
new MagicParticleMotion(0.0D, 0.04D, 0.0D)
```

Это находится в `FireMagicClientBootstrap.java`. Значение `y > 0` тянет огненный круг вверх; `y < 0` потянуло бы вниз.
Старый constructor без motion использует `MagicParticleMotion.NONE`, поэтому Void particle не изменилась.

`echo_fire_magic:fire_magic` отмечен одновременно в `forge:is_magic` и `minecraft:is_fire`. На этом этапе Fire Bolt
наносит fire-tagged magic damage, но ещё не оставляет отдельное горение на несколько секунд: DSL-контракта поджигания
пока нет.

## Реализованный Water Bolt

Файл заклинания:

```text
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_magic\spells\water\water_bolt.es
```

Water Bolt использует те же числовые параметры, что первая версия Fire Bolt: один projectile, `4.0` damage,
cooldown `20` ticks, скорость `1.35`, дальность `22`, радиус `0.26` и trail lifetime `10` ticks.

Исходный `void_circle.png` не перерисовывался: размер `256×256`, alpha-map, положение центра, радиусы и glow falloff
совпадают. Изменены только RGB-каналы: тёмно-зелёный стал navy-blue, а ярко-зелёный центр — cyan. В отличие от Fire,
Water particle использует `MagicParticleMotion.NONE` и не поднимается самостоятельно.

## Реализованный Air Bolt

Файл заклинания:

```text
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_magic\spells\air\air_bolt.es
```

Air Bolt легче и быстрее базовых Fire/Water Bolt: один projectile, `3.0` damage, cooldown `16` ticks, скорость
`1.65`, дальность `24`, радиус `0.22` и trail lifetime `10` ticks.

Texture сохраняет размер и alpha-map исходного круга, но использует серебристо-небесную палитру с почти белым
центром. `AirMagicClientBootstrap` передаёт `MagicParticleMotion(0.0, 0.025, 0.0)`, поэтому sprite медленно поднимается
вверх. Эта скорость меньше огненной `0.04`.

## Реализованные Blood Bolt и Blood Rapier

Файл заклинания:

```text
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_magic\spells\blood\blood_bolt.es
```

Blood Bolt — более тяжёлый базовый projectile: `5.0` начального damage, cooldown `24` ticks, скорость `1.2`, дальность
`20`, радиус `0.28` и trail lifetime `10` ticks. Успешное попадание ставит метку на 5 секунд. Раз в секунду она
наносит ещё `1.0` кровавого урона, то есть полный урон без повторного обновления достигает `10.0`.

Texture сохраняет размер, alpha-map, кольцо и glow falloff исходного `void_circle.png`. Центр имеет цвет около
`#FF1307`, боковая область — около `#4F0602`; между ними остаётся исходный плавный градиент.

`blood_rapier.es` использует `Technique(WEAPON)`. Замкнутый треугольник `△` через `V` призывает временный настоящий
`echo_blood_magic:blood_rapier`. Предыдущий предмет ведущей руки сохраняется внутри NBT временного оружия и безопасно
возвращается после истечения срока, смерти или выхода игрока. Рапиру нельзя выбросить, а пока она активна система
возвращает её в выбранный слот.

PNG-спрайта у рапиры нет: item model использует невидимый `minecraft:builtin/entity`, а универсальный
`echo-magic-epicfight-adapter` строит клинок, гарду и след цветной ванильной пылью. NBT-контракт
`SummonedWeaponData` хранит владельца, срок, сохранённый предмет, RGB, длину клинка и характер движения. Поэтому
будущая пустотная сабля или другое призванное оружие подключаются к тому же адаптеру без новой проверки item ID.
Epic Fight capability `type: epicfight:spear` даёт рапире колющий набор атак. Каждый успешный ближний удар получает
`+2.0` урона и заново ставит полную метку. Cooldown рапиры — 15 секунд, поэтому после рассеивания остаётся 3 секунды
паузы.

Начиная с adapter `0.1.1`, положение рукояти не вычисляется от камеры или груди. Адаптер получает текущую позу
Epic Fight, преобразует main-hand joint `Tool_R` в мировые координаты и строит все точки клинка относительно него.
Постоянные частицы живут только 2 тика, поэтому не отстают от кисти; более длинный след создаётся только при атаке.

## Реализованная movement-only Spark Magic

У Spark Magic намеренно нет `spark_bolt.es`: эта школа не является атакующим projectile-spell. Сервер на каждом
завершённом player tick сравнивает текущую позицию с предыдущей. Если movement trail включён и смещение больше `0.02`
блока, у ног создаётся одна `echo_spark_magic:spark_circle` размером `0.16`, которая исчезает за `7` ticks.

Texture имеет ярко-жёлтый центр около `#FFFA04` и тёмно-жёлтые/охристые края около `#4F3601`. Размер, alpha-map,
кольцо и glow falloff совпадают с исходным `void_circle.png`.

Важно: это Java/Forge movement capability, а не секция текущего `SpellResource`. Поэтому параметры `Projectile`,
`Hit`, damage type и cast key здесь отсутствуют.

## Particle, texture и damage type: что нельзя объявить только в DSL

Magic `.es` ссылается на ID, но не создаёт registry entries.

Void particle состоит из:

```text
Java registration:
void-magic-forge-1.20.1/.../VoidMagicParticles.java

Particle JSON:
assets/echo_void_magic/particles/void_circle.json

Texture:
assets/echo_void_magic/textures/particle/void_circle.png

DSL reference:
echo_void_magic:void_circle
```

Void damage состоит из:

```text
data/echo_void_magic/damage_type/void_magic.json
data/forge/tags/damage_type/is_magic.json
DSL reference: echo_void_magic:void_magic
```

Fire particle состоит из:

```text
Java registration и движение вверх:
fire-magic-forge-1.20.1/.../FireMagicParticles.java
fire-magic-forge-1.20.1/.../FireMagicClientBootstrap.java

Particle JSON:
assets/echo_fire_magic/particles/fire_flame.json

Texture:
assets/echo_fire_magic/textures/particle/fire_flame.png

DSL reference:
echo_fire_magic:fire_flame
```

Fire damage состоит из:

```text
data/echo_fire_magic/damage_type/fire_magic.json
data/forge/tags/damage_type/is_magic.json
data/minecraft/tags/damage_type/is_fire.json
DSL reference: echo_fire_magic:fire_magic
```

Water particle и damage состоят из:

```text
assets/echo_water_magic/particles/water_circle.json
assets/echo_water_magic/textures/particle/water_circle.png
data/echo_water_magic/damage_type/water_magic.json
data/forge/tags/damage_type/is_magic.json
DSL references:
echo_water_magic:water_circle
echo_water_magic:water_magic
```

Air particle и damage состоят из:

```text
assets/echo_air_magic/particles/air_circle.json
assets/echo_air_magic/textures/particle/air_circle.png
data/echo_air_magic/damage_type/air_magic.json
data/forge/tags/damage_type/is_magic.json
DSL references:
echo_air_magic:air_circle
echo_air_magic:air_magic
```

Blood particle и damage состоят из:

```text
assets/echo_blood_magic/particles/blood_circle.json
assets/echo_blood_magic/textures/particle/blood_circle.png
data/echo_blood_magic/damage_type/blood_magic.json
data/forge/tags/damage_type/is_magic.json
DSL references:
echo_blood_magic:blood_circle
echo_blood_magic:blood_magic
```

Spark movement particle состоит из:

```text
Java movement runtime:
spark-magic-forge-1.20.1/.../SparkMagicMovementTrail.java
spark-magic-forge-1.20.1/.../SparkMagicPlayerState.java

Particle JSON:
assets/echo_spark_magic/particles/spark_circle.json

Texture:
assets/echo_spark_magic/textures/particle/spark_circle.png

Particle ID:
echo_spark_magic:spark_circle
```

Если создаётся только ещё одно зелёное projectile-spell, можно переиспользовать Void IDs без Java. Если нужна новая
texture, отдельный particle style или новый damage type, их надо добавить resource/registration слоем content-мода.

## Команды Echo Magic

### Список spell

```text
/echo_magic spells
```

Показывает загруженные `SpellId` и display name.

### Выдать spell

```text
/echo_magic grant <player> <spell>
/echo_magic grant @s echo_void_magic:void_daggers
```

Spell должен присутствовать в текущем catalog. Known state сохраняется.

### Удалить spell

```text
/echo_magic remove <player> <spell>
/echo_magic remove @s echo_void_magic:void_daggers
```

Если удалён selected spell, selection очищается.

### Выбрать spell

```text
/echo_magic select <player> <spell>
/echo_magic select @s echo_void_magic:void_daggers
```

Можно выбрать только загруженный и уже известный игроку spell.

### Применить

Selected spell:

```text
/echo_magic cast @s
```

Явный spell:

```text
/echo_magic cast @s echo_void_magic:void_daggers
```

Явный spell тоже должен быть известен игроку. Принятый cast запускает cooldown и burst.

### Статус игрока

```text
/echo_magic status @s
/echo_magic debug player @s
```

Показывает selected, количество known и cooldown выбранного spell.

### Runtime status

```text
/echo_magic debug status
```

Показывает active casts, projectiles и число diagnostics.

### Projectile debug placeholder

```text
/echo_magic debug projectiles on
/echo_magic debug projectiles off
```

Текущее ограничение: эти команды только печатают сообщение и ещё не переключают world visualization. Не используй их как
доказательство включённого hitbox renderer.

### Быстрый Void setup

```text
/echo_void_magic setup @s
```

Команда через public `MagicService`:

1. выдаёт `echo_void_magic:void_daggers`;
2. выбирает его;
3. очищает cooldown;
4. напоминает про назначенное действие управления.

Она не обходит known/selected state скрытым client hack.

## F3 overlay

При открытом vanilla debug screen `F3` Echo Magic рисует компактный блок:

```text
Echo Magic
selected: ...
known: ...
cooldown: ...
particles: ...
last rejection: ...
```

Это клиентский snapshot. Server остаётся authority для cast и damage.

## Magic.* в EchoScript

Adapter распознаёт ровно шесть вызовов:

```csharp
Magic.GrantToPlayer("namespace:spell");
Magic.RemoveFromPlayer("namespace:spell");
Magic.SelectForPlayer("namespace:spell");
Magic.CastAsPlayer("namespace:spell");
Magic.HasSpellOnPlayer("namespace:spell");
Magic.SelectedSpellIs("namespace:spell");
```

Аргумент сейчас обязан быть literal string. Поле или local variable binder не принимает:

```csharp
private SpellId Spell = "jdh:example";

private void Start()
{
    Magic.GrantToPlayer(Spell); // Для MagicCommandBinder сейчас неправильно.
}
```

Нужно:

```csharp
private void Start()
{
    Magic.GrantToPlayer("jdh:example");
}
```

Все вызовы требуют `MagicScriptExecutionContext.currentPlayer()`. Engine не выбирает ближайшего или случайного игрока.

Семантика:

| Вызов | Результат |
| --- | --- |
| `GrantToPlayer` | Добавляет spell в known set, сохраняет state |
| `RemoveFromPlayer` | Удаляет из known и очищает selection, если нужно |
| `SelectForPlayer` | Выбирает только known и loaded spell |
| `CastAsPlayer` | Возвращает принят/отклонён; не ждёт полёта projectile |
| `HasSpellOnPlayer` | Boolean predicate по known set |
| `SelectedSpellIs` | Boolean predicate по server selection |

### Текущий статус интеграции

Эти calls проходят отдельные unit tests binder/executor, но поиск по текущим модулям показывает их использование только в
`echo-magic`. Story Engine и Dialogue не создают `MagicScriptExecutionContext` и не передают вызовы в
`MagicScriptExecutor`.

Следовательно:

```text
В SpellResource Magic.* писать не нужно и нельзя считать частью definition.
В StoryResource/DialogueResource синтаксис можно распарсить только после отдельной host integration.
Для использования сейчас: `/echo_magic`, `/echo_void_magic setup` и назначенная клавиша применения.
```

Когда bridge будет подключён, ожидаемый сценарий будет выглядеть так:

```csharp
private void UnlockVoidMagic()
{
    Magic.GrantToPlayer("echo_void_magic:void_daggers");
    Magic.SelectForPlayer("echo_void_magic:void_daggers");
}
```

Но до появления bridge это пример будущего host usage, а не инструкция для текущего Story runtime.

## Hot reload и last-known-good

После изменения внешнего `.es` engine автоматически:

1. находит все datapack и external `.es`;
2. запускает EchoScript parser;
3. binder строит immutable `SpellDefinition`;
4. validator проверяет значения и server limits;
5. собирает единый candidate catalog;
6. публикует candidate только если нет error diagnostics.

Если хотя бы один spell broken или найден duplicate `SpellId`, новый candidate отклоняется. Предыдущий рабочий catalog
остаётся активным.

Active cast хранит pinned definition revision. Изменение `.es` через reload не должно молча менять уже летящие projectile.
Новый cast использует новую опубликованную definition.

## Server config

Файл создаётся Forge в:

```text
<gameDir>/config/echo-magic-server.toml
```

Defaults:

```toml
maximum_known_spells_per_player = 128
maximum_active_casts_per_player = 8
maximum_active_projectiles_per_player = 64
maximum_projectile_range = 128.0
maximum_projectile_speed = 8.0
maximum_burst_count = 32
maximum_damage_per_hit = 100.0
```

Это safety limits, а не параметры конкретного spell. Значения spell всё равно задаются в `.es`, но не могут превышать
server maximum.

## Client config

Файл:

```text
<gameDir>/config/echo-magic-client.toml
```

Объявленные defaults:

```toml
maximum_magic_particles_per_frame = 512
maximum_magic_particles_active = 4096
particle_quality = "HIGH"
use_vanilla_colored_particles = false
echo_particle_scale = 0.7
vanilla_particle_scale = 3.0
show_cast_rejection_message = true
```

Фактически сейчас используются:

- `maximum_magic_particles_per_frame`;
- `maximum_magic_particles_active`;
- `use_vanilla_colored_particles` — заменяет trail, core и impact всех заклинаний на `minecraft:dust`, сохраняя
  заданные школами `vanillaColor`;
- `echo_particle_scale` — общий размер собственных Echo billboard-частиц; default `0.7`;
- `vanilla_particle_scale` — размер ванильной цветной пыли; default `3.0`;
- `show_cast_rejection_message`.

Эту замену и оба масштаба удобнее менять без ручной правки TOML: `Моды → Echo Magic → Настройки`. Размеры
применяются к новым частицам сразу, перезапуск мира не требуется. Масштаб ванильных simple particle вроде
`minecraft:flame` Minecraft задаёт сама; `vanilla_particle_scale` относится к цветной `minecraft:dust`.

Пока не влияет на runtime:

- `particle_quality` — отдельные quality presets ещё не реализованы.

Particle budget режет только cosmetic trail. Core имеет приоритет, а server collision/damage от бюджета не зависят.

## Тестовая арена

Production JAR не содержит testmod. Для локальной dev-проверки можно дополнительно установить:

```text
D:\JDHSeason\mods\echo-magic\magic-testmod-1.20.1\build\libs\echo-magic-testmod-0.1.1.jar
D:\JDHSeason\mods\echo-void-magic\void-magic-testmod-1.20.1\build\libs\echo-void-magic-testmod-0.1.1.jar
D:\JDHSeason\mods\echo-fire-magic\fire-magic-testmod-1.20.1\build\libs\echo-fire-magic-testmod-0.1.0.jar
```

Не клади `*-sources.jar` в `mods`.

После запуска в тестовом мире встань так, чтобы впереди было свободное место, и выполни:

```text
/echo_magic_test arena
/echo_void_magic setup @s
/echo_void_magic_test acceptance
```

Arena создаёт:

```text
ally armor stand: 4 блока
обычная target: 8 блоков
каменная стена: 10 блоков
target за стеной: 12 блоков
```

Caster и первая armor stand добавляются в одну vanilla team. При `FriendlyFire(false)` projectile должен игнорировать ally,
попасть в target на 8 блоках и не пройти сквозь стену к target на 12 блоках.

Testmod предназначен только для разработки. В release instance оставляй два production JAR без testmod.

## Типичные ошибки

### `/echo_magic` — unknown command

Причины:

1. `echo-magic-0.1.1-all.jar` не установлен.
2. Игра не была полностью перезапущена после копирования JAR.
3. Установлен не `-all.jar`.
4. Forge отказался загрузить мод — смотри `logs/latest.log`.

### `/echo_void_magic` — unknown command

Проверь `echo-void-magic-0.1.1.jar` и обязательный `echo_magic` dependency.

### `/echo_magic spells` пуст

Проверь:

- загружен ли Void JAR;
- правильный ли external path;
- расширение `.es`;
- нет ли broken spell или duplicate ID;
- diagnostics/log после сообщения hot reload.

### `setup` пишет `Void Daggers was not loaded`

Void `.es` не вошёл в опубликованный catalog. Чаще всего candidate отклонён из-за ошибки или duplicate ID.

### Назначенная клавиша применения ничего не делает

Проверь:

```text
/echo_magic status @s
F3 overlay: selected, cooldown, last rejection
Controls: какая клавиша назначена и нет ли конфликта
```

Также cast блокируется для spectator, мёртвого игрока, неизвестного spell или активного cooldown.

### Projectile есть, но damage нет

Возможные причины:

- target является ally при `FriendlyFire(false)`;
- target invulnerable;
- `Damage(type: ...)` ссылается на отсутствующий damage type;
- projectile первым встретил block;
- target не `LivingEntity`.

### Нет trail/core

Проверь:

- установлен ли content-мод, который регистрирует particle ID;
- правильный ли `particle` ID;
- существует ли particle JSON и PNG;
- не достигнут ли client particle budget.

Отсутствие particle не означает отсутствие server projectile.

### Правка `.es` не применилась

Проверь:

1. Появилось ли сообщение об успешном hot reload после сохранения.
2. Редактируешь ли live-файл внутри `story/content-pack/jdh/data/jdh/echo_magic/spells`.
3. Нет ли duplicate ID.
4. Не был ли candidate отклонён с сохранением last-known-good.

### `Magic.GrantToPlayer(...)` ничего не делает в Story/Dialogue

Это ожидаемо в текущей сборке: host bridge ещё не подключён. Используй серверную команду или реализуй отдельный adapter,
который создаёт `MagicScriptExecutionContext` и вызывает `MagicScriptExecutor`.

## Быстрый workflow автора нового spell

Если достаточно существующих Void particle и damage type:

1. Установи production Echo Magic и Echo Void Magic JAR.
2. Создай каталог:

```text
...\story\content-pack\jdh\data\jdh\echo_magic\spells\
```

3. Скопируй минимальный template в новый `.es`.
4. Поставь уникальный `Id`, например `jdh:void_bolt`.
5. Оставь `particle: "echo_void_magic:void_circle"` и `type: "echo_void_magic:void_magic"`.
6. Сохрани файл и подожди до `0,75` секунды.
7. Проверь `/echo_magic spells`.
8. Выполни grant/select/cast.
9. Проверь ally, обычную target и стену.
10. Только после этого используй spell ID в сюжетной логике — после подключения Magic host bridge.

## Сборка после изменения исходников

Echo Magic:

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-magic'
.\gradlew.bat check build --console=plain
```

Echo Void Magic:

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-void-magic'
.\gradlew.bat check build --console=plain
```

Echo Fire Magic:

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-fire-magic'
.\gradlew.bat check build --console=plain
```

Echo Water Magic:

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-water-magic'
.\gradlew.bat check build --console=plain
```

Echo Air Magic:

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-air-magic'
.\gradlew.bat check build --console=plain
```

Echo Blood Magic:

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-blood-magic'
.\gradlew.bat check build --console=plain
```

Echo Spark Magic:

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-spark-magic'
.\gradlew.bat check build --console=plain
```

Production artifacts после успешной сборки:

```text
echo-magic/magic-forge-1.20.1/build/libs/echo-magic-0.7.0-all.jar
echo-void-magic/void-magic-forge-1.20.1/build/libs/echo-void-magic-0.2.2.jar
echo-fire-magic/fire-magic-forge-1.20.1/build/libs/echo-fire-magic-0.3.1.jar
echo-water-magic/water-magic-forge-1.20.1/build/libs/echo-water-magic-0.2.3.jar
echo-air-magic/air-magic-forge-1.20.1/build/libs/echo-air-magic-0.2.3.jar
echo-blood-magic/blood-magic-forge-1.20.1/build/libs/echo-blood-magic-0.3.0.jar
echo-magic-epicfight-adapter/build/libs/echo-magic-epicfight-adapter-0.1.1.jar
echo-spark-magic/spark-magic-forge-1.20.1/build/libs/echo-spark-magic-0.1.0.jar
```

Проверки включают parser/binder фактического `void_daggers.es`, unit tests, source-shape и production JAR contents.

## Итоговая памятка

```text
Magic DSL пишется в .es, не в .dsl.
Профиль spell-файла: class ... : SpellResource.
Описание находится в прямых вызовах Init().
SpellResource уже работает через loader и автоматический hot reload внешнего content pack; ручной `/reload` остаётся запасным способом.
Для текущего запуска: установить engine и нужные content JAR; затем `/echo_void_magic setup @s` или
`/echo_fire_magic setup @s`, `/echo_water_magic setup @s`, `/echo_air_magic setup @s` или
`/echo_blood_magic setup @s`, после чего выбрать школу через `N` и рисовать технику через `V` + ЛКМ.
Spark Magic включается отдельно через `/echo_spark_magic setup @s` и работает только при движении; клавиша cast для
неё не используется.
Для нового spell без Java: внешний .../echo_magic/spells/<name>.es и существующие particle/damage IDs.
Magic.* для Story/Dialogue подготовлен, но host bridge ещё не подключён.
Pierce, PASS collision, OFF_HAND swing и quality presets пока не считать реализованными.
Server всегда авторитетен для cast, collision и damage.
```

## Техники, веер, составные жесты и эффекты попадания

Каждый spell явно относится к жесту:

```csharp
School("echo_fire_magic:fire");
Technique(ARC); // LINE, ARC, CIRCLE или HOOK
```

Для дуги можно выпустить веер. `spread` распределяет весь `Burst` слева направо по указанному углу:

```csharp
Burst(
    count: 7,
    interval: Ticks(1),
    spread: Degrees(82)
);
```

`Hit` поддерживает независимые data-driven реакции. Их можно комбинировать без Java-кода конкретной школы:

```csharp
Hit(
    Damage(type: "my_mod:water_magic", amount: 1.0),
    Extinguish(true),
    Knockback(strength: 1.15, direction: FORWARD),
    DeflectProjectiles(false)
);
```

Для огня используется `Ignite(duration: Ticks(100))`. Для бокового сдвига вместо `FORWARD` доступно `LATERAL`. `DeflectProjectiles(true)` отклоняет стрелы и другие vanilla projectile рядом с летящей магической дугой.

Круговая область задаётся теми же секциями: `Technique(CIRCLE)`, `Origin(PLAYER_CENTER)`, `Direction(PLAYER_HORIZONTAL_LOOK)`, `interval: Ticks(0)` и веер до `Degrees(360)`. Нулевой interval выпускает кольцо в одном server tick в пределах общего emission budget.

Начиная с Echo Magic 0.4.1, поверх техники можно дорисовать отдельный модификатор без создания второго `.es`:

- удерживаемая точка после `LINE` или `ARC` откладывает исполнение на 2 секунды и сохраняет исходные позицию и направление;
- удерживаемая точка после `CIRCLE` ставит server-authoritative печать, которая активирует этот же spell при входе врага;
- развилка утраивает `Burst` в runtime, не меняя опубликованный `SpellDefinition` и исходный `.es`.

Cooldown и safety limits всё равно берутся из исходного DSL. Модифицированный burst ограничивается `maximum_burst_count`; печать занимает обычный active cast slot. Тайминги модификаторов пока являются правилами gesture engine, а не отдельными DSL-параметрами.

`Technique(HOOK)` распознаётся движком начиная с 0.4.2. Встроенная композиция `HOOK + точка` берёт существующий `LINE` spell школы как payload связи, резервирует cast/cooldown и выпускает его из точки привязки после удаления или достаточного отдаления цели. Это позволяет использовать связь без копирования всех line spell; отдельный `.es` с `Technique(HOOK)` зарезервирован для будущего самостоятельного контента крюка.

## Утилитарная техника POINT и «Рука кузнеца»

Echo Magic 0.5.0 принимает `Technique(POINT)`: это одиночный штрих, в котором ЛКМ удерживается не меньше `0.45` секунды, а прицел остаётся почти неподвижным. Если точка нарисована вторым штрихом после линии, дуги, круга или крюка, она по-прежнему считается модификатором, а не `POINT`.

Fire Magic 0.3.0 объявляет spell в обычном `.es`:

```csharp
Id("echo_fire_magic:blacksmith_hand");
School("echo_fire_magic:fire");
Technique(POINT);
Cooldown(Ticks(120));
```

Файл находится здесь:

```text
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_magic\spells\fire\blacksmith_hand.es
```

DSL выбирает школу, технику и cooldown. Сама работа с инвентарём и BlockState выполняется серверным адаптером Fire Magic через обработанное `MagicLifecycleEvent`; поэтому generic engine не выпускает фиктивный projectile. После cast адаптер ждёт 100 тиков, затем использует обычный `RecipeType.SMELTING` и переплавляет ровно одну единицу предмета в руке.

Для своих нагреваемых блоков добавь block ID в datapack-тег:

```text
data/echo_fire_magic/tags/blocks/blacksmith_heatable.json
```

Простейший блок должен иметь boolean BlockState property с именем `heated`: адаптер сам переключит её в `true`. Для другого поведения подпишись на `BlacksmithHeatBlockEvent` из package `io.github.kxyr0.echofiremagic.forge.api.event`, выполни собственное изменение и вызови `event.markHandled()`.
