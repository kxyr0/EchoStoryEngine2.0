# Identity

## Definition identity

`NpcDefinitionId` описывает resource-authored definition. Он стабилен между reload
при неизменном `.npc.es`.

## Actor identity

`ActorId` задается вызывающей стороной при spawn. Он является логическим public ID
актора и не равен Minecraft entity UUID.

## Entity identity

Forge entity UUID остается platform handle. Он хранится только внутри Forge host
и не выходит через public API.

## Conflict rules

- Повторный spawn того же `ActorId` и той же definition возвращает
  `ALREADY_ACTIVE` и не создает вторую entity.
- Повторный spawn того же `ActorId` с другой definition возвращает
  `ACTOR_ID_CONFLICT`.
- Две разные `ActorId` могут использовать одну `NpcDefinitionId`.
- Failed platform spawn не регистрирует actor.
