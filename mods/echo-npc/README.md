# Echo NPC

Echo NPC - независимый Forge 1.20.1 мод для N2: durable NPC runtime без Story,
Dialogue, animation, navigation и AI-систем.

## Идентичность

- Root project: `echo-npc`
- Mod ID: `echo_npc`
- Maven group: `io.github.kxyr0.echonpc`
- Version: `0.6.0-SNAPSHOT`
- Minecraft: `1.20.1`
- Forge: `47.4.10`
- Java: `17`
- License: решение не принято; `mods.toml` фиксирует `License decision unresolved`.

## Модули

- `npc-contracts` - публичные ID, запросы, результаты и read models без Forge.
- `npc-core` - immutable definitions, atomic catalog, actor desired state и
  repository port.
- `npc-persistence` - Actor Save Schema V4, JSON codec и file-backed repository.
- `npc-echoscript-adapter` - парсинг `.npc.es` через публичные EchoScript artifacts.
- `npc-forge-api-1.20.1` - typed Forge bridge для доступа других модов к `NpcService`.
- `npc-forge-1.20.1` - Forge runtime, commands, reload listener, entity и renderer.

## Быстрый старт

```powershell
.\gradlew.bat projects --console=plain
.\gradlew.bat prepareEchoScriptArtifacts --console=plain
.\gradlew.bat check --console=plain
.\gradlew.bat :npc-forge-1.20.1:runGameTestServer --console=plain
```

`prepareEchoScriptArtifacts` публикует публичные artifacts EchoScript из sibling
репозитория `../echo-script` в `build/external-maven`. Maven Local не требуется
для обычной automated acceptance; он включается только флагом
`-PenableMavenLocal=true`.

## Команды в игре

- `/echo_npc health`
- `/echo_npc definitions`
- `/echo_npc actors`
- `/echo_npc actor <actorId>`
- `/echo_npc spawn <actorId> <definitionId> [pos]`
- `/echo_npc teleport <actorId> <pos>`
- `/echo_npc despawn <actorId>`
- `/echo_npc skin get <actorId>`
- `/echo_npc skin set <actorId> <skinId>`
- `/echo_npc operations`
- `/echo_npc operation <operationId>`
- `/echo_npc blocked`
- `/echo_npc recovery retry <actorId>`
- `/echo_npc persistence status`
- `/echo_npc diagnostics [limit]`

Пример:

```text
/echo_npc spawn sample:father_actor sample:father ~ ~ ~
/echo_npc operation enpc-op-1
/echo_npc teleport sample:father_actor ~2 ~ ~2
/echo_npc skin set sample:father_actor sample:father/coated
/echo_npc despawn sample:father_actor
```

## Документация

- [N1 execution plan](docs/plans/N1_EXECUTION_PLAN.md)
- [N2 execution plan](docs/plans/N2_EXECUTION_PLAN.md)
- [Architecture](docs/architecture/ARCHITECTURE.md)
- [Persistence](docs/npc/PERSISTENCE.md)
- [Recovery](docs/npc/RECOVERY.md)
- [NPC authoring](docs/npc/WRITING_NPCS.md)
- [Testing](docs/development/TESTING.md)
- [Definition of Done](docs/development/DEFINITION_OF_DONE.md)
- [Current progress](PROGRESS.md)
