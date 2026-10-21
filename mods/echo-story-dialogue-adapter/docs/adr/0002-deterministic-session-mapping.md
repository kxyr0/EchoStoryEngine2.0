# ADR 0002: Deterministic Session Mapping

Статус: принято.

Решение: `ActionExecutionId` маппится в `DialogueSessionId` алгоритмом:

`story-action/` + `ActionExecutionId.asString()`

Причины:

- same Story WAITING action после restart находит ту же Dialogue session;
- pre-dispatch crash window может безопасно повторить start;
- adapter не нуждается в собственном persistence;
- duplicate start не создаёт новую random session.

Если Echo Dialogue сообщает mismatch или conflict, adapter не подменяет session id и не стартует replacement.
