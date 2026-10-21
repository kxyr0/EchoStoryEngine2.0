# Threading

Adapter предполагает вызовы из server-scoped Story extension lifecycle.

Правила:

- `EchoDialogueServiceLookupEvent` синхронный.
- Lookup выполняется для конкретного `MinecraftServer`.
- Adapter не создаёт executor, thread pool или background worker.
- Observations закрываются при `DialogueBridgeSession.close`.
- Локальная map terminal observations ограничена `MAX_OBSERVATIONS = 1024`.
- Adapter не хранит static mutable registry.

Если Forge/Story вызовет handler не на server thread, adapter не создаёт собственную синхронизацию и полагается на
контракт public Story/Dialogue host lifecycle.
