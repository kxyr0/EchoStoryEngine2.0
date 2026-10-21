# Spawn

Action type: `echo_npc:spawn`.

Обязательный payload:

- `actorId`;
- `definitionId`;
- `worldId`;
- `x`;
- `y`;
- `z`;
- `yaw`;
- `pitch`.

Адаптер отправляет `NpcService.spawn(SpawnNpcRequest)` с детерминированным
`ActorMutationId`. Success completion отправляется только после того, как
public NPC read model показывает совпадающий definition, target transform,
compatible definition и `MATERIALIZED` actor.

Outputs:

- `actorId`;
- `result=created|already_present`.

Тот же `ActorId` с другим definition или другим desired transform отклоняется.
