# Player body lock

Body lock хранит snapshot тела игрока на момент старта cutscene:

- координаты;
- yaw/pitch;
- pose;
- vehicle UUID;
- selected hotbar slot;
- main/offhand item stacks;
- текущий `GameType`.

Во время playback `LockedPlayerMotionGuard` проверяет `ServerPlayer` на server tick. Если тело ушло с сохраненной
позиции или поменяло pose, оно корректируется серверным teleport. Это не spectator и не fake player.

Предметы и selected slot восстанавливаются в snapshot для диагностики и дальнейшего расширения. В текущем runtime
главный enforcement - позиция и pose, потому что они критичны для C1 сцены.
