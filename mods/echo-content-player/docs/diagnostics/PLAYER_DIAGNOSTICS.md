# Диагностика профиля игрока

Диагностика bounded: сообщение описывает код, severity, текст и source. Она не должна включать произвольный дамп профиля или бесконечный stack trace.

Ключевые коды P1:

- `PLAYER_NAME_INVALID`: custom display name нарушает правила plain text.
- `PLAYER_SKIN_INVALID`: PNG, path или resource id не прошли validation.
- `PLAYER_TEMPLATE_PARSE_ERROR`: EchoScript parser не построил syntax tree.
- `PLAYER_TEMPLATE_BIND_ERROR`: AST корректен, но template semantics неверны.
- `PLAYER_MUTATION_CONFLICT`: mutation id повторён с другими аргументами.
- `PLAYER_PROFILE_MISSING`: mutation пришла для неизвестного `EchoPlayerId`.

Сломанный template не перезаписывает существующие профили. Сломанный skin не делает игрока невидимым: используется previous known-good или account fallback.
