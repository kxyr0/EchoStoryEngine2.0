# Player.Rotate

`Player.Rotate(yaw, pitch)` задаёт абсолютный поворот тела и головы настоящего игрока.

`yaw` вращает body/head yaw, `pitch` вращает view/head pitch. Roll отсутствует. Если `duration` равен нулю, поворот применяется сразу. Если duration положительный, команда является blocking и прогрессирует по server tick.

Поддержанные interpolation: `LINEAR`, `SMOOTH`, `CUBIC`.
