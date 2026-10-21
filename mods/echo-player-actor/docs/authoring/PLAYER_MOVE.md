# Player.Move

`Player.Move(x, y, z)` запускает blocking движение настоящего `ServerPlayer` к позиции в текущем dimension.

В PA1 movement идёт через `PlayerActorMotor`, который делает server-tick шаги через collision-aware `Entity.move`. Команда отклоняется, если actor сидит: сначала вызовите `Player.StandUp()`.

`Player.Stop()` отменяет активное движение и не меняет GameMode.
