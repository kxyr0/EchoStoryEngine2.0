# Отмена

В N3 cancellation не поддерживается.

Descriptor capabilities:

- `supportsRecovery=true`;
- `supportsCancellation=false`;
- `supportsAsyncCompletion=true`;
- `idempotentStartRequired=true`.

`cancel` возвращает `UNSUPPORTED` и не откатывает durable state NPC.
