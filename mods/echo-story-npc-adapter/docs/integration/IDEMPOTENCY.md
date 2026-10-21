# Идемпотентность

`ActionExecutionId` сопоставляется с одним `ActorMutationId` через
детерминированное UUID-v5-style SHA-1 name derivation.

Mapper использует:

- fixed namespace UUID;
- canonical `ActionExecutionId.asString()`;
- UTF-8 bytes;
- lowercase output namespace `echo_story_npc_adapter`;
- без clock;
- без random;
- без locale-sensitive casing.

Если active observation уже использует тот же `ActionExecutionId` с другим
payload fingerprint, адаптер отклоняет дубль как idempotency conflict.
