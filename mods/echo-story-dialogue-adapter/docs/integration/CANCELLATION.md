# Cancellation Flow

`DialoguePlayActionHandler.cancel`:

1. Проверяет action type.
2. Получает `DialogueService` для активного сервера.
3. Строит deterministic `DialogueSessionId` из `ActionExecutionId`.
4. Вызывает `DialogueService.cancel`.
5. Закрывает локальную terminal observation для этого action.

Adapter не мутирует Dialogue internals напрямую. Отмена идёт только через public service.

`SERVICE_UNAVAILABLE` возвращает rejected cancellation. Остальные public statuses считаются принятым cancellation request,
потому что authoritative state находится в Echo Dialogue.
