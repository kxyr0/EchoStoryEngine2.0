# Player.LookAt

`Player.LookAt(x, y, z)` вычисляет yaw/pitch от eye position игрока до мировой точки.

Команда использует shortest yaw path, clamp pitch до физического диапазона и отклоняет слишком близкую цель. По умолчанию режим `BODY_AND_HEAD`: тело и голова поворачиваются вместе. `HEAD_ONLY` зарезервирован public contract-ом для будущего точного head policy.
