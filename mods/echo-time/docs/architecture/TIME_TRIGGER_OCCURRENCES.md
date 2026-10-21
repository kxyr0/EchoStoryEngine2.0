# Time trigger occurrences

`echo-trigger-time-adapter` does not implement a second Trigger engine. `TimeSubscriptionIndex` reads the existing Echo Trigger subscription catalog only when its catalog revision changes. `TimeSchedulePublisher` replaces one provider image in Echo Time; the clock evaluates indexed time buckets, not every `TriggerResource`.

`Time.At("09:50")` is stored as a daily `TimeScheduleEntry`. `Time.At(Day(2), "10:00")` is stored as a specific-day entry. A crossing uses the half-open interval `(previous, current]`, so `09:49 → 09:51` emits `09:50` exactly once even when equality never occurs. Specific absolute instants are read with a `NavigableMap` range. Same-day daily ranges use indexed submaps; multi-day ranges are bounded by the crossed-event limit before expansion.

Ordering is deterministic: absolute occurrence instant, then descending priority, then selector ID. Midnight is therefore ordered naturally. `TimeOccurrenceId` is derived from selector ID, occurrence day, time, and definition generation; daily occurrences on different days have different IDs. Backward movement emits nothing. Returning to an already delivered same-day occurrence is blocked by its durable delivery ID, while the next day's occurrence remains distinct.

Before target dispatch, `TimeTriggerDeliveryStore` synchronously saves a `PENDING` receipt. After deterministic Story or Dialogue startup it synchronously saves `DELIVERED`. Recovery replays only pending entries. Story `ExecutionId`, Dialogue `DialogueSessionId`, Trigger owner/command IDs, and delivery IDs are derived from the occurrence, subscription, and audience, making the crash window idempotent.

`FireOnlyOnce()` deliberately derives a stable delivery ID from subscription and audience rather than the daily occurrence. Trigger context includes occurrence ID, subscription ID, day, time, interval start/end, and crossing reason. NPC scheduling remains a Story concern: the adapter starts a Story and has no NPC dependency.

Replacing the index after hot reload does not perform catch-up. A newly registered time already earlier than the current clock fires only on a future crossing.

