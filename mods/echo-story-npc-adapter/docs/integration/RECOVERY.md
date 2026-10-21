# Recovery

Recovery управляется Story. Когда Story загружает durable WAITING checkpoint,
она вызывает adapter `recover` с тем же `ActionExecutionId` и payload.

Recovery в адаптере:

- derives the same `ActorMutationId`;
- получает public `NpcService`;
- проверяет, удовлетворяет ли public NPC state нужному postcondition;
- восстанавливает observation для pending/dormant states;
- отправляет deterministic mutation, если она еще не была отправлена;
- оставляет blocked/dormant actors в ожидании вместо force-loading chunks.

Дублирующий Story completion обрабатывается Story completion sink и считается
безопасным для adapter observations.
