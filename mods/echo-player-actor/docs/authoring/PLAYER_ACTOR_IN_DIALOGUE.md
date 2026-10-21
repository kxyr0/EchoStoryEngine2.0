# Player Actor в Dialogue

Dialogue EchoScript binder распознаёт `Player.*` и сохраняет их как opaque external commands с типами `echo_player_actor:*`.

Команда должна стоять перед следующей `Line`, чтобы Dialogue мог перейти в WAITING и затем продолжить с нужной строки. Текущий actor берётся из viewer/player context Dialogue.

Диалог не импортирует Player Actor runtime и не знает о `ServerPlayer`.
