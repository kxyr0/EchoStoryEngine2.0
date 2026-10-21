# Sleep.Arm

`Sleep.Arm` явно связывает следующую успешную попытку сна с одним scenario id.

Один игрок может иметь только один активный arm. Повтор того же durable command id возвращает idempotent success. Другой scenario при активном arm отклоняется diagnostic-кодом `PLAYER_SLEEP_ALREADY_ARMED`.

Arm не потребляется при клике. Он потребляется только после сохранения `PlayerSleepSessionSnapshot`, поэтому crash до session creation оставляет arm активным.

Если scenario не загружен, возвращается `PLAYER_SLEEP_SCENARIO_NOT_FOUND`, а ванильный сон не изменяется.
