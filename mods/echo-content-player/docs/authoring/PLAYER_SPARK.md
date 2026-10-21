# Искра

Искра — сюжетная мана игрока. P1 хранит только durable значения и безопасно меняет их.

Инварианты:

- `0 <= current <= maximum`;
- `maximum > 0`;
- hard limit `maximum <= 1_000_000`.

Операции:

- `SetSpark`;
- `SetMaximumSpark`;
- `RestoreSpark`;
- `SpendSpark`;
- `TrySpendSpark`.

`SpendSpark` атомарен: если значения достаточно, current уменьшается и операция возвращает `SUCCESS`; если значения недостаточно, профиль не меняется и возвращается `INSUFFICIENT_SPARK`.

`SetMaximumSpark` по умолчанию использует `CLAMP_CURRENT`. Политика `PRESERVE_PERCENTAGE` доступна в core для явных сценариев.
