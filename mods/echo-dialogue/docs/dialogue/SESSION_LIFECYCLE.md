# Жизненный цикл сессии

Core model:

- `ACTIVE`: session показывает текущую строку или список choices и ждёт явный input.
- `COMPLETED`: последняя строка подтверждена, presentation закрывается.
- `CANCELLED`: session закрыта явной отменой.
- `FAILED`: инвариант нарушен или команда не может стартовать session.

Переход:

`DialogueSessionState + DialogueInput -> DialogueTransition`

`DialogueTransition` содержит новое immutable state, optional presentation и diagnostics.

Правила:

- start открывает первую строку start node с revision `0`;
- каждый валидный advance увеличивает revision на `1`;
- advance на последней строке node открывает choice presentation, если node содержит choices;
- `SELECT_CHOICE` валиден только для текущей choice presentation и переносит session в target node;
- terminal node после последней строки завершает session с explicit `DialogueOutcomeId`;
- stale или duplicate revision не меняет state;
- wrong session ID не меняет state;
- unknown choice ID не меняет state;
- repeated advance after completion возвращает ignored result;
- вторая active session для одной audience отклоняется.

Forge host rules D4:

- `ForgeDialogueHost` создаётся на `ServerStartedEvent`;
- `ServerStoppingEvent` вызывает idempotent `shutdown()`;
- shutdown отклоняет новые операции, ждёт bounded flush accepted persistence work и пишет `EDLG-FORGE-007`;
- player logout не отменяет активную session: session остаётся durable `ACTIVE`, а presentation становится detached/offline;
- player login вызывает reattach: та же current line/choice presentation отправляется снова без увеличения revision;
- host не хранит `MinecraftServer` и не переносится между world/server instances;
- active sessions pinned к compiled definition, с которой стартовали, поэтому reload не меняет текущий cursor.

Save-before-presentation ordering:

- start сначала создаёт preview state и checkpoint, затем repository save с `expectedEmpty`;
- open presentation отправляется только после successful durable create;
- advance/cancel/complete сначала строят preview transition и checkpoint;
- choice selection сначала строит preview branch transition и checkpoint;
- update/close presentation отправляется только после successful durable update;
- при save failure, queue saturation или repository revision conflict authoritative in-memory state не меняется;
- клиент получает resync текущей durable state, если она есть.

Recovery:

- `ACTIVE` с compatible compiled fingerprint восстанавливается detached;
- `COMPLETED` и `CANCELLED` восстанавливаются как retained terminal records и не открывают overlay;
- missing compiled dialogue или incompatible fingerprint переводят snapshot в blocked recovery state с diagnostic;
- corrupt primary может восстановиться из одного backup;
- corrupt primary и corrupt backup дают явный failed load diagnostic;
- duplicate `ACTIVE` snapshots для одного audience разрешаются детерминированно: первый по `sessionId` восстанавливается, остальные блокируются diagnostic `DUPLICATE_ACTIVE_AUDIENCE_CONFLICT`.
