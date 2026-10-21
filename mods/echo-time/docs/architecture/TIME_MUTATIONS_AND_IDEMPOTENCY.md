# Mutations and idempotency

`Time.Set` becomes a typed `TimeSetRequest`; `Time.Add` becomes a `TimeAddRequest`. Both require `TimeMutationId`, `TimeCrossingPolicy`, and `TimeRequestSource`. The source stores module, system, immediate owner, root owner, source resource, source line or command index, and reason. The mutation ID is stored beside that provenance in `TimeMutationReceipt`; no untyped metadata map is used.

`TimeMutationLedger` is checked before changing the clock. Reusing an ID with the same signature returns `DUPLICATE` and the original before/after instants. Reusing it with a different signature returns `IDEMPOTENCY_CONFLICT`. Applied and rejected receipts are encoded in the same `echo_time_clock` SavedData record as the canonical instant.

An accepted mutation updates the instant and receipt together in the same SavedData image. Before a crossed Trigger target starts, the adapter writes `PENDING` and synchronously saves the dimension storage; that flush contains both the new clock/receipt and the pending delivery. Echo Time performs a final synchronous save before returning success. Therefore recovery cannot observe a saved `09:50` without its `Time.Add` receipt, or a dispatched target without its delivery receipt. Overflow and crossed-event-limit failures do not partially move the clock.

Default policies are deliberately different:

- `Time.Set(..., crossed: SKIP_CROSSED)` does not synthesize skipped story events.
- `Time.Add(..., crossed: FIRE_CROSSED)` publishes all indexed occurrences in `(old, new]`.

The maximum number of crossed events is configured as `1024`. The schedule preflights the count and returns `CROSSING_LIMIT_EXCEEDED` without changing the clock; it never returns a partial occurrence list.

Pause is lease-based. A Story execution or Dialogue session owns its own `TimePauseOwner`; resume releases only that owner. Waits use durable `TimeWaitId` entries with an absolute target. Natural advancement and forward mutations publish `EchoTimeWaitCompletedEvent`; the owning adapter removes the wait only while completing its durable continuation.
