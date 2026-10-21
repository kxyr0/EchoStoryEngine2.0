# Player.Interact

`Player.Interact(x, y, z, face: WEST, hand: MAIN_HAND)` выполняет взаимодействие с блоком через серверный interaction pipeline.

Runtime проверяет distance, line of sight и наличие block target. Затем вызывает `ServerPlayerGameMode.useItemOn`, делает swing и сохраняет receipt key. Это не является симуляцией ПКМ и не зависит от пользовательских keybindings.

Для произвольных модовых блоков PA1 не обещает exactly-once без transactional boundary блока.
