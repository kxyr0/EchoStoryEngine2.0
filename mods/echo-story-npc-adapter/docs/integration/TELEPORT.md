# Teleport

Action type: `echo_npc:teleport`.

Обязательный payload:

- `actorId`;
- `worldId`;
- `x`;
- `y`;
- `z`;
- `yaw`;
- `pitch`.

Адаптер требует, чтобы actor уже существовал. Если public read model уже
совпадает с target transform, completion использует
`result=already_at_target`. Иначе адаптер отправляет
`NpcService.teleport(TeleportNpcRequest)` с детерминированным
`ActorMutationId`.

Outputs:

- `actorId`;
- `result=moved|already_at_target`.

Teleport не spawn-ит отсутствующих actors и не выполняет pathfinding.
