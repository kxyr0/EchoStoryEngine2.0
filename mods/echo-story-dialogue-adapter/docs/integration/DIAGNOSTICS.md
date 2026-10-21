# Diagnostics

Пользовательские diagnostics остаются короткими и bounded:

- unsupported action type
- некорректный `echo_dialogue:play` payload
- Echo Dialogue service ещё не готов
- Echo Dialogue service lookup failed
- dialogue-session-unavailable

Adapter не пишет отдельные log files и не сохраняет diagnostics на диск. Forge logging остаётся стандартным runtime
поведением.
