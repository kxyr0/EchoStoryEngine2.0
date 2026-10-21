# Trigger Wand: diagnostics

UI показывает stable code и короткое русское сообщение. Stack trace не выводится в chat.

Основные коды:

- `DEBUG_PERMISSION_DENIED` — у игрока нет permission level 2.
- `DEBUG_TRIGGER_WAND_NOT_HELD` — wand не находится в руке.
- `DEBUG_SELECTION_REVISION_STALE` — клиент отправил устаревшую revision.
- `DEBUG_SELECTION_DIMENSION_CHANGED` — новая точка в другом dimension.
- `DEBUG_SELECTION_DISTANCE_INVALID` — точка слишком далеко от игрока.
- `DEBUG_SELECTION_INCOMPLETE` — Enter на неполной форме.
- `DEBUG_REQUEST_IDEMPOTENCY_CONFLICT` — request ID повторно использован с другими аргументами.
- `DEBUG_TRIGGER_ID_INVALID` — namespace/path не проходит правила TriggerId.
- `DEBUG_TRIGGER_VALIDATION_FAILED` — parser или binder вернул diagnostics.
- `DEBUG_TRIGGER_FILE_WRITE_FAILED` — не удалось записать файл.
- `DEBUG_TRIGGER_RELOAD_FAILED` — reload не применил catalog.
- `DEBUG_TRIGGER_CATALOG_VERIFY_FAILED` — ожидаемый TriggerId не найден после reload.
- `DEBUG_TRIGGER_ROLLBACK_FAILED` — cleanup после failure не удался.

SUCCESS означает, что сервер получил catalog entry после reload. Отправленный packet или записанный файл отдельно
не считаются успехом.
