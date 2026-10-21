# Диагностика

N3 adapter возвращает русские rejection details через `ActionInvocationResult`.

Примеры:

- в payload отсутствуют обязательные arguments или есть unknown arguments;
- `ActorMutationId` уже используется другим action или actor;
- `ActorId` конфликтует с другим `NpcDefinitionId`;
- `NpcService` не удалось получить;
- очередь observations заполнена.

Адаптер не создает отдельные log files. Story и Echo NPC остаются
авторитетными diagnostic surfaces для собственных persistence/runtime ошибок.
