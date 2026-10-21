# Echo Item Inspection — EII1

Версия `0.1.1`, Forge `1.20.1 / 47.4.10`, Java 17. Самостоятельный репозиторий;
мод `echo_item_inspection` добавляет только клиентский осмотр конкретного предмета.

| Модуль | Ответственность |
|---|---|
| `inspection-core` | Типизированные определения, ES2 parser/binder, каталог, LKG, quaternion transform |
| `inspection-forge-1.20.1` | Item registry binding, discovery, snapshot, client Screen/input/render/commands |
| `inspection-testmod-1.20.1` | Только dev: предмет, варианты модели, inspection fixtures, dedicated GameTests |

Проверка `architectureTest` запрещает Minecraft/Forge imports в core и зависимости
от Observation, Thought, Story и Item Attachments. Item Pack остаётся отдельным
источником зарегистрированных item IDs. Удаление JAR осмотра не требует менять Item Pack.

## Открытие и возврат

`ScreenEvent.MouseButtonPressed.Pre` проверяет RMB, normal `AbstractContainerScreen`,
текущий menu, активный slot и определение предмета. Только успешное открытие отменяет
vanilla click до изменения stack; завершающий release того же RMB также поглощается.
Пустой слот и предмет без определения продолжают обычное взаимодействие.

Creative inventory и modded screens с собственными mouse/slot handlers сохраняют
свою семантику. Обычные inventory/chest/container screens поддерживаются общей границей slot.

Viewer открывается через Forge GUI layer: родительский экземпляр screen не удаляется,
menu не закрывается и packet закрытия при открытии не отправляется. Return context
проверяет личность player, level, menu, состояние player и `menu.stillValid(player)`.
Esc возвращает тот же родительский screen только пока контекст действителен; закрытый
сервером menu не восстанавливается. Pause semantics наследуются от родителя.

## Exact stack и рендер

`api.ItemStackSnapshot` сохраняет `ItemStack.copy()`. Каждый `copyStack()` возвращает
новую копию. Count, NBT, CustomModelData, damage, enchantments и glint сохраняются;
snapshot никогда не вставляется обратно в inventory.

`InspectionRenderer` получает реальную baked model через `ItemRenderer.getModel`
с исходным stack и рендерит через `ItemRenderer.render` с `ItemDisplayContext.GUI`,
full-bright light и выбором flat/3D lighting по model. Отдельный 3D pose применяет
quaternion ориентации и масштаб модели; это не увеличение готовой 16×16 картинки.

`InspectionViewport` вычисляет физические размеры и переводит их в GUI coordinates
один раз. ЛКМ drag меняет quaternion, wheel — ограниченный zoom. Mouse move без drag
не вращает модель. Трансформация отделена от input для будущих model-local anchors.

## Фон и необязательный текст

Viewer перекрывает parent screen тёмно-фиолетовым shader background из Echo Blackspace.
`InspectionBackground` адаптирует палитру и smoothstep из bundled `final.fsh`, затемняя
её до 32%. Core shader рисуется только перед моделью и текстом; Oculus и глобальный
shaderpack не переключаются, состояние Blackspace и положение игрока не меняются.
При отсутствии загруженного shader используется фиолетовый gradient fallback.
Inventory/container остаётся виден под viewer, а input продолжает принадлежать осмотру.

Типизированный `ItemInspectionText` входит в definition и descriptor. Class-level
ES2 declarations `Title("...")` и `Description("...")` необязательны; defaults —
пустые строки. UI использует реальное hover name snapshot, если Title не задан,
и не показывает отсутствующее описание. Limits проверяются до публикации каталога:
Title — 256, Description — 8192 Unicode code points; строки не меняют ItemStack NBT.
Активная сессия удерживает прежний текст вместе с definition revision.

Текст переносится внутри отдельной responsive панели и сохраняется целиком.
Model viewport и текстовая панель имеют собственное место; длинное описание
прокручивается в панели и не увеличивает Screen или модель.
`InspectionLayout` выбирает правую колонку для широкого framebuffer и нижнюю панель
для остальных размеров, независимо от GUI Scale. `InspectionTextContent` кеширует
переносы при init/resize, `InspectionTextScroll` хранит ограниченную позицию строк.
`InspectionTextPanel` владеет drag ползунка и клавишами Up/Down, PgUp/PgDn, Home/End;
wheel остаётся zoom модели, а жесты внутри панели не начинают model drag.

## Контент и reload

Упакованные ресурсы: `data/<namespace>/echo_item_inspection/inspections/*.inspection.es`.
Клиентские resource packs: `assets/<namespace>/echo_item_inspection/inspections/*.inspection.es`;
F3+T перечитывает их через Minecraft resource reload listener.
Внешние: `<minecraft>/story/item-inspection/<namespace>/*.inspection.es`.
При совпадении логического source path resource pack заменяет packaged definition,
а внешний файл заменяет оба варианта.

Каталог публикует валидный candidate атомарно. Ошибка parser/binder, неизвестный
item ID, неверный View или duplicate item binding сохраняют последний рабочий каталог.
Сессия удерживает своё определение и revision; следующий open использует новый каталог.
Внешние файлы проверяются раз в секунду в игровом мире; есть явный `reload` command.

Defaults: Scale `1`, Euler `(12, -20, 0)` градусов, Zoom `0.65–2.4` с начальным `1`;
rotation sensitivity `0.35` градуса на framebuffer pixel, exponential wheel sensitivity `0.12`.

Binding выполняется по canonical registry ID типа предмета. Vanilla F3+H включает
`advancedItemTooltips`; `ItemStack.getTooltipLines(ADVANCED)` выводит
`BuiltInRegistries.ITEM.getKey(item)`. Этот ID передаётся в `Item(...)`, а не display name.
`/echo_item_inspection list` выводит `inspection resource -> item ID revision=N`.
Отсутствие definitions оставляет все предметы с vanilla RMB.

## Проверка

Из корня репозитория:

```powershell
.\gradlew.bat check build --offline
.\gradlew.bat :inspection-testmod-1.20.1:runGameTestServer --offline
.\gradlew.bat :inspection-testmod-1.20.1:runClient --offline
.\gradlew.bat :inspection-testmod-1.20.1:runClient -PinspectionSmoke=true --offline
```

`sourceShapeCheck` завершает build с ошибкой при >180 logical LOC в production
Java/ES или >7 файлов в leaf package/content folder; >100 Java LOC выводятся для SRP review.
Методы >30 logical LOC находятся через JDK syntax tree и выводятся для review.
`verifySourceShapeEnforcement` проверяет ту же enforcement closure на generated fixtures
под `build/source-shape-probes`: границы 180/181 LOC, 7/8 файлов, комментарии и строки.
`verifyProductionJarContents` проверяет reobfuscated `-all.jar`, встроенный core,
локализации и отсутствие testmod, Minecraft, JUnit и runtime artifacts.

Шесть GameTests проверяют real ItemStack snapshot, count/NBT, defensive copies,
block/custom data, damage/glint, vanilla split stack и неизменность slot. Они запускаются
в dedicated server; успешный запуск также проверяет отсутствие client classloading crash.
GameTests не заменяют проверку изображения и UI в работающем клиенте.

Автоматический client smoke использует отдельный dev world `inspection-smoke` в
`inspection-testmod-1.20.1/run/saves` (копию GameTest world), пишет
`run/inspection-smoke.json`, PNG в `run/screenshots` и завершает свой dev client.
Resolution matrix проверяется layout-тестами; live screenshots фиксируют фактический
framebuffer и GUI scales в JSON.

Content Doctor использует `ItemInspectionDescriptor` из того же semantic compiler:
source повторно не разбирается. Реальные registry IDs экспортируются клиентом в
`config/echo_item_inspection/item-catalog.txt`; offline tool принимает этот snapshot,
чтобы отличать зарегистрированные Echo/vanilla items от неизвестных или pending items.
Из соседнего `echo-content-doctor`:

```powershell
.\gradlew.bat contentDoctor -PcontentRoot="D:\content-pack" -PitemCatalog="D:\minecraft\config\echo_item_inspection\item-catalog.txt" --no-parallel --max-workers=1
```

Doctor проверяет syntax/View/item references и duplicate bindings; без registry snapshot
он явно требует `ITEM_INSPECTION_ITEM_CATALOG_REQUIRED` для inspection resources.
CLI arguments: `--resources <content-root> --item-catalog <file> <report.json>`.
Каталог UTF-8 содержит один canonical item ID на строке. Используется реальный экспорт
vanilla и modded registry, а не вручную составленный список. `.weapon.es` проходит
настоящий Item Pack compiler; определение ещё не зарегистрированного предмета не удовлетворяет binding.

Dev item: `echo_item_inspection_testmod:inspection_token`. Его базовая модель —
generated item; `CustomModelData:19` выбирает отдельную объёмную модель. Также есть
определения для stone, written_book и diamond_sword; cobblestone намеренно не inspectable.

Live smoke: RMB над каждым fixture → viewer; ЛКМ drag → реальное вращение; wheel → zoom;
Esc → тот же inventory/chest. Cobblestone×64 → обычное деление 32/32. Дополнительно:
server close menu во время viewer, GUI Scale 1–4/Auto, 1280×720, 1366×768, 1920×1080,
2560×1440 и 3440×1440, valid/broken reload и открытая сессия старой revision.

Production JAR: `inspection-forge-1.20.1/build/libs/echo-item-inspection-0.2.2-all.jar`.
Testmod JAR в production не устанавливается. Deployment: `.deploying`, SHA-256 до/после,
атомарное перемещение, старый JAR вне active mods, ровно один активный mod ID.

```powershell
.\scripts\deploy-prism.ps1
```

Скрипт сам не запускает build: перед ним должны пройти проверки. Он сохраняет backups
в `D:/JDHSeason/deployment-backups/echo-item-inspection`, возвращает JSON с hash/путём/count
и останавливается с `FAILED_LOCKED` и точным путём при занятом файле, не завершая Minecraft.

Hotspots, Observation/Thought, сюжетные действия, NBT predicates, animation и разборка
предмета в EII1 отсутствуют; их границы не смешиваются с model/input/session.

## EII2: audit и интеграция IPLA

Расширение версии 0.1.1 → 0.2.0 остаётся в трёх существующих модулях и одном
production mod ID. Проверены текущие catalog/binder, `ItemStackSnapshot`, session,
return context, API, input, renderer, локализации, Doctor, tests и deploy script.
Предыдущие пользовательские изменения интерфейса сохранены.

Аудированный установленный артефакт: `ipla-mc1.20.1-forge-6.3.1.jar`, mod ID `ipla`,
версия 6.3.1, SHA-256 `55B108DD743876B6A48708F9B059767C1C5CE88AA4C4EDD0916C5ADB19C0457F`.
Зависимость IPLA: Architectury 9.2.14. Исходного API-модуля в workspace нет;
интеграция компилируется против фактического JAR через ForgeGradle deobf/compileOnly.
Публичные сигнатуры и bytecode проверены через javap и локальный ForgeFlower;
runtime reflection/field guessing отсутствуют.

`com.akicater.blocks.LayingItemEntity` — BlockEntity с публичным `inv` из 24
ItemStack, шестью флагами `quad` и 24 углами `rot`. `setItem` сохраняет копию
стека с count=1 — это семантика самого IPLA. Точный источник EII — `inv.get(slot)`,
не новый стек по ID. NBT inventory сохраняется через ContainerHelper в saveAdditional,
getUpdateTag/getUpdatePacket синхронизируют его на клиент; markDirty обновляет блок.
Удаление блока очищает inventory и снимает BlockEntity. Selection shape собирается
IPLA из публичных basicShapes/basicQuadShapes и состояния слотов, с внутренним cache.

`IplaInspectionTargetResolver` делает один `ClientLevel.clip` от позиции/направления
активной `GameRenderer` camera, `OUTLINE/NONE`, в пределах client config distance.
Первая поверхность перекрывает предметы за стеной. Только для одного найденного
IPLA BlockEntity вызывается публичный `IPLA_Methods.getPreciseIndexFromHit(entity, hit, false)`;
выбирается первый индекс, как в штатном `LayingItem.use`. Максимум 24 локальных
кандидата, сканирования world/chunk/списка размещённых предметов нет.
На activation selection API вызывается заново даже при неизменной камере;
frame cache не может подменить проверку нового состояния других IPLA sub-slots.
`IplaTargetSnapshotCache` переиспользует selection slot при неизменном попадании,
identity и snapshot/fingerprint до изменения entity/slot/stack; placement rotation/quad
входят в validity token. Кэш очищается при logout.

В generic session хранится только immutable `ItemInspectionWorldTarget`: typed source
`IPLA_WORLD`, dimension + immutable BlockPos + slot + incarnation UUID, snapshot,
hit position/distance и validity token. Объекты IPLA остаются только в `client/integration/ipla`.
Inventory sessions сохраняют source `INVENTORY`. World activation повторно raycast-ит
и сверяет identity/revision до общего `ItemInspectionClient.openWorldInspection`.
Затем viewer удерживает snapshot независимо от удаления исходного предмета.
Новых пакетов, вызовов pickup/remove/use/rotate/move в production integration нет.

## EII2: HUD и владение вводом

Prompt использует тот же min-axis transform от reference canvas 1920×1080, что
Echo Player HUD: left=24, bottom=32, height=36 reference pixels. Forge RenderGui.Post
рисует его отдельно от chat/actionbar. Label берётся из KeyMapping и ru/en локализации.
Smoothstep fade/slide занимает 140 ms; 100 ms grace относится только к видимости,
active target пропадает сразу при потере попадания. Экран/overlay/скрытый HUD,
неживой player, spectator и чужая camera entity полностью блокируют взаимодействие.
Thought Space использует свой Screen. Concentration проверяется через публичный
ConcentrationServiceBridge; dialogue/cutscene — условно загруженные typed checks
существующего hudSuppressed, cutscene detached camera также блокируется общей проверкой.
Дополнительный cancelable `WorldInspectionPermissionEvent` позволяет foreground owner
наложить veto, не завися от IPLA.

`InputEvent.Key` в реальном KeyboardHandler вызывается после `KeyMapping.click`,
но до следующего `Minecraft.handleKeybinds`. На press EII revalidate-ит цель и
синхронно открывает viewer; общий API вызывает `KeyMapping.releaseAll`, обнуляя
clickCount vanilla inventory до его обработки. Screen не успевает открыться.
Без valid target очередь не трогается. MouseButton.Pre отменяется только при
успешном открытии. Обработчики LOWEST дают foreground системам приоритет.
Esc возвращает через существующий popGuiLayer к null parent и штатному mouse grab.

`/echo_item_inspection status` показывает source/target metadata открытой session,
а вне viewer — последний target, его hit/distance/fingerprint, actual binding,
prompt visibility/active и typed diagnostic. Последняя цель явно помечена как lastIdentity;
это debug history, не разрешение на взаимодействие. Диагностики rate-limited, не каждый frame.

## EII2: проверки

Unit tests покрывают fade/grace/foreground clear/reversal, safe margins для четырёх
разрешений и независимость физических координат от GUI Scale. Отдельный opt-in
`-PinspectionIplaSmoke=true` включает реальные IPLA/Architectury/Concentration
в dev runtime и использует IPLA ItemPlacePayload.receive для расстановки 102 предметов.
Проверки используют публичный Forge key hook после той же enqueue-стадии, что
KeyboardHandler, считают открытия InventoryScreen и проверяют опустошённую очередь E.
Изоляция, отсутствие reflection и отсутствие классов IPLA/Architectury в production
JAR контролируются architecture/artifact checks. Обычный dev run не содержит IPLA
и проверяет optional classloading; dedicated GameTests запускаются без клиентских модов.

Проверено 17.10.2026: 79 JUnit, 18 Doctor и 6 dedicated GameTests — PASS;
client inventory regression без IPLA — PASS; client IPLA smoke с настоящими
IPLA/Architectury/Concentration — PASS. Snapshot NBT/name/damage/CMD/glint совпал
с размещённым стеком. На inspect E InventoryScreen не открывался ни разу;
серверные identity, stack, rotation/quad и inventory не изменились.
Detached camera и foreground veto блокировали activation, удаление до/после open безопасно.
GUI Scale 1/2/3/4/Auto дал одинаковую физическую точку (24, 1012) при 1920×1080;
pixel checks прошли также при 1280×720, 2560×1440 и 3440×1440.

При 102 IPLA objects: один spatial ray/frame, дополнительный свежий ray на activation;
неподвижное попадание переиспользует slot (0 selection candidates), движущаяся камера
проверяет максимум 24 слота одного блока. В dev microbenchmark после 200 warmup queries
на 1000 queries: stationary ≈3.0 μs и 1350 allocated bytes/query; camera jitter + query
≈5.5 μs и 1383 bytes/query. Это измерение текущего dev-клиента, не обещание FPS.
Исходные integration evidence: `build/reports/ipla-client-0.2.0.json`,
`inventory-client-0.2.0.json`, `dedicated-server-0.2.0.log`.
Тесты используют отдельные dev worlds; отсутствующий save копируется из GameTest seed,
без session.lock. Production Prism world и его контент не изменяются тестовым стендом.

### Размер плашки — 0.2.2

Client config хранит `promptScale` (0.5–3), `promptMinWidth` (0–1200, 0 auto)
и `promptHeight` (24–120). Локальное дерево `/echo_item_inspection prompt`
изменяет значения без перезапуска и сохраняет Forge config. `reset` возвращает
1/0/36. Layout учитывает размер framebuffer, сохраняет safe margins и ограничивает
размер доступной областью; ширина не может обрезать текст. Размер модели не затронут.

Проверено 17.10.2026: build, 83 JUnit, source-shape checks и real IPLA client smoke — PASS.
Клиент исполнил зарегистрированные команды scale/width/height/reset, проверил
сохранённый TOML, отказ для scale 99 без мутации и реальный оранжевый пиксель
при размере 540×90 на 1920×1080. Проверки всех GUI Scale и разрешений также прошли.
Evidence: `build/reports/prompt-size-0.2.2.log`, `build/reports/ipla-client-0.2.2.json`.
SRP review: `InspectionClientCommands.register` остаётся одним декларативным деревом
регистрации команд; добавленная ветка делегирует настройки отдельному классу.
