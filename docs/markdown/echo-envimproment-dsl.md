# echo-envimproment: полный справочник DSL окружений

Документ описывает DSL, который реализован в модуле `echo-envimproment` сюжетного движка для Minecraft Forge 1.20.1.
Фокус только на этом модуле: `EnvironmentResource`, intrinsic-вызовы окружений, загрузка `.es` ресурсов и выполнение
`Environment.Set`. Полный общий Story DSL, Dialogue DSL, NPC DSL, Animation DSL и Sleep DSL здесь не дублируются.

Основано на исходниках:

- `D:\JDHSeason\mods\echo-story-engine\echo-envimproment`
- `D:\JDHSeason\mods\echo-story-engine\story-forge-1.20.1`
- `D:\JDHSeason\mods\echo-story-engine\story-echoscript-adapter`
- контент-пак `C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story`

## Карта Исходников

Главные Java-точки, которые задают поведение DSL:

| Файл | За что отвечает |
| --- | --- |
| `EchoScriptEnvironmentSourceCompiler.java` | общий pipeline: parse -> bind -> validate -> result |
| `EnvironmentResourceBinder.java` | проверка профиля `EnvironmentResource`, `Build()`, полей и helper-методов |
| `EnvironmentExpressionEvaluator.java` | вычисление полей, locals, `return`, helper-вызовов и intrinsic-вызовов |
| `EnvironmentIntrinsicFactory.java` | полный список environment intrinsic-ов и создание model-объектов |
| `EnvironmentIntrinsicReader.java` | проверка количества аргументов, запрет named args, чтение типов и id |
| `EnvironmentValue.java` | внутренние типы значений DSL |
| `EnvironmentResourceDef.java` | корневая compiled model окружения |
| `EnvironmentStateDef.java` | compiled model одного state |
| `SetBlockChangeDef.java` | compiled model операции установки блока |
| `ClearBlockChangeDef.java` | compiled model операции очистки блока |
| `BlockPropertyDef.java` | compiled model blockstate property |
| `EnvironmentResourceValidator.java` | loader-neutral validation: states, duplicate state id, duplicate position |
| `ForgeEnvironmentResourceValidator.java` | Forge validation: registry block, property, property value, SNBT |
| `EnvironmentResourceCandidateCompiler.java` | сборка catalog candidate, cache, duplicate environment id |
| `EnvironmentCatalogState.java` | atomic публикация или отклонение environment catalog |
| `EnvironmentSetCommandSupport.java` | обработчик action type `echo_story_engine:environment_set` |
| `EnvironmentService.java` | поиск environment/state/dimension и планирование операций |
| `ForgeEnvironmentBlockAccess.java` | фактическая запись блоков в `ServerLevel` |
| `ForgeStoryResourceReloadListener.java` | discovery `.es` окружений под `echo_story/environments` |
| `ForgeCommandSinkWiring.java` | перехват `Environment.Set` перед обычным extension dispatch |

## Что Делает Модуль

`echo-envimproment` добавляет слой окружений для сюжетного движка:

1. Компилирует EchoScript `.es` файлы профиля `EnvironmentResource`.
2. Собирает каталог окружений по `environmentId`.
3. Проверяет окружения на уровне DSL и Forge registry.
4. Обрабатывает story action `echo_story_engine:environment_set`.
5. Применяет выбранное состояние окружения к миру: ставит блоки, очищает позиции воздухом, применяет blockstate properties и опциональный block entity NBT.

Окружение в этом модуле - это не регион, не структура и не entity-сцена. Это набор именованных состояний, где каждое
состояние содержит список операций над блоками.

## Где Лежать Файлам

Forge loader ищет окружения здесь:

```text
data/<namespace>/echo_story/environments/**/*.es
```

Пример из контент-пака:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_story\environments\chapter_1\home\kitchen_table.environment.es
```

Внутри datapack это соответствует resource id примерно такого вида:

```text
jdh:echo_story/environments/chapter_1/home/kitchen_table.environment.es
```

Технически loader принимает любой файл с расширением `.es` под `echo_story/environments`. Суффикс
`.environment.es` - понятная конвенция для людей, но не отдельное требование loader-а.

Ограничение общего reload discovery: в одной папке не должно быть больше `7` `.es` файлов. Если файлов больше,
каталог получит diagnostic folder-limit через общий Forge diagnostic слой.

## Минимальный Файл

```csharp
namespace JDH.Chapter1.Home;

class KitchenTableEnvironment : EnvironmentResource
{
    private DimensionId World = "minecraft:overworld";
    private Position Plate = Position(49, -2, 90);
    private EnvironmentStateDef[] states = [Night(), Breakfast()];

    public override EnvironmentResourceDef Build()
    {
        return Environment("jdh:chapter_1/home/kitchen_table", World, states);
    }

    private EnvironmentStateDef Night()
    {
        return State("night", [
            ClearBlock(Plate)
        ]);
    }

    private EnvironmentStateDef Breakfast()
    {
        return State("breakfast", [
            SetBlock(Plate, "displaydelight:bacon_and_eggs", Properties(Property("facing", "south")))
        ]);
    }
}
```

## Базовый EchoScript Для Environment DSL

`echo-envimproment` не исполняет Java/C# код. EchoScript парсится в синтаксическое дерево, а модуль вручную
интерпретирует только разрешённые конструкции.

Разрешённый базовый синтаксис, который реально полезен для Environment DSL:

```csharp
namespace Optional.Name;

class ClassName : EnvironmentResource
{
    private SomeType FieldName = Expression;

    public override EnvironmentResourceDef Build()
    {
        var local = Expression;
        return Expression;
    }

    private SomeReturnType Helper()
    {
        var local = Expression;
        return Expression;
    }
}
```

Поддерживаются:

- `namespace ...;` - опционален, для DSL окружений сейчас не участвует в формировании id.
- `class ... : EnvironmentResource` - первый top-level class должен быть именно этим профилем.
- поля класса с initializer-выражением.
- методы без параметров.
- `var name = expression;` внутри методов.
- `return expression;`.
- строковые литералы `"text"`.
- целые числа, включая отрицательные: `10`, `64`, `-5`.
- массивы: `[A(), B(), C()]`.
- вызовы intrinsic-ов: `Environment(...)`, `State(...)`, `Position(...)`, `SetBlock(...)`, `ClearBlock(...)`, `Properties(...)`, `Property(...)`.
- вызовы helper-методов: `Night()`, `FacingSouth()`.
- скобки вокруг выражения: `(Position(1, 2, 3))`.
- комментарии EchoScript: `// line comment` и `/* block comment */`.

Строковые escape-последовательности:

- `\"`
- `\\`
- `\n`
- `\r`
- `\t`

Не поддерживается семантикой `echo-envimproment`:

- `if/else` внутри `EnvironmentResource` методов.
- expression statements вроде `SetBlock(...);` без `return` или без массива.
- `return;` без значения.
- присваивания после объявления: `x = ...;`.
- арифметика: `x + 1`, `64 - 2`.
- сравнения и логические операторы.
- `for`, `while`, `switch`, `try`, `throw`.
- параметры методов.
- runtime-вызовы Minecraft/Java.
- `new SomeType(...)`.
- boolean и decimal значения как Environment-значения.
- member access как значение, например `Some.Value`, кроме случая, когда это имя intrinsic-цели. Внутри Environment DSL intrinsic-и сейчас без namespace/member form, поэтому использовать `Environment.Position(...)` нельзя.

## Профиль EnvironmentResource

Первый class в файле должен наследоваться от `EnvironmentResource`:

```csharp
class KitchenTableEnvironment : EnvironmentResource
{
}
```

Если base type другой, компиляция окружения падает с `ESE-ENV-SOURCE-010`.

Практическое правило: один файл - один `EnvironmentResource`. Актуальный parser EchoScript может распарсить несколько
top-level классов, но binder окружений берёт `unit.classDeclaration()`, то есть первый class. Дополнительные классы не
являются частью контракта Environment DSL.

## Build Метод

Обязательный метод:

```csharp
public override EnvironmentResourceDef Build()
{
    return Environment("namespace:path", "minecraft:overworld", states);
}
```

Требования:

- имя метода: `Build`.
- модификаторы содержат `public` и `override`.
- возвращаемый тип: `EnvironmentResourceDef`.
- без параметров.
- тело должно вернуть значение типа `Environment(...)`.

Внутри `Build()` можно объявлять `var` перед `return`:

```csharp
public override EnvironmentResourceDef Build()
{
    var allStates = [Night(), Morning(), Evening()];
    return Environment("jdh:house/table", "minecraft:overworld", allStates);
}
```

Ошибки:

- нет `Build` - `ESE-ENV-SOURCE-011`.
- `Build` есть, но не возвращает `Environment(...)` - `ESE-ENV-SOURCE-012`.
- неправильная сигнатура `Build` - `ESE-ENV-SOURCE-013`.

## Поля

Поля используются как константы/переменные для сборки окружения:

```csharp
private DimensionId World = "minecraft:overworld";
private Position SashaPlatePosition = Position(49, -2, 90);
private EnvironmentStateDef[] states = [Night(), MorningBreakfast(), AfterBreakfast()];
```

Важно:

- declared type поля почти не проверяется evaluator-ом. `DimensionId`, `Position`, `EnvironmentStateDef[]` работают как читаемая документация для автора.
- фактический тип определяется initializer-выражением.
- поля вычисляются сверху вниз.
- поле может ссылаться только на уже вычисленное выше поле.
- local `var` внутри метода может затенить поле с тем же именем.
- повторяющиеся имена полей лучше не использовать: текущая внутренняя карта перезапишет старое значение более поздним.

Хороший стиль:

```csharp
private DimensionId World = "minecraft:overworld";
private Position TableCenter = Position(49, -2, 90);
private BlockPropertyDef[] FacingSouth = [Property("facing", "south")];
```

Плохой стиль:

```csharp
private Position Plate = NextPlate; // NextPlate ещё не вычислен.
private Position NextPlate = Position(49, -2, 90);
```

## Helper Методы

Любой helper должен быть `private`, без параметров:

```csharp
private EnvironmentStateDef MorningBreakfast()
{
    var changes = [
        SetBlock(Plate, "displaydelight:bacon_and_eggs", FacingSouth())
    ];
    return State("morning_breakfast", changes);
}
```

Возвращаемый тип helper-а читается человеком, но evaluator ориентируется на фактически возвращённое значение:

- `EnvironmentStateDef` - обычно возвращает `State(...)`.
- `EnvironmentChangeDef` - обычно возвращает `SetBlock(...)` или `ClearBlock(...)`.
- `BlockPropertyDef[]` - обычно возвращает `[Property(...)]`.
- `PropertiesDef` как отдельный тип не нужен, но `Properties(...)` возвращает специальное значение properties.

Методы могут быть объявлены ниже места вызова:

```csharp
private EnvironmentStateDef Morning()
{
    return State("morning", [BreakfastPlate()]);
}

private EnvironmentChangeDef BreakfastPlate()
{
    return SetBlock(Plate, "displaydelight:bacon_and_eggs");
}
```

Не поддерживается:

```csharp
private EnvironmentStateDef Morning(string id) // параметры запрещены
{
    return State(id, []);
}
```

Рекурсия запрещена:

```csharp
private EnvironmentStateDef A()
{
    return B();
}

private EnvironmentStateDef B()
{
    return A(); // ESE-ENV-SOURCE-024
}
```

## Scope И Вычисление

Evaluator работает так:

1. Индексирует поля и методы первого `EnvironmentResource` class.
2. Проверяет сигнатуры методов.
3. Вычисляет поля в порядке объявления.
4. Вызывает `Build()`.
5. Внутри каждого метода создаёт локальную таблицу символов из текущих доступных значений.
6. `var` добавляет или перезаписывает имя в локальной таблице.
7. `return` завершает метод и отдаёт значение.

Пример нормального scope:

```csharp
private Position Plate = Position(49, -2, 90);

private EnvironmentChangeDef Food()
{
    var block = "displaydelight:bacon_and_eggs";
    return SetBlock(Plate, block);
}
```

Неизвестное имя:

```csharp
return SetBlock(UnknownPlate, "minecraft:stone");
```

даст `ESE-ENV-SOURCE-022`.

Метод без `return`:

```csharp
private EnvironmentStateDef Broken()
{
    var changes = [];
}
```

даст `ESE-ENV-SOURCE-019`.

Неподдерживаемый statement:

```csharp
private EnvironmentStateDef Broken()
{
    if (true) {
        return State("x", []);
    }
    return State("y", []);
}
```

даст `ESE-ENV-SOURCE-020`, потому что Environment evaluator не исполняет `if`.

## Значения Environment DSL

Внутренние значения, которые понимает `echo-envimproment`:

| Значение | Как получить | Где используется |
| --- | --- | --- |
| `Text` | `"minecraft:overworld"` | id, dimension, blockId, stateId, property name/value, NBT |
| `Number` | `49`, `-2`, `90` | координаты `Position` |
| `Array` | `[Night(), Morning()]` | списки states, changes, properties |
| `Position` | `Position(x, y, z)` | `SetBlock`, `ClearBlock` |
| `Property` | `Property("facing", "south")` | `Properties`, property arrays |
| `Properties` | `Properties(Property(...))` | третий аргумент `SetBlock` |
| `Change` | `SetBlock(...)`, `ClearBlock(...)` | массив changes в `State` |
| `State` | `State("id", changes)` | массив states в `Environment` |
| `Resource` | `Environment(...)` | return из `Build` |
| `Invalid` | результат ошибки | не должен попадать в валидный ресурс |

## Intrinsic: Environment

Сигнатура:

```csharp
Environment(environmentId, dimension, states)
```

Аргументы строго позиционные. Named arguments не принимаются.

| Позиция | Имя | Тип | Пример |
| --- | --- | --- | --- |
| `0` | `environmentId` | string namespaced id | `"jdh:chapter_1/home/kitchen_table"` |
| `1` | `dimension` | string namespaced id | `"minecraft:overworld"` |
| `2` | `states` | array of `EnvironmentStateDef` | `[Night(), Morning()]` |

Пример:

```csharp
return Environment("jdh:chapter_1/home/kitchen_table", World, states);
```

`environmentId` и `dimension` проходят через `NamespacedId.of`.

Формат `NamespacedId`:

```text
namespace:path
```

Требования:

- `namespace`: `[a-z][a-z0-9_.-]*`
- `path`: `[a-z0-9_./-]+`
- обязательно есть `:`
- нельзя использовать uppercase в id.

Правильно:

```text
jdh:chapter_1/home/kitchen_table
minecraft:overworld
displaydelight:bacon_and_eggs
```

Неправильно:

```text
JDH:Home/Table
jdh
jdh:
:home
jdh:home table
```

Ошибки:

- неправильный id - `ESE-ENV-SOURCE-014`.
- не 3 аргумента - `ESE-ENV-SOURCE-015`.
- named argument - `ESE-ENV-SOURCE-016`.
- `states` не массив states - `ESE-ENV-SOURCE-018`.
- пустой список states - `ESE-ENV-SOURCE-001`.

## Intrinsic: State

Сигнатура:

```csharp
State(stateId, changes)
```

Аргументы строго позиционные. Named arguments не принимаются.

| Позиция | Имя | Тип | Пример |
| --- | --- | --- | --- |
| `0` | `stateId` | string, non-blank | `"night"` |
| `1` | `changes` | array of `EnvironmentChangeDef` | `[ClearBlock(Plate)]` |

Пример:

```csharp
private EnvironmentStateDef Night()
{
    var changes = [
        ClearBlock(SashaPlatePosition),
        ClearBlock(JohnPlatePosition),
        ClearBlock(DrinkPosition)
    ];
    return State("night", changes);
}
```

`stateId` не является `NamespacedId`. Он просто trim-ится и должен быть непустым. Практически лучше использовать
нижний регистр, `_`, `-` и понятные имена:

```text
night
morning_breakfast
after_breakfast
```

Состояние с пустым массивом changes допустимо:

```csharp
return State("noop", []);
```

Такое состояние ничего не меняет.

Ошибки:

- не 2 аргумента - `ESE-ENV-SOURCE-015`.
- named argument - `ESE-ENV-SOURCE-016`.
- `stateId` не string - `ESE-ENV-SOURCE-018`.
- `changes` не массив changes - `ESE-ENV-SOURCE-018`.
- duplicate state id внутри одного environment - `ESE-ENV-SOURCE-002`.

## Intrinsic: Position

Сигнатура:

```csharp
Position(x, y, z)
```

Аргументы строго позиционные. Named arguments не принимаются.

| Позиция | Имя | Тип | Пример |
| --- | --- | --- | --- |
| `0` | `x` | integer | `49` |
| `1` | `y` | integer | `-2` |
| `2` | `z` | integer | `90` |

Пример:

```csharp
private Position SashaPlatePosition = Position(49, -2, 90);
```

Поддерживаются отрицательные координаты:

```csharp
Position(-120, 64, 240)
```

Не поддерживаются decimal и арифметика:

```csharp
Position(49.5, -2, 90) // ошибка типа
Position(48 + 1, -2, 90) // EchoScript parser/evaluator не поддерживает оператор +
```

Ошибки:

- не 3 аргумента - `ESE-ENV-SOURCE-015`.
- named argument - `ESE-ENV-SOURCE-016`.
- координата не integer - `ESE-ENV-SOURCE-018`.

## Intrinsic: SetBlock

Сигнатуры:

```csharp
SetBlock(position, blockId)
SetBlock(position, blockId, properties)
SetBlock(position, blockId, properties, nbt)
```

Аргументы строго позиционные. Named arguments не принимаются.

| Позиция | Имя | Тип | Обязательно | Пример |
| --- | --- | --- | --- | --- |
| `0` | `position` | `Position` | да | `SashaPlatePosition` |
| `1` | `blockId` | string namespaced id | да | `"displaydelight:bacon_and_eggs"` |
| `2` | `properties` | `Properties` или array of `Property` | нет | `FacingSouth()` |
| `3` | `nbt` | string SNBT | нет | `"{Items:[]}"` |

Минимальный вариант:

```csharp
SetBlock(Plate, "minecraft:stone")
```

С blockstate property:

```csharp
SetBlock(Plate, "displaydelight:bacon_and_eggs", Properties(Property("facing", "south")))
```

С массивом properties:

```csharp
SetBlock(Plate, "displaydelight:bacon_and_eggs", [
    Property("facing", "south")
])
```

Через helper:

```csharp
private EnvironmentChangeDef SashaBreakfast()
{
    return SetBlock(SashaPlatePosition, "displaydelight:bacon_and_eggs", FacingSouth());
}

private BlockPropertyDef[] FacingSouth()
{
    return [Property("facing", "south")];
}
```

С NBT без properties нужно явно передать пустой третий аргумент:

```csharp
SetBlock(ChestPos, "minecraft:chest", [], "{Items:[]}")
```

или так:

```csharp
SetBlock(ChestPos, "minecraft:chest", Properties(), "{Items:[]}")
```

NBT строка trim-ится. Пустая NBT строка превращается в отсутствие NBT.

Forge-проверки для `SetBlock`:

- `blockId` должен существовать в `ForgeRegistries.BLOCKS`.
- каждый `Property(name, value)` должен существовать у блока.
- значение property должно приниматься property-типом блока.
- NBT должен быть валидным SNBT для `TagParser.parseTag`.
- если NBT указан для блока без BlockEntity, runtime application завершится ошибкой.

Ошибки source/bind:

- меньше 2 или больше 4 аргументов - `ESE-ENV-SOURCE-015`.
- named argument - `ESE-ENV-SOURCE-016`.
- `position` не `Position` - `ESE-ENV-SOURCE-018`.
- `blockId` не string или invalid namespaced id - `ESE-ENV-SOURCE-018` / `ESE-ENV-SOURCE-014`.
- `properties` не `Properties` и не array of `Property` - `ESE-ENV-SOURCE-018`.
- `nbt` не string - `ESE-ENV-SOURCE-018`.

Forge reload errors:

- unknown block - `ESE-FORGE-ENV-004`.
- unknown block property - `ESE-FORGE-ENV-005`.
- invalid property value - `ESE-FORGE-ENV-006`.
- invalid SNBT - `ESE-FORGE-ENV-007`.

## Intrinsic: ClearBlock

Сигнатура:

```csharp
ClearBlock(position)
```

Аргументы строго позиционные. Named arguments не принимаются.

| Позиция | Имя | Тип | Пример |
| --- | --- | --- | --- |
| `0` | `position` | `Position` | `Plate` |

Пример:

```csharp
ClearBlock(SashaPlatePosition)
```

Runtime заменяет позицию на `minecraft:air`.

Ошибки:

- не 1 аргумент - `ESE-ENV-SOURCE-015`.
- named argument - `ESE-ENV-SOURCE-016`.
- аргумент не `Position` - `ESE-ENV-SOURCE-018`.

## Intrinsic: Property

Сигнатура:

```csharp
Property(name, value)
```

Аргументы строго позиционные. Named arguments не принимаются.

| Позиция | Имя | Тип | Пример |
| --- | --- | --- | --- |
| `0` | `name` | string, non-blank | `"facing"` |
| `1` | `value` | string, non-blank | `"south"` |

Пример:

```csharp
Property("facing", "south")
```

`Property` сам по себе не является допустимым третьим аргументом `SetBlock`. Его нужно обернуть:

```csharp
SetBlock(Plate, "minecraft:oak_stairs", Properties(Property("facing", "north")))
```

или:

```csharp
SetBlock(Plate, "minecraft:oak_stairs", [Property("facing", "north")])
```

Ошибки:

- не 2 аргумента - `ESE-ENV-SOURCE-015`.
- named argument - `ESE-ENV-SOURCE-016`.
- `name` или `value` не string - `ESE-ENV-SOURCE-018`.
- пустой `name` или `value` приведёт к ошибке модели.

## Intrinsic: Properties

Сигнатуры:

```csharp
Properties()
Properties(Property(...))
Properties(Property(...), Property(...))
Properties([Property(...), Property(...)])
Properties(Property(...), [Property(...), Property(...)])
```

`Properties` собирает список `BlockPropertyDef`.

Примеры:

```csharp
Properties()
```

```csharp
Properties(Property("facing", "south"))
```

```csharp
Properties(
    Property("facing", "north"),
    Property("half", "top")
)
```

```csharp
Properties([
    Property("facing", "north"),
    Property("half", "top")
])
```

Техническая деталь текущей реализации: `Properties` не проверяет имена named arguments, но эти имена не используются.
Пиши позиционно:

```csharp
Properties(Property("facing", "south")) // правильно
```

Не закладывайся на такой стиль:

```csharp
Properties(main: Property("facing", "south")) // имя main игнорируется текущим кодом
```

Ошибки:

- любой элемент, который не является `Property` или массивом `Property`, даст `ESE-ENV-SOURCE-018`.

## Полный Живой Пример

Это форма, близкая к текущему контент-паку:

```csharp
namespace JDH.Chapter1.Home;

class KitchenTableEnvironment : EnvironmentResource
{
    private DimensionId World = "minecraft:overworld";
    private Position SashaPlatePosition = Position(49, -2, 90);
    private Position JohnPlatePosition = Position(48, -2, 90);
    private Position DrinkPosition = Position(47, -2, 90);
    private EnvironmentStateDef[] states = [Night(), MorningBreakfast(), AfterBreakfast()];

    public override EnvironmentResourceDef Build()
    {
        return Environment("jdh:chapter_1/home/kitchen_table", World, states);
    }

    private EnvironmentStateDef Night()
    {
        var changes = [
            ClearBlock(SashaPlatePosition),
            ClearBlock(JohnPlatePosition),
            ClearBlock(DrinkPosition)
        ];
        return State("night", changes);
    }

    private EnvironmentStateDef MorningBreakfast()
    {
        var changes = [
            SashaBreakfast(),
            SetBlock(JohnPlatePosition, "displaydelight:plated_bacon_sandwich", FacingSouth()),
            SetBlock(DrinkPosition, "displaydelight:apple_cider", FacingSouth())
        ];
        return State("morning_breakfast", changes);
    }

    private EnvironmentStateDef AfterBreakfast()
    {
        var changes = [
            ClearBlock(SashaPlatePosition),
            ClearBlock(JohnPlatePosition),
            ClearBlock(DrinkPosition)
        ];
        return State("after_breakfast", changes);
    }

    private EnvironmentChangeDef SashaBreakfast()
    {
        return SetBlock(SashaPlatePosition, "displaydelight:bacon_and_eggs", FacingSouth());
    }

    private BlockPropertyDef[] FacingSouth()
    {
        return [Property("facing", "south")];
    }
}
```

## Как Проектировать Состояния

Состояние применяет только те позиции, которые перечислены в `changes`.

Если state не очищает позицию, старый блок на этой позиции останется. Поэтому состояние лучше писать как полный снимок
управляемых позиций:

```csharp
private EnvironmentStateDef Night()
{
    return State("night", [
        ClearBlock(SashaPlatePosition),
        ClearBlock(JohnPlatePosition),
        ClearBlock(DrinkPosition)
    ]);
}

private EnvironmentStateDef MorningBreakfast()
{
    return State("morning_breakfast", [
        SetBlock(SashaPlatePosition, "displaydelight:bacon_and_eggs", FacingSouth()),
        SetBlock(JohnPlatePosition, "displaydelight:plated_bacon_sandwich", FacingSouth()),
        SetBlock(DrinkPosition, "displaydelight:apple_cider", FacingSouth())
    ]);
}

private EnvironmentStateDef AfterBreakfast()
{
    return State("after_breakfast", [
        ClearBlock(SashaPlatePosition),
        ClearBlock(JohnPlatePosition),
        ClearBlock(DrinkPosition)
    ]);
}
```

В одном `State` одна и та же позиция не может изменяться дважды:

```csharp
return State("broken", [
    SetBlock(Plate, "minecraft:stone"),
    ClearBlock(Plate)
]);
```

Это даст `ESE-ENV-SOURCE-003`.

Одинаковая позиция в разных состояниях разрешена и является нормальным способом переключения окружения.

## Вызов Из StoryResource

Сам EnvironmentResource описывает состояния. Переключение выполняется из story action.

В линейном `OnStart()`:

```csharp
private void OnStart()
{
    Environment.Set("jdh:chapter_1/home/kitchen_table", "morning_breakfast");
}
```

Эта форма компилируется в external action:

```text
actionType = echo_story_engine:environment_set
payload.environmentId = jdh:chapter_1/home/kitchen_table
payload.stateId = morning_breakfast
```

В явном `Nodes()` графе поддерживаются две формы.

Позиционная:

```csharp
Environment.Set(
    "set_kitchen_table_breakfast",
    "jdh:chapter_1/home/kitchen_table",
    "morning_breakfast",
    next: "next_node"
);
```

Named payload:

```csharp
Environment.Set(
    "set_kitchen_table_breakfast",
    environmentId: "jdh:chapter_1/home/kitchen_table",
    stateId: "morning_breakfast",
    next: "next_node"
);
```

Для `Nodes()`:

- первый positional argument - node id.
- `next` обязателен.
- `environmentId` и `stateId` можно дать либо позиционно, либо named.
- `output` у `Environment.Set` не поддержан.

Для `OnStart()`:

- ровно 2 positional arguments: `environmentId`, `stateId`.
- named arguments не принимаются.
- node id генерируется автоматически как `set_environment`, при повторениях - `set_environment_2` и далее.

Runtime handler находится в `echo-envimproment` и перехватывает action type:

```text
echo_story_engine:environment_set
```

Если action type другой, команда уходит в обычный extension dispatch.

## Runtime Поведение Environment.Set

При выполнении `Environment.Set(environmentId, stateId)`:

1. `environmentId` парсится как `NamespacedId`.
2. Environment ищется в текущем immutable catalog.
3. Внутри environment ищется state по `stateId`.
4. Берётся dimension из `Environment(...)`.
5. Проверяется, что dimension загружен на сервере.
6. Проверяется, что world adapter действительно соответствует dimension окружения.
7. Для каждого change строится операция:
   - `ClearBlock` -> `minecraft:air`.
   - `SetBlock` -> default block state + применённые properties + optional prepared NBT.
8. После успешного планирования операции последовательно применяются через `ServerLevel.setBlock`.
9. Story action завершается `SUCCESS`.

Если что-то падает, action завершается `FAILURE`, а diagnostic получает код `ESE-FORGE-ENV-101`.

Типичные runtime failures:

- `Environment id '...' is invalid.`
- `Environment '...' is not loaded.`
- `Environment '...' does not contain state '...'.`
- `Dimension '...' is not loaded.`
- `Environment dimension mismatch for '...'.`
- `Block '...' is not registered.`
- `Block '...' does not contain property '...'.`
- `Property '...' does not accept value '...'.`
- `Block entity NBT was provided for a block without BlockEntity.`
- `Placed block did not create the expected BlockEntity.`
- `Block entity NBT is invalid: ...`

Планирование неизвестного state/block/property/value происходит до изменения мира. Низкоуровневые ошибки во время
самого `setBlock` не откатывают уже применённые операции, поэтому для сложных NBT-состояний лучше держать изменения
небольшими и проверять их отдельно.

## Block Entity NBT

Четвёртый аргумент `SetBlock` - SNBT строка:

```csharp
SetBlock(ChestPos, "minecraft:chest", [], "{Items:[]}")
```

Правила:

- NBT передаётся только строкой.
- строка парсится через Minecraft `TagParser.parseTag`.
- пустая строка после trim считается отсутствием NBT.
- Forge reload проверяет синтаксис SNBT.
- Runtime дополнительно проверяет, что блок является `EntityBlock`.
- При загрузке NBT runtime принудительно выставляет координаты `x`, `y`, `z` в tag под позицию блока.
- После `blockEntity.load(tag)` вызывается `setChanged()` и `sendBlockUpdated(...)`.

Если нужен NBT без properties, третий аргумент всё равно обязателен:

```csharp
SetBlock(ChestPos, "minecraft:chest", Properties(), "{CustomName:'\"Kitchen\"'}")
```

## Reload И Каталог

При `/reload` или reload ресурсов:

1. Forge discovery читает `.es` под `echo_story/environments`.
2. Файлы сортируются по source id.
3. `EnvironmentResourceCandidateCompiler` компилирует каждый source.
4. Если source text не изменился, используется compile cache.
5. Для каждого успешно скомпилированного Environment выполняется Forge validation.
6. Environment кладётся в candidate map по `environment.id()`.
7. Duplicate environment id даёт ошибку.
8. Если error count равен `0`, candidate устанавливается как новый catalog revision.
9. Если есть хотя бы одна ошибка, весь candidate отклоняется, текущий предыдущий catalog остаётся активным.

Успешная установка даёт diagnostic:

```text
ESE-FORGE-ENV-001 Environment resource catalog installed.
```

Отклонение candidate:

```text
ESE-FORGE-ENV-002 Environment resource catalog candidate was rejected.
```

Duplicate id:

```text
ESE-FORGE-ENV-003 Duplicate Environment ID was found.
```

Каталог хранит:

- `revision`
- `installedAt`
- map `NamespacedId -> EnvironmentCatalogEntry`

Статусы reload:

- `NOT_LOADED`
- `ACCEPTED`
- `REJECTED`

## Все Source Diagnostics

| Код | Когда возникает |
| --- | --- |
| `ESE-ENV-SOURCE-001` | `Environment(...)` содержит пустой список states |
| `ESE-ENV-SOURCE-002` | два `State` в одном environment имеют одинаковый `stateId` |
| `ESE-ENV-SOURCE-003` | один `State` меняет одну и ту же позицию больше одного раза |
| `ESE-ENV-SOURCE-010` | первый class наследуется не от `EnvironmentResource` |
| `ESE-ENV-SOURCE-011` | отсутствует `Build()` |
| `ESE-ENV-SOURCE-012` | `Build()` не вернул `Environment(...)` |
| `ESE-ENV-SOURCE-013` | метод имеет неподдержанную сигнатуру |
| `ESE-ENV-SOURCE-014` | строка id не проходит `NamespacedId.of` |
| `ESE-ENV-SOURCE-015` | intrinsic получил неправильное число аргументов |
| `ESE-ENV-SOURCE-016` | intrinsic не принимает named argument |
| `ESE-ENV-SOURCE-017` | неизвестный intrinsic |
| `ESE-ENV-SOURCE-018` | аргумент имеет неправильный Environment-тип |
| `ESE-ENV-SOURCE-019` | метод не вернул значение |
| `ESE-ENV-SOURCE-020` | statement внутри Environment method не поддерживается |
| `ESE-ENV-SOURCE-021` | вызов неизвестного helper-метода или intrinsic |
| `ESE-ENV-SOURCE-022` | ссылка на неизвестное значение |
| `ESE-ENV-SOURCE-023` | expression не поддерживается Environment evaluator-ом |
| `ESE-ENV-SOURCE-024` | рекурсия helper-методов |

К этим кодам могут добавляться базовые EchoScript parser diagnostics `ESL-*`, например:

- invalid string escape.
- missing semicolon.
- missing closing brace/bracket/parenthesis.
- unsupported token.
- лимиты размера файла, количества токенов, вложенности, аргументов, элементов массива.

## Все Forge Environment Diagnostics

| Код | Severity | Когда возникает |
| --- | --- | --- |
| `ESE-FORGE-ENV-001` | INFO | environment catalog успешно установлен |
| `ESE-FORGE-ENV-002` | ERROR | candidate catalog отклонён из-за ошибок |
| `ESE-FORGE-ENV-003` | ERROR | duplicate `environmentId` в разных source |
| `ESE-FORGE-ENV-004` | ERROR | `SetBlock` ссылается на неизвестный block registry id |
| `ESE-FORGE-ENV-005` | ERROR | блок не содержит указанную property |
| `ESE-FORGE-ENV-006` | ERROR | property не принимает указанное значение |
| `ESE-FORGE-ENV-007` | ERROR | block entity NBT не парсится как SNBT |
| `ESE-FORGE-ENV-101` | ERROR | runtime `Environment.Set` не смог применить state |

## Таблица Всех Intrinsic-ов

| Intrinsic | Возвращает | Аргументы | Named args | Назначение |
| --- | --- | --- | --- | --- |
| `Environment(id, dimension, states)` | `EnvironmentResourceDef` | ровно 3 | нет | корневой ресурс |
| `State(id, changes)` | `EnvironmentStateDef` | ровно 2 | нет | состояние окружения |
| `Position(x, y, z)` | `Position` | ровно 3 | нет | координата блока |
| `SetBlock(position, blockId)` | `EnvironmentChangeDef` | 2 | нет | поставить блок default state |
| `SetBlock(position, blockId, properties)` | `EnvironmentChangeDef` | 3 | нет | поставить блок с properties |
| `SetBlock(position, blockId, properties, nbt)` | `EnvironmentChangeDef` | 4 | нет | поставить block entity с SNBT |
| `ClearBlock(position)` | `EnvironmentChangeDef` | ровно 1 | нет | поставить воздух |
| `Property(name, value)` | `BlockPropertyDef` | ровно 2 | нет | одна blockstate property |
| `Properties(...)` | properties list | 0..N | имена игнорируются текущим кодом | собрать properties |

## Частые Ошибки И Исправления

### Named arguments в EnvironmentResource

Плохо:

```csharp
return Environment(id: "jdh:table", dimension: World, states: states);
```

Правильно:

```csharp
return Environment("jdh:table", World, states);
```

### Один Property напрямую в SetBlock

Плохо:

```csharp
SetBlock(Plate, "minecraft:oak_stairs", Property("facing", "north"))
```

Правильно:

```csharp
SetBlock(Plate, "minecraft:oak_stairs", Properties(Property("facing", "north")))
```

или:

```csharp
SetBlock(Plate, "minecraft:oak_stairs", [Property("facing", "north")])
```

### Верхний Регистр В Id

Плохо:

```csharp
return Environment("JDH:Chapter1/Home/Table", World, states);
```

Правильно:

```csharp
return Environment("jdh:chapter_1/home/table", World, states);
```

### Две Операции На Одну Позицию В Одном State

Плохо:

```csharp
return State("breakfast", [
    ClearBlock(Plate),
    SetBlock(Plate, "displaydelight:bacon_and_eggs")
]);
```

Правильно:

```csharp
return State("breakfast", [
    SetBlock(Plate, "displaydelight:bacon_and_eggs")
]);
```

### State Не Очищает Старые Блоки

Плохо:

```csharp
private EnvironmentStateDef AfterBreakfast()
{
    return State("after_breakfast", []);
}
```

Если до этого breakfast поставил еду, она останется.

Правильно:

```csharp
private EnvironmentStateDef AfterBreakfast()
{
    return State("after_breakfast", [
        ClearBlock(SashaPlatePosition),
        ClearBlock(JohnPlatePosition),
        ClearBlock(DrinkPosition)
    ]);
}
```

### NBT Без Третьего Аргумента

Плохо:

```csharp
SetBlock(ChestPos, "minecraft:chest", "{Items:[]}")
```

Третий аргумент будет прочитан как `properties`, а не `nbt`.

Правильно:

```csharp
SetBlock(ChestPos, "minecraft:chest", [], "{Items:[]}")
```

## Authoring Checklist

Перед reload проверь:

- файл лежит под `data/<namespace>/echo_story/environments`.
- файл имеет расширение `.es`.
- первый class наследуется от `EnvironmentResource`.
- есть `public override EnvironmentResourceDef Build()`.
- `Build()` возвращает `Environment(id, dimension, states)`.
- `environmentId`, `dimension`, `blockId` написаны lowercase и имеют `namespace:path`.
- `states` не пустой.
- `stateId` уникальны внутри environment.
- каждый `State` содержит только один change на каждую позицию.
- каждый `SetBlock` использует существующий block id.
- properties реально существуют у блока.
- property values соответствуют значениям блока.
- если используется NBT, он валиден как SNBT.
- если state должен убрать старые предметы/декор, в нём есть `ClearBlock(...)`.

## Команда Проверки Модуля

Для JVM-тестов модуля:

```powershell
cd D:\JDHSeason\mods\echo-story-engine
.\gradlew.bat :echo-envimproment:test
```

Для проверки интеграции story-команды `Environment.Set` часть покрытия лежит в тестах `echo-envimproment` и
`story-echoscript-adapter`.
