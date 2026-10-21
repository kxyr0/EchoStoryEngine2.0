# EE1: архитектура и проверка

## Решение аудита

Выбран CASE B: отдельный `echo-events`. Существующий TriggerEvent представляет
ванильные/геометрические сигналы с `Map<String,String>`, а TriggerEventSelector
описывает частные sleep/seating/time/visibility подписки. У них нет общего
типизированного payload, журнала occurrences и квитанций на каждого подписчика.
Расширение этих контрактов превратило бы их в несовместимую универсальную шину.

`echo-events` владеет только фактом события, индексом подписок, очередью и
квитанциями. Trigger компилирует реакции. Модули Dialogue, Scene, Cutscene и
Animation публикуют факты на своих переходах; `echo-events-integration`
адаптирует существующие Navigation/NPC/Animation контракты и
вызывает публичные сервисы действий. Forge EventBus используется на границе
модулей, подписки контента обрабатывает собственный индекс.

## Контракты

Стабильный EventKind хранится как `echo:dialogue/completed`, без имён Java-классов.
Каталог содержит 17 видов, author-facing predicate, durability, scope и фильтры.
Payload — sealed интерфейс с immutable records. Audience — Player(UUID) либо
World; отсутствие игрока не подменяется текущим/ближайшим игроком.

OccurrenceId определяется модулем, execution, transition и видом события.
Envelope содержит resource, execution, root, caller, revision, время и Origin.
Повтор одного перехода подавляется; несовместимое повторное использование ID
отклоняется. Новое исполнение создаёт отдельный occurrence.

## Порядок и сохранение

Публикация добавляет очередь и не вызывает реакции синхронно. Серверный END tick
на LOWEST обрабатывает максимум 128 доставок. Внутри очереди порядок задают
nextTick, исходный tick, occurrence ID и subscription ID. Повторный вход запрещён.

Durable виды: Dialogue Choice/Completed/Cancelled/Failed, Scene terminal,
Cutscene terminal, Observation Completed и NPC Died. Остальные transient.
Transient history ограничена 512 элементами, очередь — 2048 доставками.
Переполнение transient учитывается счётчиком dropped.

Журнал `<world>/echo/events/journal.ee1` сохраняет occurrences, отдельные
subscription receipts, retry/failure, once receipts и causal context. Формат
имеет schema, checksum, fsync временного файла и atomic replace. Повреждение или
неподдержанная schema останавливают загрузку; пустой журнал взамен не создаётся.

Pending deliveries сохраняют исходные subscription ID, definition revision и
фильтры. Trigger архивирует определения действий по ID/revision, поэтому reload
не меняет уже поставленную реакцию. Ошибка reload оставляет действующий каталог.

Dialogue сохраняет явный outbox в том же checkpoint, что и переход. В outbox
зафиксированы факт и eligible subscriptions до асинхронного сохранения. После
commit он переносится в общий журнал; восстановление повторяет перенос, а
occurrence ID подавляет дубли. Старый checkpoint без outbox не создаёт события.
Started при recovery не воспроизводится. Scene хранит definition snapshot,
execution checkpoint и принадлежность игроку в отдельных атомарных файлах.

Действия получают детерминированный token на delivery и номер action. Scene
использует его как execution ID; Dialogue как session ID, Story как execution ID.
Квитанция ACCEPTED записывается после durable принятия целевым сервисом.
Observation Activate/Enable — идемпотентные установки состояния.

RETRY: четыре повторных попытки с интервалами 20/40/80/160 ticks. Отсутствующий
игрок даёт WAITING_CONTEXT без расхода retry attempts и проверяется через 200
ticks. Ошибка одного подписчика не расходует квитанцию другого. Ограничение
каскада — 32 принятые реакции на root/kind/resource/subscription, включая restart.

RESTORE/MIGRATION/DEV_SETUP политики не публикуют gameplay occurrences.
Прямые Scene orchestration и точные animation marker callbacks сохранены.
Time crossing сохраняет существующие occurrence ID/receipts и не получает
параллельную систему идентичности.

Observation сохраняет прогресс и подготовленный completion outbox атомарно в
`<world>/echo/observations/progress.nbt` перед публикацией. Vanilla SavedData
сохраняется как совместимое зеркало. Активация/включение подтверждаются после
этого commit; неуспешная запись откатывает изменение в памяти. Legacy progress
без явного outbox не превращается в новые completed events.

Исторический resource в факте сохраняется. Канонический sourceResource контекста
реакции и переименованные цели разрешаются перед вызовом сервиса через
CanonicalResourceLookupEvent существующего Echo Save Migration и его AliasGraph.
Исторические occurrence/subscription IDs и квитанции сохраняются. Отдельная
таблица aliases в Events не создаётся. Scene также хранит подготовленный outbox
в checkpoint; изменение каталога между source commit и перезапуском не меняет
исходный список подписчиков.

## Диагностика и tooling

`/echo_events status`, `watch [predicate]`, `history`, `inspect <occurrence>` и
`subscriptions` требуют уровень оператора 2. Watch ограничивает вывод на tick.
Inspect показывает состояние доставки каждого durable подписчика.

Content Doctor разбирает TriggerResource настоящим EchoScript parser/binder,
проверяет source/action targets, choice IDs, observation points, дубли и
доказуемые completion cycles. Каталог VS Code экспортируется из
EventCatalogDescriptor через EventCatalogExport, новый language server не нужен.

## Подтверждение и границы

Логи проверок находятся в `echo-events/build/ee1-check-latest.log` и
`echo-events-integration/build/ee1-*.log`. Они являются источником результата;
наличие исходников или JAR само по себе не означает готовность к установке.
Отчёт при передаче должен отдельно перечислять успешные проверки, незакрытые
пункты EE1, установленные JAR и фактический live smoke. Непройденные проверки
не разрешают замену файлов Prism.

## Итоговая приёмка EE1 — 20.10.2026

### Сборка и модули

Финальная общая production-сборка: **BUILD SUCCESSFUL**, 571 Gradle tasks
(150 executed, 421 up-to-date), 3m 7s. Лог:
[ee1-production-build.log](../../echo-events-integration/build/ee1-production-build.log).
После неё отдельно исправлен только чистый CLI-адаптер Content Doctor;
его повторные build/check/test/installDist также успешны:
[ee1-doctor-final-build.log](../../echo-events-integration/build/ee1-doctor-final-build.log).

CASE B реализован отдельным ядром echo-events и Forge-модом echo-events-integration.
Trigger отвечает за parser/binder, каталог и архив реакций. Dialogue/Scene/Observation
сохраняют source outbox; Cutscene и Animation публикуют свои реальные переходы.
Story предоставляет durable acknowledgement запуска. Save Migration разрешает
переименования существующим AliasGraph. NPC Animation Adapter, Story Dialogue
Adapter и Background NPC пересобраны с изменёнными API. Doctor остаётся CLI,
его JAR не устанавливается как Minecraft-мод. EchoTool 1.10.0 содержит экспорт
каталога, подсказки и диагностику событий; check/smoke/package успешны.

В XML-результатах затронутых проектов: **862 тестов, 0 failures, 0 errors**. Это сумма результатов проектов, включая существующие тесты и architecture suites, а не число новых тестов EE1.

### Реализованный каталог

| Предикат | Stable kind | Durable | Scope |
|---|---|---|---|
| Dialogue.Started | `echo:dialogue/started` | нет | PLAYER |
| Dialogue.Choice | `echo:dialogue/choice` | да | PLAYER |
| Dialogue.Completed | `echo:dialogue/completed` | да | PLAYER |
| Dialogue.Cancelled | `echo:dialogue/cancelled` | да | PLAYER |
| Dialogue.Failed | `echo:dialogue/failed` | да | PLAYER |
| Scene.Started | `echo:scene/started` | нет | PLAYER |
| Scene.Completed | `echo:scene/completed` | да | PLAYER |
| Scene.Cancelled | `echo:scene/cancelled` | да | PLAYER |
| Scene.Failed | `echo:scene/failed` | да | PLAYER |
| Cutscene.Started | `echo:cutscene/started` | нет | PLAYER |
| Cutscene.Completed | `echo:cutscene/completed` | да | PLAYER |
| Cutscene.Cancelled | `echo:cutscene/cancelled` | да | PLAYER |
| Observation.Completed | `echo:observation/completed` | да | PLAYER |
| Npc.Arrived | `echo:npc/arrived` | нет | EXECUTION |
| Npc.Died | `echo:npc/died` | да | WORLD |
| Animation.Marker | `echo:animation/marker` | нет | EXECUTION |
| Animation.Completed | `echo:animation/completed` | нет | EXECUTION |

Execution-scoped NPC/Animation события наследуют доступный контекст исполнения;
при его отсутствии player context не выдумывается.

### Авторский EchoScript

Ниже реальный синтаксис действующего parser/binder, использованный в трёх
исходниках приёмочного набора. Полный пример с Observation.Activate, Once и
фильтрами: [EE1_AUTHORING.md](EE1_AUTHORING.md).

```csharp
class FatherFinished : TriggerResource {
    private void Init() {
        Id("jdh:father_finished");
        When(Dialogue.Completed("jdh:father"));
    }
    private void Then() { Scene.Play("jdh:father_leaves"); }
}

class TruthChosen : TriggerResource {
    private void Init() {
        Id("jdh:truth_chosen");
        When(Dialogue.Choice("jdh:father", "tell_truth"));
    }
    private void Then() { Scene.Play("jdh:father_angry_exit"); }
}
```

### Durability и предел подтверждения

12 тестов ядра проверяют журнал на диске, повторную загрузку, отдельные receipts,
сбой сохранения receipt после принятия действия, pin исходных подписчиков в
source outbox, hot reload, per-player once, retry, causal budget после restart,
сохранение transient bounds и подавление restore/migration/dev публикаций.
Нативные тесты дополнительно проверяют Dialogue failure/commit и durable acceptance
первой записи с revision=0, а также Scene и Observation disk checkpoints/outbox.

Из вывода disk-recovery теста:

```text
Occurrence: evt-a0a74c8f-5d2b-35c2-b0f7-64106d451e0a
Delivery receipts: 2 (два подписчика одного occurrence)
Action execution token: event-reaction-57494688-270d-3701-871b-22801b93e5af
Duplicate actions: 0 (проверка идемпотентного test port)
```

Receipt идентифицируется парой subscription ID + occurrence ID и сохраняет
ACCEPTED/FAILED, attempts и diagnostic. Тест принудительно роняет запись журнала
после принятия действия; восстановленная доставка получает прежний token.
Это подтверждает протокол ядра с идемпотентным портом. Полный сценарий аварийного
завершения JVM после реального Scene.Play в запущенном мире не выполнялся;
результат test port не объявляется доказательством такого live-сценария.

### Производительность

Измерение `EventDispatcherTest`:

```text
subscriptions: 5000
candidates per test event: 1
publish + one dispatch: 253000 ns (0.253 ms)
```

Это одиночный замер ядра с in-memory journal store, без Forge tick и fsync;
он не является SLA или замером дисковой доставки. Проверка кандидатов
подтверждает выбор по kind/resource. Domain-only TriggerResource исключены
из runtimeView старого сканера геометрических сигналов.

### Content Doctor

Настоящий CLI проверил **3 подписки**, 8 файлов, 8 ресурсов: **errors=0, warnings=4**.

Набор содержит Dialogue.Completed, Dialogue.Choice(tell_truth) и
Observation.Completed(house#photo), реальные диалог, сцену и наблюдение.
Это отдельный синтетический приёмочный набор, не аудит всего контента сезона.
Четыре предупреждения относятся к поддерживаемому legacy-синтаксису диалога
Build/Start/new Node; событийные ссылки разрешены.

Исходники: [acceptance-content](../../echo-events-integration/build/acceptance-content).
Результат: [ee1-doctor-acceptance.json](../../echo-events-integration/build/ee1-doctor-acceptance.json).
Отдельные негативные тесты проверяют отсутствующие source/target/choice,
дубли подписок, циклы и несовместимость World event с player action.

### Debug

Пример цепочки приёмочного контента (не live trace):

```text
Dialogue.Completed("jdh:father")
  → subscription jdh:father_finished#When, pinned definition revision
  → Observation.Activate("jdh:house"), action ordinal 0
  → Scene.Play("jdh:father_leaves"), action ordinal 1
  → ACCEPTED receipt после durable acknowledgement целей
```

В игре проверка доступна через `/echo_events watch Dialogue.Completed`,
`history`, `inspect <occurrence>` и `subscriptions`. History ограничена;
отсутствующий игрок оставляет доставку WAITING_CONTEXT без расхода retries.

### Source Shape

Во всех 14 затронутых модульных репозиториях: **production Java >180 logical
LOC = 0; leaf packages >7 Java files = 0**. Использовано то же определение
logical lines, что в build gates: без пустых строк и строк комментариев.
Гейты не ослаблены. Детали и тестовые количества:
[ee1-verification.json](../../echo-events-integration/build/ee1-verification.json).

### Deployment

Установлено 13 Minecraft-модов в PrismLauncher / Форма Мысли.
После установки SHA-256 target совпадает с source для всех 13 JAR;
**duplicate active mod IDs = 0**. Старые JAR перемещены в резервную копию
вне активной папки mods. Полные пути, версии, хеши и резервная копия:
[ee1-deployment.json](../../echo-events-integration/build/ee1-deployment.json).

| Установленный JAR | SHA-256 |
|---|---|
| echo-events-0.1.0.jar | `82291c190555ce301b7ad9f320ef677436527cf7c6af1f51d961c4121cb0f969` |
| echo-events-integration-0.1.0.jar | `2db9749753579c64c6d7756c5472e347459eab2dddad2eb7dfb7df2f5dd5500d` |
| echo-trigger-0.6.0-all.jar | `92c299d79ff226e0f3cc71e94a99ee29c9ff75f5f57a21887852eef568c31169` |
| echo-dialogue-0.16.0-all.jar | `743c7064b5d5e9d86232583ed137668fc3cfcf051fc5dfb452ff0a6b7d2969dc` |
| echo-scene-0.3.0-all.jar | `db0ccae724dad6ae74b9ae1304f3da05b31f908720d4649c79008d0727b75159` |
| echo-cutscene-0.3.0-all.jar | `60f3c19f8e8c55c570791e8650befc3793fed5bef3e4916fad7e1f43f1d0d087` |
| echo-animation-0.9.0-SNAPSHOT-all.jar | `37463f690a11593bc3cb77eddd412ee459babaf683dcb59f0319debcf3eea132` |
| echo-npc-animation-adapter-0.4.0-SNAPSHOT.jar | `e4fd24b016b29d5f56410ac6d5aaa4e4b533cd118affd02092538ee8940be731` |
| echo-story-engine-1.12.0-all.jar | `996122893118358aaa6b17390e332ac37348534e4d17e1a355dcb3e094411cac` |
| echo-story-dialogue-adapter-0.3.0-SNAPSHOT.jar | `2848fa546ea3d7acb47788b565e7e8c8daba601dc1db39eeecee736032cd2d89` |
| echo-observation-1.4.0-all.jar | `4acf3b4afdfa493292a7be0f0218d7a56011b10eba32ecb79f62b40d55b9d947` |
| echo-save-migration-0.2.0-all.jar | `beb21bd11399f7e94a3c3d582ebde9b565875e118834ec84ad703f1644b695f3` |
| echo-background-npc-forge-1.20.1-0.4.2-all.jar | `6266251ab75e0e99ffef3e60d7327eb604d620f7bf80b8821928eb3ed98acb62` |

### Live Smoke

**LIVE_SMOKE_NOT_RUN**. Minecraft/Prism не были запущены. Клиентское подключение,
отрисовка и цепочка событий в игровом мире после установки не проверены.
Автоматические тесты и установка JAR не заменяют эту проверку.

## Inline-подписки — 21.10.2026

В DialogueResource, StoryResource и TriggerResource добавлен единый
`When(predicate, id: "stable_name", once: true) { actions; }`. Заголовок и
действия находятся в одном блоке; отдельный Then нужен только старому синтаксису
Trigger. Несколько блоков в ресурсе имеют независимые стабильные subscription IDs
с префиксом профиля/владельца. Перестановка блоков не меняет идентичность.

Это декларации уровня ресурса, активные при загрузке на сервер. Они не зависят
от того, запущена ли сейчас соответствующая история или диалог. При компиляции
Story блоки не попадают в OnStart; в Dialogue они не превращаются в Node.
Загрузка подписок читает активные SERVER_DATA ресурсы через Minecraft
ResourceManager; pending definitions продолжают храниться в прежнем архиве EE1.
Новые блоки используют существующие dispatcher, receipts и integration actions.
Произвольный переход к Node и полный набор Story intrinsics в When не добавлены.

Восемь новых тестов прошли: native Story и Dialogue compiler; три проверки inline
binder; canonical reload с ошибкой второго обработчика; реальная загрузка
Story/Dialogue/Trigger из PathPackResources + MultiPackResourceManager с LKG;
полный SourceCatalogScanner/Doctor на реальных исходниках трёх профилей.
[Результаты](../../echo-events-integration/build/ee1-inline-verification.json).

Общая финальная сборка успешна: 333 Gradle tasks, 61 executed, 272 up-to-date,
1m 35s. [Лог](../../echo-events-integration/build/ee1-inline-production-build.log).
Source Shape: Java >180 = 0, leaf packages >7 = 0. Checkstyle, PMD,
архитектурные тесты и build затронутых модулей пройдены.

CLI Doctor проверил смешанный приёмочный набор: 11 файлов и 11 ресурсов,
содержащих шесть подписок (три прежние и три inline): errors=0, warnings=4.
Предупреждения относятся к legacy Build/Start/new Node исходного диалога.
[JSON](../../echo-events-integration/build/ee1-inline-doctor-acceptance.json).
EchoTool 1.11.0: check/smoke/package успешны; добавлены диагностика inline
заголовка и snippet `when-event`.

Установлено четыре обновлённых JAR с резервной копией, SHA-256 source/target
совпадают, duplicate active mod IDs=0.
[Manifest](../../echo-events-integration/build/ee1-inline-deployment.json).

| JAR | SHA-256 |
|---|---|
| echo-events-integration-0.1.1.jar | `c75f0aa5341f3320af0b058abc68a49c60eb3ea0ad7f67c69beb2d16ec93088e` |
| echo-trigger-0.7.0-all.jar | `26f9d084f3079184551a3ab6245cb818018c5ff4d8418cc8fee79b5cb56b5838` |
| echo-dialogue-0.17.0-all.jar | `5b03d09b0675f4c9697961e4fba708f71158e685e1cb3e305ca9d2fcd7e26179` |
| echo-story-engine-1.13.0-all.jar | `6fc749822ef7546952ae3ae754ef3191f7106da6e72030c9f203f3cbd2da3245` |

**LIVE_SMOKE_NOT_RUN**: запуск Minecraft и проверка обработчика в игровом мире не выполнялись.
