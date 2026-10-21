# Persistence

N2 хранит actors в world-scoped repository:

```text
<world-root>/data/echo_npc/actors/<sha256(canonical ActorId)>.json
```

Имя файла не раскрывает raw `ActorId`, но вычисляется детерминированно из
canonical namespaced id.

## Actor Save Schema V4

Schema V4 сохраняет все поля V1 и добавляет optional authored runtime state:

- `schemaVersion`;
- `actorId`;
- `definitionId`;
- `definitionFingerprint`;
- `presence`: `ACTIVE` или `ABSENT`;
- `worldId`;
- `position`;
- `rotation`;
- `entityBindingId`;
- `actorRevision`;
- `repositoryRevision`;
- `createdAtEpochMillis`;
- `updatedAtEpochMillis`.
- `activeSkinId` для `Npc.ChangeSkin`;
- `activeForm`;
- `restAttachment`.

Файл не содержит Minecraft entity UUID, numeric entity id, `Entity`,
`ServerLevel`, `MinecraftServer`, full `NpcDefinition`, `.npc.es` source, AST,
display bytes, renderer/cache bytes, animation/navigation/AI/Story or
Dialogue state.

Decoder сохраняет backward compatibility со schema V1-V3. Новая запись всегда
использует V4.

## Atomic Writes

Repository пишет sibling temp file, flushes channel, декодирует generated JSON,
сохраняет один backup только если текущий primary читается, затем делает atomic
replace when supported. Если atomic replace недоступен, используется обычный
replace в той же директории.

Primary authoritative. Backup используется только как fallback при corrupt
primary. Valid `ABSENT` primary prevents active backup resurrection.

Expected repository outcomes возвращаются explicit statuses, а не unchecked
exceptions: `FOUND`, `NOT_FOUND`, `CORRUPT`, `UNSUPPORTED_SCHEMA`, `FAILED`,
`CREATED`, `UPDATED`, `REVISION_CONFLICT`, `TOMBSTONED`, `REMOVED`.

## Queue

Forge host выполняет file I/O на одном bounded worker с capacity `64`. Worker не
вызывает Minecraft API и не mutates actor runtime. Results drain/apply happens
on server thread.
