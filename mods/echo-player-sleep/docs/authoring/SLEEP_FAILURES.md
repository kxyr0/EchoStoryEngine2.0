# Ошибки сна

Основные diagnostics:

- `PLAYER_SLEEP_SCENARIO_NOT_FOUND`;
- `PLAYER_SLEEP_ALREADY_ARMED`;
- `PLAYER_SLEEP_VANILLA_CONFIRMATION_TIMEOUT`;
- `PLAYER_SLEEP_EYES_NOT_CLOSED`;
- `PLAYER_SLEEP_DREAM_DIMENSION_INVALID`;
- `PLAYER_SLEEP_RETURN_FAILED`;
- `PLAYER_SLEEP_RECOVERY_FAILED`.

При неудачном vanilla bed interaction session не создаётся. Vanilla feedback остаётся за Minecraft.

Зависший black screen диагностируется через active session status, последнюю transition revision и отсутствие ожидаемого acknowledgement.
