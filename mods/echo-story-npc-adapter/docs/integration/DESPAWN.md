# Despawn

Action type: `echo_npc:despawn`.

Обязательный payload:

- `actorId`.

Если `NpcService.query(actorId)` возвращает not found, completion использует
`result=already_absent`. Иначе адаптер отправляет
`NpcService.despawn(actorId, mutationId)`.

Outputs:

- `actorId`;
- `result=removed|already_absent`.

Адаптер никогда не удаляет repository files или Minecraft entities напрямую.
