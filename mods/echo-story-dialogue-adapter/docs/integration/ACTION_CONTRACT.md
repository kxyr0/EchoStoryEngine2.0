# Контракт `echo_dialogue:play`

Action type: `echo_dialogue:play`.

Payload обязан содержать ровно два ключа:

- `dialogueId` - public `DialogueId`, например `sample:greeting`
- `audienceId` - public `AudienceId`, обычно UUID игрока как строка

Любой отсутствующий или лишний ключ отклоняется как некорректный payload.

Outputs completion:

- `outcome`, если Echo Dialogue вернул terminal `DialogueOutcomeId`
- `dialogue.sessionId`
- `dialogue.status`
- `dialogue.terminalResult`, если Echo Dialogue вернул terminal result
- `dialogue.cancellationReason`, если сессия отменена с причиной
- `dialogue.blockedReason`, если recovery заблокирован
- `dialogue.failureReason`, если adapter не получил usable session view

Story scripts должны использовать generic `Action(..., output: "outcome")`, если им нужен результат ветки.

Adapter не добавляет choices, NPC metadata, portraits или animation outputs.
