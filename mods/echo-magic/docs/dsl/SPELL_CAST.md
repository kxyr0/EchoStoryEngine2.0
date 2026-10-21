# Cast

`Cast(Origin(PLAYER_EYES), Direction(PLAYER_LOOK), Hand(MAIN_HAND), SwingHand(true))` задаёт источник, направление и vanilla swing. Сервер берёт фактический `ServerPlayer.getLookAngle()` и добавляет безопасный offset 0.35 блока.

Клиент отправляет только intent и не передаёт произвольный direction.

