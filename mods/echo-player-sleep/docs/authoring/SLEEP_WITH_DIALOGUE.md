# Sleep с Dialogue

Echo Player Sleep не импортирует Dialogue implementation.

Правильная схема:

1. Sleep scenario входит в dream room.
2. Story запускает Dialogue обычной внешней командой Dialogue.
3. Sleep runtime только удерживает session, return mode и visual transition.

Так Dialogue можно менять независимо от сна, а sleep lifecycle остаётся доступным будущим adapters.
