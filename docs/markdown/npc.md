# Echo NPC: подробная документация по NPC

Этот документ описывает модуль `echo-npc`: как создавать NPC, как ставить их в
мир, как менять детскую и взрослую формы, как укладывать NPC спать, как читать
состояние через Java API и как отлаживать ошибки.

Документ написан для контентщиков, сценаристов и разработчиков интеграций. В
нем много примеров кода. После каждого важного блока есть разбор строк: что
эта строка делает, где используется и какие ошибки обычно возникают.

Исходники, на которых основан документ:

```text
D:\JDHSeason\mods\echo-npc
```

Основные модули:

```text
npc-contracts
npc-core
npc-echoscript-adapter
npc-persistence
npc-forge-api-1.20.1
npc-forge-1.20.1
```

## 1. Что такое Echo NPC

Echo NPC хранит NPC как долговечного логического актера. Это важно: NPC не
равен Minecraft entity UUID. Minecraft entity может исчезнуть при unload chunk,
может быть пересоздана после рестарта, но `ActorId` остается тем же.

Главные понятия:

| Понятие | Пример | Что значит |
| --- | --- | --- |
| `ActorId` | `jdh:sasha` | Долговечная личность NPC в сохранении и сценариях. |
| `NpcDefinitionId` | `jdh:sasha` | ID authored definition из `.npc.es`. |
| `NpcFormId` | `jdh:sasha/child` | Тело и внешность NPC: скин, размеры, anchors, portrait, animation graph. |
| `NpcWorldId` | `minecraft:overworld` | Измерение, где должен быть NPC. |
| `EntityBindingId` | UUID-like internal id | Внутренняя связь с текущей Minecraft entity. В сценариях не использовать. |
| `ActorMutationId` | `story:scene-1-spawn-sasha` | Идемпотентный ключ операции, чтобы повтор не создал вторую mutation. |

Ключевая мысль:

```text
Один NPC = один ActorId.
Детская и взрослая версия = разные NpcFormId, но тот же ActorId.
```

Правильно:

```text
ActorId:    jdh:sasha
Child form: jdh:sasha/child
Adult form: jdh:sasha/adult
```

Неправильно:

```text
ActorId: jdh:sasha_child
ActorId: jdh:sasha_adult
```

Почему неправильно: диалоги, катсцены, сюжетные flags и persistence будут
считать это двумя разными персонажами.

## 2. Где лежат ресурсы NPC

NPC definition лежит здесь:

```text
src/main/resources/data/<namespace>/echo_npc/npcs/**/*.npc.es
```

Форма NPC лежит здесь:

```text
src/main/resources/data/<namespace>/echo_npc/forms/**/*.form.es
```

Скины лежат здесь:

```text
src/main/resources/assets/<namespace>/textures/entity/<path>.png
```

Также поддерживается старый fallback path:

```text
src/main/resources/assets/<namespace>/textures/echo_npc/skins/<path>.png
```

Пример из проекта:

```text
D:\JDHSeason\mods\echo-npc\npc-forge-1.20.1\src\main\resources\data\jdh\echo_npc\npcs\sasha.npc.es
D:\JDHSeason\mods\echo-npc\npc-forge-1.20.1\src\main\resources\data\jdh\echo_npc\forms\sasha\child.form.es
D:\JDHSeason\mods\echo-npc\npc-forge-1.20.1\src\main\resources\data\jdh\echo_npc\forms\sasha\adult.form.es
```

## 3. Минимальный NPC в EchoScript

Файл:

```text
data/jdh/echo_npc/npcs/sasha.npc.es
```

Код:

```echoscript
namespace JDH.Npcs.Sasha;

class SashaNpc : NpcResource
{
    public override NpcResourceDef Build()
    {
        return Npc(
            id: "jdh:sasha",
            display: "Саша",
            skin: Skin("jdh:npc/sasha/adult"),
            defaultForm: DefaultForm("jdh:sasha/child")
        );
    }
}
```

Разбор строк:

```echoscript
namespace JDH.Npcs.Sasha;
```

Логическая область имен EchoScript. На runtime identity NPC она не влияет.
Нужна для организации authored content.

```echoscript
class SashaNpc : NpcResource
```

Класс должен наследоваться от `NpcResource`. Компилятор Echo NPC ищет именно
такие классы в `.npc.es`.

```echoscript
public override NpcResourceDef Build()
```

Метод `Build` возвращает definition. В одном ресурсе должен быть понятный
authored return.

```echoscript
return Npc(
```

Создает `NpcDefinition`. Это еще не спавнит NPC в мире. Это только запись в
каталог definition после `/reload`.

```echoscript
id: "jdh:sasha",
```

`NpcDefinitionId`. В простом случае совпадает с `ActorId`, но это разные роли:
definition описывает тип/профиль NPC, actor описывает конкретного NPC в мире.

```echoscript
display: "Саша",
```

Имя, которое попадет в `NpcActorView.displayName()` и может использоваться UI,
диалогом или debug-командами.

```echoscript
skin: Skin("jdh:npc/sasha/adult"),
```

Базовый skin id. Для N5 forms активная форма может заменить skin. Этот base skin
нужен для fallback и старых интеграций.

```echoscript
defaultForm: DefaultForm("jdh:sasha/child")
```

Форма, которая будет закреплена при новом spawn. Это не новая личность NPC.
`jdh:sasha/child` должен существовать в `.form.es`.

## 4. Детская форма NPC

Файл:

```text
data/jdh/echo_npc/forms/sasha/child.form.es
```

Код:

```echoscript
namespace JDH.Npcs.Sasha.Forms;

class SashaChildForm : NpcFormResource
{
    public override NpcFormDef Build()
    {
        return Form(
            id: "jdh:sasha/child",
            skin: Skin("jdh:npc/sasha/child"),
            model: "echo_npc:humanoid",
            dimensions: Dimensions(width: 0.42, height: 1.22, eyeHeight: 1.06),
            bodyScale: 0.74,
            parts: [
                Part(id: "head", scale: 1.18, pivotY: -0.08),
                Part(id: "body", scaleX: 0.84, scaleY: 0.88, scaleZ: 0.84),
                Part(id: "left_arm", scaleX: 0.78, scaleY: 0.82, scaleZ: 0.78, pivotY: -0.04),
                Part(id: "right_arm", scaleX: 0.78, scaleY: 0.82, scaleZ: 0.78, pivotY: -0.04),
                Part(id: "left_leg", scaleX: 0.72, scaleY: 0.74, scaleZ: 0.72, pivotY: -0.12),
                Part(id: "right_leg", scaleX: 0.72, scaleY: 0.74, scaleZ: 0.72, pivotY: -0.12)
            ],
            anchors: [
                Anchor(id: "feet", x: 0, y: 0, z: 0),
                Anchor(id: "chest", x: 0, y: 0.76, z: 0),
                Anchor(id: "head", x: 0, y: 1.14, z: 0),
                Anchor(id: "eyes", x: 0, y: 1.06, z: 0)
            ],
            animationGraph: "jdh:sasha/child_locomotion",
            faceProfile: "jdh:sasha/child_face",
            portrait: "jdh:sasha/child",
            tags: ["sasha", "child"]
        );
    }
}
```

Разбор важных строк:

```echoscript
class SashaChildForm : NpcFormResource
```

Форма должна наследоваться от `NpcFormResource`, не от `NpcResource`.
`NpcResource` создает definition, `NpcFormResource` создает форму.

```echoscript
id: "jdh:sasha/child",
```

`NpcFormId`. Суффикс `/child` обычный path segment. Он показывает, что это
детская форма Саши. Это не отдельный `ActorId`.

```echoscript
skin: Skin("jdh:npc/sasha/child"),
```

Скин активной формы. Он будет синхронизирован в `EchoNpcEntity` и использован
рендером.

```echoscript
model: "echo_npc:humanoid",
```

Профиль модели. Сейчас основной runtime ожидает humanoid-like модель.

```echoscript
dimensions: Dimensions(width: 0.42, height: 1.22, eyeHeight: 1.06),
```

Серверные размеры:

- `width`: ширина collision box;
- `height`: высота collision box;
- `eyeHeight`: высота глаз для камеры, line of sight и интеграций.

Важно: `dimensions` влияют на серверную физику и проверку места. Это не только
визуальный scale.

```echoscript
bodyScale: 0.74,
```

Глобальный визуальный scale тела. Он применяется renderer'ом. Collision при
этом берется из `dimensions`.

```echoscript
Part(id: "head", scale: 1.18, pivotY: -0.08),
```

Голова чуть крупнее относительно тела. Для детской формы это помогает силуэту.
`pivotY` смещает pivot по вертикали.

```echoscript
Part(id: "left_arm", scaleX: 0.78, scaleY: 0.82, scaleZ: 0.78, pivotY: -0.04),
```

Левая рука тоньше и короче. `scaleX`, `scaleY`, `scaleZ` позволяют делать
неравномерную форму.

```echoscript
Anchor(id: "eyes", x: 0, y: 1.06, z: 0)
```

Anchor для глаз. Его могут использовать катсцены, камера, look-at и dialogue
portrait framing.

```echoscript
animationGraph: "jdh:sasha/child_locomotion",
```

ID графа анимаций для детского тела. Animation adapter должен использовать это
как selector, а не пытаться угадывать по имени NPC.

```echoscript
faceProfile: "jdh:sasha/child_face",
```

Профиль лица для dialogue/animation integrations.

```echoscript
portrait: "jdh:sasha/child",
```

Portrait resource id. Диалог может показывать детский portrait, пока форма
активна.

```echoscript
tags: ["sasha", "child"]
```

Теги для поиска, диагностики и authoring conventions. Runtime identity на них
не строится.

## 5. Взрослая форма NPC

Файл:

```text
data/jdh/echo_npc/forms/sasha/adult.form.es
```

Код:

```echoscript
return Form(
    id: "jdh:sasha/adult",
    skin: Skin("jdh:npc/sasha/adult"),
    model: "echo_npc:humanoid",
    dimensions: Dimensions(width: 0.60, height: 1.80, eyeHeight: 1.62),
    bodyScale: 1.0,
    parts: [
        Part(id: "head", scale: 1.0),
        Part(id: "body", scale: 1.0),
        Part(id: "left_arm", scale: 1.0),
        Part(id: "right_arm", scale: 1.0),
        Part(id: "left_leg", scale: 1.0),
        Part(id: "right_leg", scale: 1.0)
    ],
    anchors: [
        Anchor(id: "feet", x: 0, y: 0, z: 0),
        Anchor(id: "chest", x: 0, y: 1.12, z: 0),
        Anchor(id: "head", x: 0, y: 1.70, z: 0),
        Anchor(id: "eyes", x: 0, y: 1.62, z: 0)
    ],
    animationGraph: "jdh:sasha/adult_locomotion",
    faceProfile: "jdh:sasha/adult_face",
    portrait: "jdh:sasha/adult",
    tags: ["sasha", "adult"]
);
```

Отличия от child form:

- height больше: `1.80` вместо `1.22`;
- eyeHeight выше: `1.62` вместо `1.06`;
- bodyScale равен `1.0`;
- anchors выше;
- animation/face/portrait указывают на adult ресурсы.

Для time skip обычно делают так:

```text
FadeOut
Npc.SetForm(jdh:sasha, jdh:sasha/adult)
Npc.PlaceAt(jdh:sasha, minecraft:overworld, 120.5 64 34.5)
FadeIn
```

Идея: игрок не видит резкую замену тела в кадре.

## 6. Команды в игре

Все команды начинаются с:

```text
/echo_npc
```

Нужны права оператора уровня 2.

### 6.1 Проверить здоровье runtime

```text
/echo_npc health
```

Использование: понять, активен ли host, сколько definitions загружено, есть ли
degraded state.

### 6.2 Посмотреть definitions

```text
/echo_npc definitions
```

Использование: проверить, что `.npc.es` попал в каталог после `/reload`.

### 6.3 Посмотреть actors

```text
/echo_npc actors
```

Использование: увидеть активных durable actors.

### 6.4 Посмотреть одного actor

```text
/echo_npc actor jdh:sasha
```

Что смотреть в выводе:

- `actorId`: должен быть `jdh:sasha`;
- `definitionId`: definition, по которой создан actor;
- `activeForm`: текущая форма, например `jdh:sasha/child`;
- `status`: `MATERIALIZED`, `NPC_SLEEPING`, `PENDING_MOVE`, `DORMANT_CHUNK_UNLOADED`;
- `actorRevision`: растет после durable changes;
- `repositoryRevision`: растет после сохранений.

### 6.5 Spawn рядом с игроком

```text
/echo_npc spawn jdh:sasha jdh:sasha
```

Первый аргумент:

```text
jdh:sasha
```

Это `ActorId`. Он становится долговечной личностью NPC.

Второй аргумент:

```text
jdh:sasha
```

Это `NpcDefinitionId`. По нему берется authored definition.

### 6.6 Spawn в точке

```text
/echo_npc spawn jdh:sasha jdh:sasha 120.5 64 34.5
```

Команда ставит NPC в указанную позицию, но это низкоуровневый debug spawn.
Для сцен лучше использовать `spawn_at`.

### 6.7 Scene spawn

```text
/echo_npc spawn_at jdh:sasha 120.5 64 34.5 180 0
```

Аргументы:

```text
jdh:sasha
```

`ActorId` NPC.

```text
120.5 64 34.5
```

Позиция `x y z`.

```text
180
```

Yaw. Поворот вокруг вертикальной оси.

```text
0
```

Pitch. Должен быть от `-90` до `90`.

### 6.8 Place existing NPC

```text
/echo_npc place_at jdh:sasha 130.5 64 40.5 90 0
```

`place_at` отличается от обычного `teleport`: он предназначен для authored
scene placement и учитывает active form dimensions при проверке места.

Если взрослая форма выше и шире, место может быть заблокировано:

```text
NPC_FORM_SPACE_BLOCKED
```

### 6.9 Teleport

```text
/echo_npc teleport jdh:sasha 130.5 64 40.5
```

Debug-перемещение без yaw/pitch аргументов в command tree. Для сцен лучше
использовать `place_at`.

### 6.10 Despawn

```text
/echo_npc despawn jdh:sasha
```

Удаляет durable actor или ставит tombstone в persistence. Повторная команда
может вернуть `ALREADY_ABSENT`.

### 6.11 Список форм

```text
/echo_npc forms list
```

Примерный вывод:

```text
jdh:sasha/child skin=jdh:npc/sasha/child dimensions=NpcBodyDimensions[width=0.42, height=1.22, eyeHeight=1.06] fingerprint=...
jdh:sasha/adult skin=jdh:npc/sasha/adult dimensions=NpcBodyDimensions[width=0.6, height=1.8, eyeHeight=1.62] fingerprint=...
```

Использование: проверить, что `.form.es` скомпилировались.

### 6.12 Inspect form

```text
/echo_npc forms inspect jdh:sasha/child
```

Выводит:

- source path;
- revision;
- skin;
- model;
- bodyScale;
- parts;
- dimensions;
- anchors;
- animationGraph;
- faceProfile;
- portrait;
- fingerprint.

### 6.13 Получить активную форму NPC

```text
/echo_npc form get jdh:sasha
```

Пример:

```text
npc=jdh:sasha activeForm=jdh:sasha/child
```

### 6.14 Сменить форму

```text
/echo_npc form set jdh:sasha jdh:sasha/adult
```

Что происходит:

1. runtime проверяет, что actor существует;
2. проверяет, что форма есть в catalog;
3. проверяет, что NPC не спит;
4. проверяет, что NPC не в pending move;
5. проверяет, что новая collision box помещается;
6. пишет durable state;
7. применяет форму к entity.

### 6.15 Проверить место под форму

```text
/echo_npc form validate_space jdh:sasha jdh:sasha/adult
```

Полезно перед time skip. Если вывод:

```text
NPC_FORM_SPACE_OK
```

Можно менять форму в текущей точке.

Если вывод:

```text
NPC_FORM_SPACE_BLOCKED
```

Нужно сначала подобрать другую позицию или делать `PlaceAt` после закрытого
перехода.

### 6.16 Уложить NPC спать

```text
/echo_npc sleep_at jdh:sasha 100 64 200 minecraft:red_bed
```

Аргументы:

```text
jdh:sasha
```

NPC.

```text
100 64 200
```

Позиция блока кровати.

```text
minecraft:red_bed
```

Ожидаемый block id. Runtime проверяет точное совпадение.

### 6.17 Разбудить NPC

```text
/echo_npc wake_up jdh:sasha
```

Освобождает rest attachment и occupied state кровати.

### 6.18 Inspect rest

```text
/echo_npc inspect_rest jdh:sasha
```

Показывает, где NPC спит, какой provider, какая canonical bed position, какой
pose и command id.

### 6.19 Inspect bed

```text
/echo_npc inspect_bed 100 64 200
```

Показывает сведения о блоке кровати и provider detection.

### 6.20 Rest providers

```text
/echo_npc rest_providers
```

Показывает зарегистрированные rest providers. Встроенный provider:

```text
echo_npc:vanilla_bed
```

### 6.21 Diagnostics

```text
/echo_npc diagnostics
/echo_npc diagnostics 20
```

Использование: смотреть ошибки компиляции ресурсов, reload, persistence,
missing form, bed mismatch.

### 6.22 Operations

```text
/echo_npc operations
/echo_npc operation <operationId>
```

Mutating команды часто возвращают `operationId`. По нему можно посмотреть
состояние async durable operation.

## 7. Java API: базовый доступ к NpcService

Другие Forge-моды получают `NpcService` через bridge.

```java
EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED) {
    return;
}
NpcService npc = lookup.service().orElseThrow();
```

Разбор:

```java
EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
```

Публикует Forge event и ищет ровно одного provider Echo NPC service.

```java
if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED) {
```

Проверяет статус. Возможные статусы:

- `RESOLVED`: сервис найден;
- `UNAVAILABLE`: Echo NPC не активен или provider не ответил;
- `DUPLICATE_PROVIDER`: ответило больше одного provider, использовать сервис
  нельзя.

```java
NpcService npc = lookup.service().orElseThrow();
```

Получает нейтральный `NpcService`. Через него нельзя трогать Forge entity
напрямую; это правильно.

Полный пример utility-метода:

```java
private Optional<NpcService> npcService(MinecraftServer server) {
    EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
    if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED) {
        return Optional.empty();
    }
    return lookup.service();
}
```

## 8. Java API: spawn NPC

```java
NpcResults.Spawn result = npc.spawn(new SpawnNpcRequest(
        new ActorId("jdh:sasha"),
        new NpcDefinitionId("jdh:sasha"),
        new NpcWorldId("minecraft:overworld"),
        new NpcPosition(120.5D, 64.0D, 34.5D),
        new NpcRotation(180.0F, 0.0F)
));
```

Разбор строк:

```java
new ActorId("jdh:sasha")
```

Долговечный ID конкретного NPC. Не генерируй его из entity UUID.

```java
new NpcDefinitionId("jdh:sasha")
```

Definition из catalog. Должен быть загружен из `.npc.es`.

```java
new NpcWorldId("minecraft:overworld")
```

Измерение. Для Nether было бы `minecraft:the_nether`.

```java
new NpcPosition(120.5D, 64.0D, 34.5D)
```

Позиция в мире. Координаты должны быть finite и в допустимых bounds.

```java
new NpcRotation(180.0F, 0.0F)
```

Yaw/pitch. Pitch ограничен диапазоном `[-90, 90]`.

Обработка результата:

```java
switch (result.status()) {
    case QUEUED -> log("Spawn записан в очередь: " + result.operationId().orElse("<none>"));
    case CREATED -> log("NPC создан сразу.");
    case ALREADY_ACTIVE -> log("NPC уже существует.");
    case DEFINITION_NOT_FOUND -> log("Нет definition jdh:sasha. Проверь /reload.");
    case WORLD_NOT_FOUND -> log("Мир не найден.");
    case SERVICE_STOPPED -> log("Echo NPC еще не готов.");
    default -> log("Spawn не выполнен: " + result.message());
}
```

В production обычно ожидай `QUEUED`, потому что Echo NPC использует
save-before-materialization: сначала durable write, потом entity.

## 9. Java API: spawn с идемпотентностью

Если action может повториться после reload/retry, передавай mutation id.

```java
ActorMutationId mutationId = new ActorMutationId("story:prologue-spawn-sasha");

NpcResults.Spawn result = npc.spawn(new SpawnNpcRequest(
        new ActorId("jdh:sasha"),
        new NpcDefinitionId("jdh:sasha"),
        new NpcWorldId("minecraft:overworld"),
        new NpcPosition(120.5D, 64.0D, 34.5D),
        new NpcRotation(180.0F, 0.0F),
        mutationId
));
```

Зачем это нужно:

- первый вызов ставит operation в очередь;
- повторный вызов с тем же `ActorMutationId` не создает вторую запись;
- story adapter может безопасно retry action.

## 10. Java API: query actor

```java
NpcResults.Query query = npc.query(new ActorId("jdh:sasha"));
if (query.actor().isEmpty()) {
    return;
}
NpcActorView actor = query.actor().orElseThrow();
```

Чтение полей:

```java
ActorId actorId = actor.actorId();
NpcDefinitionId definitionId = actor.definitionId();
String displayName = actor.displayName();
Optional<NpcFormId> activeForm = actor.activeFormId();
NpcActorView.Status status = actor.status();
NpcPosition desiredPosition = actor.position();
Optional<NpcPosition> materializedPosition = actor.materializedPosition();
```

Что значат поля:

| Поле | Что значит |
| --- | --- |
| `actorId()` | Долговечная личность NPC. |
| `definitionId()` | Definition, по которой actor создан. |
| `displayName()` | Имя для UI/диалога/debug. |
| `skinId()` | Base skin из definition. Active form может иметь другой skin. |
| `activeFormId()` | Текущая форма. Может быть empty у старого actor без формы. |
| `activeFormRevision()` | Revision catalog на момент закрепления формы. |
| `activeFormFingerprint()` | SHA-256 fingerprint формы. |
| `worldId()` | Desired world. |
| `position()` | Desired position. |
| `materializedPosition()` | Фактическая позиция entity, если она есть. |
| `restAttachment()` | Сон/кровать, если NPC сейчас sleeping. |
| `actorRevision()` | Логическая ревизия actor state. |
| `repositoryRevision()` | Ревизия сохранения на диске. |

Статусы:

| Status | Значение |
| --- | --- |
| `MATERIALIZED` | Entity есть в мире. |
| `NPC_SLEEPING` | NPC спит, есть rest attachment. |
| `DORMANT_CHUNK_UNLOADED` | Actor есть, но chunk не загружен. |
| `PENDING_SPAWN` | Spawn записан, entity еще создается. |
| `PENDING_MOVE` | Move/place/form operation еще применяется. |
| `PENDING_REMOVAL` | Despawn/tombstone в процессе. |
| `BLOCKED_DEFINITION_MISSING` | Definition исчезла из catalog. |
| `BLOCKED_WORLD_MISSING` | Мир недоступен. |
| `BLOCKED_CORRUPT_RECORD` | Сохранение повреждено. |
| `PLATFORM_FAILURE` | Durable write прошел, но platform materialization упал. |

## 11. Java API: смена формы

```java
NpcFormChangeResult result = npc.setForm(new NpcFormChangeRequest(
        new ActorId("jdh:sasha"),
        new NpcFormId("jdh:sasha/adult")
));
```

Разбор:

```java
new ActorId("jdh:sasha")
```

Кого меняем. Это не form id.

```java
new NpcFormId("jdh:sasha/adult")
```

На какую форму меняем.

```java
npc.setForm(...)
```

Операция durable. Runtime сначала проверяет условия, потом пишет состояние.

Обработка результата:

```java
switch (result.status()) {
    case QUEUED -> log("Форма будет применена: " + result.operationId().orElse("<none>"));
    case COMMITTED -> log("Форма уже применена.");
    case ALREADY_ACTIVE -> log("Эта форма уже активна.");
    case FORM_NOT_FOUND -> log("Форма не найдена. Проверь /echo_npc forms list.");
    case SPACE_BLOCKED -> log("Новая форма не помещается в текущей точке.");
    case WHILE_SLEEPING -> log("NPC спит. Сначала wake_up.");
    case WHILE_MOVING -> log("NPC движется или уже pending move.");
    default -> log("SetForm не выполнен: " + result.message());
}
```

Проверить форму read-only:

```java
boolean adult = npc.isForm(new ActorId("jdh:sasha"), new NpcFormId("jdh:sasha/adult"));
```

Получить active form id:

```java
Optional<NpcFormId> active = npc.getForm(new ActorId("jdh:sasha"));
```

Получить definition формы:

```java
Optional<NpcFormDefinition> form = npc.form(new NpcFormId("jdh:sasha/adult"));
```

## 12. Java API: place existing NPC

```java
NpcPlaceResult result = npc.placeAt(new NpcPlaceRequest(
        new ActorId("jdh:sasha"),
        new NpcWorldId("minecraft:overworld"),
        new NpcPosition(130.5D, 64.0D, 40.5D),
        new NpcRotation(90.0F, 0.0F)
));
```

Когда использовать:

- после `SetForm`;
- при time skip;
- при постановке NPC в сцену;
- когда нужна проверка active form dimensions.

Обработка статуса:

```java
switch (result.status()) {
    case QUEUED -> log("PlaceAt записан: " + result.operationId().orElse("<none>"));
    case PLACED -> log("NPC уже в нужной точке.");
    case NOT_FOUND -> log("Actor не найден.");
    case TARGET_INVALID -> log("Мир или позиция невалидны.");
    case COLLISION -> log("Место занято.");
    case WHILE_SLEEPING -> log("NPC спит. Сначала wake_up.");
    default -> log("PlaceAt не выполнен: " + result.message());
}
```

## 13. Java API: сон на кровати

```java
NpcResults.Sleep sleep = npc.sleepAt(new NpcSleepRequest(
        new ActorId("jdh:sasha"),
        new NpcWorldId("minecraft:overworld"),
        new BlockPosition(100, 64, 200),
        new BlockId("minecraft:red_bed")
));
```

Разбор:

```java
new BlockPosition(100, 64, 200)
```

Точная позиция authored bed. Runtime не ищет ближайшую кровать.

```java
new BlockId("minecraft:red_bed")
```

Ожидаемый block id. Если в мире стоит другая кровать, будет
`EXPECTED_BLOCK_MISMATCH`.

Статусы сна:

| Status | Что значит |
| --- | --- |
| `QUEUED` | Durable sleep mutation принята. |
| `SLEEPING` | NPC уже спит или sleep применен. |
| `ALREADY_SLEEPING` | NPC уже имеет rest attachment. |
| `NOT_FOUND` | Actor не найден. |
| `NOT_MATERIALIZED` | Entity нет в загруженном мире. |
| `TOO_FAR` | NPC слишком далеко от кровати. |
| `BED_NOT_FOUND` | Блок кровати отсутствует или chunk не загружен. |
| `EXPECTED_BLOCK_MISMATCH` | Фактический block id не совпал с expectedBlock. |
| `BED_UNSUPPORTED` | Нет provider для этого блока. |
| `BED_OCCUPIED` | Кровать занята. |
| `SERVICE_STOPPED` | Runtime не готов. |

Проверки:

```java
boolean sleeping = npc.isSleeping(new ActorId("jdh:sasha"));
boolean sleepingAt = npc.isSleepingAt(new ActorId("jdh:sasha"), new BlockPosition(100, 64, 200));
Optional<NpcRestAttachmentView> rest = npc.inspectRest(new ActorId("jdh:sasha"));
```

Разбудить:

```java
NpcResults.Wake wake = npc.wakeUp(new NpcWakeRequest(new ActorId("jdh:sasha")));
```

## 14. Time skip: ребенок стал взрослым

Сценарный порядок:

```text
1. Закрыть кадр: FadeOut или черный экран.
2. Остановить движение NPC, если оно идет.
3. Разбудить NPC, если он спит.
4. Поставить взрослую форму.
5. Переставить NPC в позицию, где взрослая форма помещается.
6. Обновить portrait/animation через active form snapshot.
7. Открыть кадр: FadeIn.
```

Команды:

```text
/echo_npc wake_up jdh:sasha
/echo_npc form set jdh:sasha jdh:sasha/adult
/echo_npc place_at jdh:sasha 130.5 64 40.5 90 0
```

Java:

```java
ActorId sasha = new ActorId("jdh:sasha");

npc.wakeUp(new NpcWakeRequest(sasha));
npc.setForm(new NpcFormChangeRequest(sasha, new NpcFormId("jdh:sasha/adult")));
npc.placeAt(new NpcPlaceRequest(
        sasha,
        new NpcWorldId("minecraft:overworld"),
        new NpcPosition(130.5D, 64.0D, 40.5D),
        new NpcRotation(90.0F, 0.0F)
));
```

Чего не делать:

```java
npc.spawn(new SpawnNpcRequest(
        new ActorId("jdh:sasha_adult"),
        new NpcDefinitionId("jdh:sasha_adult"),
        world,
        pos,
        rot
));
```

Почему плохо: это создаст новую личность и оторвет ее от старых диалогов,
flags, memories и persistence.

## 15. Как устроен form snapshot

`NpcFormSnapshot` содержит:

```java
NpcFormId id;
NpcFormRevision revision;
Optional<NpcSkinId> skinId;
String modelProfile;
NpcBodyGeometry bodyGeometry;
NpcBodyDimensions dimensions;
List<NpcBodyAnchorSnapshot> anchors;
String animationGraphId;
String faceProfileId;
String portraitResourceId;
String fingerprint;
```

Использование в dialogue adapter:

```java
Optional<NpcFormDefinition> form = npc.form(actor.activeFormId().orElseThrow());
String portrait = form.map(value -> value.snapshot().portraitResourceId())
        .orElse("dialogue:fallback");
```

Использование в animation adapter:

```java
Optional<NpcFormDefinition> form = actor.activeFormId().flatMap(npc::form);
String graph = form.map(value -> value.snapshot().animationGraphId())
        .orElse("animation:default_humanoid");
```

Использование в cutscene/camera:

```java
Optional<NpcFormDefinition> form = actor.activeFormId().flatMap(npc::form);
var eyes = form.stream()
        .flatMap(value -> value.snapshot().anchors().stream())
        .filter(anchor -> anchor.id().value().equals("eyes"))
        .findFirst();
```

## 16. Dimensions и transforms

`NpcBodyDimensions`:

```java
new NpcBodyDimensions(0.60D, 1.80D, 1.62D)
```

Аргументы:

```text
width     = ширина collision box
height    = высота collision box
eyeHeight = высота глаз, должна быть <= height
```

Ограничения:

```text
width > 0.05 и width <= 4.0
height > 0.05 и height <= 8.0
eyeHeight > 0.0 и eyeHeight <= height
```

`NpcBodyPartTransform`:

```java
new NpcBodyPartTransform(
        NpcBodyPartId.HEAD,
        1.18D, 1.18D, 1.18D,
        0.0D, -0.08D, 0.0D,
        0.0D, 0.0D, 0.0D
);
```

Поля:

| Поле | Значение |
| --- | --- |
| `partId` | Semantic часть тела: `HEAD`, `BODY`, `LEFT_ARM`, `RIGHT_ARM`, `LEFT_LEG`, `RIGHT_LEG`. |
| `scaleX` | Масштаб по X. |
| `scaleY` | Масштаб по Y. |
| `scaleZ` | Масштаб по Z. |
| `pivotX` | Смещение pivot по X. |
| `pivotY` | Смещение pivot по Y. |
| `pivotZ` | Смещение pivot по Z. |
| `rotationX` | Поворот части вокруг X, градусы. |
| `rotationY` | Поворот части вокруг Y, градусы. |
| `rotationZ` | Поворот части вокруг Z, градусы. |

Короткий uniform helper:

```java
NpcBodyPartTransform.uniform(NpcBodyPartId.HEAD, 1.18D)
```

Это то же самое, что задать одинаковый scale для X/Y/Z и нулевые pivots/rotations.

## 17. Persistence: что сохраняется

Actor save schema v3 хранит active form как ссылку:

```json
{
  "schemaVersion": 3,
  "actorId": "jdh:sasha",
  "definitionId": "jdh:sasha",
  "definitionFingerprint": "...",
  "presence": "ACTIVE",
  "worldId": "minecraft:overworld",
  "position": {"x": 130.5, "y": 64.0, "z": 40.5},
  "rotation": {"yaw": 90.0, "pitch": 0.0},
  "entityBindingId": "12345678-1234-1234-1234-123456789abc",
  "actorRevision": 7,
  "repositoryRevision": 4,
  "createdAtEpochMillis": 1000,
  "updatedAtEpochMillis": 2000,
  "activeForm": {
    "formId": "jdh:sasha/adult",
    "revision": 3,
    "fingerprint": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
  }
}
```

Разбор:

```json
"schemaVersion": 3
```

Версия схемы. `activeForm` появилась в v3.

```json
"actorId": "jdh:sasha"
```

Долговечная личность. Не меняется при смене формы.

```json
"activeForm": { ... }
```

Текущая форма. Здесь нет полного тела/скина/anchors. Полное описание берется из
каталога `.form.es`.

```json
"fingerprint": "bbbb..."
```

Semantic fingerprint формы. Нужен, чтобы runtime понимал, какой authored
snapshot был закреплен.

## 18. Диагностика и частые ошибки

### Форма не найдена

Симптом:

```text
NPC_FORM_NOT_FOUND
```

Проверить:

```text
/reload
/echo_npc forms list
/echo_npc forms inspect jdh:sasha/adult
```

Причины:

- файл лежит не в `data/<namespace>/echo_npc/forms`;
- расширение не `.form.es`;
- `id` в файле отличается от того, что вызывается в `SetForm`;
- `.form.es` не скомпилировался.

### Форма уже активна

Симптом:

```text
ALREADY_ACTIVE
```

Это не ошибка. Runtime сообщает, что менять ничего не нужно.

### Новая форма не помещается

Симптом:

```text
SPACE_BLOCKED
NPC_FORM_SPACE_BLOCKED
```

Проверить:

```text
/echo_npc form validate_space jdh:sasha jdh:sasha/adult
```

Решение:

- изменить позицию;
- вызвать `place_at` в свободную точку;
- уменьшить `dimensions`, если authored модель должна быть меньше.

### NPC спит

Симптом:

```text
WHILE_SLEEPING
NPC_FORM_CHANGE_WHILE_SLEEPING
```

Решение:

```text
/echo_npc wake_up jdh:sasha
/echo_npc form set jdh:sasha jdh:sasha/adult
```

### Bed mismatch

Симптом:

```text
EXPECTED_BLOCK_MISMATCH
NPC_SLEEP_EXPECTED_BLOCK_MISMATCH
```

Причина: в сценарии указан `minecraft:red_bed`, а в мире стоит другой блок.

Проверить:

```text
/echo_npc inspect_bed 100 64 200
```

### Definition не найдена

Симптом:

```text
DEFINITION_NOT_FOUND
BLOCKED_DEFINITION_MISSING
```

Проверить:

```text
/reload
/echo_npc definitions
/echo_npc diagnostics 20
```

## 19. Полный сценарий: создать Сашу, проверить, сменить форму

1. Убедиться, что ресурсы лежат на месте:

```text
data/jdh/echo_npc/npcs/sasha.npc.es
data/jdh/echo_npc/forms/sasha/child.form.es
data/jdh/echo_npc/forms/sasha/adult.form.es
```

2. Перезагрузить ресурсы:

```text
/reload
```

3. Проверить definition:

```text
/echo_npc definitions
```

4. Проверить формы:

```text
/echo_npc forms list
```

5. Создать NPC:

```text
/echo_npc spawn_at jdh:sasha 120.5 64 34.5 180 0
```

6. Проверить actor:

```text
/echo_npc actor jdh:sasha
```

7. Проверить active form:

```text
/echo_npc form get jdh:sasha
```

Ожидаемо:

```text
jdh:sasha/child
```

8. Проверить место под adult form:

```text
/echo_npc form validate_space jdh:sasha jdh:sasha/adult
```

9. Сменить форму:

```text
/echo_npc form set jdh:sasha jdh:sasha/adult
```

10. Поставить NPC в сцену:

```text
/echo_npc place_at jdh:sasha 130.5 64 40.5 90 0
```

11. Проверить итог:

```text
/echo_npc actor jdh:sasha
/echo_npc form get jdh:sasha
```

## 20. Полный Java пример: scene controller

```java
public final class SashaSceneController {
    private final MinecraftServer server;

    public SashaSceneController(MinecraftServer server) {
        this.server = java.util.Objects.requireNonNull(server, "server");
    }

    public void runTimeSkip() {
        Optional<NpcService> service = npcService();
        if (service.isEmpty()) {
            return;
        }
        NpcService npc = service.orElseThrow();
        ActorId sasha = new ActorId("jdh:sasha");

        npc.wakeUp(new NpcWakeRequest(sasha));
        npc.setForm(new NpcFormChangeRequest(sasha, new NpcFormId("jdh:sasha/adult")));
        npc.placeAt(new NpcPlaceRequest(
                sasha,
                new NpcWorldId("minecraft:overworld"),
                new NpcPosition(130.5D, 64.0D, 40.5D),
                new NpcRotation(90.0F, 0.0F)
        ));
    }

    private Optional<NpcService> npcService() {
        EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
        if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED) {
            return Optional.empty();
        }
        return lookup.service();
    }
}
```

Разбор:

```java
private final MinecraftServer server;
```

Нужен Forge server context, потому что Echo NPC service server-scoped.

```java
Optional<NpcService> service = npcService();
```

Не предполагаем, что Echo NPC всегда доступен. Интеграция должна переживать
отсутствие сервиса.

```java
ActorId sasha = new ActorId("jdh:sasha");
```

Один ID на всю жизнь персонажа.

```java
npc.wakeUp(new NpcWakeRequest(sasha));
```

Перед сменой формы снимаем sleeping attachment. Если NPC не спит, wake вернет
`ALREADY_AWAKE`.

```java
npc.setForm(new NpcFormChangeRequest(sasha, new NpcFormId("jdh:sasha/adult")));
```

Меняем active form.

```java
npc.placeAt(new NpcPlaceRequest(...));
```

Ставим взрослую Сашу в позицию, рассчитанную под взрослые dimensions.

## 21. Интеграции

### Dialogue

Dialogue adapter должен использовать:

```java
actor.displayName()
actor.activeFormId()
npc.form(activeFormId)
form.snapshot().portraitResourceId()
form.snapshot().faceProfileId()
```

Правило: `activeFormId` не становится speaker identity. Speaker identity
остается `ActorId`.

### Animation

Animation adapter должен использовать:

```java
ActorId -> AnimationSubjectId
NpcFormSnapshot.animationGraphId()
NpcFormSnapshot.bodyGeometry()
```

Правило: subject identity не меняется при смене формы. Меняется graph/profile.

### Cutscene

Cutscene/camera должна использовать anchors:

```java
eyes
head
chest
feet
```

Пример применения:

```text
Для детской формы камера целится в eyes y=1.06.
Для взрослой формы камера целится в eyes y=1.62.
```

### Navigation

Navigation subject должен оставаться основанным на `ActorId`.

```text
jdh:sasha -> navigation subject jdh:sasha
```

Смена формы не должна создавать новый navigation subject.

## 22. Правила authoring

1. Не создавай отдельный actor для child/adult.
2. Всегда проверяй `/echo_npc forms list` после `/reload`.
3. Для важных сцен задавай anchors явно.
4. Для time skip делай `SetForm` под закрытым кадром.
5. Перед adult form проверяй `validate_space`.
6. Для сна указывай точный `expectedBlock`.
7. Для story retry используй stable mutation id.
8. Не читай Forge entity напрямую, если хватает `NpcActorView`.
9. Не используй Minecraft entity UUID как gameplay identity.
10. Не меняй skin вручную вне forms, если персонаж уже form-aware.

## 23. Быстрая шпаргалка

Создать NPC:

```text
/echo_npc spawn_at jdh:sasha 120.5 64 34.5 180 0
```

Проверить:

```text
/echo_npc actor jdh:sasha
```

Формы:

```text
/echo_npc forms list
/echo_npc forms inspect jdh:sasha/child
/echo_npc form get jdh:sasha
```

Сменить форму:

```text
/echo_npc form set jdh:sasha jdh:sasha/adult
```

Поставить в сцену:

```text
/echo_npc place_at jdh:sasha 130.5 64 40.5 90 0
```

Сон:

```text
/echo_npc sleep_at jdh:sasha 100 64 200 minecraft:red_bed
/echo_npc inspect_rest jdh:sasha
/echo_npc wake_up jdh:sasha
```

Диагностика:

```text
/echo_npc diagnostics 20
/echo_npc operations
/echo_npc blocked
```

## 24. Минимальный checklist перед сдачей сцены

- `.npc.es` есть в `data/<namespace>/echo_npc/npcs`.
- `.form.es` есть в `data/<namespace>/echo_npc/forms`.
- `/reload` не дал ошибок в diagnostics.
- `/echo_npc definitions` показывает нужный definition.
- `/echo_npc forms list` показывает child/adult формы.
- `/echo_npc spawn_at ...` создает actor.
- `/echo_npc actor ...` показывает правильный `ActorId`.
- `/echo_npc form get ...` показывает default form.
- `/echo_npc form validate_space ...` проходит для target form.
- `SetForm` выполняется под закрытым переходом, если смена должна быть незаметной.
- Dialogue берет portrait из active form.
- Animation берет graph из active form.
- Cutscene берет framing anchors из active form.

## 25. Когда использовать каждую сущность

В Echo NPC много похожих слов: actor, definition, form, snapshot, entity,
operation. Ниже таблица “когда использовать”, чтобы не путаться.

| Нужно сделать | Используй | Не используй |
| --- | --- | --- |
| Сослаться на персонажа в сюжете | `ActorId` | Minecraft UUID |
| Описать authored NPC в ресурсах | `NpcDefinitionId` / `.npc.es` | `ActorId` как файл сохранения |
| Описать тело, скин, рост, portrait | `NpcFormId` / `.form.es` | Отдельный NPC |
| Поставить нового NPC в мир | `spawn` / `spawn_at` | `SetForm` |
| Переставить существующего NPC | `place_at` / `placeAt` | новый `spawn` |
| Быстро debug-передвинуть NPC | `/echo_npc teleport` | `spawn_at` |
| Сменить ребенка на взрослого | `SetForm` + `PlaceAt` | `despawn` + `spawn` другого actor |
| Проверить, что NPC видим физически | `isMaterialized` или `NpcActorView.status()` | client renderer state |
| Узнать portrait для диалога | active `NpcFormSnapshot.portraitResourceId()` | hardcoded actor name |
| Узнать animation graph | active `NpcFormSnapshot.animationGraphId()` | definition id |
| Узнать точку глаз для камеры | active form anchor `eyes` | fixed Y offset |
| Уложить на конкретную кровать | `SleepAt` с `BlockPosition` и `BlockId` | поиск ближайшей кровати |
| Повторить story action безопасно | `ActorMutationId` / command id | случайный UUID каждый раз |

Правило:

```text
Если меняется личность персонажа - это ActorId.
Если меняется вид персонажа - это NpcFormId.
Если меняется место персонажа - это PlaceAt/Teleport.
Если меняется authored описание - это .npc.es или .form.es + /reload.
```

## 26. Когда использовать команды, EchoScript и Java API

### Команды

Команды нужны, когда:

- ты руками проверяешь сцену в игре;
- нужно быстро понять, загрузился ли NPC;
- нужно debug-проверить форму, место, кровать, operations;
- ты не пишешь интеграцию, а настраиваешь authored content.

Пример:

```text
/echo_npc forms inspect jdh:sasha/child
```

Когда команда лучше Java:

```text
Во время тестирования карты и ресурсов.
```

Когда команда хуже Java:

```text
Когда действие должно быть частью story/cutscene runtime.
```

### EchoScript resources

`.npc.es` и `.form.es` нужны, когда:

- ты описываешь каталог NPC;
- ты задаешь display name, skin, default form;
- ты описываешь размеры тела, anchors, portrait, animation graph;
- ты хочешь, чтобы `/reload` пересобрал контент.

Пример:

```echoscript
return Npc(
    id: "jdh:sasha",
    display: "Саша",
    defaultForm: DefaultForm("jdh:sasha/child")
);
```

Когда EchoScript лучше Java:

```text
Когда это authored data, а не runtime-действие.
```

Когда EchoScript хуже Java:

```text
Когда нужно выполнить действие прямо сейчас в сцене: spawn, place, setForm.
```

### Java API

Java API нужен, когда:

- другой мод запускает story action;
- dialogue adapter читает presentation;
- animation adapter выбирает graph;
- cutscene adapter строит camera target;
- automation/recovery должен быть идемпотентным.

Пример:

```java
npc.setForm(new NpcFormChangeRequest(
        new ActorId("jdh:sasha"),
        new NpcFormId("jdh:sasha/adult")
));
```

Когда Java API лучше команды:

```text
Когда действие должно быть повторяемым, проверяемым и частью runtime flow.
```

## 27. Жизненный цикл NPC от файла до entity

Полный путь NPC:

```text
.npc.es и .form.es
-> /reload
-> NpcCatalogCompiler
-> NpcDefinitionCatalog
-> spawn/spawn_at
-> ActorDesiredState
-> save-before-materialization
-> ActorFileRepository
-> ForgeActorMaterializer
-> EchoNpcEntity
-> client renderer
```

Что происходит по шагам:

1. Автор кладет `.npc.es` и `.form.es` в resources.
2. Minecraft reload вызывает resource discovery.
3. Echo NPC компилирует NPC definitions и forms.
4. Если есть ошибки, catalog не должен устанавливаться частично.
5. `spawn_at` создает durable actor state.
6. State сначала пишется в repository.
7. Только после успешной записи создается или обновляется entity.
8. Entity получает actor id, binding id, skin, dimensions, active form state.
9. Client renderer рисует entity по synchronized state.

Ключевая гарантия:

```text
Если сохранение не прошло, видимое состояние не считается принятым.
```

Это называется save-before-materialization.

## 28. Что происходит при /reload

`/reload` перечитывает resources:

```text
data/*/echo_npc/npcs/**/*.npc.es
data/*/echo_npc/forms/**/*.form.es
```

После reload:

- новые definitions появляются в catalog;
- новые forms появляются в catalog;
- fingerprints пересчитываются;
- actor state не удаляется автоматически;
- active actors сверяются с новым catalog;
- missing definition/form может заблокировать actor.

Когда reload безопасен:

```text
Ты добавил новую форму или поправил dimensions, но ActorId остался тем же.
```

Когда reload может заблокировать NPC:

```text
Ты удалил .npc.es, на который ссылается сохраненный actor.
Ты переименовал NpcFormId, который уже записан в activeForm.
Ты сломал EchoScript syntax.
```

Что делать после reload:

```text
/echo_npc diagnostics 20
/echo_npc definitions
/echo_npc forms list
/echo_npc blocked
```

Если actor заблокировался из-за missing form:

```text
1. Верни старый NpcFormId в .form.es.
2. Сделай /reload.
3. Проверь /echo_npc actor <id>.
4. Потом уже меняй форму штатно через SetForm.
```

## 29. Что происходит при рестарте мира

При остановке сервера:

- runtime останавливается;
- materialized entities удаляются или перестают быть authoritative;
- durable actor records остаются на диске.

При старте сервера:

- `ActorFileRepository` читает сохранения;
- actor records возвращаются в `ActorRuntime`;
- runtime сверяет definitions и forms с catalog;
- если chunk загружен, NPC может материализоваться;
- если chunk не загружен, actor становится dormant.

Нормальный dormant статус:

```text
DORMANT_CHUNK_UNLOADED
```

Это не ошибка. Это значит:

```text
NPC есть в durable state, но физическая entity сейчас не нужна, потому что chunk не загружен.
```

Ошибки восстановления:

| Симптом | Что значит | Что делать |
| --- | --- | --- |
| `BLOCKED_DEFINITION_MISSING` | Нет definition в catalog | Вернуть `.npc.es` или исправить id |
| `BLOCKED_WORLD_MISSING` | Нет dimension/world | Проверить world id |
| `BLOCKED_CORRUPT_RECORD` | JSON сохранения поврежден | Смотреть backup/diagnostics |
| `PLATFORM_FAILURE` | Не удалось создать entity | Смотреть Forge diagnostics/log |

## 30. Что происходит при chunk unload

Когда chunk выгружается:

```text
ActorId остается в ActorRuntime и repository.
Minecraft entity может исчезнуть или стать недоступной.
```

Правильная логика интеграции:

```java
NpcResults.Query query = npc.query(new ActorId("jdh:sasha"));
Optional<NpcActorView> actor = query.actor();
```

Потом:

```java
if (actor.map(value -> value.status() == NpcActorView.Status.DORMANT_CHUNK_UNLOADED).orElse(false)) {
    // Не проваливай сюжет навсегда.
    // Подожди загрузки chunk или перенеси NPC через PlaceAt в загруженную сцену.
}
```

Не делай так:

```java
// Плохо: считать отсутствие entity отсутствием NPC.
if (minecraftEntity == null) {
    spawnAnotherSasha();
}
```

Почему плохо: появятся дубликаты или расхождение durable state.

## 31. Когда использовать Spawn, SpawnAt, Teleport, PlaceAt

| Действие | Когда использовать | Что проверяет | Типичный результат |
| --- | --- | --- | --- |
| `spawn` | Debug или простое создание actor | definition, world, actor conflict | `QUEUED`, `ALREADY_ACTIVE` |
| `spawn_at` | Сцена создает NPC в точке | position, yaw/pitch, scene readiness | `QUEUED` |
| `teleport` | Debug-перемещение существующего actor | world/position basic validation | `QUEUED`, `MOVED` |
| `place_at` | Authored scene placement | active form dimensions/collision | `QUEUED`, `COLLISION` |

Если NPC еще не существует:

```text
Используй spawn_at.
```

Если NPC уже существует и нужно поставить его для сцены:

```text
Используй place_at.
```

Если NPC уже существует и ты просто debug-тестишь:

```text
Можно использовать teleport.
```

Если NPC стал взрослым:

```text
SetForm -> PlaceAt.
```

Если NPC спит:

```text
WakeUp -> PlaceAt.
```

## 32. Когда использовать SleepAt и WakeUp

`SleepAt` использовать, когда:

- в сцене NPC должен лежать на конкретной кровати;
- важно, чтобы bed block был именно authored block;
- после рестарта NPC должен восстановиться sleeping;
- dialogue/cutscene predicates должны видеть sleeping state.

`WakeUp` использовать, когда:

- NPC должен встать после сна;
- перед `SetForm`;
- перед `PlaceAt`, если NPC лежит;
- перед despawn, если нужно освободить occupied state кровати.

Нормальный порядок:

```text
SpawnAt -> SleepAt -> Dialogue -> WakeUp -> PlaceAt
```

Пример в `StoryResource`:

```csharp
class FirstMorningStory : StoryResource
{
	private void Init()
	{
		Id("jdh:chapter_1/home/first_morning");

		Npc.SleepAt("jdh:sasha", 49, -3, 92);
		Dialogue.Load("jdh:chapter_1/home/first_morning");

		// Поднять NPC именно с кровати.
		Npc.WakeUp("jdh:sasha");

		// Если после пробуждения нужно поставить NPC в точку утренней сцены.
		Npc.SpawnAt("jdh:sasha", 42, -3, 100, yaw: 180, pitch: 0);
	}
}
```

`Npc.StandUp("actorId")` не поднимает NPC с кровати. Это команда для сидячего
interaction/seat состояния, созданного через `Npc.Interact(...)`.

```csharp
Npc.Interact("jdh:sasha", 42, -3, 100);
Npc.StandUp("jdh:sasha");
```

Для сна и кровати используй `Npc.WakeUp("actorId")`. Для игрока, который сидит на
стуле после `Player.SitAt(...)`, используй `Player.StandUp()`.

Плохой порядок:

```text
SleepAt -> SetForm
```

Почему плохой:

```text
Форма может изменить dimensions и pose, пока NPC закреплен на bed attachment.
Runtime поэтому отклоняет SetForm для sleeping NPC.
```

## 33. Когда использовать activeFormId, а когда form snapshot

`activeFormId` достаточно, когда:

- нужно проверить child/adult;
- нужно вывести debug;
- нужно решить простой predicate.

Пример:

```java
boolean isAdult = actor.activeFormId()
        .map(new NpcFormId("jdh:sasha/adult")::equals)
        .orElse(false);
```

`NpcFormSnapshot` нужен, когда:

- нужен portrait;
- нужен animation graph;
- нужны anchors;
- нужны dimensions;
- нужен skin active form.

Пример:

```java
Optional<NpcFormDefinition> definition = actor.activeFormId().flatMap(npc::form);
Optional<NpcFormSnapshot> snapshot = definition.map(NpcFormDefinition::snapshot);
```

Правило:

```text
ID отвечает на вопрос "какая форма".
Snapshot отвечает на вопрос "что внутри формы".
```

## 34. Когда использовать actorRevision и repositoryRevision

`actorRevision` меняется, когда меняется логическое состояние actor:

- spawn;
- place/teleport;
- setForm;
- sleep;
- wake;
- despawn/tombstone;
- binding changes.

`repositoryRevision` меняется, когда состояние записано в repository.

Использование:

```java
long before = actor.actorRevision();
// вызвать action
long after = npc.query(actor.actorId()).actor().orElseThrow().actorRevision();
boolean changed = after > before;
```

Когда полезно:

- postcondition в story adapter;
- ожидание завершения async operation;
- проверка, что replay не сделал лишнего;
- диагностика "почему actor не поменялся".

Когда не использовать:

```text
Не используй revisions как gameplay flags.
Не сохраняй "если revision == 7, значит adult".
Для gameplay используй activeFormId или свои story flags.
```

## 35. Когда использовать operationId

Mutating operations могут вернуть `operationId`.

Пример:

```java
NpcResults.Spawn result = npc.spawn(request);
Optional<String> operationId = result.operationId();
```

Используй `operationId`, когда:

- действие async;
- нужно показать progress;
- нужно восстановиться после reload story action;
- нужно проверить terminal state.

Проверить operation:

```java
NpcResults.QueryOperation operation = npc.operation(operationId.orElseThrow());
```

Статусы:

| Status | Значение |
| --- | --- |
| `QUEUED` | Операция поставлена в очередь persistence. |
| `RUNNING` | Выполняется. |
| `SUCCEEDED` | Durable write и materialization прошли. |
| `FAILED` | Упало после принятия. |
| `REJECTED` | Отклонено до нормального выполнения. |

## 36. Когда нужна идемпотентность

Идемпотентность нужна почти всегда, когда action приходит из story/cutscene
runtime.

Нужно:

```java
ActorMutationId id = new ActorMutationId("story:scene-12-spawn-sasha");
```

Плохо:

```java
ActorMutationId id = new ActorMutationId("story:" + UUID.randomUUID());
```

Почему плохо:

```text
Каждый retry станет новой операцией.
```

Хороший id:

```text
<system>:<scene-id>-<action-id>-<npc-id>
```

Примеры:

```text
story:prologue-001-spawn-sasha
story:prologue-002-sasha-adult-form
cutscene:chapter2-place-sasha-marker-a
```

Для forms есть `NpcFormCommandId`, для scene placement есть
`NpcSceneCommandId`, для spawn/teleport/despawn есть `ActorMutationId`.

## 37. Когда создавать новый ActorId

Создавай новый `ActorId`, когда:

- это реально другой персонаж;
- это клон по сюжету;
- это временный статист без memories;
- это одноразовый crowd actor.

Не создавай новый `ActorId`, когда:

- ребенок стал взрослым;
- NPC переоделся;
- NPC сменил portrait;
- NPC переехал в другую локацию;
- NPC лег спать или встал;
- NPC временно не виден в сцене.

Примеры:

```text
jdh:sasha              - основная Саша
jdh:sasha_dream_copy   - отдельная сущность сна, если по сюжету это копия
jdh:market_vendor_01   - отдельный торговец
jdh:guard_patrol_a     - отдельный охранник
```

## 38. Когда менять .npc.es, а когда .form.es

Меняй `.npc.es`, если:

- изменилось display name;
- изменился base skin fallback;
- изменился default form для новых spawns;
- меняется definition id;
- меняется authored definition metadata.

Меняй `.form.es`, если:

- изменился рост;
- изменился active skin;
- изменился bodyScale;
- изменились части тела;
- изменились anchors;
- изменился portrait;
- изменился animation graph;
- изменился face profile.

Важно:

```text
Изменение defaultForm в .npc.es не мигрирует автоматически уже сохраненных actors.
У уже существующего actor activeForm хранится в persistence.
```

Если нужно поменять уже существующего NPC:

```text
/echo_npc form set <actorId> <newFormId>
```

или Java:

```java
npc.setForm(new NpcFormChangeRequest(actorId, newFormId));
```

## 39. Когда форма применяется сразу, а когда через очередь

Echo NPC предпочитает durable write перед видимым изменением.

Частые случаи:

| Вызов | Что может вернуть | Что значит |
| --- | --- | --- |
| `spawn` | `QUEUED` | Запись принята, entity появится после repository result. |
| `setForm` | `QUEUED` | Форма будет применена после durable update. |
| `placeAt` | `QUEUED` | Позиция будет применена после durable update. |
| `despawn` | `QUEUED` | Tombstone/remove будет применен после durable update. |

Не пиши код так:

```java
npc.setForm(request);
NpcActorView immediately = npc.query(actorId).actor().orElseThrow();
// Ожидать, что immediately уже adult, нельзя.
```

Правильно:

```java
NpcFormChangeResult result = npc.setForm(request);
if (result.operationId().isPresent()) {
    // Подождать operation SUCCEEDED или actorRevision change.
}
```

## 40. Рецепт: NPC появляется только если его еще нет

```java
ActorId actorId = new ActorId("jdh:sasha");
NpcResults.Query query = npc.query(actorId);

if (query.actor().isEmpty()) {
    npc.spawn(new SpawnNpcRequest(
            actorId,
            new NpcDefinitionId("jdh:sasha"),
            new NpcWorldId("minecraft:overworld"),
            new NpcPosition(120.5D, 64.0D, 34.5D),
            new NpcRotation(180.0F, 0.0F),
            new ActorMutationId("story:prologue-spawn-sasha")
    ));
}
```

Почему так:

- сначала читаем durable actor;
- если actor уже есть, не создаем второго;
- mutation id защищает от retry.

## 41. Рецепт: NPC должен быть в сцене перед диалогом

```java
ActorId actorId = new ActorId("jdh:sasha");
NpcResults.Query query = npc.query(actorId);

if (query.actor().isEmpty()) {
    npc.spawn(new SpawnNpcRequest(
            actorId,
            new NpcDefinitionId("jdh:sasha"),
            new NpcWorldId("minecraft:overworld"),
            new NpcPosition(120.5D, 64.0D, 34.5D),
            new NpcRotation(180.0F, 0.0F),
            new ActorMutationId("story:scene-a-spawn-sasha")
    ));
} else {
    npc.placeAt(new NpcPlaceRequest(
            actorId,
            new NpcWorldId("minecraft:overworld"),
            new NpcPosition(120.5D, 64.0D, 34.5D),
            new NpcRotation(180.0F, 0.0F)
    ));
}
```

Когда использовать:

```text
Перед dialogue scene, где NPC должен стоять в конкретной точке.
```

## 42. Рецепт: диалог зависит от формы

```java
ActorId sasha = new ActorId("jdh:sasha");
boolean adult = npc.isForm(sasha, new NpcFormId("jdh:sasha/adult"));

if (adult) {
    playDialogue("sasha_adult_intro");
} else {
    playDialogue("sasha_child_intro");
}
```

Важное правило:

```text
Dialogue branch может зависеть от формы.
Dialogue speaker identity все равно должен быть ActorId.
```

## 43. Рецепт: portrait из active form

```java
NpcActorView actor = npc.query(new ActorId("jdh:sasha")).actor().orElseThrow();

String portrait = actor.activeFormId()
        .flatMap(npc::form)
        .map(NpcFormDefinition::snapshot)
        .map(NpcFormSnapshot::portraitResourceId)
        .orElse("jdh:sasha/default");
```

Разбор:

```java
actor.activeFormId()
```

Берем текущую форму.

```java
.flatMap(npc::form)
```

Находим authored form definition в catalog.

```java
.map(NpcFormDefinition::snapshot)
```

Получаем public snapshot.

```java
.map(NpcFormSnapshot::portraitResourceId)
```

Берем portrait.

```java
.orElse("jdh:sasha/default")
```

Fallback, если actor старый или форма недоступна.

## 44. Рецепт: camera target на глаза

```java
Optional<NpcBodyAnchorSnapshot> eyes = actor.activeFormId()
        .flatMap(npc::form)
        .map(NpcFormDefinition::snapshot)
        .stream()
        .flatMap(snapshot -> snapshot.anchors().stream())
        .filter(anchor -> anchor.id().value().equals("eyes"))
        .findFirst();
```

Когда использовать:

- close-up в диалоге;
- катсцена с focus на лицо;
- look-at;
- camera rail, где child/adult рост разный.

Fallback:

```java
double y = actor.activeFormId()
        .flatMap(npc::form)
        .map(value -> value.snapshot().dimensions().eyeHeight())
        .orElse(1.62D);
```

## 45. Рецепт: безопасный adult time skip

```java
ActorId sasha = new ActorId("jdh:sasha");
NpcFormId adult = new NpcFormId("jdh:sasha/adult");
NpcWorldId world = new NpcWorldId("minecraft:overworld");
NpcPosition position = new NpcPosition(130.5D, 64.0D, 40.5D);
NpcRotation rotation = new NpcRotation(90.0F, 0.0F);

if (npc.isSleeping(sasha)) {
    npc.wakeUp(new NpcWakeRequest(sasha));
}

NpcFormChangeResult formResult = npc.setForm(new NpcFormChangeRequest(sasha, adult));
if (formResult.status() == NpcFormStatus.SPACE_BLOCKED) {
    npc.placeAt(new NpcPlaceRequest(sasha, world, position, rotation));
    npc.setForm(new NpcFormChangeRequest(sasha, adult));
} else {
    npc.placeAt(new NpcPlaceRequest(sasha, world, position, rotation));
}
```

Когда использовать:

```text
Если сценарий может запускаться из непредсказуемого состояния: NPC мог спать,
стоять в тесном месте или уже быть adult.
```

В реальном production flow лучше ждать завершения операций по `operationId`.

## 46. Рецепт: проверить, что NPC готов к сцене

```java
NpcSceneReadinessView readiness = npc.sceneReadiness(new ActorId("jdh:sasha"));
if (!readiness.materialized()) {
    return;
}
```

Когда использовать:

- перед стартом dialogue camera;
- перед cutscene focus;
- перед action, которому нужна физическая entity;
- перед SleepAt.

Если `materialized=false`:

```text
NPC может быть dormant из-за unloaded chunk.
Scene может подождать, загрузить area или вызвать PlaceAt в загруженной точке.
```

## 47. Рецепт: проверить кровать до SleepAt

Команда:

```text
/echo_npc inspect_bed 100 64 200
```

Java-side логика:

```java
NpcResults.Sleep sleep = npc.sleepAt(new NpcSleepRequest(
        sasha,
        new NpcWorldId("minecraft:overworld"),
        new BlockPosition(100, 64, 200),
        new BlockId("minecraft:red_bed")
));

if (sleep.status() == NpcResults.SleepStatus.EXPECTED_BLOCK_MISMATCH) {
    // Автор поменял кровать в мире или expectedBlock в сценарии неверен.
}
```

Когда использовать:

```text
Когда сцена требует конкретную кровать, а не "любую рядом".
```

## 48. Рецепт: восстановиться после missing definition

Симптом:

```text
/echo_npc blocked
BLOCKED_DEFINITION_MISSING
```

Порядок:

```text
1. Найти actorId: /echo_npc actor <id>
2. Посмотреть definitionId в выводе.
3. Вернуть .npc.es с таким id.
4. Выполнить /reload.
5. Проверить /echo_npc blocked.
6. Если actor все еще blocked, смотреть /echo_npc diagnostics 20.
```

Не исправляй это созданием нового ActorId. Это скроет проблему и создаст
расхождение в story state.

## 49. Рецепт: восстановиться после missing form

Симптом:

```text
NPC_FORM_NOT_FOUND
```

Порядок:

```text
1. /echo_npc actor jdh:sasha
2. Посмотреть activeFormId.
3. /echo_npc forms list
4. Если activeFormId отсутствует в списке, вернуть .form.es с этим id.
5. /reload
6. /echo_npc form get jdh:sasha
```

Если форму действительно переименовали:

```text
1. Временно вернуть старую форму.
2. Сделать /reload.
3. Выполнить /echo_npc form set jdh:sasha <newFormId>.
4. Убедиться, что activeFormId новый.
5. Только потом удалить старую форму.
```

## 50. Рецепт: временно убрать NPC со сцены

Если NPC должен исчезнуть навсегда:

```text
/echo_npc despawn jdh:sasha
```

Если NPC должен просто уйти в другую точку:

```text
/echo_npc place_at jdh:sasha 1000.5 64 1000.5 0 0
```

Если NPC должен быть невидим из-за unloaded area:

```text
Не делай ничего. Durable actor может стать dormant.
```

Когда использовать despawn:

- персонаж удален из мира по сюжету;
- временный статист больше не нужен;
- actor record должен стать absent/tombstone.

Когда не использовать despawn:

- для time skip;
- для переодевания;
- для перемещения между сценами;
- для сна;
- для временного chunk unload.

## 51. Ошибки identity: как их распознать

Плохой признак:

```text
В коде есть строки jdh:sasha_child и jdh:sasha_adult как ActorId.
```

Правильно:

```text
ActorId: jdh:sasha
NpcFormId: jdh:sasha/child
NpcFormId: jdh:sasha/adult
```

Плохой признак:

```java
new ActorId(entity.getUUID().toString())
```

Почему плохо:

```text
Minecraft UUID не стабилен как gameplay identity Echo NPC.
```

Плохой признак:

```java
if (actor.activeFormId().orElseThrow().value().contains("adult")) {
```

Лучше:

```java
actor.activeFormId().map(new NpcFormId("jdh:sasha/adult")::equals).orElse(false);
```

Почему лучше: exact id не ломается от случайного совпадения строки.

## 52. Ошибки authoring: как их ловить

### Неверный namespace id

Плохо:

```echoscript
id: "Sasha"
```

Правильно:

```echoscript
id: "jdh:sasha"
```

Плохо:

```echoscript
id: "jdh:Sasha"
```

Правильно:

```echoscript
id: "jdh:sasha"
```

ID должен быть lowercase и namespaced.

### Default form указывает в пустоту

Плохо:

```echoscript
defaultForm: DefaultForm("jdh:sasha/kid")
```

Если формы `jdh:sasha/kid` нет, reload должен дать diagnostic.

Правильно:

```echoscript
defaultForm: DefaultForm("jdh:sasha/child")
```

### Eye height выше height

Плохо:

```echoscript
dimensions: Dimensions(width: 0.42, height: 1.22, eyeHeight: 1.40)
```

Правильно:

```echoscript
dimensions: Dimensions(width: 0.42, height: 1.22, eyeHeight: 1.06)
```

### Слишком большой transform

Плохо:

```echoscript
Part(id: "head", scale: 10.0)
```

`NpcBodyPartTransform` ограничивает scale диапазоном `0.05..4.0`.

## 53. Как читать diagnostics

Diagnostics нужны не только при падении. Это основной инструмент authoring.

Команды:

```text
/echo_npc diagnostics
/echo_npc diagnostics 20
```

Что искать:

| Код | Значение |
| --- | --- |
| `NPC_FORM_NOT_FOUND` | Форма отсутствует. |
| `NPC_FORM_ID_CONFLICT` | Две формы с одним id. |
| `NPC_FORM_SPACE_BLOCKED` | Форма не помещается. |
| `NPC_SLEEP_BED_NOT_FOUND` | Кровать не найдена. |
| `NPC_SLEEP_EXPECTED_BLOCK_MISMATCH` | Не тот block id. |
| `ENPC-PERSISTENCE-ROOT` | Persistence root инициализирован. |
| `ENPC-PERSISTENCE-QUEUE-FULL` | Очередь persistence переполнена. |
| `ENPC-ACTOR-ENTITY-MISSING` | Runtime ожидал entity, но ее нет. |

Как действовать:

```text
1. Сначала смотри самый ранний error после /reload.
2. Потом warning по actor/form.
3. Потом operations.
4. Потом Forge log, если platform failure.
```

## 54. Как читать operations

Команда:

```text
/echo_npc operations
```

Пример kinds:

```text
SPAWN
TELEPORT
DESPAWN
SLEEP
WAKE
FORM_CHANGE
PLACE
RECOVERY_RETRY
```

Если operation stuck в `QUEUED`:

```text
Проверь persistence queue и diagnostics.
```

Если operation `FAILED`:

```text
Смотри message и Forge log.
```

Если operation `SUCCEEDED`, но actor не там:

```text
Проверь actor view, materializedPosition и activeForm.
```

## 55. Как выбирать размеры child/adult

Базовые ориентиры:

| Тип | width | height | eyeHeight | bodyScale |
| --- | --- | --- | --- | --- |
| Ребенок | `0.40..0.46` | `1.15..1.30` | `1.00..1.12` | `0.70..0.80` |
| Подросток | `0.50..0.56` | `1.45..1.65` | `1.30..1.50` | `0.82..0.92` |
| Взрослый | `0.60` | `1.80` | `1.62` | `1.0` |
| Высокий взрослый | `0.62..0.70` | `1.90..2.10` | `1.72..1.92` | `1.05..1.18` |

Когда менять `dimensions`:

```text
Если NPC должен реально помещаться или не помещаться в место.
```

Когда менять `bodyScale`:

```text
Если нужно визуально уменьшить/увеличить модель без другого gameplay collision.
```

Когда менять `parts`:

```text
Если нужен характерный силуэт: крупная голова ребенка, тонкие руки, другие пропорции.
```

## 56. Как выбирать anchors

Минимум:

```echoscript
anchors: [
    Anchor(id: "feet", x: 0, y: 0, z: 0),
    Anchor(id: "eyes", x: 0, y: 1.06, z: 0)
]
```

Лучше для сцен:

```echoscript
anchors: [
    Anchor(id: "feet", x: 0, y: 0, z: 0),
    Anchor(id: "chest", x: 0, y: 0.76, z: 0),
    Anchor(id: "head", x: 0, y: 1.14, z: 0),
    Anchor(id: "eyes", x: 0, y: 1.06, z: 0)
]
```

Когда нужен `feet`:

- spawn marker;
- ground alignment;
- debug render.

Когда нужен `chest`:

- medium shot;
- interaction target;
- body-facing camera.

Когда нужен `head`:

- look-at head;
- dialogue bubble;
- overhead marker.

Когда нужен `eyes`:

- close-up;
- first-person line;
- emotional dialogue camera.

## 57. Как выбирать portrait и faceProfile

`portrait` менять, когда:

- форма меняет возраст;
- персонаж переодет;
- есть отдельные emotional portraits;
- UI должен показать другой visual identity.

`faceProfile` менять, когда:

- другое лицо;
- другой набор facial animations;
- ребенок и взрослый используют разные face bones/offsets;
- dialogue expression system должен выбрать другой профиль.

Пример:

```echoscript
faceProfile: "jdh:sasha/adult_face",
portrait: "jdh:sasha/adult",
```

Не делай:

```text
portrait: "adult"
```

Лучше:

```text
portrait: "jdh:sasha/adult"
```

Потому что namespaced-like строка меньше конфликтует между content packs.

## 58. Как выбирать animationGraph

`animationGraph` должен отвечать на вопрос:

```text
Как эта форма двигается?
```

Примеры:

```echoscript
animationGraph: "jdh:sasha/child_locomotion"
animationGraph: "jdh:sasha/adult_locomotion"
animationGraph: "jdh:guard/heavy_armor_locomotion"
animationGraph: "jdh:old_man/slow_walk"
```

Когда можно оставить один graph:

```text
Если child/adult отличаются только skin, а пропорции и движение одинаковые.
```

Когда нужен отдельный graph:

```text
Если stride, arms, head bob или idle отличаются.
```

## 59. Что делать, если нужно переодевание, а не возраст

Используй формы:

```text
jdh:sasha/pajamas
jdh:sasha/clothes
jdh:sasha/school
jdh:sasha/winter
```

Каждая одежда - отдельный `NpcFormId`, но это всё ещё один персонаж с одним
`ActorId`. Для одежды обычно не нужна отдельная ходьба и отдельное лицо:
`animationGraph`, `faceProfile` и `portrait` можно не писать. Runtime поставит
общую ходьбу `echo_npc:humanoid_locomotion`, общее лицо
`echo_npc:humanoid_face`, а `portrait` возьмет из `Skin(...)`.

Полный файл:

```echoscript
namespace JDH.Npcs.Sasha.Forms;

class SashaPajamasForm : NpcFormResource
{
    public override NpcFormDef Build()
    {
        return Form(
            id: "jdh:sasha/pajamas",
            skin: Skin("jdh:npc/sasha/pajamas"),
            model: "echo_npc:humanoid",
            dimensions: Dimensions(width: 0.60, height: 1.80, eyeHeight: 1.62),
            bodyScale: 1.0,
            tags: ["sasha", "adult", "pajamas", "wardrobe"]
        );
    }
}
```

Строки отдельно:

```echoscript
id: "jdh:sasha/pajamas",
```

ID формы. По нему форма выбирается через `/echo_npc form set`.

```echoscript
skin: Skin("jdh:npc/sasha/pajamas"),
```

ID скина. Для него файл должен лежать здесь:

```text
assets/jdh/textures/entity/npc/sasha/pajamas.png
```

```echoscript
dimensions: Dimensions(width: 0.60, height: 1.80, eyeHeight: 1.62),
```

Размеры тела остаются взрослыми, потому что одежда не меняет возраст и collision.

```echoscript
tags: ["sasha", "adult", "pajamas", "wardrobe"]
```

Теги нужны для поиска и диагностики. На личность они не влияют.

Переключение:

```mcfunction
/echo_npc form set jdh:sasha jdh:sasha/pajamas
/echo_npc form set jdh:sasha jdh:sasha/clothes
```

ActorId все равно:

```text
jdh:sasha
```

F3+T не нужен, если ты переключаешься на другой `Skin(...)`, чей PNG уже лежит
в активном resource pack. Если ты во время игры перезаписал тот же самый PNG-файл
на диске, Minecraft resource manager всё равно увидит новый файл только после
resource reload.

## 60. Что делать, если нужно временное disguise

Если disguise не должен менять личность:

```text
SetForm(jdh:sasha, jdh:sasha/disguise)
```

Если disguise должен обманывать gameplay systems:

```text
ActorId все равно jdh:sasha, но story/dialogue predicates могут смотреть activeFormId.
```

Если это магическая копия или двойник:

```text
Создай отдельный ActorId, например jdh:sasha_double.
```

## 61. Что делать, если NPC должен умереть или исчезнуть

Если персонаж больше не должен существовать:

```text
/echo_npc despawn jdh:sasha
```

Если персонаж ушел за кадр:

```text
/echo_npc place_at jdh:sasha <offscreen-position> <yaw> <pitch>
```

Если персонаж стал недоступен в сюжете:

```text
Храни это в story flags, не в ActorId.
```

Не переименовывай actor:

```text
jdh:sasha_dead
```

Лучше:

```text
ActorId: jdh:sasha
Story flag: sasha_dead = true
```

## 62. Что делать, если нужен NPC в нескольких местах

Если это один и тот же персонаж:

```text
Нельзя иметь одного ActorId одновременно в двух местах.
```

Варианты:

1. Переставлять через `PlaceAt`.
2. Создать отдельную копию с другим ActorId, если это реально копия.
3. Использовать cutscene illusion, если второй NPC нужен только визуально.

Пример копии:

```text
jdh:sasha
jdh:sasha_memory_echo
```

Документируй в story, что `sasha_memory_echo` не является основной Сашей.

## 63. Что делать, если несколько сцен спорят за NPC

Симптом:

```text
NPC прыгает между местами или form меняется туда-сюда.
```

Причина:

```text
Две системы одновременно вызывают PlaceAt/SetForm.
```

Решение:

- использовать story ownership;
- использовать stable mutation ids;
- перед action проверять current state;
- не запускать две катсцены с одним ActorId одновременно.

Пример проверки:

```java
NpcActorView actor = npc.query(sasha).actor().orElseThrow();
if (actor.status() == NpcActorView.Status.PENDING_MOVE) {
    return;
}
```

## 64. Что должно быть в документации конкретного NPC

Для каждого важного NPC заведи маленькую карточку:

```md
# jdh:sasha

ActorId: `jdh:sasha`
Definition: `jdh:sasha`
Default form: `jdh:sasha/child`

Forms:
- `jdh:sasha/child`: используется в прологе до time skip.
- `jdh:sasha/adult`: используется после Chapter 2.
- `jdh:sasha/winter`: используется в зимней сцене.

Dialogue speaker: `jdh:sasha`
Main portrait source: active form.
Main animation graph source: active form.

Important scenes:
- Prologue bedroom: spawn child, sleep_at bed.
- Time skip: wake, set adult, place at station.
- Finale: adult form, festival portrait.
```

Зачем:

```text
Чтобы сценаристы не создавали случайные новые ActorId и не ломали continuity.
```

## 65. Полная карта “что смотреть при проблеме”

| Проблема | Первая команда | Вторая команда | Вероятная причина |
| --- | --- | --- | --- |
| NPC не появился | `/echo_npc actor <id>` | `/echo_npc diagnostics 20` | spawn queued/failed, missing definition |
| NPC не в той форме | `/echo_npc form get <id>` | `/echo_npc forms inspect <form>` | activeForm не менялся или reload missing form |
| NPC не помещается | `/echo_npc form validate_space <id> <form>` | `/echo_npc actor <id>` | dimensions/collision |
| Диалог показывает старый портрет | `/echo_npc form get <id>` | `/echo_npc forms inspect <form>` | dialogue adapter не читает active form |
| Анимация не та | `/echo_npc forms inspect <form>` | animation diagnostics | adapter не взял animationGraph |
| Камера смотрит выше/ниже | `/echo_npc forms inspect <form>` | cutscene debug | не используется anchor eyes |
| Сон не работает | `/echo_npc inspect_bed <pos>` | `/echo_npc diagnostics 20` | expectedBlock mismatch/bed occupied |
| После reload actor blocked | `/echo_npc blocked` | `/echo_npc definitions`, `/echo_npc forms list` | missing definition/form |
| После restart NPC исчез | `/echo_npc actor <id>` | `/echo_npc operations` | dormant chunk или recovery issue |

## 66. Минимальные правила для интеграционного кода

1. Всегда lookup через `EchoNpcServiceBridge`.
2. Всегда проверяй `lookup.status()`.
3. Не держи ссылку на Forge entity как source of truth.
4. Не создавай ActorId из UUID entity.
5. Не считай `query.actor().isEmpty()` поводом сразу spawn, если service stopped.
6. Для story retry используй stable mutation id.
7. Для visuals читай active form snapshot.
8. Для camera читай anchors.
9. Для movement не создавай новый ActorId.
10. Для sleeping сначала проверяй materialized/readiness.

Пример defensive query:

```java
NpcResults.Query query = npc.query(actorId);
if (query.status() == NpcResults.QueryStatus.SERVICE_STOPPED) {
    return;
}
if (query.actor().isEmpty()) {
    // Actor действительно не найден или еще не создан.
}
```

## 67. Минимальные правила для контента

1. Все ids lowercase.
2. Все ids namespaced.
3. Формы лежат отдельно от NPC definitions.
4. `defaultForm` должен существовать.
5. Child/adult не являются разными actors.
6. Dimensions должны соответствовать authored сценам.
7. Anchors явно задаются для важных cutscenes.
8. Portrait и animation graph указываются в форме.
9. После каждого изменения ресурсов делай `/reload`.
10. После `/reload` проверяй diagnostics.

## 68. Полный order-of-operations для сцены

Перед сценой:

```text
1. /reload
2. /echo_npc diagnostics 20
3. /echo_npc definitions
4. /echo_npc forms list
```

Старт сцены:

```text
1. Проверить actor.
2. SpawnAt, если actor отсутствует.
3. WakeUp, если actor sleeping.
4. SetForm, если нужна конкретная форма.
5. PlaceAt в marker.
6. Дождаться materialized/readiness.
7. Запустить dialogue/cutscene.
```

Во время сцены:

```text
1. Dialogue читает displayName и portrait.
2. Animation читает graph.
3. Camera читает anchors.
4. Story не меняет ActorId.
```

После сцены:

```text
1. Если NPC должен остаться - ничего не делать или PlaceAt.
2. Если NPC должен лечь - SleepAt.
3. Если NPC должен уйти навсегда - Despawn.
4. Проверить operations/diagnostics при ошибке.
```

## 69. Термины одним списком

`ActorId`:

```text
Кто это в сюжете и сохранении.
```

`NpcDefinitionId`:

```text
Какой authored NPC profile использовался при создании.
```

`NpcFormId`:

```text
Какое тело/внешность сейчас активны.
```

`NpcSkinId`:

```text
Какой texture resource использовать.
```

`NpcWorldId`:

```text
В каком dimension находится desired actor.
```

`NpcPosition`:

```text
Точная позиция NPC в world coordinates.
```

`NpcRotation`:

```text
Yaw/pitch presentation orientation.
```

`BlockPosition`:

```text
Integer block coordinate, например позиция кровати.
```

`BlockId`:

```text
Exact block resource id, например minecraft:red_bed.
```

`NpcRestAttachmentView`:

```text
Public snapshot сна: provider, bed, canonical bed, anchor, pose.
```

`NpcSceneReadinessView`:

```text
Readiness snapshot: можно ли начинать сцену, где нужна physical entity.
```

`NpcFormSnapshot`:

```text
Public snapshot формы: skin, model, dimensions, anchors, animation, face, portrait.
```

`OperationId`:

```text
ID async mutation в Echo NPC operation book.
```

## 70. Финальный большой checklist “буквально все перед релизом”

Контент:

- `.npc.es` существует.
- `.form.es` для каждой формы существует.
- default form существует.
- ids lowercase.
- ids namespaced.
- skin paths существуют.
- child dimensions подходят сценам.
- adult dimensions подходят сценам.
- anchors заданы явно для cutscenes.
- portrait ids согласованы с dialogue content.
- animationGraph ids согласованы с animation content.

Runtime:

- `/reload` без errors.
- `/echo_npc diagnostics 20` чистый или понятный.
- `/echo_npc definitions` содержит NPC.
- `/echo_npc forms list` содержит формы.
- `/echo_npc forms inspect` показывает правильные dimensions.
- `/echo_npc spawn_at` работает.
- `/echo_npc actor` показывает правильный ActorId.
- `/echo_npc form get` показывает ожидаемую форму.
- `/echo_npc form set` меняет форму.
- `/echo_npc place_at` ставит NPC в сцену.
- `/echo_npc sleep_at` работает, если сцена со сном.
- `/echo_npc wake_up` освобождает sleeping state.

Интеграции:

- Dialogue не использует form id как speaker id.
- Dialogue берет portrait из active form.
- Animation не использует form id как subject id.
- Animation берет graph из active form.
- Cutscene берет anchors из active form.
- Navigation subject остается ActorId.
- Story actions используют stable mutation ids.
- Ни один adapter не читает `EchoNpcEntity`, если достаточно public API.

Persistence/restart:

- После restart actor остается.
- После restart active form остается.
- После restart sleeping attachment остается, если NPC спал.
- Missing definition/form не маскируется созданием нового actor.
- Blocked actors проверяются через `/echo_npc blocked`.

## 71. Поисковый FAQ: вопросы и ответы

Этот раздел специально написан вопросами. Если ты ищешь по Ctrl+F, вводи
примерно так, как спросил бы вживую: “как создать NPC”, “почему форма не
меняется”, “когда использовать PlaceAt”, “как сделать взрослую Сашу”.

### 71.1 Как создать NPC?

Создай `.npc.es` в `data/<namespace>/echo_npc/npcs`, сделай `/reload`, затем
вызови `/echo_npc spawn_at <actorId> <pos> <yaw> <pitch>`. Для Саши: `/echo_npc
spawn_at jdh:sasha 120.5 64 34.5 180 0`.

### 71.2 Как проверить, что NPC создан?

Используй `/echo_npc actor <actorId>`. Для Саши: `/echo_npc actor jdh:sasha`.
Если actor найден, смотри `status`, `position`, `activeForm` и revisions.

### 71.3 Как удалить NPC?

Используй `/echo_npc despawn <actorId>`. Это удаляет durable actor или ставит
tombstone. Не используй despawn для переодевания, взросления или перемещения.

### 71.4 Как переместить NPC в сцену?

Используй `/echo_npc place_at <actorId> <x y z> <yaw> <pitch>`. `PlaceAt`
предназначен для authored scene placement и учитывает active form dimensions.

### 71.5 Чем PlaceAt отличается от Teleport?

`Teleport` это debug/низкоуровневое перемещение. `PlaceAt` это сценическая
постановка: она проверяет target position и active form dimensions. Для сцен
используй `PlaceAt`.

### 71.6 Когда использовать SpawnAt?

Используй `SpawnAt`, когда NPC еще нет, но он должен появиться в конкретной
точке сцены. Если actor уже существует, используй `PlaceAt`.

### 71.7 Когда использовать Spawn?

Используй `spawn` для debug или простого создания actor. Для authored сцен
предпочитай `spawn_at`, потому что там явно задаются позиция и поворот.

### 71.8 Когда использовать Teleport?

Используй `teleport`, когда руками тестируешь перемещение. Для story/cutscene
лучше `PlaceAt`, потому что он семантически означает “поставить NPC в сцену”.

### 71.9 Как сделать детскую форму NPC?

Создай `.form.es` с `id: "namespace:npc/child"`, маленькими `dimensions`,
уменьшенным `bodyScale`, отдельными anchors, portrait и animation graph.
Пример: `jdh:sasha/child`.

### 71.10 Как сделать взрослую форму NPC?

Создай `.form.es` с `id: "namespace:npc/adult"`, взрослыми dimensions,
например `width: 0.60`, `height: 1.80`, `eyeHeight: 1.62`, и отдельным
portrait/animation graph.

### 71.11 Как сменить детскую форму на взрослую?

Используй `/echo_npc form set <actorId> <formId>`, например `/echo_npc form set
jdh:sasha jdh:sasha/adult`. Для time skip делай это под черным экраном, затем
вызывай `place_at`.

### 71.12 Почему нельзя создать SashaChild и SashaAdult как двух NPC?

Потому что это будут два разных `ActorId`. Диалоги, story flags, persistence,
катсцены и memories будут считать их разными персонажами. Правильно: один
`ActorId` и несколько `NpcFormId`.

### 71.13 Что такое ActorId?

`ActorId` это долговечная личность NPC в сюжете и сохранении. Пример:
`jdh:sasha`. Он не должен зависеть от Minecraft entity UUID.

### 71.14 Что такое NpcDefinitionId?

`NpcDefinitionId` это authored definition из `.npc.es`. Он отвечает на вопрос:
“какой NPC profile использовать при создании actor”.

### 71.15 Что такое NpcFormId?

`NpcFormId` это authored форма тела и внешности. Пример: `jdh:sasha/child` или
`jdh:sasha/adult`. Это не новая личность.

### 71.16 Что такое NpcFormSnapshot?

`NpcFormSnapshot` это public снимок формы: skin, model, dimensions,
bodyGeometry, anchors, animationGraph, faceProfile, portrait и fingerprint.

### 71.17 Как узнать текущую форму NPC?

Команда: `/echo_npc form get <actorId>`. Java API: `npc.getForm(actorId)` или
`npc.query(actorId).actor().flatMap(NpcActorView::activeFormId)`.

### 71.18 Как проверить, что NPC сейчас взрослый?

Проверь exact form id: `npc.isForm(new ActorId("jdh:sasha"), new
NpcFormId("jdh:sasha/adult"))`. Не проверяй через `contains("adult")`.

### 71.19 Как вывести список всех форм?

Используй `/echo_npc forms list`. Для Java API используй `npc.formCatalog(200)`.

### 71.20 Как посмотреть все данные формы?

Используй `/echo_npc forms inspect <formId>`. Там будут source, revision, skin,
model, bodyScale, parts, dimensions, anchors, animationGraph, faceProfile,
portrait и fingerprint.

### 71.21 Почему форма не меняется?

Проверь `/echo_npc diagnostics 20`, `/echo_npc form get <actorId>`, `/echo_npc
forms list`. Частые причины: `FORM_NOT_FOUND`, NPC спит, NPC движется, место
заблокировано, операция еще `QUEUED`.

### 71.22 Почему SetForm возвращает SPACE_BLOCKED?

Новая форма не помещается в текущей позиции. Проверь `/echo_npc form
validate_space <actorId> <formId>`. Решение: сначала `place_at` в свободную
точку или измени `dimensions`.

### 71.23 Почему SetForm возвращает WHILE_SLEEPING?

NPC спит и имеет rest attachment. Сначала вызови `/echo_npc wake_up <actorId>`,
после этого меняй форму.

### 71.24 Почему SetForm возвращает FORM_NOT_FOUND?

Форма отсутствует в catalog. Проверь путь `.form.es`, `id` внутри файла, затем
сделай `/reload` и `/echo_npc forms list`.

### 71.25 Почему SetForm возвращает ALREADY_ACTIVE?

Это не ошибка. Target form уже активна, runtime ничего не меняет.

### 71.26 Как проверить, помещается ли взрослая форма?

Используй `/echo_npc form validate_space <actorId> <formId>`, например
`/echo_npc form validate_space jdh:sasha jdh:sasha/adult`.

### 71.27 Как сделать time skip без видимой подмены?

Закрой кадр, вызови `WakeUp` если NPC спит, затем `SetForm`, затем `PlaceAt`,
затем открой кадр. Не делай despawn/spawn другого actor.

### 71.28 Как сделать переодевание NPC?

Создай отдельные forms: `jdh:sasha/casual`, `jdh:sasha/winter`,
`jdh:sasha/festival`. Потом меняй через `SetForm`. `ActorId` остается
`jdh:sasha`.

Для обычной смены одежды достаточно менять только `id`, `skin` и `tags`.
`animationGraph`, `faceProfile` и `portrait` можно не писать, если ходьба и лицо
остаются теми же. Пример runtime-команды:

```mcfunction
/echo_npc form set jdh:sasha jdh:sasha/pajamas
/echo_npc form set jdh:sasha jdh:sasha/clothes
```

### 71.29 Как сделать disguise или маскировку?

Если это тот же персонаж, используй новую form, например `jdh:sasha/disguise`.
Если это отдельный двойник по сюжету, тогда нужен отдельный `ActorId`.

### 71.30 Как сделать NPC ниже или выше?

Меняй `dimensions.height`, `dimensions.eyeHeight` и `bodyScale` в `.form.es`.
Помни: `dimensions` влияет на серверную collision box, `bodyScale` на визуал.

### 71.31 Как сделать ребенку большую голову?

В `.form.es` добавь part transform: `Part(id: "head", scale: 1.18, pivotY:
-0.08)`. Это визуальный transform головы.

### 71.32 Какие body parts поддерживаются?

Поддерживаются semantic parts: `head`, `body`, `left_arm`, `right_arm`,
`left_leg`, `right_leg`. В Java enum это `HEAD`, `BODY`, `LEFT_ARM`,
`RIGHT_ARM`, `LEFT_LEG`, `RIGHT_LEG`.

### 71.33 Что такое dimensions?

`dimensions` это серверные размеры формы: `width`, `height`, `eyeHeight`.
Runtime использует их для collision, eye height и проверки места.

### 71.34 Что такое bodyScale?

`bodyScale` это глобальный визуальный масштаб тела. Он не заменяет
`dimensions`. Для физики и проверки места нужны `dimensions`.

### 71.35 Что такое anchors?

Anchors это точки тела: `feet`, `chest`, `head`, `eyes`. Их используют camera,
cutscene, dialogue framing, look-at и debug.

### 71.36 Как сделать камеру на глаза NPC?

Читай active form snapshot и anchor `eyes`. Для child форма может иметь `eyes`
на `y=1.06`, adult на `y=1.62`.

### 71.37 Что такое portrait в форме?

`portrait` это resource id портрета для dialogue/UI. У child и adult формы
может быть разный portrait.

### 71.38 Что такое faceProfile?

`faceProfile` выбирает профиль лица для dialogue/facial animation. Его нужно
читать из active form snapshot.

### 71.39 Что такое animationGraph?

`animationGraph` выбирает граф анимаций для текущей формы. Child/adult могут
иметь разные locomotion graphs.

### 71.40 Почему диалог показывает старый портрет?

Скорее всего dialogue adapter читает `NpcActorView.skinId()` или authored
fallback, а не active form snapshot `portraitResourceId()`.

### 71.41 Почему анимация не меняется после SetForm?

Animation adapter должен после смены формы читать active form snapshot и брать
`animationGraphId()`. Если он продолжает использовать старый graph, проверь
adapter diagnostics.

### 71.42 Почему камера смотрит выше головы ребенка?

Камера использует fixed offset вместо anchor `eyes` из active form. Нужно брать
anchors из `NpcFormSnapshot`.

### 71.43 Как уложить NPC спать?

Используй `/echo_npc sleep_at <actorId> <bedPos> <expectedBlock>`, например
`/echo_npc sleep_at jdh:sasha 100 64 200 minecraft:red_bed`.

### 71.44 Как разбудить NPC?

Используй `/echo_npc wake_up <actorId>`, например `/echo_npc wake_up
jdh:sasha`.

### 71.45 Как проверить, спит ли NPC?

Команда: `/echo_npc inspect_rest <actorId>`. Java API: `npc.isSleeping(actorId)`
или `npc.inspectRest(actorId)`.

### 71.46 Почему SleepAt не работает?

Проверь `/echo_npc inspect_bed <pos>` и `/echo_npc diagnostics 20`. Частые
причины: кровать отсутствует, chunk не загружен, block id не совпал, bed
occupied, NPC не materialized или слишком далеко.

### 71.47 Что значит EXPECTED_BLOCK_MISMATCH?

В `SleepAt` указан один `BlockId`, а в мире стоит другой блок. Исправь
`expectedBlock` или поставь правильную кровать.

### 71.48 Что значит BED_OCCUPIED?

Кровать занята. Нужно освободить кровать, выбрать другую или вызвать wake для
NPC, который уже занимает эту кровать.

### 71.49 Что значит NOT_MATERIALIZED при SleepAt?

Durable actor есть, но физической entity сейчас нет в загруженном мире. Сначала
сделай `PlaceAt`/загрузи chunk и дождись materialized.

### 71.50 Как понять, готов ли NPC к сцене?

Используй `npc.sceneReadiness(actorId)` или командно проверяй `/echo_npc actor
<actorId>`. Для cutscene/dialogue нужен `MATERIALIZED` или допустимый scene
ready status.

### 71.51 Что делать, если NPC dormant?

`DORMANT_CHUNK_UNLOADED` значит actor сохранен, но chunk не загружен. Это не
ошибка. Загрузи area или используй `PlaceAt` в загруженную сцену.

### 71.52 Почему NPC исчез после рестарта?

Сначала проверь `/echo_npc actor <actorId>`. Если actor dormant, это нормально.
Если blocked, смотри `/echo_npc blocked` и `/echo_npc diagnostics 20`.

### 71.53 Почему actor blocked после reload?

Обычно missing definition, missing form или corrupt record. Проверь
`/echo_npc definitions`, `/echo_npc forms list`, `/echo_npc blocked`.

### 71.54 Что делать при BLOCKED_DEFINITION_MISSING?

Верни `.npc.es` с тем же `NpcDefinitionId`, сделай `/reload`, проверь
`/echo_npc blocked`. Не создавай новый actor.

### 71.55 Что делать при NPC_FORM_NOT_FOUND после переименования формы?

Временно верни старую форму, сделай `/reload`, переведи actor на новую форму
через `SetForm`, только потом удаляй старую.

### 71.56 Где хранятся сохранения NPC?

Actor state хранится через `npc-persistence` в world-scoped repository. Внутри
сохранения есть `actorId`, desired position, revisions, rest attachment и
`activeForm` для schema v3.

### 71.57 Что сохраняется при active form?

Сохраняется `formId`, `revision`, `fingerprint`. Полное описание формы не
дублируется в save file, оно берется из catalog `.form.es`.

### 71.58 Почему defaultForm не применился к старому NPC?

`defaultForm` применяется при новом spawn. Уже сохраненный actor хранит свой
`activeForm` в persistence. Для старого NPC вызови `SetForm`.

### 71.59 Что такое actorRevision?

`actorRevision` это ревизия логического состояния actor. Она растет при
изменениях: form, place, sleep, wake, despawn, binding.

### 71.60 Что такое repositoryRevision?

`repositoryRevision` это ревизия записи в repository. Она нужна для
save/update conflict control и восстановления.

### 71.61 Что такое operationId?

`operationId` это id async mutation в operation book. По нему можно проверить
статус через `/echo_npc operation <operationId>`.

### 71.62 Что значит операция QUEUED?

Операция принята в очередь durable write. Видимое состояние может примениться
после успешной записи.

### 71.63 Что значит операция RUNNING?

Операция выполняется. Нужно дождаться `SUCCEEDED`, `FAILED` или проверить actor
revision/status.

### 71.64 Что значит операция SUCCEEDED?

Durable operation завершилась успешно. Проверь actor view, если нужно
подтвердить конкретную форму или позицию.

### 71.65 Что значит операция FAILED?

Операция была принята, но упала. Смотри message, diagnostics и Forge log.

### 71.66 Что значит operation REJECTED?

Runtime отклонил действие до нормального выполнения: invalid request, missing
actor, missing form, service stopped, collision и т.д.

### 71.67 Зачем нужен ActorMutationId?

Для идемпотентности spawn/teleport/despawn. Если story action повторится, stable
mutation id не создаст вторую операцию.

### 71.68 Почему нельзя использовать random UUID как mutation id?

Потому что каждый retry станет новой операцией. Mutation id должен быть
стабильным: например `story:prologue-spawn-sasha`.

### 71.69 Как получить NpcService в другом моде?

Используй `EchoNpcServiceBridge.lookup(server)`. Если статус `RESOLVED`, бери
`lookup.service()`. Если `UNAVAILABLE` или `DUPLICATE_PROVIDER`, не вызывай API.

### 71.70 Почему нельзя хранить EchoNpcEntity в интеграции?

Потому что public contract должен идти через `NpcService` и snapshots. Entity
может быть пересоздана, отсутствовать при chunk unload или измениться внутри
Forge runtime.

### 71.71 Как dialogue adapter должен работать с NPC?

Он должен использовать `ActorId` как speaker identity, `displayName()` как имя,
а portrait/faceProfile брать из active form snapshot, если он есть.

### 71.72 Как animation adapter должен работать с NPC?

Он должен использовать `ActorId` как animation subject identity, а
`animationGraphId()` и body geometry брать из active form snapshot.

### 71.73 Как cutscene adapter должен работать с NPC?

Он должен использовать anchors active form: `eyes`, `head`, `chest`, `feet`.
Особенно важно для child/adult, потому что рост разный.

### 71.74 Как navigation adapter должен работать с NPC?

Navigation subject должен оставаться основанным на `ActorId`. Смена формы не
создает новый navigation subject.

### 71.75 Как понять, какой skin используется?

У definition есть base `skinId()`, но active form может иметь свой skin. Для
точного визуала смотри `/echo_npc forms inspect <formId>` или
`NpcFormSnapshot.skinId()`.

### 71.76 Почему скин не поменялся после формы?

Проверь, есть ли skin в `.form.es`, загрузился ли asset path, и активна ли
форма. Команды: `/echo_npc form get <actorId>`, `/echo_npc forms inspect
<formId>`.

Без F3+T работает смена на другой `Skin(...)`, например с
`jdh:npc/sasha/pajamas` на `jdh:npc/sasha/clothes`, если оба PNG уже есть в
resource pack. Если ты заменил содержимое того же файла
`assets/jdh/textures/entity/npc/sasha/pajamas.png`, Minecraft увидит новое
содержимое только после resource reload.

### 71.77 Где должен лежать skin?

Новый путь: `assets/<namespace>/textures/entity/<path>.png`. Старый fallback:
`assets/<namespace>/textures/echo_npc/skins/<path>.png`.

### 71.78 Как написать путь для Skin("jdh:npc/sasha/adult")?

Ищи texture как `assets/jdh/textures/entity/npc/sasha/adult.png`, если
используется новый путь.

### 71.79 Как сделать NPC без формы?

Можно создать `.npc.es` без `defaultForm`. Тогда actor будет иметь base skin и
empty `activeFormId`. Но для child/adult и новых visuals лучше использовать
forms.

### 71.80 Как сделать NPC без скина?

В `.npc.es` можно не указывать `skin`, если downstream rendering имеет fallback.
Но для production NPC лучше задавать skin или form skin.

### 71.81 Как сделать только одну форму?

Создай одну `.form.es`, например `jdh:guard/default`, и укажи ее в
`defaultForm`. Даже одна форма полезна для dimensions, anchors и portrait.

### 71.82 Как сделать много возрастов?

Используй несколько forms: `jdh:sasha/child`, `jdh:sasha/teen`,
`jdh:sasha/adult`, `jdh:sasha/old`. `ActorId` все равно один: `jdh:sasha`.

### 71.83 Как сделать NPC для массовки?

Если у каждого статиста нет индивидуальной памяти, можно создавать отдельные
ActorId вроде `jdh:market_extra_01`. Если это один персонаж в разных формах,
используй forms.

### 71.84 Как сделать временную копию NPC?

Если копия реально отдельная сущность по сюжету, создай отдельный `ActorId`,
например `jdh:sasha_memory_echo`. Если это просто внешний вид, используй form.

### 71.85 Как проверить, что reload увидел новую форму?

После `/reload` выполни `/echo_npc forms list`. Если формы нет, смотри
`/echo_npc diagnostics 20`.

### 71.86 Как проверить, из какого файла пришла форма?

Используй `/echo_npc forms inspect <formId>`. В выводе есть `source`.

### 71.87 Что значит fingerprint формы?

Fingerprint это SHA-256 semantic snapshot формы. Он помогает понимать, какая
authored версия была закреплена в actor state.

### 71.88 Нужно ли вручную редактировать save file?

Нет, почти никогда. Используй команды и API. Ручное изменение save file может
сломать revisions/fingerprint.

### 71.89 Как исправить неправильную active form у сохраненного NPC?

Лучше через `/echo_npc form set <actorId> <formId>`. Если actor blocked из-за
missing old form, временно верни old form и потом штатно переведи на new form.

### 71.90 Как понять, NPC активен или отсутствует?

Смотри `NpcActorView.desiredPresence()`. `ACTIVE` означает actor должен
существовать, `ABSENT` означает durable tombstone/удаление.

### 71.91 Как понять, entity физически есть?

Смотри `status`: `MATERIALIZED` или `NPC_SLEEPING` означает физическое
присутствие. `DORMANT_CHUNK_UNLOADED` означает durable actor без загруженной
entity.

### 71.92 Как сделать NPC сидящим?

В текущем описанном контракте есть sleep/rest как durable pose. Для seated pose
нужен отдельный integration/runtime contract; не подменяй это формой, если
нужна настоящая интеракция с сидением.

### 71.93 Как сделать NPC лежащим?

Используй `SleepAt` для кровати. Это durable rest attachment с provider,
canonical bed, sleep anchor и occupied state.

### 71.94 Как сделать NPC невидимым?

Если это временно для сцены, лучше управлять сценой/cutscene или переместить
NPC. Если персонаж реально исчезает, используй `despawn`. Не создавай fake
форму “invisible”, если это ломает gameplay.

### 71.95 Как сделать NPC в другом измерении?

Используй `NpcWorldId`, например `minecraft:the_nether`, и `PlaceAt` или
`SpawnAt` в нужном world. Если world не найден, будет `WORLD_NOT_FOUND` или
`TARGET_INVALID`.

### 71.96 Как понять, почему команда ничего не сделала?

Смотри ответ команды, затем `/echo_npc operations`, затем `/echo_npc diagnostics
20`, затем `/echo_npc actor <actorId>`.

### 71.97 Как проверить все заблокированные NPC?

Используй `/echo_npc blocked`. Потом для каждого actor используй `/echo_npc
actor <actorId>` и diagnostics.

### 71.98 Как retry recovery?

Команда: `/echo_npc recovery retry <actorId>`. Используй, когда actor blocked
или materialization failure исправлен, и нужно повторить восстановление.

### 71.99 Как проверить persistence status?

Используй `/echo_npc persistence status`. Это полезно при подозрении на очередь,
repository или degraded mode.

### 71.100 Какие команды самые важные для ежедневной работы?

Самые важные: `/reload`, `/echo_npc diagnostics 20`, `/echo_npc definitions`,
`/echo_npc forms list`, `/echo_npc actor <id>`, `/echo_npc form get <id>`,
`/echo_npc form set <id> <form>`, `/echo_npc place_at ...`.

### 71.101 Что делать, если я не помню точный id NPC?

Сначала смотри `/echo_npc actors` для actors и `/echo_npc definitions` для
definitions. Для forms смотри `/echo_npc forms list`.

### 71.102 Что делать, если я не помню точный id формы?

Используй `/echo_npc forms list`, потом `/echo_npc forms inspect <formId>`.
Ищи по tags, skin, dimensions, source.

### 71.103 Как найти, где описана Саша?

Ищи `jdh:sasha` в ресурсах. Definition обычно в
`data/jdh/echo_npc/npcs/sasha.npc.es`, формы в
`data/jdh/echo_npc/forms/sasha/*.form.es`.

### 71.104 Что значит defaultForm?

`defaultForm` это форма, которая применяется при новом spawn. Это не forced
форма для всех существующих actors.

### 71.105 Можно ли поменять defaultForm и обновить всех NPC?

Нет автоматически. Для уже сохраненных actors используй `SetForm`. Это сделано,
чтобы reload не менял персонажей неожиданно.

### 71.106 Что значит activeFormFingerprint?

Это fingerprint формы, закрепленный в actor state. Он нужен для диагностики и
совместимости authored snapshot.

### 71.107 Как понять, что форма изменилась в файле?

После `/reload` у формы будет новый fingerprint. Сравни вывод `/echo_npc forms
inspect <formId>` до и после изменения.

### 71.108 Как сделать safe migration формы?

Добавь новую форму, сделай `/reload`, переведи actors через `SetForm`, проверь
active form, потом удаляй старую форму только если она больше не нужна.

### 71.109 Как проверить, что story не создает дублей?

Проверь `/echo_npc actors` и stable mutation ids. Если один сюжетный персонаж
имеет несколько похожих ActorId, это ошибка моделирования.

### 71.110 Как назвать ActorId?

Используй стабильное смысловое имя: `jdh:sasha`, `jdh:john_father`,
`jdh:market_vendor_01`. Не добавляй состояние в ActorId, например `_adult`,
`_sleeping`, `_angry`.

### 71.111 Как назвать NpcFormId?

Используй путь состояния внешности: `jdh:sasha/child`, `jdh:sasha/adult`,
`jdh:sasha/winter`, `jdh:guard/heavy_armor`.

### 71.112 Как назвать animationGraph?

Используй resource-like id: `jdh:sasha/child_locomotion`,
`jdh:sasha/adult_locomotion`, `jdh:old_man/slow_walk`.

### 71.113 Как назвать portrait?

Используй stable id, связанный с формой: `jdh:sasha/child`,
`jdh:sasha/adult`, `jdh:sasha/winter`.

### 71.114 Как понять, какие строки кода использовать в Java?

Для чтения: `npc.query(actorId)`. Для создания: `npc.spawn(new
SpawnNpcRequest(...))`. Для формы: `npc.setForm(new NpcFormChangeRequest(...))`.
Для сцены: `npc.placeAt(new NpcPlaceRequest(...))`.

### 71.115 Какой самый короткий Java код смены формы?

`npc.setForm(new NpcFormChangeRequest(new ActorId("jdh:sasha"), new
NpcFormId("jdh:sasha/adult")));`

### 71.116 Какой самый короткий Java код проверки формы?

`npc.isForm(new ActorId("jdh:sasha"), new NpcFormId("jdh:sasha/adult"));`

### 71.117 Какой самый короткий Java код постановки NPC?

`npc.placeAt(new NpcPlaceRequest(actorId, worldId, position, rotation));`

### 71.118 Какой самый короткий Java код сна?

`npc.sleepAt(new NpcSleepRequest(actorId, worldId, bedPosition, expectedBlock));`

### 71.119 Что делать, если NPC нужен только для катсцены?

Все равно используй `ActorId`, `.npc.es`, `spawn_at` и `place_at`. Если NPC
одноразовый, после сцены можно `despawn`. Если персонаж сюжетный, не despawn
без причины.

### 71.120 Что делать, если NPC нужен только для диалога?

Если нужен physical speaker в мире, используй Echo NPC actor. Если диалог
абстрактный и NPC не должен быть в мире, dialogue system может использовать
authored fallback, но это уже не physical Echo NPC.
