# Быстрый старт профиля игрока

Echo Content Player создаёт профиль при первом login игрока. Ключ профиля — `EchoPlayerId`, типизированная обёртка над UUID аккаунта Minecraft. Имя аккаунта, entity id и текущая `ServerPlayer` entity не являются durable profile identity.

Минимальный профиль содержит:

- `EchoPlayerId`;
- account name как текущее имя из `GameProfile`;
- Echo display name в режиме `ACCOUNT` или `CUSTOM`;
- `PlayerSpark` current/max;
- base appearance в режиме `ACCOUNT` или `RESOURCE`;
- `PlayerProfileTemplateId` и fingerprint шаблона;
- lifecycle metadata;
- profile/repository revision.

Default template применяется только при первом создании профиля. Повторный login, respawn, dimension change и restart не перезаписывают уже созданный профиль.

Команда для ручной проверки в игре:

```text
/echoplayer inspect
```

Она показывает `EchoPlayerId`, resolved display name, Spark и revision.
