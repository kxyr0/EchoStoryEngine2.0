# Recovery

Startup recovery запускается при `MinecraftServer` start.

```text
STARTING -> LOADING_ACTORS -> RECONCILING -> ACTIVE
                                  \-> DEGRADED
```

Host загружает bounded actor records из текущего world root, затем применяет
результаты на server thread. Один corrupt/unsupported actor не блокирует
остальные records.

## Compatibility

Для ACTIVE actor recovery требуется:

- существующий `NpcDefinitionId`;
- exact `NpcDefinitionFingerprint`;
- существующий world;
- target chunk loaded for immediate materialization.

Если definition отсутствует, actor получает `BLOCKED_DEFINITION_MISSING`.
Если fingerprint отличается, actor получает
`BLOCKED_DEFINITION_INCOMPATIBLE`. Record сохраняется без migration и без
closest-match fallback.

## Reconstruction

Loaded `EchoNpcEntity` принимается только если entity linkage matches durable
state: `ActorId + EntityBindingId`. Orphan, stale-binding и malformed entities
удаляются. Durable storage остается authority; stale entity NBT не может
восстановить actor самостоятельно.

`/echo_npc blocked` показывает actors, которые требуют reload/fix. После
совместимого resource reload можно выполнить:

```text
/echo_npc recovery retry <actorId>
```

Retry ставит recovery operation в bounded queue и не создает duplicate entity,
если actor уже корректно materialized.
