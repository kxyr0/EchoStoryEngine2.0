# Player Actor persistence

Core содержит durable request/result/lease contracts и idempotency ledger.

Forge W1 runtime хранит active command state in-memory. DTO boundary подготовлена контрактами `PlayerActorCommandId`, `PlayerControlLeaseId`, `PlayerActorSnapshot` и `PlayerActorResult`; полноценный SavedData recovery должен сохранять active leases, active movement и interaction receipts до dispatch.
