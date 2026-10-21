# Player.StandUp

`Player.StandUp()` безопасно снимает actor с текущего vanilla vehicle через `stopRiding`.

Если игрок уже стоит, команда idempotent и завершается успехом. Команда не симулирует Shift и не меняет keybindings. Для нестандартных сидений предусмотрен public extension point `PlayerActorSeatProvider`.
