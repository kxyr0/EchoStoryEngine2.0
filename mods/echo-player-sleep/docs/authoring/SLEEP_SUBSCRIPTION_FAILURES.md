# Sleep Subscription Failures

Если session создана, но точная подписка отсутствует, Sleep не закрывает глаза и не двигает игрока. Adapter ждет claim
до 20 ticks, затем безопасно будит игрока, отменяет текущую session и оставляет arm активным для повторной попытки.

Основные diagnostics:

- `PLAYER_SLEEP_STORY_SUBSCRIPTION_NOT_FOUND`
- `PLAYER_SLEEP_STORY_SUBSCRIPTION_CONFLICT`
- `PLAYER_SLEEP_STORY_CLAIM_WAITING`
- `PLAYER_SLEEP_STORY_CLAIM_TIMEOUT`
- `PLAYER_SLEEP_STORY_CLAIM_CONFLICT`
- `PLAYER_SLEEP_STORY_CLAIM_OWNER_MISMATCH`
- `PLAYER_SLEEP_STORY_CLAIM_RECOVERY_FAILED`
- `PLAYER_SLEEP_STORY_START_FAILED`
- `TRIGGER_SLEEP_PROVIDER_UNAVAILABLE`
- `TRIGGER_SLEEP_SCENARIO_INVALID`
- `TRIGGER_SLEEP_SELECTOR_STALE`
- `TRIGGER_SLEEP_EVENT_STALE`
- `TRIGGER_SLEEP_EVENT_DUPLICATE`
- `TRIGGER_SLEEP_CONTEXT_INHERITANCE_FAILED`
- `TRIGGER_SLEEP_SUBSCRIPTION_IDEMPOTENCY_CONFLICT`
- `TRIGGER_SLEEP_RESTART_RECOVERY_FAILED`

При конфликте нескольких claiming subscriptions Story не выбирается по порядку регистрации, имени файла или случайно.
Ни один Story не должен получить session owner.
