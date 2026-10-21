# Player Actor Interaction

Interaction pipeline:

EchoScript → `Player.Interact` → reach/LOS preflight → `ServerPlayerGameMode.useItemOn` → swing → receipt → result.

Pre-state/post-state witness в PA1 представлен receipt key и snapshot revision. Для модовых блоков без transaction boundary runtime честно возвращает uncertain/rejected state вместо обещания exactly-once.
