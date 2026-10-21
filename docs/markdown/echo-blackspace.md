# Echo Blackspace

Документ только про `echo-blackspace`.

Здесь описано, где можно использовать Blackspace DSL, какие action id реально есть, как работает создание площадки,
какие классы за что отвечают и какой код писать в сюжетах.

## Главное

`echo-blackspace` отвечает не только за визуальный режим. Сейчас модуль делает полный цикл:

1. Создает Blackspace в текущем измерении игрока.
2. Выбирает координаты далеко от основной карты.
3. Загружает чанки под площадку.
4. Ставит barrier-платформу.
5. Возвращает готовый `BlackspaceInstance` только после завершения создания.
6. Дает Story/EchoScript API для создания и телепорта.
7. Автоматически включает черный visual mode, когда игрок попадает в Blackspace.
8. Автоматически выключает visual mode, когда игрок выходит из Blackspace.

Главная авторская форма:

```csharp
private void OnStart()
{
    var blackspace = await Blackspace.Create("jdh:first_contact");
    Player.Teleport(blackspace.Spawn);
}
```

Это не JavaScript `async/await`. В текущей реализации `await` в такой записи является синтаксическим маркером для
автора: parser пропускает его как прозрачный prefix expression, а Story runtime ждет завершения external action через
обычный механизм `ActionCompletion`.

## Где находится модуль

```text
D:\JDHSeason\mods\echo-blackspace
```

Главные файлы:

```text
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\EchoBlackspace.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\api\BlackspaceScene.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\api\BlackspaceInstance.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\world\BlackspaceAllocator.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\world\BlackspaceBuilder.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\world\BlackspaceInstances.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\story\BlackspaceStoryExtensionFactory.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\story\CreateBlackspaceStoryAction.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\story\TeleportPlayerToBlackspaceStoryAction.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\network\BlackspaceNetwork.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\network\SetBlackspaceStatePacket.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\network\PrepareBlackspaceEnterPacket.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\network\BlackspaceShaderReadyPacket.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\client\BlackspaceClientState.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\client\BlackspaceFogRenderer.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\client\BlackspaceSkyRenderer.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\client\BlackspaceClientLifecycle.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\shader\BlackspaceShaderController.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\shader\BlackspaceShaderInstaller.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\shader\BlackspaceShaderState.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\shader\OculusShaderBridge.java
mods\echo-blackspace\src\main\resources\bundled-shaderpacks\EchoBlackspaceShader.zip
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\mixin\BlackspaceCameraMixin.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\mixin\BlackspaceEntityRenderDispatcherMixin.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\mixin\BlackspaceWorldLayerRendererMixin.java
mods\echo-blackspace\src\main\java\io\github\kxyr0\echostoryengine\blackspace\command\BlackspaceDebugCommand.java
```

DSL-компиляция Blackspace лежит не в `echo-blackspace`, а в Story/EchoScript adapter:

```text
mods\echo-story-engine\story-echoscript-adapter\src\main\java\io\github\kxyr0\echostoryengine\story\echoscript\intrinsic\actions\BlackspaceActionIntrinsics.java
mods\echo-story-engine\story-echoscript-adapter\src\main\java\io\github\kxyr0\echostoryengine\story\echoscript\intrinsic\LinearMethodBinder.java
mods\echo-story-engine\story-echoscript-adapter\src\main\java\io\github\kxyr0\echostoryengine\story\echoscript\intrinsic\NodeMethodBinder.java
mods\echo-script\echo-script-language\src\main\java\io\github\kxyr0\echoscript\language\parser\ExpressionParser.java
```

## Быстрая модель

```text
Story DSL
-> EchoScript compiler
-> SourceNode EXTERNAL_ACTION
-> Story runtime starts external command
-> echo_blackspace story extension receives action
-> BlackspaceScene.create / BlackspaceScene.teleportTo
-> chunks load
-> barrier platform appears
-> player is teleported
-> server sends SetBlackspaceStatePacket(true)
-> client state becomes active
-> fog renderer and sky renderer draw blackspace view
```

Когда игрок выходит из области Blackspace:

```text
server tick
-> BlackspaceScene.tick(server)
-> instance.contains(player) becomes false
-> BlackspaceScene.stop(player)
-> SetBlackspaceStatePacket(false)
-> client state becomes inactive
-> normal rendering returns
```

## Доступные DSL

Есть четыре поверхности использования:

1. Linear `OnStart()` DSL.
2. Explicit `Nodes()` DSL.
3. Generic `Action(...)` DSL.
4. Java server API.

Для авторов сюжета основной вариант - Linear `OnStart()`.

## Linear OnStart DSL

### Создать и телепортировать

```csharp
private void OnStart()
{
    var blackspace = await Blackspace.Create("jdh:first_contact");
    Player.Teleport(blackspace.Spawn);
}
```

Что это значит:

```text
Blackspace.Create("jdh:first_contact")
-> компилируется в external action echo_blackspace:create
-> action создает площадку
-> action возвращает Spawn
-> Story runtime переходит к следующему node только после SUCCESS

Player.Teleport(blackspace.Spawn)
-> компилируется в external action echo_blackspace:teleport_player
-> compiler знает, что blackspace был создан из "jdh:first_contact"
-> payload получает instanceId=jdh:first_contact
-> action телепортирует игрока на готовый spawn
```

Скомпилированная логика примерно такая:

```json
[
  {
    "id": "create_blackspace",
    "kind": "external_action",
    "actionType": "echo_blackspace:create",
    "arguments": {
      "instanceId": "jdh:first_contact",
      "spawnOutput": "blackspace.Spawn"
    },
    "output": "blackspace.Spawn",
    "next": "teleport_player"
  },
  {
    "id": "teleport_player",
    "kind": "external_action",
    "actionType": "echo_blackspace:teleport_player",
    "arguments": {
      "instanceId": "jdh:first_contact"
    },
    "next": "finish"
  }
]
```

### Создать без локальной переменной

Можно создать Blackspace отдельной командой:

```csharp
private void OnStart()
{
    Blackspace.Create("jdh:first_contact");
    Player.Teleport("jdh:first_contact");
}
```

Это полезно, если не нужен объект `blackspace` и не нужен `blackspace.Spawn`.

Компиляция:

```text
Blackspace.Create("jdh:first_contact")
-> actionType = echo_blackspace:create
-> arguments.instanceId = jdh:first_contact

Player.Teleport("jdh:first_contact")
-> actionType = echo_blackspace:teleport_player
-> arguments.instanceId = jdh:first_contact
```

### Указать игрока по имени

Если на сервере может быть больше одного игрока, лучше явно указывать игрока:

```csharp
private void OnStart()
{
    var blackspace = await Blackspace.Create("jdh:first_contact", playerName: "DevPlayer");
    Player.Teleport(blackspace.Spawn, playerName: "DevPlayer");
}
```

Payload:

```json
{
  "instanceId": "jdh:first_contact",
  "playerName": "DevPlayer"
}
```

### Указать игрока по UUID

```csharp
private void OnStart()
{
    var blackspace = await Blackspace.Create(
        "jdh:first_contact",
        playerUuid: "00000000-0000-0000-0000-000000000000"
    );
    Player.Teleport(
        blackspace.Spawn,
        playerUuid: "00000000-0000-0000-0000-000000000000"
    );
}
```

Payload:

```json
{
  "instanceId": "jdh:first_contact",
  "playerUuid": "00000000-0000-0000-0000-000000000000"
}
```

### Полный StoryResource пример

```csharp
namespace JDH.Chapter1.Blackspace;

class FirstContactBlackspaceStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:first_contact_blackspace", start: "start");
    }

    private void OnStart()
    {
        Story.Set("chapter_1.blackspace_entered", true);

        var blackspace = await Blackspace.Create("jdh:first_contact");
        Player.Teleport(blackspace.Spawn);

        Story.Set("chapter_1.blackspace_ready", true);
    }
}
```

Порядок выполнения:

```text
start
-> set_variable
-> create_blackspace
-> teleport_player
-> set_variable_2
-> finish
```

### Что можно писать в Linear DSL

Разрешено:

```csharp
var blackspace = await Blackspace.Create("namespace:path");
Player.Teleport(blackspace.Spawn);
```

```csharp
Blackspace.Create("namespace:path");
Player.Teleport("namespace:path");
```

```csharp
Blackspace.Create("namespace:path", playerName: "Name");
Player.Teleport("namespace:path", playerName: "Name");
```

```csharp
Blackspace.Create("namespace:path", playerUuid: "uuid");
Player.Teleport("namespace:path", playerUuid: "uuid");
```

Нельзя рассчитывать на runtime evaluation такого выражения в generic payload:

```csharp
Action(
    "teleport",
    type: "echo_blackspace:teleport_player",
    target: blackspace.Spawn,
    next: "finish"
);
```

Почему: Story action payload хранит строки. `blackspace.Spawn` как выражение понимает именно EchoScript compiler в
`Player.Teleport(...)`. Generic `Action(...)` не является expression runtime.

## Explicit Nodes DSL

`Nodes()` DSL используется, когда нужно руками задать node id и `next`.

### Создать Blackspace node

Позиционная форма:

```csharp
private void Nodes()
{
    Start("start", next: "create");

    Blackspace.Create(
        "create",
        "jdh:first_contact",
        output: "blackspace.Spawn",
        next: "teleport"
    );

    Player.Teleport(
        "teleport",
        "jdh:first_contact",
        next: "finish"
    );

    Finish("finish");
}
```

Что значит каждый аргумент:

```text
Blackspace.Create("create", "jdh:first_contact", output: "blackspace.Spawn", next: "teleport")
                 ^ node id  ^ instance id         ^ output variable       ^ next node
```

Компилируется в:

```json
{
  "id": "create",
  "kind": "external_action",
  "actionType": "echo_blackspace:create",
  "arguments": {
    "instanceId": "jdh:first_contact",
    "spawnOutput": "blackspace.Spawn"
  },
  "output": "blackspace.Spawn",
  "next": "teleport"
}
```

### Named форма create

```csharp
private void Nodes()
{
    Start("start", next: "create");

    Blackspace.Create(
        "create",
        instanceId: "jdh:first_contact",
        output: "blackspace.Spawn",
        next: "teleport"
    );

    Player.Teleport(
        "teleport",
        instanceId: "jdh:first_contact",
        next: "finish"
    );

    Finish("finish");
}
```

Также поддерживается alias `id`:

```csharp
Blackspace.Create(
    "create",
    id: "jdh:first_contact",
    next: "teleport"
);
```

### Player.Teleport в Nodes

Позиционная форма:

```csharp
Player.Teleport("teleport", "jdh:first_contact", next: "finish");
```

Named форма:

```csharp
Player.Teleport(
    "teleport",
    instanceId: "jdh:first_contact",
    next: "finish"
);
```

Альтернативно можно передать `target`, но для Blackspace лучше использовать `instanceId`:

```csharp
Player.Teleport(
    "teleport",
    target: "jdh:first_contact",
    next: "finish"
);
```

Если `target` выглядит как `namespace:path`, compiler кладет его как `instanceId`.

### Nodes DSL с playerName

```csharp
private void Nodes()
{
    Start("start", next: "create");

    Blackspace.Create(
        "create",
        "jdh:first_contact",
        playerName: "DevPlayer",
        next: "teleport"
    );

    Player.Teleport(
        "teleport",
        "jdh:first_contact",
        playerName: "DevPlayer",
        next: "finish"
    );

    Finish("finish");
}
```

## Generic Action DSL

Generic `Action(...)` работает, потому что Blackspace story bridge регистрирует внешние action types:

```text
echo_blackspace:create
echo_blackspace:teleport_player
```

### Создать через Action

```csharp
private void Nodes()
{
    Start("start", next: "create");

    Action(
        "create",
        type: "echo_blackspace:create",
        instanceId: "jdh:first_contact",
        spawnOutput: "blackspace.Spawn",
        output: "blackspace.Spawn",
        next: "teleport"
    );

    Action(
        "teleport",
        type: "echo_blackspace:teleport_player",
        instanceId: "jdh:first_contact",
        next: "finish"
    );

    Finish("finish");
}
```

Это самая низкоуровневая форма. Она не знает про `blackspace.Spawn` как expression. Все значения здесь строки.

### Payload echo_blackspace:create

Минимальный payload:

```json
{
  "instanceId": "jdh:first_contact"
}
```

Опциональный payload:

```json
{
  "instanceId": "jdh:first_contact",
  "playerName": "DevPlayer",
  "spawnOutput": "blackspace.Spawn"
}
```

или:

```json
{
  "instanceId": "jdh:first_contact",
  "playerUuid": "00000000-0000-0000-0000-000000000000"
}
```

Поддерживаемые ключи:

| Ключ | Обязателен | Что делает |
| --- | --- | --- |
| `instanceId` | да | ID Blackspace в формате `namespace:path`. |
| `id` | alias | Альтернативное имя для `instanceId` в runtime payload. |
| `playerName` | нет | Выбирает online player по имени. |
| `playerUuid` | нет | Выбирает online player по UUID. |
| `spawnOutput` | нет | Дополнительный output key, куда handler положит spawn string. |

Если `playerName` и `playerUuid` не указаны, adapter разрешает действие только когда онлайн ровно один игрок.

### Outputs echo_blackspace:create

`CreateBlackspaceStoryAction` возвращает:

```json
{
  "instanceId": "jdh:first_contact",
  "dimension": "minecraft:overworld",
  "spawnX": "123.5",
  "spawnY": "289.0",
  "spawnZ": "456.5",
  "yaw": "180.0",
  "pitch": "0.0",
  "Spawn": "minecraft:overworld;123.5;289.0;456.5;180.0;0.0"
}
```

Если payload содержит:

```json
{
  "spawnOutput": "blackspace.Spawn"
}
```

то outputs дополнительно получат:

```json
{
  "blackspace.Spawn": "minecraft:overworld;123.5;289.0;456.5;180.0;0.0"
}
```

Это нужно, потому что Story runtime привязывает output по имени `node.outputName()`. Для linear формы compiler сам ставит
`output: "blackspace.Spawn"` и `spawnOutput: "blackspace.Spawn"`.

### Payload echo_blackspace:teleport_player

Минимальный payload:

```json
{
  "instanceId": "jdh:first_contact"
}
```

С player selector:

```json
{
  "instanceId": "jdh:first_contact",
  "playerName": "DevPlayer"
}
```

Поддерживаемые ключи:

| Ключ | Обязателен | Что делает |
| --- | --- | --- |
| `instanceId` | да | ID уже созданного Blackspace. |
| `id` | alias | Альтернативное имя для `instanceId`. |
| `target` | частично | Если это `namespace:path`, runtime считает это instance id. |
| `playerName` | нет | Выбирает online player по имени. |
| `playerUuid` | нет | Выбирает online player по UUID. |

Важное ограничение:

```json
{
  "target": "blackspace.Spawn"
}
```

не сработает как expression. Runtime специально отвергает `.Spawn` в `target`, если compiler заранее не превратил это в
`instanceId`.

### Outputs echo_blackspace:teleport_player

```json
{
  "status": "TELEPORTED",
  "instanceId": "jdh:first_contact",
  "dimension": "minecraft:overworld",
  "spawnX": "123.5",
  "spawnY": "289.0",
  "spawnZ": "456.5",
  "yaw": "180.0",
  "pitch": "0.0",
  "Spawn": "minecraft:overworld;123.5;289.0;456.5;180.0;0.0"
}
```

## Java API

Для Forge/server code основной API:

```java
BlackspaceInstance instance = BlackspaceScene.create(player, "jdh:first_contact");
BlackspaceScene.teleportTo(player, instance);
```

### BlackspaceScene.create

```java
public static BlackspaceInstance create(ServerPlayer player, String instanceId) {
    Objects.requireNonNull(player, "player");
    BlackspaceInstance instance = INSTANCES.getOrCreate(player, instanceId);
    EchoBlackspace.LOGGER.info("Blackspace {} ready at {} {} {} in {}.",
            instance.id(),
            instance.spawnX(),
            instance.spawnY(),
            instance.spawnZ(),
            instance.dimensionId());
    return instance;
}
```

Ответственность:

```text
BlackspaceScene.create
-> получает ServerPlayer и instanceId
-> передает создание в BlackspaceInstances.getOrCreate
-> возвращает уже готовый BlackspaceInstance
```

`create` не просто создает объект в памяти. Через `BlackspaceInstances` он вызывает builder и создает площадку.

### BlackspaceScene.teleportTo

```java
public static void teleportTo(ServerPlayer player, BlackspaceInstance instance) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(instance, "instance");
    MinecraftServer server = player.getServer();
    if (server == null) {
        throw new IllegalStateException("Cannot teleport without an active server.");
    }
    ServerLevel level = server.getLevel(instance.dimension());
    if (level == null) {
        throw new IllegalStateException("Blackspace dimension is not loaded: " + instance.dimensionId());
    }
    player.teleportTo(level, instance.spawnX(), instance.spawnY(), instance.spawnZ(), Set.<RelativeMovement>of(),
            instance.yaw(), instance.pitch());
    start(player);
}
```

Ответственность:

```text
BlackspaceScene.teleportTo
-> находит ServerLevel по dimension из instance
-> вызывает ServerPlayer.teleportTo
-> сразу включает visual mode через BlackspaceScene.start(player)
```

Поэтому после `Player.Teleport(blackspace.Spawn)` не нужно вручную вызывать `start`.

### BlackspaceScene.start / stop

```java
public static void start(ServerPlayer player) {
    boolean changed = ACTIVE_PLAYERS.add(player.getUUID());
    BlackspaceNetwork.sendState(player, true);
}

public static void stop(ServerPlayer player) {
    boolean changed = ACTIVE_PLAYERS.remove(player.getUUID());
    BlackspaceNetwork.sendState(player, false);
}
```

Ответственность:

```text
start
-> помечает игрока active на сервере
-> отправляет packet active=true клиенту

stop
-> снимает active на сервере
-> отправляет packet active=false клиенту
```

Эти методы остались как низкоуровневый visual API. Для сюжета обычно использовать не надо.

### BlackspaceScene.tick

```java
public static void tick(MinecraftServer server) {
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        boolean inside = INSTANCES.containing(player).isPresent();
        boolean active = isActive(player);
        if (inside && !active) {
            start(player);
        } else if (!inside && active) {
            stop(player);
        }
    }
}
```

Ответственность:

```text
каждый server tick
-> проверить всех online игроков
-> если игрок внутри любого BlackspaceInstance и visual off: включить
-> если игрок не внутри BlackspaceInstance и visual on: выключить
```

Это и есть автоматическое включение/выключение visual mode после телепорта/выхода.

## BlackspaceInstance

`BlackspaceInstance` - immutable record с координатами готовой сцены.

```java
public record BlackspaceInstance(
        String id,
        ResourceKey<Level> dimension,
        BlockPos platformCenter,
        double spawnX,
        double spawnY,
        double spawnZ,
        float yaw,
        float pitch,
        int platformRadius,
        double visualRadius,
        double verticalRadius) {
}
```

Поля:

| Поле | Значение |
| --- | --- |
| `id` | Авторский id Blackspace, например `jdh:first_contact`. |
| `dimension` | Текущее измерение, где был создан Blackspace. |
| `platformCenter` | Центр barrier-платформы. |
| `spawnX/Y/Z` | Точка появления игрока над платформой. |
| `yaw/pitch` | Поворот игрока после телепорта. |
| `platformRadius` | Радиус платформы. Сейчас `8`. |
| `visualRadius` | Горизонтальная зона, внутри которой visual mode должен быть включен. Сейчас `96`. |
| `verticalRadius` | Вертикальная зона visual mode. Сейчас `96`. |

### Spawn string

```java
public String spawnValue() {
    return dimensionId() + ";" + spawnX + ";" + spawnY + ";" + spawnZ + ";" + yaw + ";" + pitch;
}
```

Формат:

```text
dimension;x;y;z;yaw;pitch
```

Пример:

```text
minecraft:overworld;65536.5;289.0;65536.5;180.0;0.0
```

### Outputs

```java
public Map<String, String> outputs() {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("instanceId", id);
    values.put("dimension", dimensionId());
    values.put("spawnX", Double.toString(spawnX));
    values.put("spawnY", Double.toString(spawnY));
    values.put("spawnZ", Double.toString(spawnZ));
    values.put("yaw", Float.toString(yaw));
    values.put("pitch", Float.toString(pitch));
    values.put("Spawn", spawnValue());
    return Map.copyOf(values);
}
```

Эти outputs используются story actions.

### Проверка нахождения игрока внутри Blackspace

```java
public boolean contains(ServerPlayer player) {
    if (!player.level().dimension().equals(dimension)) {
        return false;
    }
    double dx = Math.abs(player.getX() - platformCenter.getX());
    double dz = Math.abs(player.getZ() - platformCenter.getZ());
    double dy = Math.abs(player.getY() - platformCenter.getY());
    return dx <= visualRadius && dz <= visualRadius && dy <= verticalRadius;
}
```

Если игрок в том же измерении и находится в радиусах `visualRadius`/`verticalRadius`, visual mode считается активным.

## Генерация Blackspace

### BlackspaceAllocator

`BlackspaceAllocator` выбирает фиксированные координаты далеко от основной карты.

Ключевые константы:

```java
static final int PLATFORM_RADIUS = 8;
private static final int BLACKSPACE_X = 65_536;
private static final int BLACKSPACE_Z = 65_536;
private static final int Y_OFFSET_FROM_TOP = 32;
private static final double VISUAL_RADIUS = 96.0D;
private static final double VERTICAL_RADIUS = 96.0D;
```

Что это значит:

| Константа | Значение |
| --- | --- |
| `BLACKSPACE_X` | Фиксированный X центра Blackspace: `65536`. |
| `BLACKSPACE_Z` | Фиксированный Z центра Blackspace: `65536`. |
| `PLATFORM_RADIUS` | Платформа от центра на 8 блоков в каждую сторону. |
| `Y_OFFSET_FROM_TOP` | Платформа ставится ближе к верхней границе мира, но на 32 блока ниже `maxBuildHeight`. |
| `VISUAL_RADIUS` | Радиус зоны автоматического visual mode. |
| `VERTICAL_RADIUS` | Вертикальный радиус зоны visual mode. |

Алгоритм:

```java
BlackspaceInstance allocate(ServerLevel level, String id, int attempt) {
    int y = Math.max(level.getMinBuildHeight() + 8, level.getMaxBuildHeight() - Y_OFFSET_FROM_TOP);
    BlockPos center = new BlockPos(BLACKSPACE_X, y, BLACKSPACE_Z);
    return new BlackspaceInstance(...);
}
```

Важные свойства:

```text
любой instanceId в одном измерении
-> та же дальняя Blackspace-точка X=65536 Z=65536

несколько story instanceId могут ссылаться на одну и ту же построенную область
-> это сделано специально, чтобы Blackspace спавнился только по заданным координатам
```

### BlackspaceBuilder

`BlackspaceBuilder` реально меняет мир.

```java
void build(ServerLevel level, BlackspaceInstance instance) {
    loadPlatformChunks(level, instance);
    placePlatform(level, instance);
    clearSpawnHeadroom(level, instance);
}
```

Это порядок важен:

```text
1. Сначала загрузить чанки.
2. Потом поставить barrier.
3. Потом очистить воздух вокруг spawn.
```

#### Загрузка чанков

```java
private void loadPlatformChunks(ServerLevel level, BlackspaceInstance instance) {
    BlockPos center = instance.platformCenter();
    int radius = instance.platformRadius();
    int minChunkX = SectionPos.blockToSectionCoord(center.getX() - radius);
    int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + radius);
    int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - radius);
    int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + radius);
    for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            level.getChunk(chunkX, chunkZ);
        }
    }
}
```

`level.getChunk(chunkX, chunkZ)` синхронно подгружает нужные чанки перед установкой блоков.

#### Barrier platform

```java
private void placePlatform(ServerLevel level, BlackspaceInstance instance) {
    BlockPos center = instance.platformCenter();
    int radius = instance.platformRadius();
    int y = center.getY();
    for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
        for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
            level.setBlock(new BlockPos(x, y, z), BARRIER, Block.UPDATE_ALL);
        }
    }
}
```

При `platformRadius = 8` размер площадки:

```text
(8 * 2 + 1) x (8 * 2 + 1) = 17 x 17 blocks
```

Тип блока:

```java
Blocks.BARRIER.defaultBlockState()
```

#### Очистка места появления

```java
private void clearSpawnHeadroom(ServerLevel level, BlackspaceInstance instance) {
    BlockPos spawn = BlockPos.containing(instance.spawnX(), instance.spawnY(), instance.spawnZ());
    for (int yOffset = 0; yOffset <= 2; yOffset++) {
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                level.setBlock(spawn.offset(xOffset, yOffset, zOffset), AIR, Block.UPDATE_ALL);
            }
        }
    }
}
```

Это чистит 3x3x3 область воздуха вокруг точки появления, чтобы игрок не застрял.

### BlackspaceInstances

`BlackspaceInstances` - runtime registry всех созданных Blackspace на текущем сервере.

Главная структура:

```java
private final Map<String, BlackspaceInstance> instances = new ConcurrentHashMap<>();
```

Ключ:

```java
private String key(ServerLevel level, String id) {
    return level.dimension().location() + "|" + id;
}
```

То есть один и тот же `jdh:first_contact` в разных измерениях считается разными инстансами:

```text
minecraft:overworld|jdh:first_contact
minecraft:the_nether|jdh:first_contact
```

Главный метод:

```java
public BlackspaceInstance getOrCreate(ServerLevel level, String id) {
    String checkedId = requireId(id);
    String key = key(level, checkedId);
    BlackspaceInstance existing = instances.get(key);
    if (existing != null) {
        return existing;
    }
    synchronized (this) {
        existing = instances.get(key);
        if (existing != null) {
            return existing;
        }
        BlackspaceInstance created = allocateUnique(level, checkedId);
        builder.build(level, created);
        instances.put(key, created);
        return created;
    }
}
```

Важное поведение:

```text
если instance уже есть
-> вернуть существующий

если instance еще нет
-> allocateUnique
-> build platform
-> сохранить в map
-> вернуть готовый instance
```

То есть `getOrCreate` идемпотентен для одного `dimension + id`.

## Story Extension

`echo-blackspace` регистрируется как Story Extension через Forge event.

### EchoBlackspace

```java
public EchoBlackspace() {
    BlackspaceNetwork.register();
    MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
    MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
    MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
    MinecraftForge.EVENT_BUS.addListener(this::onStoryExtensionRegistration);
    LOGGER.info("Echo Blackspace started.");
}
```

Ответственность:

| Метод | Что делает |
| --- | --- |
| `BlackspaceNetwork.register()` | Регистрирует packet channel. |
| `onRegisterCommands` | Регистрирует debug command `/echo blackspace ...`. |
| `onPlayerLoggedOut` | Удаляет игрока из active visual set. |
| `onServerTick` | Автоматически включает/выключает visual mode. |
| `onServerStopping` | Чистит runtime registry. |
| `onStoryExtensionRegistration` | Регистрирует Story Extension bridge. |

### BlackspaceStoryIds

Action ids:

```java
static final NamespacedId EXTENSION_ID = NamespacedId.of("echo_blackspace:story_bridge");
static final NamespacedId CREATE = NamespacedId.of("echo_blackspace:create");
static final NamespacedId TELEPORT_PLAYER = NamespacedId.of("echo_blackspace:teleport_player");
```

Именно эти строки нужно использовать в generic `Action(...)`.

### BlackspaceStoryExtensionFactory

```java
private static final StoryExtensionDescriptor DESCRIPTOR = new StoryExtensionDescriptor(
        BlackspaceStoryIds.EXTENSION_ID,
        "Echo Blackspace Story Bridge",
        BlackspaceStoryIds.IMPLEMENTATION_VERSION,
        API_VERSION.major(),
        API_VERSION.minor(),
        List.of(action(BlackspaceStoryIds.CREATE), action(BlackspaceStoryIds.TELEPORT_PLAYER)));
```

Регистрирует два handler-а:

```text
echo_blackspace:create
echo_blackspace:teleport_player
```

Capabilities:

```java
ActionHandlerCapabilities.recoverableAsync()
```

Это значит, что Story Engine может восстанавливать waiting external action, а handler реализует `start`, `recover`,
`cancel`.

### BlackspaceStorySession

```java
this.handlers = List.of(
        new CreateBlackspaceStoryAction(server),
        new TeleportPlayerToBlackspaceStoryAction(server));
```

`StoryExtensionSession` серверный. Он хранит `MinecraftServer` и action handlers.

## CreateBlackspaceStoryAction

Handler для:

```text
echo_blackspace:create
```

Главная логика:

```java
ServerPlayer player = BlackspaceStoryPayload.player(server, payload);
BlackspaceInstance instance = BlackspaceScene.create(player, BlackspaceStoryPayload.instanceId(payload));
Map<String, String> outputs = new LinkedHashMap<>(instance.outputs());
BlackspaceStoryPayload.optional(payload, "spawnOutput")
        .ifPresent(outputName -> outputs.put(outputName, instance.spawnValue()));
sink.complete(new ActionCompletion(storyId, executionId, actionId, requestedType,
        ExternalActionResultCategory.SUCCESS, outputs));
```

Что происходит:

```text
1. Payload выбирает игрока.
2. Payload достает instanceId.
3. BlackspaceScene.create создает или возвращает готовый Blackspace.
4. Handler собирает outputs.
5. Handler отправляет SUCCESS в ActionCompletionSink.
```

Если payload плохой:

```java
return ActionInvocationResult.rejected("Invalid echo_blackspace:create payload: " + exception.getMessage());
```

Типичные причины:

```text
нет instanceId
instanceId не в формате namespace:path
нет playerName/playerUuid при нескольких online игроках
playerName/playerUuid не найден online
```

## TeleportPlayerToBlackspaceStoryAction

Handler для:

```text
echo_blackspace:teleport_player
```

Главная логика:

```java
ServerPlayer player = BlackspaceStoryPayload.player(server, payload);
String instanceId = BlackspaceStoryPayload.instanceId(payload);
BlackspaceInstance instance = BlackspaceScene.find(player, instanceId)
        .orElseThrow(() -> new IllegalArgumentException("Blackspace is not ready: " + instanceId));
BlackspaceScene.teleportTo(player, instance);
Map<String, String> outputs = new LinkedHashMap<>();
outputs.put("status", "TELEPORTED");
outputs.putAll(instance.outputs());
sink.complete(new ActionCompletion(storyId, executionId, actionId, requestedType,
        ExternalActionResultCategory.SUCCESS, outputs));
```

Что происходит:

```text
1. Payload выбирает игрока.
2. Payload достает instanceId.
3. Handler ищет уже созданный Blackspace в текущем измерении игрока.
4. Если Blackspace не создан, action rejected.
5. Если найден, игрок телепортируется.
6. BlackspaceScene.teleportTo сразу включает visual mode.
7. Handler завершает action SUCCESS.
```

Важное правило:

```text
Player.Teleport не должен сам создавать площадку.
Площадку создает Blackspace.Create.
Если teleport вызван до create, будет rejected: Blackspace is not ready.
```

## Player selection

`BlackspaceStoryPayload.player` выбирает игрока так:

```java
String uuid = payload.get("playerUuid");
if (uuid != null && !uuid.isBlank()) {
    ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuid.trim()));
    ...
}
String name = payload.get("playerName");
if (name != null && !name.isBlank()) {
    ServerPlayer player = server.getPlayerList().getPlayerByName(name.trim());
    ...
}
List<ServerPlayer> players = server.getPlayerList().getPlayers();
if (players.size() == 1) {
    return players.get(0);
}
throw new IllegalArgumentException("playerUuid or playerName is required when online player count is not 1.");
```

Правило:

```text
1 online player
-> можно не указывать playerName/playerUuid

2+ online players
-> обязательно указывать playerName или playerUuid
```

## Client visual layer

Visual layer сохранен отдельно от server generation.

### Network packet

Сервер отправляет:

```java
new SetBlackspaceStatePacket(active)
```

Packet содержит только boolean:

```java
public record SetBlackspaceStatePacket(boolean active) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
    }

    public static SetBlackspaceStatePacket decode(FriendlyByteBuf buffer) {
        return new SetBlackspaceStatePacket(buffer.readBoolean());
    }
}
```

На клиенте:

```java
private static void applyClientState(boolean active) {
    BlackspaceClientState.setActive(active);
}
```

### BlackspaceClientState

```java
public final class BlackspaceClientState {
    private static final AtomicBoolean ACTIVE = new AtomicBoolean();

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static void setActive(boolean active) {
        ACTIVE.set(active);
    }

    public static void clear() {
        ACTIVE.set(false);
    }
}
```

Это единственная клиентская truth-state переменная для визуала.

### Fog renderer

```java
private static final float FOG_NEAR = 1_000_000.0F;
private static final float FOG_FAR = 1_000_001.0F;
private static final float FOG_RED = 0.0F;
private static final float FOG_GREEN = 0.0F;
private static final float FOG_BLUE = 0.0F;
```

Когда active:

```java
event.setRed(0.0F);
event.setGreen(0.0F);
event.setBlue(0.0F);
event.setNearPlaneDistance(1_000_000.0F);
event.setFarPlaneDistance(1_000_001.0F);
event.setFogShape(FogShape.SPHERE);
event.setCanceled(true);
```

Итог:

```text
fog остается черным по цвету
distance выставлена далеко, чтобы игрок не растворялся в тумане при катсценной камере
```

### Sky renderer / world blackout

`BlackspaceSkyRenderer` рисует черный cube на `AFTER_SKY`, то есть до vanilla entity pass:

```java
if (!BlackspaceClientState.isActive() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
    return;
}
renderCameraCenteredCube(event.getPoseStack(), event.getCamera().getPosition());
```

Ключевые параметры:

```java
private static final float CUBE_RADIUS = 128.0F;
private static final int BLACK = 0;
private static final int OPAQUE = 255;
```

Черный cube не пишет depth buffer, поэтому vanilla player renderer потом рисует модель игрока в настоящем world space.

### Client mixins для Blackspace visibility

`BlackspaceWorldLayerRendererMixin` отключает рендер world layers, которые иначе перекрывали бы Blackspace-фон или игрока:

```text
renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V
-> не рисуются solid/cutout/translucent/tripwire blocks

renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V
-> не рисуются rain/snow weather layers

isChunkCompiled(Lnet/minecraft/core/BlockPos;)Z для localPlayer.blockPosition
-> local player не пропадает, когда катсценная камера далеко и chunk игрока не compiled относительно камеры
```

Все `@Inject` targets указаны через реальные Mojang/Forge имена методов (`isChunkCompiled`, `renderChunkLayer`,
`renderSnowAndRain`) с обычным remap. Ручные SRG/obfuscated имена `m_...` здесь не используются.

`BlackspaceEntityRenderDispatcherMixin` не рисует игрока вручную. Он только меняет правила vanilla entity pass:

```text
shouldRender(localPlayer) -> true
-> локальный игрок не отваливается по distance/frustum culling

getPackedLightCoords(localPlayer) -> LightTexture.FULL_BRIGHT
-> игрок игнорирует темноту и виден с катсценной дистанции
```

`BlackspaceCameraMixin` возвращает `Camera.isDetached() == true`, если Blackspace active и camera реально вышла из глаз
игрока. Это нужно, чтобы vanilla `LevelRenderer` сам показал локального игрока в F5/cutscene camera, а не через
отдельный camera-space рендер.

`BlackspaceShaderController` управляет fade-входом и fade-выходом Blackspace на клиенте. До телепорта он затемняет экран,
устанавливает bundled shaderpack `EchoBlackspaceShader.zip`, просит Oculus/Iris переключить pipeline и только после готовности
отправляет серверу `BlackspaceShaderReadyPacket`. Сервер завершает подготовленный телепорт уже после этого ACK.

`BlackspaceShaderInstaller` копирует внутренний shaderpack из ресурсов мода в runtime-папку `shaderpacks`, если файла ещё нет
или его содержимое отличается от bundled версии. Это внутренняя реализация `echo-blackspace`, не отдельный Forge-модуль и не
публичный SceneShader API.

`OculusShaderBridge` содержит все прямые вызовы Oculus/Iris API. При входе он сохраняет состояние пользователя, временно
ставит `EchoBlackspaceShader.zip`, вызывает reload shader pipeline и сразу возвращает файл `oculus.properties` к прежним
байтам, чтобы Blackspace shaderpack не становился постоянным выбором пользователя. При выходе, logout или world unload
мост через fade восстанавливает прежний shaderpack; если shaders были выключены до Blackspace, после выхода они снова
выключаются. Без Oculus контроллер оставляет Forge fallback: чёрный sky/fog renderer и обычное сетевое состояние Blackspace.

Mixin config регистрируется явно в `META-INF/mods.toml`:

```toml
[[mixins]]
config = "echo_blackspace.mixins.json"
```

HUD рисуется уже после level render, поэтому хотбар, сердца и интерфейс остаются видимыми обычным способом.

Чтобы first-person рука не оставалась поверх черного мира, renderer отменяет `RenderHandEvent`:

```java
@SubscribeEvent
public static void onRenderHand(RenderHandEvent event) {
    if (BlackspaceClientState.isActive()) {
        event.setCanceled(true);
    }
}
```

Это убирает руку и предмет в руке только во время active Blackspace visual.

### Client lifecycle

`BlackspaceClientLifecycle` сбрасывает visual state:

```text
client logout
client player clone
client level unload
client tick with no player or no level
```

Код:

```java
@SubscribeEvent
public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    BlackspaceClientState.clear();
}

@SubscribeEvent
public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
    BlackspaceClientState.clear();
}

@SubscribeEvent
public static void onLevelUnload(LevelEvent.Unload event) {
    if (event.getLevel() instanceof ClientLevel) {
        BlackspaceClientState.clear();
    }
}
```

## Debug command

Команда:

```text
/echo blackspace start
/echo blackspace start <player>
/echo blackspace stop
/echo blackspace stop <player>
```

`start` теперь не просто включает visual. Он создает debug Blackspace и телепортирует:

```java
private int start(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
    BlackspaceInstance instance = BlackspaceScene.create(player, "echo_blackspace:debug");
    BlackspaceScene.teleportTo(player, instance);
    source.sendSuccess(() -> Component.literal("Blackspace ready for "
            + player.getGameProfile().getName() + " at " + instance.spawnValue()), false);
    return 1;
}
```

`stop` выключает visual:

```java
private int stop(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
    BlackspaceScene.stop(player);
    source.sendSuccess(() -> Component.literal("Blackspace disabled for "
            + player.getGameProfile().getName()), false);
    return 1;
}
```

Важная деталь: если игрок стоит внутри Blackspace, server tick может снова включить visual после `stop`.
Чтобы выйти полностью, игрока нужно телепортировать за пределы `visualRadius`.

## Консольные команды

В `echo-blackspace` сейчас есть только debug-команды. Они нужны для ручной проверки модуля в игре или через server
console.

Команды требуют permission level `2`, потому что зарегистрированы так:

```java
dispatcher.register(Commands.literal("echo").requires(source -> source.hasPermission(2))
        .then(Commands.literal("blackspace")
                .then(Commands.literal("start") ...)
                .then(Commands.literal("stop") ...)));
```

### Быстрый вход в debug Blackspace

Если команда выполняется игроком:

```mcfunction
/echo blackspace start
```

Что произойдет:

```text
1. Будет создан или найден Blackspace с id echo_blackspace:debug.
2. Он будет создан в текущем измерении игрока.
3. Чанки под площадку будут загружены.
4. Barrier-платформа будет поставлена.
5. Игрок будет телепортирован на spawn.
6. Visual mode включится автоматически.
```

Ожидаемый ответ команды:

```text
Blackspace ready for <playerName> at <dimension>;<x>;<y>;<z>;<yaw>;<pitch>
```

Пример:

```text
Blackspace ready for DevPlayer at minecraft:overworld;65536.5;289.0;65536.5;180.0;0.0
```

### Вход в debug Blackspace для конкретного игрока

Из server console или от оператора:

```mcfunction
/echo blackspace start DevPlayer
```

Что важно:

```text
DevPlayer должен быть online.
Blackspace будет создан в текущем измерении DevPlayer.
Командующий игрок не переносится, если указан другой target player.
```

### Выключить visual mode себе

```mcfunction
/echo blackspace stop
```

Что произойдет:

```text
1. Сервер удалит игрока из ACTIVE_PLAYERS.
2. Клиенту уйдет SetBlackspaceStatePacket(false).
3. Черный fog и sky renderer отключатся.
```

Важная деталь: если игрок все еще стоит внутри зоны Blackspace, следующий server tick снова включит visual mode.
Это нормальное поведение автоматического tracking.

Чтобы visual mode не включился обратно, сначала телепортируй игрока наружу:

```mcfunction
/tp DevPlayer 0 100 0
/echo blackspace stop DevPlayer
```

### Выключить visual mode конкретному игроку

```mcfunction
/echo blackspace stop DevPlayer
```

Эта команда не удаляет созданную платформу и не удаляет `BlackspaceInstance` из runtime registry. Она только выключает
visual flag для игрока.

### Полный ручной smoke test

Один игрок:

```mcfunction
/echo blackspace start
```

Проверить, что:

```text
игрок телепортирован далеко от карты
под игроком barrier-платформа 17x17
экран черный
fog не съедает игрока с катсценной дистанции
sky черный
```

Выйти наружу:

```mcfunction
/tp @s 0 120 0
```

После выхода visual mode должен отключиться автоматически на server tick. Если нужно принудительно:

```mcfunction
/echo blackspace stop
```

### Multiplayer smoke test

```mcfunction
/echo blackspace start DevPlayer
```

Проверить:

```text
DevPlayer перенесен в debug Blackspace
у DevPlayer включился black visual
у других игроков visual не включился
```

Отключить:

```mcfunction
/tp DevPlayer 0 120 0
/echo blackspace stop DevPlayer
```

### Проверить, что visual включается автоматически после возврата внутрь

1. Создать debug Blackspace:

```mcfunction
/echo blackspace start DevPlayer
```

2. Скопировать координаты из ответа команды:

```text
minecraft:overworld;65536.5;289.0;65536.5;180.0;0.0
```

3. Увести игрока наружу:

```mcfunction
/tp DevPlayer 0 120 0
```

4. Вернуть игрока на координаты Blackspace:

```mcfunction
/execute in minecraft:overworld run tp DevPlayer 65536.5 289.0 65536.5 180 0
```

Ожидаемо:

```text
server tick увидит, что игрок снова внутри BlackspaceInstance
BlackspaceScene.start(player) вызовется автоматически
visual mode снова включится
```

### Проверить, что visual выключается после выхода

```mcfunction
/echo blackspace start DevPlayer
/tp DevPlayer 0 120 0
```

Ожидаемо:

```text
после выхода за пределы visualRadius=96
BlackspaceScene.tick(server) вызовет stop
клиент получит active=false
black fog/sky исчезнут
```

### Ограничения текущих команд

Сейчас debug command не умеет задавать custom instance id:

```text
/echo blackspace start
```

всегда использует:

```text
echo_blackspace:debug
```

Если нужен конкретный id вроде `jdh:first_contact`, используй Story DSL:

```csharp
var blackspace = await Blackspace.Create("jdh:first_contact");
Player.Teleport(blackspace.Spawn);
```

или generic Story action:

```csharp
Action(
    "create_blackspace",
    type: "echo_blackspace:create",
    instanceId: "jdh:first_contact",
    next: "teleport_player"
);
```

### Серверная консоль без slash

В некоторых server consoles команды вводятся без `/`:

```text
echo blackspace start DevPlayer
echo blackspace stop DevPlayer
```

В игровом чате обычно нужен `/`:

```mcfunction
/echo blackspace start DevPlayer
/echo blackspace stop DevPlayer
```

## Команды сборки и установки

## Сборка и зависимости

`echo-blackspace` зависит от:

```gradle
dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"
    compileOnly "io.github.kxyr0.echostoryengine:story-contracts:${story_api_version}"
    compileOnly "io.github.kxyr0.echostoryengine:story-extension-api:${story_api_version}"
    compileOnly "io.github.kxyr0.echostoryengine:story-extension-forge-1.20.1:${story_api_version}"
}
```

И подключает local composite build:

```gradle
includeBuild '../echo-story-engine'
```

В `mods.toml` есть runtime dependency:

```toml
[[dependencies.${mod_id}]]
modId = "echo_story_engine"
mandatory = true
versionRange = "${story_mod_version_range}"
ordering = "AFTER"
side = "BOTH"
```

Это нужно, потому что `echo-blackspace` регистрирует Story Extension через `EchoStoryExtensionRegistrationEvent`.

## Что точно создает Blackspace.Create

`Blackspace.Create` обязан создать площадку.

Фактическая цепочка:

```text
Blackspace.Create DSL
-> BlackspaceActionIntrinsics.linearCreate/createNode
-> actionType echo_blackspace:create
-> CreateBlackspaceStoryAction.invoke
-> BlackspaceScene.create
-> BlackspaceInstances.getOrCreate
-> BlackspaceAllocator.allocate
-> BlackspaceBuilder.build
-> BlackspaceBuilder.loadPlatformChunks
-> BlackspaceBuilder.placePlatform
-> BlackspaceBuilder.clearSpawnHeadroom
-> ActionCompletion SUCCESS
```

Если `ActionCompletion SUCCESS` получен, значит:

```text
instance создан
чанки под платформу загружены
barrier blocks поставлены
spawn headroom очищен
outputs сформированы
```

## Что НЕ надо делать внешней системе

Старое правило больше не действует:

```text
Внешняя система должна сама поставить игрока и вызвать start/stop.
```

Теперь правильно:

```csharp
var blackspace = await Blackspace.Create("jdh:first_contact");
Player.Teleport(blackspace.Spawn);
```

Внешняя система не должна:

```text
сама строить platform
сама решать координаты Blackspace
сама вручную вызывать BlackspaceScene.start после Player.Teleport
сама управлять client state
```

Этим занимается `echo-blackspace`.

## Частые сценарии

### Начать сцену в Blackspace

```csharp
private void OnStart()
{
    var blackspace = await Blackspace.Create("jdh:first_contact");
    Player.Teleport(blackspace.Spawn);
    Dialogue.Load("jdh:first_contact_dialogue", audienceId: "jdh:local_author", output: "dialogueResult");
}
```

### Создать Blackspace заранее, телепортировать позже

```csharp
private void OnStart()
{
    Blackspace.Create("jdh:first_contact");
    Story.Set("chapter_1.blackspace_prepared", true);
    Player.Teleport("jdh:first_contact");
}
```

### Multiplayer-safe

```csharp
private void OnStart()
{
    var blackspace = await Blackspace.Create("jdh:first_contact", playerName: "DevPlayer");
    Player.Teleport(blackspace.Spawn, playerName: "DevPlayer");
}
```

### Explicit nodes, если нужен ручной граф

```csharp
private void Nodes()
{
    Start("start", next: "create_blackspace");

    Blackspace.Create(
        "create_blackspace",
        "jdh:first_contact",
        output: "blackspace.Spawn",
        next: "teleport_player"
    );

    Player.Teleport(
        "teleport_player",
        "jdh:first_contact",
        next: "finish"
    );

    Finish("finish");
}
```

### Generic Action, если intrinsic временно недоступен

```csharp
private void Nodes()
{
    Start("start", next: "create_blackspace");

    Action(
        "create_blackspace",
        type: "echo_blackspace:create",
        instanceId: "jdh:first_contact",
        next: "teleport_player"
    );

    Action(
        "teleport_player",
        type: "echo_blackspace:teleport_player",
        instanceId: "jdh:first_contact",
        next: "finish"
    );

    Finish("finish");
}
```

## Ошибки и диагностика

### Нет instanceId

Плохо:

```csharp
Blackspace.Create();
```

Плохо:

```csharp
Action("create", type: "echo_blackspace:create", next: "finish");
```

Правильно:

```csharp
Blackspace.Create("jdh:first_contact");
```

или:

```csharp
Action(
    "create",
    type: "echo_blackspace:create",
    instanceId: "jdh:first_contact",
    next: "finish"
);
```

### Неверный id

Плохо:

```csharp
Blackspace.Create("First Contact");
```

Правильно:

```csharp
Blackspace.Create("jdh:first_contact");
```

`instanceId` должен быть `namespace:path`.

### Teleport до Create

Плохо:

```csharp
private void OnStart()
{
    Player.Teleport("jdh:first_contact");
}
```

Если `jdh:first_contact` еще не создан в текущем измерении игрока, будет rejected:

```text
Blackspace is not ready: jdh:first_contact
```

Правильно:

```csharp
private void OnStart()
{
    Blackspace.Create("jdh:first_contact");
    Player.Teleport("jdh:first_contact");
}
```

### Несколько игроков online

Плохо:

```csharp
private void OnStart()
{
    var blackspace = await Blackspace.Create("jdh:first_contact");
    Player.Teleport(blackspace.Spawn);
}
```

если online больше одного игрока.

Правильно:

```csharp
private void OnStart()
{
    var blackspace = await Blackspace.Create("jdh:first_contact", playerName: "DevPlayer");
    Player.Teleport(blackspace.Spawn, playerName: "DevPlayer");
}
```

## Проверочный минимум после изменений

Для сборки `echo-blackspace`:

```powershell
& 'D:\JDHSeason\mods\echo-trigger-player-sleep-adapter\gradlew.bat' `
  -p 'D:\JDHSeason\mods\echo-blackspace' `
  build --console=plain
```

Для проверки DSL-компилятора:

```powershell
cd D:\JDHSeason\mods\echo-story-engine
.\gradlew.bat :story-echoscript-adapter:test --console=plain
```

Для полной проверки Story Engine:

```powershell
cd D:\JDHSeason\mods\echo-story-engine
.\gradlew.bat build --console=plain
```

Для проверки EchoScript parser:

```powershell
cd D:\JDHSeason\mods\echo-script
.\gradlew.bat build --console=plain
```

## Короткая памятка

Основной код для сюжета:

```csharp
var blackspace = await Blackspace.Create("jdh:first_contact");
Player.Teleport(blackspace.Spawn);
```

Низкоуровневые action ids:

```text
echo_blackspace:create
echo_blackspace:teleport_player
```

Главный server API:

```java
BlackspaceInstance instance = BlackspaceScene.create(player, "jdh:first_contact");
BlackspaceScene.teleportTo(player, instance);
```

Главное runtime правило:

```text
Create строит площадку.
Teleport переносит в уже готовый Blackspace.
Visual mode включается и выключается автоматически.
```
