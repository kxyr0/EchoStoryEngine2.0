# PROGRESS

2026-08-30: added `/echo_npc list` with separate definition and actor identities. An Echo NPC entity removed with
the Minecraft `KILLED` reason now schedules an idempotent durable despawn, so reconciliation cannot resurrect a dead
actor. Version advanced to `0.7.0-SNAPSHOT`.

2026-08-05: implemented durable dynamic skin changes through
`NpcService.changeSkin` and the adapter-facing `Npc.ChangeSkin(npcId, skinId)`
contract. Active skin overrides are persisted in Actor Save Schema V4, applied
after form/definition appearance during materialization, synchronized to the
client without a character-specific registry, and covered by unit and Forge
GameTests. Version advanced to `0.6.0-SNAPSHOT`; NPC protocol version advanced
to `2`.

2026-08-04: added the public client-only generic body attachment boundary with
stable torso-back and hand anchors. It enables independent equipment adapters
without exposing NPC entity or renderer internals. Version advanced to
`0.5.0-SNAPSHOT`; automated verification is recorded by the consuming NSB1
adapter work.

2026-08-02: public client render extensions gained the terminal
`NpcClientRenderResult.CANCELLED` outcome for lease-based whole-NPC
concealment. Cancellation has precedence and suppresses model, name, shadow,
layers and later extensions.

Дата отчета: 2026-07-19.

## Статус

N1 принят пользователем как prerequisite для N2.

N2 реализован на уровне production code, public API evolution, persistence
module, Forge integration, commands, documentation and automated checks.
External manual acceptance after EULA and visual client checks are not claimed.

## Выполнено в N2

- Добавлен module `npc-persistence`.
- Добавлен world-scoped actor repository:
  `data/echo_npc/actors/<sha256(canonical ActorId)>.json`.
- Реализованы Actor Save Schema V1, deterministic JSON codec, temp/backup
  atomic write policy, revision conflicts, corrupt primary backup fallback,
  tombstone and removal flow.
- `npc-core` расширен desired actor state, `EntityBindingId`, repository port and
  durable runtime read model.
- Mutating public API теперь возвращает `QUEUED` + `operationId`; operation status
  доступен через public API and commands.
- Forge host получил bounded serial persistence queue, startup recovery,
  server-thread result application, blocked actors, recovery retry and
  persistence status command.
- `EchoNpcEntity` хранит minimal technical linkage:
  `ActorId + EntityBindingId + NpcDefinitionId`.
- Entity reconciliation treats durable storage as authority and does not delete
  unrelated nearby Echo NPC entities.
- N1 GameTest markers сохранены, проверки адаптированы под durable async flow.

## Изменение public API для N3

- Добавлен `ActorMutationId` как caller-supplied idempotency key для durable
  actor mutations.
- `SpawnNpcRequest` и `TeleportNpcRequest` принимают optional mutation ID;
  старые constructors без mutation ID сохранены.
- `NpcService.despawn(ActorId, ActorMutationId)` добавлен как compatibility
  overload; `despawn(ActorId)` сохранен для команд и существующих callers.
- Forge runtime использует supplied `ActorMutationId` как `operationId` и
  переиспользует уже известную operation при повторном вызове.
- Это изменение не добавляет Story-типы, Minecraft entity UUID или
  `EntityBindingId` в public actor identity.

## Проверки N2

- `.\gradlew.bat :npc-contracts:test :npc-core:test :npc-persistence:test --console=plain` - пройдено.
- `.\gradlew.bat :npc-contracts:compileJava :npc-core:compileJava :npc-persistence:compileJava :npc-forge-1.20.1:compileJava --console=plain` - пройдено.
- `.\gradlew.bat :npc-forge-1.20.1:compileGameTestJava --console=plain` - пройдено.
- `.\gradlew.bat sourceShapeCheck --console=plain` - пройдено.
- `.\gradlew.bat check --console=plain` - пройдено; production JAR verified,
  `entries=172`.
- `.\gradlew.bat :npc-forge-1.20.1:runGameTestServer --console=plain` - пройдено;
  `discovered=21`, `executed=21`, `passed=21`.
- `.\gradlew.bat build --console=plain` - пройдено.

## Проверки N1, сохраненные как baseline

- `projects` - пройдено.
- `dependencies` - пройдено; root не имеет configurations, отдельно проверены
  `:npc-echoscript-adapter:compileClasspath` и `:npc-forge-1.20.1:runtimeClasspath`.
- Unit tests для `npc-contracts`, `npc-core`, `npc-echoscript-adapter` - пройдено.
- `architectureTest` - пройдено.
- `checkWhitespace` - пройдено.
- `check` - пройдено.
- `build` - пройдено.
- `:npc-forge-1.20.1:verifyProductionJarContents` - пройдено.
- Production scans на Story/Dialogue/GeckoLib/navigation/HTTP/reflection markers -
  пустой вывод.
- Public artifact metadata - найдено в `build/integration-maven`.
- EchoScript external metadata - найдено в `build/external-maven`.
- `git diff --check` - не может быть выполнен как git-проверка, потому что каталог
  не является git repository; Git вернул `Not a git repository`.

## Manual Acceptance, не заявлено

- Обычный dedicated server после ручного принятия EULA.
- Isolated final-JAR server после ручного принятия EULA, без sibling source sets
  и Maven Local artifacts.
- Real client и isolated final-JAR client с clean stop/return to title.
- Manual spawn, duplicate, conflict, teleport, despawn, skin, missing-skin
  fallback, reload-pinning, world-switch и visual acceptance.

## Следующий milestone

N3 - Story-NPC Adapter для spawn, despawn, teleport и durable recovery.

Animation, navigation, AI, Story integration, Dialogue integration и combat
остаются отложенными за пределами N2.

2026-10-11: 0.8.0-SNAPSHOT — ES2 NPC metadata Id/Display/Skin/NameColor/DefaultForm
с inferred identity по namespace и basename. Старый Build сохранён; смешанные
профили и повторные metadata отклоняются. Все 25 ресурсов source/live после
перевода дают точно те же NpcDefinition, что и резервные legacy-файлы.
`:npc-echoscript-adapter:check` и `:npc-forge-1.20.1:jarJar` прошли.
Root check запускался: прежний EchoNpcEntityRenderer превышает sourceShapeCheck
(283 logical lines при лимите 200). Whitespace baseline исправлен без поведения.
Minecraft для ручной проверки не запускался.
