# Player Actor predicates

Predicates читают состояние настоящего тела и не создают команду движения.

- `Player.IsMoving()` возвращает состояние active `PlayerActorMotor`.
- `Player.IsSitting()` проверяет passenger/seat snapshot.
- `Player.IsUsingItem()` проверяет server-side `ServerPlayer.isUsingItem()`.

В Dialogue binder predicates компилируются как `echo_player_actor:is_*` external commands с non-blocking intent.
