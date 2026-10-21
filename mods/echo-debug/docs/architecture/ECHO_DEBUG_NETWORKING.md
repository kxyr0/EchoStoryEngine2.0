# Echo Debug networking

Channel:

```text
echo_debug:main
```

Packets:

- `ServerboundSelectionActionPacket` — V, Shift+V, Enter, clear/undo operation variants.
- `ServerboundCreateTriggerPacket` — request на создание source.
- `ClientboundSelectionPacket` — authoritative selection snapshot.
- `ClientboundCreationResultPacket` — final success/failure ACK.
- `ClientboundAreaCatalogPacket` — immutable catalog area snapshots.

## Idempotency

Core ledger `TriggerWandRequestLedger` хранит последние request IDs для selection operations.
Forge ledger `TriggerCreationLedger` защищает create trigger от повторного packet.

Повтор с теми же аргументами возвращает прежний result. Повтор с другими аргументами возвращает
`DEBUG_REQUEST_IDEMPOTENCY_CONFLICT`.

## Thread ownership

Network thread только decode/enqueue. Изменение selection и создание файлов выполняются через main-thread handlers
и server-side services.
