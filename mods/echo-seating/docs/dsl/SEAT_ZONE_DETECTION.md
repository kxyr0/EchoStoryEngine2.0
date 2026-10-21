# Seat Detection

Поддерживаемые политики:

- `RIDING_ANCHOR`: любой подтвержденный riding anchor внутри box может дать generic `SeatedIn`.
- `EXACT_SLOT_ONLY`: событие публикуется только при совпадении с authored slot.

Источник истины: server-side riding state. Client pose не используется.

Запрещено: block scan каждый tick, furniture BlockId classification, reflection и per-mod furniture imports.
