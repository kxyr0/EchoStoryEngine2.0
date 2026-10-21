# Persistence профиля игрока

Persistence формат не является domain API. Domain API — typed records из `player-contracts`.

Минимальные persisted поля P1:

- `EchoPlayerId`;
- account name;
- name mode и optional custom name;
- Spark current/max;
- normalized base skin id/model;
- template id/fingerprint;
- profile revision;
- appearance revision;
- lifecycle metadata.

При restart профиль восстанавливается по `EchoPlayerId`. Duplicate profiles для одного UUID недопустимы. Mutation ledger должен переживать restart, чтобы повтор той же mutation id возвращал прежний результат.
