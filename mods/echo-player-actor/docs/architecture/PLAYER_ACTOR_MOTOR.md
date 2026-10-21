# Player Actor Motor

`PlayerActorMotor` является public boundary для movement.

Forge runtime содержит `ServerPlayerActorMotor`: он делает один active move на actor, проверяет dimension, отклоняет seated actor и двигает `ServerPlayer` через `Entity.move(MoverType.SELF, step)`.

Это не teleport-each-tick policy. Collision остаётся в Minecraft movement pipeline.
