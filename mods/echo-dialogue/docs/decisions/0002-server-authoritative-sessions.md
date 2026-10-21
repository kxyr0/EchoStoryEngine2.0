# ADR 0002: Server-Authoritative Sessions

Status: accepted for D1.

Сервер владеет session state, revision и progression. Клиент получает только presentation read model и отправляет request advance.

Reason:

- нельзя доверять клиенту выбор следующей строки;
- stale revision должен предотвращать duplicate advance;
- final completion должен быть server result.
