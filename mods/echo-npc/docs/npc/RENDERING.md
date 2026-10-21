# Rendering

N1 renderer intentionally simple.

## Entity

`EchoNpcEntity` наследуется от `PathfinderMob`, но N1 отключает AI goals и
самостоятельное поведение. Entity служит platform representation для actor.

## Synced presentation data

На клиент синхронизируются только bounded strings:

- `ActorId`;
- `NpcDefinitionId`;
- display name;
- skin ID.

Клиент не получает mutable definition objects и не управляет actor lifecycle.

## Renderer

Forge client bootstrap регистрирует `EchoNpcEntityRenderer` только на `Dist.CLIENT`.
Dedicated server не должен загружать renderer classes.
