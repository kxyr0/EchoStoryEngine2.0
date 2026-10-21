# Echo Visibility DSL

Документ описывает текущую реализацию Echo Visibility 0.1.0 для Forge 1.20.1: где создавать visibility-watch, как наблюдать за NPC или мировой областью, где разрешены `View.*`, как подписывать Echo Trigger на потерю видимости и как проверять всё в игре.

## Что делает Visibility

Echo Visibility отвечает на вопрос: видит ли конкретный игрок конкретную цель через свою фактическую активную камеру.

При проверке учитываются:

- текущая render camera, а не только поворот тела игрока;
- first person, third person и detached/cutscene camera;
- фактические view/projection matrices;
- динамический FOV;
- aspect ratio и размер framebuffer;
- полный AABB цели, а не одна точка;
- частичная видимость цели;
- при включённом режиме — препятствия между камерой и sample points;
- отдельный lifecycle наблюдения для каждого игрока.

Visibility не является проверкой боевой видимости или античитом. Клиент определяет восприятие, а сервер валидирует report и решает, можно ли запускать сюжетное действие.

## Где что пишется

Visibility использует три разных типа EchoScript-файлов. Они не взаимозаменяемы.

| Что описывается | Тип ресурса | Куда класть | Что в нём разрешено |
| --- | --- | --- | --- |
| Цель и правила наблюдения | `VisibilityWatchResource` | `<gameDirectory>/story/content-pack/<namespace>/echo_visibility/watches/**/*.es` | `Id`, `Target`, `Viewpoint`, `HiddenWhen`, `Samples`, hysteresis, concealment и другие настройки watch |
| Реакция на visibility event | `TriggerResource` | `<gameDirectory>/story/triggers/**/*.trigger.es` | `Subscribe(Player.LostSightOf(...))`, `Subscribe(Player.GainedSightOf(...))`, `Subscribe(Player.TargetIsOutOfView(...))`, `StartStory(...)` |
| Управление watch из сюжета | `StoryResource` | `data/<namespace>/echo_story/stories/**/*.es` внутри активного content pack | `View.Arm`, `View.Disarm`, waits, queries и `View.AssertStillOutOfView` внутри линейного `OnStart()` |

Короткое правило:

```text
VisibilityWatchResource.Init()
    описывает ЧТО и КАК наблюдать

TriggerResource.Init()
    описывает НА КАКОЕ visibility-событие подписаться

StoryResource.OnStart()
    описывает КОГДА включить/выключить watch и что делать дальше
```

Нельзя писать `View.Arm(...)` внутри `VisibilityWatchResource`. Нельзя писать `Target(Npc(...))` внутри Story. Нельзя писать `Player.LostSightOf(...)` как обычную команду Story.

## Точные пути в Prism «Форма Мысли»

Игровая директория instance:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft
```

Visibility watches:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\echo_visibility\watches\
```

Пример:

```text
...\story\content-pack\jdh\echo_visibility\watches\chapter_1\home\father_vanish.es
```

Trigger subscriptions:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\triggers\
```

Пример:

```text
...\story\triggers\chapter_1\father_vanish.trigger.es
```

Story resources в JDH content pack:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_story\stories\
```

Пример:

```text
...\data\jdh\echo_story\stories\chapter_1\father_vanish.story.es
```

Для watch подходит любое имя с окончанием `.es`. Для Trigger текущий loader требует окончание `.trigger.es`.

## Какие моды за что отвечают

| Мод | Когда нужен |
| --- | --- |
| `echo-visibility` | Всегда. Загружает watches, снимает активную камеру, вычисляет состояние и хранит arm lifecycle. |
| `echo-npc` | Для `Target(Npc(...))` и немедленного client-side concealment NPC. |
| `echo-trigger` | Для декларативных `Subscribe(Player.*SightOf(...))`. |
| `echo-trigger-visibility-adapter` | Связывает Visibility events с Trigger/Story и исполняет story actions `View.*`. |
| `echo-story-engine` | Для `StoryResource` и запуска story. |
| `echo-debug` + `echo-debug-visibility-adapter` | Необязательно для production; нужны для HUD и world-space отладки Visibility. |

## Общий порядок работы

1. Создать `VisibilityWatchResource` с постоянным `VisibilityWatchId`.
2. Выполнить `/echo_visibility reload`.
3. Заармить watch через Story `View.Arm(...)` или команду `/echo_visibility arm ...`.
4. Клиент игрока начнёт проверять цель через выбранную камеру.
5. При изменении стабильного состояния сервер примет visibility report.
6. Story может ждать состояние через `View.WaitUntil...`, либо Trigger может получить typed visibility event.
7. После завершения сцены выполнить `View.Disarm(...)`, если наблюдение больше не нужно.

Одновременно у одного игрока может быть не больше 32 активных watches.

## Минимальный NPC watch

Файл:

```text
story/content-pack/jdh/echo_visibility/watches/chapter_1/home/father_vanish.es
```

Код:

```csharp
namespace JDH.Chapter1.Home.Visibility;

class FatherVanishWatch : VisibilityWatchResource
{
    private VisibilityWatchId FatherVanish = "jdh:home/father_vanish";
    private NpcId Father = "jdh:father";

    private void Init()
    {
        Id(FatherVanish);
        Target(Npc(Father));
        Viewpoint(ACTIVE_CAMERA);
        HiddenWhen(OUTSIDE_FRUSTUM_OR_OCCLUDED);
        Samples(NPC_HEAD, NPC_CHEST, NPC_PELVIS, NPC_FEET, BOUNDS_CORNERS);
        Occlusion(VISUAL_MULTI_RAY);
        RequireVisibleFirst();
        LostAfter(RenderedFrames(1));
        SeenAfter(RenderedFrames(2));
        EdgeMargin(Pixels(2));
        Concealment(HIDE_NPC_IMMEDIATELY);
        Evaluate(EVERY_RENDER_FRAME);
    }
}
```

Этот watch:

- ищет конкретного NPC `jdh:father` через Echo NPC;
- использует реальную активную камеру;
- считает NPC скрытым, если весь его bounds вышел из frustum или все доступные sample rays закрыты;
- не публикует потерю зрения, пока игрок хотя бы один раз стабильно не увидел NPC;
- принимает потерю зрения после одного render frame;
- принимает возвращение в поле зрения после двух render frames;
- при `LOST_SIGHT` скрывает NPC на клиенте в том же кадре до решения сервера.

## Минимальный Box watch

Box watch наблюдает не сущность, а authored world-space AABB. Это подходит для комнаты, дверного проёма, части декора или зоны, которую разрешено изменить только пока игрок туда не смотрит.

```csharp
namespace JDH.Chapter1.Home.Visibility;

class KitchenEnvironmentWatch : VisibilityWatchResource
{
    private VisibilityWatchId Kitchen = "jdh:home/kitchen_change";

    private void Init()
    {
        Id(Kitchen);
        Target(Box(
            dimension: "minecraft:overworld",
            min: Position(-8.5, 60, -4),
            max: Position(8, 72, 12)
        ));
        Viewpoint(ACTIVE_CAMERA);
        HiddenWhen(OUTSIDE_FRUSTUM_OR_OCCLUDED);
        Samples(BOX_CENTER, BOX_CORNERS, BOX_FACE_CENTERS);
        Occlusion(VISUAL_MULTI_RAY);
        RequireVisibleFirst();
        LostAfter(RenderedFrames(1));
        SeenAfter(RenderedFrames(2));
        EdgeMargin(Pixels(2));
        Evaluate(EVERY_CLIENT_TICK);
    }
}
```

Box не может использовать `HIDE_NPC_IMMEDIATELY`. Visibility также не скрывает блоки и не меняет environment автоматически. Изменение мира выполняет Story после `View.AssertStillOutOfView(...)`.

## Форма VisibilityWatchResource

Watch — строго Init-only ресурс:

```csharp
namespace JDH.Visibility;

class ExampleWatch : VisibilityWatchResource
{
    private VisibilityWatchId Watch = "jdh:example/watch";

    private void Init()
    {
        Id(Watch);
        Target(...);
    }
}
```

Обязательные правила:

- класс должен наследоваться от `VisibilityWatchResource`;
- должен существовать ровно один `Init()`;
- другие методы и callbacks запрещены;
- внутри `Init()` разрешены только invocation statements;
- `Id(...)` и `Target(...)` обязательны;
- `Build()`, `Nodes()`, `return`, локальные переменные, `if`, циклы и ручные callbacks здесь не поддерживаются.

Поля используются как читаемые aliases для строковых ID:

```csharp
private VisibilityWatchId Watch = "jdh:home/father_vanish";
private NpcId Father = "jdh:father";
```

Текущий binder разрешает literal string или поле, напрямую инициализированное string literal. Он не вычисляет конкатенацию и динамические выражения.

Работает:

```csharp
Id("jdh:home/father_vanish");
Id(FatherVanish);
Target(Npc("jdh:father"));
Target(Npc(Father));
```

Не работает:

```csharp
Id("jdh:" + chapter + "/father");
Target(Npc(GetFatherId()));
```

## Справочник вызовов VisibilityWatchResource

| DSL | Обязателен | По умолчанию | Что делает |
| --- | --- | --- | --- |
| `Id(watch);` | Да | — | Задаёт постоянный `VisibilityWatchId`. |
| `Target(Npc(...));` или `Target(Box(...));` | Да | — | Задаёт одну наблюдаемую цель. |
| `Viewpoint(ACTIVE_CAMERA);` | Нет | `ACTIVE_CAMERA` | Выбирает источник камеры. |
| `HiddenWhen(OUTSIDE_FRUSTUM);` | Нет | `OUTSIDE_FRUSTUM` | Задаёт геометрический режим определения скрытия. |
| `Occlusion(NONE);` | Нет | `NONE` | Управляет visual raycasts. |
| `Samples(...);` | Нет | Зависят от типа target | Задаёт sample points для occlusion. |
| `RequireVisibleFirst();` | Нет | Включено | Не разрешает `LOST_SIGHT`, пока цель сначала не была видна. |
| `AllowInitiallyHidden();` | Нет | Выключено | Разрешает принять изначально скрытое состояние. |
| `LostAfter(RenderedFrames(n));` | Нет | `1` | Число подряд скрытых render frames для принятия hidden state. |
| `SeenAfter(RenderedFrames(n));` | Нет | `2` | Число подряд видимых render frames для принятия visible state. |
| `EdgeMargin(Pixels(n));` | Нет | `2` | Дополнительный допуск у границы экрана. |
| `Concealment(NONE);` | Нет | `NONE` | Политика немедленного client-side concealment. |
| `Evaluate(EVERY_RENDER_FRAME);` | Нет | `EVERY_RENDER_FRAME` | Частота вычисления на клиенте. |

Порядок вызовов внутри `Init()` для binder не является смысловой последовательностью, но для читаемости рекомендуется порядок из примеров: `Id`, `Target`, camera/detection, samples/occlusion, hysteresis, concealment, evaluation.

## ID

`VisibilityWatchId`, `NpcId` и dimension ID используют формат:

```text
namespace:path
```

Разрешены нижний регистр, цифры, `_`, `.`, `-` и `/`.

Хорошо:

```text
jdh:home/father_vanish
jdh:chapter_1/kitchen_change
minecraft:overworld
```

Плохо:

```text
FatherVanish
JDH:Home/Father
jdh_home_father
```

Не меняй watch ID после того, как на него сослались Trigger или Story.

## Target: NPC

Обычная форма:

```csharp
Target(Npc("jdh:father"));
```

С дополнительным расширением bounds:

```csharp
Target(Npc("jdh:father", padding: "0.15"));
```

`padding`:

- по умолчанию `0.05` блока;
- в текущем EchoScript binder передаётся строковым числом, например `padding: "0.15"`;
- допускается диапазон `0..4`;
- расширяет текущий клиентский AABB NPC со всех сторон;
- полезен, если тонкие части модели визуально видны, но не входят в базовый bounds;
- слишком большой padding заставит watch считать NPC частично видимым дольше.

NPC должен существовать в Echo NPC под тем же стабильным `NpcId` и быть materialized для выбранного игрока. Visibility не сканирует мир в поисках похожей сущности.

## Target: Box

Синтаксис:

```csharp
Target(Box(
    dimension: "minecraft:overworld",
    min: Position(100.0, 64.0, 100.0),
    max: Position(106.0, 70.0, 106.0)
));
```

Правила:

- `dimension`, `min` и `max` обязательны;
- координаты могут быть целыми или дробными;
- `min.x/y/z` не должны превышать соответствующие `max.x/y/z`;
- каждая ось должна иметь ненулевой размер;
- размер каждой оси не может превышать 128 блоков;
- Box существует только в указанном dimension.

Пиши bounds вокруг той геометрии, которая действительно должна исчезнуть или измениться. Огромный Box комнаты труднее полностью вывести из поля зрения, чем небольшой Box конкретного объекта.

## Viewpoint

### ACTIVE_CAMERA

```csharp
Viewpoint(ACTIVE_CAMERA);
```

Рекомендуемый режим для сюжета. Использует текущую render camera и поэтому учитывает first person, third person, camera collision, detached cutscene camera, актуальный FOV и projection.

### PLAYER_EYES

```csharp
Viewpoint(PLAYER_EYES);
```

Использует позицию глаз и направление самого игрока. Этот режим нужен только когда игровое правило намеренно относится к направлению взгляда персонажа, а не к изображению на экране.

Для ответа на вопрос «что игрок реально видит на экране» используй `ACTIVE_CAMERA`.

## HiddenWhen и Occlusion

### Только frustum

```csharp
HiddenWhen(OUTSIDE_FRUSTUM);
Occlusion(NONE);
```

Цель считается скрытой только когда весь её bounds находится вне видимой области или полностью за камерой. Стены не учитываются.

Это подходит, когда действие должно происходить только после отвода камеры, но стена не должна считаться достаточным условием.

### Frustum плюс препятствия

```csharp
HiddenWhen(OUTSIDE_FRUSTUM_OR_OCCLUDED);
Occlusion(VISUAL_MULTI_RAY);
```

Когда bounds пересекает frustum, Visibility создаёт sample points и делает bounded raycasts от позиции камеры. Состояние `OCCLUDED` принимается только если все sample points внутри viewport надёжно заблокированы. Неопределённый raycast не превращает цель в скрытую.

Это подходит, когда стена, закрытая дверь или другой непрозрачный объект должны скрывать цель.

### Бессмысленные комбинации

```csharp
HiddenWhen(OUTSIDE_FRUSTUM);
Occlusion(VISUAL_MULTI_RAY);
```

При `OUTSIDE_FRUSTUM` runtime не использует occlusion для итогового состояния, даже если `VISUAL_MULTI_RAY` записан.

```csharp
HiddenWhen(OUTSIDE_FRUSTUM_OR_OCCLUDED);
Occlusion(NONE);
```

Без raycast-режима препятствия не проверяются, поэтому результат практически остаётся frustum-only.

## Samples

NPC sample kinds:

```text
NPC_HEAD
NPC_CHEST
NPC_PELVIS
NPC_FEET
BOUNDS_CENTER
BOUNDS_CORNERS
```

Box sample kinds:

```text
BOX_CENTER
BOX_CORNERS
BOX_FACE_CENTERS
```

Рекомендуемый NPC набор:

```csharp
Samples(NPC_HEAD, NPC_CHEST, NPC_PELVIS, NPC_FEET, BOUNDS_CORNERS);
```

Рекомендуемый Box набор:

```csharp
Samples(BOX_CENTER, BOX_CORNERS, BOX_FACE_CENTERS);
```

Если `Samples(...)` не указан, используются defaults:

```text
NPC: NPC_HEAD, NPC_CHEST, NPC_PELVIS, NPC_FEET, BOUNDS_CENTER, BOUNDS_CORNERS
Box: BOX_CENTER, BOX_CORNERS, BOX_FACE_CENTERS
```

Правила:

- список не может быть пустым;
- значения не должны повторяться;
- Box не может использовать `NPC_*`;
- runtime ограничивает один watch максимум 27 сгенерированными samples;
- если виден хотя бы один надёжный sample, цель остаётся `VISIBLE` или `PARTIALLY_VISIBLE`.

Последнее правило особенно важно: край NPC на экране считается видимостью. `LOST_SIGHT` не должен срабатывать, пока хотя бы одна значимая часть остаётся видна.

## RequireVisibleFirst и AllowInitiallyHidden

`RequireVisibleFirst()` включён по умолчанию, даже если вызов не написан:

```csharp
RequireVisibleFirst();
```

После arm watch находится в ожидании. Сначала цель должна стать стабильно `VISIBLE` или `PARTIALLY_VISIBLE`. Только после этого возможен настоящий переход `LOST_SIGHT`.

Используй это для исчезновения NPC: arm не должен немедленно сработать только потому, что NPC уже находился за спиной.

`AllowInitiallyHidden()` отключает это ограничение:

```csharp
AllowInitiallyHidden();
```

Это полезно для проверки текущего состояния зоны сразу после arm. Для сюжетного исчезновения NPC обычно оставляй `RequireVisibleFirst()`.

Не пиши оба вызова в одном watch. Текущий binder применит последний встреченный вызов, что сделает ресурс трудно читаемым.

## LostAfter и SeenAfter

```csharp
LostAfter(RenderedFrames(1));
SeenAfter(RenderedFrames(2));
```

Оба значения должны находиться в диапазоне `1..120`.

`LostAfter` задаёт, сколько последовательных скрытых render frames нужно для принятия `OUTSIDE_FRUSTUM`, `BEHIND_CAMERA` или `OCCLUDED`.

`SeenAfter` задаёт, сколько последовательных видимых render frames нужно для первого появления или возвращения цели.

Для исчезновения при быстром повороте рекомендуется `LostAfter(RenderedFrames(1))`. `SeenAfter(RenderedFrames(2))` подавляет дрожание состояния на границе экрана.

## EdgeMargin

```csharp
EdgeMargin(Pixels(2));
```

Допустимый диапазон: `0..64` пикселя.

Margin расширяет допустимую область около границы viewport. Небольшое значение уменьшает случайное переключение состояния из-за численной погрешности. Большое значение заставляет цель считаться хотя бы частично видимой уже за визуальной границей экрана.

Обычное значение — `2`.

## Evaluate

```csharp
Evaluate(EVERY_RENDER_FRAME);
```

Проверка выполняется каждый render frame. Это рекомендуемый режим для быстрого поворота камеры и same-frame concealment NPC.

```csharp
Evaluate(EVERY_CLIENT_TICK);
```

Проверка выполняется с частотой client tick. Подходит для менее чувствительных Box watches и уменьшает стоимость наблюдения, но реакция может быть менее плавной.

## Concealment

Без автоматического скрытия:

```csharp
Concealment(NONE);
```

Немедленное скрытие NPC:

```csharp
Concealment(HIDE_NPC_IMMEDIATELY);
```

`HIDE_NPC_IMMEDIATELY` разрешён только для `Target(Npc(...))`.

При подтверждённом `LOST_SIGHT` клиент сразу запрещает render этого NPC и создаёт concealment lease. Trigger Visibility Adapter перед запуском Story claim-ит lease. Story должна действительно удалить, despawn-нуть или перевести NPC в отсутствующее состояние. Adapter завершает lease, когда Echo NPC больше не находит actor body.

Если удаление NPC не подтверждено в течение 10 server ticks, lease отклоняется и NPC снова разрешается отображать. Поэтому concealment нельзя использовать как постоянную невидимость без серверного сюжетного действия.

## Visibility states

| State | Смысл | `View.IsVisible` | `View.IsOutOfView` |
| --- | --- | --- | --- |
| `UNKNOWN` | Ещё нет стабильного наблюдения | `false` | `false` |
| `VISIBLE` | Bounds и значимые samples видимы | `true` | `false` |
| `PARTIALLY_VISIBLE` | Видна хотя бы одна значимая часть | `true` | `false` |
| `OCCLUDED` | Bounds в frustum, но все проверенные rays заблокированы | `false` | `true` |
| `OUTSIDE_FRUSTUM` | Bounds полностью вне frustum | `false` | `true` |
| `BEHIND_CAMERA` | Bounds полностью за камерой | `false` | `true` |
| `TARGET_UNAVAILABLE` | NPC/target сейчас нельзя разрешить | `false` | `false` |
| `CAMERA_UNAVAILABLE` | Camera snapshot/projection недоступны | `false` | `false` |
| `CONCEALED_PENDING_SERVER` | Клиентское concealment ожидает решения сервера | Не считается обычной видимостью | Не является самостоятельным геометрическим hidden state |

`PARTIALLY_VISIBLE` всегда считается видимостью. `TARGET_UNAVAILABLE` не означает, что игрок перестал видеть цель: это отдельная ошибка доступности, поэтому она не должна использоваться как разрешение на исчезновение.

## Trigger visibility subscriptions

Visibility selectors пишутся только внутри `TriggerResource.Init()` и только внутри `Subscribe(...)`.

Поддерживаются:

```csharp
Subscribe(Player.LostSightOf(Watch));
Subscribe(Player.GainedSightOf(Watch));
Subscribe(Player.TargetIsOutOfView(Watch));
```

### Player.LostSightOf

```csharp
Subscribe(Player.LostSightOf(FatherVanish));
```

Срабатывает на переход из стабильного `VISIBLE`/`PARTIALLY_VISIBLE` в `OCCLUDED`, `OUTSIDE_FRUSTUM` или `BEHIND_CAMERA` после `LostAfter(...)`.

Это основной selector для сюжетного исчезновения NPC.

### Player.GainedSightOf

```csharp
Subscribe(Player.GainedSightOf(FatherVanish));
```

Срабатывает, когда ранее скрытая цель снова стала стабильно `VISIBLE` или `PARTIALLY_VISIBLE` после `SeenAfter(...)`.

Первое обнаружение цели публикуется как `FIRST_VISIBLE`, а не `GAINED_SIGHT`, поэтому этот selector предназначен именно для возвращения после потери зрения.

### Player.TargetIsOutOfView

```csharp
Subscribe(Player.TargetIsOutOfView(Kitchen));
```

Селектор получает report о геометрически скрытом состоянии, если report не является более конкретным `LOST_SIGHT` или `GAINED_SIGHT`. В текущем router переход `LOST_SIGHT` имеет приоритет и маршрутизируется в `Player.LostSightOf`.

Практическое применение `TargetIsOutOfView` — initial-hidden сценарий с `AllowInitiallyHidden()`. Не используй его как полный синоним `LostSightOf`.

## Полный TriggerResource

Файл:

```text
story/triggers/chapter_1/father_vanish.trigger.es
```

Код:

```csharp
namespace JDH.Chapter1.Home.Triggers;

class FatherVanishTrigger : TriggerResource
{
    private VisibilityWatchId FatherVanish = "jdh:home/father_vanish";
    private StoryId RemoveFather = "jdh:chapter_1/remove_father";

    private void Init()
    {
        Id("jdh:father_vanish_trigger");
        Subscribe(Player.LostSightOf(FatherVanish));
        StartStory(RemoveFather);
        FireOnlyOncePerArm();
    }
}
```

Правила subscription mode:

- в одном `TriggerResource` допускается только один `Subscribe(...)`;
- рядом должен быть ровно один `StartStory(...)`;
- `StartDialogue(...)` для visibility subscriptions текущей реализацией не поддерживается;
- selector принимает ровно один `VisibilityWatchId` или string;
- нельзя объявлять два TriggerResource с одинаковой комбинацией selector kind + watch ID: adapter отклоняет неоднозначное совпадение;
- `FireOnlyOncePerArm()` является visibility-ориентированным написанием `FireOnlyOnce()`; owner включает игрока, watch ID и arm generation.

Один новый arm создаёт новую generation. Поэтому `FireOnlyOncePerArm()` разрешит одно выполнение на конкретного игрока в рамках этой generation и снова разрешит выполнение после disarm + нового arm.

## Где можно писать View.*

Текущая реализация распознаёт `View.*` только как линейные команды внутри `StoryResource.OnStart()`:

```csharp
class ExampleVisibilityStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:example_visibility_story", start: "start");
    }

    private void OnStart()
    {
        View.Arm("jdh:example/watch");
        View.WaitUntilOutOfView("jdh:example/watch");
        View.AssertStillOutOfView("jdh:example/watch");
        View.Disarm("jdh:example/watch");
    }
}
```

Не пиши `View.*`:

- в `Build()`;
- в `VisibilityWatchResource.Init()`;
- в `TriggerResource.Init()`;
- в произвольном callback;
- как selector без `Subscribe(...)`.

В node-oriented `Nodes()` нет отдельного typed `View.*` binder. Если нужен node fallback, технически можно использовать обычный external `Action(..., type: "echo_visibility:...")` по правилам Story Engine, но для authoring рекомендуется линейный `OnStart()` и typed `View.*`.

## Справочник View.*

| DSL | Результат |
| --- | --- |
| `View.Arm(watch);` | Включает watch для текущего игрока и создаёт новую arm generation. |
| `View.Disarm(watch);` | Выключает активный watch. |
| `View.WaitUntilOutOfView(watch);` | Останавливает Story action до стабильного геометрически hidden state. |
| `View.WaitUntilVisible(watch);` | Останавливает Story action до `VISIBLE` или `PARTIALLY_VISIBLE`. |
| `View.IsOutOfView(watch);` | Немедленно записывает boolean query result. |
| `View.IsVisible(watch);` | Немедленно записывает boolean query result. |
| `View.AssertStillOutOfView(watch);` | Проверяет свежесть blind window перед опасной мутацией. |

Все вызовы имеют один обязательный positional argument — watch ID:

```csharp
View.Arm("jdh:home/father_vanish");
```

Опционально можно указать игрока явно:

```csharp
View.Arm("jdh:home/father_vanish", player: "00000000-0000-0000-0000-000000000000");
```

Обычно `player` писать не нужно. Story, запущенная Visibility Trigger Adapter, наследует UUID игрока из visibility event. Для Story, запущенной другим способом, должен существовать persisted player context или явный UUID.

## View.Arm

```csharp
View.Arm("jdh:home/father_vanish");
```

Arm делает watch активным только для текущего игрока. Один и тот же watch может иметь независимое состояние у разных игроков.

Arm завершится ошибкой, если:

- watch ID отсутствует в catalog;
- этот watch уже armed для игрока;
- достигнут лимит 32 watches;
- NPC target не удалось разрешить;
- target находится в неправильном dimension или невалиден.

## View.WaitUntilOutOfView и WaitUntilVisible

```csharp
View.WaitUntilOutOfView("jdh:home/kitchen_change");
View.WaitUntilVisible("jdh:home/kitchen_change");
```

Wait является recoverable Story action. Если нужное состояние уже установлено, action завершается сразу. Иначе Story остаётся на этом action, пока watch не сообщит требуемое состояние.

У этих DSL-вызовов нет authorable timeout. Если цель никогда не достигнет нужного состояния, Story будет ждать до внешней отмены или изменения состояния. Поэтому всегда проверяй, что watch armed и target доступен.

## View.IsOutOfView и View.IsVisible

```csharp
View.IsOutOfView("jdh:home/kitchen_change");
View.IsVisible("jdh:home/kitchen_change");
```

Результаты по умолчанию записываются в Story execution variables:

```text
visibility.isOutOfView
visibility.isVisible
```

Можно выбрать другое имя:

```csharp
View.IsOutOfView("jdh:home/kitchen_change", output: "kitchen.hidden");
View.IsVisible("jdh:home/kitchen_change", output: "kitchen.visible");
```

Query выполняется немедленно. Для отсутствующего или не armed watch оба результата будут `false`, потому что неизвестное состояние не считается ни видимым, ни геометрически скрытым.

## View.AssertStillOutOfView

```csharp
View.AssertStillOutOfView("jdh:home/kitchen_change");
```

Это серверная проверка blind window. Она проходит, только если:

- watch всё ещё armed;
- последний принятый state — `OCCLUDED`, `OUTSIDE_FRUSTUM` или `BEHIND_CAMERA`;
- последний accepted report не старше 2 server ticks.

Пиши assert непосредственно перед изменением environment, удалением объекта или другой операцией, которая не должна происходить на глазах игрока:

```csharp
View.WaitUntilOutOfView("jdh:home/kitchen_change");
View.AssertStillOutOfView("jdh:home/kitchen_change");
Environment.Set("jdh:kitchen", "changed");
```

`WaitUntilOutOfView` отвечает «состояние было достигнуто». `AssertStillOutOfView` отвечает «окно всё ещё открыто прямо сейчас». Для Box mutation нужны оба.

## Прямой Story-сценарий без Trigger

Если вся последовательность уже управляется одной Story, Trigger не обязателен:

```csharp
namespace JDH.Chapter1.Home.Stories;

class ChangeKitchenWhenUnseen : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/change_kitchen_when_unseen", start: "start");
    }

    private void OnStart()
    {
        View.Arm("jdh:home/kitchen_change");
        View.WaitUntilVisible("jdh:home/kitchen_change");
        View.WaitUntilOutOfView("jdh:home/kitchen_change");
        View.AssertStillOutOfView("jdh:home/kitchen_change");
        Environment.Set("jdh:kitchen", "changed");
        View.Disarm("jdh:home/kitchen_change");
    }
}
```

`WaitUntilVisible` здесь делает смысл `RequireVisibleFirst()` явным на уровне последовательности Story.

## Сценарий через Trigger

Этот вариант нужен, когда одна система включает наблюдение, а потеря зрения должна декларативно запустить отдельную Story.

### 1. Setup Story включает watch

```csharp
class ArmFatherVanish : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/arm_father_vanish", start: "start");
    }

    private void OnStart()
    {
        View.Arm("jdh:home/father_vanish");
        View.WaitUntilVisible("jdh:home/father_vanish");
    }
}
```

### 2. Trigger подписывается на потерю зрения

```csharp
class FatherVanishTrigger : TriggerResource
{
    private VisibilityWatchId FatherVanish = "jdh:home/father_vanish";

    private void Init()
    {
        Id("jdh:father_vanish_trigger");
        Subscribe(Player.LostSightOf(FatherVanish));
        StartStory("jdh:chapter_1/remove_father");
        FireOnlyOncePerArm();
    }
}
```

### 3. Reaction Story проверяет окно и завершает изменение

```csharp
class RemoveFather : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:chapter_1/remove_father", start: "start");
    }

    private void OnStart()
    {
        View.AssertStillOutOfView("jdh:home/father_vanish");
        Environment.Set("jdh:father_state", "vanished");
        View.Disarm("jdh:home/father_vanish");
    }
}
```

Reaction Story не должна снова вызывать `View.Arm` для того же watch: он уже armed и именно его generation породила событие.

Если используется `HIDE_NPC_IMMEDIATELY`, `Environment.Set(...)` в реальной сцене должен привести к фактическому удалению/дематериализации соответствующего Echo NPC. Простая смена несвязанного флага не завершит concealment lease.

## Reload и команды

После изменения visibility watch:

```text
/echo_visibility reload
```

После изменения TriggerResource:

```text
/echo_trigger reload
```

После изменения StoryResource или другого datapack content:

```text
/reload
```

Production-команды Visibility:

```text
/echo_visibility arm <watch_id>
/echo_visibility disarm <watch_id>
/echo_visibility assert_out_of_view <watch_id>
/echo_visibility reload
/echo_visibility status
/echo_visibility inspect <watch_id>
```

`arm`, `disarm`, `assert_out_of_view`, `reload` и `inspect` требуют permission level 2. `status` показывает размер catalog и число active watches для текущего игрока.

`inspect` доступен при установленном Trigger Visibility Adapter и показывает state/status, generation, число подходящих subscriptions, concealment leases и diagnostics.

## Debug-команды

При установленных `echo-debug` и `echo-debug-visibility-adapter` доступны клиентские команды без OP:

```text
/echo_visibility debug player on
/echo_visibility debug player off
/echo_visibility debug watch <watch_id>
/echo_visibility debug watch off
/echo_visibility debug all on
/echo_visibility debug all off
/echo_visibility debug status
```

Практический порядок проверки:

```text
/echo_visibility reload
/echo_trigger reload
/reload
/echo_visibility arm jdh:home/father_vanish
/echo_visibility inspect jdh:home/father_vanish
/echo_visibility debug player on
/echo_visibility debug watch jdh:home/father_vanish
```

World overlay показывает:

- зелёную линию направления глаз игрока;
- голубую линию active camera;
- frustum из той же projection matrix, что использует classifier;
- target AABB;
- sample points;
- occlusion rays и hit points;
- state-зависимые цвета.

HUD показывает camera source, FOV, aspect ratio, state, frustum classification, counters hysteresis, report acknowledgement, transition и concealment.

## Hot reload

При `/echo_visibility reload` loader перечитывает только изменённые `.es` watches. Успешные определения атомарно заменяют catalog.

Если ранее рабочий файл стал невалидным, runtime сохраняет last-known-good definition для этого source вместо мгновенного удаления работающего watch. Уже armed generation остаётся привязана к своей definition revision; новый arm получает новую revision.

После исправления ошибки снова выполни `/echo_visibility reload`.

## Частые ошибки

| Симптом | Причина | Что проверить |
| --- | --- | --- |
| `Visibility watch not found` | Watch не загрузился или ID не совпадает | Путь `echo_visibility/watches`, `.es`, `/echo_visibility reload`, точный `namespace:path` |
| `VisibilityWatchResource must declare Init()` | Нет `private void Init()` | Добавить ровно один `Init()` |
| `Unsupported Visibility Init call` | В `Init()` написан неизвестный DSL-вызов | Сверить таблицу вызовов |
| `Target supports only Npc(...) or Box(...)` | Неверная форма target | Использовать `Target(Npc(...))` или `Target(Box(...))` |
| `NPC id must be namespace:path` | Невалидный ID | Нижний регистр и обязательное `namespace:path` |
| `Visibility Box must have non-zero extent` | Min и max совпали по одной оси | Увеличить bounds |
| `Immediate concealment is allowed only for NPC targets` | Concealment применён к Box | Удалить `HIDE_NPC_IMMEDIATELY` |
| Watch навсегда ждёт visible | Target отсутствует, не в dimension или не попадает в камеру | Debug HUD, target ID, bounds, `Viewpoint`, `RequireVisibleFirst` |
| Стена не скрывает target | Включён frustum-only режим | `OUTSIDE_FRUSTUM_OR_OCCLUDED` + `VISUAL_MULTI_RAY` |
| Потеря зрения не срабатывает у края экрана | Хотя бы один sample остаётся видимым | Это ожидаемо; проверить debug samples и `EdgeMargin` |
| `BLIND_WINDOW_CLOSED` | Игрок успел вернуть камеру или report старше 2 ticks | Assert сразу после wait/event, не вставлять долгие actions между ними |
| Trigger не запускается | Нет exact subscription или изменённый trigger не reload-нут | `/echo_trigger reload`, selector, watch ID, `/echo_visibility inspect ...` |
| Adapter пишет selector conflict | Несколько TriggerResource имеют один selector kind + watch ID | Оставить одну декларативную подписку |
| NPC снова появился | Story не подтвердила удаление за 10 ticks | Проверить фактическое Echo NPC removal/dematerialization |

## Что нельзя считать настоящей visibility-проверкой

Не заменяй Echo Visibility следующими приближениями:

```text
abs(playerYaw - targetYaw) < 90
player.hasLineOfSight(entity)
server-side eye raycast
расстояние до игрока
наличие entity в чанке
факт Minecraft render/culling
```

Эти проверки не знают одновременно active camera, third-person offset, detached camera, реальную projection matrix, динамический FOV, aspect ratio, размер target и частичную видимость.

## Короткая карта допустимого DSL

```text
VisibilityWatchResource.Init()
  можно:
    Id(...)
    Target(Npc(...))
    Target(Box(...))
    Viewpoint(...)
    HiddenWhen(...)
    Occlusion(...)
    Samples(...)
    RequireVisibleFirst()
    AllowInitiallyHidden()
    LostAfter(RenderedFrames(...))
    SeenAfter(RenderedFrames(...))
    EdgeMargin(Pixels(...))
    Concealment(...)
    Evaluate(...)
  нельзя:
    Build()
    Nodes()
    View.*
    Subscribe(...)
    callbacks
    if/loops/return

TriggerResource.Init()
  можно:
    Id(...)
    Subscribe(Player.LostSightOf(watch))
    Subscribe(Player.GainedSightOf(watch))
    Subscribe(Player.TargetIsOutOfView(watch))
    StartStory(...)
    FireOnlyOncePerArm()
  нельзя:
    View.*
    Target(...)
    StartDialogue(...) для visibility subscription
    несколько Subscribe в одном resource

StoryResource.Build()
  можно:
    Story("story_id", start: "start")
  нельзя:
    View.*
    Subscribe(...)
    Target(...)

StoryResource.OnStart()
  можно:
    View.Arm(...)
    View.Disarm(...)
    View.WaitUntilOutOfView(...)
    View.WaitUntilVisible(...)
    View.IsOutOfView(...)
    View.IsVisible(...)
    View.AssertStillOutOfView(...)
    другие поддерживаемые линейные Story-команды
  нельзя:
    Subscribe(Player.*SightOf(...))
    VisibilityWatchResource DSL
```

## Минимальный production checklist

Перед использованием в сцене проверь:

1. Watch лежит в `<gameDirectory>/story/content-pack/<namespace>/echo_visibility/watches`.
2. `Id` и все ссылки используют один и тот же `namespace:path`.
3. NPC ID существует или Box имеет точные production coordinates.
4. Для сюжетного восприятия выбран `ACTIVE_CAMERA`.
5. Для учёта стен одновременно включены `OUTSIDE_FRUSTUM_OR_OCCLUDED` и `VISUAL_MULTI_RAY`.
6. Для исчезновения NPC используется `RequireVisibleFirst()` и `LostAfter(RenderedFrames(1))`.
7. `HIDE_NPC_IMMEDIATELY` используется только для NPC, которого Story действительно удалит.
8. В Trigger существует ровно одна exact subscription на selector.
9. Reaction Story начинает опасное изменение с `View.AssertStillOutOfView(...)`.
10. После завершения watch disarm-ится.
11. Выполнены `/echo_visibility reload`, `/echo_trigger reload` и `/reload` для изменённых типов ресурсов.
12. В debug проверены first person, third person, быстрый поворот, край экрана и occlusion стеной.
