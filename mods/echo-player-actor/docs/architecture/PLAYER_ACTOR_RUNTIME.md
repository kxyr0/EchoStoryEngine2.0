# Player Actor runtime

Runtime расположен в `player-actor-forge-1.20.1`.

Главные классы:

- `io.github.kxyr0.echoplayeractor.forge.runtime.ServerPlayerActorHost` публикует public services.
- `PlayerActorCommandGateway` выполняет команды после lease validation.
- `ServerPlayerBodyResolver` резолвит `PlayerActorId` в настоящий `ServerPlayer`.
- `ServerPlayerRotationFlow`, `ServerPlayerActorMotor`, `ServerPlayerInteractionPipeline` отвечают за узкие виды body operation.

Contracts/core не импортируют Minecraft или Forge.
