# Recovery Flow

`DialoguePlayActionHandler.recover` использует тот же deterministic `DialogueSessionId`, что и start:

`story-action/` + `ActionExecutionId.asString()`

Поведение:

- `ACTIVE` - adapter заново подписывается на terminal transition.
- `COMPLETED` - adapter немедленно отправляет Story success completion.
- `CANCELLED` - adapter немедленно отправляет Story cancellation completion.
- `BLOCKED` - adapter отправляет Story failure completion с `dialogue.blockedReason`.
- `NOT_FOUND` - adapter повторяет start с тем же `DialogueSessionId`.
- `CONFLICT` или session mismatch - Dialogue public service не отдаёт чужую сессию как успешный reuse.

Pre-dispatch crash window покрыт веткой `NOT_FOUND`: Story уже имеет WAITING action, а Dialogue session ещё не создана.

Post-completion crash window покрыт durable terminal retention Echo Dialogue: terminal public view остаётся доступным для
query и немедленного completion.
